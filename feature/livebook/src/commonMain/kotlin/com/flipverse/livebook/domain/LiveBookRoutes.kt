package com.flipverse.livebook.domain

import kotlinx.serialization.Serializable

sealed interface LiveBookRoutes {
    @Serializable
    data object LiveBook: LiveBookRoutes

    @Serializable
    data object LiveBookUserProfile : LiveBookRoutes

    @Serializable
    data object LiveBookEditProfile : LiveBookRoutes

    @Serializable
    data object NewLiveBook: LiveBookRoutes

    @Serializable
    data object LiveBookYourAccount: LiveBookRoutes

    @Serializable
    data object LiveBookPrivacyAndSafety: LiveBookRoutes

    @Serializable
    data object LiveBookSupport: LiveBookRoutes

    @Serializable
    data object LiveBookAppTheme: LiveBookRoutes

    @Serializable
    data object LiveBookFontSize : LiveBookRoutes

    @Serializable
    data object LiveBookChangePassword : LiveBookRoutes

    @Serializable
    data class LiveBookVerifyPasswordResetOTP(val email: String) : LiveBookRoutes

    @Serializable
    data object LiveBookAccountInformation : LiveBookRoutes

    @Serializable
    data class ContinueLiveBook(val liveBookId: String? = null) : LiveBookRoutes

    @Serializable
    data class ViewLiveBook(val liveBookId: String) : LiveBookRoutes

    @Serializable
    data class ViewProfile(val id: String) : LiveBookRoutes

    @Serializable
    data class ChatDirectMessage(val userId: String) : LiveBookRoutes



    @Serializable
    data class LiveBookPostDetails(val postId: String) : LiveBookRoutes

    @Serializable
    data class ChatConversation(val conversationId: String, val otherUserId: String) :
        LiveBookRoutes

}