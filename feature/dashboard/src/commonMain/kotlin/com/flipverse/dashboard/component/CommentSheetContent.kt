package com.flipverse.dashboard.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.flipverse.data.util.formatTimestamp
import com.flipverse.data.util.isFirebaseTimestampString
import com.flipverse.data.util.normalizeUsername
import com.flipverse.shared.Alpha
import com.flipverse.shared.BlackLight
import com.flipverse.shared.PreferencesRepository.getFullName
import com.flipverse.shared.PreferencesRepository.getThumbnail
import com.flipverse.shared.Resources
import com.flipverse.shared.WorkSansBoldFont
import com.flipverse.shared.domain.Post
import com.flipverse.shared.presentation.component.FlipExpandableText
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun CommentSheetContent(
    post: Post,
    onDismiss: () -> Unit,
    onPost: (String) -> Unit,
    modifier: Modifier = Modifier,
    isPosting: Boolean = false
) {
    var replyText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { focusManager.clearFocus() }
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = Alpha.DISABLED),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .padding(bottom = 80.dp)
        ) {
            val isDarkTheme = isSystemInDarkTheme()
            val buttonTextColor = if (isDarkTheme) BlackLight else MaterialTheme.colorScheme.onPrimary
            val cancelTextColor = buttonTextColor.copy(alpha = 0.7f)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel button - subtle text style with theme color
                TextButton(
                    onClick = { onDismiss() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = buttonTextColor
                    )
                ) {
                    Text(
                        "Cancel",
                        color = cancelTextColor,
                        fontSize = 17.sp
                    )
                }
                
                // Post button - filled elevated style with theme colors
                val isPostEnabled = replyText.isNotBlank() && !isPosting
                Button(
                    onClick = { onPost(replyText) },
                    enabled = isPostEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = buttonTextColor,
                        disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        disabledContentColor = buttonTextColor.copy(alpha = 0.3f)
                    )
                ) {
                    AnimatedContent(
                        targetState = isPosting
                    ) { posting ->
                        if (posting) {
                            AdaptiveCircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = buttonTextColor
                            )
                        } else {
                            Text(
                                "Post",
                                color = buttonTextColor,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (post.authorProfileImage!!.isNotEmpty()) {
                    AsyncImage(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        model = ImageRequest.Builder(
                            LocalPlatformContext.current
                        ).data(post.authorProfileImage)
                            .crossfade(enable = true)
                            .build(),
                        contentDescription = "Author Profile URL",
                        contentScale = ContentScale.Crop
                    )

                } else {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = Alpha.HALF)) //  color for the circle
                            .border(2.dp, Color.Transparent, CircleShape), //  border color
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = post.authorName.take(1).uppercase(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 16.sp,
                            fontFamily = WorkSansBoldFont()
                        )

                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = normalizeUsername(post.authorHandle),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFirebaseTimestampString(post.timestamp)) formatTimestamp(
                                post.timestamp
                            ) else post.timestamp,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    val content =
                        post.whatsNew + "\n" + "\n" + "Source: " + post.source + "\n" + "Tags:" + post.tags.joinToString(
                            ", "
                        )

                    FlipExpandableText(text = content)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (getThumbnail().isEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = Alpha.HALF)) //  color for the circle
                            .border(2.dp, Color.Transparent, CircleShape), //  border color
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
                        modifier = Modifier.size(32.dp)
                            .clip(CircleShape)
                            .border(
                                1.dp,
                                Color.Transparent,
                                CircleShape
                            ),
                        model = ImageRequest.Builder(
                            LocalPlatformContext.current
                        ).data(getThumbnail())
                            .crossfade(enable = true)
                            .build(),
                        contentDescription = com.flipverse.shared.Strings.user_profile_thumbnail,
                        contentScale = ContentScale.Crop
                    )

                }
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onPrimary),
                    minLines = 4,
                    decorationBox = { innerTextField ->
                        if (replyText.isEmpty()) {
                            Text(
                                text = "Leave a reply...",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

//            HorizontalDivider(
//                modifier = Modifier.padding(4.dp).fillMaxWidth(),
//                thickness = 1.dp,
//                color = Color.Gray.copy(alpha = Alpha.DISABLED)
//            )

//            Spacer(modifier = Modifier.height(64.dp))
        }

//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .align(Alignment.BottomStart)
//                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.95f))
//                .padding(bottom = 64.dp, top = 8.dp, start = 12.dp, end = 12.dp),
//            horizontalArrangement = Arrangement.Start,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//
//            IconButton(onClick = { /* Handle image selection */ }) {
//                Icon(
//                    imageVector = vectorResource(Resources.Icon.CreatePostImage),
//                    contentDescription = "Attach image",
//                    tint = Color.Gray
//                )
//            }
//            Spacer(modifier = Modifier.width(16.dp))

//            IconButton(
//                modifier = Modifier.size(48.dp).padding(top = 8.dp, bottom = 3.dp),
//                onClick = { /* Handle camera */ }) {
//                Icon(
//                    imageVector = vectorResource(Resources.Icon.Camera),
//                    contentDescription = "Open camera",
//                    tint = Color.Gray
//                )
//            }
//        }
    }
}
