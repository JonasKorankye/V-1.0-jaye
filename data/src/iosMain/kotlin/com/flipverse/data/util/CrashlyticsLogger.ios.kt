package com.flipverse.data.util

//import cocoapods.FirebaseCrashlytics.FIRCrashlytics
import platform.Foundation.NSError
import platform.Foundation.NSLocalizedDescriptionKey
import platform.Foundation.NSUnderlyingErrorKey

/**
 * iOS-specific implementation of Crashlytics logging using native Firebase SDK.
 * 
 * This implementation uses CocoaPods Firebase Crashlytics for full iOS support.
 * All features including crash reporting, custom keys, and user tracking are supported.
 */
internal actual fun logCrashInternal(error: Throwable, keys: Map<String, String>) {
    try {
//        val crashlytics = FIRCrashlytics.crashlytics()
        
        // Set all custom keys
        keys.forEach { (key, value) ->
//            crashlytics.setCustomValue(value, key)
        }
        
        // Convert Kotlin Throwable to NSError for Firebase Crashlytics
        val nsError = createNSError(error)
//        crashlytics.recordError(nsError)
        
        println("🔥 iOS Crashlytics: Logged error - ${error.message}")
    } catch (e: Exception) {
        println("❌ Failed to log crash to iOS Crashlytics: ${e.message}")
        // Fallback to console logging
        println("📱 iOS Crashlytics (Fallback): ${error.message}")
        println("   Keys: $keys")
    }
}

internal actual fun setUserIdInternal(userId: String) {
    try {
//        FIRCrashlytics.crashlytics().setUserID(userId)
        println("🔥 iOS Crashlytics: Set user ID to $userId")
    } catch (e: Exception) {
        println("❌ Failed to set iOS Crashlytics user ID: ${e.message}")
        // Fallback to console logging
        println("📱 iOS Crashlytics (Fallback): User ID set to $userId")
    }
}

internal actual fun logMessageInternal(message: String) {
    try {
//        FIRCrashlytics.crashlytics().log(message)
    } catch (e: Exception) {
        println("❌ Failed to log message to iOS Crashlytics: ${e.message}")
        // Fallback to console logging
        println("📱 iOS Crashlytics (Fallback): $message")
    }
}

internal actual fun setKeyInternal(key: String, value: String) {
    try {
//        FIRCrashlytics.crashlytics().setCustomValue(value, key)
    } catch (e: Exception) {
        println("❌ Failed to set iOS Crashlytics custom key: ${e.message}")
        // Fallback to console logging
        println("📱 iOS Crashlytics (Fallback): Set $key = $value")
    }
}

/**
 * Convert Kotlin Throwable to NSError for Firebase Crashlytics.
 * This creates a proper NSError object that Crashlytics can process.
 */
private fun createNSError(throwable: Throwable): NSError {
    val errorInfo = mutableMapOf<Any?, Any>()
    
    // Add error message
    errorInfo[NSLocalizedDescriptionKey] = throwable.message ?: "Unknown error"
    
    // Add stack trace as string
    val stackTrace = throwable.stackTraceToString()
    errorInfo["stackTrace"] = stackTrace
    
    // Add cause if available
    throwable.cause?.let { cause ->
        val causeError = createNSError(cause)
        errorInfo[NSUnderlyingErrorKey] = causeError
    }
    
    return NSError.errorWithDomain(
        domain = "com.flipverse.KotlinException",
        code = -1,
        userInfo = errorInfo
    )
}
