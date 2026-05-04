package com.flipverse.notification

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.flipverse.data.util.formatTimestamp
import com.flipverse.shared.Alpha
import com.flipverse.shared.Ash
import com.flipverse.shared.BlackLight
import com.flipverse.shared.CoffeeDark
import com.flipverse.shared.PreferencesRepository.getEmail
import com.flipverse.shared.PreferencesRepository.getFullName
import com.flipverse.shared.PreferencesRepository.getThumbnail
import com.flipverse.shared.Red
import com.flipverse.shared.Resources
import com.flipverse.shared.SeaBlueLight
import com.flipverse.shared.Strings
import com.flipverse.shared.White
import com.flipverse.shared.WorkSansBoldFont
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlipNotifyScreen(
    navigateToProfile: (String) -> Unit,
    navigateToPost: (String) -> Unit = {},
    navigateToLiveBook: (String) -> Unit = {},
    onDoubleTapChanged: ((suspend () -> Unit) -> Unit)? = null
) {
    val viewModel = koinViewModel<NotificationViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showBottomSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()
    val systemBarPaddingValues = WindowInsets.systemBars.asPaddingValues()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Refresh notifications when screen becomes visible
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.onResume()
    }

    // Register double-tap callback
    androidx.compose.runtime.LaunchedEffect(Unit) {
        onDoubleTapChanged?.invoke {
            coroutineScope.launch {
                // Scroll to top with animation
                listState.animateScrollToItem(0)
                // Refresh notifications
                viewModel.onResume()
                println(" Double-tap detected on Notifications button - scrolling to top and refreshing")
            }
        }
    }

    Scaffold(
        modifier = Modifier.padding(6.dp),
        contentWindowInsets = WindowInsets(
            top = systemBarPaddingValues.calculateTopPadding(),
            bottom = systemBarPaddingValues.calculateBottomPadding(),
        ),
        topBar = {
            CenterAlignedTopAppBar(
//                modifier = Modifier.padding(top = 8.dp),
                title = {
                    Column {
                        Text(
                            text = Strings.notifications_title,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
//                        if (uiState.unreadCount > 0) {
//                            Text(
//                                text = "${uiState.unreadCount} unread",
//                                color = Ash,
//                                fontSize = 12.sp,
//                                textAlign = TextAlign.Justify
//                            )
//                        }
                    }
                },
                navigationIcon = {
                    Box(
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Image(
                            painter = painterResource(
                                if (isSystemInDarkTheme()) Resources.Image.AppLogoDark
                                else Resources.Image.AppLogoWhite
                            ),
                            contentDescription = Strings.app_logo,
                            modifier = Modifier
                                .wrapContentSize()
                                .height(40.dp),
                        )
                    }
                },
                actions = {
                    // Menu button and profile picture with overlay badge
                    IconButton(
                        onClick = { showBottomSheet = true }
                    ) {
                        Icon(
                            imageVector = vectorResource(Resources.Icon.HorizontalMenu),
                            contentDescription = Strings.more_options,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Profile image or initial
                        if (getThumbnail().isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(CircleShape)
                                    .clickable { navigateToProfile(getEmail()) }
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = Alpha.HALF)
                                    )
                                    .border(0.dp, Color.Transparent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = getFullName().take(1).uppercase(),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 16.sp,
                                    fontFamily = WorkSansBoldFont()
                                )
                            }
                        } else {
                            AsyncImage(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(CircleShape)
                                    .clickable { navigateToProfile(getEmail()) }
                                    .border(
                                        0.dp,
                                        Color.Transparent,
                                        CircleShape
                                    ),
                                model = ImageRequest.Builder(LocalPlatformContext.current)
                                    .data(getThumbnail())
                                    .crossfade(enable = true)
                                    .build(),
                                contentDescription = Strings.user_profile_thumbnail,
                                contentScale = ContentScale.Crop
                            )
                        }
                        // Notification badge overlays at top right
                        if (uiState.unreadCount > 0) {
                            Badge(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (6).dp, y = (-3).dp),
                                containerColor = Red,
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = uiState.unreadCount.toString(),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.primary
    ) { paddingValues ->
        when {
            uiState.isLoading && uiState.notifications.isEmpty() -> {
                LoadingState()
            }

            uiState.notifications.isEmpty() -> {
                EmptyNotificationState()
            }

            else -> {
                NotificationsList(
                    notifications = uiState.notifications,
                    onNotificationClick = { notification ->
                        viewModel.markAsRead(notification.id)
                        when (notification.type) {
                            NotificationType.LIKE_POST,
                            NotificationType.COMMENT_POST,
                            NotificationType.TAG_POST -> {
                                notification.targetPost?.let { post ->
                                    navigateToPost(post.id)
                                }
                            }

                            NotificationType.LIVEBOOK_INVITATION,
                            NotificationType.LIVEBOOK_CONTRIBUTION,
                            NotificationType.TAG_LIVEBOOK -> {
                                notification.targetLiveBook?.let { liveBook ->
                                    navigateToLiveBook(liveBook.id)
                                }
                            }

                            else -> {
                                // Handle other notification types
                            }
                        }
                    },
                    modifier = Modifier.padding(paddingValues),
                    listState = listState
                )
            }
        }
    }

    // Bottom Sheet
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = bottomSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = MaterialTheme.colorScheme.outline
                )
            }
        ) {
            NotificationBottomSheetContent(
                unreadCount = uiState.unreadCount,
                onMarkAllAsRead = {
                    viewModel.markAllAsRead()
                    showBottomSheet = false
                },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun NotificationBottomSheetContent(
    unreadCount: Int,
    onMarkAllAsRead: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
//        Text(
//            text = "Notifications",
//            fontSize = 20.sp,
//            fontWeight = FontWeight.Bold,
//            color = MaterialTheme.colorScheme.onSurface
//        )

        if (unreadCount > 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMarkAllAsRead() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = Strings.mark_all_as_read,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = unreadCount.toString() + Strings.unread_notifications_suffix,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Badge(
                        containerColor = Red,
                    ) {
                        Text(
                            text = unreadCount.toString(),
                            color = White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = Strings.all_caught_up,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = Strings.no_unread_notifications,
                            fontSize = 14.sp,
                            color = Ash
                        )
                    }

                    Icon(
                        imageVector = vectorResource(Resources.Icon.Check),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = Strings.loading_notifications,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun EmptyNotificationState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = vectorResource(Resources.Icon.Notification),
            contentDescription = Strings.no_notifications,
            tint = Color.Gray,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = Strings.no_activity_yet,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = Strings.likes_comments_replies_here,
            color = MaterialTheme.colorScheme.onSecondary,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun NotificationsList(
    notifications: List<NotificationItem>,
    onNotificationClick: (NotificationItem) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
    ) {
        items(notifications) { notification ->
            NotificationItemCard(
                notification = notification,
                onClick = { onNotificationClick(notification) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun NotificationItemCard(
    notification: NotificationItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead)
                MaterialTheme.colorScheme.surface
            else
                SeaBlueLight
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (notification.isRead) 0.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            NotificationAvatar(
                avatarUrl = notification.avatarUrl,
                notificationType = notification.type
            )

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = notification.message,
                    fontSize = 15.sp,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Medium,
                    color = if (notification.isRead && isSystemInDarkTheme()) MaterialTheme.colorScheme.onPrimary else BlackLight,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = formatTimestamp(notification.timestamp),
                    fontSize = 13.sp,
                    color = if (!notification.isRead && isSystemInDarkTheme()) CoffeeDark else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Unread indicator
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun NotificationAvatar(
    avatarUrl: String,
    notificationType: NotificationType
) {
    Box {
        if (avatarUrl.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Default avatar based on notification type
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(getNotificationColor(notificationType)),
                contentAlignment = Alignment.Center
            ) {
                if (notificationType == NotificationType.LIVEBOOK_INVITATION ||
                    notificationType == NotificationType.LIVEBOOK_CONTRIBUTION
                ) {
                    Icon(
                        painter = painterResource(getNotificationIcon(notificationType)),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = vectorResource(getNotificationIcon(notificationType)),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Notification type badge
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(getNotificationColor(notificationType))
                .align(Alignment.BottomEnd),
            contentAlignment = Alignment.Center
        ) {
            if (notificationType == NotificationType.LIVEBOOK_INVITATION ||
                notificationType == NotificationType.LIVEBOOK_CONTRIBUTION
            ) {
                Icon(
                    painter = painterResource(getNotificationIcon(notificationType)),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = vectorResource(getNotificationIcon(notificationType)),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

private fun getNotificationColor(type: NotificationType): Color {
    return when (type) {
        NotificationType.LIKE_POST, NotificationType.LIKE_COMMENT -> Color(0xFFE91E63)
        NotificationType.COMMENT_POST, NotificationType.REPLY_COMMENT -> Color(0xFF2196F3)
        NotificationType.TAG_POST, NotificationType.TAG_LIVEBOOK -> Color(0xFF4CAF50)
        NotificationType.LIVEBOOK_INVITATION -> Color(0xFF9C27B0)
        NotificationType.LIVEBOOK_CONTRIBUTION -> Color(0xFFFF9800)
        NotificationType.FOLLOW -> Color(0xFF00BCD4)
        NotificationType.POST_SHARED -> Color(0xFF607D8B)
    }
}

private fun getNotificationIcon(type: NotificationType): org.jetbrains.compose.resources.DrawableResource {
    return when (type) {
        NotificationType.LIKE_POST, NotificationType.LIKE_COMMENT -> Resources.Icon.Like
        NotificationType.COMMENT_POST, NotificationType.REPLY_COMMENT -> Resources.Icon.Comment
        NotificationType.TAG_POST, NotificationType.TAG_LIVEBOOK -> Resources.Icon.Person
        NotificationType.LIVEBOOK_INVITATION, NotificationType.LIVEBOOK_CONTRIBUTION -> Resources.Icon.FlipLiveBook
        NotificationType.FOLLOW -> Resources.Icon.Person
        NotificationType.POST_SHARED -> Resources.Icon.Share
    }
}

