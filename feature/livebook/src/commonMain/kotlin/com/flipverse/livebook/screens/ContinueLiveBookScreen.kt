package com.flipverse.livebook.screens

import ContentWithMessageBar
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.flipverse.data.util.calculateLiveBookProgress
import com.flipverse.data.util.getCurrentTimeMillis
import com.flipverse.data.util.vetStoryContinuation
import com.flipverse.data.util.formatElapsedTime
import com.flipverse.data.util.calculateTurnTimeRemaining
import com.flipverse.data.util.getCurrentTurnHolderName
import com.flipverse.livebook.LiveBookEvent
import com.flipverse.livebook.LiveBookViewModel
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.runtime.derivedStateOf
import com.flipverse.shared.Alpha
import com.flipverse.shared.BlackLight
import com.flipverse.shared.LexendMediumFont
import com.flipverse.shared.Resources
import com.flipverse.shared.Strings
import com.flipverse.shared.domain.LiveBook
import com.flipverse.shared.presentation.component.FlipButton
import com.flipverse.shared.presentation.component.FlipExpandableText
import com.flipverse.shared.presentation.ui.ObserveAsEvents
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import MessageBarState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import rememberMessageBarState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContinueLiveBookScreen(
    liveBookId: String? = null,
    onBackClick: () -> Unit,
    onNavigateToOpenLiveBook: () -> Unit
) {
    val viewModel = koinViewModel<LiveBookViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messageBarState = rememberMessageBarState()
    val scope = rememberCoroutineScope()

    var storyContinuation by remember { mutableStateOf("") }
    val maxChars = 500

    LaunchedEffect(liveBookId) {
        if (liveBookId != null) {
            // Fetch specific LiveBook by ID
            viewModel.fetchLiveBookById(liveBookId)
        } else {
            // Fallback to checking user participation for current writing turn
            viewModel.checkUserParticipation()
        }
    }

    ObserveAsEvents(viewModel.event) { event ->

        when (event) {
            is LiveBookEvent.Navigate.OpenLiveBook -> {}
            is LiveBookEvent.Error -> {
                messageBarState.addError(message = event.error)
            }

            is LiveBookEvent.Success -> {
                scope.launch {
                    messageBarState.addSuccess("Contribution submitted successfully!")
                    delay(1200)
                    onNavigateToOpenLiveBook()
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        Strings.continue_story_challenge_title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBackClick() }) {
                        Icon(
                            imageVector = vectorResource(Resources.Icon.BackArrow),
                            contentDescription = Strings.back,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        ContentWithMessageBar(
            contentBackgroundColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding()
                ),
            messageBarState = messageBarState,
            errorMaxLines = 2,
            fontFamily = FontFamily.SansSerif,
            errorContainerColor = MaterialTheme.colorScheme.error,
            errorContentColor = MaterialTheme.colorScheme.onErrorContainer,
            successContainerColor = MaterialTheme.colorScheme.primaryContainer,
            successContentColor = BlackLight,
            showCopyButton = false,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
//                    .padding(paddingValues)
            ) {
                when {
                    uiState.isLoadingParticipation || (liveBookId != null && uiState.isLoadingCurrentBook) -> {
                        LoadingState()
                    }

                    liveBookId != null && uiState.currentLiveBook != null -> {
                        // Show specific LiveBook when liveBookId is provided
                        StoryContent(
                            liveBook = uiState.currentLiveBook!!,
                            storyContinuation = storyContinuation,
                            onStoryChange = { storyContinuation = it },
                            maxChars = maxChars,
                            messageBarState = messageBarState,
                            onBackClick = onBackClick
                        )
                    }

                    liveBookId == null && uiState.userParticipation?.currentWritingTurn == null -> {
                        NoWritingTurnState()
                    }

                    liveBookId == null && uiState.userParticipation?.currentWritingTurn != null -> {
                        // Show current writing turn when no specific liveBookId is provided
                        StoryContent(
                            liveBook = uiState.userParticipation!!.currentWritingTurn!!,
                            storyContinuation = storyContinuation,
                            onStoryChange = { storyContinuation = it },
                            maxChars = maxChars,
                            messageBarState = messageBarState,
                            onBackClick = onBackClick
                        )
                    }

                    else -> {
                        NoWritingTurnState()
                    }
                }
            }
        }
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
            AdaptiveCircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = Strings.loading_writing_turns,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun NoWritingTurnState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                painter = painterResource(Resources.Icon.FlipLiveBook),
                contentDescription = Strings.no_writing_turns_cd,
                tint = Color.Gray,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = Strings.no_active_writing_turns,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = Strings.no_stories_waiting,
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StoryContent(
    liveBook: LiveBook,
    storyContinuation: String,
    onStoryChange: (String) -> Unit,
    maxChars: Int,
    messageBarState: MessageBarState,
    onBackClick: () -> Unit
) {
    val viewModel = koinViewModel<LiveBookViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // --- Writing Timer State ---
    var writingStartTime by remember { mutableStateOf<Long?>(null) }
    var elapsedSeconds by remember { mutableStateOf(0L) }
    var timerRunning by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // Clipboard manager for paste detection
    val clipboardManager = LocalClipboardManager.current
    var previousText by remember { mutableStateOf("") }

    LaunchedEffect(timerRunning) {
        if (timerRunning) {
            writingStartTime = writingStartTime ?: getCurrentTimeMillis()
            while (timerRunning) {
                val now = getCurrentTimeMillis()
                elapsedSeconds = ((now - (writingStartTime ?: now)) / 1000L)
                delay(1000)
            }
        }
    }

    // Favorite state for this book (synced with backend)
    var isFavorite by remember(liveBook.id) { mutableStateOf(false) }

    // Load favorite status when the LiveBook changes
    LaunchedEffect(liveBook.id) {
        viewModel.loadLiveBookFavoriteStatus(liveBook.id) { favoriteStatus ->
            isFavorite = favoriteStatus
        }
    }

    // Check if current user has already contributed to this story
    val currentUserEmail = remember { com.flipverse.shared.PreferencesRepository.getId() }
    val hasUserContributed = remember(liveBook, currentUserEmail) {
        // Check if user is the author or has contributed to any paragraph
        liveBook.authorId == currentUserEmail ||
                listOf(
                    liveBook.paragraph1ContributorId,
                    liveBook.paragraph2ContributorId,
                    liveBook.paragraph3ContributorId,
                    liveBook.paragraph4ContributorId,
                    liveBook.paragraph5ContributorId,
                    liveBook.paragraph6ContributorId
                ).any { it == currentUserEmail }
    }

    // Get other available LiveBooks (active ones where user hasn't contributed yet)
    val otherAvailableBooks = remember(uiState.activeLiveBooks, liveBook.id) {
        uiState.activeLiveBooks.filter { book ->
            book.id != liveBook.id &&
                    !listOf(
                        book.paragraph1ContributorId,
                        book.paragraph2ContributorId,
                        book.paragraph3ContributorId,
                        book.paragraph4ContributorId,
                        book.paragraph5ContributorId,
                        book.paragraph6ContributorId
                    ).contains(currentUserEmail) &&
                    book.authorId != currentUserEmail
        }
    }

    // Check if the current turn has expired
    val isTurnExpired by remember(liveBook.currentTurnStartTime, liveBook.turnDurationHours) {
        derivedStateOf {
            if (liveBook.currentTurnStartTime.isNotEmpty()) {
                calculateTurnTimeRemaining(
                    turnStartTime = liveBook.currentTurnStartTime,
                    turnDurationHours = liveBook.turnDurationHours
                ) == "Expired"
            } else false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Story Title Row (with Favorite Button)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = liveBook.title.uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f)
                )

                // Revert button - show only if user can revert to previous book
                if (uiState.canRevertToPrevious && uiState.previousLiveBook != null) {
                    IconButton(
                        onClick = {
                            viewModel.revertToPreviousLiveBook()
                        }
                    ) {
                        Icon(
                            imageVector = vectorResource(Resources.Icon.BackArrow),
                            contentDescription = "Revert to ${uiState.previousLiveBook?.title}",
                            tint = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                IconButton(
                    onClick = {
                        isFavorite = !isFavorite
                        // Sync favorite state to backend
                        viewModel.updateLiveBookFavoriteStatus(liveBook.id, isFavorite)
                    }
                ) {
                    Icon(
                        painter = painterResource(if (isFavorite) Resources.Icon.LikeSelected else Resources.Icon.Like),
                        contentDescription = if (isFavorite) "Book Liked" else "Add to likes",
                        tint = if (isFavorite) Color.Red else Color.Gray
                    )
                }
            }

            // Show revert info if available
            if (uiState.canRevertToPrevious && uiState.previousLiveBook != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = Strings.revert_tap_prefix,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Icon(
                        imageVector = vectorResource(Resources.Icon.BackArrow),
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = Strings.revert_to_return_prefix + (uiState.previousLiveBook?.title ?: "") + Strings.revert_to_return_suffix,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = Strings.genre_prefix + liveBook.genre,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = Alpha.DISABLED)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Story Progress
            val progress = calculateLiveBookProgress(liveBook)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = Strings.story_progress_prefix + "$progress" + Strings.story_progress_suffix,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // --- Turn Info: Who's turn and time remaining ---
                    Spacer(modifier = Modifier.height(8.dp))

                    if (liveBook.contributorTurnOrder.isNotEmpty() && liveBook.currentTurnStartTime.isNotEmpty()) {
                        val turnHolderName = getCurrentTurnHolderName(liveBook)
                        val turnTimeRemaining = calculateTurnTimeRemaining(
                            turnStartTime = liveBook.currentTurnStartTime,
                            turnDurationHours = liveBook.turnDurationHours
                        )

                        if (turnHolderName != null && turnTimeRemaining != "Expired") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(Resources.Icon.FlipNotify),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = Strings.waiting_for + turnHolderName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = Strings.turn_expires_in + turnTimeRemaining,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        } else if (turnTimeRemaining == "Expired") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(Resources.Icon.FlipNotify),
                                    contentDescription = null,
                                    tint = Color.Red,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = Strings.turn_expired,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Red,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    // Display the initial paragraph
                    FlipExpandableText(
                        text = liveBook.initialParagraph,
                        minLines = 4
                    )

                    // Display existing paragraphs
                    if (liveBook.paragraph1.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FlipExpandableText(
                            text = liveBook.paragraph1,
                            minLines = 4
                        )
                    }

                    // Add more paragraphs as needed
                    if (liveBook.paragraph2.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FlipExpandableText(
                            text = liveBook.paragraph2,
                            minLines = 4
                        )
                    }

                    // Continue for paragraphs 3-6 if they exist
                    listOf(
                        liveBook.paragraph3,
                        liveBook.paragraph4,
                        liveBook.paragraph5,
                        liveBook.paragraph6
                    ).forEach { paragraph ->
                        if (paragraph.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FlipExpandableText(
                                text = paragraph,
                                minLines = 4
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Writing Timer UI ---
            if (!hasUserContributed && !isTurnExpired) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(Resources.Icon.FlipNotify),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = Strings.writing_time_prefix + formatElapsedTime(elapsedSeconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Participants Section
            Text(
                text = Strings.participants,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Show author avatar
                item {
                    ParticipantAvatar(
                        imageUrl = liveBook.authorThumbnail,
                        name = liveBook.authorName,
                        isAuthor = true
                    )
                }

                // Show tagged users
                items(liveBook.taggedUsers) { taggedUser ->
                    ParticipantAvatar(
                        imageUrl = taggedUser.thumbnail,
                        name = taggedUser.fullname,
                        isAuthor = false
                    )
                }
            }

            if (hasUserContributed) {
                // User has already contributed
                Text(
                    text = Strings.already_contributed,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Show other available LiveBooks
                if (otherAvailableBooks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = Strings.other_stories_you_can_contribute_to,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    otherAvailableBooks.forEach { availableBook ->
                        OtherLiveBookItem(
                            liveBook = availableBook,
                            onClick = {
                                // Switch to this story and refresh the current view
                                viewModel.switchToLiveBook(availableBook)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(Resources.Icon.FlipLiveBook),
                                    contentDescription = Strings.no_more_stories,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = Strings.no_more_stories_available,
                                    color = Color.Gray,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = Strings.contributed_to_all_active_stories,
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                // User hasn't contributed yet
                if (isTurnExpired) {
                    // Turn has expired - show expired message, no input field
                    Text(
                        text = Strings.your_turn_has_expired,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Red,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                } else {
                    // Turn is still active - show input field
                    val keyboardController = LocalSoftwareKeyboardController.current

                    Text(
                        text = Strings.your_turn_to_continue_story,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Local text state to keep iOS native text field in sync.
                    // Using return@TextField on iOS causes UITextField/Compose desync,
                    // making the keyboard unresponsive after errors.
                    var localContinuationText by remember { mutableStateOf(storyContinuation) }

                    // Sync from parent when it changes externally
                    LaunchedEffect(storyContinuation) {
                        if (localContinuationText != storyContinuation) {
                            localContinuationText = storyContinuation
                        }
                    }

                    // Story continuation input
                    TextField(
                        value = localContinuationText,
                        onValueChange = { input ->
                            // Check for clipboard paste by comparing with actual clipboard content
                            val clipboardText = clipboardManager.getText().toString().trim()
                            val textDiff = input.length - previousText.length

                            // Detect clipboard paste: large text insertion that matches clipboard content
                            val isClipboardPaste = clipboardText.isNotEmpty() &&
                                    clipboardText.length > 10 && // Clipboard has substantial content
                                    textDiff > 10 && // Large text insertion (more than typical typing)
                                    input.contains(
                                        clipboardText,
                                        ignoreCase = true
                                    ) // New text contains clipboard content

                            // Detect unnecessary whitespace
                            val hasMultipleSpaces = input.contains(Regex("\\s{2,}")) // Two or more consecutive spaces
                            val hasLeadingSpaces = input.startsWith(" ") && previousText.isEmpty()
                            val hasExcessiveWhitespace = input.count { it.isWhitespace() } > input.length * 0.3 && input.length > 20

                            when {
                                isClipboardPaste -> {
                                    messageBarState.addError(
                                        message = "Pasting from clipboard is not allowed. Please type your continuation manually to ensure authentic writing."
                                    )
                                    // Revert local text to keep iOS keyboard in sync
                                    localContinuationText = previousText
                                }
                                hasMultipleSpaces -> {
                                    messageBarState.addError(
                                        message = "Multiple consecutive spaces detected. Please use single spaces between words."
                                    )
                                    localContinuationText = previousText
                                }
                                hasLeadingSpaces -> {
                                    messageBarState.addError(
                                        message = "Please don't start with spaces. Begin typing your continuation directly."
                                    )
                                    localContinuationText = previousText
                                }
                                hasExcessiveWhitespace -> {
                                    messageBarState.addError(
                                        message = "Excessive whitespace detected. Please write naturally without unnecessary spaces."
                                    )
                                    localContinuationText = previousText
                                }
                                input.length <= maxChars -> {
                                    localContinuationText = input
                                    onStoryChange(input)
                                    previousText = input
                                }
                            }

                            // Start timer on first character typed
                            if (timerRunning.not() && input.isNotEmpty()) {
                                timerRunning = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = Alpha.DISABLED)
                                )
                            )
                            .height(180.dp),
                        placeholder = {
                            Text(
                                text = Strings.continue_story_placeholder,
                                color = Color.Gray
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.primary,
                            unfocusedContainerColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                            unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                            cursorColor = MaterialTheme.colorScheme.onPrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${maxChars - storyContinuation.length}" + Strings.characters_remaining_suffix,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Submit/Dismiss Button - only show if user hasn't contributed
        if (!hasUserContributed) {
            if (isTurnExpired) {
                FlipButton(
                    text = Strings.dismiss,
                    enabled = true,
                    onClick = { onBackClick() },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            } else {
                FlipButton(
                    text = if (uiState.isSubmittingContribution) Strings.submitting else Strings.submit_contribution,
                    enabled = storyContinuation.isNotEmpty() && !uiState.isSubmittingContribution,
                    onClick = {
                        // Use the current storyContinuation value for vetting
                        val currentText = storyContinuation
                        val vettingResult =
                            vetStoryContinuation(currentText, liveBook.initialParagraph)
                        if (vettingResult.isValid) {
                            viewModel.submitContribution(
                                liveBookId = liveBook.id,
                                paragraphContent = currentText,
                                writingTimeSeconds = elapsedSeconds
                            )
                            // Clear the input field after submission
                            onStoryChange("")
                            timerRunning = false
                        } else {
                            messageBarState.addError(
                                message = vettingResult.errorMessage ?: Strings.invalid_content
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(88.dp))
    }
}


@Composable
private fun ParticipantAvatar(
    imageUrl: String,
    name: String,
    isAuthor: Boolean
) {
    Box {
        if (imageUrl.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isAuthor) MaterialTheme.colorScheme.primaryContainer
                        else Color(0xFF87CEEB)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LexendMediumFont()
                )
            }
        }

        // Author badge
        if (isAuthor) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color.Green)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
private fun OtherLiveBookItem(
    liveBook: LiveBook,
    onClick: () -> Unit
) {
    val progress = calculateLiveBookProgress(liveBook)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with title and progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = liveBook.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "$progress%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Genre and participants info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Genre chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary)
                    )
                    Text(
                        text = liveBook.genre,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Participants count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(Resources.Icon.FlipLiveBook),
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${liveBook.participantsCount} writers",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            // Call to action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap to continue this story",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )

                Icon(
                    imageVector = vectorResource(Resources.Icon.ArrowRight),
                    contentDescription = "Continue story",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
