package com.flipverse.chat


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.flipverse.dashboard.DashboardViewModel
import com.flipverse.data.util.capitalizeFirstLetter
import com.flipverse.data.util.formatTimestamp
import com.flipverse.data.util.normalizeUsername
import com.flipverse.livebook.LiveBookViewModel
import com.flipverse.shared.Alpha
import com.flipverse.shared.BlackLight
import com.flipverse.shared.BrandOrange
import com.flipverse.shared.PreferencesRepository
import com.flipverse.shared.Resources
import com.flipverse.shared.Strings
import com.flipverse.shared.White
import com.flipverse.shared.domain.LiveBook
import com.flipverse.shared.domain.Post
import com.flipverse.shared.domain.User
import com.flipverse.shared.presentation.component.FlipExpandableText
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewProfileScreen(
    userId: String,
    navigateBack: () -> Unit,
    navigateToSendMessage: () -> Unit,
    navigateToPostDetails: (String) -> Unit,
    navigateToViewLiveBook: (String) -> Unit
) {
    val dashboardViewModel = koinViewModel<DashboardViewModel>()
    val liveBookViewModel = koinViewModel<LiveBookViewModel>()

    // Collect states
    val dashboardState by dashboardViewModel.uiState.collectAsState()
    val liveBookState by liveBookViewModel.uiState.collectAsState()

    // Loading states for each tab
    var isPostsRefreshing by remember { mutableStateOf(false) }
    var isLiveBooksRefreshing by remember { mutableStateOf(false) }
    var isBookmarksRefreshing by remember { mutableStateOf(false) }
    var isFollowingRefreshing by remember { mutableStateOf(false) }
    var isFollowersRefreshing by remember { mutableStateOf(false) }

    // Trigger data loading
    LaunchedEffect(userId) {
        println("ViewProfileScreen: userId = $userId")

        // Clear previous user's livebooks immediately to prevent showing stale count
        liveBookViewModel.clearUserLiveBooks()

        dashboardViewModel.viewProfile(userId)
        dashboardViewModel.getUserPosts(userId)
        dashboardViewModel.getUserFollowing(userId)
        dashboardViewModel.getUserFollowers(userId)
        dashboardViewModel.getUserLikedPosts(userId)

        // Fetch livebooks for the new user
        liveBookViewModel.getUserLiveBooks(userId)

        // Load current user's following list to check if already following this user
        dashboardViewModel.loadCurrentUserFollowing()
    }

    val pagerState = rememberPagerState(pageCount = { 5 })

    // Refresh data when tab changes (either by click or swipe)
    LaunchedEffect(pagerState.currentPage) {
        when (pagerState.currentPage) {
            0 -> {
                // Posts tab - refresh user posts
                isPostsRefreshing = true
                dashboardViewModel.getUserPosts(userId)
                isPostsRefreshing = false
            }

            1 -> {
                // LiveBook tab - refresh user livebooks
                isLiveBooksRefreshing = true
                liveBookViewModel.getUserLiveBooks(userId)
                isLiveBooksRefreshing = false
            }

            2 -> {
                // Bookmark tab - refresh user liked posts
                isBookmarksRefreshing = true
                dashboardViewModel.getUserLikedPosts(userId)
                isBookmarksRefreshing = false
            }

            3 -> {
                // Following tab - refresh user following
                isFollowingRefreshing = true
                dashboardViewModel.getUserFollowing(userId)
                isFollowingRefreshing = false
            }
            4 -> {
                // Followers tab - refresh user followers
                isFollowersRefreshing = true
                dashboardViewModel.getUserFollowers(userId)
                isFollowersRefreshing = false
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (userId == PreferencesRepository.getEmail()) "Your Profile" else "Profile",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
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
                actions = {
//                    IconButton(onClick = { }) {
//                        Icon(
//                            imageVector = vectorResource(Resources.Icon.VerticalMenu),
//                            contentDescription = Strings.settings,
//                            tint = MaterialTheme.colorScheme.onPrimary
//                        )
//                    }
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
            // Profile Header
            ProfileHeader(
                user = dashboardState.profileDetails,
                navigateToSendMessage = navigateToSendMessage,
                postsCount = dashboardState.userPosts.size,
                followersCount = dashboardState.followersCount,
                liveBooksCount = liveBookState.userLiveBooks.size,
                followingUsers = dashboardState.followingUsers,
                onFollowClick = { id -> dashboardViewModel.onFollowClick(id.toString()) },
                isOwnProfile = userId == PreferencesRepository.getEmail()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tabs
            val tabs = listOf("Posts", "LiveBook", "Bookmark", "Following", "Followers")
            val coroutineScope = rememberCoroutineScope()
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    if (tabPositions.isNotEmpty()) {
                        SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { coroutineScope.launch { pagerState.scrollToPage(index) } },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = title,
                                    color = if (pagerState.currentPage == index) MaterialTheme.colorScheme.onPrimary else Color.Gray,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // Show loading indicator when refreshing
                                val isRefreshing = when (index) {
                                    0 -> isPostsRefreshing
                                    1 -> isLiveBooksRefreshing
                                    2 -> isBookmarksRefreshing
                                    3 -> isFollowingRefreshing
                                    4 -> isFollowersRefreshing
                                    else -> false
                                }

                                if (isRefreshing) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    AdaptiveCircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 1.dp
                                    )
                                }
                            }
                        },
                        selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                        unselectedContentColor = Color.Gray
                    )
                }
            }

            HorizontalDivider(thickness = 1.dp, color = Color.Gray.copy(alpha = Alpha.DISABLED))
            Spacer(modifier = Modifier.height(16.dp))

            // Content based on selected tab
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
            ) { page ->
                Page(
                    page = page,
                    posts = dashboardState.userPosts,
                    likedPosts = dashboardState.userLikedPosts,
                    followingUsers = dashboardState.userFollowing,
                    followersUsers = dashboardState.userFollowers,
                    liveBooks = liveBookState.userLiveBooks,
                    isLoading = when (page) {
                        0 -> dashboardState.isLoading || isPostsRefreshing
                        1 -> liveBookState.isLoading || isLiveBooksRefreshing
                        2 -> dashboardState.isLoading || isBookmarksRefreshing
                        3 -> dashboardState.isLoading || isFollowingRefreshing
                        4 -> dashboardState.isLoading || isFollowersRefreshing
                        else -> false
                    },
                    navigateToPostDetails = { postId -> navigateToPostDetails(postId) },
                    navigateToViewLiveBook = { liveBookId -> navigateToViewLiveBook(liveBookId)}
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    user: User?,
    navigateToSendMessage: () -> Unit,
    postsCount: Int,
    followersCount: Int,
    liveBooksCount: Int,
    followingUsers: Set<String>,
    onFollowClick: (String?) -> Unit,
    isOwnProfile: Boolean
) {

    // Check if current user is following this profile user
    val isFollowing = user?.email?.let { email ->
        followingUsers.contains(email)
    } ?: false
    println("isFollowing:$isFollowing")

    var showZoomedImage by remember { mutableStateOf(false) }
    var zoomedImage by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = user?.fullname ?: Strings.unknown_user,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            user?.let {
                Text(
                    text = normalizeUsername(it.username),
                    color = Color.Gray,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }

        UserAvatar(user = user, size = 64.dp, onAvatarClick = { url ->
            zoomedImage = url
            showZoomedImage = true
        })
    }

    Spacer(modifier = Modifier.height(16.dp))

    Spacer(modifier = Modifier.height(16.dp))

    // Bio section - only show if user has a bio or show placeholder
    if (!user?.bio.isNullOrEmpty()) {
        Text(
            text = user?.bio?.take(100) ?: "",
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
    } else {
        Text(
            text = Strings.no_bio_yet,
            color = Color.Gray,
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    Text(
        text = "$followersCount followers • $postsCount posts • $liveBooksCount livebooks",
        color = MaterialTheme.colorScheme.onPrimary,
        fontSize = 14.sp
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Buttons: Follow and Send Message
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (!isOwnProfile) {
            Button(
                onClick = { onFollowClick(user?.email) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing)
                        MaterialTheme.colorScheme.surface
                    else
                        MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .then(
                        if (isFollowing) {
                            Modifier.border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = Alpha.HALF),
                                shape = RoundedCornerShape(8.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
            ) {
                Text(
                    text = if (isFollowing) Strings.following else Strings.follow,
                    color = if (isFollowing)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        BlackLight,
                    fontSize = 16.sp
                )
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.width(16.dp))

        if (!isOwnProfile) {
            IconButton(
                onClick = { navigateToSendMessage() },
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp)
                    ),
                colors = IconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = Color.Unspecified,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContentColor = Color.Unspecified
                )
            ) {
                Icon(
                    imageVector = vectorResource(Resources.Icon.SendMessageTilted),
                    contentDescription = Strings.send_message,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }

    if (showZoomedImage) {
        ZoomableImageDialog(
            url = zoomedImage,
            onDismiss = { showZoomedImage = false }
        )
    }
}

@Composable
private fun UserAvatar(user: User?, size: Dp, onAvatarClick: (String?) -> Unit) {
    if (user?.thumbnail?.isNotEmpty() == true) {
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(user.thumbnail)
                .crossfade(true)
                .build(),
            contentDescription = Strings.profile_picture,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .clickable { onAvatarClick(user.thumbnail) },
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = Alpha.HALF)
                )
                .border(2.dp, Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user?.fullname?.take(1)?.uppercase() ?: "U",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = (size.value / 4).sp,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

@Composable
private fun ZoomableImageDialog(url: String?, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black)
        ) {
            // Background clickable area (behind the image) to dismiss
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
            )

            // Zoomable Image
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = Strings.profile_picture,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)

                            if (scale > 1f) {
                                val maxX = (size.width * (scale - 1)) / 2
                                val maxY = (size.height * (scale - 1)) / 2
                                offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                                offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    ),
                contentScale = ContentScale.Fit
            )

            // Close button
            IconButton(
                onClick = { onDismiss() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = vectorResource(Resources.Icon.Close),
                    contentDescription = "Close",
                    tint =  White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Instructions text
            Text(
                text = "Pinch to zoom • Tap ✕ to close",
                color = White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun Page(
    page: Int,
    posts: List<Post> = emptyList(),
    likedPosts: List<Post> = emptyList(),
    followingUsers: List<User> = emptyList(),
    followersUsers: List<User> = emptyList(),
    liveBooks: List<LiveBook> = emptyList(),
    isLoading: Boolean = false,
    navigateToPostDetails: (String) -> Unit,
    navigateToViewLiveBook: (String) -> Unit
) {
    when (page) {
        0 -> PostsTabContent(
            posts = posts,
            isLoading = isLoading,
            navigateToPostDetails = { postId -> navigateToPostDetails(postId) }
        )
        1 -> LiveBookTabContent(
            liveBooks = liveBooks,
            isLoading = isLoading,
            navigateToViewLiveBook = {id -> navigateToViewLiveBook(id)}
        )
        2 -> LikesTabContent(
            likedPosts = likedPosts,
            isLoading = isLoading,
            navigateToPostDetails = { postId-> navigateToPostDetails(postId) }
        )
        3 -> FollowingTabContent(followingUsers = followingUsers, isLoading = isLoading)
        4 -> FollowersTabContent(followersUsers = followersUsers, isLoading = isLoading)
    }
}

@Composable
private fun PostsTabContent(
    posts: List<Post>,
    isLoading: Boolean,
    navigateToPostDetails: (String) -> Unit
) {
    if (isLoading) {
        LoadingContent()
    } else if (posts.isEmpty()) {
        EmptyContent("No posts yet")
    } else {
        LazyColumn {
            items(posts) { post ->
                PostItem(post = post, navigateToPostDetails = {postId -> navigateToPostDetails(postId)})
            }

            item {
                Spacer(modifier = Modifier.padding(84.dp))
            }
        }
    }
}

@Composable
private fun LiveBookTabContent(
    liveBooks: List<LiveBook>,
    isLoading: Boolean,
    navigateToViewLiveBook: (String) -> Unit
) {
    if (isLoading) {
        LoadingContent()
    } else if (liveBooks.isEmpty()) {
        EmptyContent("No LiveBooks yet")
    } else {
        LazyColumn {
            items(liveBooks) { liveBook ->
                LiveBookItem(liveBook = liveBook, navigateToViewLiveBook = {id->navigateToViewLiveBook(id)})
            }
            item {
                Spacer(modifier = Modifier.padding(84.dp))
            }
        }
    }
}

@Composable
private fun LikesTabContent(
    likedPosts: List<Post>,
    isLoading: Boolean,
    navigateToPostDetails: (String) -> Unit
) {
    if (isLoading) {
        LoadingContent()
    } else if (likedPosts.isEmpty()) {
        EmptyContent("No bookmarked posts yet")
    } else {
        LazyColumn {
            items(likedPosts) { post ->
                PostItem(post = post, navigateToPostDetails = navigateToPostDetails)
            }
        }
    }
}

@Composable
private fun FollowingTabContent(followingUsers: List<User>, isLoading: Boolean) {
    if (isLoading) {
        LoadingContent()
    } else if (followingUsers.isEmpty()) {
        EmptyContent("No following users yet")
    } else {
        LazyColumn {
            items(followingUsers) { user ->
                FollowingItem(user = user)
            }
            item {
                Spacer(modifier = Modifier.padding(84.dp))
            }
        }
    }
}

@Composable
private fun FollowersTabContent(followersUsers: List<User>, isLoading: Boolean) {
    if (isLoading) {
        LoadingContent()
    } else if (followersUsers.isEmpty()) {
        EmptyContent("No followers yet")
    } else {
        LazyColumn {
            items(followersUsers) { user ->
                FollowingItem(user = user)
            }
            item {
                Spacer(modifier = Modifier.padding(84.dp))
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        AdaptiveCircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 1.dp
        )
    }
}

@Composable
private fun EmptyContent(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color.Gray,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun PostItem(post: Post, navigateToPostDetails: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { navigateToPostDetails(post.id) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            val normalizedTags = post.tags.map { normalizeUsername(it) }
            val content =
                post.whatsNew + "\n" + "\n" + "Source: " + post.source + "\n" + "Tags: " + normalizedTags.joinToString(
                    ", "
                )

            Text(
                content
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(post.timestamp),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "${post.engagement.likesCount} likes",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = post.postType.name.capitalizeFirstLetter(),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun LiveBookItem(liveBook: LiveBook, navigateToViewLiveBook: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { navigateToViewLiveBook(liveBook.id) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = liveBook.title,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = liveBook.genre,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = Alpha.HALF),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = liveBook.status.capitalizeFirstLetter(),
                    color = when (liveBook.status) {
                        "active" -> BrandOrange
                        "completed" -> MaterialTheme.colorScheme.onPrimary.copy(alpha = Alpha.HALF)
                        else -> Color.Gray
                    },
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "${liveBook.totalLikes} likes",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun FollowingItem(user: User) {
    var showZoomedImage by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(user = user, size = 48.dp, onAvatarClick = { url ->
                if (!url.isNullOrEmpty()) {
                    showZoomedImage = true
                }
            })
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.fullname,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = normalizeUsername(user.username),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                val bio = user.bio
                if (!bio.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = bio,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = Alpha.HALF),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${user.followersCount}",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "followers",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }

    if (showZoomedImage) {
        ZoomableImageDialog(
            url = user.thumbnail,
            onDismiss = { showZoomedImage = false }
        )
    }
}
