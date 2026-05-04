package com.flipverse.livebook.screens

import ContentWithMessageBar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import com.flipverse.data.util.getCurrentTimeMillis
import com.flipverse.data.util.normalizeUsername
import com.flipverse.data.util.vetStoryContinuation
import com.flipverse.data.util.formatElapsedTime
import com.flipverse.livebook.LiveBookEvent
import com.flipverse.livebook.LiveBookViewModel
import com.flipverse.shared.Alpha
import com.flipverse.shared.Ash
import com.flipverse.shared.BlackLight
import com.flipverse.shared.LexendMediumFont
import com.flipverse.shared.PreferencesRepository.getEmail
import com.flipverse.shared.Resources
import com.flipverse.shared.Strings
import com.flipverse.shared.domain.User
import com.flipverse.shared.presentation.component.FlipButton
import com.flipverse.shared.presentation.ui.ObserveAsEvents
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import rememberMessageBarState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewLiveBookScreen(
    onClose: () -> Unit,
) {
    val viewModel = koinViewModel<LiveBookViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messageBarState = rememberMessageBarState()
    val scope = rememberCoroutineScope()

    val keyboardController = LocalSoftwareKeyboardController.current

    var genreExpanded by remember { mutableStateOf(false) }

    val hideKeyboardController = LocalSoftwareKeyboardController.current

    val scrollState = rememberScrollState()

    // Auto-scroll to bottom when search results appear so user can see them above keyboard
    LaunchedEffect(uiState.filteredUsers) {
        if (uiState.filteredUsers.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }


    // Writing Timer State
    var writingStartTime by remember { mutableStateOf<Long?>(null) }
    var elapsedSeconds by remember { mutableStateOf(0L) }
    var timerRunning by remember { mutableStateOf(false) }

    // Clipboard paste detection state
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

    val maxChars = 500
    val maxTaggedParticipants = 7 // Max 7 tagged users + 1 initiator = 8 total



    ObserveAsEvents(viewModel.event) { event ->

        when (event) {
            is LiveBookEvent.Navigate.OpenLiveBook -> onClose()
            is LiveBookEvent.Error -> {
                messageBarState.addError(message = event.error)
            }

            is LiveBookEvent.Success -> {
                scope.launch {
                    messageBarState.addSuccess(Strings.story_published_successfully)
                    delay(1200)
                    onClose()
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(Strings.new_story_challenge_title, fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = { onClose() }) {
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
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    // Genre Dropdown
                    ExposedDropdownMenuBox(
                        expanded = genreExpanded,
                        onExpandedChange = { genreExpanded = !genreExpanded }
                    ) {
                        OutlinedTextField(
                            value = if (uiState.selectedGenre.isEmpty()) Strings.select_genre else uiState.selectedGenre,
                            onValueChange = { },
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            trailingIcon = {
                                Icon(
                                    imageVector = vectorResource(Resources.Icon.ArrowDown),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                focusedBorderColor = Color.Gray,
                                unfocusedBorderColor = Color.Gray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = genreExpanded,
                            onDismissRequest = { genreExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            if (uiState.isLoadingGenres) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = Strings.loading_genres,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    },
                                    onClick = { }
                                )
                            } else if (uiState.genres.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = Strings.no_genres_available,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    },
                                    onClick = { }
                                )
                            } else {
                                uiState.genres.forEach { genre ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = genre,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        },
                                        onClick = {
                                            viewModel.selectGenre(genre)
                                            genreExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Challenge Title
                    OutlinedTextField(
                        value = uiState.challengeTitle,
                        onValueChange = { viewModel.updateChallengeTitle(it) },
                        placeholder = { Text(Strings.challenge_title, color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                            unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                            cursorColor = MaterialTheme.colorScheme.onPrimary,
                            focusedBorderColor = Color.Gray,
                            unfocusedBorderColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Local text state to keep iOS native text field in sync.
                    // Using return@TextField on iOS causes UITextField/Compose desync,
                    // making the keyboard unresponsive after errors.
                    var localParagraphText by remember { mutableStateOf(uiState.initialParagraph) }

                    // Sync from ViewModel when it changes externally
                    LaunchedEffect(uiState.initialParagraph) {
                        if (localParagraphText != uiState.initialParagraph) {
                            localParagraphText = uiState.initialParagraph
                        }
                    }

                    // Initial Paragraph
                    TextField(
                        value = localParagraphText,
                        onValueChange = { newText ->
                            // Check for clipboard paste by comparing with actual clipboard content
                            val clipboardText = clipboardManager.getText().toString().trim()
                            val textDiff = newText.length - previousText.length

                            // Detect clipboard paste: large text insertion that matches clipboard content
                            val isClipboardPaste = clipboardText.isNotEmpty() &&
                                    clipboardText.length > 10 && // Clipboard has substantial content
                                    textDiff > 10 && // Large text insertion (more than typical typing)
                                    newText.contains(
                                        clipboardText,
                                        ignoreCase = true
                                    ) // New text contains clipboard content

                            // Detect unnecessary whitespace
                            val hasMultipleSpaces = newText.contains(Regex("\\s{2,}")) // Two or more consecutive spaces
                            val hasLeadingSpaces = newText.startsWith(" ") && previousText.isEmpty()
                            val hasExcessiveWhitespace = newText.count { it.isWhitespace() } > newText.length * 0.3 && newText.length > 20

                            when {
                                isClipboardPaste -> {
                                    messageBarState.addError(
                                        "Pasting from clipboard is not allowed. Please type your paragraph manually to ensure authentic writing."
                                    )
                                    // Revert local text to keep iOS keyboard in sync
                                    localParagraphText = previousText
                                }
                                hasMultipleSpaces -> {
                                    messageBarState.addError(
                                        "Multiple consecutive spaces detected. Please use single spaces between words."
                                    )
                                    localParagraphText = previousText
                                }
                                hasLeadingSpaces -> {
                                    messageBarState.addError(
                                        "Please don't start with spaces. Begin typing your paragraph directly."
                                    )
                                    localParagraphText = previousText
                                }
                                hasExcessiveWhitespace -> {
                                    messageBarState.addError(
                                        "Excessive whitespace detected. Please write naturally without unnecessary spaces."
                                    )
                                    localParagraphText = previousText
                                }
                                newText.length <= maxChars -> {
                                    localParagraphText = newText
                                    viewModel.updateInitialParagraph(newText)
                                    previousText = newText
                                }
                            }

                            // Start timer on first character typed
                            if (!timerRunning && newText.isNotEmpty()) {
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
                                text = Strings.write_initial_paragraph_placeholder,
                                color = Color.Gray
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { keyboardController?.hide() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.primary,
                            unfocusedContainerColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                            unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                            cursorColor = MaterialTheme.colorScheme.onPrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Favorite Button
                            IconButton(
                                onClick = { viewModel.toggleLike() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = vectorResource(
                                        if (uiState.isLiked) Resources.Icon.LikeSelected else Resources.Icon.Like
                                    ),
                                    contentDescription = if (uiState.isLiked) Strings.remove_from_likes else Strings.add_to_likes,
                                    tint = if (uiState.isLiked) Color.Red else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Writing Timer Display
                            if (timerRunning && uiState.initialParagraph.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(Resources.Icon.FlipNotify),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = Strings.writing_prefix + formatElapsedTime(elapsedSeconds),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Characters Remaining
                        Text(
                            text = "${maxChars - uiState.initialParagraph.length}" + Strings.characters_remaining_suffix,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Selected Users Display (excluding current user)
                    val currentUserEmail = getEmail()
                    val filteredSelectedUsers by remember(uiState.selectedUsers, currentUserEmail) {
                        derivedStateOf {
                            uiState.selectedUsers.filter { user ->
                                user.email != currentUserEmail || user.id != currentUserEmail
                            }
                        }
                    }

                    // Tag Participants Section
                    Text(
                        text = Strings.tag_participants + " (${filteredSelectedUsers.size}/$maxTaggedParticipants)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (filteredSelectedUsers.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filteredSelectedUsers.forEach { user ->
                                SelectedUserChip(
                                    user = user,
                                    onRemove = { viewModel.removeUser(user) }
                                )
                            }
                        }
                    }

                    // Search Box with Dropdown
                    Box {
                        OutlinedTextField(
                            value = viewModel.searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = {
                                Text(
                                    Strings.type_follower_names_to_add,
                                    color = Color.Gray
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                focusedContainerColor = MaterialTheme.colorScheme.primary,
                                unfocusedContainerColor = MaterialTheme.colorScheme.primary,
                                cursorColor = MaterialTheme.colorScheme.onPrimary,
                                focusedBorderColor = Color.Gray,
                                unfocusedBorderColor = Color.Gray
                            ),
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                hideKeyboardController?.hide()
                            }),
                            shape = RoundedCornerShape(8.dp),
                            trailingIcon = {
                                if (viewModel.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.clearSearch() }) {
                                        Icon(
                                            imageVector = vectorResource(Resources.Icon.Close),
                                            contentDescription = Strings.clear_search,
                                            tint = Color.Gray
                                        )
                                    }
                                }
                            }
                        )

                        // Dropdown list for filtered users
                        if (uiState.filteredUsers.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .padding(top = 60.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                LazyColumn {
                                    items(uiState.filteredUsers) { user ->
                                        UserDropdownItem(
                                            user = user,
                                            onSelect = {
                                                val currentSelected = uiState.selectedUsers
                                                if (!currentSelected.contains(user)) {
                                                    if (currentSelected.size >= maxTaggedParticipants) {
                                                        messageBarState.addError(Strings.max_participants_reached)
                                                        return@UserDropdownItem
                                                    }
                                                    viewModel.selectUser(user)
                                                    viewModel.updateSearchQuery("")
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Post Button
                Box(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    FlipButton(
                        text = if (uiState.isPublishing) "" else Strings.publish,
                        enabled = uiState.challengeTitle.isNotEmpty() && uiState.initialParagraph.isNotEmpty() && uiState.selectedGenre.isNotEmpty() && !uiState.isPublishing,
                        onClick = {
                            val vettingResult = vetStoryContinuation(uiState.initialParagraph)
                            if (!vettingResult.isValid) {
                                messageBarState.addError(
                                    message = vettingResult.errorMessage
                                        ?: "The story paragraph does not meet content guidelines."
                                )
                                return@FlipButton
                            }
                            viewModel.publishNewStory(writingTimeSeconds = elapsedSeconds)
                            timerRunning = false
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedVisibility(
                            visible = uiState.isPublishing,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            AdaptiveCircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(88.dp))
            }
        }
    }
}

@Composable
private fun SelectedUserChip(
    user: User,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = BlackLight
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (user.thumbnail.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(user.thumbnail)
                        .crossfade(true)
                        .build(),
                    contentDescription = Strings.user_avatar,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(
                                alpha = Alpha.HALF
                            )
                        ) //  color for the circle
                        .border(1.dp, Color.White, CircleShape),//  border color
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.fullname.take(1).uppercase(),
                        modifier = Modifier.padding(bottom = 6.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 10.sp,
                        fontFamily = LexendMediumFont()
                    )
                }
            }


            Text(
                text = user.fullname.ifEmpty { user.username },
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    imageVector = vectorResource(Resources.Icon.Close),
                    contentDescription = Strings.remove_user,
                    modifier = Modifier.size(12.dp),
                    tint = Ash
                )
            }
        }
    }
}

@Composable
private fun UserDropdownItem(
    user: User,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (user.thumbnail.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(user.thumbnail)
                    .crossfade(true)
                    .build(),
                contentDescription = Strings.user_avatar,
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
                        MaterialTheme.colorScheme.primaryContainer.copy(
                            alpha = Alpha.HALF
                        )
                    ) //  color for the circle
                    .border(1.dp, Color.White, CircleShape),//  border color
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.fullname.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 10.sp,
                    fontFamily = LexendMediumFont()
                )
            }


        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = user.fullname.ifEmpty { Strings.unknown },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = normalizeUsername(user.username),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

