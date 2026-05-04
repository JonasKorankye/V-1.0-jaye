package com.flipverse.userprofile

import ContentWithMessageBar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flipverse.data.domain.UserRepository
import com.flipverse.data.util.maskEmailSimple
import com.flipverse.shared.BlackLight
import com.flipverse.shared.Resources
import com.flipverse.shared.WorkSansBoldFont
import com.flipverse.shared.presentation.component.OtpTextField
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import rememberMessageBarState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyPasswordResetScreen(
    email: String,
    onBackClicked: () -> Unit,
    onVerificationSuccess: () -> Unit,
) {
    var otpValue by remember { mutableStateOf(TextFieldValue("")) }
    var isLoading by remember { mutableStateOf(false) }

    // Timer state
    var timeLeft by rememberSaveable { mutableStateOf(60) } // 60 seconds for OTP
    var canResend by rememberSaveable { mutableStateOf(false) }
    var resendText by rememberSaveable { mutableStateOf("Resend Code") }

    val scope = rememberCoroutineScope()
    val messageBarState = rememberMessageBarState()
    val userRepository: UserRepository = koinInject()

    val maskedEmail = email.maskEmailSimple()

    LaunchedEffect(timeLeft) {
        if (timeLeft > 0 && !canResend) {
            delay(1000L)
            timeLeft--
        } else if (timeLeft == 0) {
            canResend = true
        }
    }

    fun performVerification() {
        if (otpValue.text.length != 6 || isLoading) return
        
        isLoading = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    var success = false
                    var errorMsg = ""

                    userRepository.verifyOTP(
                        email = email,
                        otp = otpValue.text,
                        purpose = "PasswordReset",
                        onSuccess = { success = true },
                        onError = { message -> errorMsg = message }
                    )

                    Pair(success, errorMsg)
                }

                if (result.first) {
                    messageBarState.addSuccess("Verification Successful!")
                    delay(1500)
                    onVerificationSuccess()
                } else {
                    messageBarState.addError(result.second)
                    isLoading = false
                }
            } catch (e: Exception) {
                messageBarState.addError(e.message ?: "An unexpected error occurred")
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { onBackClicked() }) {
                        Icon(
                            imageVector = vectorResource(Resources.Icon.BackArrow),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = MaterialTheme.colorScheme.primary
    ) { paddingValues ->
        ContentWithMessageBar(
            contentBackgroundColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding()
                ),
            messageBarState = messageBarState,
            fontFamily = FontFamily.SansSerif,
            errorMaxLines = 2,
            errorContainerColor = MaterialTheme.colorScheme.error,
            errorContentColor = MaterialTheme.colorScheme.onErrorContainer,
            successContainerColor = MaterialTheme.colorScheme.onSecondaryContainer,
            successContentColor = BlackLight,
            showCopyButton = false
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .background(MaterialTheme.colorScheme.primary),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Verify Password Reset",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 30.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "We sent a 6-digit code to $maskedEmail.\nEnter the code below to verify and reset your password.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                // OTP Input Fields
                OtpTextField(
                    otpText = otpValue.text,
                    onOtpTextChange = { newOtp ->
                        if (newOtp.length <= 6) { // Limit to 6 digits
                            otpValue = TextFieldValue(newOtp, TextRange(newOtp.length))
                        }
                    },
                    onDone = {
                        performVerification()
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Timer and Resend option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Didn't receive the code?",
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    if (canResend) {
                        Text(
                            text = resendText,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                // Logic to resend OTP
                                timeLeft = 60 // Reset timer
                                canResend = false // Disable resend until timer runs out
                                otpValue = TextFieldValue("") // Clear OTP field

                                if (resendText == "Resend Code") {
                                    resendText = "Resending..." // Immediately update text on click
                                    scope.launch {
                                        try {
                                            val result = withContext(Dispatchers.IO) {
                                                var success = false
                                                var msg = ""

                                                userRepository.sendPasswordResetEmail(
                                                    email = email,
                                                    onSuccess = { message ->
                                                        success = true
                                                        msg = message
                                                    },
                                                    onError = { message ->
                                                        msg = message
                                                    }
                                                )

                                                Pair(success, msg)
                                            }

                                            // Update state on Main thread
                                            if (result.first) {
                                                messageBarState.addSuccess(result.second)
                                                resendText = "Resend Code"
                                            } else {
                                                messageBarState.addError(result.second)
                                                resendText = "Resend Code"
                                                canResend = true
                                            }
                                        } catch (e: Exception) {
                                            messageBarState.addError(e.message ?: "Failed to resend code")
                                            resendText = "Resend Code"
                                            canResend = true
                                        }
                                    }
                                }
                            }
                        )
                    } else {
                        Text(
                            text = "Resend in $timeLeft s",
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Verify Button
                Button(
                    onClick = { performVerification() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(size = 99.dp)
                        ),
                    enabled = otpValue.text.length == 6 && !isLoading, // Enable only when 6 digits are entered
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        disabledContainerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    AnimatedVisibility(visible = !isLoading) {
                        Text(
                            "Verify",
                            color = if (otpValue.text.length == 6) BlackLight else MaterialTheme.colorScheme.onPrimary,
                            fontSize = 18.sp,
                            fontFamily = WorkSansBoldFont()
                        )
                    }

                    AnimatedVisibility(visible = isLoading) {
                        AdaptiveCircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
