package com.flipverse.chat.domain

import com.flipverse.livebook.domain.LiveBookRoutes
import kotlinx.serialization.Serializable

sealed interface ChatRoutes {
    @Serializable
    data object Chat : ChatRoutes

    @Serializable
    data object ChatUserProfile  : ChatRoutes

    @Serializable
    data object ChatNewMessage : ChatRoutes

    @Serializable
    data object ChatDirectMessage : ChatRoutes

    @Serializable
    data class ChatConversation(val conversationId: String, val otherUserId: String) : ChatRoutes

    @Serializable
    data class ChatViewProfile(val userId: String) : ChatRoutes

    @Serializable
    data object ChatEditProfile : ChatRoutes

    @Serializable
    data object ChatYourAccount : ChatRoutes

    @Serializable
    data object ChatPrivacyAndSafety : ChatRoutes

    @Serializable
    data object ChatSupport : ChatRoutes

    @Serializable
    data object ChatAppTheme : ChatRoutes

    @Serializable
    data object ChatFontSize : ChatRoutes

    @Serializable
    data object ChatChangePassword : ChatRoutes

    @Serializable
    data class ChatVerifyPasswordResetOTP(val email: String) : ChatRoutes

    @Serializable
    data object ChatAccountInformation : ChatRoutes

    @Serializable
    data class ChatContinueLiveBook(val liveBookId: String) : ChatRoutes

    @Serializable
    data class ChatAvatarSelection(val currentAvatarUrl: String? = null) : ChatRoutes

    @Serializable
    data class ChatPostDetails(val postId: String) : ChatRoutes

    @Serializable
    data class ChatViewLiveBook(val liveBookId: String) : ChatRoutes
}