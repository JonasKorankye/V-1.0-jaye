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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.flipverse.shared.CoffeeDark
import com.flipverse.shared.Resources
import com.flipverse.shared.util.openEmailApp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    onNavigateBack: () -> Unit
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Help Center",
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

            // Quick Help Section
//            SectionHeader(text = "Quick help")
//            Spacer(modifier = Modifier.height(12.dp))
//
//            SupportItem(
//                icon = painterResource(Resources.Icon.Support),
//                title = "Getting started",
//                subtitle = "Learn the basics of using FlipVerse",
//                onClick = {
//                    openWebBrowser("https://help.twitter.com/en/getting-started")
//                }
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//
//            SupportItem(
//                icon = painterResource(Resources.Icon.Account),
//                title = "Managing your account",
//                subtitle = "Profile, settings, and account management",
//                onClick = {
//                    openWebBrowser("https://help.twitter.com/en/managing-your-account")
//                }
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//
//            SupportItem(
//                icon = painterResource(Resources.Icon.Privacy),
//                title = "Safety and security",
//                subtitle = "Protecting your account and data",
//                onClick = {
//                    openWebBrowser("https://help.twitter.com/en/safety-and-security")
//                }
//            )
//
//            Spacer(modifier = Modifier.height(24.dp))

            // Content & Features Section
//            SectionHeader(text = "Content & features")
//            Spacer(modifier = Modifier.height(12.dp))
//
//            SupportItem(
//                icon = painterResource(Resources.Icon.Add),
//                title = "Posting and engagement",
//                subtitle = "How to post, like, share, and interact",
//                onClick = {
//                    openWebBrowser("https://help.twitter.com/en/using-twitter")
//                }
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//
//            SupportItem(
//                icon = painterResource(Resources.Icon.Notification),
//                title = "Notifications",
//                subtitle = "Managing your notification preferences",
//                onClick = {
//                    openWebBrowser("https://help.twitter.com/en/notifications")
//                }
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//
//            SupportItem(
//                icon = painterResource(Resources.Icon.Theme),
//                title = "Accessibility",
//                subtitle = "Making FlipVerse work better for you",
//                onClick = {
//                    openWebBrowser("https://help.twitter.com/en/accessibility")
//                }
//            )
//
//            Spacer(modifier = Modifier.height(24.dp))

            // Policies Section
//            SectionHeader(text = "Policies & reporting")
//            Spacer(modifier = Modifier.height(12.dp))
//
//            SupportItem(
//                icon = painterResource(Resources.Icon.Privacy),
//                title = "FlipVerse Rules",
//                subtitle = "Community guidelines and platform rules",
//                onClick = {
//                    openWebBrowser("https://help.twitter.com/en/rules-and-policies/twitter-rules")
//                }
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//
//            SupportItem(
//                icon = painterResource(Resources.Icon.Support),
//                title = "Report a problem",
//                subtitle = "Report bugs, abuse, or policy violations",
//                onClick = {
//                    openWebBrowser("https://help.twitter.com/forms/general")
//                }
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//
//            SupportItem(
//                icon = painterResource(Resources.Icon.Privacy),
//                title = "Copyright and trademark",
//                subtitle = "Intellectual property information",
//                onClick = {
//                    openWebBrowser("https://help.twitter.com/en/rules-and-policies/copyright-policy")
//                }
//            )
//
//            Spacer(modifier = Modifier.height(24.dp))

            // Contact Us Section
            SectionHeader(text = "Contact us")
            Spacer(modifier = Modifier.height(12.dp))

            ContactSupportItem(
                icon = painterResource(Resources.Icon.HelpCenter),
                title = "Support ticket",
                subtitle = "Submit a support request for personalized help",
                onClick = {
                    openEmailApp("support@flipverse.app")
                }
            )
            Spacer(modifier = Modifier.height(8.dp))

//            ContactSupportItem(
//                icon = painterResource(Resources.Icon.Account),
//                title = "Community forums",
//                subtitle = "Connect with other users and get help",
//                onClick = {
//                    openWebBrowser("https://twittercommunity.com/")
//                }
//            )

            Spacer(modifier = Modifier.height(32.dp))

            // Additional Resources
//            Card(
//                modifier = Modifier.fillMaxWidth(),
//                colors = CardDefaults.cardColors(
//                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
//                ),
//                shape = RoundedCornerShape(12.dp)
//            ) {
//                Column(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(16.dp)
//                ) {
//                    Text(
//                        text = "More resources",
//                        color = MaterialTheme.colorScheme.onPrimary,
//                        fontSize = 16.sp,
//                        fontWeight = FontWeight.Bold
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//
//                    Text(
//                        text = "• FlipVerse Status: Check service status\n• Developer Portal: API and developer tools\n• Transparency Report: Platform transparency data",
//                        color = MaterialTheme.colorScheme.onSecondary,
//                        fontSize = 14.sp,
//                        lineHeight = 20.sp
//                    )
//                }
//            }

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
private fun SupportItem(
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

@Composable
private fun ContactSupportItem(
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
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
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
                            tint = CoffeeDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    is Painter -> {
                        Icon(
                            painter = icon,
                            contentDescription = "$title icon",
                            tint = CoffeeDark,
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
                tint = CoffeeDark,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
