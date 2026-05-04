package com.flipverse.auth.screens

import ContentWithMessageBar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flipverse.auth.AuthAction
import com.flipverse.auth.AuthEvent
import com.flipverse.auth.AuthViewModel
import com.flipverse.auth.component.ConfirmPasswordSection
import com.flipverse.auth.component.PasswordRequirements
import com.flipverse.auth.component.PasswordSection
import com.flipverse.auth.component.SubmitButton
import com.flipverse.shared.BlackLight
import com.flipverse.shared.LexendMediumFont
import com.flipverse.shared.Resources.Icon.BackArrow
import com.flipverse.shared.WorkSansBoldFont
import com.flipverse.shared.presentation.ui.ObserveAsEvents
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import rememberMessageBarState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpWithPasswordScreen(
    onBackClicked: () -> Unit,
    navigateToCreateProfile: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }

    val viewModel = koinViewModel<AuthViewModel>()
    val authState = viewModel.authState
    val onAction = viewModel::onAction
    val messageBarState = rememberMessageBarState()

    ObserveAsEvents(viewModel.event) { event ->
        when (event) {
            is AuthEvent.Error -> {
                messageBarState.addError(event.error)
            }

            is AuthEvent.LoginSuccess -> {
            }

            is AuthEvent.Navigate.VerifyEmail -> {}
            is AuthEvent.Navigate.CreateProfile -> navigateToCreateProfile()
            AuthEvent.Navigate.SignUpWithPassword -> {
            }

            AuthEvent.Navigate.VerifyOtp -> { }
            AuthEvent.Navigate.Dashboard -> { }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { onBackClicked() }) {
                        Icon(
                            imageVector = vectorResource(BackArrow),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
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
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Set your password",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 30.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                PasswordSection(
                    authState = authState,
                    onPasswordChange = { newPassword ->
                        onAction(
                            AuthAction.OnPasswordChange(
                                newPassword
                            )
                        )
                    },
                    onVisibilityChange = { onVisible ->
                        onAction(
                            AuthAction.OnTogglePasswordVisibilityClick(
                                onVisible
                            )
                        )
                    }
                )

                PasswordRequirements(authState)

                Spacer(modifier = Modifier.height(24.dp))

                ConfirmPasswordSection(
                    confirmPasswordState = authState,
                    onConfirmPasswordChange = { newConfirmPassword ->
                        onAction(
                            AuthAction.OnConfirmPasswordChange(
                                newConfirmPassword
                            )
                        )
                    },
                    onVisibilityChange = { isVisible ->
                        onAction(
                            AuthAction.OnToggleConfirmPasswordVisibilityClick(
                                isVisible
                            )
                        )
                    }
                )
                Spacer(modifier = Modifier.weight(1f))
                SubmitButton(
                    enabled = authState.isValid &&
                            authState.password == authState.confirmPassword &&
                            authState.password.isNotEmpty(),
                    label = "Continue",
                    onButtonClicked = { onAction(AuthAction.OnSetUpPasswordClick) },
                    state = authState
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
