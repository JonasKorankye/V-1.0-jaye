package com.flipverse.userprofile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flipverse.shared.BlackLight
import com.flipverse.shared.CoffeeDark
import com.flipverse.shared.PreferencesRepository.getEmail
import com.flipverse.shared.Resources
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVerifyOTP: (String) -> Unit = {},
    initialIsChangePasswordMode: Boolean = true,
    showModeToggle: Boolean = true,
) {
    val viewModel = koinViewModel<ChangePasswordViewModel>()
    val state = viewModel.passwordState
    val onAction = viewModel::onAction

    val isLoading = viewModel.changePasswordRequestState.isLoading() ||
            viewModel.sendResetLinkRequestState.isLoading()

    LaunchedEffect(Unit) {
        println("📱 ChangePasswordScreen: Setting up navigation callback")
        viewModel.setOnPasswordResetEmailSentCallback { email ->
            println("📱 ChangePasswordScreen: Callback received with email: $email")
            println("📱 ChangePasswordScreen: About to call onNavigateToVerifyOTP")
            onNavigateToVerifyOTP(email)
            println("📱 ChangePasswordScreen: onNavigateToVerifyOTP called")
        }
    }

    LaunchedEffect(initialIsChangePasswordMode) {
        onAction(ChangePasswordAction.SwitchMode(initialIsChangePasswordMode))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isChangePasswordMode) "Change Password" else "Reset Password",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(
                            imageVector = vectorResource(Resources.Icon.BackArrow),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.primary
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary)
                .verticalScroll(rememberScrollState())
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding()
                )
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (showModeToggle) {
                // Mode Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Change Password Tab
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onAction(ChangePasswordAction.SwitchMode(true)) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (state.isChangePasswordMode) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                }
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Change Password",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                textAlign = TextAlign.Center,
                                color = if (state.isChangePasswordMode) BlackLight else MaterialTheme.colorScheme.onPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (state.isChangePasswordMode) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        // Forgot Password Tab
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onAction(ChangePasswordAction.SwitchMode(false)) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (!state.isChangePasswordMode) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                }
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Forgot Password",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                textAlign = TextAlign.Center,
                                color = if (state.isChangePasswordMode) BlackLight else MaterialTheme.colorScheme.onPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (!state.isChangePasswordMode) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (state.isChangePasswordMode) {
                // Change Password Form
                Text(
                    text = "Change your password",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Enter your current password and choose a new one.",
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Current Password Field
                OutlinedTextField(
                    value = state.currentPassword,
                    onValueChange = {
                        onAction(ChangePasswordAction.OnCurrentPasswordChange(it))
                    },
                    label = { Text("Current Password") },
                    visualTransformation = if (state.showCurrentPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { onAction(ChangePasswordAction.ToggleCurrentPasswordVisibility) }) {
                            Icon(
                                painter = painterResource(
                                    if (state.showCurrentPassword) Resources.Icon.VisibilityOff else Resources.Icon.VisibilityOn
                                ),
                                contentDescription = if (state.showCurrentPassword) "Hide password" else "Show password",
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSecondary,
                        focusedLabelColor = MaterialTheme.colorScheme.primaryContainer,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSecondary,
                        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        cursorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // New Password Field
                OutlinedTextField(
                    value = state.newPassword,
                    onValueChange = {
                        onAction(ChangePasswordAction.OnNewPasswordChange(it))
                    },
                    label = { Text("New Password") },
                    visualTransformation = if (state.showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { onAction(ChangePasswordAction.ToggleNewPasswordVisibility) }) {
                            Icon(
                                painter = painterResource(
                                    if (state.showNewPassword) Resources.Icon.VisibilityOff else Resources.Icon.VisibilityOn
                                ),
                                contentDescription = if (state.showNewPassword) "Hide password" else "Show password",
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSecondary,
                        focusedLabelColor = MaterialTheme.colorScheme.primaryContainer,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSecondary,
                        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        cursorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Password Field
                OutlinedTextField(
                    value = state.confirmPassword,
                    onValueChange = {
                        onAction(ChangePasswordAction.OnConfirmPasswordChange(it))
                    },
                    label = { Text("Confirm New Password") },
                    visualTransformation = if (state.showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { onAction(ChangePasswordAction.ToggleConfirmPasswordVisibility) }) {
                            Icon(
                                painter = painterResource(
                                    if (state.showConfirmPassword) Resources.Icon.VisibilityOff else Resources.Icon.VisibilityOn
                                ),
                                contentDescription = if (state.showConfirmPassword) "Hide password" else "Show password",
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSecondary,
                        focusedLabelColor = MaterialTheme.colorScheme.primaryContainer,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSecondary,
                        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        cursorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Change Password Button
                Button(
                    onClick = {
                        onAction(ChangePasswordAction.ChangePassword)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isLoading) "Changing..." else "Change Password",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                
            } else {
                // Forgot Password Form
                Text(
                    text = "Forgot your password?",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Enter your email address and we'll send you a code to reset your password.",
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Email Field (pre-filled with user's email)
                OutlinedTextField(
                    value = state.email.ifEmpty { getEmail() },
                    onValueChange = {
                        onAction(ChangePasswordAction.OnEmailChange(it))
                    },
                    label = { Text("Email Address") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSecondary,
                        focusedLabelColor = MaterialTheme.colorScheme.primaryContainer,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSecondary,
                        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        cursorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Send Reset Code Button
                Button(
                    onClick = {
                        onAction(ChangePasswordAction.SendResetLink)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isLoading) "Sending..." else "Send Reset Code",
                        color = BlackLight,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error Message
            if (state.errorMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = state.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Success Message
            if (state.successMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Green.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = state.successMessage,
                        color = CoffeeDark,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Password Requirements Card
            if (state.isChangePasswordMode) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Password Requirements",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val requirements = listOf(
                            "At least 8 characters long",
                            "Contains uppercase and lowercase letters",
                            "Contains at least one number",
                            "Contains at least one special character",
                            "Different from your current password"
                        )

                        requirements.forEach { requirement ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(Resources.Icon.Checkmark),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = requirement,
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Help Text
            if (!state.isChangePasswordMode && showModeToggle) {
                Text(
                    text = "Remember your password?",
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "Back to Change Password",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAction(ChangePasswordAction.SwitchMode(true)) }
                        .padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.padding(64.dp))
        }
    }
}
