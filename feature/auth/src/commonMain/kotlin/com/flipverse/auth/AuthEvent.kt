package com.flipverse.auth

sealed interface AuthEvent {
    data object LoginSuccess : AuthEvent
    data class Error(val error: String) : AuthEvent

    sealed interface Navigate {
        data object VerifyEmail : AuthEvent
        data object ForgotPassword : AuthEvent
        data object SignUpWithPassword : AuthEvent
        data object VerifyOtp : AuthEvent
        data object Dashboard : AuthEvent
        data object CreateProfile : AuthEvent
        data object CreateUsername : AuthEvent
    }
}
