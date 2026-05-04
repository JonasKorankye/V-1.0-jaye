package com.flipverse.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.flipverse.chat.ChatDMScreen
import com.flipverse.chat.ConversationsScreen
import com.flipverse.chat.FlipChatScreen
import com.flipverse.chat.NewMessageScreen
import com.flipverse.chat.ViewProfileScreen
import com.flipverse.chat.domain.ChatRoutes
import com.flipverse.dashboard.PostDetailsScreen
import com.flipverse.livebook.domain.LiveBookRoutes
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

@Composable
fun ChatRoot(
    navigateToAuthController: NavController,
    deepLinkPath: String? = null,
    deepLinkParameters: Map<String, String> = emptyMap()
) {

    val navController = rememberNavController()
    var navigationAttempted by remember { mutableStateOf(false) }
    var requestedInitialTab by remember { mutableStateOf(0) }

    // Handle deep link navigation to specific screens
    LaunchedEffect(deepLinkPath, deepLinkParameters, navController) {
        println("💬 ChatRoot: LaunchedEffect triggered")
        println("💬 ChatRoot: deepLinkPath = $deepLinkPath")
        println("💬 ChatRoot: deepLinkParameters = $deepLinkParameters")
        println("💬 ChatRoot: navigationAttempted = $navigationAttempted")

        if (!navigationAttempted &&
            deepLinkPath == "/chat_dm" &&
            deepLinkParameters.containsKey("conversationId") &&
            deepLinkParameters.containsKey("otherUserId")
        ) {
            val conversationId = deepLinkParameters["conversationId"]!!
            val otherUserId = deepLinkParameters["otherUserId"]!!

            println("💬 ChatRoot: Deep link conditions met!")
            println("💬 ChatRoot: conversationId = $conversationId")
            println("💬 ChatRoot: otherUserId = $otherUserId")

            // Mark as attempted before delay to prevent re-entry
            navigationAttempted = true

            // Add a longer delay to ensure NavHost is completely ready
            println("💬 ChatRoot: Waiting for NavHost to initialize...")
            delay(500)
            println("💬 ChatRoot: Attempting navigation now...")

            try {
                navController.navigate(ChatRoutes.ChatConversation(conversationId, otherUserId)) {
                    launchSingleTop = true
                    // Also clear any existing chat screens from back stack
                    popUpTo(ChatRoutes.Chat) {
                        saveState = false
                    }
                }
                println("✅ ChatRoot: Navigation command executed successfully!")
            } catch (e: Exception) {
                println("❌ ChatRoot: Navigation failed with exception: ${e.message}")
                e.printStackTrace()
            }
        } else {
            println("💬 ChatRoot: Deep link conditions NOT met")
            if (navigationAttempted) println("   - Already attempted navigation")
            if (deepLinkPath != "/chat_dm") println("   - Wrong path: $deepLinkPath")
            if (!deepLinkParameters.containsKey("conversationId")) println("   - Missing conversationId")
            if (!deepLinkParameters.containsKey("otherUserId")) println("   - Missing otherUserId")
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primary // Main content background
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = ChatRoutes.Chat,
            enterTransition = { slideInVertically(tween(300), initialOffsetY = { it / 2 }) },
            exitTransition = { fadeOut(tween(300)) },
            popEnterTransition = { slideInVertically(tween(300), initialOffsetY = { it / 2 }) },
            popExitTransition = { fadeOut(tween(300)) }
        ) {
            composable<ChatRoutes.Chat> {
                LaunchedEffect(Unit) {
                    println(" ChatRoot: Composing ConversationsScreen with initialTab = $requestedInitialTab")
                }

                ConversationsScreen(
                    navigateToNewMessage = {
                        navController.navigate(ChatRoutes.ChatNewMessage)
                    },
                    navigateToConversation = { conversationId, otherUserId ->
                        navController.navigate(
                            ChatRoutes.ChatConversation(
                                conversationId,
                                otherUserId
                            )
                        )
                    },
                    navigateToUserProfile = {
                        navController.navigate(ChatRoutes.ChatUserProfile)
                    },
                    initialTab = requestedInitialTab
                )

                // Reset the tab after composing
                LaunchedEffect(Unit) {
                    delay(100) // Small delay to ensure screen is composed
                    if (requestedInitialTab != 0) {
                        println(" ChatRoot: Resetting requestedInitialTab from $requestedInitialTab to 0")
                        requestedInitialTab = 0
                    }
                }
            }

            composable<ChatRoutes.ChatUserProfile> {
                UserProfileScreen(
                    onNavigateBack = {
                        navController.navigateUp()
                    },
                    navigateToEditProfile = {
                        navController.navigate(ChatRoutes.ChatEditProfile)
                    },
                    navigateToAuthController = (navigateToAuthController),
                    navigateToViewProfile = { userEmail ->
                        navController.navigate(
                            ChatRoutes.ChatViewProfile(
                                userEmail
                            )
                        )
                    },
                    navigateToPrivacyAndSafety = { navController.navigate(ChatRoutes.ChatPrivacyAndSafety) },
                    navigateToYourAccount = { navController.navigate(ChatRoutes.ChatYourAccount) },
                    navigateToSupport = { navController.navigate(ChatRoutes.ChatSupport) },
                    navigateToAppTheme = { navController.navigate(ChatRoutes.ChatAppTheme) }
                )
            }

            composable<ChatRoutes.ChatEditProfile> {
                EditProfileScreen(
                    onNavigateBack = {
                        navController.navigateUp()
                    }
                )
            }


            composable<ChatRoutes.ChatYourAccount> {
                YourAccountScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToChangePassword = { navController.navigate(ChatRoutes.ChatChangePassword) },
                    navigateToAccountInformation = { navController.navigate(ChatRoutes.ChatAccountInformation) }
                )
            }

            composable<ChatRoutes.ChatPrivacyAndSafety> {
                PrivacyAndSafetyScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }

            composable<ChatRoutes.ChatSupport> {
                SupportScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }

            composable<ChatRoutes.ChatAppTheme> {
                AppThemeScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToFontSize = { navController.navigate(ChatRoutes.ChatFontSize) }
                )
            }

            composable<ChatRoutes.ChatFontSize> {
                FontSizeScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }

            composable<ChatRoutes.ChatChangePassword> {
                ChangePasswordScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToVerifyOTP = { email ->
                        navController.navigate(ChatRoutes.ChatVerifyPasswordResetOTP(email))
                    }
                )
            }

            composable<ChatRoutes.ChatNewMessage> {
                NewMessageScreen(
                    navigateBack = { navController.navigateUp() },
                    navigateToConversation = { conversationId, otherUserId ->
                        navController.navigate(
                            ChatRoutes.ChatConversation(
                                conversationId,
                                otherUserId
                            )
                        )
                    }
                )
            }

            composable<ChatRoutes.ChatConversation>(
                deepLinks = listOf(
                    navDeepLink<ChatRoutes.ChatConversation>(
                        "app://flipverse.com/chat_dm?conversationId={conversationId}&otherUserId={otherUserId}"
                    )
                )
            ) { backStackEntry ->

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
                    navigateBack = {
                        navController.navigate(ChatRoutes.Chat) {
                            popUpTo(ChatRoutes.Chat) { saveState = true }
                        }
                    },
                    navigateToViewProfile = {
                        navController.navigate(ChatRoutes.ChatViewProfile(route.otherUserId))
                    },
                    navigateToAvatarSelection = {
                        println("🎨 ChatRoot: navigateToAvatarSelection called - setting requestedInitialTab to 1")
                        requestedInitialTab = 1
                        println("🎨 ChatRoot: Navigating to Chat screen...")
                        navController.navigate(ChatRoutes.Chat) {
                            popUpTo(ChatRoutes.Chat) { inclusive = true }
                            launchSingleTop = true
                        }
                        println("🎨 ChatRoot: Navigation command executed")
                    }
                )
            }

            composable<ChatRoutes.ChatDirectMessage> {
                // This route is deprecated - redirect to conversations screen
                FlipChatScreen(
                    navigateToUserProfile = {
                        navController.navigate(ChatRoutes.ChatUserProfile)
                    },
                    navigateToNewMessage = {
                        navController.navigate(ChatRoutes.ChatNewMessage)
                    }
                )
            }

            composable<ChatRoutes.ChatViewProfile> { backStackEntry ->
                val route = backStackEntry.toRoute<ChatRoutes.ChatViewProfile>()

                ViewProfileScreen(
                    navigateBack = {
                        navController.navigateUp()
                    },
                    navigateToSendMessage = {
                        navController.navigate(ChatRoutes.ChatDirectMessage)
                    },
                    userId = route.userId,
                    navigateToPostDetails = { postId ->
                        navController.navigate(ChatRoutes.ChatPostDetails(postId))
                    },
                    navigateToViewLiveBook = { liveBookId ->
                        navController.navigate(ChatRoutes.ChatViewLiveBook(liveBookId))
                    }
                )
            }

            composable<ChatRoutes.ChatViewLiveBook>
            { backStackEntry ->
                val route = backStackEntry.toRoute<ChatRoutes.ChatViewLiveBook>()

                ViewLiveBookScreen(
                    liveBookId = route.liveBookId,
                    onBackClick = { navController.navigateUp() },
                    navigateToViewProfile = { id ->
                        navController.navigate(
                            ChatRoutes.ChatViewProfile(
                                id
                            )
                        )
                    },
                    navigateToContinueLiveBook = { navController.navigate(ChatRoutes.ChatContinueLiveBook(route.liveBookId))   }
                )
            }
            composable<ChatRoutes.ChatAccountInformation> {
                AccountInformationScreen(
                    onNavigateBack = { navController.navigateUp() },
                    navigateToAuthController = navigateToAuthController
                )
            }

            composable<ChatRoutes.ChatPostDetails> { backStackEntry ->
                val route = backStackEntry.toRoute<ChatRoutes.ChatPostDetails>()
                PostDetailsScreen(
                    postId = route.postId,
                    navigateBack = { navController.navigateUp() }
                )
            }

            composable<ChatRoutes.ChatVerifyPasswordResetOTP> { backStackEntry ->
                val route = backStackEntry.toRoute<ChatRoutes.ChatVerifyPasswordResetOTP>()
                VerifyPasswordResetScreen(
                    email = route.email,
                    onBackClicked = { navController.navigateUp() },
                    onVerificationSuccess = {
                        navController.navigate(ChatRoutes.ChatChangePassword) {
                            popUpTo<ChatRoutes.ChatVerifyPasswordResetOTP> { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
