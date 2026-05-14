package com.flipverse.dashboard.component

import MessageBarState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flipverse.dashboard.DashboardAction
import com.flipverse.dashboard.DashboardViewModel
import com.flipverse.dashboard.PostsFeedState
import com.flipverse.dashboard.SuggestionItem
import com.flipverse.data.util.normalizeUsername
import com.flipverse.shared.Alpha
import com.flipverse.shared.BlackLight
import com.flipverse.shared.Constants
import com.flipverse.shared.DisplayResult
import com.flipverse.shared.PreferencesRepository.getEmail
import com.flipverse.shared.PreferencesRepository.saveSuggestedUsers
import com.flipverse.shared.RequestState
import com.flipverse.shared.Resources
import com.flipverse.shared.Strings
import com.flipverse.shared.domain.Post
import com.flipverse.shared.presentation.component.InfoCardWithRetry
import com.flipverse.shared.presentation.component.LoadingCard
import com.flipverse.shared.util.ShareManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.reflect.KFunction1


@Composable
fun FollowingTab(
    followScreenReady: RequestState<Unit>,
    isFabMenuExpanded: Boolean,
    shareManager: ShareManager,
    viewModel: DashboardViewModel,
    scope: CoroutineScope,
    onAction: KFunction1<DashboardAction, Unit>,
    uiState: PostsFeedState,
    selectedTab: String,
    listFollowState: LazyListState,
    commentPost: Post?,
    showCommentSheet: Boolean,
    navigateToPostDetails: (String) -> Unit,
    onImageClick: (String) -> Unit,
    navigateToViewProfile: (String) -> Unit,
    messageBarState: MessageBarState,
    navigateToSeeAllScreen: () -> Unit,
    shouldFetchMorePosts: Boolean,
    onShowCommentSheet: (Post) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var commentPost1 = commentPost
    var showCommentSheet1 = showCommentSheet
    followScreenReady.DisplayResult(
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    // Suggested Users Feed
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 16.dp)
                    ) {

                        // Invite friends Section
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary) // Card-like background
                                .padding(12.dp)
                                .clickable { uriHandler.openUri(Constants.INVITE_LANDING_URL) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Color(0xFF74C0FC)
                                        ) //  color for the circle
                                        .border(
                                            2.dp,
                                            Color.Transparent,
                                            CircleShape
                                        ), //  border color
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

                                Spacer(modifier = Modifier.height(4.dp))

//                                Text(
//                                    text = Constants.INVITE_LANDING_URL,
//                                    color = MaterialTheme.colorScheme.primaryContainer,
//                                    fontSize = 12.sp,
//                                    textDecoration = TextDecoration.Underline,
//                                    modifier = Modifier.clickable {
//                                        uriHandler.openUri(Constants.INVITE_LANDING_URL)
//                                    }
//                                )
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

                        var isLoadingSuggestions by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PEOPLE TO FOLLOW",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )

                            FilledTonalIconButton(
                                modifier = Modifier.padding(bottom = 8.dp),
                                onClick = {
                                    viewModel.pageSize = 30
                                    scope.launch {
                                        isLoadingSuggestions = true
                                        try {
                                            viewModel.loadSuggestedUsers(getEmail())
                                                .collectLatest { suggestedUsersData ->
                                                    println("Suggested Users Data: $suggestedUsersData")
                                                    if (suggestedUsersData.isNotEmpty()) {
                                                        onAction(
                                                            DashboardAction.OnRefreshSuggestedUsersClick(
                                                                suggestedUsersData
                                                            )
                                                        )
                                                    }
                                                    isLoadingSuggestions = false
                                                }
                                        } catch (e: Exception) {
                                            isLoadingSuggestions = false
                                        }
                                    }
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color.Transparent
                                ),
                                enabled = !isLoadingSuggestions
                            ) {
                                if (isLoadingSuggestions) {
                                    AdaptiveCircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(Alpha.HALF)
                                    )
                                } else {
                                    Icon(
                                        vectorResource(Resources.Icon.Refresh),
                                        contentDescription = "Refresh List of Suggested People",
                                        tint = MaterialTheme.colorScheme.onPrimary.copy(Alpha.HALF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Suggestions List
                        LazyColumn(
                            userScrollEnabled = true
                        ) {
                            items(uiState.suggestedUsers) { suggestion ->
                                SuggestionItem(
                                    suggestion = suggestion,
                                    onFollowClick = { userId ->
                                        viewModel.onFollowClick(userId)
                                    },
                                    isFollowing = uiState.followingUsers.contains(
                                        suggestion.email
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }

        },
        onError = { message ->
            InfoCardWithRetry(
                image = Resources.Image.AppLogoFullOutlineDark,
                title = "Oops!",
                subtitle = message,
                buttonText = "Try Again",
                onRetry = {
                    if (selectedTab == "For You") {
                        viewModel.loadPostsFeed()
                    } else {
                        viewModel.loadFollowingFeed()
                    }
                }
            )
        },
        onSuccess = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp)
                    .background(MaterialTheme.colorScheme.primary)
                    .blur(if (isFabMenuExpanded) 12.dp else 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    // Posts Feed
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        state = listFollowState,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp)
                    ) {
                        println("LazyColumn initialized Size:" + uiState.posts.size)
                        println("ui StatesMorePages" + uiState.hasMorePages)
                        println("ui StatesIsLoadingMore" + uiState.isLoadingMore)

                        itemsIndexed(uiState.posts) { index, post ->

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
                                    val content =
                                        post.whatsNew + "\n" + "\n" + "Source: " + post.source + "\n" + "Tags:" + normalizedTags.joinToString(
                                            ", "
                                        )
                                    shareManager.shareInviteLink(
                                        inviteLink = content,
                                        title = "Share your Post"
                                    )
                                    onAction(
                                        DashboardAction.OnPostShareClick(
                                            post.id
                                        )
                                    )
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
                                    onImageClick(userId)
                                },
                                onBlockAuthor = { authorId, onComplete ->
                                    viewModel.blockAuthor(authorId, onComplete)
                                },
                                onReportPost = { postId, authorId, onComplete ->
                                    viewModel.reportPost(postId, authorId, onComplete)
                                },
                            )
//                                    Show people to follow after the first 2 posts (index 1 since 0-based)
                            if (uiState.suggestedUsers.isNotEmpty() && index == 2) {

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
                                    onImageClick = { userId -> navigateToViewProfile(userId) }
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(4.dp),
                                thickness = 1.dp,
                                color = Color.Gray.copy(alpha = Alpha.DISABLED)
                            )
                        }

                        println("ui States Post Size:" + uiState.posts.size)
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
                                println("shouldLoadMoreBoolean: $shouldFetchMorePosts")
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
                                        animationSpec = tween(durationMillis = 500), label = "iconScale"
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
