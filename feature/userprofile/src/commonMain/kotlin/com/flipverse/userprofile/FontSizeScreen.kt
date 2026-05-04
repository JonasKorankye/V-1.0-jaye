package com.flipverse.userprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flipverse.shared.FontSizePreference
import com.flipverse.shared.FontSizePreferenceManager
import com.flipverse.shared.Resources
import com.flipverse.shared.WorkSansBoldFont
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontSizeScreen(
    onNavigateBack: () -> Unit
) {
    var selectedFontSize by remember { mutableStateOf(FontSizePreferenceManager.getFontSizePreference()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Font Size",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = WorkSansBoldFont(),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(
                            imageVector = vectorResource(Resources.Icon.BackArrow),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.primary
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary)
                .verticalScroll(rememberScrollState())
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding()
                )
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Font size selection section
            SectionHeader(text = "Font Size")
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Choose the text size that's most comfortable for you to read.",
                color = MaterialTheme.colorScheme.onSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Font size options
            FontSizePreference.values().forEach { preference ->
                FontSizeOptionCard(
                    icon = painterResource(Resources.Icon.Theme),
                    title = preference.displayName,
                    subtitle = "Sample text in ${preference.displayName.lowercase()} size",
                    fontSize = (16f * preference.scale).sp,
                    isSelected = selectedFontSize == preference,
                    onClick = {
                        selectedFontSize = preference
                        FontSizePreferenceManager.saveFontSizePreference(preference)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Preview section
            SectionHeader(text = "Preview")
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Preview Text",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = (18f * selectedFontSize.scale).sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = WorkSansBoldFont()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "This is how regular text will appear throughout FlipVerse with your selected font size. You can adjust this setting anytime from the Display settings.",
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontSize = (14f * selectedFontSize.scale).sp,
                        lineHeight = (20f * selectedFontSize.scale).sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Note",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = (16f * selectedFontSize.scale).sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Font size changes may require restarting the app to take full effect on all screens. Some elements may need time to update.",
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontSize = (14f * selectedFontSize.scale).sp,
                        lineHeight = (20f * selectedFontSize.scale).sp
                    )
                }
            }

            Spacer(modifier = Modifier.padding(64.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onPrimary,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = WorkSansBoldFont()
    )
}

@Composable
private fun FontSizeOptionCard(
    icon: Any,
    title: String,
    subtitle: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.2f)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (icon) {
                    is ImageVector -> {
                        Icon(
                            imageVector = icon,
                            contentDescription = "$title icon",
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    is Painter -> {
                        Icon(
                            painter = icon,
                            contentDescription = "$title icon",
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontSize = fontSize * 0.875f // Slightly smaller for subtitle
                    )
                }
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedColor = MaterialTheme.colorScheme.onSecondary
                )
            )
        }
    }
}