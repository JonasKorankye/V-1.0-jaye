package com.flipverse.auth.screens

import ContentWithMessageBar
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flipverse.auth.AuthViewModel
import com.flipverse.auth.component.AppleButton
import com.flipverse.auth.component.GoogleButton
import com.flipverse.shared.BlackLight
import com.flipverse.shared.PreferencesRepository.getFirstTimeLoginStatus
import com.flipverse.shared.PreferencesRepository.saveTermsAccepted
import com.flipverse.shared.Resources
import com.flipverse.shared.presentation.component.FlipButton
import com.flipverse.shared.presentation.component.FlipPrivacyPolicyLabel
import com.flipverse.shared.util.PlatformType
import com.flipverse.shared.util.getPlatformType
import com.mmk.kmpauth.firebase.apple.AppleButtonUiContainer
import com.mmk.kmpauth.firebase.apple.AppleSignInRequestScope
import com.mmk.kmpauth.firebase.google.GoogleButtonUiContainerFirebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import openWebBrowser
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import rememberMessageBarState

private data class ConsentSlide(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val bullets: List<String>
)

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
                title = {},
                navigationIcon = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.primary
    ) { padding ->
        ContentWithMessageBar(
            contentBackgroundColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = padding.calculateTopPadding()),
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
                Spacer(modifier = Modifier.height(32.dp))

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

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FlipButton(
                        text = "Continue with Email",
                        onClick = {
                            navigateToLogin()
                        },
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
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    loadingState = true
                                    this@GoogleButtonUiContainerFirebase.onClick()
                                }
                            )
                        }
                    } else if (platformType == PlatformType.IOS) {
                        AppleButtonUiContainer(
                            requestScopes = listOf(
                                AppleSignInRequestScope.FullName,
                                AppleSignInRequestScope.Email
                            ),
                            linkAccount = false,
                            onResult = { result ->
                                result.onSuccess { user ->
                                    viewModel.preloadSocialIdentity(user)
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
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    loadingState = true
                                    this@AppleButtonUiContainer.onClick()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

//                Surface(
//                    modifier = Modifier.fillMaxWidth(),
//                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.16f),
//                    shape = RoundedCornerShape(18.dp)
//                ) {
//                    Column(
//                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
//                    ) {
//                        Text(
//                            text = "You're all set to continue.",
//                            color = MaterialTheme.colorScheme.onPrimary,
//                            fontSize = 13.sp,
//                            fontWeight = FontWeight.SemiBold
//                        )
//                        Spacer(modifier = Modifier.height(4.dp))
//                        Text(
//                            text = "A quick swipe-through explains community safety, privacy, and the expectations behind your first sign-in.",
//                            color = MaterialTheme.colorScheme.onSecondary,
//                            fontSize = 12.sp,
//                            lineHeight = 18.sp
//                        )
//                    }
//                }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsentGateScreen(
    onAgree: () -> Unit
) {
    PremiumConsentGateScreen(
        onAgree = {
            saveTermsAccepted(true)
            onAgree()
        },
        onOpenPrivacyPolicy = {
            openWebBrowser("https://flipverse.app/start/legal-hub.html#privacy")
        },
        onOpenTerms = {
            openWebBrowser("https://flipverse.app/start/legal-hub.html#terms")
        }
    )
}

@Composable
private fun PremiumConsentGateScreen(
    onAgree: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTerms: () -> Unit
) {
    val slides = remember {
        listOf(
            ConsentSlide(
                title = "A calmer way to connect",
                description = "FlipVerse is built for thoughtful conversations, not noise. We ask every new member to enter with that same mindset.",
                icon = Icons.Outlined.LocalLibrary,
                accentColor = Color(0xFF7C5CFF),
                bullets = listOf(
                    "Choose quality over clutter",
                    "Treat people like people",
                    "Keep conversations constructive"
                )
            ),
            ConsentSlide(
                title = "Safety is part of the design",
                description = "Reports help us protect the space. Harmful behavior and objectionable content may be reviewed, removed, blocked, or deleted.",
                icon = Icons.Outlined.VerifiedUser,
                accentColor = Color(0xFFFF7A59),
                bullets = listOf(
                    "Abuse can be reported",
                    "Content can be moderated",
                    "Bad actors can lose access"
                )
            ),
            ConsentSlide(
                title = "Privacy, clearly stated",
                description = "By continuing, you agree to the Terms of Use and Privacy Policy that explain how the platform works and how your data is handled.",
                icon = Icons.Outlined.Gavel,
                accentColor = Color(0xFF00A88F),
                bullets = listOf(
                    "Review Terms of Use",
                    "Review Privacy Policy",
                    "Continue only if you agree"
                )
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    val isIos = getPlatformType() == PlatformType.IOS
    val horizontalPadding = if (isIos) 28.dp else 24.dp
    val topSpacing = if (isIos) 24.dp else 16.dp
    val bottomSpacing = if (isIos) 28.dp else 20.dp
    val heroCardPadding = if (isIos) 22.dp else 20.dp
    val ctaTopSpacing = if (isIos) 14.dp else 10.dp
    val titleSpacing = if (isIos) 28.dp else 22.dp
    val screenInsets = WindowInsets.systemBars.asPaddingValues()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primary,
        contentWindowInsets = WindowInsets(
            top = screenInsets.calculateTopPadding(),
            bottom = screenInsets.calculateBottomPadding(),
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                MaterialTheme.colorScheme.primary
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = screenInsets.calculateTopPadding())
                    .padding(bottom = screenInsets.calculateBottomPadding())
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = horizontalPadding, vertical = topSpacing)
                    .padding(bottom = bottomSpacing)
            ) {
                Spacer(modifier = Modifier.height(titleSpacing))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Welcome to FlipVerse",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(if (isIos) 8.dp else 6.dp))
                        AnimatedContent(targetState = pagerState.currentPage) { page ->
                            Text(
                                text = "Step ${page + 1} of ${slides.size}",
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(slides.size) { index ->
                            val selected = pagerState.currentPage == index
                            val width by animateFloatAsState(
                                targetValue = if (selected) 24f else 8f,
                                label = "pager_indicator"
                            )
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(width.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) slides[index].accentColor
                                        else MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.20f)
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isIos) 22.dp else 18.dp))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) { page ->
                    val slide = slides[page]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = slide.accentColor.copy(alpha = 0.12f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(heroCardPadding)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(slide.accentColor.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = slide.icon,
                                    contentDescription = slide.title,
                                    tint = slide.accentColor
                                )
                            }

                            Spacer(modifier = Modifier.height(if (isIos) 20.dp else 18.dp))

                            Text(
                                text = slide.title,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = slide.description,
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontSize = 14.sp,
                                lineHeight = 21.sp
                            )

                            Spacer(modifier = Modifier.height(if (isIos) 20.dp else 18.dp))

                            slide.bullets.forEach { bullet ->
                                ConsentBullet(
                                    text = bullet,
                                    accentColor = slide.accentColor
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isIos) 24.dp else 18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = onOpenPrivacyPolicy
                    ) {
                        Text("Privacy Policy",color = MaterialTheme.colorScheme.onPrimary)
                    }
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = onOpenTerms
                    ) {
                        Text("Terms of Use",color = MaterialTheme.colorScheme.onPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(ctaTopSpacing))

                if (pagerState.currentPage < slides.lastIndex) {
                    FlipButton(
                        text = "Next",
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    FlipButton(
                        text = "I Agree, Continue",
                        onClick = onAgree,
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(if (isIos) 14.dp else 10.dp))

                Text(
                    text = "Review the steps above, then continue when you're ready.",
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ConsentBullet(
    text: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
