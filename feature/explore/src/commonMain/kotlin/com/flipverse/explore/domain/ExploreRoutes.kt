package com.flipverse.explore.domain

import kotlinx.serialization.Serializable

sealed interface ExploreRoutes {
    @Serializable
    data object Explore : ExploreRoutes

    @Serializable
    data object ExploreUserProfile : ExploreRoutes

    @Serializable
    data object ExploreEditProfile : ExploreRoutes

    @Serializable
    data class ExploreContinueLiveBook(val liveBookId: String) : ExploreRoutes

    @Serializable
    data class ExploreBookDetails(val bookId: String)  : ExploreRoutes

    @Serializable
    data class ExploreBookReader(val bookId: String) : ExploreRoutes

    @Serializable
    data object ExploreCart : ExploreRoutes

    @Serializable
    data object ExploreYourAccount : ExploreRoutes

    @Serializable
    data object ExplorePrivacyAndSafety : ExploreRoutes

    @Serializable
    data object ExploreSupport : ExploreRoutes

    @Serializable
    data object ExploreAppTheme : ExploreRoutes

    @Serializable
    data object ExploreFontSize : ExploreRoutes

    @Serializable
    data object ExploreChangePassword : ExploreRoutes

    @Serializable
    data class ExploreVerifyPasswordResetOTP(val email: String) : ExploreRoutes

    @Serializable
    data object ExploreAccountInformation : ExploreRoutes

    @Serializable
    data class ExploreViewProfile(val userId: String) : ExploreRoutes

    @Serializable
    data object ExploreMainScreen : ExploreRoutes

    @Serializable
    data object DiscoverScreen : ExploreRoutes

    @Serializable
    data object ExploreCheckout : ExploreRoutes

    @Serializable
    data class ExplorePostDetails(val postId: String) : ExploreRoutes

    @Serializable
    data class ExploreViewLiveBook(val liveBookId: String) : ExploreRoutes

    @Serializable
    data class ExploreChatDirectMessage(val userId: String) : ExploreRoutes

}