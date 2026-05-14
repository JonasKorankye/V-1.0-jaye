package com.flipverse.auth.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.flipverse.shared.FontSize
import com.flipverse.shared.Resources
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun AppleButton(
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
    primaryText: String = "Continue with Apple",
    secondaryText: String = "Please wait...",
    icon: DrawableResource = if (isSystemInDarkTheme()) Resources.Image.AppleBlack else Resources.Image.AppleWhite,
    shape: Shape = RoundedCornerShape(size = 99.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.onPrimary,
    borderColor: Color = MaterialTheme.colorScheme.onSecondary,
    progressIndicator: Color = MaterialTheme.colorScheme.onPrimary,
    onClick: () -> Unit,
) {
    var buttonText by rememberSaveable { mutableStateOf(primaryText) }

    LaunchedEffect(loading) {
        buttonText = if (loading) secondaryText else primaryText
    }

    Surface(
        modifier = modifier
            .clip(shape = shape)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape
            )
            .clickable(enabled = enabled && !loading) { onClick() },
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp)
                .animateContentSize(
                    animationSpec = tween(200),
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = !loading
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = "Apple Logo",
                    tint = Color.Unspecified
                )
            }
            AnimatedVisibility(
                visible = loading
            ) {
                AdaptiveCircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = progressIndicator
                )
//                CircularProgressIndicator(
//                    modifier = Modifier.size(24.dp),
//                    strokeWidth = 2.dp,
//                    color = progressIndicator
//                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = buttonText,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.SansSerif,
                fontSize = FontSize.MEDIUM
            )
        }
    }

}
