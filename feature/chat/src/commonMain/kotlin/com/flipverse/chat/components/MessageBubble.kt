package com.flipverse.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flipverse.data.util.formatChatTimestamp
import com.flipverse.shared.Ash
import com.flipverse.shared.BlackLight
import com.flipverse.shared.Coffee
import com.flipverse.shared.CoffeeLighter
import com.flipverse.shared.Gray
import com.flipverse.shared.GreenLighter
import com.flipverse.shared.LimeGreen
import com.flipverse.shared.Resources
import com.flipverse.shared.Yellowish
import com.flipverse.shared.domain.ChatMessage
import com.flipverse.shared.domain.ChatParticipant
import com.flipverse.shared.domain.MessageStatus
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun MessageBubble(
    message: ChatMessage,
    isCurrentUser: Boolean,
    participant: ChatParticipant? = null,
    onFlipMessage: ((String) -> Unit)? = null,
    onStopSpeaking: ((String) -> Unit)? = null,
    showTimestamp: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Show talking avatar if message is flipped
    if (message.isFlipped) {
        TalkingAvatar(
            message = message,
            participant = participant,
            isCurrentUser = isCurrentUser,
            onStopSpeaking = { onStopSpeaking?.invoke(message.id) },
            modifier = modifier
        )
    } else {
        // Original message bubble
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                            bottomEnd = if (isCurrentUser) 4.dp else 16.dp
                        )
                    )
                    .background(
                        color = if (isCurrentUser) {
                            LimeGreen
                        } else {
                            Gray
                        }
                    )
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = message.content,
                        color = if (isCurrentUser) {
                            BlackLight
                        } else {
                            BlackLight
                        },
                        fontSize = 16.sp
                    )

                    if (showTimestamp || onFlipMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Spacer to push content to the right
                            Spacer(modifier = Modifier.weight(1f))

                            // Timestamp and status
                            if (showTimestamp) {
                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatChatTimestamp(message.timestamp),
                                        color = if (isCurrentUser) {
                                            Coffee
                                        } else {
                                            Ash
                                        },
                                        fontSize = 12.sp
                                    )

                                    if (isCurrentUser) {
                                        MessageStatusIndicator(
                                            status = message.status,
                                            modifier = Modifier.padding(start = 4.dp)
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

@Composable
private fun MessageStatusIndicator(
    status: MessageStatus,
    modifier: Modifier = Modifier
) {
    when (status) {
        MessageStatus.SENDING -> {
            AdaptiveCircularProgressIndicator(
                modifier = modifier.size(12.dp),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                strokeWidth = 1.dp
            )
        }

        MessageStatus.SENT -> {
            Icon(
                imageVector = vectorResource(Resources.Icon.sent),
                contentDescription = "Sent",
                tint = BlackLight,
                modifier = modifier.size(16.dp)
            )
        }

        MessageStatus.DELIVERED -> {
            Text(
                text = "✓✓",
                color = BlackLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = modifier
            )
        }

        MessageStatus.FAILED -> {
            Text(
                text = "✗",
                color = Color.Red,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = modifier
            )
        }
    }
}
