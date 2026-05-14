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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flipverse.shared.Alpha
import com.flipverse.shared.PreferencesRepository.getEmail
import com.flipverse.shared.PreferencesRepository.getFullName
import com.flipverse.shared.PreferencesRepository.getUsername
import com.flipverse.shared.Red
import com.flipverse.shared.Resources
import com.flipverse.shared.WorkSansBoldFont
import com.flipverse.shared.White
import com.flipverse.data.util.normalizeUsername
import com.flipverse.shared.Ash
import com.flipverse.shared.BlackLight
import openWebBrowser
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YourAccountScreen(
    onNavigateBack: () -> Unit,
    navigateToAccountInformation: () -> Unit = {},
    onNavigateToChangePassword: () -> Unit = {},
    navigateToTwoFactorAuth: () -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDownloadDataDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Your Account",
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

            // Account Overview Card
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
                        text = "Account overview",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = getFullName(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = normalizeUsername(getUsername()),
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = getEmail(),
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Account Information Section
            SectionHeader(text = "Account information")
            Spacer(modifier = Modifier.height(12.dp))

            AccountMenuItem(
                icon = painterResource(Resources.Icon.Account),
                title = "Account information",
                subtitle = "See your account information like your phone number and email address.",
                onClick = { navigateToAccountInformation() }
            )
            Spacer(modifier = Modifier.height(8.dp))

            AccountMenuItem(
                icon = painterResource(Resources.Icon.Privacy),
                title = "Change your password",
                subtitle = "Change your password at any time.",
                onClick = { onNavigateToChangePassword() }
            )
//            Spacer(modifier = Modifier.height(8.dp))
//
//            AccountMenuItem(
//                icon = painterResource(Resources.Icon.Privacy),
//                title = "Two-factor authentication",
//                subtitle = "Help protect your account with an additional security step when you sign in.",
//                onClick = { navigateToTwoFactorAuth() }
//            )

            Spacer(modifier = Modifier.height(24.dp))

            // Data and Permissions Section
//            SectionHeader(text = "Data and permissions")
//            Spacer(modifier = Modifier.height(12.dp))
//
//            AccountMenuItem(
//                icon = painterResource(Resources.Icon.Support),
//                title = "Apps and sessions",
//                subtitle = "See information about when you accessed your account and the apps you connected to it.",
//                onClick = {
//                    openWebBrowser("https://twitter.com/settings/sessions")
//                }
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//
//            AccountMenuItem(
//                icon = painterResource(Resources.Icon.Support),
//                title = "Download an archive of your data",
//                subtitle = "Get insights into the type of information stored for your account.",
//                onClick = { showDownloadDataDialog = true }
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//
//            AccountMenuItem(
//                icon = painterResource(Resources.Icon.Privacy),
//                title = "Connected accounts",
//                subtitle = "Manage Google, Apple, and other connected accounts.",
//                onClick = {
//                    openWebBrowser("https://twitter.com/settings/connected_apps")
//                }
//            )

            Spacer(modifier = Modifier.height(24.dp))

            // Additional Resources Section  
//            SectionHeader(text = "Additional resources")
//            Spacer(modifier = Modifier.height(12.dp))
//
//            AccountMenuItem(
//                icon = painterResource(Resources.Icon.Support),
//                title = "Learn about FlipVerse for professionals",
//                subtitle = "Get helpful tips, the latest news, and more.",
//                onClick = {
//                    openWebBrowser("https://business.twitter.com")
//                }
//            )
//
//            Spacer(modifier = Modifier.height(32.dp))

            // Account Deletion - Danger Zone
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Red.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDeleteDialog = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(Resources.Icon.Delete),
                            contentDescription = "Delete account",
                            tint = Red,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Delete your account",
                                color = Red,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Permanently delete your FlipVerse account and personal data",
                                color = Red.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        }
                        Icon(
                            imageVector = vectorResource(Resources.Icon.ArrowRight),
                            contentDescription = "Go to delete account",
                            tint = Red.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.padding(64.dp))
        }
    }

    // Delete Account Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = White,
            title = {
                Text(
                    text = "Delete account?",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This starts permanent account deletion. You should not need to create a password or use customer support to finish. If deletion is completed on the web, this button opens the direct deletion page.",
                    color = BlackLight
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        openWebBrowser("https://flipverse.app/start/request-deletion.html")
                    }
                ) {
                    Text("Continue to deletion", color = Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel", color = BlackLight)
                }
            }
        )
    }

    // Download Data Dialog
    if (showDownloadDataDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadDataDialog = false },
            containerColor = Ash,
            title = {
                Text(
                    text = "Request your archive",
                    color = White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "You'll get an email with a link to download your FlipVerse data. This may take up to 24 hours.",
                    color = MaterialTheme.colorScheme.onSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDownloadDataDialog = false
                        // Handle data download request
                    }
                ) {
                    Text("Request archive", color = MaterialTheme.colorScheme.primaryContainer)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDownloadDataDialog = false }
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSecondary)
                }
            }
        )
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
private fun AccountMenuItem(
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
