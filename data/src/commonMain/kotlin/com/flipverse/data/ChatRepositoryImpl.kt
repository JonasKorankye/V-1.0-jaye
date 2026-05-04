package com.flipverse.data

import com.flipverse.data.domain.ChatRepository
import com.flipverse.data.util.getCurrentTimeMillis
import com.flipverse.data.util.CrashlyticsLogger
import com.flipverse.shared.RequestState
import com.flipverse.shared.domain.ChatMessage
import com.flipverse.shared.domain.ChatParticipant
import com.flipverse.shared.domain.Conversation
import com.flipverse.shared.domain.ConversationPreview
import com.flipverse.shared.domain.ConversationType
import com.flipverse.shared.domain.MessageStatus
import com.flipverse.shared.domain.MessageType
import com.flipverse.shared.domain.TypingIndicator
import com.flipverse.shared.domain.User
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.functions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable

@Serializable
private data class FirestoreConversation(
    val id: String,
    val participantIds: List<String>,
    val lastMessageContent: String,
    val lastMessageSenderId: String,
    val lastMessageTimestamp: Long,
    val lastActivity: Long,
    val createdAt: Long,
    val isActive: Boolean,
    val conversationType: String,
    val unreadCount: Map<String, Int>,
    val mutedBy: List<String>,
    val pinnedBy: List<String>
)

@Serializable
private data class FirestoreChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val recipientId: String,
    val content: String,
    val timestamp: Long,
    val messageType: String,
    val status: String,
    val isRead: Boolean,
    val editedAt: Long?,
    val replyToMessageId: String?,
    val attachmentUrl: String?,
    val attachmentType: String?
)

@Serializable
private data class FirestoreTypingIndicator(
    val conversationId: String,
    val userId: String,
    val isTyping: Boolean,
    val timestamp: Long
)

@Serializable
data class FirestoreUserStatus(
    val userId: String,
    val isOnline: Boolean,
    val lastSeen: Long
)

class ChatRepositoryImpl(

) : ChatRepository {


    private val conversationsCollection = Firebase.firestore.collection("conversations")
    private val messagesCollection = Firebase.firestore.collection("messages")
    private val typingIndicatorsCollection = Firebase.firestore.collection("typing_indicators")
    private val userStatusCollection = Firebase.firestore.collection("user_status")
    private val usersCollection = Firebase.firestore.collection("user")

    // Cache for user details to avoid redundant fetches
    private val userDetailsCache = mutableMapOf<String, Pair<ChatParticipant, Long>>()
    private val cacheExpirationMs = 5 * 60 * 1000L // 5 minutes

    companion object {
        // Static reference to clear cache on logout
        private val globalCaches = mutableListOf<MutableMap<String, *>>()

        /**
         * Clear all repository caches.
         * Call this when user logs out or switches accounts.
         */
        fun clearAllCaches() {
            globalCaches.forEach { cache ->
                cache.clear()
            }
            println("🗑️ ChatRepository: All caches cleared")
        }
    }

    init {
        // Register this cache for global clearing
        @Suppress("UNCHECKED_CAST")
        globalCaches.add(userDetailsCache as MutableMap<String, *>)
    }


    override suspend fun createConversation(
        currentUserId: String,
        otherUserId: String
    ): RequestState<Conversation> {
        return try {
            val conversationId = generateConversationId(currentUserId, otherUserId)
            val currentTime = getCurrentTimeMillis()

            // Replace dots with underscores to avoid Firebase nested path issues
            val sanitizedCurrentUserId = currentUserId.replace(".", "_")
            val sanitizedOtherUserId = otherUserId.replace(".", "_")

            val firestoreConversation = FirestoreConversation(
                id = conversationId,
                participantIds = listOf(currentUserId, otherUserId).sorted(),
                createdAt = currentTime,
                lastActivity = currentTime,
                isActive = true,
                conversationType = "DIRECT",
                unreadCount = mapOf(
                    sanitizedCurrentUserId to 0,
                    sanitizedOtherUserId to 0
                ),
                lastMessageContent = "",
                lastMessageSenderId = "",
                lastMessageTimestamp = 0L,
                mutedBy = emptyList(),
                pinnedBy = emptyList()
            )

            conversationsCollection
                .document(conversationId)
                .set(firestoreConversation)
            println("New Conversation created: $firestoreConversation")
            RequestState.Success(firestoreConversation.toDomainModel())
        } catch (e: Exception) {
            // Log to Crashlytics for debugging
            CrashlyticsLogger.logChatError(
                conversationId = generateConversationId(currentUserId, otherUserId),
                error = e,
                additionalInfo = mapOf(
                    "operation" to "createConversation",
                    "current_user_id" to currentUserId,
                    "other_user_id" to otherUserId
                )
            )
            RequestState.Error("Failed to create conversation: ${e.message}")
        }
    }

    override suspend fun getOrCreateConversation(
        currentUserId: String,
        otherUserId: String
    ): RequestState<Conversation> {
        return try {
            println("Getting or creating conversation between $currentUserId and $otherUserId")
            val conversationId = generateConversationId(currentUserId, otherUserId)
            println("Generated conversation ID: $conversationId")

            // Try to get existing conversation first
            val existingConversation = conversationsCollection
                .document(conversationId)
                .get()

            if (existingConversation.exists) {
                println("Conversation exists")
                val conversation = existingConversation.data<FirestoreConversation>()
                println("Existing conversation found: ${conversation.lastMessageContent}")
                RequestState.Success(conversation.toDomainModel())
            } else {
                println("Conversation does not exist")
                // Create new conversation
                createConversation(currentUserId, otherUserId)
            }
        } catch (e: Exception) {
            // Log to Crashlytics for debugging
            CrashlyticsLogger.logChatError(
                conversationId = generateConversationId(currentUserId, otherUserId),
                error = e,
                additionalInfo = mapOf(
                    "operation" to "getOrCreateConversation",
                    "current_user_id" to currentUserId,
                    "other_user_id" to otherUserId
                )
            )
            RequestState.Error("Failed to get or create conversation: ${e.message}")
        }
    }

    override suspend fun getConversations(userId: String): RequestState<List<ConversationPreview>> {
        return try {
            println("🔄 Fetching conversations for user: $userId")

            // Query only conversations where user is a participant (server-side filtering)
            val userConversations = conversationsCollection
                .where { "participantIds" contains userId }
                .where { "isActive" equalTo true }
                .get()

            println("📊 Found ${userConversations.documents.size} conversations")

            val conversationPreviews = userConversations.documents.mapNotNull { doc ->
                try {
                    val conversation = doc.data<FirestoreConversation>()
                    val otherUserId = conversation.participantIds.firstOrNull { it != userId }

                    if (otherUserId != null) {
                        // Fetch user details with caching
                        val participant = fetchUserDetailsWithCache(otherUserId)

                        if (participant != null) {
                            // Sanitize userId for unreadCount access
                            val sanitizedUserId = userId.replace(".", "_")
                            ConversationPreview(
                                conversation = conversation.toDomainModel(),
                                otherParticipant = participant,
                                unreadCount = conversation.unreadCount[sanitizedUserId] ?: 0,
                                isPinned = conversation.pinnedBy.contains(userId),
                                isMuted = conversation.mutedBy.contains(userId)
                            )
                        } else {
                            println("⚠️ Participant not found for userId: $otherUserId")
                            null
                        }
                    } else {
                        println("⚠️ No other participant found in conversation: ${conversation.id}")
                        null
                    }
                } catch (e: Exception) {
                    println("❌ Error processing conversation: ${e.message}")
                    null
                }
            }

            // Sort by pinned first, then by last activity descending
            val sortedConversations = conversationPreviews.sortedWith(
                compareByDescending<ConversationPreview> { it.isPinned }
                    .thenByDescending { it.conversation.lastActivity }
            )

            println("✅ Successfully loaded ${sortedConversations.size} conversations")
            RequestState.Success(sortedConversations)
        } catch (e: Exception) {
            println("❌ Failed to get conversations: ${e.message}")
            RequestState.Error("Failed to get conversations: ${e.message}")
        }
    }

    override fun getConversationsFlow(userId: String): Flow<RequestState<List<ConversationPreview>>> {
        return flow {
            emit(RequestState.Loading)

            try {
                println("🔄 Starting real-time conversation flow for user: $userId")

                // Use Firebase snapshots for real-time updates
                conversationsCollection.snapshots.collect { snapshot ->
                    try {
                        val conversationPreviews = snapshot.documents.mapNotNull { doc ->
                            try {
                                val conversation = doc.data<FirestoreConversation>()

                                // Filter for this user's conversations
                                if (conversation.participantIds.contains(userId) && conversation.isActive) {
                                    val otherUserId =
                                        conversation.participantIds.firstOrNull { it != userId }

                                    if (otherUserId != null) {
                                        // Fetch user details with caching
                                        val participant = fetchUserDetailsWithCache(otherUserId)

                                        if (participant != null) {
                                            // Sanitize userId for unreadCount access
                                            val sanitizedUserId = userId.replace(".", "_")
                                            ConversationPreview(
                                                conversation = conversation.toDomainModel(),
                                                otherParticipant = participant,
                                                unreadCount = conversation.unreadCount[sanitizedUserId]
                                                    ?: 0,
                                                isPinned = conversation.pinnedBy.contains(userId),
                                                isMuted = conversation.mutedBy.contains(userId)
                                            )
                                        } else null
                                    } else null
                                } else null
                            } catch (e: Exception) {
                                println("❌ Error processing conversation in flow: ${e.message}")
                                null
                            }
                        }

                        // Sort by pinned first, then by last activity descending
                        val sortedConversations = conversationPreviews.sortedWith(
                            compareByDescending<ConversationPreview> { it.isPinned }
                                .thenByDescending { it.conversation.lastActivity }
                        )

                        println("📊 Flow update: ${sortedConversations.size} conversations")
                        emit(RequestState.Success(sortedConversations))
                    } catch (e: Exception) {
                        println("❌ Error parsing conversations in flow: ${e.message}")
                        emit(RequestState.Error("Failed to parse conversations: ${e.message}"))
                    }
                }
            } catch (e: Exception) {
                println("❌ Conversation flow error: ${e.message}")
                emit(RequestState.Error("Conversation stream error: ${e.message}"))
            }
        }
    }

    override suspend fun getConversation(conversationId: String): RequestState<Conversation> {
        return try {
            val doc = conversationsCollection
                .document(conversationId)
                .get()

            if (doc.exists) {
                val conversation = doc.data<FirestoreConversation>()
                RequestState.Success(conversation.toDomainModel())
            } else {
                RequestState.Error("Conversation not found")
            }
        } catch (e: Exception) {
            RequestState.Error("Failed to get conversation: ${e.message}")
        }
    }

    override suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        recipientId: String,
        content: String,
        replyToMessageId: String?,
        attachmentUrl: String?,
        attachmentType: String?
    ): RequestState<ChatMessage> {
        return try {
            val messageId = "msg_${getCurrentTimeMillis()}_${(1000..9999).random()}"
            val currentTime = getCurrentTimeMillis()

            val cleanContent = if (content.startsWith("TEXT:")) {
                content.removePrefix("TEXT:")
            } else {
                content
            }

            val firestoreMessage = FirestoreChatMessage(
                id = messageId,
                conversationId = conversationId,
                senderId = senderId,
                recipientId = recipientId,
                content = cleanContent,
                timestamp = currentTime,
                messageType = if (attachmentUrl != null) "IMAGE" else "TEXT",
                status = "SENT",
                isRead = false,
                editedAt = null,
                replyToMessageId = replyToMessageId,
                attachmentUrl = attachmentUrl,
                attachmentType = attachmentType
            )

            // Add message to collection
            messagesCollection
                .document(messageId)
                .set(firestoreMessage)

            // Update conversation with last message info
            val updateData = mapOf(
                "lastMessageContent" to cleanContent,
                "lastMessageSenderId" to senderId,
                "lastMessageTimestamp" to currentTime,
                "lastActivity" to currentTime
            )

            conversationsCollection
                .document(conversationId)
                .update(updateData)

            // Call sendPushNotification Cloud Function to send a push notification
            try {
                // Get sender's name for personalized notification
                val senderName = try {
                    val senderDetails = fetchUserDetails(senderId)
                    senderDetails?.fullName ?: "Someone"
                } catch (e: Exception) {
                    "Someone"
                }

                val data = mapOf(
                    "recipientId" to recipientId,
                    "title" to senderName,
                    "body" to cleanContent,
                    "type" to "chat_message",
                    "senderId" to senderId,
                    "conversationId" to conversationId,
                    "messageId" to messageId,
                    "customData" to "chat_notification"
                )

                val callable = Firebase.functions.httpsCallable("sendPushNotification")
                callable(data)

                println("✅ Push notification triggered for recipient: $recipientId from $senderName")
            } catch (e: Exception) {
                println("❌ Failed to send push notification: ${e.message}")
                // Don't fail the message sending if push notification fails
            }

            RequestState.Success(firestoreMessage.toDomainModel())
        } catch (e: Exception) {
            // Log to Crashlytics for debugging
            CrashlyticsLogger.logChatError(
                conversationId = conversationId,
                error = e,
                additionalInfo = mapOf(
                    "operation" to "sendMessage",
                    "sender_id" to senderId,
                    "recipient_id" to recipientId,
                    "has_attachment" to (attachmentUrl != null).toString(),
                    "message_type" to (if (attachmentUrl != null) "IMAGE" else "TEXT")
                )
            )
            RequestState.Error("Failed to send message: ${e.message}")
        }
    }

    override fun getMessagesFlow(conversationId: String): Flow<RequestState<List<ChatMessage>>> {
        return flow {
            emit(RequestState.Loading)

            try {
                // Use Firebase snapshots for real-time updates
                messagesCollection.snapshots.collect { snapshot ->
                    try {
                        val messages = snapshot.documents.mapNotNull { doc ->
                            try {
                                val message = doc.data<FirestoreChatMessage>()
                                if (message.conversationId == conversationId) {
                                    message.toDomainModel()
                                } else null
                            } catch (e: Exception) {
                                println("Error parsing message: ${e.message}")
                                null
                            }
                        }.sortedBy { it.timestamp }

                        emit(RequestState.Success(messages))
                    } catch (e: Exception) {
                        emit(RequestState.Error("Failed to parse messages: ${e.message}"))
                    }
                }
            } catch (e: Exception) {
                emit(RequestState.Error("Message stream error: ${e.message}"))
            }
        }
    }

    override suspend fun getMessages(
        conversationId: String,
        limit: Int,
        before: String?
    ): RequestState<List<ChatMessage>> {
        return try {
            // Simplified implementation - get all messages for this conversation
            val allMessages = messagesCollection.get()

            val conversationMessages = allMessages.documents.mapNotNull { doc ->
                try {
                    val message = doc.data<FirestoreChatMessage>()
                    if (message.conversationId == conversationId) {
                        message.toDomainModel()
                    } else null
                } catch (e: Exception) {
                    println("Error parsing message: ${e.message}")
                    null
                }
            }.sortedBy { it.timestamp }.takeLast(limit)

            RequestState.Success(conversationMessages)
        } catch (e: Exception) {
            RequestState.Error("Failed to get messages: ${e.message}")
        }
    }

    override suspend fun markMessageAsRead(messageId: String, userId: String): RequestState<Unit> {
        return try {
            messagesCollection
                .document(messageId)
                .update(mapOf("isRead" to true))

            RequestState.Success(Unit)
        } catch (e: Exception) {
            RequestState.Error("Failed to mark message as read: ${e.message}")
        }
    }

    override suspend fun markConversationAsRead(
        conversationId: String,
        userId: String
    ): RequestState<Unit> {
        return try {
            // Sanitize userId for unreadCount access
            val sanitizedUserId = userId.replace(".", "_")
            // Reset unread count for this user
            val updateData = mapOf("unreadCount.$sanitizedUserId" to 0)
            conversationsCollection
                .document(conversationId)
                .update(updateData)

            RequestState.Success(Unit)
        } catch (e: Exception) {
            RequestState.Error("Failed to mark conversation as read: ${e.message}")
        }
    }

    override suspend fun updateTypingStatus(
        conversationId: String,
        userId: String,
        isTyping: Boolean
    ): RequestState<Unit> {
        return try {
            val typingId = "${conversationId}_$userId"

            if (isTyping) {
                val typingIndicator = FirestoreTypingIndicator(
                    conversationId = conversationId,
                    userId = userId,
                    isTyping = isTyping,
                    timestamp = getCurrentTimeMillis()
                )
                typingIndicatorsCollection
                    .document(typingId)
                    .set(typingIndicator)
            } else {
                typingIndicatorsCollection
                    .document(typingId)
                    .delete()
            }

            RequestState.Success(Unit)
        } catch (e: Exception) {
            RequestState.Error("Failed to update typing status: ${e.message}")
        }
    }

    override fun getTypingIndicators(conversationId: String): Flow<RequestState<List<TypingIndicator>>> {
        return flow {
            emit(RequestState.Loading)

            try {
                // Listen to real-time typing indicators for this conversation
                typingIndicatorsCollection.snapshots.collect { snapshot ->
                    try {
                        val indicators = snapshot.documents.mapNotNull { doc ->
                            try {
                                val firestoreIndicator = doc.data<FirestoreTypingIndicator>()

                                // Filter for this conversation and active typing indicators
                                if (firestoreIndicator.conversationId == conversationId &&
                                    firestoreIndicator.isTyping
                                ) {

                                    // Check if typing indicator is still valid (not expired)
                                    val currentTime = getCurrentTimeMillis()
                                    val typingTimeout = 5000L // 5 seconds timeout

                                    if (currentTime - firestoreIndicator.timestamp <= typingTimeout) {
                                        TypingIndicator(
                                            conversationId = firestoreIndicator.conversationId,
                                            userId = firestoreIndicator.userId,
                                            isTyping = firestoreIndicator.isTyping,
                                            timestamp = firestoreIndicator.timestamp
                                        )
                                    } else {
                                        // Clean up expired typing indicator
                                        try {
                                            doc.reference.delete()
                                        } catch (e: Exception) {
                                            println("Failed to delete expired typing indicator: ${e.message}")
                                        }
                                        null
                                    }
                                } else null
                            } catch (e: Exception) {
                                println("Error parsing typing indicator: ${e.message}")
                                null
                            }
                        }

                        emit(RequestState.Success(indicators))
                    } catch (e: Exception) {
                        emit(RequestState.Error("Failed to parse typing indicators: ${e.message}"))
                    }
                }
            } catch (e: Exception) {
                emit(RequestState.Error("Typing indicators stream error: ${e.message}"))
            }
        }
    }

    override suspend fun updateUserOnlineStatus(
        userId: String,
        isOnline: Boolean
    ): RequestState<Unit> {
        return try {
            println("🔄 Updating online status for user: $userId to $isOnline")

            // Check if userId is email (contains @) or Firebase ID
            val userEmail = if (userId.contains("@")) {
                // userId is already email
                println("📧 Using userId as email: $userId")
                userId
            } else {
                // userId is Firebase ID, need to get email
                val userDoc = usersCollection.document(userId).get()
                if (userDoc.exists) {
                    val user = userDoc.data<User>()
                    println("📧 Got email ${user.email} for user ID $userId")
                    user.email
                } else {
                    println("❌ User not found with ID: $userId")
                    return RequestState.Error("User not found")
                }
            }

            val userStatus = FirestoreUserStatus(
                userId = userEmail,
                isOnline = isOnline,
                lastSeen = if (!isOnline) getCurrentTimeMillis() else 0L
            )

            // Use email as document ID
            userStatusCollection
                .document(userEmail)
                .set(userStatus)

            println("✅ Online status updated successfully for email: $userEmail")

            RequestState.Success(Unit)
        } catch (e: Exception) {
            RequestState.Error("Failed to update user online status: ${e.message}")
        }
    }

    override suspend fun getUserOnlineStatus(userId: String): RequestState<Boolean> {
        return try {
            // First get the user's email using their ID
            val userDoc = usersCollection.document(userId).get()

            if (userDoc.exists) {
                val user = userDoc.data<User>()
                val userEmail = user.email

                println("📧 Got email $userEmail for user ID $userId")

                // Now get online status using email as document ID
                val statusDoc = userStatusCollection
                    .document(userEmail)
                    .get()

                val isOnline = if (statusDoc.exists) {
                    val status = statusDoc.data<FirestoreUserStatus>()
                    status.isOnline
                } else {
                    false
                }

                RequestState.Success(isOnline)
            } else {
                println("❌ User not found with ID: $userId")
                RequestState.Success(false) // User doesn't exist, consider offline
            }
        } catch (e: Exception) {
            RequestState.Error("Failed to get user online status: ${e.message}")
        }
    }

    override fun getUserOnlineStatusFlow(userId: String): Flow<RequestState<Boolean>> {
        return flow {
            emit(RequestState.Loading)

            try {
                // First get the user's email using their ID
                val userDoc = usersCollection.document(userId).get()

                if (userDoc.exists) {
                    val user = userDoc.data<User>()
                    val userEmail = user.email

                    println("📧 Got email $userEmail for user ID $userId")

                    // Now get online status using email as document ID
                    userStatusCollection.document(userEmail).snapshots.collect { snapshot ->
                        try {
                            val isOnline = if (snapshot.exists) {
                                val status = snapshot.data<FirestoreUserStatus>()
                                status.isOnline
                            } else {
                                false
                            }
                            emit(RequestState.Success(isOnline))
                        } catch (e: Exception) {
                            emit(RequestState.Error("Failed to parse user status: ${e.message}"))
                        }
                    }
                } else {
                    println("❌ User not found with ID: $userId")
                    emit(RequestState.Success(false)) // User doesn't exist, consider offline
                }
            } catch (e: Exception) {
                emit(RequestState.Error("User status stream error: ${e.message}"))
            }
        }
    }

    override suspend fun muteConversation(
        conversationId: String,
        userId: String
    ): RequestState<Unit> {
        return try {
            // Basic implementation - you'd need to implement array operations properly
            RequestState.Success(Unit)
        } catch (e: Exception) {
            RequestState.Error("Failed to mute conversation: ${e.message}")
        }
    }

    override suspend fun unmuteConversation(
        conversationId: String,
        userId: String
    ): RequestState<Unit> {
        return try {
            RequestState.Success(Unit)
        } catch (e: Exception) {
            RequestState.Error("Failed to unmute conversation: ${e.message}")
        }
    }

    override suspend fun pinConversation(
        conversationId: String,
        userId: String
    ): RequestState<Unit> {
        return try {
            RequestState.Success(Unit)
        } catch (e: Exception) {
            RequestState.Error("Failed to pin conversation: ${e.message}")
        }
    }

    override suspend fun unpinConversation(
        conversationId: String,
        userId: String
    ): RequestState<Unit> {
        return try {
            RequestState.Success(Unit)
        } catch (e: Exception) {
            RequestState.Error("Failed to unpin conversation: ${e.message}")
        }
    }

    override suspend fun deleteConversation(
        conversationId: String,
        userId: String
    ): RequestState<Unit> {
        return try {
            conversationsCollection
                .document(conversationId)
                .update(mapOf("isActive" to false))

            RequestState.Success(Unit)
        } catch (e: Exception) {
            RequestState.Error("Failed to delete conversation: ${e.message}")
        }
    }

    override suspend fun searchMessages(
        conversationId: String,
        query: String
    ): RequestState<List<ChatMessage>> {
        return try {
            val messagesResult = getMessages(conversationId, 1000)
            when (messagesResult) {
                is RequestState.Success -> {
                    val filteredMessages = messagesResult.getSuccessData().filter { message ->
                        message.content.contains(query, ignoreCase = true)
                    }
                    RequestState.Success(filteredMessages)
                }

                is RequestState.Error -> messagesResult
                else -> RequestState.Error("Failed to search messages")
            }
        } catch (e: Exception) {
            RequestState.Error("Failed to search messages: ${e.message}")
        }
    }

    override suspend fun getUserDetails(userId: String): RequestState<ChatParticipant> {
        return try {
            // Fetch user details with caching
            val participant = fetchUserDetailsWithCache(userId)
            if (participant != null) {
                RequestState.Success(participant)
            } else {
                RequestState.Error("User not found")
            }
        } catch (e: Exception) {
            RequestState.Error("Failed to get user details: ${e.message}")
        }
    }

    // Helper functions
    private fun generateConversationId(userId1: String, userId2: String): String {
        return listOf(userId1, userId2).sorted().joinToString("_")
    }

    private suspend fun fetchUserDetailsWithCache(userId: String): ChatParticipant? {
        val currentTime = getCurrentTimeMillis()

        // Check cache first
        userDetailsCache[userId]?.let { (cachedParticipant, cacheTime) ->
            if (currentTime - cacheTime < cacheExpirationMs) {
                println("📦 Using cached details for user: $userId")
                return cachedParticipant
            } else {
                println("🗑️ Cache expired for user: $userId")
            }
        }

        // Fetch from Firestore if not in cache or expired
        val participant = fetchUserDetails(userId)

        // Cache the result
        if (participant != null) {
            userDetailsCache[userId] = Pair(participant, currentTime)
            println("💾 Cached details for user: $userId")
        }

        return participant
    }

    // Helper method to fetch user details and convert to ChatParticipant
    suspend fun fetchUserDetails(userId: String): ChatParticipant? {
        return try {
            // Determine if userId is an email or a Firebase user ID
            val userEmail = if (userId.contains("@")) {
                // userId is already an email
                println("Using userId as email: $userId")
                userId.trim()
            } else {
                // userId is a Firebase ID, need to get the email first
                println("Getting email for user ID: $userId")
                val userDoc = usersCollection.document(userId).get()
                if (userDoc.exists) {
                    val user = userDoc.data<User>()
                    println("Found email ${user.email} for user ID $userId")
                    user.email
                } else {
                    println("No user found with ID: $userId")
                    return null
                }
            }

            // Now query by email to get the user document
            val userQuery = usersCollection
                .where { "email" equalTo userEmail }
                .get()

            if (userQuery.documents.isNotEmpty()) {
                val userDoc = userQuery.documents.first()
                val user = userDoc.data<User>()
                println("User details fetched for email $userEmail: $user")

                // Get user online status using the actual user ID
                val onlineStatus = getUserOnlineStatus(user.id)
                val isOnline = when (onlineStatus) {
                    is RequestState.Success -> onlineStatus.getSuccessData()
                    else -> false
                }

                // Get last seen if user is offline
                val lastSeen = if (!isOnline) {
                    try {
                        val statusDoc = userStatusCollection.document(userEmail).get()
                        if (statusDoc.exists) {
                            val status = statusDoc.data<FirestoreUserStatus>()
                            status.lastSeen
                        } else 0L
                    } catch (e: Exception) {
                        0L
                    }
                } else 0L

                println("Online status for user ${user.id}: $isOnline, Last seen: $lastSeen")

                val chatParticipant = ChatParticipant(
                    userId = user.id,
                    username = user.username,
                    fullName = user.fullname,
                    thumbnail = user.thumbnail,
                    isOnline = isOnline,
                    lastSeen = lastSeen,
                    isTyping = false // This will be updated separately via typing indicators
                )

                println("Chat Participant created: $chatParticipant")
                chatParticipant
            } else {
                println("No user found with email: $userEmail")
                null
            }
        } catch (e: Exception) {
            println("Error fetching user details for userId $userId: ${e.message}")
            null
        }
    }

    private fun FirestoreConversation.toDomainModel(): Conversation {
        val lastMessage = if (lastMessageContent.isNotEmpty()) {
            ChatMessage(
                id = "",
                conversationId = id,
                senderId = lastMessageSenderId,
                recipientId = "",
                content = lastMessageContent,
                timestamp = lastMessageTimestamp,
                messageType = MessageType.TEXT,
                status = MessageStatus.SENT,
                isRead = false
            )
        } else null

        return Conversation(
            id = id,
            participantIds = participantIds,
            lastMessage = lastMessage,
            lastActivity = lastActivity,
            createdAt = createdAt,
            isActive = isActive,
            conversationType = when (conversationType) {
                "DIRECT" -> ConversationType.DIRECT
                "GROUP" -> ConversationType.GROUP
                else -> ConversationType.DIRECT
            },
            unreadCount = unreadCount,
            mutedBy = mutedBy,
            pinnedBy = pinnedBy
        )
    }

    private fun FirestoreChatMessage.toDomainModel(): ChatMessage {
        return ChatMessage(
            id = id,
            conversationId = conversationId,
            senderId = senderId,
            recipientId = recipientId,
            content = content,
            timestamp = timestamp,
            messageType = when (messageType) {
                "TEXT" -> MessageType.TEXT
                "IMAGE" -> MessageType.IMAGE
                "FILE" -> MessageType.FILE
                "SYSTEM" -> MessageType.SYSTEM
                else -> MessageType.TEXT
            },
            status = when (status) {
                "SENDING" -> MessageStatus.SENDING
                "SENT" -> MessageStatus.SENT
                "DELIVERED" -> MessageStatus.DELIVERED
                "FAILED" -> MessageStatus.FAILED
                else -> MessageStatus.SENT
            },
            isRead = isRead,
            editedAt = editedAt,
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
    }
}