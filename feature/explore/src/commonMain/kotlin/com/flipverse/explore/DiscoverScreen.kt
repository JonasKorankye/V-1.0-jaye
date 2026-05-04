package com.flipverse.explore

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.flipverse.shared.Alpha
import com.flipverse.shared.BlackLight
import com.flipverse.shared.Constants
import com.flipverse.shared.PreferencesRepository
import com.flipverse.shared.Resources
import com.flipverse.shared.Strings
import com.flipverse.shared.domain.User
import com.flipverse.shared.util.createShareManager
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    paddingValues: PaddingValues,
    navigateToViewProfile: (String) -> Unit = {}
) {

    val viewModel: ExploreViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var searchText by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }


    val shareManager = remember { createShareManager() }

    // Get current user email
    val currentUserEmail = remember { PreferencesRepository.getEmail() }

    // Filter users based on search text from all users, excluding current user
    val filteredUsers = remember(uiState.allUsers, searchText, currentUserEmail) {
        val usersExcludingCurrent = uiState.allUsers.filter { user ->
            user.email != currentUserEmail
        }
        
        if (searchText.isBlank()) {
            usersExcludingCurrent  // Show all users (except current) when not searching
        } else {
            usersExcludingCurrent.filter { user ->
                user.fullname.contains(searchText, ignoreCase = true) ||
                        user.username.contains(searchText, ignoreCase = true) ||
                        user.email.contains(searchText, ignoreCase = true)
            }
        }
    }

    val suggestedUsers = remember(uiState.suggestedUsers, currentUserEmail) {
        uiState.suggestedUsers.filter { user ->
            user.email != currentUserEmail
        }
    }

    // Load more users when filtered results are getting low
    LaunchedEffect(filteredUsers.size, uiState.allUsers.size) {
        if (searchText.isNotBlank() && filteredUsers.size < 10 && uiState.hasMorePages && !uiState.isLoadingMore) {
            viewModel.loadMoreUsers(limit = 200)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
            .padding(paddingValues)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Search Bar
//        TextField(
//            value = searchText,
//            onValueChange = { searchText = it },
//            placeholder = {
//                Text(
//                    "Search people to follow...",
//                    color = MaterialTheme.colorScheme.onSecondary
//                )
//            },
//            leadingIcon = {
//                Icon(
//                    imageVector = vectorResource(Resources.Icon.Search),
//                    contentDescription = "Search icon",
//                    tint = MaterialTheme.colorScheme.onSecondary
//                )
//            },
//            modifier = Modifier
//                .fillMaxWidth()
//                .clip(RoundedCornerShape(8.dp))
//                .background(MaterialTheme.colorScheme.surface),
//            colors = TextFieldDefaults.colors(
//                focusedContainerColor = MaterialTheme.colorScheme.surface,
//                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
//                disabledContainerColor = MaterialTheme.colorScheme.surface,
//                focusedIndicatorColor = Color.Transparent,
//                unfocusedIndicatorColor = Color.Transparent,
//                disabledIndicatorColor = Color.Transparent,
//                cursorColor = MaterialTheme.colorScheme.onPrimary,
//                focusedTextColor = MaterialTheme.colorScheme.onPrimary,
//                unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
//            ),
//            singleLine = true
//        )

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
                            text = "Search people to follow...",
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

        Spacer(modifier = Modifier.height(8.dp))

//        Spacer(modifier = Modifier.height(8.dp))


        // Invite friends Section - Only show when not searching
        if (searchText.isBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary) // Card-like background
                    .padding(12.dp)
                    .clickable { /* Handle invite friends click */ },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Color(0xFF74C0FC)
                            ) //  color for the circle
                            .border(2.dp, Color.Transparent, CircleShape), //  border color
                    ) {
                        Icon(
                            modifier = Modifier.padding(10.dp),
                            imageVector = vectorResource(Resources.Icon.Invite), // Replace with appropriate icon if available
                            contentDescription = "Invite Friends Icon",
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Invite friends",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 16.sp
                    )
                }

                FilledTonalIconButton(
                    onClick = {
                        shareManager.shareInviteLink(
                            inviteLink = Constants.INVITE_LANDING_URL,
                            title = "Join me on this awesome app!"
                        )
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        vectorResource(Resources.Icon.Arrow),
                        "Chevron Right",
                        tint = BlackLight,
                        modifier = Modifier.size(16.dp)
                    )
                }

            }

            HorizontalDivider(
                modifier = Modifier.padding(4.dp),
                thickness = 1.dp,
                color = Color.Gray.copy(alpha = Alpha.DISABLED)
            )


            Spacer(modifier = Modifier.height(16.dp))
        }


        // Show search results count or all users header
        if (searchText.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "SEARCH RESULTS",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${filteredUsers.size} found",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "WHO TO FOLLOW",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Display users list - either all users or filtered search results
        LazyColumn(
            userScrollEnabled = true
        ) {
            val usersToDisplay = if (searchText.isBlank()) suggestedUsers else filteredUsers

            if (usersToDisplay.isNotEmpty()) {
                items(usersToDisplay) { user ->
                    SuggestionItem(
                        suggestion = user,
                        onFollowClick = { userId ->
                            viewModel.onFollowClick(userId)
                        },
                        isFollowing = uiState.followingUsers.contains(user.email),
                        onProfileClick = { userId ->
                            navigateToViewProfile(userId)
                        }
                    )
                }
                item{
                    Spacer(Modifier.padding(64.dp))
                }

                // Show loading indicator when loading more users
                if (uiState.isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Loading more users...",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    item {
                        Spacer(Modifier.padding(64.dp))
                    }
                }
        } else if (searchText.isNotBlank()) {
                // Show "no results" message when searching returns nothing
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No users found",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Try searching with different keywords",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (uiState.isLoading && searchText.isBlank()) {
                // Show loading state
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Loading suggestions...",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Show empty state when no users available
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (searchText.isBlank()) "No suggestions available" else "No users available",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchText.isBlank()) "Pull to refresh or check back later" else "Pull to refresh or check back later",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(72.dp))
    }

}

@Composable
fun SuggestionItem(
    suggestion: User,
    isFollowing: Boolean,
    onFollowClick: (String) -> Unit,
    onProfileClick: (String) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (suggestion.thumbnail.isNotEmpty()) {
                AsyncImage(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onProfileClick(suggestion.email) },
                    model = ImageRequest.Builder(
                        LocalPlatformContext.current
                    ).data(suggestion.thumbnail)
                        .crossfade(enable = true)
                        .build(),
                    contentDescription = "Author Profile URL",
                    contentScale = ContentScale.Crop
                )

            } else {

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onProfileClick(suggestion.email) }
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = Alpha.HALF)) //  color for the circle
                        .border(2.dp, Color.Transparent, CircleShape), //  border color
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = suggestion.fullname.take(1).uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.SansSerif
                    )

                }
            }

            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    suggestion.fullname,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 16.sp
                )
                Text(
                    text = if (suggestion.username.isEmpty()) "" else normalizeUsername(suggestion.username),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
        Button(
            onClick = { onFollowClick(suggestion.email) },
            colors = ButtonDefaults.buttonColors(containerColor = if (isFollowing) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = if (isFollowing) Strings.following else Strings.follow,
                fontSize = 12.sp,
                color = if (isFollowing)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    BlackLight
            )
        }
    }
}

// Helper function to normalize username
private fun normalizeUsername(username: String): String {
    return if (username.startsWith("@")) {
        username
    } else {
        "@$username"
    }
}
