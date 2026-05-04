package com.flipverse.dashboard.component

import MessageBarState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.scale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Icon
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.flipverse.dashboard.DashboardAction
import com.flipverse.dashboard.DashboardViewModel
import com.flipverse.dashboard.PostsFeedState
import com.flipverse.data.util.normalizeUsername
import com.flipverse.shared.Alpha
import com.flipverse.shared.Constants
import com.flipverse.shared.DisplayResult
import com.flipverse.shared.PreferencesRepository.getEmail
import com.flipverse.shared.PreferencesRepository.saveSuggestedUsers
import com.flipverse.shared.RequestState
import com.flipverse.shared.Resources
import com.flipverse.shared.domain.Post
import com.flipverse.shared.presentation.component.InfoCardWithRetry
import com.flipverse.shared.presentation.component.LoadingCard
import com.flipverse.shared.util.ShareManager
import com.flipverse.shared.Strings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.reflect.KFunction1

@Composable
fun HomeScreenTab(
    screenReady: RequestState<Unit>,
    isFabMenuExpanded: Boolean,
    listState: LazyListState,
    uiState: PostsFeedState,
    onAction: KFunction1<DashboardAction, Unit>,
    shareManager: ShareManager,
    navigateToPostDetails: (String) -> Unit,
    navigateToViewProfile: (String) -> Unit,
    messageBarState: MessageBarState,
    viewModel: DashboardViewModel,
    scope: CoroutineScope,
    navigateToSeeAllScreen: () -> Unit,
    selectedTab: String,
    shouldFetchMorePosts: Boolean,
    onShowCommentSheet: (Post) -> Unit
) {
    screenReady.DisplayResult(
        onLoading = {
            LoadingCard(modifier = Modifier.fillMaxSize())
        },
        onIdle = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary)
                    .blur(if (isFabMenuExpanded) 12.dp else 0.dp)
            ) {
                // Horizontal scrollable chips
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    // Posts Feed
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {

                        // Display posts with people to follow after the first 4 posts
                        itemsIndexed(uiState.posts, key = { _, post -> post.id }) { index, post ->
                            PostCard(
                                post = post,
                                onLikeClick = {
                                    onAction(
                                        DashboardAction.OnPostLikeClick(
                                            post.id
                                        )
                                    )
                                },
                                onCommentClick = {
                                    onShowCommentSheet(post)
                                },
                                onShareClick = {
                                    val normalizedTags = post.tags.map { normalizeUsername(it) }
                                    val content = buildString {
                                        append(post.whatsNew)
                                        append("\n\n")
                                        if (post.source.isNotBlank()) {
                                            append("Source: ${post.source}\n")
                                        }
                                        if (normalizedTags.isNotEmpty()) {
                                            append("Tags: ${normalizedTags.joinToString(", ")}")
                                        }
                                        append("\n\n")
                                        append("— — — — — — — — — — — — —\n")
                                        append("${Constants.INVITE_LANDING_URL}\n")
                                        append("✨ Download Flipverse — Rediscover Meaningful Engagement!")
                                    }
                                    shareManager.shareInviteLink(
                                        inviteLink = content,
                                        title = Strings.share_your_post
                                    )
                                    onAction(DashboardAction.OnPostShareClick(post.id))
                                },
                                onClickPost = { navigateToPostDetails(post.id) },
                                messageBarState = messageBarState,
                                onPinPost = { id, isPinned ->
                                    viewModel.pinPostOnClick(
                                        id,
                                        isPinned
                                    )
                                },
                                onClickDeletePost = { id -> viewModel.deletePost(id) },
                                onClickHidePost = { id -> viewModel.hidePost(id) },
                                onImageClick = { userId -> navigateToViewProfile(userId) },
                            )

                            // Show people to follow after the first 2 posts (index 1 since 0-based)
                            if (index == 1) {

                                HorizontalDivider(
                                    modifier = Modifier.padding(4.dp),
                                    thickness = 1.dp,
                                    color = Color.Gray.copy(alpha = Alpha.DISABLED)
                                )

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
                                    onImageClick = { userId ->
                                        println("Image clicked for user: $userId")
                                        navigateToViewProfile(userId)
                                    }
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(4.dp),
                                thickness = 1.dp,
                                color = Color.Gray.copy(alpha = Alpha.DISABLED)
                            )
                        }

                        // Loading more indicator
                        if (uiState.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LoadingCard(modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }

                        // End of list indicator
                        if (!uiState.hasMorePages && uiState.posts.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    var isVisible by remember { mutableStateOf(false) }
                                    LaunchedEffect(Unit) { isVisible = true }
                                    val scale by animateFloatAsState(
                                        targetValue = if (isVisible) 1f else 0f,
                                        animationSpec = tween(durationMillis = 500),
                                        label = "iconScale"
                                    )
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(Resources.Icon.sent),
                                            contentDescription = "All caught up",
                                            modifier = Modifier
                                                .size(24.dp)
                                                .scale(scale),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.padding(4.dp))
                                        Text(
                                            text = Strings.youre_all_caught_up,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                        }

                    }

                }
            }

        },
        onError = { message ->
            InfoCardWithRetry(
                image = Resources.Image.AppLogoFullOutlineDark,
                title = Strings.oops,
                subtitle = message,
                buttonText = Strings.try_again,
                onRetry = {
                    // Call your reload/refresh method, for example:
                    if (selectedTab == "For You") {
                        screenReady.isLoading()
                        viewModel.loadPostsFeed()
                        screenReady.isSuccess()
                    } else {
                        screenReady.isLoading()
                        viewModel.loadFollowingFeed()
                        screenReady.isSuccess()
                    }
                }
            )
        },
        onSuccess = {
//                                Box(
//                                    modifier = Modifier
//                                        .fillMaxSize()
//                                        .background(MaterialTheme.colorScheme.primary)
//                                        .blur(if (isFabMenuExpanded) 12.dp else 0.dp)
//                                )
            Column(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary)
                    .blur(if (isFabMenuExpanded) 12.dp else 0.dp),
//                                isRefreshing = uiState.isRefreshing,
//                                onRefresh = {
//                                    scope.launch {
//                                        println("🚀 onRefresh called - setting isRefreshing = true")
//                                        uiState.isRefreshing = true
//
//                                        // Simulate network call
//                                        delay(2000)
//
//                                        // 🔥 CRITICAL: Always set isRefreshing = false at the end
//                                        println("✅ Refresh complete - setting isRefreshing = false")
//                                        uiState.isRefreshing = false
//                                    }
//
//                                },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {

                    // Posts Feed
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        state = listState,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp)
                    ) {
                        itemsIndexed(uiState.posts, key = { _, post -> post.id }) { index, post ->
                            PostCard(
                                post = post,
                                onLikeClick = {
                                    onAction(
                                        DashboardAction.OnPostLikeClick(
                                            post.id
                                        )
                                    )
                                },
                                onCommentClick = {
                                    onShowCommentSheet(post)
                                },
                                onShareClick = {
                                    val normalizedTags = post.tags.map { normalizeUsername(it) }
                                    val content = buildString {
                                        append(post.whatsNew)
                                        append("\n\n")
                                        if (post.source.isNotBlank()) {
                                            append("Source: ${post.source}\n")
                                        }
                                        if (normalizedTags.isNotEmpty()) {
                                            append("Tags: ${normalizedTags.joinToString(", ")}")
                                        }
                                        append("\n\n")
                                        append("— — — — — — — — — — — — —\n")
                                        append("${Constants.INVITE_LANDING_URL}\n")
                                        append("✨ Download Flipverse — Rediscover Meaningful Engagement!")
                                    }
                                    shareManager.shareInviteLink(
                                        inviteLink = content,
                                        title = Strings.share_your_post
                                    )
                                    onAction(DashboardAction.OnPostShareClick(post.id))
                                },
                                onClickPost = { navigateToPostDetails(post.id) },
                                messageBarState = messageBarState,
                                onPinPost = { id, isPinned ->
                                    viewModel.pinPostOnClick(
                                        id,
                                        isPinned
                                    )
                                },
                                onClickDeletePost = { id -> viewModel.deletePost(id) },
                                onClickHidePost = { id -> viewModel.hidePost(id) },
                                onImageClick = { userId ->
                                    println("Imagey clicked for user: $userId")
                                    navigateToViewProfile(userId)
                                },
                            )
//                                    Show people to follow after the first 2 posts (index 1 since 0-based)
                            if (uiState.suggestedUsers.isNotEmpty() && index == 3) {

                                HorizontalDivider(
                                    modifier = Modifier.padding(4.dp),
                                    thickness = 1.dp,
                                    color = Color.Gray.copy(alpha = Alpha.DISABLED)
                                )

                                PeopleToFollowSection(
                                    suggestedUsers = uiState.suggestedUsers,
                                    followingUsers = uiState.followingUsers,
                                    onFollowClick = { userId ->
                                        viewModel.onFollowClick(userId)
                                    },
                                    onSeeAllClick = {
                                        viewModel.pageSize = 30
                                        scope.launch {
                                            viewModel.loadSuggestedUsers(
                                                getEmail()
                                            )
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
                                    onImageClick = { userId ->
                                        println("Imagey clicked for user: $userId")
                                        navigateToViewProfile(userId)
                                    }
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(4.dp),
                                thickness = 1.dp,
                                color = Color.Gray.copy(alpha = Alpha.DISABLED)
                            )
                        }

                        // Loading more indicator
                        if (uiState.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LoadingCard(modifier = Modifier.fillMaxWidth())
                                }
                                LaunchedEffect(key1 = shouldFetchMorePosts) {
                                    if (shouldFetchMorePosts) {
                                        viewModel.loadMoreFeedPosts()
                                    }
                                }
                            }
                        }


                        // End of list indicator
                        if (!uiState.hasMorePages && uiState.posts.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    var isVisible by remember { mutableStateOf(false) }
                                    LaunchedEffect(Unit) { isVisible = true }
                                    val scale by animateFloatAsState(
                                        targetValue = if (isVisible) 1f else 0f,
                                        animationSpec = tween(durationMillis = 500),
                                        label = "iconScale"
                                    )
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(Resources.Icon.sent),
                                            contentDescription = "All caught up",
                                            modifier = Modifier
                                                .size(32.dp)
                                                .scale(scale),
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.padding(4.dp))
                                        Text(
                                            text = Strings.youre_all_caught_up,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                    }
                }


            }


        },
        transitionSpec = fadeIn() togetherWith fadeOut()
    )
}


