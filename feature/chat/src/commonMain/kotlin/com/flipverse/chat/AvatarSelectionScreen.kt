package com.flipverse.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.flipverse.chat.avatar.AvatarSelectionViewModel
import com.flipverse.chat.avatar.AvatarStyle
import com.flipverse.shared.Ash
import com.flipverse.shared.BlackLight
import com.flipverse.shared.Gray
import com.flipverse.shared.PreferencesRepository.saveAvatar
import com.flipverse.shared.Resources
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarSelectionScreen(
    onBackPressed: () -> Unit,
    onAvatarSelected: (String) -> Unit,
    currentAvatarUrl: String? = null,
) {
    val viewModel: AvatarSelectionViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.generateAvatars()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 16.dp)
        ) {
            // Current avatar preview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current Avatar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )

                            // Customize button - only show if avatar is selected and style supports customization
//                            if (state.selectedAvatarUrl.isNotEmpty() && state.selectedStyle.customizationOptions.isNotEmpty()) {
//                                TextButton(
//                                    onClick = { viewModel.toggleCustomization() },
//                                    colors = ButtonDefaults.textButtonColors(
//                                        contentColor = MaterialTheme.colorScheme.primary
//                                    )
//                                ) {
//                                    Icon(
//                                        imageVector = vectorResource(Resources.Icon.Edit),
//                                        contentDescription = "Customize",
//                                        modifier = Modifier.size(16.dp),
//                                        tint = MaterialTheme.colorScheme.onPrimary
//                                    )
//                                    Spacer(modifier = Modifier.width(4.dp))
//                                    Text(
//                                        text = "Customize",
//                                        fontSize = 12.sp,
//                                        color = MaterialTheme.colorScheme.onPrimary
//                                    )
//                                }
//                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Gray),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.selectedAvatarUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalPlatformContext.current)
                                        .data(state.selectedAvatarUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Selected avatar",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                    onError = { println("Error loading selected avatar: ${it.result.throwable}") }
                                )
                            } else if (currentAvatarUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalPlatformContext.current)
                                        .data(currentAvatarUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Current avatar",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                    onError = { println("Error loading current avatar: ${it.result.throwable}") }
                                )
                            } else {
                                Text(
                                    text = "",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Refresh button positioned at bottom right
                    IconButton(
                        onClick = { viewModel.regenerateAvatars() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = vectorResource(Resources.Icon.Refresh),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            contentDescription = "Regenerate avatars"
                        )
                    }
                }
            }

            // Avatar styles chips
            if (state.availableStyles.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    items(state.availableStyles) { style ->
                        AvatarStyleChip(
                            style = style,
                            isSelected = state.selectedStyle == style,
                            onClick = {
                                viewModel.selectStyle(style)
                                viewModel.hideCustomization() // Hide customization when changing style
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Avatar grid
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Generating avatars...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                state.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Failed to load avatars",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.error!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.generateAvatars() }
                            ) {
                                Text("Try Again")
                            }
                        }
                    }
                }

                state.avatars.isNotEmpty() -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(state.avatars) { avatar ->
                            AvatarItem(
                                avatarUrl = avatar.url,
                                isSelected = state.selectedAvatarUrl == avatar.url,
                                onClick = {
                                    println("Avatar clicked: ${avatar.url}")
                                    println("Current selectedAvatarUrl: ${state.selectedAvatarUrl}")
                                    viewModel.selectAvatar(avatar.url)
                                    viewModel.hideCustomization() // Hide customization when selecting new avatar
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.padding(36.dp))
                        }
                    }
                }
            }

            // Confirm button
//            Button(
//                onClick = {
//                    println("Confirm button clicked with selectedAvatarUrl: ${state.selectedAvatarUrl}")
//                    if (state.selectedAvatarUrl.isNotEmpty()) {
//                        onAvatarSelected(state.selectedAvatarUrl)
//                    }
//                },
//                enabled = state.selectedAvatarUrl.isNotEmpty(),
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .navigationBarsPadding()
//                    .padding(bottom = 96.dp, start = 16.dp, end = 16.dp),
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = if (state.selectedAvatarUrl.isNotEmpty()) {
//                        MaterialTheme.colorScheme.primaryContainer
//                    } else {
//                        Ash
//                    }
//                ),
//                shape = RoundedCornerShape(12.dp)
//            ) {
//                Icon(
//                    imageVector = vectorResource(Resources.Icon.Check),
//                    contentDescription = null,
//                    modifier = Modifier.size(20.dp),
//                    tint = if (state.selectedAvatarUrl.isNotEmpty()) BlackLight else Gray
//                )
//                Spacer(modifier = Modifier.width(8.dp))
//                Text(
//                    text = if (state.selectedAvatarUrl.isNotEmpty()) "Select Avatar" else "Choose an avatar",
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.Medium,
//                    color = if (state.selectedAvatarUrl.isNotEmpty()) BlackLight else MaterialTheme.colorScheme.onPrimary
//                )
//            }
        }

        // Customization panel overlay
        if (state.showCustomization) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable {
                        viewModel.hideCustomization()
                        // Save the customized avatar when clicking outside to close
                        if (state.selectedAvatarUrl.isNotEmpty()) {
                            saveAvatar(state.selectedAvatarUrl)
                        }
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                AvatarCustomizationPanel(
                    style = state.selectedStyle,
                    currentCustomizations = state.customizations,
                    onCustomizationChange = { key, value ->
                        viewModel.updateCustomization(key, value)
                    },
                    onClose = {
                        viewModel.hideCustomization()
                        // Save the customized avatar when closing the panel
                        if (state.selectedAvatarUrl.isNotEmpty()) {
                            saveAvatar(state.selectedAvatarUrl)
                        }
                    },
                    modifier = Modifier.clickable(enabled = false) { } // Prevent closing when clicking on panel
                )
            }
        }
    }
}

@Composable
private fun AvatarStyleChip(
    style: AvatarStyle,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .height(48.dp)
            .padding(vertical = 2.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null,
        tonalElevation = if (isSelected) 2.dp else 1.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(style.previewUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = style.displayName,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                onError = { println("Error loading style preview: ${it.result.throwable}") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = style.displayName,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                color = if (isSelected) BlackLight else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AvatarItem(
    avatarUrl: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(avatarUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Avatar option",
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            onError = { println("Error loading avatar: ${it.result.throwable}") }
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = vectorResource(Resources.Icon.Check),
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
