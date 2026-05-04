package com.flipverse.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flipverse.data.domain.ChatRepository
import com.flipverse.data.util.getCurrentTimeMillis
import com.flipverse.shared.PreferencesRepository.getEmail
import com.flipverse.shared.PreferencesRepository.getId
import com.flipverse.shared.RequestState
import com.flipverse.shared.domain.ChatMessage
import com.flipverse.shared.domain.ChatParticipant
import com.flipverse.shared.domain.Conversation
import com.flipverse.shared.domain.ConversationPreview
import com.flipverse.shared.domain.MessageStatus
import com.flipverse.shared.domain.TypingIndicator
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

data class ChatState(
    val conversations: List<ConversationPreview> = emptyList(),
    val currentConversation: Conversation? = null,
    val messages: List<ChatMessage> = emptyList(),
    val typingIndicators: List<TypingIndicator> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMessages: Boolean = false,
    val isSendingMessage: Boolean = false,
    val error: String? = null,
    val currentUserId: String = "",
    val isTyping: Boolean = false,
    val otherParticipant: ChatParticipant? = null,
    val currentUserOnlineStatus: Boolean = false,
    val lastConversationsLoadTime: Long = 0L
)

class ChatViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        println("ChatViewModel coroutine exception: ${throwable::class.simpleName}: ${throwable.message}")
        throwable.printStackTrace()
    }

    private val _uiState = MutableStateFlow(ChatState())
    val uiState: StateFlow<ChatState> = _uiState.asStateFlow()

    private var messagesJob: Job? = null
    private var typingJob: Job? = null
    private var typingTimeoutJob: Job? = null
    private var onlineStatusJob: Job? = null
    private var onlineStatusRefreshJob: Job? = null
    private var loadConversationsJob: Job? = null

    // Debounce time to prevent multiple rapid loads
    private val loadDebounceMs = 2000L // 2 seconds

    companion object {
        /**
         * Clear all chat caches globally.
         * Call this when user logs out or switches accounts.
         */
        fun clearAllCaches() {
            println("🗑️ Clearing all chat caches on logout/account switch")
        }
    }

    init {
        _uiState.value = _uiState.value.copy(currentUserId = getEmail())
        loadConversations()
    }

    fun loadConversations(forceRefresh: Boolean = false) {
        loadConversationsJob?.cancel()

        loadConversationsJob = viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            try {
                val currentTime = getCurrentTimeMillis()
                val timeSinceLastLoad = currentTime - _uiState.value.lastConversationsLoadTime

                if (!forceRefresh && timeSinceLastLoad < loadDebounceMs) {
                    return@launch
                }

                // Show cached conversations instantly (no spinner on revisit)
                val hasCached = _uiState.value.conversations.isNotEmpty()
                _uiState.value = _uiState.value.copy(
                    isLoading = !hasCached, // Only show spinner on first load
                    error = null
                )

                val result = chatRepository.getConversations(_uiState.value.currentUserId)
                when (result) {
                    is RequestState.Success -> {
                        _uiState.value = _uiState.value.copy(
                            conversations = result.getSuccessData(),
                            isLoading = false,
                            lastConversationsLoadTime = currentTime
                        )
                    }

                    is RequestState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.getErrorMessage()
                        )
                    }

                    else -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load conversations"
                )
            }
        }
    }

    fun startConversationWith(otherUserId: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch(exceptionHandler) {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val result = chatRepository.getOrCreateConversation(
                    _uiState.value.currentUserId,
                    otherUserId
                )
                when (result) {
                    is RequestState.Success -> {
                        println("Conversation created successfully: ${result.getSuccessData()}")
                        val conversation = result.getSuccessData()
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        onSuccess(conversation.id)
                    }

                    is RequestState.Error -> {
                        println("Error creating conversation: ${result.getErrorMessage()}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.getErrorMessage()
                        )
                    }

                    else -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to create conversation"
                )
            }
        }
    }

    fun loadConversation(conversationId: String) {
        viewModelScope.launch(exceptionHandler) {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val result = chatRepository.getConversation(conversationId)
                when (result) {
                    is RequestState.Success -> {
                        val conversation = result.getSuccessData()
                        _uiState.value = _uiState.value.copy(
                            currentConversation = conversation,
                            isLoading = false
                        )

                        // Start listening to messages and typing indicators
                        startListeningToMessages(conversationId)
                        startListeningToTypingIndicators(conversationId)

                        // Mark conversation as read
                        markConversationAsRead(conversationId)
                        conversation.participantIds.firstOrNull { it != _uiState.value.currentUserId }
                            ?.let { userId ->
                                otherParticipantDetails(userId)
                                startComprehensiveOnlineStatusMonitoring(userId)
                            }
                    }

                    is RequestState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.getErrorMessage()
                        )
                    }

                    else -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load conversation"
                )
            }
        }
    }

    private fun startListeningToMessages(conversationId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch(exceptionHandler) {
            chatRepository.getMessagesFlow(conversationId).collectLatest { result ->
                when (result) {
                    is RequestState.Success -> {
                        val newMessages = result.getSuccessData().sortedBy { it.timestamp }
                        val currentMessages = _uiState.value.messages

                        // Merge real-time messages with optimistic messages
                        val mergedMessages = mergeMessages(currentMessages, newMessages)

                        _uiState.value = _uiState.value.copy(
                            messages = mergedMessages,
                            isLoadingMessages = false
                        )
                    }

                    is RequestState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoadingMessages = false,
                            error = result.getErrorMessage()
                        )
                    }

                    is RequestState.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoadingMessages = true)
                    }

                    else -> {}
                }
            }
        }
    }

    private fun mergeMessages(
        currentMessages: List<ChatMessage>,
        newMessages: List<ChatMessage>
    ): List<ChatMessage> {
        // Keep optimistic messages (temp_*) that haven't been replaced yet
        val optimisticMessages = currentMessages.filter { it.id.startsWith("temp_") }

        // Filter out optimistic messages that have a corresponding real message
        val validOptimisticMessages = optimisticMessages.filter { optimistic ->
            // Check if there's a real message with same content and sender
            !newMessages.any { real ->
                real.content == optimistic.content &&
                        real.senderId == optimistic.senderId &&
                        !real.id.startsWith("temp_")
            }
        }

        // Get all non-optimistic messages from current state
        val nonOptimisticMessages = currentMessages.filter { !it.id.startsWith("temp_") }

        // Combine new messages with valid optimistic messages, avoiding duplicates
        val allMessages = mutableListOf<ChatMessage>()

        // Add all new messages first
        allMessages.addAll(newMessages)

        // Add optimistic messages that don't have real counterparts
        allMessages.addAll(validOptimisticMessages)

        // Remove duplicates by ID and sort by timestamp
        return allMessages.distinctBy { it.id }.sortedBy { it.timestamp }
    }

    private fun startListeningToTypingIndicators(conversationId: String) {
        typingJob?.cancel()
        typingJob = viewModelScope.launch(exceptionHandler) {
            chatRepository.getTypingIndicators(conversationId).collectLatest { result ->
                when (result) {
                    is RequestState.Success -> {
                        println("Typing indicators updated: ${result.getSuccessData()}")
                        val indicators = result.getSuccessData()
                            .filter { it.userId != _uiState.value.currentUserId && it.isTyping }
                        _uiState.value = _uiState.value.copy(typingIndicators = indicators)
                    }

                    else -> {}
                }
            }
        }
    }

    fun startComprehensiveOnlineStatusMonitoring(userId: String) {
        // Cancel existing monitoring jobs
        onlineStatusJob?.cancel()
        onlineStatusRefreshJob?.cancel()

        println("🟢 Starting comprehensive online status monitoring for user: $userId")

        // Primary real-time flow listener
        onlineStatusJob = viewModelScope.launch(exceptionHandler) {
            try {
                chatRepository.getUserOnlineStatusFlow(userId).collectLatest { result ->
                    when (result) {
                        is RequestState.Success -> {
                            val isOnline = result.getSuccessData()
                            val currentParticipant = _uiState.value.otherParticipant

                            println("🔄 Online status updated from flow - User: $userId, Online: $isOnline")

                            // Update the other participant's online status
                            _uiState.value = _uiState.value.copy(
                                otherParticipant = currentParticipant?.copy(
                                    isOnline = isOnline,
                                    lastSeen = if (!isOnline) getCurrentTimeMillis() else currentParticipant.lastSeen
                                )
                            )
                        }

                        is RequestState.Error -> {
                            println("❌ Error in online status flow: ${result.getErrorMessage()}")
                            // Don't update UI on error - keep last known status
                        }

                        is RequestState.Loading -> {
                            println("⏳ Loading online status...")
                        }

                        else -> {
                            println("🔍 Unknown state in online status flow")
                        }
                    }
                }
            } catch (e: Exception) {
                println("❌ Exception in online status monitoring: ${e.message}")
            }
        }

        // Secondary periodic refresh job (every 30 seconds) for backup/accuracy
        onlineStatusRefreshJob = viewModelScope.launch(exceptionHandler) {
            try {
                while (true) {
                    delay(30000) // 30 seconds

                    // Check if coroutine is still active before proceeding
                    if (!isActive) {
                        println("🔄 Periodic refresh cancelled - stopping")
                        break
                    }

                    try {
                        // Refresh participant details including online status
                        refreshParticipantOnlineStatus(userId)
                    } catch (e: Exception) {
                        println("❌ Error in single refresh attempt: ${e.message}")
                        // Continue the loop even on error, but don't crash
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                println("🔄 Periodic online status refresh cancelled")
                // Don't rethrow CancellationException - it's expected when coroutine is cancelled
            } catch (e: Exception) {
                println("❌ Unexpected error in periodic refresh: ${e.message}")
            }
        }
    }

    /**
     * Refreshes the participant's online status by fetching fresh data
     * This is used as a backup to the real-time flow
     */
    private suspend fun refreshParticipantOnlineStatus(userId: String) {
        try {
            val result = chatRepository.getUserDetails(userId)
            when (result) {
                is RequestState.Success -> {
                    val updatedParticipant = result.getSuccessData()
                    val currentParticipant = _uiState.value.otherParticipant

                    // Only update if there's actually a change to avoid unnecessary UI updates
                    if (currentParticipant?.isOnline != updatedParticipant.isOnline ||
                        currentParticipant?.lastSeen != updatedParticipant.lastSeen
                    ) {

                        println("🔄 Refreshed participant status - Online: ${updatedParticipant.isOnline}")

                        _uiState.value = _uiState.value.copy(
                            otherParticipant = updatedParticipant
                        )
                    }
                }

                is RequestState.Error -> {
                    println("❌ Error refreshing participant status: ${result.getErrorMessage()}")
                }

                else -> {
                    println("🔍 Unknown state when refreshing participant status")
                }
            }
        } catch (e: Exception) {
            println("❌ Exception refreshing participant status: ${e.message}")
        }
    }

    /**
     * Updates the other participant's status in real-time
     * This is an enhanced version that includes better state management
     */
    fun updateOtherParticipantOnlineStatus(userId: String, isOnline: Boolean) {
        val currentParticipant = _uiState.value.otherParticipant

        if (currentParticipant?.userId == userId) {
            val updatedParticipant = currentParticipant.copy(
                isOnline = isOnline,
                lastSeen = if (!isOnline) getCurrentTimeMillis() else currentParticipant.lastSeen
            )

            _uiState.value = _uiState.value.copy(
                otherParticipant = updatedParticipant
            )

            println("✅ Updated participant online status - User: $userId, Online: $isOnline")
        }
    }

    fun sendMessage(
        content: String,
        replyToMessageId: String? = null,
        attachmentUrl: String? = null,
        attachmentType: String? = null,
        onSuccess: () -> Unit
    ) {
        val conversation = _uiState.value.currentConversation ?: return
        val trimmedContent = content.trim().removePrefix("TEXT:")
        if (trimmedContent.isEmpty()) return

        viewModelScope.launch(exceptionHandler) {
            try {
                _uiState.value = _uiState.value.copy(isSendingMessage = true, error = null)

                // Stop typing indicator
                updateTypingStatus(false)

                // Get recipient ID
                val recipientId = conversation.participantIds
                    .firstOrNull { it != _uiState.value.currentUserId } ?: ""

                // Create optimistic message for immediate display
                val currentTime = getCurrentTimeMillis()
                val optimisticMessage = ChatMessage(
                    id = "temp_${currentTime}",
                    conversationId = conversation.id,
                    senderId = _uiState.value.currentUserId,
                    recipientId = recipientId,
                    content = trimmedContent,
                    timestamp = currentTime,
                    messageType = com.flipverse.shared.domain.MessageType.TEXT,
                    status = com.flipverse.shared.domain.MessageStatus.SENDING,
                    isRead = false,
                    replyToMessageId = replyToMessageId,
                    attachmentUrl = attachmentUrl,
                    attachmentType = when (attachmentType) {
                        "IMAGE" -> com.flipverse.shared.domain.AttachmentType.IMAGE
                        "DOCUMENT" -> com.flipverse.shared.domain.AttachmentType.DOCUMENT
                        "VIDEO" -> com.flipverse.shared.domain.AttachmentType.VIDEO
                        "AUDIO" -> com.flipverse.shared.domain.AttachmentType.AUDIO
                        else -> null
                    }
                )

                // Add optimistic message to UI immediately
                val currentMessages = _uiState.value.messages.toMutableList()
                currentMessages.add(optimisticMessage)
                _uiState.value = _uiState.value.copy(messages = currentMessages)

                val result = chatRepository.sendMessage(
                    conversationId = conversation.id,
                    senderId = _uiState.value.currentUserId,
                    recipientId = recipientId,
                    content = trimmedContent,
                    replyToMessageId = replyToMessageId,
                    attachmentUrl = attachmentUrl,
                    attachmentType = attachmentType
                )

                when (result) {
                    is RequestState.Success -> {
                        println("✅ Message sent successfully: ${result.getSuccessData()}")
                        _uiState.value = _uiState.value.copy(isSendingMessage = false)

                        // Update the optimistic message with the real message data
                        val realMessage = result.getSuccessData()
                        val updatedMessages = _uiState.value.messages.map { message ->
                            if (message.id == optimisticMessage.id) {
                                realMessage.copy(status = com.flipverse.shared.domain.MessageStatus.SENT)
                            } else {
                                message
                            }
                        }
                        _uiState.value = _uiState.value.copy(messages = updatedMessages)

                        // Only refresh conversations list when needed (user navigates back to conversations screen)
                        // Removed automatic loadConversations() call here for performance
                        onSuccess()
                    }

                    is RequestState.Error -> {
                        println("❌ Failed to send message: ${result.getErrorMessage()}")

                        // Remove the optimistic message and show error
                        val filteredMessages =
                            _uiState.value.messages.filter { it.id != optimisticMessage.id }
                        _uiState.value = _uiState.value.copy(
                            messages = filteredMessages,
                            isSendingMessage = false,
                            error = result.getErrorMessage()
                        )
                    }

                    else -> {
                        // Remove optimistic message on unknown state
                        val filteredMessages =
                            _uiState.value.messages.filter { it.id != optimisticMessage.id }
                        _uiState.value = _uiState.value.copy(
                            messages = filteredMessages,
                            isSendingMessage = false
                        )
                    }
                }
            } catch (e: Exception) {
                // Remove optimistic message on exception
                val filteredMessages = _uiState.value.messages.filter { !it.id.startsWith("temp_") }
                _uiState.value = _uiState.value.copy(
                    messages = filteredMessages,
                    isSendingMessage = false,
                    error = e.message ?: "Failed to send message"
                )
            }
        }
    }

    fun updateTypingStatus(isTyping: Boolean) {
        val conversation = _uiState.value.currentConversation ?: return
        if (isTyping == _uiState.value.isTyping) return

        _uiState.value = _uiState.value.copy(isTyping = isTyping)

        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            chatRepository.updateTypingStatus(
                conversationId = conversation.id,
                userId = _uiState.value.currentUserId,
                isTyping = isTyping
            )
        }

        if (isTyping) {
            // Cancel typing after 3 seconds
            typingTimeoutJob?.cancel()
            typingTimeoutJob = viewModelScope.launch(exceptionHandler) {
                delay(3000)
                updateTypingStatus(false)
            }
        } else {
            typingTimeoutJob?.cancel()
        }
    }

    private fun markConversationAsRead(conversationId: String) {
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            chatRepository.markConversationAsRead(conversationId, _uiState.value.currentUserId)
        }
    }

    fun markMessageAsRead(messageId: String) {
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            chatRepository.markMessageAsRead(messageId, _uiState.value.currentUserId)
        }
    }

    fun muteConversation(conversationId: String) {
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            chatRepository.muteConversation(conversationId, _uiState.value.currentUserId)
            loadConversations(forceRefresh = true) // Force refresh to update mute status
        }
    }

    fun pinConversation(conversationId: String) {
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            chatRepository.pinConversation(conversationId, _uiState.value.currentUserId)
            loadConversations(forceRefresh = true) // Force refresh to update pin status
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            chatRepository.deleteConversation(conversationId, _uiState.value.currentUserId)
            loadConversations(forceRefresh = true) // Force refresh conversations list
        }
    }

    fun clearCurrentConversation() {
        messagesJob?.cancel()
        typingJob?.cancel()
        typingTimeoutJob?.cancel()
        onlineStatusJob?.cancel()
        onlineStatusRefreshJob?.cancel()

        _uiState.value = _uiState.value.copy(
            currentConversation = null,
            messages = emptyList(),
            typingIndicators = emptyList(),
            isTyping = false,
            otherParticipant = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        messagesJob?.cancel()
        typingJob?.cancel()
        typingTimeoutJob?.cancel()
        onlineStatusJob?.cancel()
        onlineStatusRefreshJob?.cancel()
        loadConversationsJob?.cancel()
    }

    /**
     * Gets details about the other participant in the currently loaded conversation.
     * Returns null if other participant cannot be found.
     */
    fun otherParticipantDetails(userId: String) {
        viewModelScope.launch(exceptionHandler) {
            try {
                val result = chatRepository.getUserDetails(userId)
                when (result) {
                    is RequestState.Success -> {
                        val participant = result.getSuccessData()
                        _uiState.value = _uiState.value.copy(
                            otherParticipant = participant
                        )
                        println("Other participant details loaded: $participant")
                    }

                    is RequestState.Error -> {
                        println("Error loading other participant details: ${result.getErrorMessage()}")
                        _uiState.value = _uiState.value.copy(
                            error = result.getErrorMessage()
                        )
                    }

                    else -> {}
                }
            } catch (e: Exception) {
                println("Error loading other participant details: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to load participant details"
                )
            }
        }
    }

    /**
     * Updates the current user's online status
     * @param isOnline true if user is online, false if offline
     * Note: This updates the CURRENT user's status, not the other participant's status
     */
    fun updateCurrentUserOnlineStatus(isOnline: Boolean) {
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            try {
                val result = chatRepository.updateUserOnlineStatus(
                    userId = _uiState.value.currentUserId,
                    isOnline = isOnline
                )

                when (result) {
                    is RequestState.Success -> {
                        println("User online status updated successfully: isOnline = $isOnline")
                        _uiState.value = _uiState.value.copy(currentUserOnlineStatus = isOnline)
//                        _uiState.value = _uiState.value.copy(
//                            otherParticipant = _uiState.value.otherParticipant?.copy(isOnline = isOnline)
//                        )
                    }

                    is RequestState.Error -> {
                        println("Failed to update user online status: ${result.getErrorMessage()}")
                        _uiState.value = _uiState.value.copy(
                            error = result.getErrorMessage()
                        )
                    }

                    else -> {}
                }
            } catch (e: Exception) {
                println("Error updating user online status: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update online status"
                )
            }
        }
    }

    /**
     * Convenience function to set user as online
     * Usage: Call when user becomes active (app foreground, typing, sending messages)
     */
    fun setUserOnline() {
        updateCurrentUserOnlineStatus(true)
    }

    /**
     * Convenience function to set user as offline
     * Usage: Call when user becomes inactive (app background, idle for too long)
     */
    fun setUserOffline() {
        updateCurrentUserOnlineStatus(false)
    }

    /**
     * Handle app lifecycle changes for online status
     * @param isAppInForeground true when app comes to foreground, false when goes to background
     */
    fun handleAppLifecycleChange(isAppInForeground: Boolean) {
        if (isAppInForeground) {
            setUserOnline()
            println("App in foreground - User set to online")
        } else {
            setUserOffline()
            println("App in background - User set to offline")
        }
    }
}