package com.flipverse.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flipverse.data.domain.FeedRepository
import com.flipverse.data.domain.NomenclatureRepository
import com.flipverse.data.domain.PostRepository
import com.flipverse.data.domain.UserRepository
import com.flipverse.data.util.normalizeUsername
import com.flipverse.shared.PreferencesRepository.getEmail
import com.flipverse.shared.PreferencesRepository.getId
import com.flipverse.shared.PreferencesRepository.getUsername
import com.flipverse.shared.PreferencesRepository.saveAvatar
import com.flipverse.shared.PreferencesRepository.saveCreatedAt
import com.flipverse.shared.PreferencesRepository.saveFirstName
import com.flipverse.shared.PreferencesRepository.saveFlipAccounts
import com.flipverse.shared.PreferencesRepository.saveFlipGenres
import com.flipverse.shared.PreferencesRepository.saveFlipInterests
import com.flipverse.shared.PreferencesRepository.saveFullName
import com.flipverse.shared.PreferencesRepository.saveId
import com.flipverse.shared.PreferencesRepository.saveLastName
import com.flipverse.shared.PreferencesRepository.saveThumbnail
import com.flipverse.shared.PreferencesRepository.saveUsername
import com.flipverse.shared.RequestState
import com.flipverse.shared.domain.Comment
import com.flipverse.shared.domain.CreatePostRequest
import com.flipverse.shared.domain.CreatePostState
import com.flipverse.shared.domain.ImageItem
import com.flipverse.shared.domain.Post
import com.flipverse.shared.domain.PostEngagement
import com.flipverse.shared.domain.PostWithComments
import com.flipverse.shared.domain.Reply
import com.flipverse.shared.domain.SuggestionCriteria
import com.flipverse.shared.domain.TrendingCriteria
import com.flipverse.shared.domain.User
import com.flipverse.shared.domain.VideoItem
import com.flipverse.shared.util.getPushNotificationToken
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.where
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class PostDetailState(
    val postsWithComments: PostWithComments,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class PostsFeedState(
    val posts: List<Post> = emptyList(),
    val trendingPosts: List<Post> = emptyList(),
    val allUsers: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val isInitialLoading: Boolean = false,
    val isPostDetailsLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    var isRefreshing: Boolean = false,
    val isPostingComment: Boolean = false,
    val hasMorePages: Boolean = true,
    val postWithComments: PostWithComments = PostWithComments(
        post = Post(),
        comments = listOf(Comment())
    ),
    val suggestedUsers: List<User> = emptyList(),
    val followingUsers: Set<String> = emptySet(),
    val lastRefreshTime: Long = 0L,
    val followingCount: Int = 0,
    val followersCount: Int = 0,
    val profileDetails: User? = null,
    val userPosts: List<Post> = emptyList(),
    val userLikedPosts: List<Post> = emptyList(),
    val userFollowing: List<User> = emptyList(),
    val userFollowers: List<User> = emptyList(),
    val error: String? = null,
    /** Live results from the on-demand tag-friend search (Recommendation/Review/Quote screens). */
    val userSearchResults: List<User> = emptyList(),
    /** True while a searchUsers Firestore query is in-flight. */
    val isSearchingUsers: Boolean = false,
)

// Post data class
data class PostData(
    val text: String = "",
    val images: List<ImageItem> = emptyList(),
    val videos: List<VideoItem> = emptyList(),
    val whatsNewText: String = "",
    val tagFriendsText: String = "",
    val sourceText: String = "",
    val tags: String = "",
)


class DashboardViewModel(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val nomenclatureRepository: NomenclatureRepository,
    private val feedRepository: FeedRepository
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        println("DashboardViewModel coroutine exception: ${throwable::class.simpleName}: ${throwable.message}")
        throwable.printStackTrace()
    }

    var screenReady: RequestState<Unit> by mutableStateOf(RequestState.Loading)
    var followScreenReady: RequestState<Unit> by mutableStateOf(RequestState.Loading)

    private val _uiState = MutableStateFlow(PostsFeedState())
    val uiState: StateFlow<PostsFeedState> = _uiState.asStateFlow()


    var recommendationScreenState by mutableStateOf(PostData())
        private set

    var pageSize = 15
    private val refreshThreshold = 5 * 60 * 1000L // 5 minutes

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _uiCreatePostState = MutableStateFlow(CreatePostState())
    val uiCreatePostState: StateFlow<CreatePostState> = _uiCreatePostState.asStateFlow()

    /** Drives the debounced on-demand tag-friend search used by Recommendation/Review/Quote screens. */
    private val _tagSearchQuery = MutableStateFlow("")

    private val eventChannel = Channel<DashboardEvent>()
    val event = eventChannel.receiveAsFlow()

    private var followingFeedLoaded = false

    init {
        getCurrentUser()
        loadPostsFeed()
        loadFollowingCount()
        fetchLiveBookGenres()
        startEngagementMonitoring()
        fetchPushToken()
        startTagFriendSearch()
    }

    /**
     * Wires up the debounced Firestore search for tagging friends.
     * Only fires a network call 300 ms after the user stops typing.
     * Results are stored in [PostsFeedState.userSearchResults].
     */
    @OptIn(FlowPreview::class)
    private fun startTagFriendSearch() {
        viewModelScope.launch(exceptionHandler) {
            _tagSearchQuery
                .debounce(300)
                .collectLatest { query ->
                    if (query.isBlank()) {
                        _uiState.value = _uiState.value.copy(
                            userSearchResults = emptyList(),
                            isSearchingUsers = false
                        )
                        return@collectLatest
                    }
                    _uiState.value = _uiState.value.copy(isSearchingUsers = true)
                    val currentUser = _uiCreatePostState.value.currentUser
                    feedRepository.searchUsers(query)
                        .onSuccess { users ->
                            val filtered = users.filter { user ->
                                !user.firstTimeLogin &&
                                currentUser?.let { cu -> cu.id != user.id && cu.email != user.email } ?: true
                            }
                            _uiState.value = _uiState.value.copy(
                                userSearchResults = filtered,
                                isSearchingUsers = false
                            )
                        }
                        .onFailure {
                            _uiState.value = _uiState.value.copy(
                                isSearchingUsers = false,
                                userSearchResults = emptyList()
                            )
                        }
                }
        }
    }

    /** Called from Recommendation/Review/Quote screens as the user types in the tag field. */
    fun onTagSearchQueryChange(query: String) {
        _tagSearchQuery.value = query
    }

    /**
     * Loads the following feed lazily — only on first tab selection.
     */
    fun ensureFollowingFeedLoaded() {
        if (!followingFeedLoaded) {
            followingFeedLoaded = true
            loadFollowingFeed()
        }
    }

    private fun fetchPushToken() {
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            val token = getPushNotificationToken()
            if (token != null) {
                println("Push token retrieved: $token")
                val currentUserId = userRepository.getCurrentUserId()
                if (currentUserId != null) {
                    userRepository.updateMessageToken(
                        userId = currentUserId,
                        token = token,
                        onSuccess = {
                            println("✅ Push token saved to Firebase messages collection successfully")
                        },
                        onError = { error ->
                            println("❌ Failed to save push token to Firebase: $error")
                        }
                    )
                } else {
                    println("⚠️ No current user found, push token not saved")
                }
            } else {
                println("Failed to retrieve push token.")
            }
        }
    }

    fun updatePushToken(token: String) {
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            val currentUserId = userRepository.getCurrentUserId()
            println("🔄 Updating push token: ${token.take(20)}...")
            if (currentUserId != null) {
                userRepository.updateMessageToken(
                    userId = currentUserId,
                    token = token,
                    onSuccess = {
                        println("✅ Push token updated successfully")
                    },
                    onError = { error ->
                        println("❌ Failed to update push token: $error")
                    }
                )
            } else {
                println("⚠️ No current user found, cannot update push token")
            }
        }.invokeOnCompletion { exception ->
            exception?.let { e ->
                println("❌ Exception updating push token: ${e.message}")
            }
        }
    }

    fun loadPostWithComments(postId: String) {
        println("Loading post with comments for postId: $postId")
        viewModelScope.launch(exceptionHandler) {
            _uiState.value = _uiState.value.copy(isPostDetailsLoading = true, error = null)
            postRepository.getPostWithComments(postId).fold(
                onSuccess = {
                    println("Successfully fetched post and comments-->$it")
                    _uiState.value =
                        _uiState.value.copy(postWithComments = it, isPostDetailsLoading = false)
                },
                onFailure = {
                    println("Failed to fetch post and comments-->$it")
                    _uiState.value = _uiState.value.copy(
                        error = it.message ?: "Failed to load post and comments",
                        isPostDetailsLoading = false
                    )
                }
            )
        }
    }

    private fun fetchLiveBookGenres() {
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            nomenclatureRepository.readGenresFlow().collectLatest { data ->
                if (data.isSuccess()) {
                    val fetchedFlipGenres = data.getSuccessData()
                    saveFlipGenres(fetchedFlipGenres.flatMap { it.genres })

                } else if (data.isError()) {
                    screenReady = RequestState.Error(data.getErrorMessage())
                }

            }
        }
    }

    fun onAction(action: DashboardAction) {
        when (action) {
            is DashboardAction.OnSourceChange -> {
                recommendationScreenState =
                    recommendationScreenState.copy(sourceText = action.newValue)
            }

            is DashboardAction.OnTagFriendsChange -> {
                recommendationScreenState =
                    recommendationScreenState.copy(tagFriendsText = action.newValue)
            }

            is DashboardAction.OnWhatsNewChange -> {
                recommendationScreenState =
                    recommendationScreenState.copy(whatsNewText = action.newValue)
            }

            is DashboardAction.OnSelectImages -> {
                recommendationScreenState =
                    recommendationScreenState.copy(images = recommendationScreenState.images + action.selectedImages)
            }

            is DashboardAction.OnSelectVideos -> {
                recommendationScreenState =
                    recommendationScreenState.copy(videos = recommendationScreenState.videos + action.selectedVideos)
            }

            is DashboardAction.OnPostLikeClick -> {
                onPostLikeEnhanced(action.postId, onSuccess = { Unit }, onError = { Unit })
            }

            is DashboardAction.OnPostShareClick -> {
                viewModelScope.launch(exceptionHandler) {
                    onPostShare(
                        action.postId,
                        onSuccess = { },
                        onError = { }
                    )
                }
            }

            is DashboardAction.OnRefreshSuggestedUsersClick -> {
                _uiState.value = _uiState.value.copy(suggestedUsers = action.users)
            }

            is DashboardAction.OnCommentLikeClick -> {
                viewModelScope.launch(exceptionHandler) {
                    onCommentLike(
                        postId = action.postId,
                        onSuccess = { Unit },
                        onError = { Unit },
                        commentId = action.commentId,
                    )
                }
            }

            is DashboardAction.OnCommentShareClick -> {
                viewModelScope.launch(exceptionHandler) {
                    onCommentShare(
                        postId = "",
                        onSuccess = { },
                        onError = { },
                        commentId = action.commentId
                    )
                }
            }

            is DashboardAction.OnPostDetailsLikeClick -> {
                viewModelScope.launch(exceptionHandler) {
                    onPostDetailsLike(
                        action.postId,
                        onSuccess = { Unit },
                        onError = { Unit }
                    )
                }
            }
        }
    }

    fun updateSearchQuery(value: String) {
        _searchQuery.value = value
    }


    @OptIn(ExperimentalTime::class)
    fun loadPostsFeed() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch(exceptionHandler) {
            // Show cached posts instantly if available (no spinner on revisit)
            val hasCachedPosts = _uiState.value.posts.isNotEmpty()
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                isInitialLoading = !hasCachedPosts, // Only show full spinner on first load
                error = null
            )

            if (getEmail().isEmpty() && userRepository.getCurrentUserId() == Firebase.auth.currentUser?.uid) {
                userRepository.fetchUserDetails(Firebase.auth.currentUser?.uid)
            }

            postRepository.getPostsFeedFlow(getEmail(), pageSize)
                .collectLatest { data ->

                    if (data.isSuccess()) {
                        if (data.getSuccessDataOrNull()?.isEmpty() == true) {
                            trendingFeed()

                            launch {
                                loadSuggestedUsers(getEmail()).collectLatest { suggestedUsersData ->
                                    if (suggestedUsersData.isNotEmpty()) {
                                        _uiState.value = _uiState.value.copy(
                                            suggestedUsers = suggestedUsersData,
                                            isLoading = false,
                                            isInitialLoading = false,
                                            hasMorePages = suggestedUsersData.size == pageSize,
                                            lastRefreshTime = Clock.System.now()
                                                .toEpochMilliseconds(),
                                            error = null
                                        )
                                    }
                                }
                            }
                            screenReady = RequestState.Idle
                        } else {
                            val posts = data.getSuccessData()
                            
                            // Refresh author profile images with current user data
                            val postsWithFreshImages = refreshAuthorProfileImages(posts)
                            
                            _uiState.value = _uiState.value.copy(
                                posts = postsWithFreshImages,
                                isLoading = false,
                                isInitialLoading = false,
                                isRefreshing = false,
                                hasMorePages = posts.size == pageSize,
                                isLoadingMore = posts.size == pageSize,
                                lastRefreshTime = Clock.System.now().toEpochMilliseconds(),
                                error = null
                            )

                            // Preload author profile images for better perceived performance
                            preloadPostImages(postsWithFreshImages)

                            // Load suggested users in parallel
                            launch {
                                loadSuggestedUsers(getEmail()).collectLatest { suggestedUsersData ->
                                    if (suggestedUsersData.isNotEmpty()) {
                                        _uiState.value = _uiState.value.copy(
                                            suggestedUsers = suggestedUsersData,
                                        )
                                    }
                                }
                            }

                            screenReady = RequestState.Success(Unit)
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isInitialLoading = false,
                            error = data.getErrorMessage()
                        )
                        screenReady = RequestState.Error(data.getErrorMessage())
                    }
                }
        }
    }

    /**
     * Refreshes author profile images by fetching current user data from Firestore.
     * post.authorId stores the user's EMAIL — so we query by "email", not "id".
     */
    private suspend fun refreshAuthorProfileImages(posts: List<Post>): List<Post> {
        // Get unique author emails from posts (authorId == email)
        val uniqueAuthorEmails = posts.map { it.authorId }.filter { it.isNotBlank() }.distinct()

        if (uniqueAuthorEmails.isEmpty()) return posts

        // Fetch current user data for each unique author email from Firestore
        val authorDataMap = mutableMapOf<String, User>()
        val userCollection = Firebase.firestore.collection("user")

        uniqueAuthorEmails.forEach { authorEmail ->
            try {
                val userQuery = userCollection
                    .where { "email" equalTo authorEmail }
                    .limit(1)
                    .get()

                if (userQuery.documents.isNotEmpty()) {
                    val user = userQuery.documents.first().data<User>()
                    authorDataMap[authorEmail] = user
                }
            } catch (e: Exception) {
                // Silently skip — keep whatever image the post already has
            }
        }

        // Update posts with fresh author data
        return posts.map { post ->
            val freshUser = authorDataMap[post.authorId] ?: return@map post
            post.copy(
                authorProfileImage = freshUser.thumbnail.takeIf { it.isNotBlank() }
                    ?: post.authorProfileImage,
                authorName = freshUser.fullname.takeIf { it.isNotBlank() } ?: post.authorName,
                authorHandle = freshUser.username.takeIf { it.isNotBlank() } ?: post.authorHandle,
                authorIsVerified = freshUser.isVerified
            )
        }
    }

    /**
     * Preloads author profile images for posts to improve perceived performance.
     * Loads images in background without blocking UI.
     * Note: Full implementation requires platform-specific context access.
     */
    private fun preloadPostImages(posts: List<Post>) {
        // Image preloading is handled by Coil's built-in caching mechanism
        // when images are loaded via SubcomposeAsyncImage in the UI layer.
        // This method is a placeholder for future advanced preloading if needed.
    }

    @OptIn(ExperimentalTime::class)
    suspend fun trendingFeed() {
        loadTrendingFeed(
            getEmail()
        ).collectLatest { trendingData ->
            //todo:make interests the base
            if (trendingData.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    trendingPosts = trendingData,
                    isLoading = false,
                    isInitialLoading = false,
                    hasMorePages = trendingData.size == pageSize,
                    lastRefreshTime = Clock.System.now().toEpochMilliseconds(),
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    trendingPosts = emptyList(),
                    isLoading = false,
                    isInitialLoading = false,
                    isRefreshing = false,
                    error = "Something Happened!"
                )

            }
        }
    }

    fun loadMoreFeedPosts() {
        val currentState = _uiState.value
        if (!currentState.hasMorePages || currentState.posts.isEmpty()) {
            return
        }

        viewModelScope.launch(exceptionHandler) {
            val lastPostTimestamp = currentState.posts.lastOrNull()?.timestamp

            postRepository.getPostsFeedFlow(
                getEmail(),
                pageSize,
                lastPostTimestamp
            ).collectLatest { data ->
                if (data.isSuccess()) {
                    val newPosts = data.getSuccessData()
                    val allPosts = currentState.posts + newPosts
                    _uiState.value = _uiState.value.copy(
                        posts = allPosts,
                        isLoadingMore = false,
                        hasMorePages = newPosts.size == pageSize,
                        error = null
                    )
                    screenReady = RequestState.Success(Unit)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        error = data.getErrorMessage()
                    )
                    screenReady = RequestState.Error(data.getErrorMessage())
                }
            }
        }
    }

    fun onFollowClick(userId: String) {
        val currentFollowing = _uiState.value.followingUsers
        val isNowFollowing = !currentFollowing.contains(userId)

        val newFollowing = if (currentFollowing.contains(userId)) {
            currentFollowing - userId
        } else {
            currentFollowing + userId
        }

        // Optimistically update local state
        _uiState.value = _uiState.value.copy(followingUsers = newFollowing)
        println("New Following status updated: $newFollowing")
        println("Following status updated: $isNowFollowing")

        // Update following status in Firebase and sync counts
        viewModelScope.launch(exceptionHandler) {
            val result = feedRepository.updateFollowingStatus(
                currentUserId = getEmail(),
                targetUserId = userId,
                isFollowing = isNowFollowing
            )
            println("Following status updated: $result")
            if (result.isFailure) {
                println("Failure here!!!!")
                // Revert if backend fails
                _uiState.value = _uiState.value.copy(
                    followingUsers = currentFollowing,
                    error = result.exceptionOrNull()?.message ?: "Could not update following status"
                )
            }
            else {
                println("Success here!!!!")
                loadFollowingCount()
            }
        }
    }

    /**
     * Updates engagement counts for a specific post in real-time
     * This function uses the existing post data and refreshes from the feed
     */
    fun updatePostEngagement(postId: String) {
        viewModelScope.launch(exceptionHandler) {
            try {
                // Use getPostWithComments to get fresh data for the specific post
                postRepository.getPostWithComments(postId).fold(
                    onSuccess = { postWithComments ->
                        val updatedPost = postWithComments.post
                        val updatedEngagement = updatedPost.engagement

                        // Update the specific post in all relevant lists
                        val updatedPosts = _uiState.value.posts.map { post ->
                            if (post.id == postId) {
                                post.copy(engagement = updatedEngagement)
                            } else {
                                post
                            }
                        }

                        val updatedTrendingPosts = _uiState.value.trendingPosts.map { post ->
                            if (post.id == postId) {
                                post.copy(engagement = updatedEngagement)
                            } else {
                                post
                            }
                        }

                        val updatedUserPosts = _uiState.value.userPosts.map { post ->
                            if (post.id == postId) {
                                post.copy(engagement = updatedEngagement)
                            } else {
                                post
                            }
                        }

                        // Update post details if viewing the specific post
                        val updatedPostWithComments =
                            if (_uiState.value.postWithComments.post.id == postId) {
                                postWithComments
                        } else {
                            _uiState.value.postWithComments
                        }

                        _uiState.value = _uiState.value.copy(
                            posts = updatedPosts,
                            trendingPosts = updatedTrendingPosts,
                            userPosts = updatedUserPosts,
                            postWithComments = updatedPostWithComments
                        )

                        println("✅ Updated engagement for post $postId: likes=${updatedEngagement.likesCount}, comments=${updatedEngagement.commentsCount}")
                    },
                    onFailure = { error ->
                        println("❌ Failed to update engagement for post $postId: ${error.message}")
                        // Don't show error to user for background sync failures
                    }
                )
            } catch (e: Exception) {
                println("❌ Exception updating engagement for post $postId: ${e.message}")
            }
        }
    }

    /**
     * Starts real-time monitoring of engagement for the currently viewed post only.
     * Only syncs the post detail view to avoid hammering the backend with bulk API calls.
     * Feed-level engagement is kept in sync via optimistic updates on user interactions.
     */
    fun startEngagementMonitoring() {
        viewModelScope.launch(exceptionHandler) {
            while (true) {
                try {
                    // Only sync the currently viewed post detail (if any)
                    val currentPostId = _uiState.value.postWithComments.post.id
                    if (currentPostId.isNotEmpty()) {
                        updatePostEngagement(currentPostId)
                    }

                    // 5-minute interval — feed engagement is handled by optimistic updates
                    kotlinx.coroutines.delay(300_000)

                } catch (e: Exception) {
                    kotlinx.coroutines.delay(600_000)
                }
            }
        }
    }

    /**
     * Manually refreshes engagement for a specific post
     * Useful for immediate updates after user interactions
     */
    fun refreshPostEngagement(postId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch(exceptionHandler) {
            try {
                updatePostEngagement(postId)
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun onPostLikeEnhanced(
        postId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        // First update local state for immediate UI feedback across all post lists
        val updatePostEngagement: (Post) -> Post = { post ->
            if (post.id == postId) {
                val count = if (post.engagement.isLikedByCurrentUser) -1 else +1
                post.copy(
                    engagement = PostEngagement(
                        likesCount = post.engagement.likesCount + count,
                        sharesCount = post.engagement.sharesCount,
                        commentsCount = post.engagement.commentsCount,
                        isLikedByCurrentUser = !post.engagement.isLikedByCurrentUser,
                        isSharedByCurrentUser = post.engagement.isSharedByCurrentUser
                    )
                )
            } else {
                post
            }
        }
        
        val updatedPosts = _uiState.value.posts.map(updatePostEngagement)
        val updatedTrendingPosts = _uiState.value.trendingPosts.map(updatePostEngagement)
        val updatedUserPosts = _uiState.value.userPosts.map(updatePostEngagement)
        
        // Update post details if currently viewing this post
        val updatedPostWithComments = if (_uiState.value.postWithComments.post.id == postId) {
            val currentDetails = _uiState.value.postWithComments
            val currentlyLiked = currentDetails.post.engagement.isLikedByCurrentUser
            val delta = if (currentlyLiked) -1 else 1
            currentDetails.copy(
                post = currentDetails.post.copy(
                    engagement = currentDetails.post.engagement.copy(
                        likesCount = currentDetails.post.engagement.likesCount + delta,
                        isLikedByCurrentUser = !currentlyLiked
                    )
                )
            )
        } else {
            _uiState.value.postWithComments
        }
        
        _uiState.value = _uiState.value.copy(
            posts = updatedPosts,
            trendingPosts = updatedTrendingPosts,
            userPosts = updatedUserPosts,
            postWithComments = updatedPostWithComments
        )

        // Then sync with backend and refresh engagement
        viewModelScope.launch(exceptionHandler) {
            val targetPost = updatedPosts.find { it.id == postId }
            if (targetPost != null) {
                feedRepository.updatePostEngagement(
                    post = targetPost,
                    interactionType = "like",
                    onSuccess = {
                        // Don't fetch fresh data here to avoid race conditions
                        // The optimistic update is already correct
                        onSuccess()
                    },
                    onError = { message ->
                        // Revert local state on backend failure across all lists
                        val revertPostEngagement: (Post) -> Post = { post ->
                            if (post.id == postId) {
                                val count = if (!post.engagement.isLikedByCurrentUser) -1 else +1
                                post.copy(
                                    engagement = post.engagement.copy(
                                        likesCount = post.engagement.likesCount + count,
                                        isLikedByCurrentUser = !post.engagement.isLikedByCurrentUser
                                    )
                                )
                            } else {
                                post
                            }
                        }
                        
                        val revertedPostWithComments = if (_uiState.value.postWithComments.post.id == postId) {
                            val currentDetails = _uiState.value.postWithComments
                            val currentlyLiked = currentDetails.post.engagement.isLikedByCurrentUser
                            val delta = if (!currentlyLiked) -1 else 1
                            currentDetails.copy(
                                post = currentDetails.post.copy(
                                    engagement = currentDetails.post.engagement.copy(
                                        likesCount = currentDetails.post.engagement.likesCount + delta,
                                        isLikedByCurrentUser = !currentlyLiked
                                    )
                                )
                            )
                        } else {
                            _uiState.value.postWithComments
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            posts = _uiState.value.posts.map(revertPostEngagement),
                            trendingPosts = _uiState.value.trendingPosts.map(revertPostEngagement),
                            userPosts = _uiState.value.userPosts.map(revertPostEngagement),
                            postWithComments = revertedPostWithComments
                        )
                        onError(message)
                    }
                )
            }
        }
    }

    fun onPostLike(postId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        // For now, just update local state

        val updatedPosts = _uiState.value.posts.map { post ->
            println("Post ID: ${post.id}, Post Like: ${post.engagement.isLikedByCurrentUser}")
            if (post.id == postId) {
                val count = if (post.engagement.isLikedByCurrentUser) -1 else +1
                post.copy(
                    engagement = PostEngagement(
                        likesCount = post.engagement.likesCount.plus(count),
                        sharesCount = post.engagement.sharesCount,
                        commentsCount = post.engagement.commentsCount,
                        isLikedByCurrentUser = !post.engagement.isLikedByCurrentUser
                    )
                )
            } else {
                post
            }
        }
        _uiState.value = _uiState.value.copy(posts = updatedPosts)

        viewModelScope.launch(exceptionHandler) {
            feedRepository.updatePostEngagement(
                post = updatedPosts.find { it.id == postId }!!,
                interactionType = "like",
                onSuccess = { onSuccess() },
                onError = { message -> onError(message) })
        }
    }

    fun onPostDetailsLike(postId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        // For now, just update local state

//        val updatedPosts = _uiState.value.posts.map { post ->
//            println("Post ID: ${post.id}, Post Like: ${post.engagement.isLikedByCurrentUser}")
//            if (post.id == postId) {
//                val count = if (post.engagement.isLikedByCurrentUser) -1 else +1
//                post.copy(
//                    engagement = PostEngagement(
//                        likesCount = post.engagement.likesCount.plus(count),
//                        sharesCount = post.engagement.sharesCount,
//                        commentsCount = post.engagement.commentsCount,
//                        isLikedByCurrentUser = !post.engagement.isLikedByCurrentUser
//                    )
//                )
//            } else {
//                post
//            }
//        }
//        _uiState.value = _uiState.value.copy(posts = updatedPosts)

        // Update state for Post details as well
        val currentDetails = _uiState.value.postWithComments
        var updatedDetailsPost = currentDetails.post
        if (currentDetails.post.id == postId) {
            val currentlyLiked = currentDetails.post.engagement.isLikedByCurrentUser
            val delta = if (currentlyLiked) -1 else 1
            updatedDetailsPost = currentDetails.post.copy(
                postId = postId,
                engagement = currentDetails.post.engagement.copy(
                    likesCount = currentDetails.post.engagement.likesCount + delta,
                    isLikedByCurrentUser = !currentlyLiked
                )
            )
            _uiState.value = _uiState.value.copy(
                postWithComments = currentDetails.copy(post = updatedDetailsPost)
            )
        }

        viewModelScope.launch(exceptionHandler) {
            feedRepository.updatePostEngagement(
                post = updatedDetailsPost,
                interactionType = "like",
                onSuccess = { onSuccess() },
                onError = { message -> onError(message) })
        }
    }

    fun onPostComment(
        postId: String,
        replyText: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        viewModelScope.launch(exceptionHandler) {
            _uiState.value = _uiState.value.copy(isPostingComment = true)
            postRepository.addCommentToPost(
                postId = postId,
                commentText = replyText,
                commentById = getEmail(),
                commentByHandle = normalizeUsername(getUsername()),
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isPostingComment = false)
                    onSuccess()
                },
                onError = { error ->
                    _uiState.value = _uiState.value.copy(isPostingComment = false)
                    onError(error)
                }
            )
        }
    }

    fun onPostShare(postId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        // TODO: Share post functionality
        //todo: share link via native options/as Dm to fellow user
        val updatedPosts = _uiState.value.posts.map { post ->
            if (post.id == postId) {
                post.copy(
                    engagement = PostEngagement(
                        sharesCount = post.engagement.sharesCount.plus(1),
                        likesCount = post.engagement.likesCount,
                        isLikedByCurrentUser = post.engagement.isLikedByCurrentUser,
                        isSharedByCurrentUser = !post.engagement.isSharedByCurrentUser
                    )
                )
            } else {
                post
            }
        }
        _uiState.value = _uiState.value.copy(posts = updatedPosts)

        viewModelScope.launch(exceptionHandler) {
            feedRepository.updatePostEngagement(
                post = updatedPosts.find { it.id == postId }!!,
                interactionType = "share",
                onSuccess = { onSuccess() },
                onError = { message -> onError(message) })
        }
    }

    fun onCommentReply(
        postId: String,
        parentCommentId: String,
        replyText: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val beforeComments = _uiState.value.postWithComments.comments
        // TODO: Update the targeted comment (id == parentCommentId) by setting the reply object per your Comment/Reply schema
        // Example:
        val updatedComments = beforeComments.map { comment ->
            if (comment.id == parentCommentId) {
                comment.copy(reply = Reply(replyText = replyText, replyById = parentCommentId))
            } else {
                comment
            }
        }
//        val updatedComments = beforeComments.copy(reply = Reply(replyText = replyText, replyById = parentCommentId))
//        val updatedComments = beforeComments

        _uiState.value = _uiState.value.copy(
            postWithComments = _uiState.value.postWithComments.copy(
                comments = updatedComments
            )
        )

        viewModelScope.launch(exceptionHandler) {
            try {
                postRepository.addReplyToComment(
                    postId = postId,
                    parentCommentId = parentCommentId,
                    replyText = replyText,
                    replyById = getEmail(),
                    replyByHandle = normalizeUsername(getUsername()),
                    onSuccess = {},
                    onError = { msg -> throw Exception(msg) }
                )
                loadPostWithComments(postId)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    postWithComments = _uiState.value.postWithComments.copy(
                        comments = beforeComments
                    ),
                    error = e.message
                )
                onError(e.message ?: "Failed to reply")
            }
        }
    }

    fun onCommentLike(
        postId: String,
        commentId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val beforeComments = _uiState.value.postWithComments.comments
        val updatedComments = beforeComments.map { c ->
            if (c.id == commentId) {
                val currentlyLiked = c.engagement.isLikedByCurrentUser
                val delta = if (currentlyLiked) -1 else 1
                c.copy(
                    likesCount = c.likesCount + delta,
                    engagement = c.engagement.copy(
                        likesCount = c.engagement.likesCount + delta,
                        isLikedByCurrentUser = !currentlyLiked
                    )
                )
            } else c
        }

        _uiState.value = _uiState.value.copy(
            postWithComments = _uiState.value.postWithComments.copy(comments = updatedComments)
        )

        viewModelScope.launch(exceptionHandler) {
            try {
                postRepository.updateCommentLike(
                    postId,
                    commentId,
                    getEmail(),
                    updatedComments.find { it.id == commentId }!!.engagement.isLikedByCurrentUser,
                    onSuccess,
                    onError
                )
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    postWithComments = _uiState.value.postWithComments.copy(comments = beforeComments),
                    error = e.message
                )
                onError(e.message ?: "Failed to like comment")
            }
        }
    }

    fun onCommentShare(
        postId: String?,
        commentId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val beforeComments = _uiState.value.postWithComments.comments
        val updatedComments = beforeComments.map { c ->
            if (c.id == commentId) {
                c.copy(
                    engagement = c.engagement.copy(
                        sharesCount = c.engagement.sharesCount + 1
                    )
                )
            } else c
        }

        _uiState.value = _uiState.value.copy(
            postWithComments = _uiState.value.postWithComments.copy(comments = updatedComments)
        )

        viewModelScope.launch(exceptionHandler) {
            try {
                // TODO: postRepository.shareComment(postId, commentId, getEmail())
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    postWithComments = _uiState.value.postWithComments.copy(comments = beforeComments),
                    error = e.message
                )
                onError(e.message ?: "Failed to share comment")
            }
        }
    }

    fun pinPostOnClick(postId: String, isPinned: Boolean) {
        // Launch in the ViewModelScope for async work
        viewModelScope.launch(exceptionHandler) {
            try {
                postRepository.pinPost(
                    postId,
                    getId(),
                    onSuccess = {
                        // Optionally update local posts state if immediate UI feedback needed:
                        _uiState.value =
                            _uiState.value.copy(posts = _uiState.value.posts.map { post ->
                                if (post.id == postId) post.copy(isPinned = isPinned) else post
                            })

                        // Optionally, show a success message
                        // _messageBarState.addSuccess("Post ${if (isPinned) "pinned" else "unpinned"}!")
                    },
                    onError = { message -> println("Failed to pin post: $message") },
                    isPinned = isPinned
                )


            } catch (e: Exception) {
                // Optionally, show error
                // _messageBarState.addError("Failed to pin post: ${e.message}")
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun refreshPostsFeed() {
//        if (_uiState.value.isRefreshing) return

        viewModelScope.launch(exceptionHandler) {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)

            postRepository.refreshPostsFeedFlow(getEmail(), pageSize)
                .collectLatest { data ->
                    println("Refreshing posts feed...$data")
                    if (data.isSuccess()) {
                        val newPosts = data.getSuccessData()
                        
                        // Refresh author profile images with current user data
                        val postsWithFreshImages = refreshAuthorProfileImages(newPosts)
                        
                        _uiState.value = _uiState.value.copy(
                            posts = postsWithFreshImages,
                            isRefreshing = false,
                            hasMorePages = newPosts.size == pageSize,
                            lastRefreshTime = Clock.System.now().toEpochMilliseconds(),
                            error = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isRefreshing = false,
                            error = data.getErrorMessage()
                        )
                    }
                }
        }

    }

    private fun loadFollowingCount() {
        viewModelScope.launch(exceptionHandler) {
            postRepository.getFollowingCount(getEmail())
                .collectLatest { data ->
                    if (data.isSuccess()) {
                        println("following Count ${data.getSuccessData()}")
                        _uiState.value =
                            _uiState.value.copy(followingCount = data.getSuccessData())
                    } else {
                        _uiState.value = _uiState.value.copy(
                            followingCount = 0
                        )
                    }
                }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun shouldRefreshAutomatically(): Boolean {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        val lastRefresh = _uiState.value.lastRefreshTime
        return (currentTime - lastRefresh) > refreshThreshold
    }

    fun getCurrentUser(): User? {
        viewModelScope.launch(exceptionHandler) {
            println("Check :: ${getEmail()}")
            userRepository.readUserProfileFlow().collectLatest { userData ->
                if (userData.isSuccess()) {
                    _uiCreatePostState.value =
                        _uiCreatePostState.value.copy(currentUser = userData.getSuccessData())
                    println("User data loaded: ${userData.getSuccessData()}")
                    saveUsername(userData.getSuccessData().username)
                    saveFullName(userData.getSuccessData().fullname)
                    saveThumbnail(userData.getSuccessData().thumbnail)
                    saveFirstName(userData.getSuccessData().firstName)
                    saveLastName(userData.getSuccessData().lastName)
                    saveFlipAccounts(userData.getSuccessData().selectedAccounts)
                    saveFlipInterests(userData.getSuccessData().interests)
                    saveAvatar(userData.getSuccessData().avatar)
                    saveId(userData.getSuccessData().id)
                    saveCreatedAt(userData.getSuccessData().createdAt)

                } else {
                    println("User Error Message: ${userData.getErrorMessage()}")
                    _uiCreatePostState.value =
                        _uiCreatePostState.value.copy(error = userData.getErrorMessage())
                }
            }
        }
        return _uiCreatePostState.value.currentUser
    }

    fun getUserPosts(userId: String) {
        viewModelScope.launch(exceptionHandler) {
            try {
                postRepository.getUserPosts(userId).collectLatest { result ->
                    if (result.isSuccess()) {
                        val userPosts = result.getSuccessData()
                        _uiState.value = _uiState.value.copy(userPosts = userPosts)
                        println("Successfully fetched ${userPosts.size} user posts")
                    } else {
                        _uiState.value = _uiState.value.copy(
                            userPosts = emptyList(),
                            error = result.getErrorMessage()
                        )
                        println("Error fetching user posts: ${result.getErrorMessage()}")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    userPosts = emptyList(),
                    error = e.message
                )
                println("Exception fetching user posts: ${e.message}")
            }
        }
    }

    fun getUserLikedPosts(userId: String) {
        viewModelScope.launch(exceptionHandler) {
            try {
                postRepository.getUserLikedPosts(userId).collectLatest { result ->
                    if (result.isSuccess()) {
                        val userLikedPosts = result.getSuccessData()
                        _uiState.value = _uiState.value.copy(userLikedPosts = userLikedPosts)
                        println("Successfully fetched ${userLikedPosts.size} user liked posts")
                    } else {
                        _uiState.value = _uiState.value.copy(
                            userLikedPosts = emptyList(),
                            error = result.getErrorMessage()
                        )
                        println("Error fetching user liked posts: ${result.getErrorMessage()}")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    userLikedPosts = emptyList(),
                    error = e.message
                )
                println("Exception fetching user liked posts: ${e.message}")
            }
        }
    }

    fun getUserFollowing(userId: String) {
        viewModelScope.launch(exceptionHandler) {
            try {
                val result = feedRepository.getUserFollowing(userId)
                result.onSuccess { followingIds ->
                    // Fetch User objects for each followed user concurrently using Flow
                    val allUsers = mutableListOf<User>()
                    val jobs = followingIds.map { followedUserId ->
                        launch {
                            feedRepository.readProfileFlow(followedUserId)
                                .collectLatest { userResult ->
                                    if (userResult.isSuccess()) {
                                        allUsers.add(userResult.getSuccessData())
                                    }
                                }
                        }
                    }
                    // Wait for all jobs to complete
                    jobs.forEach { it.join() }

                    _uiState.value = _uiState.value.copy(
                        userFollowing = allUsers,
                        error = null
                    )
                    println("Successfully fetched ${allUsers.size} following users from ${followingIds.size} followed ids")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        userFollowing = emptyList(),
                        error = error.message
                    )
                    println("Error fetching followed users: ${error.message}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    userFollowing = emptyList(),
                    error = e.message
                )
                println("Exception fetching followed users: ${e.message}")
            }
        }
    }

    fun getUserFollowers(userId: String) {
        viewModelScope.launch(exceptionHandler) {
            try {
                val result = feedRepository.getUserFollowers(userId)
                result.onSuccess { followerIds ->
                    // Fetch User objects for each follower concurrently using Flow
                    val allUsers = mutableListOf<User>()
                    val jobs = followerIds.map { followerId ->
                        launch {
                            feedRepository.readProfileFlow(followerId)
                                .collectLatest { userResult ->
                                    if (userResult.isSuccess()) {
                                        allUsers.add(userResult.getSuccessData())
                                    }
                                }
                        }
                    }
                    // Wait for all jobs to complete
                    jobs.forEach { it.join() }

                    _uiState.value = _uiState.value.copy(
                        userFollowers = allUsers,
                        followersCount = allUsers.size,
                        error = null
                    )
                    println("Successfully fetched ${allUsers.size} followers from ${followerIds.size} follower ids")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        userFollowers = emptyList(),
                        error = error.message
                    )
                    println("Error fetching followers: ${error.message}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    userFollowers = emptyList(),
                    error = e.message
                )
                println("Exception fetching followers: ${e.message}")
            }
        }
    }

    /**
     * Loads the following user IDs (emails) for the current user
     * and updates the followingUsers Set in the state
     */
    fun loadCurrentUserFollowing() {
        viewModelScope.launch(exceptionHandler) {
            try {
                val currentUserId = getEmail()
                val result = feedRepository.getUserFollowing(currentUserId)
                result.onSuccess { followingIds ->
                    _uiState.value = _uiState.value.copy(
                        followingUsers = followingIds.toSet(),
                        error = null
                    )
                    println("Successfully loaded ${followingIds.size} following IDs for current user")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        followingUsers = emptySet(),
                        error = error.message
                    )
                    println("Error loading current user following: ${error.message}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    followingUsers = emptySet(),
                    error = e.message
                )
                println("Exception loading current user following: ${e.message}")
            }
        }
    }

    fun createPost(request: CreatePostRequest, onSuccess: () -> Unit, onError: (String) -> Unit) {
//todo: Source could be a link,a name etc...
        //todo: Tag shd populate users followers
        viewModelScope.launch(exceptionHandler) {
            try {

                // Step 1: Create the post

                val currentUser = _uiCreatePostState.value.currentUser
                println("Creating  user: $currentUser")

                postRepository.createPost(request, currentUser!!).collectLatest { data ->
                    println("Post creation response: $data")
                    if (data.isSuccess()) {
                        onSuccess()
                    } else {
                        onError(data.getErrorMessage())
                        return@collectLatest
                    }
                }

                // Step 2: Populate user feeds (async - don't block UI)
//                launch {
//                    postRepository.populateUserFeeds(
//                        _uiCreatePostState.value.createdPost!!,
//                        currentUser
//                    )
//                        .collectLatest { data ->
//                            if (data.isSuccess()) {
//                                _uiCreatePostState.value = _uiCreatePostState.value.copy(
//                                    feedPopulationStatus = FeedPopulationStatus.Completed
//                                )
//                            } else {
//                                _uiCreatePostState.value = _uiCreatePostState.value.copy(
//                                    feedPopulationStatus = FeedPopulationStatus.Failed
//                                )
//                            }
//                        }
//                }

                _uiCreatePostState.value = _uiCreatePostState.value.copy(isCreating = false)

            } catch (e: Exception) {
                _uiCreatePostState.value = _uiCreatePostState.value.copy(
                    isCreating = false,
                    isUploadingMedia = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun loadTrendingFeed(userId: String): Flow<List<Post>> = flow {

        val criteria = TrendingCriteria(
            timeWindowHours = 24,
            minEngagementScore = 0.4,
            minQualityScore = 0.5,
            recencyWeight = 0.3,
            engagementWeight = 0.4,
            qualityWeight = 0.3
        )

        feedRepository.getTrendingPosts(userId, 20, criteria)
            .onSuccess { posts ->
                emit(posts)
            }
            .onFailure {
            }
    }

    fun loadSuggestedUsers(userId: String): Flow<List<User>> = flow {

        val criteria = SuggestionCriteria(
            minFollowers = 0,
            maxFollowers = 10000,
            minPostCount = 0,
            minQualityScore = 0.0,
            minEngagementRate = 0.0,
            categoryMatchWeight = 0.4,
            mutualConnectionsWeight = 0.3,
            activityWeight = 0.3
        )

        // Use at least 30 to ensure we have enough suggestions to display
        val suggestionsLimit = maxOf(pageSize, 30)
        feedRepository.getSuggestedUsers(userId, suggestionsLimit, criteria)
            .onSuccess { users -> emit(users) }
            .onFailure { /* handle error */ }
    }

    fun loadAllUsers(limit: Int = 200, lastUserId: String? = null) {
        viewModelScope.launch(exceptionHandler) {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                var allFetchedUsers: List<User> = if (lastUserId == null) emptyList() else _uiState.value.allUsers
                var currentLastUserId: String? = lastUserId
                var hasMore = true

                // Auto-paginate to fetch ALL users.
                // NOTE: filtering must happen here (after pagination) — not inside the
                // repository — so that hasMore is based on raw Firestore doc counts and
                // pagination is never stopped prematurely.
                while (hasMore) {
                    val result = feedRepository.getAllUsers(limit, currentLastUserId)
                    result
                        .onSuccess { rawUsers ->
                            // Cursor is based on raw doc order so subsequent pages are correct
                            hasMore = rawUsers.size == limit
                            currentLastUserId = rawUsers.lastOrNull()?.createdAt
                            // Only keep users who have completed onboarding
                            val validUsers = rawUsers.filter { !it.firstTimeLogin }
                            allFetchedUsers = allFetchedUsers + validUsers
                            println("Loaded ${rawUsers.size} raw docs (${validUsers.size} valid). Total so far: ${allFetchedUsers.size}")
                        }
                        .onFailure { error ->
                            hasMore = false
                            println("Error loading users page: ${error.message}")
                        }
                }

                _uiState.value = _uiState.value.copy(
                    allUsers = allFetchedUsers,
                    isLoading = false,
                    isLoadingMore = false,
                    hasMorePages = false,
                    error = null
                )
                println("Successfully loaded all ${allFetchedUsers.size} users.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun loadMoreUsers(limit: Int = 100) {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || !currentState.hasMorePages || currentState.allUsers.isEmpty()) {
            return
        }

        viewModelScope.launch(exceptionHandler) {
            try {
                _uiState.value = _uiState.value.copy(isLoadingMore = true, error = null)
                val lastCreatedAt = currentState.allUsers.lastOrNull()?.createdAt

                feedRepository.getAllUsers(limit, lastCreatedAt)
                    .onSuccess { rawUsers ->
                        val validUsers = rawUsers.filter { !it.firstTimeLogin }
                        val updatedUsers = currentState.allUsers + validUsers

                        _uiState.value = _uiState.value.copy(
                            allUsers = updatedUsers,
                            isLoadingMore = false,
                            hasMorePages = rawUsers.size == limit,
                            error = null
                        )
                        println("Successfully loaded ${rawUsers.size} raw docs (${validUsers.size} valid). Total: ${updatedUsers.size}")
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoadingMore = false,
                            error = error.message ?: "Failed to load more users"
                        )
                        println("Error loading more users: ${error.message}")
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun onRemoveUser(userId: String) {
        val currentSuggestedUsers = _uiState.value.suggestedUsers
        val updatedSuggestedUsers = currentSuggestedUsers.filter { user -> user.email != userId }

        _uiState.value = _uiState.value.copy(suggestedUsers = updatedSuggestedUsers)

        println("Removed user $userId from suggested users. Remaining: ${updatedSuggestedUsers.size}")
    }

    fun updateUploadProgress(progress: Float) {
        _uiCreatePostState.value = _uiCreatePostState.value.copy(uploadProgress = progress)
    }

    fun clearCreatePostError() {
        _uiCreatePostState.value =
            _uiCreatePostState.value.copy(error = null, mediaUploadError = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetState() {
        _uiCreatePostState.value = CreatePostState()
    }


    fun signOut(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(exceptionHandler) {
            val result = withContext(Dispatchers.IO) {
                userRepository.signOut()
            }
            if (result.isSuccess()) {
                onSuccess()
            } else if (result.isError()) {
                onError(result.getErrorMessage())
            }
        }
    }

    @OptIn(ExperimentalTime::class, FlowPreview::class)
    fun loadFollowingFeed() {
        viewModelScope.launch(exceptionHandler) {
            followScreenReady = RequestState.Loading
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            postRepository.getOnlyFollowingFeedFlow(getEmail(), pageSize)
                .collectLatest { data ->
                    if (data.isSuccess()) {
                        if (data.getSuccessData().isEmpty()) {
                            // Load suggested users in parallel
                            launch {
                                loadSuggestedUsers(getEmail()).collectLatest { suggestedUsersData ->
                                    if (suggestedUsersData.isNotEmpty()) {
                                        _uiState.value = _uiState.value.copy(
                                            suggestedUsers = suggestedUsersData,
                                            isLoading = false,
                                            hasMorePages = suggestedUsersData.size == pageSize,
                                            lastRefreshTime = Clock.System.now()
                                                .toEpochMilliseconds(),
                                            error = null
                                        )
                                    }
                                }
                            }
                            followScreenReady = RequestState.Idle
                        } else {
                            val posts = data.getSuccessData()
                            
                            // Refresh author profile images with current user data
                            val postsWithFreshImages = refreshAuthorProfileImages(posts)
                            
                            _uiState.value = _uiState.value.copy(
                                posts = postsWithFreshImages,
                                isLoading = false,
                                hasMorePages = posts.size == pageSize,
                                lastRefreshTime = Clock.System.now().toEpochMilliseconds(),
                                error = null
                            )
                            followScreenReady = RequestState.Success(Unit)
                        }
                    } else {
                        if (data.getErrorMessage() == "No users are being followed") {
                            launch {
                                loadSuggestedUsers(getEmail()).collectLatest { suggestedUsersData ->
                                    if (suggestedUsersData.isNotEmpty()) {
                                        _uiState.value = _uiState.value.copy(
                                            suggestedUsers = suggestedUsersData,
                                            isLoading = false,
                                            hasMorePages = suggestedUsersData.size == pageSize,
                                            lastRefreshTime = Clock.System.now()
                                                .toEpochMilliseconds(),
                                            error = null
                                        )
                                    }
                                }
                            }
                            followScreenReady = RequestState.Idle
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = data.getErrorMessage()
                            )
                            followScreenReady = RequestState.Error(data.getErrorMessage())
                        }
                    }
                }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch(exceptionHandler) {
            // optimistic UI update: remove from list first
            val current = _uiState.value.posts
            val newList = current.filterNot { it.id == postId }
            _uiState.value = _uiState.value.copy(posts = newList)

            postRepository.deletePost(
                postId = postId,
                userId = getId(),
                onSuccess = {
                    // no-op; state already updated
                },
                onError = { msg ->
                    // revert on failure
                    _uiState.value = _uiState.value.copy(posts = current, error = msg)
                }
            )
        }
    }

    fun hidePost(postId: String, hide: Boolean = true) {
        viewModelScope.launch(exceptionHandler) {
            val current = _uiState.value.posts
            val newList = if (hide) current.filterNot { it.id == postId } else current
            if (hide) {
                _uiState.value = _uiState.value.copy(posts = newList)
            }

            postRepository.hidePost(
                postId = postId,
                userId = getId(),
                isHidden = hide,
                onSuccess = { /* no-op */ },
                onError = { msg ->
                    // revert if we hid
                    if (hide) _uiState.value = _uiState.value.copy(posts = current, error = msg)
                }
            )
        }
    }

    fun viewProfile(userId: String) {
        viewModelScope.launch(exceptionHandler) {
            feedRepository.readProfileFlow(userId).collectLatest { userData ->
                if (userData.isSuccess()) {
                    println("User data loaded: ${userData.getSuccessData()}")
                    val user = userData.getSuccessData()
                    println("username: ${normalizeUsername(user.username)}")

                    val normalizedUser = user.copy(
                        username = normalizeUsername(user.username)
                    )
                    _uiState.value = _uiState.value.copy(
                        profileDetails = normalizedUser
                    )
                } else {
                    println("User Error Message: ${userData.getErrorMessage()}")
                    _uiState.value = _uiState.value.copy(
                        error = userData.getErrorMessage()
                    )
                }
            }
        }
    }
}

sealed interface DashboardAction {
    data class OnWhatsNewChange(val newValue: String) : DashboardAction
    data class OnTagFriendsChange(val newValue: String) : DashboardAction
    data class OnSourceChange(val newValue: String) : DashboardAction
    data class OnPostLikeClick(val postId: String) : DashboardAction
    data class OnPostDetailsLikeClick(val postId: String) : DashboardAction
    data class OnPostShareClick(val postId: String) : DashboardAction
    data class OnCommentLikeClick(val commentId: String, val postId: String) : DashboardAction
    data class OnCommentShareClick(val commentId: String) : DashboardAction
    data class OnRefreshSuggestedUsersClick(val users: List<User>) : DashboardAction
    data class OnSelectImages(val selectedImages: List<ImageItem>) : DashboardAction
    data class OnSelectVideos(val selectedVideos: List<VideoItem>) : DashboardAction
}

data class SuggestedUser(
    val id: String,
    val name: String,
    val handle: String,
    val avatar: String,
    val bio: String,
    val followers: String,
    val isVerified: Boolean = false
)