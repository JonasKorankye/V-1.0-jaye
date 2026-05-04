package com.flipverse.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.flipverse.dashboard.DashboardViewModel
import com.flipverse.data.util.normalizeUsername
import com.flipverse.shared.Alpha
import com.flipverse.shared.PreferencesRepository.getEmail
import com.flipverse.shared.Resources
import com.flipverse.shared.Strings
import com.flipverse.shared.domain.User
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMessageScreen(
    navigateBack: () -> Unit,
    navigateToConversation: (String, String) -> Unit, // conversationId, otherUserId
) {
    val chatViewModel = koinViewModel<ChatViewModel>()
    val dashboardViewModel = koinViewModel<DashboardViewModel>()
    val chatState by chatViewModel.uiState.collectAsState()
    val dashboardState by dashboardViewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current



    var searchText by remember { mutableStateOf("") }
    var creatingConversationWithUserId by remember { mutableStateOf<String?>(null) }

    // Load users when screen opens
    LaunchedEffect(Unit) {
        dashboardViewModel.loadAllUsers()
    }

    // Filter users based on search
    val currentUserEmail = getEmail()
    val filteredUsers = remember(dashboardState.allUsers, searchText, currentUserEmail) {
        val filtered = if (searchText.isEmpty()) {
            dashboardState.allUsers.take(20) // Show recent users
        } else {
            dashboardState.allUsers.filter { user ->
                user.fullname.contains(searchText, ignoreCase = true) ||
                user.username.contains(searchText, ignoreCase = true) ||
                user.email.contains(searchText, ignoreCase = true)
            }.take(10)
        }
        filtered.filter { it.email != currentUserEmail }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = Strings.new_message_title,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = MaterialTheme.colorScheme.primary
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
//            TextField(
//                value = searchText,
//                onValueChange = { searchText = it },
//                placeholder = {
//                    Text(
//                        Strings.search_people_placeholder,
//                        color = MaterialTheme.colorScheme.onSecondary
//                    )
//                },
//                leadingIcon = {
//                    Icon(
//                        imageVector = vectorResource(Resources.Icon.Search),
//                        contentDescription = Strings.search_icon,
//                        tint = MaterialTheme.colorScheme.onSecondary
//                    )
//                },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .clip(RoundedCornerShape(24.dp)),
//                colors = TextFieldDefaults.colors(
//                    focusedContainerColor = MaterialTheme.colorScheme.surface,
//                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
//                    disabledContainerColor = MaterialTheme.colorScheme.surface,
//                    focusedIndicatorColor = Color.Transparent,
//                    unfocusedIndicatorColor = Color.Transparent,
//                    disabledIndicatorColor = Color.Transparent,
//                    cursorColor = MaterialTheme.colorScheme.onPrimary,
//                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
//                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
//                ),
//                singleLine = true
//            )

            val interactionSource = remember { MutableInteractionSource() }
            BasicTextField(
                value = searchText,
                onValueChange = { searchText = it },
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
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                interactionSource = interactionSource,
                decorationBox = { innerTextField ->
                    TextFieldDefaults.DecorationBox(
                        value = searchText,
                        innerTextField = innerTextField,
                        enabled = true,
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
                        interactionSource = interactionSource,
                        placeholder = {
                            Text(
                                text = Strings.search_people_placeholder,
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


            Spacer(modifier = Modifier.height(16.dp))

            // Content
            when {
                dashboardState.isLoading && filteredUsers.isEmpty() -> {
                    LoadingContent()
                }
                
                filteredUsers.isEmpty() && searchText.isNotEmpty() -> {
                    EmptySearchContent()
                }
                
                filteredUsers.isEmpty() -> {
                    EmptyUsersContent()
                }
                
                else -> {
                    UsersList(
                        users = filteredUsers,
                        onUserClick = { user ->
                            if (creatingConversationWithUserId != user.email) {
                                creatingConversationWithUserId = user.email
                                chatViewModel.startConversationWith(user.email) { conversationId ->
                                    creatingConversationWithUserId = null
                                    navigateToConversation(conversationId, user.email)
                                }
                            }
                        },
                        creatingConversationWithUserId = creatingConversationWithUserId
                    )
                }
            }
            
            // Error handling
            chatState.error?.let { error ->
                ErrorMessage(
                    message = error,
                    onDismiss = { chatViewModel.clearError() }
                )
            }
        }
    }
}

@Composable
private fun UsersList(
    users: List<User>,
    onUserClick: (User) -> Unit,
    creatingConversationWithUserId: String?,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        items(users) { user ->
            UserListItem(
                user = user,
                onClick = { onUserClick(user) },
                enabled = creatingConversationWithUserId != user.email
            )
        }
    }
}

@Composable
private fun UserListItem(
    user: User,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User Avatar
        UserAvatar(
            user = user,
            size = 48.dp
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = user.fullname.ifEmpty { Strings.unknown_user },
                color = if (enabled) MaterialTheme.colorScheme.onPrimary else Color.Gray,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = normalizeUsername(user.username),
                color = Color.Gray.copy(alpha = if (enabled) 1f else 0.6f),
                fontSize = 14.sp
            )
        }
        
        if (!enabled) {
            AdaptiveCircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun UserAvatar(
    user: User,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    if (user.thumbnail.isNotEmpty()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(user.thumbnail)
                .crossfade(true)
                .build(),
            contentDescription = Strings.profile_picture,
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = Alpha.HALF)
                )
                .border(2.dp, Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.fullname.take(1).uppercase().ifEmpty { "U" },
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = (size.value / 3).sp,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AdaptiveCircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = Strings.loading_users,
                color = Color.Gray,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun EmptySearchContent() {
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
                text = Strings.no_users_found,
                color = Color.Gray,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = Strings.try_a_different_search_term_short,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun EmptyUsersContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = vectorResource(Resources.Icon.Person),
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = Strings.no_users_available,
                color = Color.Gray,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = Strings.check_connection_try_again,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                Color.Red.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = Color.Red,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = vectorResource(Resources.Icon.Close),
                    contentDescription = Strings.dismiss,
                    tint = Color.Red
                )
            }
        }
    }
}

// Extension function to handle alpha modifier
private fun Modifier.alpha(alpha: Float): Modifier {
    return this.then(
        Modifier.background(Color.Transparent.copy(alpha = 1f - alpha))
    )
}
