package com.flipverse.userprofile

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.flipverse.shared.Alpha
import com.flipverse.shared.PreferencesRepository.getEmail
import com.flipverse.shared.PreferencesRepository.getFullName
import com.flipverse.shared.PreferencesRepository.getUsername
import com.flipverse.shared.PreferencesRepository.reset
import com.flipverse.shared.Red
import com.flipverse.shared.Resources
import com.flipverse.shared.WorkSansBoldFont
import com.flipverse.shared.navigation.Screen
import com.flipverse.data.util.normalizeUsername
import com.flipverse.shared.Green
import com.flipverse.shared.PreferencesRepository.getFormattedCreatedAt
import com.flipverse.shared.location.LocationDetector
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountInformationScreen(
    onNavigateBack: () -> Unit,
    navigateToAuthController: NavController
) {
    val viewModel = koinViewModel<UserProfileViewModel>()
    var detectedCountry by remember { mutableStateOf("Detecting...") }
    var detectionMethod by remember { mutableStateOf("") }
    var isDetectingLocation by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    // Automatically detect user location when screen loads
//    LaunchedEffect(Unit) {
//        try {
//            println("AccountInfo: Starting location detection...")
//            val locationInfo = LocationDetector.getCachedOrDetectLocation()
//            println("AccountInfo: Detected country = ${locationInfo.countryName}, code = ${locationInfo.countryCode}")
//
//            detectedCountry =
//                if (locationInfo.countryName.isNotEmpty() && locationInfo.countryName != "Unknown") {
//                    locationInfo.countryName
//                } else {
//                    "Not detected"
//                }
//
//            // Determine detection method for debugging
//            detectionMethod = when {
//                locationInfo.city.isNotEmpty() -> "IP-based"
//                locationInfo.countryCode.isNotEmpty() -> "Locale-based"
//                else -> "Default"
//            }
//
//            println("AccountInfo: Final display = $detectedCountry (via $detectionMethod)")
//        } catch (e: Exception) {
//            println("AccountInfo: Detection failed with error: ${e.message}")
//            detectedCountry = "Detection failed"
//            detectionMethod = "Error"
//        } finally {
//            isDetectingLocation = false
//        }
//    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Account information",
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

            // Account Information Section
            SectionHeader(text = "Basic information")
            Spacer(modifier = Modifier.height(12.dp))

            // Username
            AccountInfoItem(
                icon = painterResource(Resources.Icon.Person),
                label = "Username",
                value = normalizeUsername(getUsername())
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Email
            AccountInfoItem(
                icon = painterResource(Resources.Icon.Email),
                label = "Email",
                value = getEmail()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Country with refresh capability
//            Card(
//                modifier = Modifier.fillMaxWidth(),
//                colors = CardDefaults.cardColors(
//                    containerColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = Alpha.DISABLED)
//                ),
//                shape = RoundedCornerShape(12.dp)
//            ) {
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(16.dp),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Icon(
//                        painter = painterResource(Resources.Icon.MapPin),
//                        contentDescription = "Country icon",
//                        tint = MaterialTheme.colorScheme.onPrimary,
//                        modifier = Modifier.size(24.dp)
//                    )
//
//                    Spacer(modifier = Modifier.width(16.dp))
//
//                    Column(modifier = Modifier.weight(1f)) {
//                        Text(
//                            text = "Country",
//                            color = MaterialTheme.colorScheme.onSecondary,
//                            fontSize = 14.sp,
//                            fontWeight = FontWeight.Medium
//                        )
//                        Spacer(modifier = Modifier.height(4.dp))
//                        if (isDetectingLocation) {
//                            Row(verticalAlignment = Alignment.CenterVertically) {
//                                CircularProgressIndicator(
//                                    modifier = Modifier.size(16.dp),
//                                    color = MaterialTheme.colorScheme.onPrimary,
//                                    strokeWidth = 2.dp
//                                )
//                                Spacer(modifier = Modifier.width(8.dp))
//                                Text(
//                                    text = "Detecting location...",
//                                    color = MaterialTheme.colorScheme.onPrimary,
//                                    fontSize = 16.sp,
//                                    fontWeight = FontWeight.Normal
//                                )
//                            }
//                        } else {
//                            Column {
//                                Text(
//                                    text = detectedCountry,
//                                    color = MaterialTheme.colorScheme.onPrimary,
//                                    fontSize = 16.sp,
//                                    fontWeight = FontWeight.Normal
//                                )
//                                // Show detection method for debugging (optional - remove in production)
//                                if (detectionMethod.isNotEmpty()) {
//                                    Text(
//                                        text = "via $detectionMethod",
//                                        color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f),
//                                        fontSize = 12.sp,
//                                        fontWeight = FontWeight.Normal
//                                    )
//                                }
//                            }
//                        }
//                    }
//
//                    // Refresh button for location detection
//                    if (!isDetectingLocation) {
//                        IconButton(
//                            onClick = {
//                                isDetectingLocation = true
//                                detectionMethod = ""
//                                // Clear cache and retry detection
//                                coroutineScope.launch {
//                                    try {
//                                        println("AccountInfo: Manually refreshing location...")
//                                        LocationDetector.clearLocationCache()
//                                        val locationInfo =
//                                            LocationDetector.getCachedOrDetectLocation()
//                                        println("AccountInfo: Refreshed - country = ${locationInfo.countryName}, code = ${locationInfo.countryCode}")
//
//                                        detectedCountry =
//                                            if (locationInfo.countryName.isNotEmpty() && locationInfo.countryName != "Unknown") {
//                                                locationInfo.countryName
//                                            } else {
//                                                "Not detected"
//                                            }
//
//                                        detectionMethod = when {
//                                            locationInfo.city.isNotEmpty() -> "IP-based"
//                                            locationInfo.countryCode.isNotEmpty() -> "Locale-based"
//                                            else -> "Default"
//                                        }
//
//                                        println("AccountInfo: Refresh complete = $detectedCountry (via $detectionMethod)")
//                                    } catch (e: Exception) {
//                                        println("AccountInfo: Refresh failed with error: ${e.message}")
//                                        detectedCountry = "Detection failed"
//                                        detectionMethod = "Error"
//                                    } finally {
//                                        isDetectingLocation = false
//                                    }
//                                }
//                            }
//                        ) {
//                            Icon(
//                                painter = painterResource(Resources.Icon.Refresh),
//                                contentDescription = "Refresh location",
//                                tint = MaterialTheme.colorScheme.onPrimary,
//                                modifier = Modifier.size(20.dp)
//                            )
//                        }
//                    }
//                }
//            }

            Spacer(modifier = Modifier.height(32.dp))

            // Account Status Section
            SectionHeader(text = "Account status")
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(Resources.Icon.Checkmark),
                            contentDescription = "Verified",
                            tint = Green,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Account Active",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your account is active and in good standing.",
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Member Since Section
            SectionHeader(text = "Membership")
            Spacer(modifier = Modifier.height(12.dp))

            AccountInfoItem(
                icon = painterResource(Resources.Icon.Account),
                label = "Member since",
                value = getFormattedCreatedAt()
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Sign Out Button
            Button(
                onClick = {
                    viewModel.signOut(
                        onSuccess = navigateToAuthController.navigate(Screen.Auth) {
                            popUpTo(navigateToAuthController.graph.startDestinationId) {
                                inclusive = true
                            }
                            launchSingleTop = true
                            reset() // Clear all data
                        },
                        onError = { /* Handle error if needed */ }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Red
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(Resources.Icon.SignOut),
                        contentDescription = "Sign out",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sign Out",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Help Text
            Text(
                text = "Signing out will log you out of FlipVerse on this device. You can always sign back in with your credentials.",
                color = MaterialTheme.colorScheme.onSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
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
private fun AccountInfoItem(
    icon: Any,
    label: String,
    value: String,
    isLoading: Boolean = false
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (icon) {
                is ImageVector -> {
                    Icon(
                        imageVector = icon,
                        contentDescription = "$label icon",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                is Painter -> {
                    Icon(
                        painter = icon,
                        contentDescription = "$label icon",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (isLoading) {
                    AdaptiveCircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = value,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}