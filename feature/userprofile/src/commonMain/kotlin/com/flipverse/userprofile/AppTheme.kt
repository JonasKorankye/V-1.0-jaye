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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flipverse.shared.Alpha
import com.flipverse.shared.PreferencesRepository
import com.flipverse.shared.Resources
import com.flipverse.shared.WorkSansBoldFont
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource

// Theme preference enum
enum class ThemePreference(val displayName: String, val key: String) {
    SYSTEM_DEFAULT("Use device setting", "system_default"),
    LIGHT("Light", "light"),
    DARK("Dark", "dark")
}

// Theme preference manager using centralized PreferencesRepository
object ThemePreferenceManager {
    fun saveThemePreference(preference: ThemePreference) {
        PreferencesRepository.saveThemePreference(preference.key)
    }

    fun getThemePreference(): ThemePreference {
        val savedKey = PreferencesRepository.getThemePreference()
        return ThemePreference.values().find { it.key == savedKey }
            ?: ThemePreference.SYSTEM_DEFAULT
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppThemeScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFontSize: () -> Unit = {}
) {
    var selectedTheme by remember { mutableStateOf(ThemePreferenceManager.getThemePreference()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Display",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.SansSerif,
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

            // Theme selection section
            SectionHeader(text = "Appearance")
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Manage how FlipVerse looks on your device.",
                color = MaterialTheme.colorScheme.onSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // System Default Option
            ThemeOptionCard(
                icon = painterResource(Resources.Icon.Theme),
                title = ThemePreference.SYSTEM_DEFAULT.displayName,
                subtitle = "FlipVerse will match your device's system setting",
                isSelected = selectedTheme == ThemePreference.SYSTEM_DEFAULT,
                onClick = {
                    selectedTheme = ThemePreference.SYSTEM_DEFAULT
                    ThemePreferenceManager.saveThemePreference(ThemePreference.SYSTEM_DEFAULT)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Light Theme Option
            ThemeOptionCard(
                icon = painterResource(Resources.Icon.Theme),
                title = ThemePreference.LIGHT.displayName,
                subtitle = "Light theme with bright backgrounds",
                isSelected = selectedTheme == ThemePreference.LIGHT,
                onClick = {
                    selectedTheme = ThemePreference.LIGHT
                    ThemePreferenceManager.saveThemePreference(ThemePreference.LIGHT)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Dark Theme Option  
            ThemeOptionCard(
                icon = painterResource(Resources.Icon.Theme),
                title = ThemePreference.DARK.displayName,
                subtitle = "Dark theme with darker backgrounds to reduce eye strain",
                isSelected = selectedTheme == ThemePreference.DARK,
                onClick = {
                    selectedTheme = ThemePreference.DARK
                    ThemePreferenceManager.saveThemePreference(ThemePreference.DARK)
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Additional Display Options Section
            SectionHeader(text = "Additional options")
            Spacer(modifier = Modifier.height(12.dp))

            DisplayOptionItem(
                icon = painterResource(Resources.Icon.Theme),
                title = "Font size",
                subtitle = "Change the size of text throughout FlipVerse",
                onClick = {
                    onNavigateToFontSize()
                }
            )
//            Spacer(modifier = Modifier.height(8.dp))
//
//            DisplayOptionItem(
//                icon = painterResource(Resources.Icon.Support),
//                title = "Accessibility",
//                subtitle = "Manage accessibility features",
//                onClick = {
//                    // Handle accessibility settings
//                }
//            )

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
                        text = "Theme changes",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Theme changes may require restarting the app to take full effect on all screens.",
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
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
        fontFamily = FontFamily.SansSerif
    )
}

@Composable
private fun ThemeOptionCard(
    icon: Any,
    title: String,
    subtitle: String,
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
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontSize = 14.sp
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

@Composable
private fun DisplayOptionItem(
    icon: Any,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.2f)
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
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    is Painter -> {
                        Icon(
                            painter = icon,
                            contentDescription = "$title icon",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            Icon(
                imageVector = vectorResource(Resources.Icon.ArrowRight),
                contentDescription = "Go to $title",
                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
