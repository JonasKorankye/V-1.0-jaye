package com.flipverse.userprofile


import ContentWithMessageBar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.ui.text.input.ImeAction
import coil3.compose.SubcomposeAsyncImageContent
import com.flipverse.shared.Alpha
import com.flipverse.shared.Black
import com.flipverse.shared.BlackLight
import com.flipverse.shared.DisplayResult
import com.flipverse.shared.FontSize
import com.flipverse.shared.LexendMediumFont
import com.flipverse.shared.PreferencesRepository.getFullName
import com.flipverse.shared.RequestState
import com.flipverse.shared.Resources
import com.flipverse.shared.WorkSansBoldFont
import com.flipverse.shared.presentation.component.FlipTextField
import com.flipverse.shared.presentation.component.LoadingCard
import com.flipverse.shared.util.PhotoPicker
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import rememberMessageBarState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(onNavigateBack: () -> Unit) {
    val viewModel = koinViewModel<UserProfileViewModel>()
    val onAction = viewModel::onAction
    val profileState = viewModel.profileState

    val thumbnailUploaderState = viewModel.thumbnailUploaderState
    val saveProfileState = viewModel.saveProfileState
    val messageBarState = rememberMessageBarState()

    val photoPicker = koinInject<PhotoPicker>()
    var previousBioText by remember { mutableStateOf("") }
    // Store the local file for immediate preview while uploading
    var localPreviewData by remember { mutableStateOf<Any?>(null) }

    photoPicker.InitializePhotoPicker(
        onImageSelect = { file, previewBytes ->
            localPreviewData = previewBytes // Show local preview immediately using bytes
            viewModel.uploadThumbnailToStorage(
                file = file,
                onSuccess = {
                    localPreviewData = null // Clear local preview, remote URL is now available
                    messageBarState.addSuccess("Thumbnail uploaded successfully!")
                },
                onError = { message ->
                    localPreviewData = null
                    messageBarState.addError(message)
                    viewModel.updateThumbnailUploaderState(RequestState.Idle)
                }
            )
        }
    )

    val hasChanges = profileState.hasChanges()

    LaunchedEffect(saveProfileState) {
        when (saveProfileState) {
            is RequestState.Success -> {
                messageBarState.addSuccess("Profile saved successfully!")
                viewModel.updateSaveProfileState(RequestState.Idle)
            }

            is RequestState.Error -> {
                messageBarState.addError(saveProfileState.message)
                viewModel.updateSaveProfileState(RequestState.Idle)
            }

            else -> {}
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Edit Profile",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 32.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = WorkSansBoldFont(),
                        modifier = Modifier.padding(start = 0.dp) // Adjust padding if necessary
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
                actions = {
                    if (hasChanges) {
                        // SAVE button with elegant theme styling
                        val isDarkTheme = isSystemInDarkTheme()
                        val buttonTextColor = if (isDarkTheme) BlackLight else MaterialTheme.colorScheme.onPrimary
                        
                        if (saveProfileState is RequestState.Loading) {
                            Box(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AdaptiveCircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = buttonTextColor,
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    onAction(ProfileAction.OnSaveClick)
                                },
                                enabled = saveProfileState !is RequestState.Loading,
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = buttonTextColor,
                                    disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    disabledContentColor = buttonTextColor.copy(alpha = 0.3f)
                                )
                            ) {
                                Text(
                                    text = "SAVE",
                                    color = buttonTextColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = FontSize.SMALL
                                )
                            }
                        }
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
            errorContainerColor = MaterialTheme.colorScheme.error,
            errorContentColor = MaterialTheme.colorScheme.onErrorContainer,
            successContainerColor = MaterialTheme.colorScheme.onSecondaryContainer,
            successContentColor = BlackLight,
            showCopyButton = false
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier.size(96.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    // Profile image — use reactive ViewModel state so it updates
                    // after upload/delete without needing to leave and re-enter the screen.
                    val thumbnailUrl = profileState.thumbnail
                    val hasValidThumbnail = thumbnailUrl.isNotBlank() &&
                        (thumbnailUrl.startsWith("http://") || thumbnailUrl.startsWith("https://"))
                    
                    if (hasValidThumbnail) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalPlatformContext.current)
                                .data(thumbnailUrl)
                                .crossfade(enable = true)
                                .build(),
                            contentDescription = com.flipverse.shared.Strings.user_profile_thumbnail,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                                .clip(CircleShape)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primaryContainer,
                                    CircleShape
                                )
                        ) {
                            // In Coil 3, painter.state is a StateFlow — must collectAsState()
                            val state by painter.state.collectAsState()
                            when (state) {
                                is coil3.compose.AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(96.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = Alpha.HALF)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = getFullName().take(1).uppercase(),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 32.sp,
                                            fontFamily = LexendMediumFont()
                                        )
                                    }
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    MaterialTheme.colorScheme.onPrimaryContainer,
                                    CircleShape
                                )
                                .align(Alignment.TopEnd)
                                .clickable {
                                    viewModel.deleteThumbnailFromStorage(
                                        onSuccess = { messageBarState.addSuccess("Thumbnail removed successfully.") },
                                        onError = { message ->
                                            messageBarState.addError(
                                                message
                                            )
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
                    } else {
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
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer.copy(
                                                    alpha = Alpha.HALF
                                                )
                                            ) //  color for the circle
                                            .border(2.dp, Color.White, CircleShape),//  border color
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = getFullName().take(1).uppercase(),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 32.sp,
                                            fontFamily = LexendMediumFont()
                                        )
                                    }

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
                                            ) // border to separate from user icon
                                            .padding(6.dp) // Inner padding for the icon
                                    )
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
                                                alpha = 0.6f
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
                                            ).data(profileState.thumbnail)
                                                .crossfade(enable = true)
                                                .build(),
                                            contentDescription = com.flipverse.shared.Strings.user_profile_thumbnail,
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.onPrimaryContainer,
                                                    CircleShape
                                                )
                                                .align(Alignment.TopEnd)
                                                .clickable {
                                                    viewModel.deleteThumbnailFromStorage(
                                                        onSuccess = { messageBarState.addSuccess("Thumbnail removed successfully.") },
                                                        onError = { message ->
                                                            messageBarState.addError(
                                                                message
                                                            )
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
                                }
                            )
                        }


                    }

                }
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "NAME",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )
                FlipTextField(
                    value = profileState.name,
                    onValueChange = { newName ->
                        if (newName.length <= 100) {
                            onAction(
                                ProfileAction.OnNameChange(
                                    newName
                                )
                            )
                        } else {
                            messageBarState.addError(
                                Exception("Name cannot exceed 100 characters.")
                            )
                        }
                    },
                    label = ""
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Bio Field
                Text(
                    text = "BIO",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )
                FlipTextField(
                    modifier = Modifier.heightIn(128.dp),
                    singleLine = false,
                    maxLines = 6,
                    imeAction = ImeAction.Default,
                    value = profileState.bio,
                    onValueChange = { newBio ->
                        // Check character limit first
                        if (newBio.length > 100) {
                            messageBarState.addError(
                                Exception("Bio cannot exceed 100 characters.")
                            )
                            return@FlipTextField
                        }

                        // Detect unnecessary whitespace (only consecutive spaces, not newlines)
                        val hasMultipleSpaces = newBio.contains(Regex(" {2,}"))
                        val hasLeadingSpaces = newBio.startsWith(" ") && previousBioText.isEmpty()
                        val hasExcessiveWhitespace = newBio.count { it == ' ' } > newBio.length * 0.3 && newBio.length > 20

                        if (hasMultipleSpaces) {
                            messageBarState.addError(
                                Exception("Multiple consecutive spaces detected. Please use single spaces between words.")
                            )
                            return@FlipTextField
                        }

                        if (hasLeadingSpaces) {
                            messageBarState.addError(
                                Exception("Please don't start with spaces. Begin typing directly.")
                            )
                            return@FlipTextField
                        }

                        if (hasExcessiveWhitespace) {
                            messageBarState.addError(
                                Exception("Excessive whitespace detected. Please write naturally without unnecessary spaces.")
                            )
                            return@FlipTextField
                        }

                        onAction(
                            ProfileAction.OnBioChange(
                                newBio
                            )
                        )
                        previousBioText = newBio
                    },
                    label = ""
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Username Field
                Text(
                    text = "USERNAME",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )
                FlipTextField(
                    value = profileState.username,
                    onValueChange = { newUserName ->
                        if (newUserName.length <= 100) {
                            onAction(
                                ProfileAction.OnUserNameChange(
                                    newUserName
                                )
                            )
                        } else {
                            messageBarState.addError(
                                Exception("Username cannot exceed 100 characters.")
                            )
                        }
                    },
                    label = ""
                )

                Spacer(modifier = Modifier.height(16.dp))

            }
        }
    }
}
