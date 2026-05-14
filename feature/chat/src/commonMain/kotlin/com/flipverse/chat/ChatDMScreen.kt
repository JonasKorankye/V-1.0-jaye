package com.flipverse.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontStyle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flipverse.chat.components.Avatar
import com.flipverse.chat.components.AvatarMessageBubble
import com.flipverse.chat.components.ChatBackground
import com.flipverse.chat.components.ChatBackgroundComplex
import com.flipverse.chat.components.MessageBubble
import com.flipverse.chat.components.WaveMessageBubble
import com.flipverse.chat.components.WhatsAppColors
import com.flipverse.chat.components.WhatsAppDarkColors
import com.flipverse.shared.GreenLighter
import com.flipverse.shared.Red
import com.flipverse.shared.Resources
import com.flipverse.shared.Resources.Icon.Delete
import com.flipverse.shared.Resources.Icon.Mute
import com.flipverse.shared.Strings
import com.flipverse.shared.domain.ChatParticipant
import com.flipverse.shared.PreferencesRepository.getAvatar
import com.flipverse.shared.util.openEmailApp
import com.flipverse.shared.util.TTSState
import com.flipverse.data.util.formatChatTimestamp
import com.flipverse.data.util.getCurrentTimeMillis
import com.flipverse.shared.Alpha
import com.flipverse.shared.CoffeeDark
import com.flipverse.shared.White
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDMScreen(
    conversationId: String,
    otherParticipant: ChatParticipant,
    navigateBack: () -> Unit,
    navigateToViewProfile: () -> Unit,
    navigateToAvatarSelection: (() -> Unit)? = null,
) {
    val chatViewModel = koinViewModel<ChatViewModel>()
    val chatState by chatViewModel.uiState.collectAsState()

    val viewModel = viewModel {
        TTSViewModel()
    }
    val currentWordRange by viewModel.currentWordRange.collectAsState()
    val ttsState by viewModel.ttsState.collectAsState()
    val isInitialized by viewModel.isInitialized.collectAsState()

    var messageText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showReportNotice by remember { mutableStateOf(false) }
    var showSendMenu by remember { mutableStateOf(false) }
    var showAvatarPromptDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current


    // Track which message is currently being played by TTS
    var currentPlayingMessageId by remember { mutableStateOf<String?>(null) }

    // --- Simplest approach: hide spacer when user has text in input ---
    val shouldShowSpacer = remember(messageText) {
        messageText.isEmpty()
    }

    val hideKeyboardController = LocalSoftwareKeyboardController.current

    val focusRequester = remember { FocusRequester() }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.release()
        }
    }
    LifecycleResumeEffect(Unit) {
        onPauseOrDispose { viewModel.pause() }
    }

    // Debug the spacer state
    LaunchedEffect(messageText, shouldShowSpacer) {
        println("MessageText: '${messageText}', ShouldShowSpacer: $shouldShowSpacer")
    }

    val imeInsets = WindowInsets.ime
    val imePaddingValues = imeInsets.asPaddingValues()
    val keyboardHeight = with(density) { imePaddingValues.calculateBottomPadding().toPx() }

//    LaunchedEffect(keyboardHeight) {
//        if (keyboardHeight > 0) {
//            // Keyboard is visible, scroll to top with offset to show top bar
//            coroutineScope.launch {
//                listState.animateScrollToItem(
//                    0,
//                    scrollOffset = -800
//                ) // Negative offset to ensure top bar visibility
//            }
//        }
//    }

    // Detect keyboard visibility for expandable topbar
    val isKeyboardVisible = keyboardHeight > 0


    // New callback for handling wave animation clicks (TTS playback)
    val onAvatarClick: (String) -> Unit = { messageId ->
        val message = chatState.messages.find { it.id == messageId }
        message?.let {
            when (ttsState) {
                TTSState.IDLE -> {
                    // Start speaking
                    currentPlayingMessageId = messageId
                    coroutineScope.launch {
                        viewModel.speak(it.content)
                    }
                }

                TTSState.PLAYING -> {
                    if (currentPlayingMessageId == messageId) {
                        // Pause
                        viewModel.pause()
                    } else {
                        // Stop current playback and start new one
                        viewModel.stop()
                        currentPlayingMessageId = messageId
                        coroutineScope.launch {
                            viewModel.speak(it.content)
                        }
                    }
                }

                TTSState.PAUSED -> {
                    if (currentPlayingMessageId == messageId) {
                        // Resume
                        viewModel.resume()
                    } else {
                        // Stop current playback and start new one
                        viewModel.stop()
                        currentPlayingMessageId = messageId
                        coroutineScope.launch {
                            viewModel.speak(it.content)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            TopAppBar(
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
                                            fontStyle = FontStyle.Italic,
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
                                                        delay(3000) // Update every 10 seconds for proper minute-resolution
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
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = vectorResource(Mute),
                                        contentDescription = Strings.mute,
                                        tint = MaterialTheme.colorScheme.onPrimary
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
                                },
                                modifier = Modifier
                                    .padding(4.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = vectorResource(Resources.Icon.Pin),
                                        contentDescription = Strings.pin,
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                },
                                text = {
                                    Text(
                                        Strings.pin,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                },
                                onClick = {
                                    chatViewModel.pinConversation(conversationId)
                                    showMenu = false
                                },
                                modifier = Modifier
                                    .padding(4.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = vectorResource(Resources.Icon.Warning),
                                        contentDescription = "Report conversation",
                                        tint = MaterialTheme.colorScheme.onPrimary
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
                                },
                                modifier = Modifier
                                    .padding(4.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
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
                                },
                                modifier = Modifier
                                    .padding(4.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
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
                                },
                                modifier = Modifier
                                    .padding(4.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.navigationBarsPadding()
            ) {
                Text(
                    Strings.bottom_navigation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.primary
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
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
            Box(
                modifier = Modifier.weight(1f)
            ) {
                ChatBackgroundComplex(
                    isDarkTheme = isSystemInDarkTheme()
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize(),
//                        .paint(
//                            painter = if (isSystemInDarkTheme()) {
//                                painterResource(resource = Resources.Image.ChatBackgroundDark)
//                            } else {
//                                painterResource(resource = Resources.Image.ChatBackground)
//                            },
//                            contentScale = ContentScale.FillBounds
//                        ),
                    reverseLayout = false
                ) {
                    items(chatState.messages) { message ->
                        // Determine message format based on content and attachmentUrl
                        when {
                            // Avatar message: has attachmentUrl (avatar URL) 
                            message.attachmentUrl?.isNotEmpty() == true -> {
                                AvatarMessageBubble(
                                    messageId = message.id,
                                    avatarUrl = message.attachmentUrl ?: "",
                                    messageText = message.content,
                                    isCurrentUser = message.senderId == chatState.currentUserId,
                                    timestamp = message.timestamp,
                                    isPlaying = ttsState == TTSState.PLAYING && currentPlayingMessageId == message.id,
                                    isPaused = ttsState == TTSState.PAUSED && currentPlayingMessageId == message.id,
                                    onAvatarClick = { onAvatarClick(message.id) },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                            // Text message: content starts with "TEXT:"
                            message.content.startsWith("TEXT:") -> {
                                MessageBubble(
                                    message = message.copy(content = message.content.removePrefix("TEXT:")),
                                    isCurrentUser = message.senderId == chatState.currentUserId,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                            // Play message: content starts with "PLAY:"
                            message.content.startsWith("PLAY:") -> {
                                WaveMessageBubble(
                                    message = message.copy(content = message.content.removePrefix("PLAY:")),
                                    onWaveClick = onAvatarClick,
                                    isCurrentUser = message.senderId == chatState.currentUserId,
                                    isPlaying = ttsState == TTSState.PLAYING && currentPlayingMessageId == message.id,
                                    isPaused = ttsState == TTSState.PAUSED && currentPlayingMessageId == message.id,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                            // Default: MessageBubble for legacy messages
                            else -> {
                                MessageBubble(
                                    message = message.copy(content = message.content.removePrefix("TEXT:")),
                                    isCurrentUser = message.senderId == chatState.currentUserId,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(
                        if (isSystemInDarkTheme()) WhatsAppDarkColors.backgroundBase
                        else WhatsAppColors.backgroundBase
                    ),
                color = if (isSystemInDarkTheme()) WhatsAppDarkColors.backgroundBase
                else WhatsAppColors.backgroundBase
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
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
                            )
                            .focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            hideKeyboardController?.hide()
                        }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            disabledContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.onPrimary,
                            focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                            unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                            selectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(
                                handleColor = MaterialTheme.colorScheme.primaryContainer,
                                backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                    alpha = 0.4f
                                )
                            )
                        ),
                        maxLines = 4,
                        minLines = 1
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send button with popup menu
                    Box(modifier = Modifier.wrapContentSize()) {
                        IconButton(
                            onClick = { showSendMenu = true },
                            enabled = messageText.trim()
                                .isNotEmpty() && !chatState.isSendingMessage,
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

                        // Popup menu for send options
                        DropdownMenu(
                            expanded = showSendMenu,
                            onDismissRequest = { showSendMenu = false }
                        ) {
                            DropdownMenuItem(
                                trailingIcon = {
                                    Icon(
                                        imageVector = vectorResource(Resources.Icon.Comment),
                                        contentDescription = Strings.send_as_text,
                                        tint = if (isSystemInDarkTheme()) White.copy(alpha = Alpha.HALF) else CoffeeDark
                                    )
                                },
                                text = {
                                    Text(
                                        Strings.text_label,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                },
                                onClick = {
                                    showSendMenu = false
                                    if (messageText.trim().isNotEmpty()) {
                                        val messageContent = "TEXT:" + messageText.trim()
                                        chatViewModel.sendMessage(
                                            messageContent,
                                            onSuccess = {
                                                // Track this message as a text-only message (for MessageBubble)
                                                val latestMessage = chatState.messages
                                                    .filter { it.senderId == chatState.currentUserId }
                                                    .maxByOrNull { it.timestamp }

                                                latestMessage?.let { message ->
                                                    // Removed tracking of text messages
                                                }
                                            }
                                        )
                                        messageText = ""
                                    }
                                },
                                modifier = Modifier.padding(2.dp)
                            )
                            DropdownMenuItem(
                                trailingIcon = {
                                    Icon(
                                        imageVector = vectorResource(Resources.Icon.Person),
                                        contentDescription = Strings.send_as_avatar,
                                        tint = if (isSystemInDarkTheme()) White.copy(alpha = Alpha.HALF) else CoffeeDark
                                    )
                                },
                                text = {
                                    Text(
                                        Strings.avatar_label,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                },
                                onClick = {
                                    showSendMenu = false
                                    val avatarContent = getAvatar()
                                    if (avatarContent.isNotEmpty()) {
                                        // Check if there's text to be spoken by the avatar
                                        val textToSpeak = messageText.trim()
                                        if (textToSpeak.isNotEmpty()) {
                                            // Send avatar with the text content for speaking
                                            chatViewModel.sendMessage(
                                                textToSpeak,
                                                attachmentUrl = avatarContent,
                                                attachmentType = "IMAGE",
                                                onSuccess = {
                                                    currentPlayingMessageId =
                                                        chatState.messages.last().id
                                                    coroutineScope.launch {
                                                        viewModel.speak(textToSpeak)
                                                    }
                                                }
                                            )
                                        } else {
                                            // No text to speak, just send avatar as AvatarMessageBubble
                                            chatViewModel.sendMessage(
                                                "",
                                                attachmentUrl = avatarContent,
                                                attachmentType = "IMAGE",
                                                onSuccess = {
                                                    currentPlayingMessageId =
                                                        chatState.messages.last().id
                                                    coroutineScope.launch {
                                                        viewModel.speak(Strings.hello_this_is_my_avatar)
                                                    }
                                                }
                                            )
                                        }
                                    } else {
                                        if (navigateToAvatarSelection != null) {
                                            showAvatarPromptDialog = true
                                        } else {
                                            // Navigation not available in this context - send text message as fallback
                                            chatViewModel.sendMessage(
                                                "TEXT:" + messageText.trim(),
                                                onSuccess = {
                                                    // Message sent as text instead
                                                }
                                            )
                                        }
                                    }
                                    messageText = ""
                                },
                                modifier = Modifier.padding(2.dp)
                            )
                        }
                    }
                }
            }
            if (showAvatarPromptDialog) {
                AlertDialog(
                    onDismissRequest = { showAvatarPromptDialog = false },
                    title = {
                        Text(
                            text = Strings.no_avatar_set_title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    text = {
                        Text(
                            text = Strings.no_avatar_set_message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                println("🔘 ChatDMScreen: Set Avatar button clicked")
                                showAvatarPromptDialog = false
                                println("🔘 ChatDMScreen: Dialog dismissed, invoking navigateToAvatarSelection")
                                navigateToAvatarSelection?.invoke()
                                println("🔘 ChatDMScreen: navigateToAvatarSelection invoked")
                            }
                        ) {
                            Text(
                                text = Strings.set_avatar_button,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAvatarPromptDialog = false }) {
                            Text(
                                text = Strings.dismiss,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp)
                )
            }
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

    // Monitor TTS state changes to clear playing message ID when idle
    LaunchedEffect(ttsState) {
        if (ttsState == TTSState.IDLE) {
            currentPlayingMessageId = null
        }
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
