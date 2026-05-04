package com.flipverse.dashboard.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flipverse.data.util.formatCount
import com.flipverse.shared.Resources
import org.jetbrains.compose.resources.painterResource

@Composable
fun EngagementButton(
    icon: Painter,
    count: Int,
    contentDescription: String,
    isPostLiked: Boolean? = null,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onClick() }) {
        Icon(
            painter = if (isPostLiked == true)
                painterResource(Resources.Icon.LikeSelected) else {
                icon
            },
            tint = if (isPostLiked == true) Color.Red else MaterialTheme.colorScheme.onSecondary,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = formatCount(count),
            color = MaterialTheme.colorScheme.onSecondary,
            fontSize = 14.sp
        )
    }
}