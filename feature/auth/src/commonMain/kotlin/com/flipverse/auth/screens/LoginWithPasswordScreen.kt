package com.flipverse.auth.screens

import ContentWithMessageBar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.flipverse.auth.component.PasswordSection
import com.flipverse.auth.component.SubmitButton
import com.flipverse.shared.BlackLight
import com.flipverse.shared.Resources.Icon.BackArrow
import com.flipverse.shared.WorkSansBoldFont
import com.flipverse.shared.presentation.component.FlipButton
import com.flipverse.shared.presentation.component.FlipPrivacyPolicyLabel
import com.flipverse.shared.presentation.component.FlipTextField
import com.flipverse.shared.presentation.ui.ObserveAsEvents
import com.flipverse.shared.Strings
import openWebBrowser
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import rememberMessageBarState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginWithPasswordScreen(
    onBackClicked: () -> Unit,
    navigateToSignUpWithEmail: () -> Unit,
    navigateToDashboard: () -> Unit,
    navigateToForgotPassword: () -> Unit,
) {


    val viewModel = koinViewModel<AuthViewModel>()
    val authState = viewModel.authState
    val onAction = viewModel::onAction
    val messageBarState = rememberMessageBarState()
    val snackbarHostState = remember { SnackbarHostState() }



    ObserveAsEvents(viewModel.event) { event ->

        when (event) {
            is AuthEvent.Error -> {
                messageBarState.addError(event.error)
            }

            is AuthEvent.LoginSuccess -> {
                navigateToDashboard()
            }

            is AuthEvent.Navigate.ForgotPassword -> navigateToForgotPassword()
            is AuthEvent.Navigate.VerifyEmail -> navigateToSignUpWithEmail()
            is AuthEvent.Navigate.CreateProfile -> {}
            is AuthEvent.Navigate.SignUpWithPassword -> {

            }

            AuthEvent.Navigate.VerifyOtp -> {

            }

            AuthEvent.Navigate.Dashboard -> {}
            else -> {}
        }
    }

    val systemBarPaddingValues = WindowInsets.systemBars.asPaddingValues()


    Scaffold(
        contentWindowInsets = WindowInsets(
            top = systemBarPaddingValues.calculateTopPadding(),
            bottom = systemBarPaddingValues.calculateBottomPadding(),
        ),
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(modifier = Modifier.padding(6.dp), onClick = { onBackClicked() }) {
                        Icon(
                            imageVector = vectorResource(resource = BackArrow),
                            contentDescription = Strings.back,
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
            errorMaxLines = 2,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
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

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = Strings.log_in_to_flipverse,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        fontFamily = FontFamily.SansSerif,
                    ),
                    color = MaterialTheme.colorScheme.onPrimary,
                )

                Spacer(modifier = Modifier.height(32.dp))

                FlipTextField(
                    value = authState.emailId,
                    onValueChange = { newEmail -> onAction(AuthAction.OnEmailIdChange(newEmail)) },
                    label = Strings.email_address
                )


                Spacer(modifier = Modifier.height(16.dp))

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

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = Strings.forgot_password,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.tertiary
                        ),
                        modifier = Modifier
                            .clickable { onAction(AuthAction.OnForgotPasswordClick) }
                            .padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Spacer(modifier = Modifier.weight(1f))

                SubmitButton(
                    enabled = authState.hasMinLength &&
                            authState.emailId.isNotEmpty() &&
                            authState.password.isNotEmpty(),
                    Strings.continue_label,
                    onButtonClicked = {
                        onAction(AuthAction.OnLoginClick)
                    },
                    state = authState
                )

                Spacer(modifier = Modifier.height(16.dp))

                FlipButton(
                    text = "Sign Up with email",
                    enabled = true,
                    onClick = { onAction(AuthAction.OnSignUpContinue) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )


                Spacer(modifier = Modifier.height(24.dp))

                FlipPrivacyPolicyLabel(
                    onPrivacyPolicyClick = {
                        openWebBrowser("https://flipverse.app/start/legal-hub.html#privacy")
                    },
                    onTermsOfUseClick = {
                        openWebBrowser("https://flipverse.app/start/legal-hub.html#terms")
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

            }
        }
    }
}
