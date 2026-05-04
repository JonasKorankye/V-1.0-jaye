package com.flipverse.shared.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.flipverse.shared.Alpha

@Composable
fun GrayPlaceholderImage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(96.dp)
            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            .background(color = Color.Gray.copy(alpha = Alpha.HALF), shape = CircleShape) // Sets the background color to gray
            .clip(CircleShape)
    ) {
    }
}