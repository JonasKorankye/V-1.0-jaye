package com.flipverse.auth.screens

import ContentWithMessageBar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
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
import com.flipverse.auth.component.SubmitButton
import com.flipverse.shared.LexendMediumFont
import com.flipverse.shared.Resources.Icon.BackArrow
import com.flipverse.shared.WorkSansBoldFont
import com.flipverse.shared.WorkSansFont
import com.flipverse.shared.Strings
import com.flipverse.shared.presentation.component.FlipButton
import com.flipverse.shared.presentation.component.FlipPrivacyPolicyLabel
import com.flipverse.shared.presentation.component.FlipTextField
import com.flipverse.shared.presentation.ui.ObserveAsEvents
import openWebBrowser
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import rememberMessageBarState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpWithEmailScreen(
    onBackClicked: () -> Unit,
    navigateToVerifyEmail: () -> Unit,
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

            is AuthEvent.LoginSuccess -> {}

            is AuthEvent.Navigate.VerifyEmail -> {}
            is AuthEvent.Navigate.VerifyOtp -> {
                navigateToVerifyEmail()
            }

            is AuthEvent.Navigate.CreateProfile -> {}
            is AuthEvent.Navigate.SignUpWithPassword -> {

            }

            AuthEvent.Navigate.Dashboard -> {

            }

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
                    IconButton(onClick = { onBackClicked() }) {
                        Icon(
                            imageVector = vectorResource(BackArrow),
                            contentDescription = Strings.back,
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
            successContentColor = MaterialTheme.colorScheme.onPrimary,
            showCopyButton = false
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .background(MaterialTheme.colorScheme.primary)
            ) {

                Text(
                    text = Strings.whats_your_email,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 30.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                FlipTextField(
                    value = authState.emailId,
                    onValueChange = { newEmail -> onAction(AuthAction.OnEmailIdChange(newEmail)) },
                    label = Strings.email_address
                )

                Text(
                    text = Strings.well_send_code,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = WorkSansFont()),
                    color = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

//                FlipButton(
//                    text = "Continue",
//                    enabled = authState.hasEmailCharacters,
//                    onClick = { navigateToVerifyEmail() },
//                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer
//                )
                authState.isLoading
                SubmitButton(
                    enabled = authState.hasEmailCharacters,
                    Strings.continue_label,
                    onButtonClicked = {
                        onAction(AuthAction.OnSendOTPMailClick)
                    },
                    state = authState
                )

                Spacer(modifier = Modifier.height(16.dp))

                FlipPrivacyPolicyLabel(
                    onPrivacyPolicyClick = {
                        openWebBrowser("https://flipverse.app/start/legal-hub.html#privacy")
                    },
                    onTermsOfUseClick = {
                        openWebBrowser("https://flipverse.app/start/legal-hub.html#terms")
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))

            }
        }
    }
}
