package com.flipverse.data.domain

import com.flipverse.shared.RequestState
import com.flipverse.shared.domain.ChatMessage
import com.flipverse.shared.domain.ChatParticipant
import com.flipverse.shared.domain.Conversation
import com.flipverse.shared.domain.ConversationPreview
import com.flipverse.shared.domain.TypingIndicator
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    // Conversation management
    suspend fun createConversation(
        currentUserId: String,
        otherUserId: String
    ): RequestState<Conversation>

    suspend fun getOrCreateConversation(
        currentUserId: String,
        otherUserId: String
    ): RequestState<Conversation>

    suspend fun getConversations(userId: String): RequestState<List<ConversationPreview>>

    fun getConversationsFlow(userId: String): Flow<RequestState<List<ConversationPreview>>>

    suspend fun getConversation(conversationId: String): RequestState<Conversation>

    // Message management
    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        recipientId: String,
        content: String,
        replyToMessageId: String? = null,
        attachmentUrl: String? = null,
        attachmentType: String? = null
    ): RequestState<ChatMessage>

    fun getMessagesFlow(conversationId: String): Flow<RequestState<List<ChatMessage>>>

    suspend fun getMessages(
        conversationId: String,
        limit: Int = 50,
        before: String? = null
    ): RequestState<List<ChatMessage>>

    suspend fun markMessageAsRead(messageId: String, userId: String): RequestState<Unit>

    suspend fun markConversationAsRead(conversationId: String, userId: String): RequestState<Unit>

    // Typing indicators
    suspend fun updateTypingStatus(
        conversationId: String,
        userId: String,
        isTyping: Boolean
    ): RequestState<Unit>

    fun getTypingIndicators(conversationId: String): Flow<RequestState<List<TypingIndicator>>>

    // User status
    suspend fun updateUserOnlineStatus(userId: String, isOnline: Boolean): RequestState<Unit>

    suspend fun getUserOnlineStatus(userId: String): RequestState<Boolean>

    fun getUserOnlineStatusFlow(userId: String): Flow<RequestState<Boolean>>

    // Conversation actions
    suspend fun muteConversation(conversationId: String, userId: String): RequestState<Unit>

    suspend fun unmuteConversation(conversationId: String, userId: String): RequestState<Unit>

    suspend fun pinConversation(conversationId: String, userId: String): RequestState<Unit>

    suspend fun unpinConversation(conversationId: String, userId: String): RequestState<Unit>

    suspend fun deleteConversation(conversationId: String, userId: String): RequestState<Unit>

    // Search
    suspend fun searchMessages(
        conversationId: String,
        query: String
    ): RequestState<List<ChatMessage>>

    // User details
    suspend fun getUserDetails(userId: String): RequestState<ChatParticipant>
}