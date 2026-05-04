package com.flipverse.data.domain

import com.flipverse.shared.RequestState
import com.flipverse.shared.domain.FeedItem
import com.flipverse.shared.domain.Post
import com.flipverse.shared.domain.SuggestionCriteria
import com.flipverse.shared.domain.TrendingCriteria
import com.flipverse.shared.domain.User
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    suspend fun getTrendingPosts(
        userId: String,
        limit: Int = 20,
        criteria: TrendingCriteria = TrendingCriteria()
    ): Result<List<Post>>

    suspend fun getSuggestedUsers(
        userId: String,
        limit: Int = 20,
        criteria: SuggestionCriteria = SuggestionCriteria()
    ): Result<List<User>>

    suspend fun getAllUsers(
        limit: Int = 100,
        lastUserId: String? = null
    ): Result<List<User>>

    suspend fun getUserInterests(userId: String): Result<List<String>>
    fun readProfileFlow(email: String): Flow<RequestState<User>>

    suspend fun getUserFollowing(userId: String): Result<List<String>>
    suspend fun getUserFollowers(userId: String): Result<List<String>>
    suspend fun updatePostEngagement(
        post: Post,
        interactionType: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )

    suspend fun updateFollowingStatus(
        currentUserId: String,
        targetUserId: String,
        isFollowing: Boolean
    ): Result<Unit>

    /**
     * Search users by username or fullname prefix.
     * Only fires when the user types — no bulk upfront fetch.
     * [query] is normalised (lowercased, @ stripped) inside the implementation.
     * [limit] caps results per field so responses stay cheap.
     */
    suspend fun searchUsers(query: String, limit: Int = 20): Result<List<User>>
}
