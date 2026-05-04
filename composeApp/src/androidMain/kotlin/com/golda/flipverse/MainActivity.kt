package com.golda.flipverse

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.flipverse.shared.GooglePlayServicesChecker
import com.flipverse.shared.navigation.MainDashboardRoutes
import com.flipverse.shared.util.setActivityProvider
import com.google.android.gms.common.GoogleApiAvailability
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import com.mmk.kmpnotifier.permission.permissionUtil
import com.mmk.kmpnotifier.extensions.onCreateOrOnNewIntent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permissionUtil by permissionUtil()

    // Hold the deep link to process after the app is ready
    private var pendingDeepLink: DeepLinkInfo? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        println("🚀 MainActivity onCreate - App Launch Event")
        println("🚀 Intent action: ${intent.action}")
        println("🚀 Intent data: ${intent.data}")
        println(
            "🚀 Intent extras: ${
                intent.extras?.keySet()?.joinToString { "$it=${intent.extras?.get(it)}" }
            }"
        )
        println("🚀 savedInstanceState: ${if (savedInstanceState != null) "RESTORED" else "FRESH_START"}")

        // Detect if this is a notification launch from killed state
        val isNotificationLaunch = intent.extras?.let { bundle ->
            bundle.keySet().any { key ->
                key.contains("notification", ignoreCase = true) ||
                        key.contains("type", ignoreCase = true) ||
                        key.contains("customData", ignoreCase = true) ||
                        key.contains("conversationId", ignoreCase = true) ||
                        key.contains("recipientId", ignoreCase = true) ||
                        key.contains("postId", ignoreCase = true) ||
                        key.contains("messageId", ignoreCase = true)
            }
        } ?: false

        val isFromKilledState = savedInstanceState == null

        if (isNotificationLaunch && isFromKilledState) {
            println("🔥 CRITICAL: App launched from KILLED STATE via notification!")
            println(
                "🔥 Intent extras: ${
                    intent.extras?.keySet()?.map { "$it=${intent.extras?.get(it)}" }
                }"
            )
            println("🔥 KMPNotifier should handle this via onNotificationClicked callback")
        }

        // CRITICAL: This enables KMPNotifier to process notification clicks properly
        // This MUST be called for proper notification handling on app launch
        NotifierManager.onCreateOrOnNewIntent(intent)
        println("🔔 KMPNotifier.onCreateOrOnNewIntent called - notification processing enabled")

        // Set up app state monitoring
        val currentAppState = AppStateManager.getCurrentState()
        println("📱 Current App State at onCreate: $currentAppState")

        // Listen to app state changes for debugging
        val appStateListener: (AppState) -> Unit = { newState ->
            println("📱 App State Changed to: $newState")
        }
        AppStateManager.addListener(appStateListener)

        // Handle traditional deep links (non-notification)
        if (!isNotificationLaunch) {
            println("🔗 Handling traditional deep link (not from notification)")
            handleDeepLink(intent)
        } else {
            println("🔔 Skipping traditional deep link handling - notification click will be handled by KMPNotifier")
        }

        println("🔗 Pending deep link after onCreate: $pendingDeepLink")

        // Use the checker (Android-specific)
        val checker = GooglePlayServicesChecker()
        val isAvailable = checker.checkGooglePlayServices(this)
        val version = checker.getGooglePlayServicesVersion(this)

        Log.d("MainActivity", "Google Play Services Available: $isAvailable")
        Log.d("MainActivity", "Google Play Services Version: $version")

        if (!isAvailable) {
            // Handle the unavailable case
            handleGooglePlayServicesUnavailable()
        } else {
            println("Google Play Services is available")

            // Request notification permission - KMPNotifier will handle the notifications
            permissionUtil.askNotificationPermission()
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setActivityProvider { this }

        setContent {
            AppWithDeepLink(pendingDeepLink)
        }

    }

    override fun onStart() {
        super.onStart()
        println("📱 MainActivity onStart")

        // When the app starts (could be from background), check for notification data
        lifecycleScope.launch {
            delay(100) // Allow system to fully initialize
            checkAndProcessPendingNotifications()
        }
    }

    private fun checkAndProcessPendingNotifications() {
        println("🔍 Checking for pending notifications")

        // Check current intent for notification data
        intent?.let { currentIntent ->
            val hasNotificationData = currentIntent.extras?.let { bundle ->
                bundle.keySet().any { key ->
                    key.contains("notification", ignoreCase = true) ||
                            key.contains("payload", ignoreCase = true) ||
                            key.contains("type", ignoreCase = true) ||
                            key.contains("conversationId", ignoreCase = true) ||
                            key.contains("senderId", ignoreCase = true) ||
                            key.contains("postId", ignoreCase = true) ||
                            key.contains("customData", ignoreCase = true) ||
                            key.contains("messageId", ignoreCase = true) ||
                            key.contains("recipientId", ignoreCase = true)
                }
            } ?: false

            if (hasNotificationData) {
                println("🔔 Processing notification data from intent")
                handleDeepLink(currentIntent)
            }
        }
    }

    private fun handleDeepLink(intent: Intent?) {
        println("🔍 handleDeepLink called with intent: $intent")

        if (intent != null) {
            println("🔍 Intent details:")
            println("   - Action: ${intent.action}")
            println("   - Data: ${intent.data}")
            println("   - Type: ${intent.type}")
            println("   - Categories: ${intent.categories}")
            println("   - Component: ${intent.component}")
            println("   - Package: ${intent.`package`}")
            println("   - Flags: ${intent.flags}")

            // Log all extras
            intent.extras?.let { bundle ->
                println("   - Extras:")
                for (key in bundle.keySet()) {
                    val value = bundle.get(key)
                    println("     $key: $value (${value?.javaClass?.simpleName})")
                }
            } ?: println("   - No extras")

            // Enhanced notification data detection for KMPNotifier
            val hasNotificationData = intent.extras?.let { bundle ->
                val notificationKeys = bundle.keySet().filter { key ->
                    key.contains("notification", ignoreCase = true) ||
                            key.contains("payload", ignoreCase = true) ||
                            key.contains("data", ignoreCase = true) ||
                            key.contains("type", ignoreCase = true) ||
                            key.contains("conversationId", ignoreCase = true) ||
                            key.contains("senderId", ignoreCase = true) ||
                            key.contains("postId", ignoreCase = true) ||
                            key.contains("customData", ignoreCase = true) ||
                            key.contains("messageId", ignoreCase = true) ||
                            key.contains("recipientId", ignoreCase = true) ||
                            key.contains("liveBookId", ignoreCase = true) ||
                            key.contains("title", ignoreCase = true) ||
                            key.contains("body", ignoreCase = true) ||
                            // KMPNotifier specific keys
                            key.startsWith("gcm.notification.") ||
                            key == "google.sent_time" ||
                            key == "google.ttl" ||
                            key == "from"
                }

                println("🔍 Found potential notification keys: $notificationKeys")

                // Specific check for payload data presence
                val hasPayloadData = bundle.keySet().any { key ->
                    key.contains("type", ignoreCase = true) ||
                            key.contains("conversationId", ignoreCase = true) ||
                            key.contains("customData", ignoreCase = true) ||
                            key.contains("senderId", ignoreCase = true)
                }

                if (notificationKeys.isNotEmpty() && !hasPayloadData) {
                    println("⚠️ NOTIFICATION WITHOUT PAYLOAD DATA DETECTED!")
                    println("⚠️ This indicates background notification payload processing issue")
                    println("⚠️ Notification keys found but no business logic payload")
                }

                notificationKeys.isNotEmpty()
            } ?: false

            if (hasNotificationData) {
                println("🔔 This looks like a notification click intent!")

                // Extract payload data from intent extras
                val payloadData = mutableMapOf<String, Any?>()
                intent.extras?.let { bundle ->
                    for (key in bundle.keySet()) {
                        val value = bundle.get(key)

                        // Clean up KMPNotifier/FCM specific keys
                        val cleanKey = when {
                            key.startsWith("gcm.notification.") -> key.removePrefix("gcm.notification.")
                            else -> key
                        }

                        payloadData[cleanKey] = value
                    }
                }

                if (payloadData.isNotEmpty()) {
                    println("🔔 Extracted payload data: $payloadData")

                    // For background/inactive states, ensure we trigger navigation after a delay
                    val currentAppState = AppStateManager.getCurrentState()
                    println("🔔 Processing notification in app state: $currentAppState")

                    when (currentAppState) {
                        AppState.FOREGROUND -> {
                            // App is active - process immediately
                            try {
                                com.flipverse.shared.navigation.NotificationNavigationBridge.handleNotificationClick(
                                    payloadData
                                )
                                println("✅ Immediately processed notification click (FOREGROUND)")
                                return
                            } catch (e: Exception) {
                                println("❌ Error immediately processing notification click: ${e.message}")
                                e.printStackTrace()
                            }
                        }

                        AppState.BACKGROUND, AppState.INACTIVE -> {
                            // App is not active - delay processing until app becomes ready
                            println("🔄 Delaying notification processing until app is ready")
                            lifecycleScope.launch {
                                // Wait for app to become ready
                                var attempts = 0
                                while (AppStateManager.getCurrentState() != AppState.FOREGROUND && attempts < 50) {
                                    delay(100)
                                    attempts++
                                }

                                try {
                                    com.flipverse.shared.navigation.NotificationNavigationBridge.handleNotificationClick(
                                        payloadData
                                    )
                                    println("✅ Delayed processed notification click (was ${currentAppState})")
                                } catch (e: Exception) {
                                    println("❌ Error delayed processing notification click: ${e.message}")
                                    e.printStackTrace()
                                }
                            }
                            return
                        }
                    }
                }
            }
        }

        var deepLink = intent?.data

        // Handle backup deep link reconstruction (still useful for direct app launches)
        if (deepLink == null && intent != null) {
            val deepLinkString = intent.getStringExtra("DEEP_LINK_URI")
            if (deepLinkString != null) {
                println("🔄 intent.data is NULL, reconstructing from extras")
                println("   - Stored URI: $deepLinkString")
                deepLink = deepLinkString.toUri()
                println("   - Reconstructed URI: $deepLink")
            } else {
                println("🔗 No deep link found in intent")
            }
        }

        deepLink?.let { uri ->
            println("🔗 Deep link received in MainActivity: $uri")
            println("   - Intent Action: ${intent?.action}")

            when (uri.host) {
                "flipverse.com" -> {
                    pendingDeepLink = parseDeepLink(uri)
                    println("🔗 Pending deep link set: $pendingDeepLink")
                }

                else -> {
                    println("❓ Unknown deep link host: ${uri.host}")
                }
            }
        }
    }

    private fun parseDeepLink(uri: Uri): DeepLinkInfo? {
        println("🔗 Parsing deep link: $uri")
        println("   - Host: ${uri.host}")
        println("   - Path: ${uri.path}")
        println("   - Query: ${uri.query}")

        return when (uri.path) {
            "/chat", "/chat_dm" -> {
                val conversationId =
                    uri.getQueryParameter("conversationId") ?: uri.getQueryParameter("chat_id")
                val otherUserId =
                    uri.getQueryParameter("otherUserId") ?: uri.getQueryParameter("recipientId")
                    ?: uri.getQueryParameter("user_id")
                println("🗨️ Chat deep link - conversationId: $conversationId, otherUserId: $otherUserId")

                val params = mutableMapOf<String, String>()
                conversationId?.let { params["conversationId"] = it }
                otherUserId?.let { params["otherUserId"] = it }

                DeepLinkInfo(
                    targetRoute = MainDashboardRoutes.FlipChatPages,
                    path = "/chat_dm",
                    parameters = params
                )
            }
            "/profile", "/view_profile" -> {
                val userId = uri.getQueryParameter("userId") ?: uri.getQueryParameter("recipientId") ?: uri.getQueryParameter("user_id")
                println("👤 Profile deep link - userId: $userId")

                val params = mutableMapOf<String, String>()
                userId?.let { params["userId"] = it }

                DeepLinkInfo(
                    targetRoute = MainDashboardRoutes.FlipHomePages,
                    path = "/view_profile",
                    parameters = params
                )
            }
            "/post", "/post_details" -> {
                val postId = uri.getQueryParameter("postId") ?: uri.getQueryParameter("post_id")
                println("📄 Post deep link - postId: $postId")

                val params = mutableMapOf<String, String>()
                postId?.let { params["postId"] = it }

                DeepLinkInfo(
                    targetRoute = MainDashboardRoutes.FlipHomePages,
                    path = "/post_details",
                    parameters = params
                )
            }
            "/livebook", "/view_livebook" -> {
                val liveBookId = uri.getQueryParameter("liveBookId") ?: uri.getQueryParameter("conversationId") ?: uri.getQueryParameter("post_id")
                println("📖 LiveBook deep link - liveBookId: $liveBookId")

                val params = mutableMapOf<String, String>()
                liveBookId?.let { params["liveBookId"] = it }

                DeepLinkInfo(
                    targetRoute = MainDashboardRoutes.FlipLiveBookPages,
                    path = "/view_livebook",
                    parameters = params
                )
            }
            else -> {
                println("❓ Unknown deep link path: ${uri.path}")
                null
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        println("🔄 onNewIntent called with action: ${intent.action}")
        println("🔄 onNewIntent data: ${intent.data}")
        println("📱 Current App State: ${AppStateManager.getCurrentState()}")

        // CRITICAL FIX: Required for KMPNotifier background/inactive notification handling
        // This MUST be called in onNewIntent for proper notification payload processing
        NotifierManager.onCreateOrOnNewIntent(intent)
        println("🔔 KMPNotifier.onCreateOrOnNewIntent called in onNewIntent")

        // KMPNotifier will handle most notification-based deeplinks through onNotificationClicked
        // This mainly handles manual deep link intents
        handleDeepLink(intent)

        // If app is already running and we have a pending deep link, trigger navigation
        if (pendingDeepLink != null) {
            val appState = AppStateManager.getCurrentState()
            println("🔄 Processing deep link in state: $appState")
            println("🔄 Pending deep link: $pendingDeepLink")

            when (appState) {
                AppState.FOREGROUND -> {
                    // App is visible - navigate immediately without delay
                    println("🟢 App in FOREGROUND - Navigating immediately")
                    lifecycleScope.launch {
                        setContent {
                            AppWithDeepLink(pendingDeepLink)
                        }
                    }
                }
                AppState.BACKGROUND, AppState.INACTIVE -> {
                    // App is not visible - add small delay for proper initialization
                    println("🟡 App in BACKGROUND/INACTIVE - Navigating with delay")
                    lifecycleScope.launch {
                        delay(100)
                        setContent {
                            AppWithDeepLink(pendingDeepLink)
                        }
                    }
                }
            }
        } else {
            println("🔗 No pending deep link to process")
        }
    }

    override fun onResume() {
        super.onResume()
        println("📱 MainActivity onResume")
        println("📱 Current Intent in onResume:")
        println("   - Action: ${intent.action}")
        println("   - Data: ${intent.data}")

        // Check if the current intent has notification data that wasn't processed
        intent.extras?.let { bundle ->
            println("   - Extras in onResume:")
            for (key in bundle.keySet()) {
                val value = bundle.get(key)
                println("     $key: $value")
            }

            // Check for any notification-related data
            val notificationKeys = bundle.keySet().filter { key ->
                key.contains("notification", ignoreCase = true) ||
                        key.contains("type", ignoreCase = true) ||
                        key.contains("conversationId", ignoreCase = true) ||
                        key.contains("senderId", ignoreCase = true) ||
                        key.contains("postId", ignoreCase = true) ||
                        key.contains("payload", ignoreCase = true)
            }

            if (notificationKeys.isNotEmpty()) {
                println("🔔 Found notification-related keys in onResume: $notificationKeys")
                // Try to handle as notification click
                val payloadData = mutableMapOf<String, Any?>()
                for (key in bundle.keySet()) {
                    payloadData[key] = bundle.get(key)
                }

                try {
                    com.flipverse.shared.navigation.NotificationNavigationBridge.handleNotificationClick(
                        payloadData
                    )
                    println("✅ Processed notification click in onResume")
                } catch (e: Exception) {
                    println("❌ Error processing notification click in onResume: ${e.message}")
                }
            }
        }

        println("📱 Pending deep link in onResume: $pendingDeepLink")

        // Check if KMPNotifier is properly initialized
        lifecycleScope.launch {
            try {
                val pushNotifier = NotifierManager.getPushNotifier()
                val token = pushNotifier.getToken()
                println("🔔 KMPNotifier status in onResume - Token available: ${token != null}")
            } catch (e: Exception) {
                println("❌ KMPNotifier error in onResume: ${e.message}")
            }
        }
    }

    private fun handleGooglePlayServicesUnavailable() {
        // Show dialog or handle the error
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)

        if (googleApiAvailability.isUserResolvableError(resultCode)) {
            googleApiAvailability.getErrorDialog(this, resultCode, 9000)?.show()
        } else {
            // Handle non-recoverable error
            Log.e("MainActivity", "This device is not supported for Google Play Services")
            // You could show a user-friendly message here instead of crashing
        }
    }

}

@Composable
fun AppWithDeepLink(
    deepLinkInfo: DeepLinkInfo?
) {
    LaunchedEffect(deepLinkInfo) {
        if (deepLinkInfo != null) {
            println("🚀 AppWithDeepLink: Passing deep link to App: $deepLinkInfo")
        }
    }

    // Pass the deep link info to the main app
    App(initialDeepLink = deepLinkInfo)
}

