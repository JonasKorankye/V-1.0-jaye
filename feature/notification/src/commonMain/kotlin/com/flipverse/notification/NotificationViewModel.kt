package com.flipverse.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flipverse.data.domain.FeedRepository
import com.flipverse.data.domain.LiveBookRepository
import com.flipverse.data.domain.NotificationRepository
import com.flipverse.data.domain.PostRepository
import com.flipverse.data.domain.UserRepository
import com.flipverse.data.util.getCurrentTimeMillis
import com.flipverse.data.util.convertTimestampStringToEpochMilliseconds
import com.flipverse.shared.PreferencesRepository.getEmail
import com.flipverse.shared.domain.User
import com.flipverse.shared.domain.Post
import com.flipverse.shared.domain.LiveBook
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope

data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val actorUser: User? = null, // The user who performed the action
    val targetPost: Post? = null,
    val targetLiveBook: LiveBook? = null,
    val avatarUrl: String = "",
    val actionText: String = ""
)

enum class NotificationType {
    LIKE_POST,
    LIKE_COMMENT,
    COMMENT_POST,
    REPLY_COMMENT,
    TAG_POST,
    TAG_LIVEBOOK,
    LIVEBOOK_INVITATION,
    LIVEBOOK_CONTRIBUTION,
    FOLLOW,
    POST_SHARED
}

data class NotificationState(
    val notifications: List<NotificationItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val unreadCount: Int = 0,
    val lastLoadTime: Long = 0L
)


class NotificationViewModel(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val feedRepository: FeedRepository,
    private val notificationRepository: NotificationRepository,
    private val liveBookRepository: LiveBookRepository
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        println("NotificationViewModel coroutine exception: ${throwable::class.simpleName}: ${throwable.message}")
        throwable.printStackTrace()
    }

    private val _uiState = MutableStateFlow(NotificationState())
    val uiState: StateFlow<NotificationState> = _uiState.asStateFlow()

    // Cache for users to avoid redundant fetches
    private val usersCache = mutableMapOf<String, User>()

    // Cache for last loaded notifications — show instantly on revisit
    private var cachedNotifications: List<NotificationItem> = emptyList()

    // Debounce time to prevent multiple rapid loads
    private val loadDebounceMs = 3000L // 3 seconds
    private var loadNotificationsJob: kotlinx.coroutines.Job? = null

    companion object {
        fun clearAllCaches() {
            // Called on logout/account switch
        }
    }

    init {
        loadNotifications()
    }

    fun onResume() {
        refreshNotifications()
    }

    fun loadNotifications(forceRefresh: Boolean = false) {
        loadNotificationsJob?.cancel()

        loadNotificationsJob = viewModelScope.launch(exceptionHandler + kotlinx.coroutines.Dispatchers.IO) {
            try {
                val currentTime = getCurrentTimeMillis()
                val timeSinceLastLoad = currentTime - _uiState.value.lastLoadTime

                if (!forceRefresh && timeSinceLastLoad < loadDebounceMs) {
                    return@launch
                }

                // Show cached notifications instantly (no loading spinner on revisit)
                if (cachedNotifications.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        notifications = cachedNotifications,
                        unreadCount = cachedNotifications.count { !it.isRead },
                        isLoading = false,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                }

                val currentUser: User = try {
                    val userResult = userRepository.readUserProfileFlow().first()
                    if (userResult.isSuccess()) {
                        userResult.getSuccessData()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to load user profile"
                        )
                        return@launch
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load user profile"
                    )
                    return@launch
                }

                // Load users, notifications, and read status ALL in parallel
                val notifications = mutableListOf<NotificationItem>()

                coroutineScope {
                    // Populate user cache in parallel (only if cache is empty)
                    val usersCacheDeferred = async {
                        if (usersCache.isEmpty()) {
                            try {
                                feedRepository.getAllUsers(limit = 200).onSuccess { users ->
                                    users.forEach { user ->
                                        usersCache[user.id] = user
                                        usersCache[user.email] = user
                                    }
                                }
                            } catch (_: Exception) { }
                        }
                    }

                    // Fetch read IDs in parallel
                    val readIdsDeferred = async {
                        try {
                            notificationRepository.getReadNotificationIds(currentUser.email)
                        } catch (_: Exception) {
                            emptySet()
                        }
                    }

                    // Wait for user cache before launching notification loaders
                    usersCacheDeferred.await()

                    val postNotificationsDeferred = async {
                        loadPostNotifications(currentUser)
                    }
                    val taggedNotificationsDeferred = async {
                        loadTaggedNotifications(currentUser)
                    }
                    val liveBookNotificationsDeferred = async {
                        loadLiveBookNotifications(currentUser)
                    }

                    try {
                        notifications.addAll(postNotificationsDeferred.await())
                        notifications.addAll(taggedNotificationsDeferred.await())
                        notifications.addAll(liveBookNotificationsDeferred.await())
                    } catch (_: Exception) { }

                    // Apply read status (already loaded in parallel)
                    val readIds = readIdsDeferred.await()
                    val merged = notifications
                        .sortedByDescending { it.timestamp }
                        .map { n ->
                            if (readIds.contains(n.id)) n.copy(isRead = true) else n
                        }

                    // Cache and update UI
                    cachedNotifications = merged
                    _uiState.value = _uiState.value.copy(
                        notifications = merged,
                        unreadCount = merged.count { !it.isRead },
                        isLoading = false,
                        lastLoadTime = currentTime
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load notifications"
                )
            }
        }
    }

    /** Resolve a user by ID or email from the pre-populated cache. */
    private fun resolveUser(userId: String): User? = usersCache[userId]

    /**
     * Post notifications — likes and comments on user's posts.
     * Fetches likes/comments for all posts IN PARALLEL instead of sequentially.
     */
    private suspend fun loadPostNotifications(
        currentUser: User
    ): List<NotificationItem> {
        val notifications = mutableListOf<NotificationItem>()
        try {
            postRepository.getUserPosts(currentUser.email).collect { result ->
                if (result.isSuccess()) {
                    val userPosts = result.getSuccessData().take(10)
                    if (userPosts.isEmpty()) return@collect

                    // Fetch likes + comments for ALL posts in parallel
                    coroutineScope {
                        val postJobs = userPosts.map { post ->
                            async {
                                val postItems = mutableListOf<NotificationItem>()
                                val postTimestamp = safeConvertTimestamp(post.timestamp)
                                val postSnippet = buildPostSnippet(post)

                                // Likes and comments for this post in parallel
                                val likesDeferred = async {
                                    try {
                                        postRepository.getPostLikes(post.id, limit = 10)
                                            .getOrNull() ?: emptyList()
                                    } catch (_: Exception) { emptyList() }
                                }

                                val commentsDeferred = async {
                                    if (post.engagement.commentsCount > 0) {
                                        try {
                                            postRepository.getRecentCommentsRaw(post.id, limit = 10)
                                                .getOrNull() ?: emptyList()
                                        } catch (_: Exception) { emptyList() }
                                    } else emptyList()
                                }

                                // Process likes
                                likesDeferred.await().forEach { like ->
                                    val liker = resolveUser(like.userId)
                                    val likeTs = safeConvertTimestamp(like.timestamp)
                                    postItems.add(
                                        NotificationItem(
                                            id = "like_post_${post.id}_${liker?.id ?: like.userId}_${likeTs}",
                                            type = NotificationType.LIKE_POST,
                                            title = "Post Liked",
                                            message = "${liker?.fullname ?: "Someone"} liked your post: \"$postSnippet\"",
                                            timestamp = likeTs.takeIf { it > 0 } ?: postTimestamp,
                                            targetPost = post,
                                            avatarUrl = liker?.thumbnail ?: "",
                                            actionText = "liked your post",
                                            actorUser = liker
                                        )
                                    )
                                }

                                // Process comments
                                val comments = commentsDeferred.await()
                                if (comments.isNotEmpty()) {
                                    comments.forEach { comment ->
                                        postItems.add(
                                            NotificationItem(
                                                id = "comment_${comment.id}",
                                                type = NotificationType.COMMENT_POST,
                                                title = "New Comment",
                                                message = "${comment.commentBy} commented on your post: \"$postSnippet\"",
                                                timestamp = safeConvertTimestamp(comment.timestamp),
                                                targetPost = post,
                                                avatarUrl = comment.commentThumbnail,
                                                actionText = "commented on your post"
                                            )
                                        )
                                    }
                                } else if (post.engagement.commentsCount > 0) {
                                    // Fallback aggregate
                                    postItems.add(
                                        NotificationItem(
                                            id = "comment_post_${post.id}",
                                            type = NotificationType.COMMENT_POST,
                                            title = "New Comments",
                                            message = "Your post received ${post.engagement.commentsCount} comment(s): \"$postSnippet\"",
                                            timestamp = postTimestamp,
                                            targetPost = post,
                                            avatarUrl = "",
                                            actionText = "commented on your post"
                                        )
                                    )
                                }

                                postItems
                            }
                        }

                        // Await all post jobs and flatten results
                        postJobs.forEach { notifications.addAll(it.await()) }
                    }
                }
            }
        } catch (_: Exception) { }
        return notifications
    }

    /**
     * Tagged notifications — posts where user is mentioned.
     * Uses feed posts already loaded, just filters for tags. Reduced limit.
     */
    private suspend fun loadTaggedNotifications(
        currentUser: User
    ): List<NotificationItem> {
        val notifications = mutableListOf<NotificationItem>()
        try {
            postRepository.getPostsFeedFlow(currentUser.email, 15).collect { postsResult ->
                if (postsResult.isSuccess()) {
                    val allPosts = postsResult.getSuccessData()
                    if (allPosts.isEmpty()) return@collect

                    allPosts.forEach { post ->
                        if (post.authorId == currentUser.id) return@forEach

                        val isTaggedInContent = post.whatsNew.contains(
                            "@${currentUser.username}",
                            ignoreCase = true
                        ) || post.whatsNew.contains(currentUser.fullname, ignoreCase = true)

                        val isTaggedInTags = post.tags.any { tag ->
                            tag.contains(currentUser.username, ignoreCase = true) ||
                                    tag.contains(currentUser.fullname, ignoreCase = true)
                        }

                        if (isTaggedInContent || isTaggedInTags) {
                            val authorUser = resolveUser(post.authorId)
                            notifications.add(
                                NotificationItem(
                                    id = "tag_post_${post.id}",
                                    type = NotificationType.TAG_POST,
                                    title = "Tagged in Post",
                                    message = "${authorUser?.fullname ?: "Someone"} tagged you in a post: \"${buildPostSnippet(post)}\"",
                                    timestamp = safeConvertTimestamp(post.timestamp),
                                    targetPost = post,
                                    avatarUrl = authorUser?.thumbnail ?: "",
                                    actionText = "tagged you in a post",
                                    actorUser = authorUser
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) { }
        return notifications
    }

    /**
     * LiveBook notifications — invitations and contributions.
     * Both livebook queries run in parallel.
     */
    private suspend fun loadLiveBookNotifications(
        currentUser: User
    ): List<NotificationItem> {
        val notifications = mutableListOf<NotificationItem>()
        try {
            // Run both livebook queries in parallel
            coroutineScope {
                val invitationsDeferred = async {
                    val items = mutableListOf<NotificationItem>()
                    try {
                        liveBookRepository.fetchActiveLiveBooks(currentUser.email).let { result ->
                            if (result.isSuccess()) {
                                result.getSuccessData().forEach { liveBook ->
                                    val isInvited = liveBook.taggedUsers.any { it.id == currentUser.id } &&
                                            liveBook.authorId != currentUser.id
                                    if (isInvited) {
                                        items.add(
                                            NotificationItem(
                                                id = "livebook_invite_${liveBook.id}",
                                                type = NotificationType.LIVEBOOK_INVITATION,
                                                title = "Story Challenge Invitation",
                                                message = "${liveBook.authorName} invited you to contribute to \"${liveBook.title}\"",
                                                timestamp = safeConvertTimestamp(liveBook.createdAt.toString()),
                                                targetLiveBook = liveBook,
                                                avatarUrl = liveBook.authorThumbnail,
                                                actionText = "invited you to contribute to a story"
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) { }
                    items
                }

                val contributionsDeferred = async {
                    val items = mutableListOf<NotificationItem>()
                    try {
                        liveBookRepository.getUserLiveBooks(currentUser.email).let { result ->
                            if (result.isSuccess()) {
                                result.getSuccessData().forEach { liveBook ->
                                    val contributorIds = listOf(
                                        liveBook.paragraph1ContributorId,
                                        liveBook.paragraph2ContributorId,
                                        liveBook.paragraph3ContributorId,
                                        liveBook.paragraph4ContributorId,
                                        liveBook.paragraph5ContributorId,
                                        liveBook.paragraph6ContributorId
                                    ).filter { it.isNotEmpty() && it != currentUser.id }

                                    contributorIds.forEach { contributorId ->
                                        val contributor = resolveUser(contributorId)
                                        items.add(
                                            NotificationItem(
                                                id = "livebook_contribution_${liveBook.id}_${contributorId}",
                                                type = NotificationType.LIVEBOOK_CONTRIBUTION,
                                                title = "New Story Contribution",
                                                message = "${contributor?.fullname ?: "Someone"} contributed to \"${liveBook.title}\"",
                                                timestamp = safeConvertTimestamp(liveBook.createdAt.toString()),
                                                targetLiveBook = liveBook,
                                                avatarUrl = contributor?.thumbnail ?: "",
                                                actionText = "contributed to your story",
                                                actorUser = contributor
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) { }
                    items
                }

                notifications.addAll(invitationsDeferred.await())
                notifications.addAll(contributionsDeferred.await())
            }
        } catch (_: Exception) { }
        return notifications
    }

    private fun buildPostSnippet(post: Post): String {
        return if (post.whatsNew.length > 50) {
            post.whatsNew.substring(0, 50) + "..."
        } else {
            post.whatsNew
        }
    }

    private fun safeConvertTimestamp(timestamp: Any?): Long {
        return try {
            when (timestamp) {
                is Long -> timestamp
                is String -> convertTimestampStringToEpochMilliseconds(timestamp)
                    ?: getCurrentTimeMillis()

                is Int -> timestamp.toLong()
                else -> getCurrentTimeMillis()
            }
        } catch (e: Exception) {
            println("Error converting timestamp: $timestamp, ${e.message}")
            getCurrentTimeMillis()
        }
    }

    fun markAsRead(notificationId: String) {
        val currentNotifications = _uiState.value.notifications
        val updatedNotifications = currentNotifications.map { notification ->
            if (notification.id == notificationId) notification.copy(isRead = true)
            else notification
        }

        _uiState.value = _uiState.value.copy(
            notifications = updatedNotifications,
            unreadCount = updatedNotifications.count { !it.isRead }
        )
        // Write to Firebase
        viewModelScope.launch(exceptionHandler) {
            try {
                notificationRepository.markNotificationAsRead(getEmail(), notificationId)
            } catch (_: Exception) {
            }
        }
    }

    fun markAllAsRead() {
        val updatedNotifications = _uiState.value.notifications.map {
            it.copy(isRead = true)
        }

        _uiState.value = _uiState.value.copy(
            notifications = updatedNotifications,
            unreadCount = 0
        )
        // Write all to Firebase
        viewModelScope.launch(exceptionHandler) {
            try {
                notificationRepository.markAllNotificationsAsRead(
                    getEmail(),
                    updatedNotifications.map { it.id }
                )
            } catch (_: Exception) {
            }
        }
    }

    fun refreshNotifications() {
        loadNotifications(forceRefresh = true)
    }

    // Clear cache when needed
    fun clearCache() {
        usersCache.clear()
        println("🗑️ Notification cache cleared")
    }

    override fun onCleared() {
        super.onCleared()
        loadNotificationsJob?.cancel()
        // Don't clear cache on onCleared, only on explicit logout
        // usersCache.clear()
    }
}