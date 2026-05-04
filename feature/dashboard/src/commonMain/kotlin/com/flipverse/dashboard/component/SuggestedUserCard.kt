package com.flipverse.dashboard.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.flipverse.data.util.normalizeUsername
import com.flipverse.shared.Alpha
import com.flipverse.shared.BlackLight
import com.flipverse.shared.CoffeeDark
import com.flipverse.shared.Resources
import com.flipverse.shared.domain.User
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun SuggestedUserCard(
    user: User,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onThumbnailClick: (String) -> Unit,
    onClose: () -> Unit,
) {
    Card(
        modifier = Modifier.width(160.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = Alpha.DISABLED)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {

            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (user.thumbnail.isNotEmpty()) {
                    AsyncImage(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .clickable { onThumbnailClick(user.email) }
                            .align(Alignment.CenterHorizontally),
                        model = ImageRequest.Builder(
                            LocalPlatformContext.current
                        ).data(user.thumbnail)
                            .crossfade(enable = true)
                            .build(),
                        contentDescription = "Author Profile URL",
                        contentScale = ContentScale.Crop
                    )

                } else {

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .clickable { onThumbnailClick(user.email) }
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = Alpha.HALF)) //  color for the circle
                            .border(2.dp, Color.Transparent, CircleShape), //  border color
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = user.fullname.take(1).uppercase(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.SansSerif,
                        )

                    }
                }

//            Box(
//                modifier = Modifier
//                    .size(48.dp)
//                    .clip(CircleShape)
//                    .background(MaterialTheme.colorScheme.onPrimary),
//                contentAlignment = Alignment.Center
//            ) {
//                Text(
//                    text = user.avatar!!,
//                    fontSize = 24.sp
//                )
//            }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = user.fullname,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (user.isVerified) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            painterResource(Resources.Icon.VerifiedAccount),
                            contentDescription = "Verified",
                            tint = Color(0xFF1DA1F2),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Text(
                    text = normalizeUsername(user.username),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

//            Spacer(modifier = Modifier.height(4.dp))
//
//            Text(
//                text = user.bio!!,
//                fontSize = 11.sp,
//                maxLines = 2,
//                overflow = TextOverflow.Ellipsis,
//                color = MaterialTheme.colorScheme.onSurfaceVariant,
//                lineHeight = 14.sp
//            )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${user.followingCount} followers",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onFollowClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing)
                            MaterialTheme.colorScheme.surface
                        else
                            MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (isFollowing) "Following" else "Follow",
                        fontSize = 12.sp,
                        color = if (isFollowing)
                            CoffeeDark
                        else
                            BlackLight,
                        textAlign = TextAlign.Center
                    )
                }
            }

            IconButton(onClick = { onClose()}) {
                Icon(
                    vectorResource(Resources.Icon.Close),
                    contentDescription = "Dismiss",
                    tint = Color.Gray
                )
            }
        }
    }
}

