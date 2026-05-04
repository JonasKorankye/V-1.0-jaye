package com.flipverse.auth.screens

import ContentWithMessageBar
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flipverse.auth.AuthViewModel
import com.flipverse.auth.component.AppleButton
import com.flipverse.auth.component.GoogleButton
import com.flipverse.shared.BlackLight
import com.flipverse.shared.FontSize
import com.flipverse.shared.PreferencesRepository.getFirstTimeLoginStatus
import com.flipverse.shared.Resources
import com.flipverse.shared.WorkSansBoldFont
import com.flipverse.shared.presentation.component.FlipButton
import com.flipverse.shared.presentation.component.FlipPrivacyPolicyLabel
import com.flipverse.shared.util.PlatformType
import com.flipverse.shared.util.getPlatformType
import com.mmk.kmpauth.firebase.apple.AppleButtonUiContainer
import com.mmk.kmpauth.firebase.google.GoogleButtonUiContainerFirebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import rememberMessageBarState
import openWebBrowser



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    navigateToDashboard: () -> Unit,
    navigateToCreateProfile: () -> Unit,
    navigateToLogin: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val viewModel = koinViewModel<AuthViewModel>()
    var loadingState by rememberSaveable { mutableStateOf(false) }
    val messageBarState = rememberMessageBarState()
    val platformType = getPlatformType()

    val systemBarPaddingValues = WindowInsets.systemBars.asPaddingValues()


    Scaffold(
        contentWindowInsets = WindowInsets(
            top = systemBarPaddingValues.calculateTopPadding(),
            bottom = systemBarPaddingValues.calculateBottomPadding(),
        ),
        topBar = {
            TopAppBar(
                title = {
//                    Box(
//                        modifier = Modifier
//                            .fillMaxWidth(),
//                        contentAlignment = Alignment.Center
//
//                    ) {
//                        Image(
//                            painter = painterResource(if (isSystemInDarkTheme()) Resources.Image.AppLogoFullDark else Resources.Image.AppLogoFullWhite),
//                            contentDescription = "App Logo",
//                            modifier = Modifier
//                                .wrapContentSize()
//                                .height(48.dp),
//
//                            )
//                    }
                },
                navigationIcon = {
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = MaterialTheme.colorScheme.primary// Screen background color
    ) { padding ->
        ContentWithMessageBar(
            contentBackgroundColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(
                    top = padding.calculateTopPadding(),
                ),
            messageBarState = messageBarState,
            errorMaxLines = 2,
            fontFamily = FontFamily.SansSerif,
            errorContainerColor = MaterialTheme.colorScheme.error,
            errorContentColor = MaterialTheme.colorScheme.onErrorContainer,
            successContainerColor = MaterialTheme.colorScheme.onSecondaryContainer,
            successContentColor = BlackLight,
            showCopyButton = false
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top spacer for balance
                Spacer(modifier = Modifier.height(32.dp))

                // Logo and tagline section - flexible space
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(
                            if (isSystemInDarkTheme()) Resources.Image.AppLogoFullDark
                            else Resources.Image.AppLogoFullWhite
                        ),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .wrapContentSize()
                            .height(80.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Rediscover Meaningful Engagement",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Buttons section - fixed space
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FlipButton(
                        text = "Continue with Email",
                        enabled = true,
                        onClick = { navigateToLogin() },
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (platformType == PlatformType.Android) {
                        GoogleButtonUiContainerFirebase(
                            linkAccount = false,
                            onResult = { result ->
                                result.onSuccess { user ->
                                    viewModel.createUser(
                                        fvUser = user,
                                        onSuccess = {
                                            scope.launch {
                                                try {
                                                    messageBarState.addSuccess("Authentication Successful!")
                                                    delay(2000)
                                                    if (getFirstTimeLoginStatus()) {
                                                        navigateToCreateProfile()
                                                    } else {
                                                        navigateToDashboard()
                                                    }
                                                } catch (e: Exception) {
                                                    println("Navigation error after Google sign-in: ${e.message}")
                                                    messageBarState.addError("Navigation failed. Please try again.")
                                                } finally {
                                                    loadingState = false
                                                }
                                            }
                                        },
                                        onError = { message ->
                                            scope.launch {
                                                messageBarState.addError(message)
                                                loadingState = false
                                            }
                                        },
                                        platformType = platformType.toString()
                                    )
                                }.onFailure { error ->
                                    scope.launch {
                                        if (error.message?.contains("A network error") == true) {
                                            messageBarState.addError("Internet Connection unavailable.")
                                        } else if (error.message?.contains("Idtoken is null") == true) {
                                            messageBarState.addError("Sign In Cancelled.")
                                        } else {
                                            messageBarState.addError(
                                                error.message ?: com.flipverse.shared.Strings.unknown
                                            )
                                        }
                                        loadingState = false
                                    }
                                }
                            }
                        ) {
                            GoogleButton(
                                loading = loadingState,
                                modifier = Modifier
                                    .fillMaxWidth(),
                                onClick = {
                                    loadingState = true
                                    this@GoogleButtonUiContainerFirebase.onClick()
                                }
                            )
                        }
                    } else if (platformType == PlatformType.IOS) {
                        AppleButtonUiContainer(
                            linkAccount = false,
                            onResult = { result ->
                                result.onSuccess { user ->
                                    viewModel.createUser(
                                        fvUser = user,
                                        onSuccess = {
                                            scope.launch {
                                                try {
                                                    messageBarState.addSuccess("Authentication Successful!")
                                                    delay(2000)
                                                    if (getFirstTimeLoginStatus()) {
                                                        navigateToCreateProfile()
                                                    } else {
                                                        navigateToDashboard()
                                                    }
                                                } catch (e: Exception) {
                                                    println("Navigation error after Apple sign-in: ${e.message}")
                                                    messageBarState.addError("Navigation failed. Please try again.")
                                                } finally {
                                                    loadingState = false
                                                }
                                            }
                                        },
                                        onError = { message ->
                                            scope.launch {
                                                messageBarState.addError(message)
                                                loadingState = false
                                            }
                                        },
                                        platformType = platformType.toString()
                                    )
                                }.onFailure { error ->
                                    scope.launch {
                                        println("AppleSignIn Error: ${error::class.simpleName}: ${error.message}")
                                        println("AppleSignIn Error cause: ${error.cause}")
                                        val errorMsg = error.message
                                            ?: error.cause?.message
                                            ?: error.toString()
                                        if (errorMsg.contains("network error", ignoreCase = true)) {
                                            messageBarState.addError("Internet Connection unavailable.")
                                        } else if (errorMsg.contains("cancel", ignoreCase = true)) {
                                            messageBarState.addError("Sign In Cancelled.")
                                        } else {
                                            messageBarState.addError(errorMsg)
                                        }
                                        loadingState = false
                                    }
                                }
                            }
                        ) {
                            AppleButton(
                                loading = loadingState,
                                modifier = Modifier
                                    .fillMaxWidth(),
                                onClick = {
                                    loadingState = true
                                    this@AppleButtonUiContainer.onClick()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Terms and Privacy Policy at bottom
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
