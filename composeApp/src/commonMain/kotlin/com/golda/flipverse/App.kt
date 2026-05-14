package com.golda.flipverse

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.flipverse.data.domain.UserRepository
import com.flipverse.navigation.SetupNavGraph
import com.flipverse.shared.Constants
import com.flipverse.shared.FlipVerseTheme
import com.flipverse.shared.PreferencesRepository.hasAcceptedTerms
import com.flipverse.shared.navigation.MainDashboardRoutes
import com.flipverse.shared.navigation.NotificationNavigationBridge
import com.flipverse.shared.navigation.NotificationNavigationInfo
import com.flipverse.shared.navigation.Screen
import com.mmk.kmpauth.google.GoogleAuthCredentials
import com.mmk.kmpauth.google.GoogleAuthProvider
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

data class DeepLinkInfo(
    val targetRoute: Any,
    val path: String,
    val parameters: Map<String, String> = emptyMap()
)

object DeepLinkManager {
    private var _pendingDeepLink = mutableStateOf<DeepLinkInfo?>(null)
    private var hasBeenProcessed = false

    val pendingDeepLink = _pendingDeepLink

    fun setPendingDeepLink(deepLink: DeepLinkInfo?) {
        _pendingDeepLink.value = deepLink
        hasBeenProcessed = false // Reset the processed flag when setting new deep link
        println("🔗 DeepLinkManager: Deep link set: $deepLink")
    }

    fun getPendingDeepLink(): DeepLinkInfo? {
        return _pendingDeepLink.value
    }

    fun markAsProcessed() {
        hasBeenProcessed = true
        println("🔗 DeepLinkManager: Deep link processed")
    }

    fun isProcessed(): Boolean {
        return hasBeenProcessed
    }

    fun clearPendingDeepLink() {
        println("🔗 DeepLinkManager: Clearing deep link")
        _pendingDeepLink.value = null
        hasBeenProcessed = false
    }
}

@Composable
fun App(initialDeepLink: DeepLinkInfo? = null) {
    LaunchedEffect(Unit) {
        println("🔔 Setting up NotificationNavigationBridge handler immediately")
        NotificationNavigationBridge.setNavigationHandler { notificationNavigationInfo: NotificationNavigationInfo ->
            println("🔔 NotificationNavigationBridge: Handling notification navigation: $notificationNavigationInfo")
            // Convert NotificationNavigationInfo to DeepLinkInfo and set in DeepLinkManager
            val deepLink = DeepLinkInfo(
                targetRoute = notificationNavigationInfo.targetRoute,
                path = notificationNavigationInfo.path,
                parameters = notificationNavigationInfo.parameters
            )

            println("🔔 Setting deep link in DeepLinkManager: $deepLink")
            DeepLinkManager.setPendingDeepLink(deepLink)
            println("🔔 Deep link created and set successfully")
        }
        println("✅ NotificationNavigationBridge handler set up successfully")
    }

    // CRITICAL: Store deep link IMMEDIATELY before any other logic
    if (initialDeepLink != null) {
        println("📱 App: Received deep link - STORING IMMEDIATELY: $initialDeepLink")
        DeepLinkManager.setPendingDeepLink(initialDeepLink)
    }

    FlipVerseTheme {
        val userRepository = koinInject<UserRepository>()
        val isUserAuthenticated = rememberSaveable { userRepository.getCurrentUserId() != null }

        val startDestination = remember {
            println("📱 App: User authenticated: $isUserAuthenticated")
            if (isUserAuthenticated) {
                MainDashboardRoutes.FlipHomePages
            } else if (hasAcceptedTerms()) {
                Screen.Auth
            } else Screen.ConsentGate
        }

        // Fetch user details outside of remember{} to avoid side-effects during composition
        if (isUserAuthenticated) {
            LaunchedEffect(Unit) {
                try {
                    if (userRepository.getCurrentUserId() == Firebase.auth.currentUser?.uid) {
                        val userEmail =
                            userRepository.fetchUserDetails(Firebase.auth.currentUser?.uid)
                        println("EmailFromFV: $userEmail")
                    }
                } catch (e: Exception) {
                    println("Error fetching user details: ${e.message}")
                }
            }
        }

        LaunchedEffect(Unit) {
            GoogleAuthProvider.create(
                credentials = GoogleAuthCredentials(serverId = Constants.WEB_CLIENT_ID)
            )
            println("📱 App: Ready to display content")

            // Log the stored deep link
            val storedLink = DeepLinkManager.getPendingDeepLink()
            println("📱 App: Stored deep link in manager: $storedLink")
        }

        // Make the navigation reactive to deep link changes
        val currentPendingDeepLink by DeepLinkManager.pendingDeepLink

        // Force recomposition when deep link changes
        LaunchedEffect(currentPendingDeepLink) {
            if (currentPendingDeepLink != null) {
                println("📱 App: Deep link changed, triggering navigation update: $currentPendingDeepLink")
            }
        }

        SetupNavGraphWithDeepLink(startDestination, currentPendingDeepLink)

    }
}

@Composable
fun SetupNavGraphWithDeepLink(startDestination: Any, deepLinkInfo: DeepLinkInfo?) {
    // Reactively consume changes to DeepLinkManager.pendingDeepLink
    val currentDeepLink by DeepLinkManager.pendingDeepLink

    val deepLinkRoute = currentDeepLink?.targetRoute as? MainDashboardRoutes

    // Handle deep link navigation after the navigation graph is ready
    LaunchedEffect(currentDeepLink) {
        val deepLink = currentDeepLink // Local copy for smart cast
        if (deepLink != null) {
            println("🎯 Setting up navigation with deep link: $deepLink")
            println("🎯 Deep link target route: ${deepLink.targetRoute}")
            println("🎯 Deep link path: ${deepLink.path}")
            println("🎯 Deep link parameters: ${deepLink.parameters}")
            println("🎯 Extracted route for navigation: $deepLinkRoute")
            // Store the deep link for individual navigation roots to process
            DeepLinkManager.setPendingDeepLink(deepLink)
        }
    }

    SetupNavGraph(
        startDestination = startDestination,
        deepLinkRoute = deepLinkRoute,
        deepLinkPath = currentDeepLink?.path,
        deepLinkParameters = currentDeepLink?.parameters ?: emptyMap()
    )
}
