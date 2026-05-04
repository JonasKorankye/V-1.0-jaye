package com.flipverse.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.flipverse.chat.ChatDMScreen
import com.flipverse.chat.ChatViewModel
import com.flipverse.chat.FlipChatScreen
import com.flipverse.chat.ViewProfileScreen
import com.flipverse.chat.domain.ChatRoutes
import com.flipverse.dashboard.PostDetailsScreen
import com.flipverse.dashboard.domain.DashboardRoutes
import com.flipverse.livebook.domain.LiveBookRoutes
import com.flipverse.livebook.screens.ViewLiveBookScreen
import com.flipverse.notification.FlipNotifyScreen
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
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun NotifyRoot(
    navigateToAuthController: NavController,
    onDoubleTapChanged: ((suspend () -> Unit) -> Unit)? = null
) {

    val navController = rememberNavController()


    Scaffold(
//        topBar = {
//            TopAppBar(
//                modifier = Modifier.padding(top = 12.dp),
//                title = {
//                    Box(
//                        modifier = Modifier
//                            .fillMaxWidth(),
//                        contentAlignment = Alignment.CenterStart
//
//                    ) {
//                        Image(
//                            painter = painterResource(if (isSystemInDarkTheme()) Resources.Image.AppLogoDark else Resources.Image.AppLogoWhite),
//                            contentDescription = "App Logo",
//                            modifier = Modifier
//                                .wrapContentSize()
//                                .height(48.dp),
//
//                            )
//                    }
//                },
//                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
//            )
//        },
        containerColor = MaterialTheme.colorScheme.primary // Main content background
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = NotifyRoutes.Notification,
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it }) + fadeIn()
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -it }) + fadeIn()
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            }
        ) {
            composable<NotifyRoutes.Notification> {
                FlipNotifyScreen(
                    navigateToProfile = { userId ->
                        navController.navigate(NotifyRoutes.NotifyUserProfile(userId))
                    },
                    navigateToPost = { postId ->
                        navController.navigate(NotifyRoutes.NotifyPostDetails(postId))
                    },
                    navigateToLiveBook = { liveBookId ->
                        navController.navigate(NotifyRoutes.NotifyViewLiveBook(liveBookId))
                    },
                    onDoubleTapChanged = onDoubleTapChanged
                )
            }

            composable<NotifyRoutes.NotifyPostDetails> { backStackEntry ->
                val route = backStackEntry.toRoute<NotifyRoutes.NotifyPostDetails>()
                PostDetailsScreen(
                    postId = route.postId,
                    navigateBack = {
                        navController.navigateUp()
                    }
                )
            }

            composable<NotifyRoutes.NotifyViewLiveBook> { backStackEntry ->
                val route = backStackEntry.toRoute<NotifyRoutes.NotifyViewLiveBook>()

                ViewLiveBookScreen(
                    liveBookId = route.liveBookId,
                    onBackClick = { navController.navigateUp() },
                    navigateToViewProfile = { id ->
                        navController.navigate(
                            NotifyRoutes.NotifyViewProfile(
                                id
                            )
                        )
                    },
                    navigateToContinueLiveBook = { navController.navigate(NotifyRoutes.NotifyContinueLiveBook(route.liveBookId))  }
                )
            }

            composable<NotifyRoutes.NotifyViewProfile> { backStackEntry ->
                val route = backStackEntry.toRoute<LiveBookRoutes.ViewProfile>()
                ViewProfileScreen(
                    userId = route.id,
                    navigateBack = { navController.navigateUp() },
                    navigateToSendMessage = {
                        navController.navigate(NotifyRoutes.NotifyChatDirectMessage(route.id))
                    },
                    navigateToPostDetails = { postId ->
                        navController.navigate(NotifyRoutes.NotifyPostDetails(postId))
                    },
                    navigateToViewLiveBook = { liveBookId ->
                        navController.navigate(NotifyRoutes.NotifyViewLiveBook(liveBookId))
                    }
                )
            }
            composable<NotifyRoutes.NotifyChatDirectMessage> { backStackEntry ->
                val route = backStackEntry.toRoute<NotifyRoutes.NotifyChatDirectMessage>()
                val chatViewModel: ChatViewModel = koinViewModel()

                // Create a conversation with the user and navigate to it
                LaunchedEffect(route.userId) {
                    chatViewModel.startConversationWith(route.userId) { conversationId ->
                        navController.navigate(
                            NotifyRoutes.ChatConversation(conversationId, route.userId)
                        ) {
                            // Replace current destination to avoid back stack issues
                            popUpTo<NotifyRoutes.NotifyChatDirectMessage> { inclusive = true }
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

            composable<NotifyRoutes.ChatConversation> { backStackEntry ->

                val route = backStackEntry.toRoute<ChatRoutes.ChatConversation>()

                ChatDMScreen(
                    conversationId = route.conversationId,
                    otherParticipant = ChatParticipant(
                        userId = route.otherUserId,
                        username = "", // Will be loaded by the screen
                        fullName = "", // Will be loaded by the screen
                        thumbnail = "",
                        isOnline = false,
                        lastSeen = 0L,
                        isTyping = false
                    ),
                    navigateBack = { navController.navigateUp() },
                    navigateToViewProfile = {
                        navController.navigate(NotifyRoutes.NotifyViewProfile(route.otherUserId))
                    },
                    navigateToAvatarSelection = null // Avatar selection not available from Notify context
                )
            }


            composable<NotifyRoutes.NotifyUserProfile> { backStackEntry ->
                val route = backStackEntry.toRoute<NotifyRoutes.NotifyUserProfile>()
                UserProfileScreen(
                    onNavigateBack = {
                        navController.navigateUp()
                    },
                    navigateToEditProfile = {
                        navController.navigate(NotifyRoutes.NotifyEditProfile)
                    },
                    navigateToAuthController = (navigateToAuthController),
                    navigateToViewProfile = {
                        navController.navigate(
                            NotifyRoutes.NotifyViewProfile(
                                route.id
                            )
                        )
                    },
                    navigateToPrivacyAndSafety = { navController.navigate(NotifyRoutes.NotifyPrivacyAndSafety) },
                    navigateToYourAccount = { navController.navigate(NotifyRoutes.NotifyYourAccount) },
                    navigateToSupport = { navController.navigate(NotifyRoutes.NotifySupport) },
                    navigateToAppTheme = { navController.navigate(NotifyRoutes.NotifyAppTheme) }
                )
            }

            composable<NotifyRoutes.NotifyEditProfile> {
                EditProfileScreen(
                    onNavigateBack = {
                        navController.navigateUp()
                    },
                )
            }

            composable<NotifyRoutes.NotifyYourAccount> {
                YourAccountScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToChangePassword = { navController.navigate(NotifyRoutes.NotifyChangePassword) },
                    navigateToAccountInformation = { navController.navigate(NotifyRoutes.NotifyAccountInformation) }
                )
            }

            composable<NotifyRoutes.NotifyPrivacyAndSafety> {
                PrivacyAndSafetyScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }

            composable<NotifyRoutes.NotifySupport> {
                SupportScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }

            composable<NotifyRoutes.NotifyAppTheme> {
                AppThemeScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToFontSize = { navController.navigate(NotifyRoutes.NotifyFontSize) }
                )
            }

            composable<NotifyRoutes.NotifyFontSize> {
                FontSizeScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }

            composable<NotifyRoutes.NotifyChangePassword> {
                ChangePasswordScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToVerifyOTP = { email ->
                        navController.navigate(NotifyRoutes.NotifyVerifyPasswordResetOTP(email))
                    }
                )
            }

            composable<NotifyRoutes.NotifyVerifyPasswordResetOTP> { backStackEntry ->
                val route = backStackEntry.toRoute<NotifyRoutes.NotifyVerifyPasswordResetOTP>()
                VerifyPasswordResetScreen(
                    email = route.email,
                    onBackClicked = { navController.navigateUp() },
                    onVerificationSuccess = {
                        navController.navigate(NotifyRoutes.NotifyChangePassword) {
                            popUpTo<NotifyRoutes.NotifyVerifyPasswordResetOTP> { inclusive = true }
                        }
                    }
                )
            }

            composable<NotifyRoutes.NotifyAccountInformation> {
                AccountInformationScreen(
                    onNavigateBack = { navController.navigateUp() },
                    navigateToAuthController = navigateToAuthController
                )
            }
        }
    }
}
