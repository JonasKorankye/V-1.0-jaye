package com.flipverse.shared.domain

import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Serializable
data class OTPRequest(
    val email: String,
    val otp: String? = null
)

@Serializable
data class OtpResponse(
    val success: Boolean,
    val message: String,
    val email: String,
    val otp: String
)

@Serializable
data class OtpVerification @OptIn(ExperimentalTime::class) constructor(
    val id: String = "",
    val email: String = "",
    val otp: String = "",
    val purpose: String = "", // "login", "registration", "password_reset"
    val userId: String = "",
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val expiresAt: Long = Clock.System.now().toEpochMilliseconds() + (6 * 60 * 1000), // 5 minutes
    val isUsed: Boolean = false,
    val attempts: Int = 0,
    val maxAttempts: Int = 3
)