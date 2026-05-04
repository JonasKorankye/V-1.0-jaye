package com.flipverse.data.util

/**
 * Centralized Crashlytics logging utility for FlipVerse app.
 * 
 * This logger helps track and debug errors across the app, especially for:
 * - LiveBook turn-based system issues
 * - User authentication problems
 * - Firestore sync errors
 * - Network failures
 * 
 * Note: This uses expect/actual pattern for platform-specific implementations
 */
object CrashlyticsLogger {
    
    /**
     * Log a LiveBook-specific error with context about the current state.
     * 
     * @param liveBookId The ID of the LiveBook where the error occurred
     * @param turnIndex The current turn index when the error occurred
     * @param error The exception/throwable that was caught
     * @param additionalInfo Optional map of additional context (e.g., contributor_id, operation name)
     */
    fun logLiveBookError(
        liveBookId: String,
        turnIndex: Int,
        error: Throwable,
        additionalInfo: Map<String, String> = emptyMap()
    ) {
        logCrash(
            error = error,
            keys = mapOf(
                "feature" to "livebook",
                "livebook_id" to liveBookId,
                "turn_index" to turnIndex.toString()
            ) + additionalInfo
        )
        println("🔥 Crashlytics: Logged LiveBook error for $liveBookId")
    }
    
    /**
     * Log a user authentication error.
     * 
     * @param userId The user ID (if available)
     * @param operation The auth operation being performed (e.g., "login", "signup", "logout")
     * @param error The exception that occurred
     */
    fun logAuthError(
        userId: String? = null,
        operation: String,
        error: Throwable
    ) {
        val keys = buildMap {
            put("feature", "auth")
            put("auth_operation", operation)
            if (userId != null) {
                put("user_id", userId)
            }
        }
        logCrash(error = error, keys = keys)
        println("🔥 Crashlytics: Logged auth error for operation: $operation")
    }
    
    /**
     * Log a chat/messaging error.
     * 
     * @param conversationId The conversation ID where the error occurred
     * @param error The exception that occurred
     * @param additionalInfo Optional additional context
     */
    fun logChatError(
        conversationId: String,
        error: Throwable,
        additionalInfo: Map<String, String> = emptyMap()
    ) {
        logCrash(
            error = error,
            keys = mapOf(
                "feature" to "chat",
                "conversation_id" to conversationId
            ) + additionalInfo
        )
        println("🔥 Crashlytics: Logged chat error for conversation $conversationId")
    }
    
    /**
     * Log a generic non-fatal error with context.
     * Use this for errors that don't fit into specific categories.
     * 
     * @param error The exception that occurred
     * @param context Description of where/why the error occurred
     * @param additionalInfo Optional map of additional context
     */
    fun logNonFatal(
        error: Throwable,
        context: String,
        additionalInfo: Map<String, String> = emptyMap()
    ) {
        logCrash(
            error = error,
            keys = mapOf("error_context" to context) + additionalInfo
        )
        println("🔥 Crashlytics: Logged non-fatal error - $context")
    }
    
    /**
     * Set the current user ID for crash reports.
     * Call this after successful login.
     * 
     * @param userId The user's unique identifier
     */
    fun setUserId(userId: String) {
        setUserIdInternal(userId)
        println("🔥 Crashlytics: Set user ID to $userId")
    }
    
    /**
     * Clear the user ID from crash reports.
     * Call this on logout.
     */
    fun clearUserId() {
        setUserIdInternal("")
        println("🔥 Crashlytics: Cleared user ID")
    }
    
    /**
     * Log a custom message/breadcrumb to track user flow.
     * These appear in crash reports to show what the user was doing.
     * 
     * @param message The message to log
     */
    fun log(message: String) {
        logMessageInternal(message)
    }
    
    /**
     * Set a custom key-value pair that will be included in all subsequent crash reports.
     * Useful for tracking app state or user preferences.
     * 
     * @param key The key name
     * @param value The value
     */
    fun setCustomKey(key: String, value: String) {
        setKeyInternal(key, value)
    }
    
    // Platform-specific implementations
    private fun logCrash(error: Throwable, keys: Map<String, String>) {
        try {
            logCrashInternal(error, keys)
        } catch (e: Exception) {
            println("❌ Failed to log to Crashlytics: ${e.message}")
        }
    }
}

// Expected platform-specific functions
internal expect fun logCrashInternal(error: Throwable, keys: Map<String, String>)
internal expect fun setUserIdInternal(userId: String)
internal expect fun logMessageInternal(message: String)
internal expect fun setKeyInternal(key: String, value: String)
