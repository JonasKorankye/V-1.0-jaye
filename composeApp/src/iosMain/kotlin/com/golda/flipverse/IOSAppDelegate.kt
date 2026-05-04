package com.golda.flipverse

import com.flipverse.di.initializeKoin
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize
import kotlin.experimental.ExperimentalNativeApi

private var delegateHookInstalled = false

object IOSAppDelegate {

    @OptIn(ExperimentalNativeApi::class)
    fun initialize() {
        println("🍎 IOSAppDelegate: Initializing iOS app components")

        // Install global exception hook FIRST to catch any init errors
        if (!delegateHookInstalled) {
            delegateHookInstalled = true
            // NOTE: setUnhandledExceptionHook runs the lambda and then STILL terminates the process.
            // It does NOT prevent crashes — it only allows logging before termination.
            setUnhandledExceptionHook { throwable ->
                println("💥 [GlobalExceptionHook] Unhandled exception — app WILL terminate:")
                println("  Type: ${throwable::class.simpleName}")
                println("  Message: ${throwable.message}")
                throwable.printStackTrace()
            }
        }

        // Initialize Firebase first
        Firebase.initialize()

        // NOTE: Push notifications are handled natively in Swift code (iOSApp.swift)
        // using UserNotifications framework and APNs directly

        // Initialize Koin DI
        initializeKoin()

        println("🍎 IOSAppDelegate: iOS initialization completed")
    }

    /**
     * Handle deep link from iOS
     */
    fun handleDeepLink(url: String): Boolean {
        println("🍎 IOSAppDelegate: Deep link received - $url")

        // Parse the deep link and store it globally
        val deepLinkInfo = parseIOSDeepLink(url)
        if (deepLinkInfo != null) {
            DeepLinkManager.setPendingDeepLink(deepLinkInfo)
            println("🍎 IOSAppDelegate: Deep link stored - $deepLinkInfo")
            return true
        }

        return false
    }

    private fun parseIOSDeepLink(url: String): DeepLinkInfo? {
        // Parse URL string to components
        val parts = url.split("?")
        if (parts.size < 2) return null

        val basePart = parts[0]
        val queryPart = parts[1]

        // Extract path from URL
        val path = when {
            basePart.contains("chat") -> "/chat_dm"
            basePart.contains("profile") -> "/view_profile"
            basePart.contains("post") -> "/post_details"
            basePart.contains("livebook") -> "/view_livebook"
            else -> return null
        }

        // Parse query parameters
        val params = mutableMapOf<String, String>()
        queryPart.split("&").forEach { param ->
            val keyValue = param.split("=")
            if (keyValue.size == 2) {
                params[keyValue[0]] = keyValue[1]
            }
        }

        // Determine target route
        val targetRoute = when (path) {
            "/chat_dm" -> com.flipverse.shared.navigation.MainDashboardRoutes.FlipChatPages
            "/view_profile", "/post_details" -> com.flipverse.shared.navigation.MainDashboardRoutes.FlipHomePages
            "/view_livebook" -> com.flipverse.shared.navigation.MainDashboardRoutes.FlipLiveBookPages
            else -> return null
        }

        return DeepLinkInfo(
            targetRoute = targetRoute,
            path = path,
            parameters = params
        )
    }
}