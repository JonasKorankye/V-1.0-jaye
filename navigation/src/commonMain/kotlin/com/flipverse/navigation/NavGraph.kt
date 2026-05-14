package com.flipverse.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.flipverse.auth.screens.AuthScreen
import com.flipverse.auth.screens.ChooseInterestsScreen
import com.flipverse.auth.screens.ConsentGateScreen
import com.flipverse.auth.screens.CreateProfileScreen
import com.flipverse.auth.screens.CreateUsernameScreen
import com.flipverse.auth.screens.LoginWithPasswordScreen
import com.flipverse.auth.screens.SignUpWithEmailScreen
import com.flipverse.auth.screens.SignUpWithPasswordScreen
import com.flipverse.auth.screens.SuggestionScreen
import com.flipverse.auth.screens.VerifyEmailScreen
import com.flipverse.userprofile.ChangePasswordScreen
import com.flipverse.userprofile.ResetPasswordScreen
import com.flipverse.userprofile.VerifyPasswordResetScreen
import com.flipverse.shared.domain.BottomBarDestination
import com.flipverse.shared.navigation.MainDashboardRoutes
import com.flipverse.shared.navigation.Screen
import com.flipverse.shared.presentation.component.AnimatedFlipLiveBookIcon
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.painterResource

@Composable
fun SetupNavGraph(
    startDestination: Any = Screen.Auth,
    deepLinkRoute: MainDashboardRoutes? = null,
    deepLinkPath: String? = null,
    deepLinkParameters: Map<String, String> = emptyMap()
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<Screen.ConsentGate> {
            ConsentGateScreen(
                onAgree = {
                    navController.navigate(Screen.Auth) {
                        popUpTo<Screen.ConsentGate> {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable<Screen.Auth> {
            AuthScreen(
                navigateToDashboard = {
                    navController.navigate(MainDashboardRoutes.FlipHomePages) {
                        popUpTo<Screen.Auth> {
                            inclusive = true
                        }
                    }
                },
                navigateToLogin = {
                    navController.navigate(Screen.LoginWithPassword) {
                        popUpTo<Screen.Auth> {
                            inclusive = true
                        }
                    }
                },
                navigateToCreateProfile = {
                    navController.navigate(Screen.CreateProfile) {
                        popUpTo<Screen.Auth> {
                            inclusive = true
                        }
                    }                }
            )
        }
        composable<Screen.SignUpWithEmail> {
            SignUpWithEmailScreen(
                onBackClicked = {
                    navController.navigateUp()
                },
                navigateToVerifyEmail = {
                    navController.navigate(Screen.VerifyEmail) {
                        popUpTo<Screen.Auth> {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable<Screen.LoginWithPassword> {
            LoginWithPasswordScreen(
                onBackClicked = {
                    navController.navigate(Screen.Auth) {
                        popUpTo<Screen.Auth> {
                            inclusive = true
                        }
                    }
                },
                navigateToSignUpWithEmail = {
                    navController.navigate(Screen.SignUpWithEmail) {
                        popUpTo<Screen.Auth> {
                            inclusive = true
                        }
                    }
                },
                navigateToDashboard = {
                    navController.navigate(MainDashboardRoutes.FlipHomePages) {
                        popUpTo<Screen.Auth> {
                            inclusive = true
                        }
                    }
                },
                navigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword)
                }
            )
        }
        composable<Screen.ForgotPassword> {
            ChangePasswordScreen(
                onNavigateBack = {
                    navController.navigateUp()
                },
                onNavigateToVerifyOTP = { email ->
                    navController.navigate(Screen.ForgotPasswordVerify(email))
                },
                initialIsChangePasswordMode = false,
                showModeToggle = false
            )
        }
        composable<Screen.ForgotPasswordVerify> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.ForgotPasswordVerify>()
            VerifyPasswordResetScreen(
                email = route.email,
                onBackClicked = {
                    navController.navigateUp()
                },
                onVerificationSuccess = {
                    navController.navigate(Screen.ForgotPasswordReset(route.email)) {
                        popUpTo<Screen.ForgotPasswordVerify> {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable<Screen.ForgotPasswordReset> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.ForgotPasswordReset>()
            ResetPasswordScreen(
                email = route.email,
                onBackClicked = {
                    navController.navigateUp()
                },
                onResetSuccess = {
                    navController.navigate(Screen.LoginWithPassword) {
                        popUpTo<Screen.Auth> {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable<Screen.CreateProfile> {
            CreateProfileScreen(
                navigateToCreateUsername = {
                    navController.navigate(Screen.CreateUsername) {
                        popUpTo<Screen.Auth> {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable<Screen.CreateUsername> {
            CreateUsernameScreen(
                navigateToChooseInterests = {
                    navController.navigate(Screen.ChooseInterest) {
                        popUpTo<Screen.Auth> {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable<Screen.ChooseInterest> {
            ChooseInterestsScreen(
                onBackClicked = { navController.navigateUp() },
                navigateToSuggestions = {
                    navController.navigate(Screen.Suggestions) {
                        popUpTo<Screen.Auth> {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable<Screen.Suggestions> {
            SuggestionScreen(
                navigateToDashboard = {
                    navController.navigate(MainDashboardRoutes.FlipHomePages) {
                        popUpTo<Screen.Auth> {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable<Screen.VerifyEmail> {
            VerifyEmailScreen(
                onBackClicked = {
                    navController.navigateUp()
                },
                navigateToSignUpWithPassword = {
                    navController.navigate(Screen.SignUpWithPassword) {
                        popUpTo<Screen.Auth> {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable<Screen.SignUpWithPassword> {
            SignUpWithPasswordScreen(
                onBackClicked = {
                    navController.navigateUp()
                },
                navigateToCreateProfile = {
                    navController.navigate(Screen.CreateProfile) {
                        popUpTo<Screen.Auth> {
                            inclusive = true
                        }
                    }
                }
            )
        }


        composable<MainDashboardRoutes.FlipHomePages> {
            MainDashboardBottomBar(navController, deepLinkRoute, deepLinkPath, deepLinkParameters)
        }
    }
}

@Composable
fun MainDashboardBottomBar(
    navController: NavController,
    initialDeepLinkRoute: MainDashboardRoutes? = null,
    deepLinkPath: String? = null,
    deepLinkParameters: Map<String, String> = emptyMap()
) {

    val bottomBarNavController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    // Set initial route based on deep link or default to home
    var currentRoute: MainDashboardRoutes by remember {
        mutableStateOf(initialDeepLinkRoute ?: MainDashboardRoutes.FlipHomePages)
    }

    // Track last tap time for double-tap detection
    var lastTapTime by remember { mutableStateOf(0L) }
    var onDoubleTapHome by remember { mutableStateOf<(() -> Unit)?>(null) }
    var onDoubleTapNotifications by remember { mutableStateOf<(suspend () -> Unit)?>(null) }

    // The NavHost startDestination now handles the initial deep link navigation
    LaunchedEffect(initialDeepLinkRoute) {
        if (initialDeepLinkRoute != null) {
            println(" MainDashboardBottomBar: Deep link route received: $initialDeepLinkRoute")
            println(" MainDashboardBottomBar: Deep link path: $deepLinkPath")
            println(" MainDashboardBottomBar: Deep link parameters: $deepLinkParameters")
            currentRoute = initialDeepLinkRoute
        } else {
            println(" MainDashboardBottomBar: No deep link, starting at home")
        }
    }

    // React to changes in deep link parameters for dynamic navigation
    LaunchedEffect(initialDeepLinkRoute, deepLinkPath, deepLinkParameters) {
        if (initialDeepLinkRoute != null) {
            println(" Deep link parameters changed - navigating to: $initialDeepLinkRoute")
            currentRoute = initialDeepLinkRoute

            // Navigate to the route
            bottomBarNavController.navigate(initialDeepLinkRoute) {
                launchSingleTop = true
                popUpTo(MainDashboardRoutes.FlipHomePages) {
                    saveState = true
                    inclusive = false
                }
                restoreState = true
            }
        }
    }

    val navigator = { route: MainDashboardRoutes ->
        println("CurrentRoute::$currentRoute")
        println("route::$route")

        if (currentRoute == route) {
            // Tapping the tab we're already on
            val currentTime = Clock.System.now().toEpochMilliseconds()
            val isDoubleTap = currentTime - lastTapTime < 300
            lastTapTime = currentTime

            if (isDoubleTap) {
                // Double-tap: trigger scroll-to-top / refresh callbacks
                if (route == MainDashboardRoutes.FlipHomePages) {
                    println(" Double-tap detected on Home button - scrolling to top and refreshing")
                    onDoubleTapHome?.invoke()
                } else if (route == MainDashboardRoutes.FlipNotifyPages) {
                    println(" Double-tap detected on Notifications button - scrolling to top and refreshing")
                    coroutineScope.launch {
                        onDoubleTapNotifications?.invoke()
                    }
                }
            } else {
                // Single tap on current tab: pop inner navigation back to the tab root
                // This handles the case where the user is deep inside a tab (e.g. profile screen)
                // and taps the tab icon to go back to the root of that tab
                bottomBarNavController.navigate(route) {
                    popUpTo(MainDashboardRoutes.FlipHomePages) {
                        inclusive = false
                        saveState = false
                    }
                    launchSingleTop = false
                    restoreState = false
                }
            }
        } else {
            currentRoute = route
            bottomBarNavController.navigate(route) {
                launchSingleTop = true
                popUpTo(MainDashboardRoutes.FlipHomePages)
                {
                    saveState = true
                    inclusive = false
                }
                restoreState = true
            }
        }
    }



    Scaffold(
        contentWindowInsets = WindowInsets(top = 0.dp),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                MainDashboardRoutes.allRoutes.forEach { route ->
                    NavigationBarItem(
                        selected = currentRoute == route,
                        onClick = { navigator(route) },
                        icon = {
                            if (route == MainDashboardRoutes.FlipNotifyPages) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = if (route == MainDashboardRoutes.FlipNotifyPages) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else Color.Transparent,
                                            modifier = Modifier.size(6.dp)
                                        )
                                    }
                                ) {
                                    Icon(
                                        painterResource(
                                            when (route) {
                                                MainDashboardRoutes.FlipHomePages -> BottomBarDestination.FlipHome.icon
                                                MainDashboardRoutes.FlipExplorePages -> BottomBarDestination.FlipExplore.icon
                                                MainDashboardRoutes.FlipLiveBookPages -> BottomBarDestination.FlipLiveBook.icon
                                                MainDashboardRoutes.FlipChatPages -> BottomBarDestination.FlipChat.icon
                                                MainDashboardRoutes.FlipNotifyPages -> BottomBarDestination.FlipNotify.icon
                                                else -> BottomBarDestination.FlipHome.icon
                                            }
                                        ),
                                        contentDescription = when (route) {
                                            MainDashboardRoutes.FlipHomePages -> BottomBarDestination.FlipHome.title
                                            MainDashboardRoutes.FlipExplorePages -> BottomBarDestination.FlipExplore.title
                                            MainDashboardRoutes.FlipLiveBookPages -> BottomBarDestination.FlipLiveBook.title
                                            MainDashboardRoutes.FlipChatPages -> BottomBarDestination.FlipChat.title
                                            MainDashboardRoutes.FlipNotifyPages -> BottomBarDestination.FlipNotify.title
                                            else -> BottomBarDestination.FlipHome.title
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else if (route == MainDashboardRoutes.FlipLiveBookPages) {
                                // Animated FlipLiveBook icon with pulsing/glowing effect
                                AnimatedFlipLiveBookIcon(
                                    icon = BottomBarDestination.FlipLiveBook.icon,
                                    contentDescription = BottomBarDestination.FlipLiveBook.title,
                                    size = 24.dp,
                                    tint = if (currentRoute == route) Color.White else Color.Gray
                                )
                            } else {
                                Icon(
                                    painterResource(
                                        when (route) {
                                            MainDashboardRoutes.FlipHomePages -> BottomBarDestination.FlipHome.icon
                                            MainDashboardRoutes.FlipExplorePages -> BottomBarDestination.FlipExplore.icon
                                            MainDashboardRoutes.FlipLiveBookPages -> BottomBarDestination.FlipLiveBook.icon
                                            MainDashboardRoutes.FlipChatPages -> BottomBarDestination.FlipChat.icon
                                            MainDashboardRoutes.FlipNotifyPages -> BottomBarDestination.FlipNotify.icon
                                            else -> BottomBarDestination.FlipHome.icon
                                        }
                                    ),
                                    contentDescription = when (route) {
                                        MainDashboardRoutes.FlipHomePages -> BottomBarDestination.FlipHome.title
                                        MainDashboardRoutes.FlipExplorePages -> BottomBarDestination.FlipExplore.title
                                        MainDashboardRoutes.FlipLiveBookPages -> BottomBarDestination.FlipLiveBook.title
                                        MainDashboardRoutes.FlipChatPages -> BottomBarDestination.FlipChat.title
                                        MainDashboardRoutes.FlipNotifyPages -> BottomBarDestination.FlipNotify.title
                                        else -> BottomBarDestination.FlipHome.title
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                when (route) {
                                    MainDashboardRoutes.FlipHomePages -> BottomBarDestination.FlipHome.title
                                    MainDashboardRoutes.FlipExplorePages -> BottomBarDestination.FlipExplore.title
                                    MainDashboardRoutes.FlipLiveBookPages -> BottomBarDestination.FlipLiveBook.title
                                    MainDashboardRoutes.FlipChatPages -> BottomBarDestination.FlipChat.title
                                    MainDashboardRoutes.FlipNotifyPages -> BottomBarDestination.FlipNotify.title
                                    else -> BottomBarDestination.FlipHome.title
                                },
                                fontSize = 12.dp.value.sp,
                                )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            unselectedIconColor = Color.Gray,
                            indicatorColor = Color.Black,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }
    ) { paddingValues ->

        NavHost(
            navController = bottomBarNavController,
            startDestination = initialDeepLinkRoute ?: MainDashboardRoutes.FlipHomePages,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {

            composable<MainDashboardRoutes.FlipHomePages> {
                currentRoute = MainDashboardRoutes.FlipHomePages

                // Pass deep link info if this route matches the initial deep link route
                val shouldPassDeepLink =
                    MainDashboardRoutes.FlipHomePages == initialDeepLinkRoute &&
                        (deepLinkPath == "/post_details" || deepLinkPath == "/view_profile")

                val pathToPass = if (shouldPassDeepLink) deepLinkPath else null
                val paramsToPass = if (shouldPassDeepLink) deepLinkParameters else emptyMap()

                println(" MainDashboardBottomBar: FlipHomePages composable:")
                println("   - initialDeepLinkRoute: $initialDeepLinkRoute")
                println("   - shouldPassDeepLink: $shouldPassDeepLink")
                println("   - pathToPass: $pathToPass")
                println("   - paramsToPass: $paramsToPass")

                DashboardRoot(
                    navigateToAuthController = navController,
                    deepLinkPath = pathToPass,
                    deepLinkParameters = paramsToPass,
                    onDoubleTapChanged = { callback -> onDoubleTapHome = callback }
                )
            }

            composable<MainDashboardRoutes.FlipExplorePages> {
                currentRoute = MainDashboardRoutes.FlipExplorePages

                ExploreRoot(navigateToAuthController = navController)
            }

            composable<MainDashboardRoutes.FlipLiveBookPages> {
                currentRoute = MainDashboardRoutes.FlipLiveBookPages

                // Pass deep link info if this route matches the initial deep link route
                val shouldPassDeepLink =
                    MainDashboardRoutes.FlipLiveBookPages == initialDeepLinkRoute && deepLinkPath == "/view_livebook"
                val pathToPass = if (shouldPassDeepLink) deepLinkPath else null
                val paramsToPass = if (shouldPassDeepLink) deepLinkParameters else emptyMap()

                println(" MainDashboardBottomBar: FlipLiveBookPages composable:")
                println("   - initialDeepLinkRoute: $initialDeepLinkRoute")
                println("   - shouldPassDeepLink: $shouldPassDeepLink")
                println("   - pathToPass: $pathToPass")
                println("   - paramsToPass: $paramsToPass")

                LiveBookRoot(navController, pathToPass, paramsToPass)
            }

            composable<MainDashboardRoutes.FlipChatPages> {
                currentRoute = MainDashboardRoutes.FlipChatPages

                // Pass deep link info if this route matches the initial deep link route
                val shouldPassDeepLink =
                    MainDashboardRoutes.FlipChatPages == initialDeepLinkRoute && deepLinkPath == "/chat_dm"
                val pathToPass = if (shouldPassDeepLink) deepLinkPath else null
                val paramsToPass = if (shouldPassDeepLink) deepLinkParameters else emptyMap()

                println(" MainDashboardBottomBar: FlipChatPages composable:")
                println("   - initialDeepLinkRoute: $initialDeepLinkRoute")
                println("   - shouldPassDeepLink: $shouldPassDeepLink")
                println("   - pathToPass: $pathToPass")
                println("   - paramsToPass: $paramsToPass")

                ChatRoot(navigateToAuthController = navController, pathToPass, paramsToPass)
            }

            composable<MainDashboardRoutes.FlipNotifyPages> {
                currentRoute = MainDashboardRoutes.FlipNotifyPages

                NotifyRoot(
                    navigateToAuthController = navController,
                    onDoubleTapChanged = { callback -> onDoubleTapNotifications = callback })
            }
        }
    }


}
