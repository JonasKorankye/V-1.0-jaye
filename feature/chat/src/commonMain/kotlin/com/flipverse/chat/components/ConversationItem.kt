package com.flipverse.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.flipverse.data.util.formatChatTimestamp
import com.flipverse.shared.Alpha
import com.flipverse.shared.Resources
import com.flipverse.shared.Strings
import com.flipverse.shared.domain.ConversationPreview
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.vectorResource

@Composable
private fun RealTimeTimestamp(timestamp: Long) {
    var formattedTime by remember(timestamp) { mutableStateOf(formatChatTimestamp(timestamp)) }

    LaunchedEffect(timestamp) {
        while (true) {
            formattedTime = formatChatTimestamp(timestamp)
            delay(60_000L) // update every minute
        }
    }

    Text(
        text = formattedTime,
        color = Color.Gray,
        fontSize = 12.sp
    )
}

@Composable
fun ConversationItem(
    conversation: ConversationPreview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        UserAvatar(
            thumbnailUrl = conversation.otherParticipant?.thumbnail,
            fullName = conversation.otherParticipant?.fullName ?: Strings.unknown,
            size = 48.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Conversation info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.otherParticipant?.fullName ?: Strings.unknown_user,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Indicators
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (conversation.isPinned) {
                        Icon(
                            imageVector = vectorResource(Resources.Icon.Pin),
                            contentDescription = Strings.pinned,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    if (conversation.isMuted) {
                        Icon(
                            imageVector = vectorResource(Resources.Icon.Mute),
                            contentDescription = Strings.muted,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // Timestamp
                    conversation.conversation.lastMessage?.let { lastMessage ->
                        RealTimeTimestamp(lastMessage.timestamp)
                    }
                }
            }

            Spacer(modifier = Modifier.size(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Last message
                val messageContent = conversation.conversation.lastMessage?.content
                val displayText = when {
                    messageContent.isNullOrBlank() -> Strings.no_messages_yet
                    messageContent.startsWith("TEXT:") -> messageContent.removePrefix("TEXT:")
                    else -> messageContent
                }
                Text(
                    text = displayText ?: Strings.no_messages_yet,
                    color = Color.Gray,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Unread count
                if (conversation.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .size(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserAvatar(
    thumbnailUrl: String?,
    fullName: String,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    if (!thumbnailUrl.isNullOrEmpty()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(thumbnailUrl)
                .crossfade(true)
                .build(),
            contentDescription = Strings.profile_picture,
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = Alpha.HALF)
                )
                .border(2.dp, Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fullName.take(1).uppercase(),
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = (size.value / 3).sp,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

