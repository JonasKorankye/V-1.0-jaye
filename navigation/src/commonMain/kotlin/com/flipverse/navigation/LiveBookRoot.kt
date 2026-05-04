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
import com.flipverse.livebook.domain.LiveBookRoutes
import com.flipverse.livebook.screens.ContinueLiveBookScreen
import com.flipverse.livebook.screens.FlipLiveBookScreen
import com.flipverse.livebook.screens.NewLiveBookScreen
import com.flipverse.livebook.screens.ViewLiveBookScreen
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
fun LiveBookRoot(
    navigateToAuthController: NavController,
    deepLinkPath: String? = null,
    deepLinkParameters: Map<String, String> = emptyMap()
) {

    val navController = rememberNavController()
    var navigationAttempted by remember { mutableStateOf(false) }

    // Handle deep link navigation to specific screens
    LaunchedEffect(deepLinkPath, deepLinkParameters, navController) {
        if (!navigationAttempted &&
            deepLinkPath == "/view_livebook" &&
            deepLinkParameters.containsKey("liveBookId")
        ) {
            navigationAttempted = true

            val liveBookId = deepLinkParameters["liveBookId"]!!
            println("📖 LiveBookRoot: Navigating to ViewLiveBook with ID: $liveBookId")

            // Add a delay to ensure NavHost is ready
            delay(300)

            try {
                navController.navigate(LiveBookRoutes.ViewLiveBook(liveBookId)) {
                    launchSingleTop = true
                }
                println("📖 LiveBookRoot: Deep link navigation SUCCESSFUL")
            } catch (e: Exception) {
                println("❌ LiveBookRoot: Navigation failed: ${e.message}")
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primary // Main content background
    ) { paddingValues ->
        NavHost(
            navController = navController,
//            modifier = Modifier.padding(paddingValues),
            startDestination = LiveBookRoutes.LiveBook,
            enterTransition = { slideInVertically(tween(300), initialOffsetY = { it / 2 }) },
            exitTransition = { fadeOut(tween(300)) },
            popEnterTransition = { slideInVertically(tween(300), initialOffsetY = { it / 2 }) },
            popExitTransition = { fadeOut(tween(300)) }
        ) {
            composable<LiveBookRoutes.LiveBook> {
                FlipLiveBookScreen(
                    navigateToProfile = {
                        navController.navigate(LiveBookRoutes.LiveBookUserProfile)
                    },
                    navigateToNewLiveBookScreen = { navController.navigate(LiveBookRoutes.NewLiveBook) },
                    navigateToContinueLiveBookScreen = { navController.navigate(LiveBookRoutes.ContinueLiveBook()) },
                    navigateToViewParticipantScreen = {
                        navController.navigate(
                            LiveBookRoutes.ViewLiveBook(
                                it
                            )
                        )
                    },
                    navigateToTaggedUserProfile = {
                        navController.navigate(
                            LiveBookRoutes.ViewProfile(
                                it
                            )
                        )
                    }
                )
            }

            composable<LiveBookRoutes.NewLiveBook> {
                NewLiveBookScreen(
                    onClose = { navController.navigate(LiveBookRoutes.LiveBook) }
                )
            }

            composable<LiveBookRoutes.LiveBookUserProfile> {
                UserProfileScreen(
                    onNavigateBack = {
                        navController.navigateUp()
                    },
                    navigateToEditProfile = {
                        navController.navigate(LiveBookRoutes.LiveBookEditProfile)
                    },
                    navigateToAuthController = (navigateToAuthController),
                    navigateToViewProfile = { userEmail ->
                        navController.navigate(
                            LiveBookRoutes.ViewProfile(
                                userEmail
                            )
                        )
                    },
                    navigateToPrivacyAndSafety = { navController.navigate(LiveBookRoutes.LiveBookPrivacyAndSafety) },
                    navigateToYourAccount = { navController.navigate(LiveBookRoutes.LiveBookYourAccount) },
                    navigateToSupport = { navController.navigate(LiveBookRoutes.LiveBookSupport) },
                    navigateToAppTheme = { navController.navigate(LiveBookRoutes.LiveBookAppTheme) }
                )
            }

            composable<LiveBookRoutes.LiveBookEditProfile> {
                EditProfileScreen(
                    onNavigateBack = {
                        navController.navigateUp()
                    },
                )
            }


            composable<LiveBookRoutes.LiveBookYourAccount> {
                YourAccountScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToChangePassword = { navController.navigate(LiveBookRoutes.LiveBookChangePassword) },
                    navigateToAccountInformation = { navController.navigate(LiveBookRoutes.LiveBookAccountInformation) }
                )
            }

            composable<LiveBookRoutes.LiveBookPrivacyAndSafety> {
                PrivacyAndSafetyScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }

            composable<LiveBookRoutes.LiveBookSupport> {
                SupportScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }

            composable<LiveBookRoutes.LiveBookAppTheme> {
                AppThemeScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToFontSize = { navController.navigate(LiveBookRoutes.LiveBookFontSize) }
                )
            }

            composable<LiveBookRoutes.LiveBookFontSize> {
                FontSizeScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }

            composable<LiveBookRoutes.LiveBookChangePassword> {
                ChangePasswordScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToVerifyOTP = { email ->
                        navController.navigate(LiveBookRoutes.LiveBookVerifyPasswordResetOTP(email))
                    }
                )
            }

            composable<LiveBookRoutes.LiveBookVerifyPasswordResetOTP> { backStackEntry ->
                val route = backStackEntry.toRoute<LiveBookRoutes.LiveBookVerifyPasswordResetOTP>()
                VerifyPasswordResetScreen(
                    email = route.email,
                    onBackClicked = { navController.navigateUp() },
                    onVerificationSuccess = {
                        navController.navigate(LiveBookRoutes.LiveBookChangePassword) {
                            popUpTo<LiveBookRoutes.LiveBookVerifyPasswordResetOTP> {
                                inclusive = true
                            }
                        }
                    }
                )
            }

            composable<LiveBookRoutes.LiveBookAccountInformation> {
                AccountInformationScreen(
                    onNavigateBack = { navController.navigateUp() },
                    navigateToAuthController = navigateToAuthController
                )
            }

            composable<LiveBookRoutes.ContinueLiveBook> { backStackEntry ->
                val route = backStackEntry.toRoute<LiveBookRoutes.ContinueLiveBook>()
                val liveBookId = route.liveBookId
                ContinueLiveBookScreen(
                    onBackClick = { navController.navigateUp() },
                    onNavigateToOpenLiveBook = { navController.navigate(LiveBookRoutes.LiveBook) },
                    liveBookId = liveBookId
                )
            }

            composable<LiveBookRoutes.ViewLiveBook>(
                deepLinks = listOf(
                    navDeepLink<LiveBookRoutes.ViewLiveBook>(
                        "app://flipverse.com/view_livebook?liveBookId={liveBookId}"
                    )
                )
            )
            { backStackEntry ->
                val route = backStackEntry.toRoute<LiveBookRoutes.ViewLiveBook>()

                ViewLiveBookScreen(
                    liveBookId = route.liveBookId,
                    onBackClick = { navController.navigateUp() },
                    navigateToViewProfile = { id ->
                        navController.navigate(
                            LiveBookRoutes.ViewProfile(
                                id
                            )
                        )
                    },
                    navigateToContinueLiveBook = {
                        navController.navigate(
                            LiveBookRoutes.ContinueLiveBook(
                                route.liveBookId
                            )
                        )
                    }
                )
            }

            composable<LiveBookRoutes.ViewProfile> { backStackEntry ->
                val route = backStackEntry.toRoute<LiveBookRoutes.ViewProfile>()
                ViewProfileScreen(
                    userId = route.id,
                    navigateBack = { navController.navigateUp() },
                    navigateToSendMessage = {
                        navController.navigate(LiveBookRoutes.ChatDirectMessage(route.id))
                    },
                    navigateToPostDetails = { postId ->
                        navController.navigate(LiveBookRoutes.LiveBookPostDetails(postId))
                    },
                    navigateToViewLiveBook = { liveBookId ->
                        navController.navigate(LiveBookRoutes.ViewLiveBook(liveBookId))
                    }
                )
            }

            composable<LiveBookRoutes.ChatDirectMessage> { backStackEntry ->
                val route = backStackEntry.toRoute<LiveBookRoutes.ChatDirectMessage>()
                val chatViewModel: ChatViewModel = koinViewModel()

                // Create a conversation with the user and navigate to it
                LaunchedEffect(route.userId) {
                    chatViewModel.startConversationWith(route.userId) { conversationId ->
                        navController.navigate(
                            LiveBookRoutes.ChatConversation(conversationId, route.userId)
                        ) {
                            // Replace current destination to avoid back stack issues
                            popUpTo<LiveBookRoutes.ChatDirectMessage> { inclusive = true }
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

            composable<LiveBookRoutes.ChatConversation> { backStackEntry ->
                val route = backStackEntry.toRoute<LiveBookRoutes.ChatConversation>()

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
                        navController.navigate(LiveBookRoutes.ViewProfile(route.otherUserId))
                    },
                    navigateToAvatarSelection = null
                )
            }
        }
    }
}
