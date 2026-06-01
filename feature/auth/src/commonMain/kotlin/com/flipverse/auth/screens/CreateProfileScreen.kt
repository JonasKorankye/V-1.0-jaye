package com.flipverse.auth.screens

import ContentWithMessageBar
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.flipverse.auth.AuthAction
import com.flipverse.auth.AuthViewModel
import com.flipverse.shared.Black
import com.flipverse.shared.BlackLight
import com.flipverse.shared.DisplayResult
import com.flipverse.shared.FontSize
import com.flipverse.shared.PreferencesRepository.saveFirstName
import com.flipverse.shared.PreferencesRepository.saveFullName
import com.flipverse.shared.PreferencesRepository.saveLastName
import com.flipverse.shared.RequestState
import com.flipverse.shared.Resources
import com.flipverse.shared.WorkSansBoldFont
import com.flipverse.shared.presentation.component.FlipErrorCard
import com.flipverse.shared.presentation.component.FlipButton
import com.flipverse.shared.presentation.component.FlipTextField
import com.flipverse.shared.presentation.component.LoadingCard
import com.flipverse.shared.util.PhotoPicker
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import rememberMessageBarState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProfileScreen(
    navigateToCreateUsername: () -> Unit
) {
    val viewModel = koinViewModel<AuthViewModel>()
    val authState = viewModel.authState
    val onAction = viewModel::onAction
    val thumbnailUploaderState = viewModel.thumbnailUploaderState
    val messageBarState = rememberMessageBarState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadSavedFullName()
    }

    val photoPicker = koinInject<PhotoPicker>()
    // Store the local file for immediate preview while uploading
    var localPreviewData by remember { mutableStateOf<Any?>(null) }

    photoPicker.InitializePhotoPicker(
        onImageSelect = { file, previewBytes ->
            localPreviewData = previewBytes // Show local preview immediately using bytes
            viewModel.uploadThumbnailToStorage(
                file = file,
                onSuccess = {
                    scope.launch {
                        localPreviewData = null // Clear local preview, remote URL is now available
                        messageBarState.addSuccess("Thumbnail uploaded successfully!")
                    }
                },
                onError = { message ->
                    scope.launch {
                        localPreviewData = null
                        messageBarState.addError(message)
                        viewModel.updateThumbnailUploaderState(RequestState.Idle)
                    }
                }
            )
        }
    )


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
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
                navigationIcon = {
//                    IconButton(onClick = { /* Handle back button click */ }) {
//                        Icon(
//                            imageVector = vectorResource(BackArrow),
//                            contentDescription = "Back",
//                            tint = MaterialTheme.colorScheme.onPrimary
//                        )
//                    }
                },
                actions = {
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary) // Dark background
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
                    .background(MaterialTheme.colorScheme.primary),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Create a profile",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        fontFamily = FontFamily.SansSerif
                    ),
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "It looks like you're new here. Add a name and a profile picture to introduce yourself.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Profile Picture Placeholder with Add icon
                Box(
                    modifier = Modifier.size(96.dp)
                        .clickable(
                            enabled = thumbnailUploaderState.isIdle()
                        ) {
                            println("Triggered!")
                            photoPicker.open()
                        },
                    contentAlignment = Alignment.BottomEnd
                ) {
                    thumbnailUploaderState.DisplayResult(
                        onIdle = {
                            Box(
                                modifier = Modifier.size(96.dp),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                Image(
                                    painter = if (isSystemInDarkTheme()) painterResource(Resources.Icon.PersonDark) else painterResource(
                                        Resources.Icon.Person
                                    ), // Placeholder user icon
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
//                                        .background(MaterialTheme.colorScheme.surface) // Dark gray background for placeholder
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.primaryContainer,
                                            CircleShape
                                        ) // Thin white border
                                )
                                Icon(
                                    imageVector = vectorResource(Resources.Icon.Add),
                                    contentDescription = "Add Profile Picture",
                                    tint = BlackLight,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onPrimaryContainer)
                                        .border(
                                            1.dp,
                                            Black,
                                            CircleShape
                                        ) // Black border to separate from user icon
                                        .padding(6.dp) // Inner padding for the icon
                                )
                            }
                        },
                        onSuccess = {
                            Box(
                                modifier = Modifier.size(96.dp),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                AsyncImage(
                                    modifier = Modifier.fillMaxSize()
                                        .clip(CircleShape)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.primaryContainer,
                                            CircleShape
                                        ),
                                    model = ImageRequest.Builder(
                                        LocalPlatformContext.current
                                    ).data(authState.thumbnail)
                                        .crossfade(enable = true)
                                        .build(),
                                    contentDescription = "Profile thumbnail image",
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(MaterialTheme.colorScheme.onPrimaryContainer,
                                            CircleShape)
                                        .align(Alignment.TopEnd)
                                        .clickable {
                                            viewModel.deleteThumbnailFromStorage(
                                                onSuccess = {
                                                    scope.launch {
                                                        messageBarState.addSuccess("Thumbnail removed successfully.")
                                                    }
                                                },
                                                onError = { message ->
                                                    scope.launch {
                                                        messageBarState.addError(message)
                                                    }
                                                }
                                            )
                                        }
                                        .padding(all = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        modifier = Modifier.size(16.dp),
                                        tint = BlackLight,
                                        painter = painterResource(Resources.Icon.Delete),
                                        contentDescription = "Delete icon"
                                    )
                            }
                            }
                        },
                        onLoading = {
                            Box(
                                modifier = Modifier.size(96.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (localPreviewData != null) {
                                    // Show selected image as preview while uploading
                                    AsyncImage(
                                        modifier = Modifier.fillMaxSize()
                                            .clip(CircleShape)
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.primaryContainer,
                                                CircleShape
                                            ),
                                        model = ImageRequest.Builder(
                                            LocalPlatformContext.current
                                        ).data(localPreviewData)
                                            .crossfade(enable = true)
                                            .build(),
                                        contentDescription = "Profile thumbnail preview",
                                        contentScale = ContentScale.Crop,
                                        alpha = 0.6f // Slightly dim to indicate uploading
                                    )
                                }
                                LoadingCard(modifier = Modifier.size(40.dp))
                            }
                        },
                        onError = { message ->
                            // Use LaunchedEffect to avoid side effects during composition
                            LaunchedEffect(message) {
                                messageBarState.addError(message)
                                viewModel.updateThumbnailUploaderState(RequestState.Idle)
                            }
                        },
                    )

                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "FULL NAME",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(4.dp))

                FlipTextField(
                    value = authState.fullName,
                    onValueChange = { newFullName ->
                        onAction(
                            AuthAction.OnFullNameChange(
                                newFullName
                            )
                        )
                    },
                    label = ""
                )

//            TextField(
//                value = fullName,
//                onValueChange = { fullName = it },
//                label = { Text("Full name") },
//                modifier = Modifier.fillMaxWidth(),
//                singleLine = true,
//                colors = TextFieldDefaults.colors(
//                    focusedContainerColor = Color(0xFF1E1E1E), // Match background
//                    unfocusedContainerColor = Color(0xFF1E1E1E),
//                    disabledContainerColor = Color(0xFF1E1E1E),
//                    cursorColor = Color.White,
//                    focusedIndicatorColor = Color.Gray, // Thin gray line
//                    unfocusedIndicatorColor = Color.Gray,
//                    focusedTextColor = Color.White,
//                    unfocusedTextColor = Color.White,
//                    focusedLabelColor = Color.Gray,
//                    unfocusedLabelColor = Color.Gray
//                )
//            )

                Spacer(modifier = Modifier.height(48.dp))

                // Social Icons Row
//            Row(
//                modifier = Modifier.fillMaxWidth().padding(8.dp),
//                horizontalArrangement = Arrangement.SpaceAround,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                SocialIconWithAdd(Resources.Icon.Search)
//                SocialIconWithAdd(Resources.Icon.Notification)
//                SocialIconWithAdd(Resources.Icon.Search)
//                SocialIconWithAdd(Resources.Icon.Notification) // Assuming butterfly for the fourth
//                SocialIconWithAdd(Resources.Icon.Search) // 'X' for the last one
//            }

                Spacer(modifier = Modifier.weight(1f)) // Pushes content above it to the top

                // Next Button
                FlipButton(
                    text = "Next",
                    enabled = authState.fullName.isNotEmpty(),
                    onClick = {

                        saveFirstName(authState.fullName.split(" ").firstOrNull() ?: "Unknown")
                        saveLastName(authState.fullName.split(" ").lastOrNull() ?: "Unknown")
                        saveFullName(authState.fullName)

                        navigateToCreateUsername()
                    },
                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(36.dp))


//            Button(
//                onClick = { /* Handle Next */ },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(56.dp)
//                    .padding(bottom = 16.dp), // Add padding for bottom margin
//                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC6600)) // Orange color
//            ) {
//                Text("Next", color = Color.White, fontSize = 18.sp)
//            }
            }
        }
    }
}

@Composable
fun SocialIconWithAdd(iconRes: DrawableResource, isLast: Boolean = false) {
    Box(
        modifier = Modifier.size(48.dp), // Adjust size of the overall box for the icon
        contentAlignment = Alignment.TopEnd // Position the add icon to top-end
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null, // Content description for the social icon
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface) // Background color for social icons
                .padding(4.dp) // Padding to make icon smaller inside circle
        )
        Icon(
            imageVector = vectorResource(Resources.Icon.Add),
            contentDescription = "Add social media",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(16.dp) // Smaller size for the add icon
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimary) // Black background for the small add icon
                .border(
                    0.5.dp,
                    MaterialTheme.colorScheme.primary,
                    CircleShape
                ) // Small white border
        )
    }
}
