package com.flipverse.auth

sealed interface AuthAction {
    data class OnTogglePasswordVisibilityClick(val isVisible: Boolean): AuthAction
    data class OnToggleConfirmPasswordVisibilityClick(val isConfirmVisible: Boolean): AuthAction
    data object OnLoginClick: AuthAction
    data object OnSendOTPMailClick: AuthAction
    data object OnSignUpContinue: AuthAction
    data object OnSetUpPasswordClick: AuthAction
    data object OnboardingCompleted: AuthAction
    data object OnForgotPasswordClick: AuthAction
    data class OnEmailIdChange(val newEmail: String): AuthAction
    data class OnUsernameChange(val newUsername: String): AuthAction
    data class OnFullNameChange(val newFullname: String): AuthAction
    data class OnPasswordChange(val newPassword: String): AuthAction
    data class OnConfirmPasswordChange(val confirmPassword: String): AuthAction
}