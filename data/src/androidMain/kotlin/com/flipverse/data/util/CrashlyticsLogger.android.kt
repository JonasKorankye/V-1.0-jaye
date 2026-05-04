package com.flipverse.data.util

import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Android-specific implementation of Crashlytics logging.
 * Uses the native Firebase Crashlytics SDK for Android.
 */
internal actual fun logCrashInternal(error: Throwable, keys: Map<String, String>) {
    try {
        val crashlytics = FirebaseCrashlytics.getInstance()
        
        // Set all custom keys
        keys.forEach { (key, value) ->
            crashlytics.setCustomKey(key, value)
        }
        
        // Record the exception
        crashlytics.recordException(error)
    } catch (e: Exception) {
        println("❌ Failed to log crash to Crashlytics: ${e.message}")
    }
}

internal actual fun setUserIdInternal(userId: String) {
    try {
        FirebaseCrashlytics.getInstance().setUserId(userId)
    } catch (e: Exception) {
        println("❌ Failed to set Crashlytics user ID: ${e.message}")
    }
}

internal actual fun logMessageInternal(message: String) {
    try {
        FirebaseCrashlytics.getInstance().log(message)
    } catch (e: Exception) {
        println("❌ Failed to log message to Crashlytics: ${e.message}")
    }
}

internal actual fun setKeyInternal(key: String, value: String) {
    try {
        FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    } catch (e: Exception) {
        println("❌ Failed to set Crashlytics custom key: ${e.message}")
    }
}
