package com.flipverse.dashboard.domain

import kotlinx.serialization.Serializable

sealed interface DashboardRoutes {
    @Serializable
    data object Dashboard : DashboardRoutes

    @Serializable
    data class DashboardPostDetails(val postId: String) : DashboardRoutes

    @Serializable
    data class DashboardViewProfileDetails(val userId: String) : DashboardRoutes

    @Serializable
    data class DashboardChatDirectMessage(val userId: String) : DashboardRoutes

    @Serializable
    data class DashboardChatConversation(val conversationId: String, val otherUserId: String) :
        DashboardRoutes

    @Serializable
    data object DashboardUserProfile : DashboardRoutes

    @Serializable
    data object DashboardEditProfile : DashboardRoutes

    @Serializable
    data object DashboardRecommendation : DashboardRoutes

    @Serializable
    data object DashboardReview : DashboardRoutes

    @Serializable
    data class DashboardViewLiveBook(val liveBookId: String) : DashboardRoutes


    @Serializable
    data object DashboardQuote : DashboardRoutes

    @Serializable
    data object DashboardSeeAll : DashboardRoutes

    @Serializable
    data object DashboardYourAccount : DashboardRoutes

    @Serializable
    data object DashboardPrivacyAndSafety : DashboardRoutes

    @Serializable
    data object DashboardSearch : DashboardRoutes

    @Serializable
    data class DashboardContinueLiveBook(val liveBookId: String) : DashboardRoutes

    @Serializable
    data object DashboardSupport : DashboardRoutes

    @Serializable
    data object DashboardAppTheme : DashboardRoutes

    @Serializable
    data object DashboardFontSize : DashboardRoutes

    @Serializable
    data object DashboardChangePassword : DashboardRoutes

    @Serializable
    data object DashboardAccountInformation : DashboardRoutes

    @Serializable
    data class DashboardVerifyPasswordResetOTP(val email: String) : DashboardRoutes
}