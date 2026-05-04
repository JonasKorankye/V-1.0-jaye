package com.flipverse.shared.presentation.component

import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flipverse.shared.BlackLight
import com.flipverse.shared.White
import com.flipverse.shared.WorkSansBoldFont
import org.jetbrains.compose.resources.DrawableResource


@Composable
fun FlipButton(
    modifier: Modifier = Modifier,
    text: String,
    icon: DrawableResource? = null,
    enabled: Boolean = true,
    containerColor: Color? = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
) {
    containerColor?.let { ButtonDefaults.buttonColors(containerColor = it) }?.let {
        Button(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSecondary,
                    shape = RoundedCornerShape(size = 99.dp)
                )
                .height(48.dp),
            enabled = enabled,
            colors = it,
        ) {
            Text(
                text,
                color = if(enabled) BlackLight else MaterialTheme.colorScheme.onPrimary,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }

}