package com.flipverse.shared.presentation.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Clock


/**
 * A Composable that displays a potentially long text with a "READ MORE" toggle.
 *
 * @param text The full text content to display.
 * @param modifier Modifier to be applied to the root Column.
 * @param minLines The minimum number of lines to display before truncating.
 * @param expandText The text for the "READ MORE" button.
 * @param collapseText The text for the "READ LESS" button.
 * @param expandTextColor The color of the "READ MORE/LESS" button.
 */
@Composable
fun FlipExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    minLines: Int = 3, // Display 3 lines by default
    expandText: String = "READ MORE",
    collapseText: String = "READ LESS",
    expandTextColor: Color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onPrimary // Use primary color for the link
) {
    var expanded by remember { mutableStateOf(false) } // State to track if text is expanded
    var hasOverflow by remember { mutableStateOf(false) } // State to track if text has overflow

    Column (modifier = modifier.fillMaxWidth()) {
        Text(
            text = text,
            // Use local text style from MaterialTheme or define your own
            style = LocalTextStyle.current,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier
                .fillMaxWidth()
                // Animate content size changes smoothly
                .animateContentSize(animationSpec = tween(durationMillis = 300)),
            maxLines = if (expanded) Int.MAX_VALUE else minLines, // Set max lines based on expanded state
            overflow = TextOverflow.Ellipsis, // Show ellipsis if content overflows minLines
            onTextLayout = { textLayoutResult ->
                // Check if text would overflow when collapsed
                if (!expanded) {
                    hasOverflow = textLayoutResult.hasVisualOverflow
                }
            }
        )

        // Show button if there's overflow (when collapsed) OR if currently expanded
        if (hasOverflow || expanded) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = expandTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.dp.value.sp
                            )
                        ) {
                            append(if (expanded) collapseText else expandText)
                        }
                    },
                    modifier = Modifier
                        .clickable { expanded = !expanded } // Toggle expanded state on click
                        .padding(top = 4.dp) // Small padding above the link
                )
            }
        }
    }
}