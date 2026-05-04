package com.flipverse.chat.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.crossfade
import com.flipverse.data.util.formatChatTimestamp
import com.flipverse.shared.Ash
import com.flipverse.shared.BlackLight
import com.flipverse.shared.Coffee
import com.flipverse.shared.Gray
import com.flipverse.shared.GreenLighter
import com.flipverse.shared.Yellowish
import com.flipverse.shared.domain.ChatMessage
import com.flipverse.shared.domain.MessageStatus
import com.flipverse.shared.util.TTSState

@Composable
fun WaveAnimation(
    numWaves: Int = 24,
    primaryColor: Color = GreenLighter,
    secondaryColor: Color = GreenLighter.copy(alpha = 0.6f),
    width: Dp = 2.dp,
    maxHeight: Dp = 30.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")
    val waves = List(numWaves) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 600 + (index % 6) * 150,
                    delayMillis = index * 40,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "wave$index"
        )
    }

    // Create sophisticated gradient colors for waves
    Row(
        horizontalArrangement = Arrangement.spacedBy(width / 2),
        modifier = modifier
            .height(40.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        waves.forEachIndexed { index, wave ->
            // Create multiple gradient layers for depth
            val centerIndex = numWaves / 2
            val distanceFromCenter = kotlin.math.abs(index - centerIndex).toFloat() / centerIndex

            // Create wave color with multiple gradient effects
            val gradientProgress = index.toFloat() / (numWaves - 1)
            val centerBias = 1f - distanceFromCenter * 0.3f // Brighter in center

            val waveColor = Color(
                red = (primaryColor.red + (secondaryColor.red - primaryColor.red) * gradientProgress) * centerBias,
                green = (primaryColor.green + (secondaryColor.green - primaryColor.green) * gradientProgress) * centerBias,
                blue = (primaryColor.blue + (secondaryColor.blue - primaryColor.blue) * gradientProgress) * centerBias,
                alpha = (primaryColor.alpha + (secondaryColor.alpha - primaryColor.alpha) * gradientProgress) * wave.value
            )

            // Create dynamic height based on position and animation
            val baseHeight = maxHeight * 0.4f + (maxHeight * 0.6f * wave.value)
            val positionEffect =
                1f - kotlin.math.abs(index - centerIndex).toFloat() / centerIndex * 0.3f
            val finalHeight = baseHeight * positionEffect

            Box(
                modifier = Modifier
                    .width(width)
                    .height(finalHeight)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                waveColor,
                                waveColor.copy(alpha = waveColor.alpha * 0.7f)
                            )
                        ),
                        shape = RoundedCornerShape(width / 2)
                    )
            )
        }
    }
}

@Composable
fun WaveMessageBubble(
    message: ChatMessage,
    onWaveClick: (String) -> Unit,
    isCurrentUser: Boolean = false,
    showTimestamp: Boolean = true,
    isPlaying: Boolean = false,
    isPaused: Boolean = false,
    ttsState: TTSState = TTSState.IDLE,
    modifier: Modifier = Modifier
) {
    // Check if this message is an avatar (if content is a URL that looks like an avatar)
    val isAvatarMessage = message.content.let { content ->
        content.startsWith("https://") && (
                content.contains("api.dicebear.com", ignoreCase = true)
                )
    }

    if (isAvatarMessage) {
        AvatarMessageBubble(
            messageId = message.id,
            avatarUrl = message.content,
            messageText = "",
            onAvatarClick = { onWaveClick(message.id) },
            isCurrentUser = isCurrentUser,
            showTimestamp = showTimestamp,
            isPlaying = isPlaying,
            isPaused = isPaused,
            timestamp = message.timestamp,
            modifier = modifier
        )
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .widthIn(min = 120.dp, max = 200.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = if (isCurrentUser) 20.dp else 6.dp,
                            bottomEnd = if (isCurrentUser) 6.dp else 20.dp
                        )
                    )
                    .background(
                        color = if (isCurrentUser) {
                            Yellowish.copy(alpha = 0.9f)
                        } else {
                            Gray.copy(alpha = 0.9f)
                        }
                    )
                    .clickable { onWaveClick(message.id) }
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Display avatar image if it's an avatar message
                    if (isAvatarMessage) {
                        // Avatar display section
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = coil3.request.ImageRequest.Builder(coil3.compose.LocalPlatformContext.current)
                                    .data(message.content)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Shared Avatar",
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isCurrentUser) "You shared your avatar" else "Shared an avatar",
                            color = if (isCurrentUser) Coffee else Ash,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Wave animation - only animate when playing this specific message
                    if (isPlaying) {
                        WaveAnimation(
                            numWaves = 24,
                            primaryColor = if (isCurrentUser) GreenLighter else BlackLight,
                            secondaryColor = if (isCurrentUser) GreenLighter.copy(alpha = 0.4f) else BlackLight.copy(
                                alpha = 0.6f
                            ),
                            width = 2.dp,
                            maxHeight = 30.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        )
                    } else {
                        // Static wave bars when not playing - with sophisticated gradient effect
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(24) { index ->
                                // Create sophisticated gradient and positioning effects
                                val centerIndex = 12 // 24/2
                                val distanceFromCenter =
                                    kotlin.math.abs(index - centerIndex).toFloat() / centerIndex
                                val gradientProgress = index.toFloat() / 23f
                                val centerBias = 1f - distanceFromCenter * 0.3f

                                val baseColor = when {
                                    isPaused -> Color.Gray
                                    isCurrentUser -> GreenLighter
                                    else -> BlackLight
                                }
                                val secondaryColor = when {
                                    isPaused -> Color.Gray.copy(alpha = 0.4f)
                                    isCurrentUser -> GreenLighter.copy(alpha = 0.4f)
                                    else -> BlackLight.copy(alpha = 0.6f)
                                }

                                val waveColor = Color(
                                    red = (baseColor.red + (secondaryColor.red - baseColor.red) * gradientProgress) * centerBias,
                                    green = (baseColor.green + (secondaryColor.green - baseColor.green) * gradientProgress) * centerBias,
                                    blue = (baseColor.blue + (secondaryColor.blue - baseColor.blue) * gradientProgress) * centerBias,
                                    alpha = (baseColor.alpha + (secondaryColor.alpha - baseColor.alpha) * gradientProgress) * centerBias
                                )

                                // Create more sophisticated height variations
                                val heightVariations =
                                    listOf(0.4f, 0.6f, 0.8f, 1.0f, 0.9f, 0.7f, 0.5f, 0.3f)
                                val heightFactor = heightVariations[index % heightVariations.size]
                                val positionEffect = 1f - distanceFromCenter * 0.4f
                                val finalHeightFactor = heightFactor * positionEffect
                                val waveHeight = (12.dp + 18.dp * finalHeightFactor)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(waveHeight)
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    waveColor,
                                                    waveColor.copy(alpha = waveColor.alpha * 0.6f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(1.dp)
                                        )
                                )
                            }
                        }
                    }

                    if (showTimestamp) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Enhanced status indicator row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Timestamp (leftmost)
                            Text(
                                text = formatChatTimestamp(message.timestamp),
                                color = if (isCurrentUser) {
                                    Coffee.copy(alpha = 0.7f)
                                } else {
                                    Ash.copy(alpha = 0.7f)
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Light
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            // Enhanced Status Indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val (statusColor, statusText, statusIcon) = when {
                                    isPlaying -> Triple(
                                        Color(0xFF2E7D32),
                                        if (isAvatarMessage) "Playing Avatar" else "Playing",
                                        "\uD83D\uDD0A"
                                    )

                                    ttsState == TTSState.PAUSED -> Triple(
                                        Color(0xFFED6C02),
                                        "Paused",
                                        "\u23F8"
                                    )

                                    else -> Triple(
                                        Coffee.copy(alpha = 0.7f),
                                        if (isAvatarMessage) "Tap to Play Avatar" else "Tap to Play",
                                        "\uD83D\uDD07"
                                    )
                                }

                                Text(
                                    text = statusIcon,
                                    color = statusColor,
                                    fontSize = 14.sp
                                )

                                Box(
                                    modifier = Modifier.size(10.dp)
                                        .background(statusColor, CircleShape)
                                )

                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Message Status (rightmost)
                            Box(
                                modifier = Modifier
                                    .padding(end = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCurrentUser) {
                                    when {
                                        message.isRead && message.status == MessageStatus.DELIVERED -> {
                                            // Double green ticks (read)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "✓",
                                                    color = GreenLighter,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "✓",
                                                    color = GreenLighter,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        message.status == MessageStatus.DELIVERED -> {
                                            // Double gray ticks (delivered, not read)
                                            Text(
                                                text = "✓✓",
                                                color = BlackLight.copy(alpha = 0.6f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        message.status == MessageStatus.SENT -> {
                                            // Single tick
                                            Text(
                                                text = "✓",
                                                color = BlackLight.copy(alpha = 0.6f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        message.status == MessageStatus.SENDING -> {
                                            Text(
                                                text = "⏳",
                                                color = BlackLight.copy(alpha = 0.4f),
                                                fontSize = 10.sp
                                            )
                                        }

                                        message.status == MessageStatus.FAILED -> {
                                            Text(
                                                text = "✗",
                                                color = Color.Red.copy(alpha = 0.7f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AvatarMessageBubble(
    messageId: String,
    avatarUrl: String,
    messageText: String,
    onAvatarClick: (String) -> Unit,
    isCurrentUser: Boolean = false,
    showTimestamp: Boolean = true,
    isPlaying: Boolean = false,
    isPaused: Boolean = false,
    timestamp: Long = 0L,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 120.dp, max = 160.dp)
                .clickable { onAvatarClick(messageId) }
                .padding(8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Main Avatar with lip animation and speaking indicator
                Box(
                    modifier = Modifier.size(110.dp), // Slightly larger to accommodate indicator
                    contentAlignment = Alignment.Center
                ) {
                    // Avatar container
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCurrentUser) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                } else {
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Avatar Image
                        AsyncImage(
                            model = coil3.request.ImageRequest.Builder(coil3.compose.LocalPlatformContext.current)
                                .data(avatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Talking Avatar",
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )

                        // Lip animation overlay when speaking
                        if (isPlaying) {
                            LipAnimationOverlay(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }

                    // Speaking indicator - positioned outside the clipped avatar
                    if (isPlaying) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .background(
                                    Color(0xFF2E7D32).copy(alpha = 0.9f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "\uD83D\uDD0A",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                // Message text display
//                if (messageText.isNotEmpty()) {
//                    Spacer(modifier = Modifier.height(8.dp))
//                    Text(
//                        text = "\"$messageText\"",
//                        color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else Ash,
//                        fontSize = 12.sp,
//                        fontWeight = FontWeight.Medium,
//                        modifier = Modifier.padding(horizontal = 4.dp)
//                    )
//                }

                if (showTimestamp) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Status row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Timestamp
                        Text(
                            text = formatChatTimestamp(timestamp),
                            color = if (isCurrentUser) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                Ash.copy(alpha = 0.7f)
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Light
                        )

                        // Status text
                        Text(
                            text = when {
                                isPlaying -> "Speaking..."
                                isPaused -> "Paused"
                                else -> "Tap to speak"
                            },
                            color = when {
                                isPlaying -> Color(0xFF2E7D32)
                                isPaused -> Color(0xFFED6C02)
                                else -> MaterialTheme.colorScheme.onSecondary
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LipAnimationOverlay(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "lipAnimation")

    // Multiple animation values for different lip movements
    val mouthOpen = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mouthOpen"
    )

    val mouthWidth = infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mouthWidth"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Animated mouth/lip overlay
        Box(
            modifier = Modifier
                .offset(y = 20.dp) // Position where mouth typically is
                .size(
                    width = (12.dp * mouthWidth.value),
                    height = (4.dp + 6.dp * mouthOpen.value)
                )
                .background(
                    Color.Red.copy(alpha = 0.7f),
                    RoundedCornerShape(50)
                )
        )

        // Additional lip animation effects
        Box(
            modifier = Modifier
                .offset(y = 18.dp)
                .size(
                    width = (8.dp * mouthWidth.value),
                    height = (2.dp + 3.dp * mouthOpen.value)
                )
                .background(
                    Color.White.copy(alpha = 0.8f),
                    RoundedCornerShape(50)
                )
        )
    }
}