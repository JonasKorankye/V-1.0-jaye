package com.flipverse.shared.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flipverse.shared.Resources
import com.flipverse.shared.domain.ImageItem
import com.flipverse.shared.domain.MediaItem
import com.flipverse.shared.domain.VideoItem
import com.mohamedrejeb.calf.picker.toImageBitmap
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import org.jetbrains.compose.resources.vectorResource

@Composable
fun MediaPreviewItem(
    item: MediaItem,
    onRemove: () -> Unit,
    onItemClick: () -> Unit = {}
) {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    // Load image from ByteArray
    LaunchedEffect(item.data) {
        try {
            isLoading = true
            hasError = false
            imageBitmap = item.data.toImageBitmap()
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
            hasError = true
            println("Error loading image: ${e.message}")
        }
    }

    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .clickable { onItemClick() }
    ) {
        when {
            isLoading -> {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    AdaptiveCircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            hasError || imageBitmap == null -> {
                // Error state - show file type icon
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            when (item) {
                                is ImageItem -> Color(0xFFFFEBEE)
                                is VideoItem -> Color(0xFFF3E5F5)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when (item) {
                                is ImageItem -> vectorResource(Resources.Icon.CreatePostImage)
                                is VideoItem -> vectorResource(Resources.Icon.CreatePostImage)
                            },
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = when (item) {
                                is ImageItem -> Color(0xFFD32F2F)
                                is VideoItem -> Color(0xFF7B1FA2)
                            }
                        )
                        Text(
                            text = when (item) {
                                is ImageItem -> "IMG"
                                is VideoItem -> "VID"
                            },
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            else -> {
                // Success state - show actual image
                imageBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap,
                        contentDescription = item.fileName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Video overlay indicator
                    if (item is VideoItem) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(
                                    Color.Black.copy(alpha = 0.6f),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Icon(
                                vectorResource(Resources.Icon.CreatePostImage),
                                contentDescription = "Video",
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Remove button
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .background(
                    Color.Transparent,
                    RoundedCornerShape(12.dp)
                )
        ) {
            Icon(
                vectorResource(Resources.Icon.Close),
                contentDescription = "Remove",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        // File info overlay
//        Box(
//            modifier = Modifier
//                .align(Alignment.BottomStart)
//                .background(
//                    Color.Black.copy(alpha = 0.8f),
//                    RoundedCornerShape(topEnd = 6.dp)
//                )
//                .padding(horizontal = 6.dp, vertical = 2.dp)
//        ) {
//            Column(
//                verticalArrangement = Arrangement.spacedBy(1.dp)
//            ) {
//                Text(
//                    text = formatFileSize(item.size),
//                    color = Color.White,
//                    fontSize = 9.sp,
//                    fontWeight = FontWeight.Medium
//                )
//                if (item.fileName.length > 8) {
//                    Text(
//                        text = item.fileName.take(8) + "...",
//                        color = Color.White.copy(alpha = 0.8f),
//                        fontSize = 8.sp,
//                        maxLines = 1,
//                        overflow = TextOverflow.Ellipsis
//                    )
//                }
//            }
//        }

        // File type indicator
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(
                    when (item) {
                        is ImageItem -> Color(0xFF4CAF50).copy(alpha = 0.9f)
                        is VideoItem -> Color(0xFF2196F3).copy(alpha = 0.9f)
                    },
                    RoundedCornerShape(bottomEnd = 6.dp)
                )
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = when (item) {
                    is ImageItem -> "IMG"
                    is VideoItem -> "VID"
                },
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
