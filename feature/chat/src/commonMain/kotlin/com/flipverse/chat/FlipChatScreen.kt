package com.flipverse.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.flipverse.shared.Alpha
import com.flipverse.shared.PreferencesRepository.getFullName
import com.flipverse.shared.PreferencesRepository.getThumbnail
import com.flipverse.shared.Strings
import com.flipverse.shared.WorkSansBoldFont

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlipChatScreen(
    navigateToUserProfile: () -> Unit,
    navigateToNewMessage: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Chat",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (getThumbnail().isEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { navigateToUserProfile() }
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = Alpha.HALF))
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = getFullName().take(1).uppercase(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 16.sp,
                                fontFamily = WorkSansBoldFont()
                            )
                        }
                    } else {
                        AsyncImage(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { navigateToUserProfile() }
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primaryContainer,
                                    CircleShape
                                ),
                            model = ImageRequest.Builder(LocalPlatformContext.current)
                                .data(getThumbnail())
                                .crossfade(enable = true)
                                .build(),
                            contentDescription = Strings.user_profile_thumbnail,
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = MaterialTheme.colorScheme.primary
    ) { paddingValues ->
        // Use the new ConversationsScreen
        ConversationsScreen(
            navigateToNewMessage = navigateToNewMessage,
            navigateToConversation = { conversationId, otherUserId ->
                // This will be handled by the navigation system
                // For now, we can't navigate directly from here without NavController
                // The navigation will be handled in ChatRoot
            },
            modifier = Modifier.background(MaterialTheme.colorScheme.primary)
        )
    }
}

