package com.flipverse.userprofile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flipverse.data.domain.UserRepository
import com.flipverse.shared.PreferencesRepository.getEmail
import com.flipverse.shared.RequestState
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangePasswordViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        println("ChangePasswordViewModel coroutine exception: ${throwable::class.simpleName}: ${throwable.message}")
        throwable.printStackTrace()
    }

    var passwordState by mutableStateOf(ChangePasswordState())
        private set

    var changePasswordRequestState: RequestState<Unit> by mutableStateOf(RequestState.Idle)
        private set

    var sendResetLinkRequestState: RequestState<Unit> by mutableStateOf(RequestState.Idle)
        private set

    private var onPasswordResetEmailSentCallback: ((String) -> Unit)? = null

    fun setOnPasswordResetEmailSentCallback(callback: (String) -> Unit) {
        onPasswordResetEmailSentCallback = callback
    }

    fun onAction(action: ChangePasswordAction) {
        when (action) {
            is ChangePasswordAction.OnCurrentPasswordChange -> {
                passwordState = passwordState.copy(
                    currentPassword = action.password,
                    errorMessage = ""
                )
            }

            is ChangePasswordAction.OnNewPasswordChange -> {
                passwordState = passwordState.copy(
                    newPassword = action.password,
                    errorMessage = ""
                )
            }

            is ChangePasswordAction.OnConfirmPasswordChange -> {
                passwordState = passwordState.copy(
                    confirmPassword = action.password,
                    errorMessage = ""
                )
            }

            is ChangePasswordAction.OnEmailChange -> {
                passwordState = passwordState.copy(
                    email = action.email,
                    errorMessage = ""
                )
            }

            is ChangePasswordAction.ToggleCurrentPasswordVisibility -> {
                passwordState = passwordState.copy(
                    showCurrentPassword = !passwordState.showCurrentPassword
                )
            }

            is ChangePasswordAction.ToggleNewPasswordVisibility -> {
                passwordState = passwordState.copy(
                    showNewPassword = !passwordState.showNewPassword
                )
            }

            is ChangePasswordAction.ToggleConfirmPasswordVisibility -> {
                passwordState = passwordState.copy(
                    showConfirmPassword = !passwordState.showConfirmPassword
                )
            }

            is ChangePasswordAction.SwitchMode -> {
                passwordState = passwordState.copy(
                    isChangePasswordMode = action.isChangePasswordMode,
                    errorMessage = "",
                    successMessage = ""
                )
            }

            ChangePasswordAction.ChangePassword -> {
                changePassword()
            }

            ChangePasswordAction.SendResetLink -> {
                sendPasswordResetLink()
            }

            ChangePasswordAction.ClearMessages -> {
                passwordState = passwordState.copy(
                    errorMessage = "",
                    successMessage = ""
                )
            }
        }
    }

    private fun changePassword() {
        // Validate inputs
        when {
            passwordState.currentPassword.isEmpty() -> {
                passwordState = passwordState.copy(
                    errorMessage = "Please enter your current password"
                )
                return
            }

            passwordState.newPassword.isEmpty() -> {
                passwordState = passwordState.copy(
                    errorMessage = "Please enter a new password"
                )
                return
            }

            passwordState.newPassword.length < 8 -> {
                passwordState = passwordState.copy(
                    errorMessage = "New password must be at least 8 characters long"
                )
                return
            }

            passwordState.newPassword != passwordState.confirmPassword -> {
                passwordState = passwordState.copy(
                    errorMessage = "New passwords do not match"
                )
                return
            }

            passwordState.newPassword == passwordState.currentPassword -> {
                passwordState = passwordState.copy(
                    errorMessage = "New password must be different from current password"
                )
                return
            }
        }

        changePasswordRequestState = RequestState.Loading
        passwordState = passwordState.copy(errorMessage = "", successMessage = "")

        viewModelScope.launch(exceptionHandler) {
            val result = withContext(Dispatchers.IO) {
                var success = false
                var errorMsg = ""

                userRepository.changePassword(
                    email = getEmail(),
                    currentPassword = passwordState.currentPassword,
                    newPassword = passwordState.newPassword,
                    onSuccess = { success = true },
                    onError = { message -> errorMsg = message }
                )

                Pair(success, errorMsg)
            }

            // Update state on Main thread
            if (result.first) {
                changePasswordRequestState = RequestState.Success(Unit)
                passwordState = passwordState.copy(
                    successMessage = "Password changed successfully!",
                    currentPassword = "",
                    newPassword = "",
                    confirmPassword = ""
                )
            } else {
                changePasswordRequestState = RequestState.Error(result.second)
                passwordState = passwordState.copy(errorMessage = result.second)
            }
        }
    }

    private fun sendPasswordResetLink() {
        val emailToUse = passwordState.email.ifEmpty { getEmail() }

        // Validate email
        if (!isValidEmail(emailToUse)) {
            passwordState = passwordState.copy(
                errorMessage = if (emailToUse.isEmpty()) {
                    "Please enter your email address"
                } else {
                    "Please enter a valid email address"
                }
            )
            return
        }

        sendResetLinkRequestState = RequestState.Loading
        passwordState = passwordState.copy(errorMessage = "", successMessage = "")

        viewModelScope.launch(exceptionHandler) {
            val result = withContext(Dispatchers.IO) {
                var success = false
                var successMsg = ""
                var errorMsg = ""

                userRepository.sendOTPVerificationEmailForForgottenPassword(
                    email = emailToUse,
                    onSuccess = { message ->
                        success = true
                        successMsg = message
                        println("✅ Password reset email sent successfully to: $emailToUse")
                    },
                    onError = { message ->
                        errorMsg = message
                        println("❌ Failed to send password reset email: $message")
                    }
                )

                Triple(success, successMsg, errorMsg)
            }

            // Update state on Main thread
            if (result.first) {
                sendResetLinkRequestState = RequestState.Success(Unit)
                passwordState = passwordState.copy(
                    successMessage = result.second
                )

                println("🔄 About to trigger navigation callback with email: $emailToUse")

                // Trigger callback for navigation
                onPasswordResetEmailSentCallback?.let { callback ->
                    println("🚀 Invoking navigation callback with email: $emailToUse")
                    callback(emailToUse)
                } ?: println("⚠️ Navigation callback is null!")
            } else {
                sendResetLinkRequestState = RequestState.Error(result.third)
                passwordState = passwordState.copy(errorMessage = result.third)
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() &&
                email.contains("@") &&
                email.contains(".") &&
                email.indexOf("@") > 0 &&
                email.lastIndexOf(".") > email.indexOf("@") &&
                email.length >= 5
    }

    fun updateChangePasswordRequestState(value: RequestState<Unit>) {
        changePasswordRequestState = value
    }

    fun updateSendResetLinkRequestState(value: RequestState<Unit>) {
        sendResetLinkRequestState = value
    }
}

data class ChangePasswordState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val email: String = "",
    val showCurrentPassword: Boolean = false,
    val showNewPassword: Boolean = false,
    val showConfirmPassword: Boolean = false,
    val isChangePasswordMode: Boolean = true,
    val errorMessage: String = "",
    val successMessage: String = ""
)

sealed interface ChangePasswordAction {
    data class OnCurrentPasswordChange(val password: String) : ChangePasswordAction
    data class OnNewPasswordChange(val password: String) : ChangePasswordAction
    data class OnConfirmPasswordChange(val password: String) : ChangePasswordAction
    data class OnEmailChange(val email: String) : ChangePasswordAction
    data object ToggleCurrentPasswordVisibility : ChangePasswordAction
    data object ToggleNewPasswordVisibility : ChangePasswordAction
    data object ToggleConfirmPasswordVisibility : ChangePasswordAction
    data class SwitchMode(val isChangePasswordMode: Boolean) : ChangePasswordAction
    data object ChangePassword : ChangePasswordAction
    data object SendResetLink : ChangePasswordAction
    data object ClearMessages : ChangePasswordAction
}
