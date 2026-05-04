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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flipverse.shared.Alpha
import com.flipverse.shared.Resources
import com.flipverse.shared.WorkSansBoldFont
import openWebBrowser

import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyAndSafetyScreen(
    onNavigateBack: () -> Unit
) {
    var protectYourPosts by remember { mutableStateOf(false) }
    var protectYourVideos by remember { mutableStateOf(false) }
    var photoTagging by remember { mutableStateOf(true) }
    var discoverableByEmail by remember { mutableStateOf(true) }
    var discoverableByPhoneNumber by remember { mutableStateOf(false) }
    var personalizeAds by remember { mutableStateOf(true) }
    var personalizeBasedOnIdentity by remember { mutableStateOf(false) }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Privacy and Safety",
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

            // Audience and Tagging Section
//            SectionHeader(text = "Audience and tagging")
//            Spacer(modifier = Modifier.height(12.dp))
//
//            PrivacySettingItem(
//                icon = painterResource(Resources.Icon.Privacy),
//                title = "Protect your posts",
//                subtitle = "When selected, your posts and other account information are only visible to people who follow you.",
//                isEnabled = protectYourPosts,
//                onToggle = { protectYourPosts = it }
//            )
//            Spacer(modifier = Modifier.height(8.dp))

//            PrivacySettingItem(
//                icon = painterResource(Resources.Icon.Privacy),
//                title = "Protect your videos",
//                subtitle = "If selected, videos in your posts will not be downloadable.",
//                isEnabled = protectYourVideos,
//                onToggle = { protectYourVideos = it }
//            )
//            Spacer(modifier = Modifier.height(8.dp))

//            PrivacySettingItem(
//                icon = painterResource(Resources.Icon.Account),
//                title = "Photo tagging",
//                subtitle = "Anyone can tag you",
//                isEnabled = photoTagging,
//                onToggle = { photoTagging = it }
//            )
//
//            Spacer(modifier = Modifier.height(24.dp))

            // Discoverability and Contacts Section  
//            SectionHeader(text = "Discoverability and contacts")
//            Spacer(modifier = Modifier.height(12.dp))
//
//            PrivacySettingItem(
//                icon = painterResource(Resources.Icon.Notification),
//                title = "Discoverability by email address",
//                subtitle = "Let others find your account using your email address.",
//                isEnabled = discoverableByEmail,
//                onToggle = { discoverableByEmail = it }
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//
//            PrivacySettingItem(
//                icon = painterResource(Resources.Icon.Notification),
//                title = "Discoverability by phone number",
//                subtitle = "Let others find your account using your phone number.",
//                isEnabled = discoverableByPhoneNumber,
//                onToggle = { discoverableByPhoneNumber = it }
//            )

//            Spacer(modifier = Modifier.height(24.dp))

            // Ads Preferences Section
//            SectionHeader(text = "Ads preferences")
//            Spacer(modifier = Modifier.height(12.dp))
//
//            PrivacySettingItem(
//                icon = painterResource(Resources.Icon.Theme),
//                title = "Personalize ads",
//                subtitle = "You will see ads based on your FlipVerse activity.",
//                isEnabled = personalizeAds,
//                onToggle = { personalizeAds = it }
//            )
//            Spacer(modifier = Modifier.height(8.dp))

//            PrivacySettingItem(
//                icon = painterResource(Resources.Icon.Theme),
//                title = "Personalize based on your inferred identity",
//                subtitle = "FlipVerse will use information like your device to personalize your experience.",
//                isEnabled = personalizeBasedOnIdentity,
//                onToggle = { personalizeBasedOnIdentity = it }
//            )

            Spacer(modifier = Modifier.height(32.dp))

            // Legal Section
            SectionHeader(text = "Legal")
            Spacer(modifier = Modifier.height(12.dp))

            LegalMenuItem(
                icon = painterResource(Resources.Icon.Privacy),
                text = "Privacy Policy",
                onClick = {
                    openWebBrowser("https://flipverse.app/start/legal-hub.html#privacy")
                }
            )
            Spacer(modifier = Modifier.height(8.dp))

            LegalMenuItem(
                icon = painterResource(Resources.Icon.EditDocument),
                text = "Terms of Service",
                onClick = {
                    openWebBrowser("https://flipverse.app/start/legal-hub.html#terms")
                }
            )
            Spacer(modifier = Modifier.height(8.dp))

            LegalMenuItem(
                icon = painterResource(Resources.Icon.Warning),
                text = "Guidelines",
                onClick = {
                    openWebBrowser("https://flipverse.app/start/legal-hub.html#guidelines")
                }
            )
            Spacer(modifier = Modifier.height(8.dp))

            LegalMenuItem(
                icon = painterResource(Resources.Icon.Settings),
                text = "Cookie Policy",
                onClick = {
                    openWebBrowser("https://flipverse.app/start/legal-hub.html#cookies")
                }
            )

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
private fun PrivacySettingItem(
    icon: Any,
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = Alpha.DISABLED)
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
                        fontSize = 14.sp,
                        modifier = Modifier.alpha(0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSecondary,
                    uncheckedTrackColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
private fun LegalMenuItem(
    icon: Any,
    text: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = Alpha.DISABLED)
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (icon) {
                    is ImageVector -> {
                        Icon(
                            imageVector = icon,
                            contentDescription = "$text icon",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    is Painter -> {
                        Icon(
                            painter = icon,
                            contentDescription = "$text icon",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Icon(
                imageVector = vectorResource(Resources.Icon.ArrowRight),
                contentDescription = "Go to $text",
                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
