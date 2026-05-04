package com.flipverse.notification.domain

import kotlinx.serialization.Serializable

sealed interface NotifyRoutes {
    @Serializable
    data object Notification : NotifyRoutes

    @Serializable
    data class NotifyUserProfile(val id: String) : NotifyRoutes

    @Serializable
    data class NotifyViewProfile(val id: String) : NotifyRoutes

    @Serializable
    data class NotifyViewLiveBook(val liveBookId: String) : NotifyRoutes

    @Serializable
    data class NotifyPostDetails(val postId: String) : NotifyRoutes

    @Serializable
    data class NotifyChatDirectMessage(val userId: String) : NotifyRoutes

    @Serializable
    data class ChatConversation(val conversationId: String, val otherUserId: String) :
        NotifyRoutes

    @Serializable
    data object NotifyEditProfile : NotifyRoutes

    @Serializable
    data class NotifyContinueLiveBook(val liveBookId: String) : NotifyRoutes

    @Serializable
    data object NotifyPrivacyAndSafety : NotifyRoutes

    @Serializable
    data object NotifyYourAccount : NotifyRoutes

    @Serializable
    data object NotifySupport : NotifyRoutes

    @Serializable
    data object NotifyAppTheme : NotifyRoutes

    @Serializable
    data object NotifyFontSize : NotifyRoutes

    @Serializable
    data object NotifyChangePassword : NotifyRoutes

    @Serializable
    data class NotifyVerifyPasswordResetOTP(val email: String) : NotifyRoutes

    @Serializable
    data object NotifyAccountInformation : NotifyRoutes

}