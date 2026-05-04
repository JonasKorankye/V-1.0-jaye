package com.flipverse.auth.screens

import ContentWithMessageBar
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flipverse.auth.AuthAction
import com.flipverse.auth.AuthEvent
import com.flipverse.auth.AuthViewModel
import com.flipverse.shared.Resources
import com.flipverse.shared.presentation.component.FlipButton
import com.flipverse.shared.presentation.component.FlipTextField
import com.flipverse.shared.presentation.ui.ObserveAsEvents
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import rememberMessageBarState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateUsernameScreen(
    navigateToChooseInterests: () -> Unit
) {
    val viewModel = koinViewModel<AuthViewModel>()
    val authState = viewModel.authState
    val onAction = viewModel::onAction
    val messageBarState = rememberMessageBarState()

    ObserveAsEvents(viewModel.event) { event ->
        when (event) {
            is AuthEvent.Error -> messageBarState.addError(event.error)
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { /* No title text in TopAppBar */ },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center

                    ) {
                        Image(
                            painter = painterResource(if (isSystemInDarkTheme()) Resources.Image.AppLogoFullDark else Resources.Image.AppLogoFullWhite),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .wrapContentSize()
                                .height(48.dp),

                            )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = MaterialTheme.colorScheme.primary // Screen background color
    ) { paddingValues ->
        ContentWithMessageBar(
            contentBackgroundColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(paddingValues),
            messageBarState = messageBarState,
            errorMaxLines = 2,
            showCopyButton = false,
            errorContainerColor = MaterialTheme.colorScheme.error,
            errorContentColor = MaterialTheme.colorScheme.onErrorContainer,
            successContainerColor = MaterialTheme.colorScheme.onSecondaryContainer,
            successContentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .background(MaterialTheme.colorScheme.primary),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Pick a username",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Interact on FlipVerse with a unique name.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "USERNAME",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlipTextField(
                    value = authState.usernameId,
                    onValueChange = { newUsername ->
                        onAction(
                            AuthAction.OnUsernameChange(
                                newUsername
                            )
                        )
                    },
                    label = ""
                )
//            TextField(
//                value = username,
//                onValueChange = { username = it },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(56.dp)
//                    .background(
//                        Color(0xFF333333),
//                        RoundedCornerShape(8.dp)
//                    ), // Darker background for text field
//                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
//                singleLine = true,
//                colors = TextFieldDefaults.colors(
//                    focusedContainerColor = Color(0xFF333333),
//                    unfocusedContainerColor = Color(0xFF333333),
//                    disabledContainerColor = Color(0xFF333333),
//                    cursorColor = Color.White,
//                    focusedIndicatorColor = Color.Transparent, // No line indicator
//                    unfocusedIndicatorColor = Color.Transparent,
//                    errorIndicatorColor = Color.Transparent,
//                    disabledIndicatorColor = Color.Transparent
//                )
//            )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Your username helps people find and connect\nwith you on FlipVerse. ",
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                // "Learn more" part of the text
//            Text(
//                text = "Learn more",
//                color = MaterialTheme.colorScheme.tertiary, // Orange color
//                fontSize = 14.sp,
//                fontWeight = FontWeight.Bold,
//                modifier = Modifier.fillMaxWidth()
//            )
            }

                Column {
                    FlipButton(
                        text = "Next",
                        enabled = authState.usernameId.isNotEmpty(),
                        onClick = {
                            viewModel.validateUsernameAndContinue {
                                navigateToChooseInterests()
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }
    }
}
