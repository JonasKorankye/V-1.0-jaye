package com.flipverse.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.flipverse.chat.ChatDMScreen
import com.flipverse.chat.ChatViewModel
import com.flipverse.chat.ViewProfileScreen
import com.flipverse.chat.domain.ChatRoutes
import com.flipverse.dashboard.DashboardScreen
import com.flipverse.dashboard.DashboardSeeAllScreen
import com.flipverse.dashboard.PostDetailsScreen
import com.flipverse.dashboard.QuoteScreen
import com.flipverse.dashboard.RecommendationScreen
import com.flipverse.dashboard.ReviewScreen
import com.flipverse.dashboard.SearchScreen
import com.flipverse.dashboard.domain.DashboardRoutes
import com.flipverse.explore.domain.ExploreRoutes
import com.flipverse.livebook.screens.ViewLiveBookScreen
import com.flipverse.notification.domain.NotifyRoutes
import com.flipverse.shared.domain.ChatParticipant
import com.flipverse.userprofile.AccountInformationScreen
import com.flipverse.userprofile.AppThemeScreen
import com.flipverse.userprofile.ChangePasswordScreen
import com.flipverse.userprofile.EditProfileScreen
import com.flipverse.userprofile.FontSizeScreen
import com.flipverse.userprofile.PrivacyAndSafetyScreen
import com.flipverse.userprofile.SupportScreen
import com.flipverse.userprofile.UserProfileScreen
import com.flipverse.userprofile.VerifyPasswordResetScreen
import com.flipverse.userprofile.YourAccountScreen
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun DashboardRoot(
    navigateToAuthController: NavController,
    deepLinkPath: String? = null,
    deepLinkParameters: Map<String, String> = emptyMap(),
    onDoubleTapChanged: ((() -> Unit) -> Unit)? = null
) {

    var searchQuery by remember { mutableStateOf("") }

    var isFabMenuExpanded by remember { mutableStateOf(false) }

    val navController = rememberNavController()
    var navigationAttempted by remember { mutableStateOf(false) }

    // Handle deep link navigation to specific screens
    LaunchedEffect(deepLinkPath, deepLinkParameters, navController) {
        if (!navigationAttempted && deepLinkPath != null) {
            navigationAttempted = true

            // Add a delay to ensure NavHost is ready
            delay(300)

            try {
                when (deepLinkPath) {
                    "/post_details" -> {
                        if (deepLinkParameters.containsKey("postId")) {
                            val postId = deepLinkParameters["postId"]!!
                            println("📄 DashboardRoot: Navigating to PostDetails with ID: $postId")

                            navController.navigate(DashboardRoutes.DashboardPostDetails(postId)) {
                                launchSingleTop = true
                            }
                            println("📄 DashboardRoot: Deep link navigation SUCCESSFUL")
                        }
                    }

                    "/view_profile" -> {
                        if (deepLinkParameters.containsKey("userId")) {
                            val userId = deepLinkParameters["userId"]!!
                            println("👤 DashboardRoot: Navigating to ViewProfile with ID: $userId")

                            navController.navigate(
                                DashboardRoutes.DashboardViewProfileDetails(
                                    userId
                                )
                            ) {
                                launchSingleTop = true
                            }
                            println("👤 DashboardRoot: Deep link navigation SUCCESSFUL")
                        }
                    }

                    else -> {
                        println("🔍 DashboardRoot: Unknown deep link path: $deepLinkPath")
                    }
                }
            } catch (e: Exception) {
                println("❌ DashboardRoot: Navigation failed: ${e.message}")
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primary // Main content background
    ) { paddingValues ->
        NavHost(
//            modifier = Modifier.padding(paddingValues),
            navController = navController,
            startDestination = DashboardRoutes.Dashboard,
            enterTransition = { slideInVertically(tween(500), initialOffsetY = { it / 2 }) },
            exitTransition = { fadeOut(tween(500)) },
            popEnterTransition = { slideInVertically(tween(500), initialOffsetY = { it / 2 }) },
            popExitTransition = { fadeOut(tween(500)) }
        ) {
            composable<DashboardRoutes.Dashboard> {
                DashboardScreen(
                    navigateToProfile = {
                        navController.navigate(DashboardRoutes.DashboardUserProfile)
                    },
                    navigateToPostDetails = { postId ->
                        navController.navigate(DashboardRoutes.DashboardPostDetails(postId))
                    },
                    navigateToRecommendation = { navController.navigate(DashboardRoutes.DashboardRecommendation) },
                    navigateToReview = { navController.navigate(DashboardRoutes.DashboardReview) },
                    navigateToQuote = { navController.navigate(DashboardRoutes.DashboardQuote) },
                    navigateToSeeAllScreen = { navController.navigate(DashboardRoutes.DashboardSeeAll) },
                    navigateToSearch = { navController.navigate(DashboardRoutes.DashboardSearch) },
                    navigateToViewProfile = { userId ->
                        navController.navigate(
                            DashboardRoutes.DashboardViewProfileDetails(
                                userId
                            )
                        )
                    },
                    onDoubleTapChanged = onDoubleTapChanged
                )
            }

            composable<DashboardRoutes.DashboardPostDetails>(
                deepLinks = listOf(
                    navDeepLink<DashboardRoutes.DashboardPostDetails>(
                        "app://flipverse.com/post_details?postId={postId}"
                    )
                )
            )
            { backStackEntry ->
                val route = backStackEntry.toRoute<DashboardRoutes.DashboardPostDetails>()
                PostDetailsScreen(
                    postId = route.postId,
                    navigateBack = {
                        navController.navigateUp()
                    }
                )
            }

            composable<DashboardRoutes.DashboardViewProfileDetails>(
                deepLinks = listOf(
                    navDeepLink<DashboardRoutes.DashboardViewProfileDetails>(
                        "app://flipverse.com/view_profile?userId={userId}"
                    )
                )
            )
            { backStackEntry ->
                val route = backStackEntry.toRoute<DashboardRoutes.DashboardViewProfileDetails>()
                ViewProfileScreen(
                    userId = route.userId,
                    navigateBack = {
                        navController.navigateUp()
                    },
                    navigateToSendMessage = {
                        navController.navigate(DashboardRoutes.DashboardChatDirectMessage(route.userId))
                    },
                    navigateToPostDetails = { postId ->
                        navController.navigate(DashboardRoutes.DashboardPostDetails(postId))
                    },
                    navigateToViewLiveBook = { liveBookId ->
                        navController.navigate(DashboardRoutes.DashboardViewLiveBook(liveBookId))
                    }
                )
            }

            composable<DashboardRoutes.DashboardViewLiveBook> { backStackEntry ->
                val route = backStackEntry.toRoute<DashboardRoutes.DashboardViewLiveBook>()

                ViewLiveBookScreen(
                    liveBookId = route.liveBookId,
                    onBackClick = { navController.navigateUp() },
                    navigateToViewProfile = { id ->
                        navController.navigate(
                            DashboardRoutes.DashboardViewProfileDetails(
                                id
                            )
                        )
                    },
                    navigateToContinueLiveBook = { navController.navigate(DashboardRoutes.DashboardContinueLiveBook(route.liveBookId))  }
                )
            }

            composable<DashboardRoutes.DashboardChatDirectMessage> { backStackEntry ->
                val route = backStackEntry.toRoute<DashboardRoutes.DashboardChatDirectMessage>()
                val chatViewModel: ChatViewModel = koinViewModel()

                // Create a conversation with the user and navigate to it
                LaunchedEffect(route.userId) {
                    chatViewModel.startConversationWith(route.userId) { conversationId ->
                        navController.navigate(
                            DashboardRoutes.DashboardChatConversation(conversationId, route.userId)
                        ) {
                            // Replace current destination to avoid back stack issues
                            popUpTo<DashboardRoutes.DashboardChatDirectMessage> { inclusive = true }
                        }
                    }
                }

                // Show loading screen while creating conversation
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AdaptiveCircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            composable<DashboardRoutes.DashboardChatConversation>(
                deepLinks = listOf(
                    navDeepLink<DashboardRoutes.DashboardChatConversation>(
                        "app://flipverse.com/chat_dm?conversationId={conversationId}&otherUserId={otherUserId}"
                    )
                )
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<DashboardRoutes.DashboardChatConversation>()

                ChatDMScreen(
                    conversationId = route.conversationId,
                    otherParticipant = ChatParticipant(
                        userId = route.otherUserId,
                        username = "",
                        fullName = "",
                        thumbnail = "",
                        isOnline = false,
                        lastSeen = 0L,
                        isTyping = false
                    ),
                    navigateBack = { navController.navigateUp() },
                    navigateToViewProfile = {
                        navController.navigate(DashboardRoutes.DashboardViewProfileDetails(route.otherUserId))
                    },
                    navigateToAvatarSelection = null // Avatar selection not available from Dashboard context
                )
            }

            composable<DashboardRoutes.DashboardSeeAll> {
                DashboardSeeAllScreen(
                    onNavigateBack = {
                        navController.navigateUp()
                    },
                    navigateToViewProfile = { userId ->
                        navController.navigate(DashboardRoutes.DashboardViewProfileDetails(userId))
                    }
                )
            }

            composable<DashboardRoutes.DashboardSearch> {
                SearchScreen(
                    onNavigateBack = {
                        navController.navigateUp()
                    },
                    navigateToProfile = { navController.navigate(DashboardRoutes.DashboardUserProfile) },
                    navigateToSeeAllScreen = {
                        navController.navigate(DashboardRoutes.DashboardSeeAll)
                    },
                    navigateToPost = { postId ->
                        navController.navigate(DashboardRoutes.DashboardPostDetails(postId))
                    },
                    navigateToViewProfile = { userId ->
                        navController.navigate(DashboardRoutes.DashboardViewProfileDetails(userId))
                    }
                )
            }

            composable<DashboardRoutes.DashboardRecommendation> {
                RecommendationScreen(
                    navigateBack = {
                        navController.navigateUp()
                    },
                    navigateToDashboard = {
                        navController.navigate(DashboardRoutes.Dashboard)
                    }
                )
            }

            composable<DashboardRoutes.DashboardReview> {
                ReviewScreen(
                    navigateBack = {
                        navController.navigateUp()
                    },
                    navigateToDashboard = {
                        navController.navigate(DashboardRoutes.Dashboard)
                    }
                )
            }

            composable<DashboardRoutes.DashboardQuote> {
                QuoteScreen(
                    navigateBack = {
                        navController.navigateUp()
                    },
                    navigateToDashboard = {
                        navController.navigate(DashboardRoutes.Dashboard)
                    }
                )
            }

            composable<DashboardRoutes.DashboardUserProfile> {
                UserProfileScreen(
                    onNavigateBack = {
                        navController.navigateUp()
                    },
                    navigateToEditProfile = {
                        navController.navigate(DashboardRoutes.DashboardEditProfile)
                    },
                    navigateToAuthController = (navigateToAuthController),
                    navigateToViewProfile = { userId ->
                        navController.navigate(
                            DashboardRoutes.DashboardViewProfileDetails(
                                userId
                            )
                        )
                    },
                    navigateToPrivacyAndSafety = { navController.navigate(DashboardRoutes.DashboardPrivacyAndSafety) },
                    navigateToYourAccount = { navController.navigate(DashboardRoutes.DashboardYourAccount) },
                    navigateToSupport = {navController.navigate(DashboardRoutes.DashboardSupport)  },
                    navigateToAppTheme = {  navController.navigate(DashboardRoutes.DashboardAppTheme)}
                )
            }

            composable<DashboardRoutes.DashboardEditProfile> {
                EditProfileScreen(
                    onNavigateBack = {
                        navController.navigateUp()
                    },
                )
            }


            composable<DashboardRoutes.DashboardYourAccount> {
                YourAccountScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToChangePassword = { navController.navigate(DashboardRoutes.DashboardChangePassword) },
                    navigateToAccountInformation = { navController.navigate(DashboardRoutes.DashboardAccountInformation) }
                )
            }

            composable<DashboardRoutes.DashboardPrivacyAndSafety> {
                PrivacyAndSafetyScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }

            composable<DashboardRoutes.DashboardSupport> {
                SupportScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }

            composable<DashboardRoutes.DashboardAppTheme> {
                AppThemeScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToFontSize = { navController.navigate(DashboardRoutes.DashboardFontSize) }
                )
            }

            composable<DashboardRoutes.DashboardFontSize> {
                FontSizeScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }

            composable<DashboardRoutes.DashboardChangePassword> {
                ChangePasswordScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToVerifyOTP = { email ->
                        navController.navigate(DashboardRoutes.DashboardVerifyPasswordResetOTP(email))
                    }
                )
            }

            composable<DashboardRoutes.DashboardVerifyPasswordResetOTP> { backStackEntry ->
                val route =
                    backStackEntry.toRoute<DashboardRoutes.DashboardVerifyPasswordResetOTP>()
                VerifyPasswordResetScreen(
                    email = route.email,
                    onBackClicked = { navController.navigateUp() },
                    onVerificationSuccess = {
                        // Navigate back to change password screen after successful verification
                        // User can now use "Forgot Password" flow or set new password
                        navController.navigate(DashboardRoutes.DashboardChangePassword) {
                            popUpTo<DashboardRoutes.DashboardVerifyPasswordResetOTP> {
                                inclusive = true
                            }
                        }
                    }
                )
            }

            composable<DashboardRoutes.DashboardAccountInformation> {
                AccountInformationScreen(
                    onNavigateBack = { navController.navigateUp() },
                    navigateToAuthController = navigateToAuthController
                )
            }
        }
    }
}
