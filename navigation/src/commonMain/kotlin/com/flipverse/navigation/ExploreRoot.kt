package com.flipverse.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.flipverse.chat.ViewProfileScreen
import com.flipverse.chat.domain.ChatRoutes
import com.flipverse.dashboard.PostDetailsScreen
import com.flipverse.explore.BookDetailsScreen
import com.flipverse.explore.BookReaderScreen
import com.flipverse.explore.CartScreen
import com.flipverse.explore.CheckoutScreen
import com.flipverse.explore.FlipExploreScreen
import com.flipverse.explore.domain.ExploreRoutes
import com.flipverse.livebook.screens.ViewLiveBookScreen
import com.flipverse.notification.domain.NotifyRoutes
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


@Composable
fun ExploreRoot(
    navigateToAuthController: NavController
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
            startDestination = ExploreRoutes.Explore,
            enterTransition = { slideInVertically(tween(300), initialOffsetY = { it / 2 }) },
            exitTransition = { fadeOut(tween(300)) },
            popEnterTransition = { slideInVertically(tween(300), initialOffsetY = { it / 2 }) },
            popExitTransition = { fadeOut(tween(300)) }
        ) {
            composable<ExploreRoutes.Explore> {
                FlipExploreScreen(
                    navigateToProfile = {
                        navController.navigate(ExploreRoutes.ExploreUserProfile)
                    },
                    navigateToViewProfile = { userEmail ->
                        navController.navigate(
                            ExploreRoutes.ExploreViewProfile(
                                userEmail
                            )
                        )
                    },
                    navigateToBookDetails = { id -> navController.navigate(ExploreRoutes.ExploreBookDetails(id)) },
                    navigateToCart = { navController.navigate(ExploreRoutes.ExploreCart) },
                    onNavigateToBookReader = { id ->
                        navController.navigate(
                            ExploreRoutes.ExploreBookReader(
                                id
                            )
                        )
                    }
                )
            }

            composable<ExploreRoutes.ExploreUserProfile> {
                UserProfileScreen(
                    onNavigateBack = {
                        navController.navigateUp()
                    },
                    navigateToEditProfile = {
                        navController.navigate(ExploreRoutes.ExploreEditProfile)
                    },
                    navigateToAuthController = (navigateToAuthController),
                    navigateToViewProfile = { userEmail ->
                        navController.navigate(
                            ExploreRoutes.ExploreViewProfile(
                                userEmail
                            )
                        )
                    },
                    navigateToPrivacyAndSafety = { navController.navigate(ExploreRoutes.ExplorePrivacyAndSafety) },
                    navigateToYourAccount = { navController.navigate(ExploreRoutes.ExploreYourAccount) },
                    navigateToSupport = {
                        navController.navigate(
                            ExploreRoutes.ExploreSupport
                        )
                    },
                    navigateToAppTheme = { navController.navigate(ExploreRoutes.ExploreAppTheme) }
                )
            }

            composable<ExploreRoutes.ExploreViewProfile> { backStackEntry ->
                val route = backStackEntry.toRoute<ExploreRoutes.ExploreViewProfile>()
                ViewProfileScreen(
                    userId = route.userId,
                    navigateBack = { navController.navigateUp() },
                    navigateToSendMessage = {
                        navController.navigate(ExploreRoutes.ExploreChatDirectMessage(route.userId))
                    },
                    navigateToPostDetails = { postId ->
                        navController.navigate(ExploreRoutes.ExplorePostDetails(postId))
                    },
                    navigateToViewLiveBook = { liveBookId ->
                       navController.navigate(ExploreRoutes.ExploreViewLiveBook(liveBookId))
                    }
                )
            }


            composable<ExploreRoutes.ExploreViewLiveBook> { backStackEntry ->
                val route = backStackEntry.toRoute<ExploreRoutes.ExploreViewLiveBook>()

                ViewLiveBookScreen(
                    liveBookId = route.liveBookId,
                    onBackClick = { navController.navigateUp() },
                    navigateToViewProfile = { id ->
                        navController.navigate(
                            ExploreRoutes.ExploreViewProfile(
                                id
                            )
                        )
                    },
                    navigateToContinueLiveBook = { navController.navigate(ExploreRoutes.ExploreContinueLiveBook(route.liveBookId))  }
                )
            }

            composable<ExploreRoutes.ExplorePostDetails> { backStackEntry ->
                val route = backStackEntry.toRoute<ExploreRoutes.ExplorePostDetails>()
                PostDetailsScreen(
                    postId = route.postId,
                    navigateBack = { navController.navigateUp() }
                )
            }

            composable<ExploreRoutes.ExploreEditProfile> {
                EditProfileScreen(
                    onNavigateBack = {
                        navController.navigateUp()
                    },
                )
            }

            composable<ExploreRoutes.ExploreCart> {
                CartScreen(
                    onNavigateBack = {
                        navController.navigateUp()
                    },
                    onNavigateToCheckout = { navController.navigate(ExploreRoutes.ExploreCheckout)},
                )
            }

            composable < ExploreRoutes . ExploreCheckout > {
                CheckoutScreen(
                    onNavigateBack = {
                        navController.navigateUp()
                    },
                    onOpenAffiliateLink = {  },
                )
            }

            composable<ExploreRoutes.ExploreBookDetails> { backStackEntry ->
                val route = backStackEntry.toRoute<ExploreRoutes.ExploreBookDetails>()
                BookDetailsScreen(
                    onNavigateBack = {
                        navController.navigateUp()
                    },
                    bookId = route.bookId,
                    onNavigateToCart = { navController.navigate(ExploreRoutes.ExploreCart) },
                )
            }

            composable<ExploreRoutes.ExploreBookReader> { backStackEntry ->
                val route = backStackEntry.toRoute<ExploreRoutes.ExploreBookReader>()
                BookReaderScreen(
                    bookId = route.bookId,
                    onNavigateBack = {
                        navController.navigateUp()
                    }
                )
            }

            composable<ExploreRoutes.ExploreYourAccount> {
                YourAccountScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToChangePassword = { navController.navigate(ExploreRoutes.ExploreChangePassword) },
                    navigateToAccountInformation = { navController.navigate(ExploreRoutes.ExploreAccountInformation) }
                )
            }

            composable<ExploreRoutes.ExplorePrivacyAndSafety> {
                PrivacyAndSafetyScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }

            composable<ExploreRoutes.ExploreSupport> {
                SupportScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }

            composable<ExploreRoutes.ExploreAppTheme> {
                AppThemeScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToFontSize = { navController.navigate(ExploreRoutes.ExploreFontSize) }
                )
            }


            composable<ExploreRoutes.ExploreFontSize> {
                FontSizeScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }

            composable<ExploreRoutes.ExploreChangePassword> {
                ChangePasswordScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToVerifyOTP = { email ->
                        navController.navigate(ExploreRoutes.ExploreVerifyPasswordResetOTP(email))
                    }
                )
            }

            composable<ExploreRoutes.ExploreVerifyPasswordResetOTP> { backStackEntry ->
                val route = backStackEntry.toRoute<ExploreRoutes.ExploreVerifyPasswordResetOTP>()
                VerifyPasswordResetScreen(
                    email = route.email,
                    onBackClicked = { navController.navigateUp() },
                    onVerificationSuccess = {
                        navController.navigate(ExploreRoutes.ExploreChangePassword) {
                            popUpTo<ExploreRoutes.ExploreVerifyPasswordResetOTP> {
                                inclusive = true
                            }
                        }
                    }
                )
            }

            composable<ExploreRoutes.ExploreAccountInformation> {
                AccountInformationScreen(
                    onNavigateBack = { navController.navigateUp() },
                    navigateToAuthController = navigateToAuthController
                )
            }
        }
    }
}
