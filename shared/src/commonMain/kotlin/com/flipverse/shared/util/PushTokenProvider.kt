package com.flipverse.shared.util

import kotlin.coroutines.cancellation.CancellationException

/**
 * Gets the current FCM push notification token.
 * Returns the token string on success or null on failure.
 */
expect suspend fun getPushNotificationToken(): String?