package com.flipverse.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.flipverse.chat.avatar.AvatarCustomizationOption
import com.flipverse.chat.avatar.AvatarCustomizations
import com.flipverse.chat.avatar.AvatarStyle
import com.flipverse.chat.avatar.CustomizationValue
import com.flipverse.shared.Alpha
import com.flipverse.shared.BlackLight
import com.flipverse.shared.Gray
import com.flipverse.shared.Resources
import org.jetbrains.compose.resources.vectorResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarCustomizationPanel(
    style: AvatarStyle,
    currentCustomizations: AvatarCustomizations,
    onCustomizationChange: (String, String?) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Customize Avatar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = vectorResource(Resources.Icon.Close),
                        contentDescription = "Close customization",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = Color.Gray.copy(alpha = Alpha.DISABLED)
            )

            if (style.customizationOptions.isEmpty()) {
                // No customization available for this style
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No customization available",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This avatar style doesn't support customization yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Customization options
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(style.customizationOptions) { option ->
                        CustomizationOptionSection(
                            option = option,
                            currentValue = getCurrentCustomizationValue(
                                currentCustomizations,
                                option.key
                            ),
                            onValueChange = { value -> onCustomizationChange(option.key, value) }
                        )
                    }

                    item{
                        Spacer(modifier = Modifier.height(56.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomizationOptionSection(
    option: AvatarCustomizationOption,
    currentValue: String?,
    onValueChange: (String?) -> Unit
) {
    Column {
        Text(
            text = option.displayName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Add "None" option if not already present
            if (option.values.none { it.value == "blank" || it.displayName == "None" }) {
                item {
                    CustomizationValueItem(
                        value = CustomizationValue("", "None"),
                        isSelected = currentValue.isNullOrEmpty(),
                        onClick = { onValueChange(null) }
                    )
                }
            }

            items(option.values) { value ->
                CustomizationValueItem(
                    value = value,
                    isSelected = currentValue == value.value,
                    onClick = { onValueChange(value.valueCode) }
                )
            }
        }
    }
}

@Composable
private fun CustomizationValueItem(
    value: CustomizationValue,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(60.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (value.previewUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(value.previewUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = value.displayName,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
            } else if (value.value.isEmpty() || value.displayName == "None") {
                Icon(
                    imageVector = vectorResource(Resources.Icon.Close),
                    contentDescription = "None",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                // Show color if it's a color value (hex pattern)
                if (value.value.matches(Regex("^[a-fA-F0-9]{6}$"))) {
                    val hexColor = value.value
                    val red = hexColor.substring(0, 2).toInt(16)
                    val green = hexColor.substring(2, 4).toInt(16)
                    val blue = hexColor.substring(4, 6).toInt(16)

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(red, green, blue))
                            .border(
                                width = 1.dp,
                                color = Gray.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    )
                } else {
                    // Show first letter of display name
                    Text(
                        text = value.displayName.take(1),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected)
                            BlackLight
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            fontSize = 10.sp
        )
    }
}

private fun getCurrentCustomizationValue(
    customizations: AvatarCustomizations,
    key: String
): String? {
    return when (key) {
        "accessories" -> customizations.accessories
        "accessoriesColor" -> customizations.accessoriesColor
        "top" -> customizations.top
        "hair" -> customizations.top // For adventurer style
        "hairColor" -> customizations.hairColor
        "facialHair" -> customizations.facialHair
        "facialHairColor" -> customizations.facialHairColor
        "eyes" -> customizations.eyes
        "eyebrows" -> customizations.eyebrows
        "mouth" -> customizations.mouth
        "skinColor" -> customizations.skinColor
        "clothing" -> customizations.clothing
        "clothesColor" -> customizations.clothesColor
        "clothingGraphic" -> customizations.clothingGraphic
        "nose" -> customizations.nose
        "hatColor" -> customizations.hatColor
        else -> null
    }
}