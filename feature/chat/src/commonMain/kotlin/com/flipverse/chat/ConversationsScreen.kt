package com.flipverse.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flipverse.chat.components.Avatar
import com.flipverse.chat.components.ConversationItem
import com.flipverse.shared.Alpha
import com.flipverse.shared.BlackLight
import com.flipverse.shared.Gray
import com.flipverse.shared.PreferencesRepository
import com.flipverse.shared.PreferencesRepository.getAvatar
import com.flipverse.shared.PreferencesRepository.getFullName
import com.flipverse.shared.PreferencesRepository.getThumbnail
import com.flipverse.shared.PreferencesRepository.saveAvatar
import com.flipverse.shared.PreferencesRepository.saveThumbnail
import com.flipverse.shared.Resources
import com.flipverse.shared.Strings
import com.flipverse.shared.domain.ConversationPreview
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    navigateToNewMessage: () -> Unit,
    navigateToConversation: (String, String) -> Unit, // conversationId, otherUserId
    navigateToUserProfile: (() -> Unit)? = null,
    initialTab: Int = 0, // 0 for Flip tab, 1 for Avatar tab
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableStateOf(initialTab) }
    val pagerState = rememberPagerState(
        initialPage = initialTab,
        pageCount = { 2 }
    )
    val coroutineScope = rememberCoroutineScope()
    val systemBarPaddingValues = WindowInsets.systemBars.asPaddingValues()


    // Synchronize pager with tab selection
    LaunchedEffect(selectedTabIndex) {
        if (pagerState.currentPage != selectedTabIndex) {
            pagerState.animateScrollToPage(selectedTabIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != selectedTabIndex) {
            selectedTabIndex = pagerState.currentPage
        }
    }

    Scaffold(
        modifier = Modifier.padding(6.dp),
        contentWindowInsets = WindowInsets(
            top = systemBarPaddingValues.calculateTopPadding(),
            bottom = systemBarPaddingValues.calculateBottomPadding(),
        ),
        topBar = {
            Column {
//                Modifier.padding(top = 8.dp)
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = Strings.messages_title,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        Box(
                            contentAlignment = Alignment.CenterStart

                        ) {
                            Image(
                                painter = painterResource(if (isSystemInDarkTheme()) Resources.Image.AppLogoDark else Resources.Image.AppLogoWhite),
                                contentDescription = Strings.app_logo,
                                modifier = Modifier
                                    .wrapContentSize()
                                    .height(40.dp),

                                )
                        }

                    },
                    actions = {
                        navigateToUserProfile?.let {
                            Avatar(
                                avatarContent = getThumbnail(),
                                name = getFullName(),
                                onClick = { it() }
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )

                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    indicator = { tabPositions ->
                        if (tabPositions.isNotEmpty()) {
                            SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = {
                            selectedTabIndex = 0
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        },
                        text = {
                            Text(
                                Strings.tab_flip,
                                color = if (selectedTabIndex == 0)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            )
                        }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = {
                            selectedTabIndex = 1
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        },
                        text = {
                            Text(
                                Strings.tab_avatar,
                                color = if (selectedTabIndex == 1)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            )
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            // Only show FAB for Flip tab
            if (selectedTabIndex == 0) {
                FloatingActionButton(
                    onClick = { navigateToNewMessage() },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(
                        bottom = 100.dp,
                        end = 0.dp,
                    ).clip(CircleShape)
                ) {
                    Icon(
                        imageVector = vectorResource(Resources.Icon.Add),
                        contentDescription = Strings.new_message,
                        tint = BlackLight
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.primary
    ) { paddingValues ->
        // Horizontal Pager for swipe functionality
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> FlipTab(
                    modifier = modifier,
                    paddingValues = paddingValues,
                    navigateToConversation = navigateToConversation
                )

                1 -> AvatarTab(
                    modifier = modifier,
                    paddingValues = paddingValues
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlipTab(
    modifier: Modifier,
    paddingValues: PaddingValues,
    navigateToConversation: (String, String) -> Unit
) {
    val chatViewModel = koinViewModel<ChatViewModel>()
    val chatState by chatViewModel.uiState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var searchFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Load conversations when screen opens
    LaunchedEffect(Unit) {
        chatViewModel.loadConversations()
    }

    // Focus the search field and show keyboard when toggled
    LaunchedEffect(searchFocused) {
        if (searchFocused) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // Filter conversations based on search query
    val filteredConversations = remember(chatState.conversations, searchQuery) {
        if (searchQuery.isEmpty()) {
            chatState.conversations
        } else {
            chatState.conversations.filter { conversation ->
                conversation.otherParticipant?.fullName?.contains(
                    searchQuery,
                    ignoreCase = true
                ) == true ||
                        conversation.otherParticipant?.username?.contains(
                            searchQuery,
                            ignoreCase = true
                        ) == true ||
                        conversation.conversation.lastMessage?.content?.contains(
                            searchQuery,
                            ignoreCase = true
                        ) == true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Spacer(modifier= Modifier.height(6.dp))
        // Search bar
        val interactionSource = remember { MutableInteractionSource() }
        BasicTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .height(42.dp)
                .clip(RoundedCornerShape(21.dp))
                .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onPrimary
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.onPrimary),
            singleLine = true,
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                TextFieldDefaults.DecorationBox(
                    value = searchQuery,
                    innerTextField = innerTextField,
                    enabled = true,
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
                    interactionSource = interactionSource,
                    placeholder = {
                        Text(
                            text = Strings.search_flip_conversations_placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = vectorResource(Resources.Icon.Search),
                            contentDescription = Strings.search,
                            tint = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.onPrimary,
                        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        )


        // Conversations list
        when {
            chatState.isLoading -> {
                LoadingContent()
            }

            filteredConversations.isEmpty() && searchQuery.isEmpty() -> {
                EmptyConversationsContent()
            }

            filteredConversations.isEmpty() && searchQuery.isNotEmpty() -> {
                NoSearchResultsContent()
            }

            else -> {
                ConversationsList(
                    conversations = filteredConversations,
                    onConversationClick = { conversation ->
                        val otherParticipant = conversation.otherParticipant
                        if (otherParticipant != null) {
                            navigateToConversation(
                                conversation.conversation.id,
                                otherParticipant.userId,
                            )
                        }
                    }
                )
            }
        }

        // Error handling
        chatState.error?.let { error ->
            ErrorContent(
                message = error,
                onRetry = { chatViewModel.loadConversations() }
            )
        }
    }
}

@Composable
private fun AvatarTab(
    modifier: Modifier,
    paddingValues: PaddingValues
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        AvatarSelectionScreen(
            onBackPressed = { /* No back action needed in tab */ },
            onAvatarSelected = { avatarUrl ->
                // Handle avatar selection - save to user preferences and update thumbnail
                println("Avatar selected in tab: $avatarUrl")

                // Update the thumbnail in preferences so it reflects immediately in UI
                saveAvatar(avatarUrl)

                // The actual saving to Firebase is handled automatically in the ViewModel
                // when selectAvatar is called, so no additional action needed here
            },
            currentAvatarUrl = getAvatar().takeIf { it.isNotEmpty() }
                ?: getAvatar().takeIf { it.isNotEmpty() }
        )
    }
}

@Composable
private fun ConversationsList(
    conversations: List<ConversationPreview>,
    onConversationClick: (ConversationPreview) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        items(conversations) { conversation ->
            ConversationItem(
                conversation = conversation,
                onClick = { onConversationClick(conversation) }
            )

            HorizontalDivider(
                thickness = 0.5.dp,
                color = Color.Gray.copy(alpha = Alpha.DISABLED),
                modifier = Modifier.padding(start = 76.dp, end = 16.dp)
            )
        }

        // Add bottom padding for FAB
        item {
            Spacer(modifier = Modifier.height(88.dp))
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AdaptiveCircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun EmptyConversationsContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = vectorResource(Resources.Icon.SendMessageTilted),
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = Strings.no_flip_conversations_yet,
                color = Color.Gray,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = Strings.start_new_flip_conversation,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun NoSearchResultsContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = vectorResource(Resources.Icon.Search),
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = Strings.no_results_found,
                color = Color.Gray,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = Strings.try_a_different_search_term,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = Strings.error_prefix + message,
                color = Color.Red,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onRetry) {
                Text(
                    text = Strings.retry,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}