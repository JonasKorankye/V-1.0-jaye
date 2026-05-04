package com.flipverse.shared.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight.Companion.SemiBold
import androidx.compose.ui.unit.dp
import com.flipverse.shared.domain.MediaItem


@Composable
fun MediaPreviewSection(
    title: String,
    items: List<MediaItem>,
    onRemove: (MediaItem) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
//        Text(
//            text = title,
//            style = MaterialTheme.typography.titleSmall,
//            fontWeight = SemiBold,
//            color = MaterialTheme.colorScheme.onSurface
//        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                MediaPreviewItem(
                    item = item,
                    onRemove = { onRemove(item) }
                )
            }
        }
    }
}