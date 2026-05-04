package com.flipverse.dashboard.component


import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.flipverse.dashboard.DashboardViewModel
import com.flipverse.dashboard.PostData
import com.flipverse.data.util.formatTimestamp
import com.flipverse.data.util.isFirebaseTimestampString
import com.flipverse.data.util.normalizeUsername
import com.flipverse.shared.Alpha
import com.flipverse.shared.PreferencesRepository.getFullName
import com.flipverse.shared.PreferencesRepository.getThumbnail
import com.flipverse.shared.Resources
import com.flipverse.shared.WorkSansBoldFont
import com.flipverse.shared.domain.Comment
import com.flipverse.shared.domain.MediaItem
import com.flipverse.shared.presentation.component.FlipExpandableText
import com.flipverse.shared.presentation.component.MediaPreviewSection
import com.mohamedrejeb.calf.permissions.ExperimentalPermissionsApi
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CommentReplySheetContent(
    comment: Comment,
    onDismiss: () -> Unit,
    onPost: (String) -> Unit,
    onClickSelectImages: () -> Unit,
    onRemoveImage: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    screenState: PostData
) {
    val viewModel = koinViewModel<DashboardViewModel>()

    var replyText by remember { mutableStateOf("") }





    Box(
        modifier = Modifier
            .fillMaxWidth()
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { onDismiss() }) {
                    Text("Cancel", color = Color.Gray, fontSize = 17.sp)
                }
                TextButton(
                    onClick = { onPost(replyText) },
                    enabled = replyText.isNotBlank()
                ) {
                    Text(
                        "Post",
                        color = if (replyText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else Color.Gray,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (comment.commentThumbnail.isNotEmpty()) {
                    AsyncImage(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        model = ImageRequest.Builder(
                            LocalPlatformContext.current
                        ).data(getThumbnail())
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
                            text = comment.fullname!!.take(1).uppercase(),
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
                            text = normalizeUsername(comment.commentBy),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFirebaseTimestampString(comment.timestamp)) formatTimestamp(
                                comment.timestamp
                            ) else comment.timestamp,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    FlipExpandableText(text = comment.commentText)
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.95f))
                .padding(bottom = 64.dp, top = 8.dp, start = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(onClick = {
                onClickSelectImages()

            }) {
                Icon(
                    imageVector = vectorResource(Resources.Icon.CreatePostImage),
                    contentDescription = "Attach image",
                    tint = Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(16.dp))

//            Column (modifier = Modifier.padding(6.dp)){
//
//                // Selected Images Preview
//                if (screenState.images.isNotEmpty()) {
//                    MediaPreviewSection(
//                        title = "Images (${screenState.images.size})",
//                        items = screenState.images,
//                        onRemove = { item ->
//                            onRemoveImage(item)
//                        }
//                    )
//                }
//            }

//
//            IconButton(
//                modifier = Modifier.size(48.dp).padding(top = 8.dp, bottom = 3.dp),
//                onClick = { /* Handle camera */ }) {
//                Icon(
//                    imageVector = vectorResource(Resources.Icon.Camera),
//                    contentDescription = "Open camera",
//                    tint = Color.Gray
//                )
//            }
        }

    }
}
