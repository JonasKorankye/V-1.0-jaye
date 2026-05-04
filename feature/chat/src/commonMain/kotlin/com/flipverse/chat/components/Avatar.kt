package com.flipverse.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.flipverse.shared.Alpha
import com.flipverse.shared.WorkSansBoldFont

@Composable
fun Avatar(avatarContent: String?, name: String, onClick: () -> Unit){
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (avatarContent?.isNotEmpty() == true) {
            AsyncImage(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onClick() }
                    .clip(CircleShape),
                model = ImageRequest.Builder(
                    LocalPlatformContext.current
                ).data(avatarContent)
                    .crossfade(enable = true)
                    .build(),
                contentDescription = "Author Profile URL",
                contentScale = ContentScale.Crop
            )

        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onClick() }
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = Alpha.HALF)) //  color for the circle
                    .border(2.dp, Color.Transparent, CircleShape), //  border color
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = name.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 16.sp,
                    fontFamily = WorkSansBoldFont()
                )

            }
        }
    }
}