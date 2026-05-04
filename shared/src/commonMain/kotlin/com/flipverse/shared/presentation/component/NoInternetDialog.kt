package com.flipverse.shared.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.flipverse.shared.Resources
import com.flipverse.shared.Strings
import com.flipverse.shared.util.NetworkConnectivity
import com.flipverse.shared.util.hasInternetConnection
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.vectorResource

@Composable
fun NoInternetDialog(
    show: Boolean,
    onDismiss: () -> Unit = {},
    onRetry: () -> Unit
) {
    if (show) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Icon
                    Icon(
                        imageVector = vectorResource(Resources.Icon.Warning),
                        contentDescription = "No Internet",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )

                    // Title
                    Text(
                        text = Strings.no_internet_title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center
                    )

                    // Message
                    Text(
                        text = Strings.no_internet_message,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(Strings.dismiss, color = MaterialTheme.colorScheme.onPrimary)
                        }

                        Button(
                            onClick = onRetry,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(Strings.retry, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoInternetBanner(
    show: Boolean,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    if (show) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = vectorResource(Resources.Icon.Warning),
                        contentDescription = "No Internet",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )

                    Text(
                        text = Strings.no_internet_connection,
                        fontSize = 14.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (onRetry != null) {
                        TextButton(onClick = onRetry) {
                            Text(
                                text = Strings.retry,
                                fontSize = 12.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (onDismiss != null) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = vectorResource(Resources.Icon.Close),
                                contentDescription = "Dismiss",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Auto-monitoring network connectivity banner
 * Automatically checks for internet and displays banner when disconnected
 */
@Composable
fun AutoNetworkBanner(
    modifier: Modifier = Modifier,
    onConnectivityChanged: ((Boolean) -> Unit)? = null,
    onRetry: (() -> Unit)? = null
) {
    var isNetworkAvailable by remember { mutableStateOf(true) }
    var isDismissed by remember { mutableStateOf(false) }
    val connectivity = remember { NetworkConnectivity() }
    val scope = rememberCoroutineScope()

    // Monitor network connectivity
    DisposableEffect(Unit) {
        connectivity.observeConnectivity { connected ->
            isNetworkAvailable = connected
            if (connected) {
                isDismissed = false // Reset dismissal when connection restored
            }
            onConnectivityChanged?.invoke(connected)
        }

        // Initial check
        scope.launch {
            isNetworkAvailable = hasInternetConnection()
            onConnectivityChanged?.invoke(isNetworkAvailable)
        }

        onDispose {
            connectivity.stopObserving()
        }
    }

    NoInternetBanner(
        show = !isNetworkAvailable && !isDismissed,
        modifier = modifier,
        onRetry = {
            scope.launch {
                isNetworkAvailable = hasInternetConnection()
                onConnectivityChanged?.invoke(isNetworkAvailable)
                if (isNetworkAvailable) {
                    isDismissed = false
                }
                onRetry?.invoke()
            }
        },
        onDismiss = {
            isDismissed = true
        }
    )
}