package com.flipverse.dashboard

import ContentWithMessageBar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarColors
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.flipverse.dashboard.component.CommentSheetContent
import com.flipverse.dashboard.component.HomeScreenTab
import com.flipverse.dashboard.component.SwipeableTabRow
import com.flipverse.dashboard.component.TabItem
import com.flipverse.dashboard.component.FollowingTab
import com.flipverse.shared.domain.FabItem
import com.flipverse.shared.Alpha
import com.flipverse.shared.PreferencesRepository.getFullName
import com.flipverse.shared.PreferencesRepository.getThumbnail
import com.flipverse.shared.Resources
import com.flipverse.shared.WorkSansBoldFont
import com.flipverse.shared.domain.Post
import com.flipverse.shared.util.createShareManager
import com.flipverse.shared.util.getPushNotificationToken
import com.flipverse.shared.presentation.component.AutoNetworkBanner
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import rememberMessageBarState




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navigateToProfile: () -> Unit,
    navigateToSearch: () -> Unit,
    navigateToPostDetails: (String) -> Unit,
    navigateToViewProfile: (String) -> Unit,
    navigateToRecommendation: () -> Unit,
    navigateToReview: () -> Unit,
    navigateToQuote: () -> Unit,
    navigateToSeeAllScreen: () -> Unit,
    onDoubleTapChanged: ((() -> Unit) -> Unit)? = null
) {

    val viewModel = koinViewModel<DashboardViewModel>()
    // AutoNetworkBanner handles network state automatically now.
    var isNetworkAvailable by remember { mutableStateOf(true) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onAction = viewModel::onAction
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val listFollowState = rememberLazyListState()
    val messageBarState = rememberMessageBarState()
    var showCommentSheet by remember { mutableStateOf(false) }
    var commentPost by remember { mutableStateOf<Post?>(null) }
    val screenReady = viewModel.screenReady
    val followScreenReady = viewModel.followScreenReady

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var searchBarVisible by mutableStateOf(false)
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    val fabMenuItems = listOf(
        FabItem(
            "Recommendation",
            painterResource(Resources.Icon.Recommendation),
            navigateToRecommendation
        ),
        FabItem("Review", painterResource(Resources.Icon.Review), navigateToReview),
        FabItem("Quote", painterResource(Resources.Icon.Quote), navigateToQuote)
    )
    val snackbarHostState = remember { SnackbarHostState() }
    val shareManager = remember { createShareManager() }
    val systemBarPaddingValues = WindowInsets.systemBars.asPaddingValues()
    val tabTitles = listOf("For You", "Following")

    // Network connectivity is handled by AutoNetworkBanner now.

    LaunchedEffect(Unit) {
        // Use platform-agnostic push token API (works on Android & iOS)
        val token = getPushNotificationToken()
        if (token != null) {
            println("🎯 Push Token Retrieved: $token")
            // Save the token to ViewModel for processing
            viewModel.updatePushToken(token)
        } else {
            println("⚠️ No push token available")
        }

        if (isNetworkAvailable && viewModel.shouldRefreshAutomatically()) {
            viewModel.refreshPostsFeed()
        }
    }
    var selectedTabIndex by remember { mutableStateOf(0) }
    val shouldFetchMorePosts by remember {
        derivedStateOf {
            val layoutInfo =
                if (selectedTabIndex == 0) listState.layoutInfo else listFollowState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (layoutInfo.totalItemsCount == 0) {
                false
            } else {
                val lastVisibleItem = visibleItemsInfo.last()
                (lastVisibleItem.index + 1 == layoutInfo.totalItemsCount)
            }
        }
    }

    // Set up double-tap callback
    LaunchedEffect(Unit) {
        onDoubleTapChanged?.invoke {
            scope.launch {
                // Scroll to top based on selected tab
                if (selectedTabIndex == 0) {
                    listState.animateScrollToItem(0)
                } else {
                    listFollowState.animateScrollToItem(0)
                }
                // Refresh the feed
                viewModel.refreshPostsFeed()
            }
        }
    }

    val swipeableTabs = listOf(
        TabItem(
            title = tabTitles[0],
            icon = null,
            content = {
                HomeScreenTab(
                    screenReady = screenReady,
                    isFabMenuExpanded = isFabMenuExpanded,
                    listState = listState,
                    uiState = uiState,
                    onAction = onAction,
                    shareManager = shareManager,
                    navigateToPostDetails = navigateToPostDetails,
                    messageBarState = messageBarState,
                    viewModel = viewModel,
                    scope = scope,
                    navigateToSeeAllScreen = navigateToSeeAllScreen,
                    selectedTab = tabTitles[selectedTabIndex],
                    shouldFetchMorePosts = shouldFetchMorePosts,
                    onShowCommentSheet = { post ->
                        if (isNetworkAvailable) {
                            commentPost = post
                            showCommentSheet = true
                        } else {
                            messageBarState.addError("No internet connection.")
                        }
                    },
                    navigateToViewProfile = { userId-> navigateToViewProfile(userId) }
                )
            }
        ),
        TabItem(
            title = tabTitles[1],
            icon = null,
            content = {
                FollowingTab(
                    followScreenReady = followScreenReady,
                    isFabMenuExpanded = isFabMenuExpanded,
                    shareManager = shareManager,
                    viewModel = viewModel,
                    scope = scope,
                    onAction = onAction,
                    uiState = uiState,
                    selectedTab = tabTitles[selectedTabIndex],
                    listFollowState = listFollowState,
                    commentPost = commentPost,
                    showCommentSheet = showCommentSheet,
                    navigateToPostDetails = navigateToPostDetails,
                    messageBarState = messageBarState,
                    navigateToSeeAllScreen = navigateToSeeAllScreen,
                    shouldFetchMorePosts = shouldFetchMorePosts,
                    navigateToViewProfile = { userId -> navigateToViewProfile(userId) },
                    onImageClick = {userId-> navigateToViewProfile(userId)},
                    onShowCommentSheet = {post ->
                        commentPost = post
                        showCommentSheet = true
                    }
                )
            }
        )
    )

    @OptIn(ExperimentalMaterial3Api::class)
    if (showCommentSheet && commentPost != null) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showCommentSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            // Sheet content -make a CommentBox composable
            CommentSheetContent(
                post = commentPost!!,
                onDismiss = { showCommentSheet = false },
                onPost = { replyText ->
                    if (isNetworkAvailable) {
                        viewModel.onPostComment(
                            postId = commentPost!!.id,
                            replyText = replyText,
                            onSuccess = {
                                messageBarState.addSuccess("Your comment has been posted.")
                                showCommentSheet = false
                                viewModel.loadPostsFeed()
                            },
                            onError = { error -> messageBarState.addSuccess("Something went wrong...$error") }
                        )
                    } else {
                        messageBarState.addError("No internet connection.")
                    }

                },
                isPosting = uiState.isPostingComment
            )
        }
    }

    Scaffold(
        modifier = Modifier.padding(6.dp),
        snackbarHost = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = WindowInsets.navigationBars.asPaddingValues()
                            .calculateBottomPadding()
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                SnackbarHost(hostState = snackbarHostState)
            }
        },
        contentWindowInsets = WindowInsets(
            top = systemBarPaddingValues.calculateTopPadding(),
            bottom = systemBarPaddingValues.calculateBottomPadding(),
        ),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    CompositionLocalProvider(
                        LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp
                        )
                    ) {
                        SearchBar(
                            modifier = Modifier
                                .padding(horizontal = 10.dp)
                                .padding(bottom = 12.dp)
                                .fillMaxWidth()
                                .clickable { navigateToSearch() }
                                .height(50.dp),
                            inputField = {
                                SearchBarDefaults.InputField(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { navigateToSearch() },
                                    query = "",
                                    onQueryChange = { },
                                    expanded = false,
                                    onExpandedChange = {},
                                    onSearch = { navigateToSearch() },
                                    enabled = false,
                                    placeholder = {
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = vectorResource(Resources.Icon.Search),
                                                    contentDescription = null,
                                                    tint = Color.Gray,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Search",
                                                    color = Color.Gray,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    },
                                    leadingIcon = null,
//                                    trailingIcon = {
//                                        IconButton(
//                                            modifier = Modifier.size(14.dp),
//                                            onClick = {
//                                                if (searchQuery.isNotEmpty()) viewModel.updateSearchQuery(
//                                                    ""
//                                                )
//                                                else searchBarVisible = false
//                                            }
//                                        ) {
//                                            Icon(
//                                                painter = painterResource(Resources.Icon.Close),
//                                                contentDescription = "Close icon"
//                                            )
//                                        }
//                                    },
                                    colors = TextFieldColors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface, // Darker gray background
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedIndicatorColor = Color.Transparent, // No indicator line when focused
                                        unfocusedIndicatorColor = Color.Transparent, // No indicator line when unfocused
                                        disabledIndicatorColor = Color.Transparent,
                                        errorIndicatorColor = Color.Transparent,
                                        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                        cursorColor = MaterialTheme.colorScheme.onPrimary,
                                        disabledTextColor = Color.Transparent,
                                        errorTextColor = Color.Transparent,
                                        errorContainerColor = Color.Transparent,
                                        errorCursorColor = Color.Transparent,
                                        textSelectionColors = TextSelectionColors(
                                            handleColor = Color.Transparent,
                                            backgroundColor = Color.Transparent
                                        ),
                                        focusedLeadingIconColor = Color.Transparent,
                                        unfocusedLeadingIconColor = Color.Transparent,
                                        disabledLeadingIconColor = Color.Transparent,
                                        errorLeadingIconColor = Color.Transparent,
                                        focusedTrailingIconColor = Color.Gray,
                                        unfocusedTrailingIconColor = Color.Gray,
                                        disabledTrailingIconColor = Color.Gray,
                                        errorTrailingIconColor = Color.Transparent,
                                        focusedLabelColor = Color.Transparent,
                                        unfocusedLabelColor = Color.Transparent,
                                        disabledLabelColor = Color.Transparent,
                                        errorLabelColor = Color.Transparent,
                                        focusedPlaceholderColor = Color.Transparent,
                                        unfocusedPlaceholderColor = Color.Transparent,
                                        disabledPlaceholderColor = Color.Transparent,
                                        errorPlaceholderColor = Color.Transparent,
                                        focusedSupportingTextColor = Color.Transparent,
                                        unfocusedSupportingTextColor = Color.Transparent,
                                        disabledSupportingTextColor = Color.Transparent,
                                        errorSupportingTextColor = Color.Transparent,
                                        focusedPrefixColor = Color.Transparent,
                                        unfocusedPrefixColor = Color.Transparent,
                                        disabledPrefixColor = Color.Transparent,
                                        errorPrefixColor = Color.Transparent,
                                        focusedSuffixColor = Color.Transparent,
                                        unfocusedSuffixColor = Color.Transparent,
                                        disabledSuffixColor = Color.Transparent,
                                        errorSuffixColor = Color.Transparent,
                                    )
                                )
                            },
                            colors = SearchBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                dividerColor = Color.Transparent
                            ),
                            tonalElevation = 0.dp,
                            windowInsets = WindowInsets(0.dp),
                            shape = MaterialTheme.shapes.medium,
                            expanded = false,
                            onExpandedChange = {},
                            content = {}
                        )
                    }
                },
                navigationIcon = {
                    Box(
                        contentAlignment = Alignment.CenterStart

                    ) {
                        Image(
                            painter = painterResource(if (isSystemInDarkTheme()) Resources.Image.AppLogoDark else Resources.Image.AppLogoWhite),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .wrapContentSize()
                                .height(40.dp),

                            )
                    }

                },
                actions = {
                    // Profile image with loading placeholder and error fallback
                    val thumbnailUrl = getThumbnail()
                    val userInitials = getFullName().take(1).uppercase()
                    val hasValidThumbnail = thumbnailUrl.isNotBlank() && 
                        (thumbnailUrl.startsWith("http://") || thumbnailUrl.startsWith("https://"))
                    
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { navigateToProfile() }
                            .border(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasValidThumbnail) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalPlatformContext.current)
                                    .data(thumbnailUrl)
                                    .crossfade(enable = true)
                                    .build(),
                                contentDescription = com.flipverse.shared.Strings.user_profile_thumbnail,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            ) { 
                                when (val state = painter.state.collectAsState().value) {
                                    is coil3.compose.AsyncImagePainter.State.Success -> {
                                        SubcomposeAsyncImageContent()
                                    }
                                    else -> {
                                        // Show initials placeholder while loading or on error
                                        ProfileInitialsPlaceholder(userInitials)
                                    }
                                }
                            }
                        } else {
                            ProfileInitialsPlaceholder(userInitials)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        floatingActionButton = {
            // Floating Action Button with expand/collapse functionality
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(
                    bottom = 100.dp,
                    end = 0.dp,
                ) // Position above bottom bar

            ) {
                AnimatedVisibility(
                    visible = isFabMenuExpanded,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        fabMenuItems.forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = item.text,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .clickable { item.onClick() }
                                        .background(
                                            MaterialTheme.colorScheme.surface,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                IconButton(
                                    onClick = item.onClick,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.onPrimaryContainer)
                                ) {
                                    Icon(
                                        painter = item.icon,
                                        contentDescription = item.text,
                                        tint = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Main FAB
                IconButton(
                    onClick = { isFabMenuExpanded = !isFabMenuExpanded },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer)
                ) {
                    Icon(
                        imageVector = if (isFabMenuExpanded) vectorResource(Resources.Icon.Close) else vectorResource(
                            Resources.Icon.Add
                        ),
                        contentDescription = if (isFabMenuExpanded) "Close" else "Expand",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.primary // Main content background
    ) { paddingValues ->
        ContentWithMessageBar(
            contentBackgroundColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding()
                ),
            messageBarState = messageBarState,
            fontFamily = FontFamily.SansSerif,
            errorMaxLines = 2,
            errorContainerColor = MaterialTheme.colorScheme.error,
            errorContentColor = MaterialTheme.colorScheme.onErrorContainer,
            successContainerColor = MaterialTheme.colorScheme.onSecondaryContainer,
            successContentColor = MaterialTheme.colorScheme.onPrimary,
            showCopyButton = false
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary)
                    .blur(if (isFabMenuExpanded) 12.dp else 0.dp)
            ) {
                AutoNetworkBanner(
                    modifier = Modifier.fillMaxWidth(),
                    onConnectivityChanged = { connected ->
                        isNetworkAvailable = connected
                        if (connected) {
                            viewModel.refreshPostsFeed()
                        }
                    },
                    onRetry = {
                        viewModel.refreshPostsFeed()
                    }
                )

                SwipeableTabRow(
                    tabs = swipeableTabs,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { idx ->
                        selectedTabIndex = idx
                        if (!isNetworkAvailable) {
                            messageBarState.addError("No internet connection.")
                        } else {
                            if (idx == 0) viewModel.loadPostsFeed() else viewModel.ensureFollowingFeedLoaded()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    indicatorColor = MaterialTheme.colorScheme.onPrimary
                )
                // Render the selected tab's actual content composable
                swipeableTabs[selectedTabIndex].content.invoke()
            }
        }
    }
}

/**
 * Displays user initials placeholder for profile image
 */
@Composable
private fun ProfileInitialsPlaceholder(initials: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 16.sp,
            fontFamily = WorkSansBoldFont()
        )
    }
}

