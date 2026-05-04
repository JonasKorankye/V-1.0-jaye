package com.flipverse.shared.domain

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val recipientId: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val messageType: MessageType = MessageType.TEXT,
    val status: MessageStatus = MessageStatus.SENT,
    val isRead: Boolean = false,
    val editedAt: Long? = null,
    val replyToMessageId: String? = null,
    val attachmentUrl: String? = null,
    val attachmentType: AttachmentType? = null,
    val isFlipped: Boolean = false
)

@Serializable
data class Conversation(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val lastMessage: ChatMessage? = null,
    val lastActivity: Long = 0L,
    val createdAt: Long = 0L,
    val isActive: Boolean = true,
    val conversationType: ConversationType = ConversationType.DIRECT,
    val unreadCount: Map<String, Int> = emptyMap(), // userId to unread count
    val mutedBy: List<String> = emptyList(),
    val pinnedBy: List<String> = emptyList()
)

@Serializable
data class ChatParticipant(
    val userId: String = "",
    val username: String = "",
    val fullName: String = "",
    val thumbnail: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
    val isTyping: Boolean = false
)

@Serializable
enum class MessageType {
    TEXT,
    IMAGE,
    FILE,
    SYSTEM
}

@Serializable
enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    FAILED
}

@Serializable
enum class AttachmentType {
    IMAGE,
    DOCUMENT,
    VIDEO,
    AUDIO
}

@Serializable
enum class ConversationType {
    DIRECT,
    GROUP
}

@Serializable
data class TypingIndicator(
    val conversationId: String = "",
    val userId: String = "",
    val isTyping: Boolean = false,
    val timestamp: Long = 0L
)

@Serializable
data class MessageRead(
    val conversationId: String = "",
    val messageId: String = "",
    val userId: String = "",
    val readAt: Long = 0L
)

@Serializable
data class ConversationPreview(
    val conversation: Conversation,
    val otherParticipant: ChatParticipant? = null,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false
)