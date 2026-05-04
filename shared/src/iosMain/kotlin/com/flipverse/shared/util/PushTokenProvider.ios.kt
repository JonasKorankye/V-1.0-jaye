package com.flipverse.shared.util

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException

/**
 * Singleton that receives the FCM token from the Swift AppDelegate.
 * Swift calls `onTokenReceived` when Firebase Messaging delivers a token.
 */
object IOSPushTokenProvider {
    private var currentToken: String? = null
    private var pendingDeferred: CompletableDeferred<String>? = null

    /**
     * Called from Swift when a new FCM token is received.
     */
    fun onTokenReceived(token: String) {
        println("🔔 IOSPushTokenProvider: Token received from Swift: $token")
        currentToken = token
        pendingDeferred?.complete(token)
        pendingDeferred = null
    }

    /**
     * Returns the current FCM token, or waits up to 10 seconds for one to arrive.
     */
    suspend fun getToken(): String? {
        // If we already have a token, return it immediately
        currentToken?.let { return it }

        // Otherwise wait for the token to arrive from Swift
        val deferred = CompletableDeferred<String>()
        pendingDeferred = deferred
        return withTimeoutOrNull(10_000L) {
            deferred.await()
        }
    }
}

/**
 * Gets the current FCM push notification token on iOS.
 * The token is provided by Firebase Messaging via the native Swift AppDelegate.
 */
actual suspend fun getPushNotificationToken(): String? {
    return try {
        IOSPushTokenProvider.getToken()
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        println("❌ iOS getPushNotificationToken error: ${e.message}")
        null
    }
}