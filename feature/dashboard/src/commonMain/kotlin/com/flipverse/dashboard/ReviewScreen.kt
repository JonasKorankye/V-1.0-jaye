package com.flipverse.dashboard

import ContentWithMessageBar
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext as CoilPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.flipverse.data.util.normalizeUsername
import com.flipverse.shared.Alpha
import com.flipverse.shared.Ash
import com.flipverse.shared.BlackLight
import com.flipverse.shared.LexendMediumFont
import com.flipverse.shared.Resources
import com.flipverse.shared.domain.CreatePostRequest
import com.flipverse.shared.domain.PostType
import com.flipverse.shared.domain.User
import com.flipverse.shared.presentation.ui.ObserveAsEvents
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import rememberMessageBarState
import com.flipverse.shared.Strings

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReviewScreen(
    navigateBack: () -> Unit,
    navigateToDashboard: () -> Unit
) {

    val viewModel = koinViewModel<DashboardViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var screenState = viewModel.recommendationScreenState
    var loadingState by rememberSaveable { mutableStateOf(false) }
    val messageBarState = rememberMessageBarState()
    val createPostState by viewModel.uiCreatePostState.collectAsStateWithLifecycle()
    val onAction = viewModel::onAction

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Local state for selected users
    var selectedUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var previousWhatsNewText by remember { mutableStateOf("") }
    var previousSourceText by remember { mutableStateOf("") }

    // Users are loaded on-demand as the user types — no upfront bulk fetch needed.

    ObserveAsEvents(viewModel.event) { event ->
        when (event) {
            is DashboardEvent.Navigate.Dashboard -> navigateToDashboard()
            is DashboardEvent.Error -> {}
            is DashboardEvent.Success -> {}
        }
    }


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {

                    Text(
                        text = Strings.review_title,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )

                },
                navigationIcon = {
                    IconButton(onClick = { navigateBack() }) {
                        Icon(
                            imageVector = vectorResource(Resources.Icon.BackArrow),
                            contentDescription = Strings.back,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary // Use primaryContainer for app bar background
                )
            )
        },
        modifier = Modifier.fillMaxSize() // Scaffold covers the entire screen
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
            successContainerColor = MaterialTheme.colorScheme.onSecondaryContainer,
            successContentColor = BlackLight,
            showCopyButton = false,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) { focusManager.clearFocus() }
                    .background(color = MaterialTheme.colorScheme.primary)
                    .verticalScroll(
                        rememberScrollState(),
                    )
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Spacer(modifier = Modifier.height(32.dp))

                // What's new? TextField
                OutlinedTextField(
                    value = screenState.whatsNewText,
                    onValueChange = { newValue ->
                        // Detect unnecessary whitespace
                        val hasMultipleSpaces = newValue.contains(Regex(" {2,}")) // Two or more consecutive spaces (not newlines)
                        val hasLeadingSpaces = newValue.startsWith(" ") && previousWhatsNewText.isEmpty()
                        val hasExcessiveWhitespace = newValue.count { it == ' ' } > newValue.length * 0.3 && newValue.length > 20

                        if (hasMultipleSpaces) {
                            messageBarState.addError(
                                Exception("Multiple consecutive spaces detected. Please use single spaces between words.")
                            )
                            return@OutlinedTextField
                        }

                        if (hasLeadingSpaces) {
                            messageBarState.addError(
                                Exception("Please don't start with spaces. Begin typing directly.")
                            )
                            return@OutlinedTextField
                        }

                        if (hasExcessiveWhitespace) {
                            messageBarState.addError(
                                Exception("Excessive whitespace detected. Please write naturally without unnecessary spaces.")
                            )
                            return@OutlinedTextField
                        }

                        onAction(
                            DashboardAction.OnWhatsNewChange(
                                newValue
                            )
                        )
                        previousWhatsNewText = newValue
                    },
                    placeholder = {
                        Text(
                            "Tell us what you think",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.onSecondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSecondary,
                        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        errorBorderColor = MaterialTheme.colorScheme.error,
                        focusedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        cursorColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Default),
                    minLines = 5
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tag friends TextField with User Search
                Text(
                    text = Strings.tag_friends,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                )

                // Selected Users Display — backed by local state, no allUsers dependency
                if (selectedUsers.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedUsers.forEach { user ->
                            SelectedUserChip(
                                user = user,
                                onRemove = {
                                    selectedUsers = selectedUsers.filter { it.id != user.id }
                                }
                            )
                        }
                    }
                }

                // Search Box with Dropdown
                Box {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { newValue ->
                            searchQuery = newValue
                            viewModel.onTagSearchQueryChange(newValue)
                        },
                        placeholder = {
                            Text(
                                Strings.type_follower_names_to_add_dashboard,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.onSecondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSecondary,
                            focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                            unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                            errorBorderColor = MaterialTheme.colorScheme.error,
                            focusedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            cursorColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    viewModel.onTagSearchQueryChange("")
                                }) {
                                    Icon(
                                        imageVector = vectorResource(Resources.Icon.Close),
                                        contentDescription = Strings.clear_search,
                                        tint = Color.Gray
                                    )
                                }
                            }
                        }
                    )

                    // Dropdown — driven by on-demand Firestore search results
                    if (searchQuery.isNotEmpty()) {
                        val dropdownUsers = uiState.userSearchResults.filter { user ->
                            !selectedUsers.contains(user)
                        }
                        val showSpinner = uiState.isSearchingUsers && dropdownUsers.isEmpty()

                        if (showSpinner || dropdownUsers.isNotEmpty()) {
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
                                if (showSpinner) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AdaptiveCircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            trackColor = Color.Transparent
                                        )
                                    }
                                } else {
                                    LazyColumn {
                                        items(dropdownUsers) { user ->
                                            UserDropdownItem(
                                                user = user,
                                                onSelect = {
                                                    selectedUsers = selectedUsers + user
                                                    searchQuery = ""
                                                    viewModel.onTagSearchQueryChange("")
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Source TextField
                OutlinedTextField(
                    value = screenState.sourceText,
                    onValueChange = { newValue ->
                        // Detect unnecessary whitespace
                        val hasMultipleSpaces = newValue.contains(Regex("\\s{2,}")) // Two or more consecutive spaces
                        val hasLeadingSpaces = newValue.startsWith(" ") && previousSourceText.isEmpty()
                        val hasExcessiveWhitespace = newValue.count { it.isWhitespace() } > newValue.length * 0.3 && newValue.length > 20

                        if (hasMultipleSpaces) {
                            messageBarState.addError(
                                Exception("Multiple consecutive spaces detected. Please use single spaces between words.")
                            )
                            return@OutlinedTextField
                        }

                        if (hasLeadingSpaces) {
                            messageBarState.addError(
                                Exception("Please don't start with spaces. Begin typing directly.")
                            )
                            return@OutlinedTextField
                        }

                        if (hasExcessiveWhitespace) {
                            messageBarState.addError(
                                Exception("Excessive whitespace detected. Please write naturally without unnecessary spaces.")
                            )
                            return@OutlinedTextField
                        }

                        onAction(DashboardAction.OnSourceChange(newValue))
                        previousSourceText = newValue
                    },
                    placeholder = {
                        Text(
                            Strings.source_label,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.onSecondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSecondary,
                        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        errorBorderColor = MaterialTheme.colorScheme.error,
                        focusedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        cursorColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    modifier = Modifier.fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onSecondary,
                            shape = RoundedCornerShape(size = 99.dp)
                        )
                        .height(48.dp),
                    onClick = {
                        loadingState = true
                        // Extract tagged users from the text
                        val taggedUsers = selectedUsers.map { "@${normalizeUsername(it.username)}" }
                        val request = CreatePostRequest(
                            whatsNew = screenState.whatsNewText,
                            postType = PostType.REVIEW,
                            source = screenState.sourceText,
                            tags = taggedUsers
                        )

                        viewModel.createPost(
                            request,
                            onSuccess = {
                                loadingState = false
                                navigateToDashboard()
                            },
                            onError = { err ->
                                println("Error in full:: $err")
                                loadingState = false
                                messageBarState.addError(err)
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = Color.LightGray,
                    ),
                    enabled = screenState.whatsNewText.isNotEmpty()
                ) {
                    if (loadingState) {
                        AdaptiveCircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = BlackLight,
                            trackColor = Color.Transparent,
                            strokeCap = StrokeCap.Round
                        )
                    } else {
                        Text(
                            Strings.post_action,
                            color = if (screenState.whatsNewText.isNotEmpty() && !createPostState.isCreating) BlackLight else MaterialTheme.colorScheme.onPrimary,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.padding(64.dp))
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
                    model = ImageRequest.Builder(CoilPlatformContext.current)
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
                        )
                        .border(1.dp, Color.White, CircleShape),
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
                model = ImageRequest.Builder(CoilPlatformContext.current)
                    .data(user.thumbnail)
                    .crossfade(true)
                    .build(),
                contentDescription = "User avatar",
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
                    )
                    .border(1.dp, Color.White, CircleShape),
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