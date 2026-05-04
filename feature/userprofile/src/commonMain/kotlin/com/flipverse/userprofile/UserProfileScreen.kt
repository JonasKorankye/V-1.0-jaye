package com.flipverse.userprofile

import ContentWithMessageBar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.flipverse.data.util.normalizeUsername
import com.flipverse.shared.Alpha
import com.flipverse.shared.Black
import com.flipverse.shared.BlackLight
import com.flipverse.shared.LexendMediumFont
import com.flipverse.shared.PreferencesRepository.getEmail
import com.flipverse.shared.PreferencesRepository.getFullName
import com.flipverse.shared.PreferencesRepository.getUsername
import com.flipverse.shared.PreferencesRepository.reset
import com.flipverse.shared.Red
import com.flipverse.shared.Resources
import com.flipverse.shared.WorkSansBoldFont
import com.flipverse.shared.navigation.Screen
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import rememberMessageBarState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    onNavigateBack: () -> Unit,
    navigateToEditProfile: () -> Unit,
    navigateToPrivacyAndSafety: () -> Unit,
    navigateToYourAccount: () -> Unit,
    navigateToSupport: () -> Unit,
    navigateToAppTheme: () -> Unit,
    navigateToViewProfile: (userEmail: String) -> Unit,
    navigateToAuthController: NavController,
) {

    val viewModel = koinViewModel<UserProfileViewModel>()
    val messageBarState = rememberMessageBarState()


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 32.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = WorkSansBoldFont(),
                        modifier = Modifier.padding(start = 0.dp) // Adjust padding if necessary
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
                actions = {
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = MaterialTheme.colorScheme.primary
    ) { paddingValues ->
        ContentWithMessageBar(
            contentBackgroundColor = MaterialTheme.colorScheme.primary,
            messageBarState = messageBarState,
            modifier = Modifier
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding()
                ),
            errorMaxLines = 2,
            fontFamily = FontFamily.SansSerif,
            errorContainerColor = MaterialTheme.colorScheme.error,
            errorContentColor = MaterialTheme.colorScheme.onErrorContainer,
            successContainerColor = MaterialTheme.colorScheme.onSecondaryContainer,
            successContentColor = MaterialTheme.colorScheme.onPrimary,
            showCopyButton = false
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Spacer(modifier = Modifier.height(6.dp))

                    // Profile Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = getFullName(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = normalizeUsername(getUsername()),
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontSize = 14.sp
                            )
                        }
                        // Profile image — reads from reactive ViewModel state,
                        // not directly from preferences, so it updates on upload/delete.
                        ProfileImageWithFallback(
                            imageUrl = viewModel.profileState.thumbnail,
                            fallbackText = getFullName().take(1).uppercase(),
                            size = 80.dp,
                            fontSize = 32.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Edit Profile Button
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable { navigateToEditProfile() },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                alpha = Alpha.HALF
                            )
                        ), // Dark gray background for button
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Edit profile",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Account section
                    SettingsItem(
                        icon = painterResource(Resources.Icon.Account),
                        text = "View Profile",
                        onClick = { navigateToViewProfile(getEmail()) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsItem(
                        icon = painterResource(Resources.Icon.Account),
                        text = "Your Account",
                        onClick = { navigateToYourAccount() }
                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//                    SettingsItem(
//                        icon = painterResource(Resources.Icon.Theme), // This is a generic edit icon, you might need a specific 'display' icon if available
//                        text = "App Theme",
//                        onClick = { /* Handle Display click */ }
//                    )
//            Spacer(modifier = Modifier.height(8.dp))
//            SettingsItem(
//                icon = painterResource(Resources.Icon.Notification),
//                text = "Notifications",
//                onClick = { /* Handle Notifications click */ }
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//            SettingsItem(
//                icon = painterResource(Resources.Icon.Repost), // This is a generic payments icon
//                text = "Payments",
//                onClick = { /* Handle Payments click */ }
//            )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsItem(
                        icon = painterResource(Resources.Icon.Privacy), // This is a generic privacy icon
                        text = "Privacy & Safety",
                        onClick = { navigateToPrivacyAndSafety() }
                    )

                    Spacer(modifier = Modifier.height(8.dp)) // Spacer between Privacy & Safety and Feedback

                    // Feedback & Support
                    SettingsItem(
                        icon = painterResource(Resources.Icon.Theme), // This is a generic feedback icon
                        text = "App Theme",
                        onClick = { navigateToAppTheme() }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsItem(
                        icon = painterResource(Resources.Icon.Support), // This is a generic support icon
                        text = "Support",
                        onClick = { navigateToSupport()}
                    )

                    Spacer(modifier = Modifier.height(8.dp)) // Spacer between Support and Sign Out

                    // Sign Out
                    SettingsItem(
                        icon = painterResource(Resources.Icon.SignOut), // Using a placeholder for sign out icon, replace if you have a specific one
                        text = "Sign Out",
                        onClick = {
                            viewModel.signOut(
                                onSuccess = navigateToAuthController.navigate(Screen.Auth) {
                                    popUpTo(navigateToAuthController.graph.startDestinationId) {
                                        inclusive = true
                                    }
                                    // Optionally, launchSingleTop if you want to avoid multiple instances of Login
                                    launchSingleTop = true
                                    reset() //clear all data
                                },
                                onError = { message ->
                                    messageBarState.addError(message)
                                }
                            )
                        },
                        isSignOut = true
                    )
                }


                // Version info at the bottom
//                Text(
//                    text = "Version 1.00.0",
//                    color = MaterialTheme.colorScheme.onPrimary,
//                    fontSize = 12.sp,
//                    modifier = Modifier
//                        .align(Alignment.CenterHorizontally)
//                        .padding(bottom = 16.dp)
//                )

                Spacer(modifier = Modifier.padding(64.dp))
            }
        }
    }
}


@Composable
fun SettingsItem(
    icon: Any, // Can accept ImageVector or Painter
    text: String,
    onClick: () -> Unit,
    isSignOut: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSignOut) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onSecondary.copy(
                alpha = Alpha.DISABLED
            )
        ), // Dark gray background for item
        shape = RoundedCornerShape(8.dp),
        onClick = onClick // Make the whole card clickable
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon is ImageVector) {

                    Icon(
                        imageVector = icon,
                        contentDescription = "$text icon",
                        tint = if (isSignOut) Red.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onPrimary, // Orange for Sign Out icon
                        modifier = Modifier.size(24.dp)
                    )

                } else if (icon is Painter) {

                    Icon(
                        painter = icon,
                        contentDescription = "$text icon",
                        tint = if (isSignOut) Red.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onPrimary, //  Sign Out icon
                        modifier = Modifier.size(24.dp)
                    )

                }

                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = text,
                    color = BlackLight,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            FilledTonalIconButton(
                onClick = onClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                        alpha = Alpha.HALF
                    )
                )
            ) {
                Icon(
                    imageVector = vectorResource(Resources.Icon.ArrowRight), // Right arrow for navigation
                    contentDescription = "Go to $text",
                    tint = if(isSignOut) Red.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Profile image composable with automatic fallback to initials on error.
 * Uses SubcomposeAsyncImage to handle loading, success, and error states gracefully.
 */
@Composable
fun ProfileImageWithFallback(
    imageUrl: String,
    fallbackText: String,
    size: androidx.compose.ui.unit.Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier
) {
    val isValidUrl = imageUrl.isNotBlank() && 
                     (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))
    
    if (isValidUrl) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(imageUrl)
                .crossfade(enable = true)
                .build(),
            contentDescription = com.flipverse.shared.Strings.user_profile_thumbnail,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.primaryContainer,
                    CircleShape
                )
        ) {
            // In Coil 3, painter.state is a StateFlow — must collectAsState() to get the value
            val state by painter.state.collectAsState()
            when (state) {
                is coil3.compose.AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                else -> {
                    // Show initials while loading or on error
                    Box(
                        modifier = Modifier
                            .size(size)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = Alpha.HALF)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = fallbackText,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = fontSize,
                            fontFamily = LexendMediumFont()
                        )
                    }
                }
            }
        }
    } else {
        // Show initials fallback for invalid/empty URL
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = Alpha.HALF)
                )
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fallbackText,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = fontSize,
                fontFamily = LexendMediumFont()
            )
        }
    }
}
