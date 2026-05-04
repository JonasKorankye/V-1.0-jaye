package com.flipverse.chat.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.flipverse.shared.*
import com.flipverse.shared.domain.ChatMessage
import com.flipverse.shared.domain.ChatParticipant
import kotlinx.coroutines.delay

@Composable
fun TalkingAvatar(
    message: ChatMessage,
    participant: ChatParticipant?,
    isCurrentUser: Boolean,
    onStopSpeaking: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSpeaking by remember { mutableStateOf(false) }
    var currentWordIndex by remember { mutableStateOf(0) }
    val words = remember(message.content) { message.content.split(" ") }

    // Animation for pulsing effect
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Start speaking effect
    LaunchedEffect(message.id) {
        isSpeaking = true
        for (i in words.indices) {
            currentWordIndex = i
            delay(500) // Simulate word-by-word speaking (adjust timing as needed)
        }
        delay(1000) // Pause at the end
        isSpeaking = false
        onStopSpeaking()
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
        ) {
            // Avatar with speaking animation
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(if (isSpeaking) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(
                        if (isCurrentUser) Yellowish.copy(alpha = 0.3f)
                        else Gray.copy(alpha = 0.3f)
                    )
                    .border(
                        width = if (isSpeaking) 3.dp else 1.dp,
                        color = if (isSpeaking) GreenLighter else Color.Gray,
                        shape = CircleShape
                    )
                    .clickable { onStopSpeaking() },
                contentAlignment = Alignment.Center
            ) {
                if (participant?.thumbnail?.isNotEmpty() == true) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalPlatformContext.current)
                            .data(participant.thumbnail)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Speaking Avatar",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = (participant?.fullName?.take(1) ?: "?").uppercase(),
                        color = BlackLight,
                        fontSize = 40.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Speaking indicator
                if (isSpeaking) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(GreenLighter)
                    ) {
                        Text(
                            text = "🎤",
                            modifier = Modifier.align(Alignment.Center),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Speech bubble showing current words
            if (isSpeaking && currentWordIndex < words.size) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isCurrentUser) Yellowish else Gray
                        )
                        .padding(12.dp)
                ) {
                    Column {
                        // Show current words being "spoken"
                        Text(
                            text = words.take(currentWordIndex + 1).joinToString(" "),
                            color = BlackLight,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        // Progress indicator
                        if (words.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = (currentWordIndex + 1).toFloat() / words.size,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .clip(RoundedCornerShape(1.dp)),
                                color = GreenLighter,
                                trackColor = Color.Gray.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            // Control buttons
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStopSpeaking,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Red.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "Stop",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = {
                        // Reset and restart speaking
                        currentWordIndex = 0
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenLighter
                    ),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "Replay",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}