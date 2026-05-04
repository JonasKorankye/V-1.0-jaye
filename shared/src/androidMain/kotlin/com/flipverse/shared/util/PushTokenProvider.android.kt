package com.flipverse.shared.util

import com.mmk.kmpnotifier.notification.NotifierManager
import kotlin.coroutines.cancellation.CancellationException

/**
 * Gets the current push notification token using KMPNotifier.
 */
actual suspend fun getPushNotificationToken(): String? {
    return try {
        // Use KMPNotifier to get the FCM token on Android (same as iOS)
        NotifierManager.getPushNotifier().getToken()
    } catch (e: Exception) {
        // Prevent crashing the app if the token retrieval fails
        if (e is CancellationException) throw e // Re-throw cancellation exceptions
        e.printStackTrace()
        null // Return null on failure
    }
}