package com.flipverse.dashboard.component

import MessageBarState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.flipverse.data.util.capitalizeFirstLetter
import com.flipverse.data.util.formatTimestamp
import com.flipverse.data.util.normalizeUsername
import com.flipverse.shared.Alpha
import com.flipverse.shared.Black
import com.flipverse.shared.FontSize
import com.flipverse.shared.Red
import com.flipverse.shared.RedLight
import com.flipverse.shared.Resources
import com.flipverse.shared.Resources.Icon.Delete
import com.flipverse.shared.Resources.Icon.Mute
import com.flipverse.shared.Resources.Icon.VisibilityOff
import com.flipverse.shared.domain.Post
import com.flipverse.shared.presentation.component.FlipExpandableText
import com.flipverse.shared.util.openEmailApp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun PostCard(
    post: Post,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    messageBarState: MessageBarState,
    onClickPost: () -> Unit,
    onClickDeletePost: (String) -> Unit,
    onImageClick: (String) -> Unit,
    onClickHidePost: (String) -> Unit,
    onPinPost: (postId: String, isPinned: Boolean) -> Unit,
    onBlockAuthor: (String, (Boolean) -> Unit) -> Unit,
    onReportPost: (postId: String, authorId: String, onComplete: (Boolean) -> Unit) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showModerationActions by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClickPost() }
            .padding(vertical = 2.dp, horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary) // Card background
    ) {
        Column {
            // Author Info and More button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {

                    // Author profile image — always show initials fallback, overlay image on top
                    val authorImageUrl = post.authorProfileImage
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { onImageClick(post.authorId) }
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = Alpha.HALF)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Initials always rendered underneath
                        Text(
                            text = post.authorName.take(1).uppercase(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        // AsyncImage renders on top when it loads; on error the initials show through
                        if (!authorImageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalPlatformContext.current)
                                    .data(authorImageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Author Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.padding(bottom = 0.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val formattedAuthorName = formatDisplayName(post.authorName)
                            Text(
                                text = formattedAuthorName,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 14.sp,
                                lineHeight = 12.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formatTimestamp(post.timestamp),
                                color = Color.Gray,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = FontSize.SMALL,
                                lineHeight = 10.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = normalizeUsername(post.authorHandle),
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 14.sp,
                                lineHeight = 14.sp,
                                modifier = Modifier.padding(
                                    start = 0.dp,
                                    end = 0.dp,
                                    top = 8.dp,
                                    bottom = 12.dp
                                )
                                    .height(24.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            if (post.authorIsVerified) {
                                Box(
                                    contentAlignment = Alignment.BottomStart,
                                    modifier = Modifier.size(32.dp).padding(bottom = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = vectorResource(Resources.Icon.VerifiedAccount),
                                        contentDescription = "Verified",
                                        tint = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))

//                            Text(
//                                text = "Subscribe",
//                                color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primaryContainer else Black,
//                                fontFamily = FontFamily.SansSerif,
//                                fontWeight = FontWeight.SemiBold,
//                                fontSize = 12.sp,
//                                lineHeight = 12.sp,
//                                modifier = Modifier.clickable {
//                                    // todo:Logic to handle Subscribe
//
//                                }
//                                    .padding(
//                                        start = 8.dp,
//                                        end = 8.dp,
//                                        top = 10.dp,
//                                        bottom = 10.dp
//                                    )
//                                    .height(24.dp)
//
//                            )
                        }
                    }
                }


                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.wrapContentSize(Alignment.TopCenter)
                ) {
                    Text(
                        text = post.postType.name.capitalizeFirstLetter(),
                        color = Color.Gray,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(
                            end = 10.dp,
                            top = 10.dp,
                            bottom = 5.dp
                        ),
                        fontSize = 10.sp,
                        lineHeight = 10.sp
                    )

                    IconButton(
                        modifier = Modifier.padding(top = 0.dp),
                        onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = vectorResource(Resources.Icon.VerticalMenu),
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = Alpha.DISABLED
                            )
                        ) // Set background for the menu
                    ) {
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = vectorResource(
                                        when (post.postType.name) {
                                            "RECOMMENDATION" -> Resources.Icon.Recommendation
                                            "REVIEW" -> Resources.Icon.Review
                                            else -> Resources.Icon.Quote
                                        }
                                    ),
                                    contentDescription = "Post Type",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            },
                            text = {
                                Text(
                                    text = post.postType.name,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            },
                            onClick = {

                                showMenu = false
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = Color.Gray.copy(alpha = Alpha.DISABLED)
                        )
//                        DropdownMenuItem(
//                            leadingIcon = {
//                                Icon(
//                                    imageVector = vectorResource(Resources.Icon.Pin),
//                                    contentDescription = "Pin Post",
//                                    tint = MaterialTheme.colorScheme.onPrimary
//                                )
//                            },
//                            text = {
//                                Text(
//                                    text = if (post.isPinned) "Unpin Post" else "Pin Post",
//                                    color = MaterialTheme.colorScheme.onPrimary
//                                )
//                            },
//                            onClick = {
//                                onPinPost(post.id, !post.isPinned)
//                                showMenu = false
//                            }
//                        )
//                        DropdownMenuItem(
//                            leadingIcon = {
//                                Icon(
//                                    imageVector = vectorResource(VisibilityOff),
//                                    contentDescription = "Mute",
//                                    tint = MaterialTheme.colorScheme.onPrimary
//                                )
//                            },
//                            text = {
//                                Text(
//                                    "Hide Post",
//                                    color = MaterialTheme.colorScheme.onPrimary
//                                )
//                            },
//                            onClick = {
//                                // Handle Hide action
//                                onClickHidePost(post.id)
//                                showMenu = false
//                            }
//                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = vectorResource(Resources.Icon.Warning),
                                    contentDescription = "Report post",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            },
                            text = {
                                Text(
                                    "Report post",
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            },
                            onClick = {
                                showMenu = false
                                onReportPost(post.id, post.authorId) { didSucceed ->
                                    if (didSucceed) {
                                        showModerationActions = true
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(Resources.Icon.Delete),
                                    contentDescription = "Block author",
                                    tint = Red
                                )
                            },
                            text = {
                                Text(
                                    "Block author",
                                    color = Red
                                )
                            },
                            onClick = {
                                showMenu = false
                                onBlockAuthor(post.authorId) { didSucceed ->
                                    if (didSucceed) {
                                        onClickHidePost(post.id)
                                        messageBarState.addSuccess("Author blocked. Their content has been removed from your feed.")
                                    }
                                }
                            }
                        )


                        Spacer(modifier = Modifier.height(8.dp))
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = vectorResource(Delete),
                                    contentDescription = "Delete",
                                    tint = Red
                                )
                            },
                            text = {
                                Text(
                                    "Delete",
                                    color = Red
                                )
                            }, // Typically delete is red
                            onClick = {
                                // Handle Delete action
                                onClickDeletePost(post.id)
                                showMenu = false
                            }
                        )
                    }

                }
            }

            if (showModerationActions) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Report sent",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "FlipVerse reviews reports and removes objectionable content or abusive users. For urgent moderation issues, contact support directly.",
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontSize = 13.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { openEmailApp("support@flipverse.app") }) {
                                Text("Contact support", color = MaterialTheme.colorScheme.onPrimary)
                            }
                            TextButton(onClick = { showModerationActions = false }) {
                                Text("Dismiss", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }

//            Spacer(modifier = Modifier.height(2.dp))
            val normalizedTags = post.tags.map { normalizeUsername(it) }
            val content =
                post.whatsNew + "\n" + "\n" + "Source: " + post.source + "\n" + "Tags:" + normalizedTags.joinToString(
                    ", "
                )

            FlipExpandableText(text = content)

            post.imageUrls.firstOrNull()?.let { imageUrl ->
                if (imageUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    AsyncImage(
                        model = ImageRequest.Builder(LocalPlatformContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Post Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Engagement counts from ViewModel (real-time updated) ---
            // Remove local timer approach and rely on ViewModel's real-time updates
            val likesCount = post.engagement.likesCount
            val commentsCount = post.engagement.commentsCount

            // Engagement Row (Likes, Comments, Shares)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EngagementButton(
                    painterResource(Resources.Icon.Like),
                    count = likesCount,
                    isPostLiked = post.engagement.isLikedByCurrentUser,
                    contentDescription = "Likes",
                    onClick = onLikeClick
                )
                EngagementButton(
                    painterResource(Resources.Icon.Comment),
                    count = commentsCount,
                    contentDescription = "Comments",
                    onClick = onCommentClick
                )
                Icon(
                    imageVector = vectorResource(Resources.Icon.Share),
                    contentDescription = "Share actions",
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.clickable { onShareClick() }
                )

//                Icon(
//                    imageVector = vectorResource(
//                        when (post.postType.name) {
//                            "RECOMMENDATION" -> Resources.Icon.Recommendation
//                            "REVIEW" -> Resources.Icon.Review
//                            else -> Resources.Icon.Quote
//                        }
//                    ),
//                    contentDescription = "Post Type actions",
//                    tint = MaterialTheme.colorScheme.onSecondary,
//                    modifier = Modifier.clickable {
//                        messageBarState.addSuccess("This post is a ${post.postType.name}")
//
//                    }
//                )
            }
        }
    }
}

/**
 * Formats a display name for proper presentation.
 * Names like "The Kweku" are concatenated as "TheKweku" and displayed fully as such.
 * This ensures "The" prefix names are kept together as a single entity.
 */
private fun formatDisplayName(name: String): String {
    if (name.isBlank()) return ""

    // Remove spaces from names that start with "The " to make them single names
    // e.g., "The Kweku" -> "TheKweku", "The Alfy" -> "TheAlfy"
    val concatenated = if (name.startsWith("The ", ignoreCase = true)) {
        "The" + name.substring(4).trim() // Remove "The " and concatenate
    } else {
        name
    }

    // Ensure proper capitalization
    return concatenated.replaceFirstChar { it.uppercase() }
}
