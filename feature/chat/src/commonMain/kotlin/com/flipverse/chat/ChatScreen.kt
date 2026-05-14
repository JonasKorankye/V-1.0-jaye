package com.flipverse.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flipverse.chat.components.Avatar
import com.flipverse.chat.components.ChatBackgroundComplex
import com.flipverse.chat.components.MessageBubble
import com.flipverse.data.util.formatChatTimestamp
import com.flipverse.shared.GreenLighter
import com.flipverse.shared.Red
import com.flipverse.shared.Resources
import com.flipverse.shared.Resources.Icon.Delete
import com.flipverse.shared.Resources.Icon.Mute
import com.flipverse.shared.domain.ChatParticipant
import com.flipverse.shared.Strings
import com.flipverse.shared.util.openEmailApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    otherParticipant: ChatParticipant,
    navigateBack: () -> Unit,
    navigateToViewProfile: () -> Unit,
) {
    val chatViewModel = koinViewModel<ChatViewModel>()
    val chatState by chatViewModel.uiState.collectAsState()

    var messageText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showReportNotice by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // --- Simplest approach: hide spacer when user has text in input ---
    val shouldShowSpacer = remember(messageText) {
        messageText.isEmpty()
    }

    val hideKeyboardController = LocalSoftwareKeyboardController.current


    // Debug the spacer state
    LaunchedEffect(messageText, shouldShowSpacer) {
        println("MessageText: '${messageText}', ShouldShowSpacer: $shouldShowSpacer")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
//            .windowInsetsPadding(WindowInsets.statusBars) // Handle status bar
    ) {
        TopAppBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding() // Ensures proper status bar spacing
                .fillMaxWidth(),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(
                        avatarContent = chatState.otherParticipant?.thumbnail ?: "",
                        name = chatState.otherParticipant?.fullName ?: Strings.unknown_user,
                        onClick = { navigateToViewProfile() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = chatState.otherParticipant?.fullName
                                ?: otherParticipant.fullName.ifEmpty { Strings.unknown_user },
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Online status or typing indicator - in the same spot
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            when {
                                // Priority 1: Show typing if user is typing (same spot as online status)
                                chatState.typingIndicators.any {
                                    it.isTyping && it.userId != chatState.currentUserId
                                } -> {
                                    Text(
                                        text = Strings.chat_typing,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Light
                                    )
                                }

                                // Priority 2: Show online status when not typing
                                else -> {
                                    // Use only chatState.otherParticipant for real-time updates
                                    val isOnline = chatState.otherParticipant?.isOnline
                                    val participantName = chatState.otherParticipant?.fullName
                                        ?: otherParticipant.fullName

                                    println("🔍 Debug - isOnline: $isOnline, participant: $participantName")
                                    println("🔍 Debug - chatState.otherParticipant: ${chatState.otherParticipant}")

                                    when (isOnline) {
                                        true -> {
                                            println("✅ Showing ONLINE status")
                                            // Online indicator dot
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(GreenLighter)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = Strings.chat_online,
                                                color = GreenLighter,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        false -> {
                                            println("❌ Showing OFFLINE status")
                                            // Offline indicator dot
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Gray)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            // --- Real-time updating Last Seen text ---
                                            var lastSeenDisplayText by remember {
                                                mutableStateOf("")
                                            }
                                            val participantLastSeen =
                                                chatState.otherParticipant?.lastSeen
                                                    ?: otherParticipant.lastSeen
                                            // Periodically update the lastSeenDisplayText
                                            LaunchedEffect(participantLastSeen) {
                                                while (true) {
                                                    lastSeenDisplayText =
                                                        if (participantLastSeen > 0) {
                                                            val formattedTime =
                                                                formatChatTimestamp(
                                                                    participantLastSeen
                                                                )
                                                            if (formattedTime == "Now") {
                                                                Strings.last_seen_recently
                                                            } else {
                                                                Strings.last_seen_prefix + formattedTime + Strings.last_seen_suffix
                                                            }
                                                        } else {
                                                            Strings.last_seen_recently
                                                        }
                                                    delay(10_000) // Update every 10 seconds for proper minute-resolution
                                                }
                                            }
                                            Text(
                                                text = lastSeenDisplayText,
                                                color = Color.Gray,
                                                fontSize = 12.sp
                                            )
                                        }

                                        null -> {
                                            println("⏳ Showing LOADING status")
                                            Text(
                                                text = Strings.chat_connecting,
                                                color = Color.Gray,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    chatViewModel.clearCurrentConversation()
                    navigateBack()
                }) {
                    Icon(
                        imageVector = vectorResource(Resources.Icon.BackArrow),
                        contentDescription = Strings.back,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            },
            actions = {
                Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = vectorResource(Resources.Icon.VerticalMenu),
                            contentDescription = Strings.more_options,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.onSecondary)
                    ) {
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = vectorResource(Mute),
                                    contentDescription = Strings.mute,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            text = {
                                Text(
                                    Strings.mute,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            },
                            onClick = {
                                chatViewModel.muteConversation(conversationId)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = vectorResource(Resources.Icon.Pin),
                                    contentDescription = Strings.pin,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            text = { Text(Strings.pin, color = MaterialTheme.colorScheme.onPrimary) },
                            onClick = {
                                chatViewModel.pinConversation(conversationId)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = vectorResource(Resources.Icon.Warning),
                                    contentDescription = "Report conversation",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            text = {
                                Text(
                                    "Report conversation",
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            },
                            onClick = {
                                chatViewModel.reportConversation(
                                    conversationId = conversationId,
                                    reportedUserId = chatState.otherParticipant?.userId
                                ) { didSucceed ->
                                    if (didSucceed) {
                                        showReportNotice = true
                                    }
                                }
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = vectorResource(Delete),
                                    contentDescription = "Block user",
                                    tint = Red
                                )
                            },
                            text = { Text("Block user", color = Red) },
                            onClick = {
                                val targetUserId = chatState.otherParticipant?.userId
                                    ?: ""
                                chatViewModel.blockUser(targetUserId, conversationId)
                                showMenu = false
                                navigateBack()
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = vectorResource(Delete),
                                    contentDescription = Strings.delete,
                                    tint = Red
                                )
                            },
                            text = { Text(Strings.delete, color = Red) },
                            onClick = {
                                chatViewModel.deleteConversation(conversationId)
                                showMenu = false
                                navigateBack()
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
        )

        // Chat messages area - Takes up remaining space, resizes when keyboard appears

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(top = 64.dp, bottom = 56.dp)
        ) {
            if (showReportNotice) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Report recorded",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Blocking removes this conversation from your inbox immediately.",
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(onClick = { openEmailApp("support@flipverse.app") }) {
                                Text("Contact support", color = MaterialTheme.colorScheme.onPrimary)
                            }
                            TextButton(onClick = { showReportNotice = false }) {
                                Text("Dismiss", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }

//            ChatBackgroundComplex(isDarkTheme = isSystemInDarkTheme())
            // Messages list that adjusts to keyboard
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
//                    contentPadding = PaddingValues(
//                        start = 16.dp,
//                        end = 16.dp,
//                        top = 8.dp,
//                        bottom = 100.dp
//                    ),
//                    verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatState.messages) { message ->
                    MessageBubble(
                        message = message,
                        isCurrentUser = message.senderId == chatState.currentUserId,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Message input field - stays at bottom
//            Surface(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .align(Alignment.BottomCenter)
//                    .padding(bottom = 100.dp)
//                    .windowInsetsPadding(WindowInsets.navigationBars) // Above nav bar
//                    .windowInsetsPadding(WindowInsets.ime), // Above keyboard
//                shadowElevation = 8.dp,
//                color = MaterialTheme.colorScheme.primary
//            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .background(Color.Transparent),
                    verticalAlignment = Alignment.Bottom
                ) {
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = {
                            Text(
                                Strings.type_a_flip_placeholder,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.onSecondary,
                                RoundedCornerShape(24.dp)
                            ),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            hideKeyboardController?.hide()
                        }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            disabledContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.onPrimary,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.onPrimary,
                            focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                            unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send button
                    IconButton(
                        onClick = {
                            if (messageText.trim().isNotEmpty()) {
                                chatViewModel.sendMessage(
                                    messageText.trim(),
                                    onSuccess = { messageText = "" }
                                )
                                messageText = ""
                            }
                        },
                        enabled = messageText.trim().isNotEmpty() && !chatState.isSendingMessage,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (messageText.trim().isNotEmpty())
                                    MaterialTheme.colorScheme.primary
                                else
                                    Color.Gray
                            )
                    ) {
                        Icon(
                            imageVector = vectorResource(Resources.Icon.SendMessageTilted),
                            contentDescription = Strings.send,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Fixed BottomAppBar
            BottomAppBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding() // Ensures proper navigation bar spacing
                    .fillMaxWidth()
            ) {
                Text(
                    Strings.bottom_navigation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    color = Color.White
                )
            }


    }

    // Load conversation when screen opens
    LaunchedEffect(conversationId) {
        println("Loading conversation with ID: $conversationId")
        chatViewModel.loadConversation(conversationId)
        chatViewModel.otherParticipantDetails(otherParticipant.userId)
        // Set user as online when entering chat screen
        chatViewModel.setUserOnline()
    }

    // Start listening to online status immediately when screen loads
    LaunchedEffect(otherParticipant.userId) {
        println("🟢 Starting online status listener for EMAIL: ${otherParticipant.userId}")
        println("🟢 Other participant full data: $otherParticipant")
        // Don't start listener here with email - wait for the real user ID
        // chatViewModel.startListeningToUserOnlineStatus(otherParticipant.userId)
    }

    // Listen when chatState updates with new participant info (this will have the real user ID)
    LaunchedEffect(chatState.otherParticipant?.userId) {
        chatState.otherParticipant?.userId?.let { actualUserId ->
            println("🟢 Starting comprehensive monitoring with ACTUAL USER ID: $actualUserId")
            println("🟢 ChatState participant full data: ${chatState.otherParticipant}")
            println("🟢 Email vs Actual ID: Email=${otherParticipant.userId}, ActualID=$actualUserId")
            chatViewModel.startComprehensiveOnlineStatusMonitoring(actualUserId)
        }
    }

    // Set user offline when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            // Set user as offline when leaving chat screen
            chatViewModel.setUserOffline()
        }
    }

    // Update online status when user is actively typing
    LaunchedEffect(messageText) {
        if (messageText.isNotEmpty()) {
            // User is actively typing, ensure they're marked as online
            chatViewModel.setUserOnline()
        }

        // Handle typing indicator
        if (messageText.isNotEmpty() && !chatState.isTyping) {
            chatViewModel.updateTypingStatus(true)
        } else if (messageText.isEmpty() && chatState.isTyping) {
            chatViewModel.updateTypingStatus(false)
        }
    }

    // Monitor online status changes for real-time debugging
    LaunchedEffect(chatState.otherParticipant?.isOnline) {
        println("🔴 Online Status Changed: ${chatState.otherParticipant?.isOnline}")
        println("🔴 Other Participant: ${chatState.otherParticipant?.fullName}")
        println("🔴 Current User ID: ${chatState.currentUserId}")
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(chatState.messages.size) {
        if (chatState.messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(chatState.messages.size - 1)
            }
        }
    }
}
