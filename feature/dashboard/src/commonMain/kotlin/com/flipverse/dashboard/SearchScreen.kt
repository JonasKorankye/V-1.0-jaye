package com.flipverse.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.flipverse.dashboard.component.PeopleToFollowSection
import com.flipverse.data.util.normalizeUsername
import com.flipverse.shared.Alpha
import com.flipverse.shared.BlackLight
import com.flipverse.shared.PreferencesRepository.getEmail
import com.flipverse.shared.PreferencesRepository.getFullName
import com.flipverse.shared.PreferencesRepository.getThumbnail
import com.flipverse.shared.PreferencesRepository.loadFlipInterests
import com.flipverse.shared.PreferencesRepository.loadSuggestedUsers
import com.flipverse.shared.PreferencesRepository.saveSuggestedUsers
import com.flipverse.shared.Resources
import com.flipverse.shared.Strings
import com.flipverse.shared.WorkSansBoldFont
import com.flipverse.shared.domain.Post
import com.flipverse.shared.domain.User
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.random.Random

// --- Color Generation Function ---
fun generateRandomBeautifulColor(): Color {
    val beautifulColors = listOf(
        Color(0xFF6B73FF), // Soft Blue
        Color(0xFF9F7AEA), // Soft Purple
        Color(0xFF4FD1C7), // Teal
        Color(0xFF38B2AC), // Green-Blue
        Color(0xFFED8936), // Orange
        Color(0xFFE53E3E), // Red
        Color(0xFF3182CE), // Blue
        Color(0xFF805AD5), // Purple
        Color(0xFF319795), // Cyan
        Color(0xFFD69E2E), // Yellow
        Color(0xFF48BB78), // Green
        Color(0xFFECC94B), // Light Yellow
        Color(0xFFF56565), // Light Red
        Color(0xFF667EEA), // Indigo
        Color(0xFF764ABC), // Dark Purple
        Color(0xFF0D9488), // Dark Teal
        Color(0xFF1E40AF), // Dark Blue
        Color(0xFF7C3AED), // Violet
        Color(0xFF059669), // Emerald
        Color(0xFFDC2626), // Dark Red
    )
    return beautifulColors[Random.nextInt(beautifulColors.size)]
}


@Composable
fun SearchScreen(
    navigateToProfile: () -> Unit,
    navigateToSeeAllScreen: () -> Unit,
    navigateToPost: (String) -> Unit,
    navigateToViewProfile: (String) -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    val viewModel = koinViewModel<DashboardViewModel>()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val onAction = viewModel::onAction
    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = Unit) {
        viewModel.trendingFeed()
        // Load all users for search functionality
        viewModel.loadAllUsers(limit = 200)
        // Load current user's following list to show correct button states
        viewModel.loadCurrentUserFollowing()
    }

    // Get user's selected interests
    val userInterests = remember { loadFlipInterests() }
    val suggestedUsers = remember { loadSuggestedUsers() }

    // Get search query from viewModel
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    // Filter users based on search query
    val filteredUsers = remember(uiState.allUsers, searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            uiState.allUsers.filter { user ->
                user.fullname.contains(searchQuery, ignoreCase = true) ||
                        user.username.contains(searchQuery, ignoreCase = true) ||
                        user.email.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Categorize trending posts by interests
    val categorizedPosts = remember(uiState.trendingPosts, userInterests) {
        if (userInterests.isEmpty()) {
            // If no interests, group all posts under "Trending for You"
            mapOf("Trending for You" to uiState.trendingPosts)
        } else {
            // Group posts by matching interests, ensuring each post appears only once
            val grouped = mutableMapOf<String, MutableList<Post>>()
            val assignedPosts = mutableSetOf<String>() // Track posts already assigned

            uiState.trendingPosts.forEach { post ->
                if (!assignedPosts.contains(post.id)) {
                    val matchingInterests = post.selectedInterests.intersect(userInterests.toSet())

                    if (matchingInterests.isNotEmpty()) {
                        // Add to the first matching interest category only
                        val firstMatchingInterest = matchingInterests.first()
                        val categoryName = "Trending in $firstMatchingInterest"
                        grouped.getOrPut(categoryName) { mutableListOf() }.add(post)
                        assignedPosts.add(post.id)
                    } else {
                        // Add to general category if no interests match
                        grouped.getOrPut("Trending for You") { mutableListOf() }.add(post)
                        assignedPosts.add(post.id)
                    }
                }
            }

            // Convert to immutable map
            grouped.mapValues { it.value.toList() }.toMap()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primary,
    ) { paddingValues ->
        // Show loading state while initial data is being fetched
        if (uiState.isInitialLoading) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                item {
                    TopSearchBar(
                        onClickBack = onNavigateBack,
                        navigateToProfile = navigateToProfile,
                        viewModel = viewModel
                    )
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = Strings.loading_ellipsis,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = Strings.getting_personalized_content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                item {
                    TopSearchBar(
                        onClickBack = onNavigateBack,
                        navigateToProfile = navigateToProfile,
                        viewModel = viewModel
                    )
                }

                // Show search results if user is searching
                if (searchQuery.isNotBlank()) {
                    if (filteredUsers.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Strings.search_results,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = filteredUsers.size.toString() + Strings.found_suffix,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                        items(filteredUsers) { user ->
                            SearchResultItem(
                                user = user,
                                onUserClick = {
                                    // Handle user click - could navigate to profile
                                    println("Clicked on user: ${user.fullname}")
                                },
                                onFollowClick = { userId ->
                                    viewModel.onFollowClick(userId)
                                },
                                isFollowing = uiState.followingUsers.contains(user.email)
                            )
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
                                        text = Strings.loading_more_users,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    } else if (uiState.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = Strings.searching_users,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = Strings.no_users_found,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = Strings.try_searching_different_keywords_short,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Show normal content when not searching
                    // Check if both suggested users lists are empty
                    val hasNoSuggestions = uiState.suggestedUsers.isEmpty() && suggestedUsers.isEmpty()
                    
                    if (hasNoSuggestions) {
                        // Show beautiful empty state
                        item {
                            EmptyFollowSuggestionsState()
                        }
                    } else {
                        item {
                            PeopleToFollowSection(
                                suggestedUsers = uiState.suggestedUsers,
                                followingUsers = uiState.followingUsers,
                                onFollowClick = { userId ->
                                    viewModel.onFollowClick(userId)
                                },
                                onSeeAllClick = {
                                    viewModel.pageSize = 30
                                    scope.launch {
                                        viewModel.loadSuggestedUsers(getEmail())
                                            .collectLatest { suggestedUsersData ->
                                                println("Suggested Users Data: $suggestedUsersData")
                                                if (suggestedUsersData.isNotEmpty()) {
                                                    saveSuggestedUsers(
                                                        suggestedUsersData
                                                    )
                                                    navigateToSeeAllScreen()
                                                }
                                            }
                                    }
                                },
                                onRemoveUser = { userId ->
                                    viewModel.onRemoveUser(userId)
                                },
                                onImageClick = { userId -> navigateToViewProfile(userId) }
                            )
                        }

                        // Dynamically create TrendingSection items for each category
                        items(categorizedPosts.entries.toList()) { (category, posts) ->
                            if (posts.isNotEmpty()) {
                                TrendingSection(
                                    sectionTitle = category,
                                    posts = posts,
                                    onClickTrendingPost = { postId -> navigateToPost(postId) }
                                )
                            }
                        }

                        if (suggestedUsers.isNotEmpty()) {
                            item {
                                Spacer(Modifier.padding(horizontal = 16.dp, vertical = 16.dp))
                                Text(
                                    text = Strings.more_follow_suggestions,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            // More Suggestions List
                            items(suggestedUsers) { suggestion ->
                                SearchResultItem(
                                    user = suggestion,
                                    onUserClick = {
                                        // Handle user click - could navigate to profile
                                        println("Clicked on user: ${suggestion.fullname}")
                                        navigateToViewProfile(suggestion.id)
                                    },
                                    onFollowClick = { userId ->
                                        viewModel.onFollowClick(userId)
                                    },
                                    isFollowing = uiState.followingUsers.contains(suggestion.email)
                                )
                            }
                        }
                    }
                    
                    // Show trending posts if available (even when no suggestions)
                    if (hasNoSuggestions && categorizedPosts.isNotEmpty()) {
                        items(categorizedPosts.entries.toList()) { (category, posts) ->
                            if (posts.isNotEmpty()) {
                                TrendingSection(
                                    sectionTitle = category,
                                    posts = posts,
                                    onClickTrendingPost = { postId -> navigateToPost(postId) }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.padding(bottom = 120.dp))
                }
            }
        }
    }

    // Load more users when search results are getting low
    LaunchedEffect(filteredUsers.size, uiState.allUsers.size) {
        if (searchQuery.isNotBlank() && filteredUsers.size < 10 && uiState.hasMorePages && !uiState.isLoadingMore) {
            viewModel.loadMoreUsers(limit = 200)
        }
    }
}

// --- Screen Sections ---
@Composable
fun TopSearchBar(
    onClickBack: () -> Unit,
    viewModel: DashboardViewModel,
    navigateToProfile: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var searchBarVisible by mutableStateOf(false)
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onClickBack() }) {
            Icon(
                imageVector = vectorResource(Resources.Icon.BackArrow),
                contentDescription = Strings.back,
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text(Strings.search_placeholder, color = Color.Gray) },
            leadingIcon = {
                Icon(
                    vectorResource(Resources.Icon.Search),
                    contentDescription = Strings.search_cd,
                    tint = Color.Gray
                )
            },
            trailingIcon = {
                IconButton(
                    modifier = Modifier.size(14.dp),
                    onClick = {
                        if (searchQuery.isNotEmpty()) viewModel.updateSearchQuery(
                            ""
                        )
                        else searchBarVisible = false
                    }
                ) {
                    Icon(
                        painter = painterResource(Resources.Icon.Close),
                        contentDescription = Strings.close_icon_cd
                    )
                }
            },
            modifier = Modifier
                .weight(0.5f)
                .padding(end = 8.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                cursorColor = MaterialTheme.colorScheme.onPrimary,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        Spacer(Modifier.width(8.dp))
        if (getThumbnail().isEmpty()) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { navigateToProfile() }
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(
                            alpha = Alpha.HALF
                        )
                    ) //  color for the circle
                    .border(2.dp, Color.Transparent, CircleShape), //  border color
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
                modifier = Modifier.size(40.dp)
                    .clip(CircleShape)
                    .clickable { navigateToProfile() }
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    ),
                model = ImageRequest.Builder(
                    LocalPlatformContext.current
                ).data(getThumbnail())
                    .crossfade(enable = true)
                    .build(),
                contentDescription = Strings.user_profile_thumbnail,
                contentScale = ContentScale.Crop
            )

        }
    }
}

@Composable
fun TrendingSection(
    sectionTitle: String,
    posts: List<Post>,
    onClickTrendingPost: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            text = sectionTitle,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(posts) { post ->
                TrendingCard(
                    trendingPost = post,
                    onClickTrending = { onClickTrendingPost(post.id) }
                )
            }
        }
    }
}

@Composable
fun TrendingCard(trendingPost: Post, onClickTrending: () -> Unit) {
    Card(
        modifier = Modifier
            .width(240.dp)
            .height(120.dp)
            .clickable { onClickTrending() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = generateRandomBeautifulColor())
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (trendingPost.authorProfileImage!!.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(100.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(
                                alpha = Alpha.HALF
                            )
                        ), //  color for the circle
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = trendingPost.authorName.take(1).uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            } else {
                Image(
                    painter = rememberAsyncImagePainter(trendingPost.authorProfileImage),
                    contentDescription = Strings.user_profile_thumbnail,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(100.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    trendingPost.postType.toString(),
                    color = BlackLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    trendingPost.whatsNew,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (trendingPost.source.isNotEmpty()) {
                    Text(
                        trendingPost.source,
                        color = BlackLight,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(
    user: User,
    onUserClick: () -> Unit,
    onFollowClick: (String) -> Unit,
    isFollowing: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUserClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (user.thumbnail.isNotEmpty()) {
                AsyncImage(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = onUserClick)
                        .clip(CircleShape),
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(user.thumbnail)
                        .crossfade(enable = true)
                        .build(),
                    contentDescription = "${user.fullname}'s profile picture",
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = Alpha.HALF)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.fullname.take(1).uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 18.sp,
                        fontFamily = WorkSansBoldFont()
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    text = user.fullname,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = normalizeUsername(user.username),
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        OutlinedButton(
            onClick = { onFollowClick(user.email) },
            modifier = Modifier.height(32.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isFollowing) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer)
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

@Composable
fun EmptyFollowSuggestionsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon or Illustration
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = vectorResource(Resources.Icon.Search),
                contentDescription = "No suggestions",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primaryContainer
            )
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Main heading
        Text(
            text = "You're All Caught Up!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(12.dp))
        
        // Description
        Text(
            text = "You're following all the amazing people in your network. Use the search above to discover new connections.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        
        Spacer(Modifier.height(24.dp))
        
        // Decorative elements
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when (index) {
                                0 -> Color(0xFF6B73FF).copy(alpha = 0.6f)
                                1 -> Color(0xFF9F7AEA).copy(alpha = 0.6f)
                                else -> Color(0xFF4FD1C7).copy(alpha = 0.6f)
                            }
                        )
                )
            }
        }
    }
}
