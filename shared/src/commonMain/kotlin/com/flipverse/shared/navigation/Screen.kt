package com.flipverse.shared.navigation

import kotlinx.serialization.Serializable


@Serializable
sealed class Screen {
    @Serializable
    data object ConsentGate : Screen()

    @Serializable
    data object Auth: Screen()

    @Serializable
    data object LoginWithPassword : Screen()

    @Serializable
    data object ForgotPassword : Screen()

    @Serializable
    data class ForgotPasswordVerify(val email: String) : Screen()

    @Serializable
    data class ForgotPasswordReset(val email: String) : Screen()

    @Serializable
    data object SignUpWithEmail : Screen()

    @Serializable
    data object SignUpWithPassword : Screen()

    @Serializable
    data object CreateProfile : Screen()

    @Serializable
    data object CreateUsername : Screen()

    @Serializable
    data object ChooseInterest : Screen()

    @Serializable
    data object Suggestions : Screen()

    @Serializable
    data object VerifyEmail : Screen()

    @Serializable
    data object UserProfile : Screen()

    @Serializable
    data object NewMessage : Screen()

    @Serializable
    data object MainDashboard : Screen()
}


@Serializable
sealed interface BottomBarScreens
{
    @Serializable
    data object FlipHome : Screen()

    @Serializable
    data object FlipExplore : Screen()

    @Serializable
    data object FlipLiveBook : Screen()

    @Serializable
    data object FlipChat : Screen()

    @Serializable
    data object FlipNotify : Screen()
}


@Serializable
sealed interface MainDashboardRoutes {
    @Serializable
    data object FlipHomePages: MainDashboardRoutes

    @Serializable
    data object  FlipExplorePages: MainDashboardRoutes

    @Serializable
    data object FlipLiveBookPages: MainDashboardRoutes

    @Serializable
    data object FlipChatPages: MainDashboardRoutes

    @Serializable
    data object FlipNotifyPages: MainDashboardRoutes

    companion object {
        val allRoutes: List<MainDashboardRoutes> = listOf(
            FlipHomePages,
            FlipExplorePages,
            FlipLiveBookPages,
            FlipChatPages,
            FlipNotifyPages,
        )
    }
}
