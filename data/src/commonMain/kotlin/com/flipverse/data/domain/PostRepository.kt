package com.flipverse.data.domain

import com.flipverse.shared.RequestState
import com.flipverse.shared.domain.CreatePostRequest
import com.flipverse.shared.domain.FeedPopulationStatus
import com.flipverse.shared.domain.MediaType
import com.flipverse.shared.domain.MediaUploadResult
import com.flipverse.shared.domain.Post
import com.flipverse.shared.domain.Comment
import com.flipverse.shared.domain.PostLike
import com.flipverse.shared.domain.PostWithComments
import com.flipverse.shared.domain.User
import dev.gitlive.firebase.storage.File
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    suspend fun getPostsFeedFlow(
        currentUserId: String,
        limit: Int = 20,
        lastPostTimestamp: String? = null,
    ): Flow<RequestState<List<Post>>>

    suspend fun refreshPostsFeedFlow(
        currentUserId: String,
        limit: Int = 20,
    ): Flow<RequestState<List<Post>>>

    suspend fun getFollowingCount(userId: String): Flow<RequestState<Int>>

    suspend fun getFollowingIds(userId: String): Flow<RequestState<List<String>>>

    suspend fun getUserFollowing(userId: String): Result<List<String>>

    suspend fun getUserPosts(userId: String): Flow<RequestState<List<Post>>>

    suspend fun getUserLikedPosts(userId: String): Flow<RequestState<List<Post>>>

    suspend fun uploadMedia(
        files: List<File>,
        types: List<MediaType>
    ): Flow<RequestState<List<MediaUploadResult>>>

    suspend fun createPost(request: CreatePostRequest, currentUser: User): Flow<RequestState<Post>>
    suspend fun addCommentToPost(
        postId: String,
        commentText: String,
        commentById: String,
        commentByHandle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    )

    suspend fun getPostWithComments(postId: String): Result<PostWithComments>
    suspend fun populateUserFeeds(
        post: Post,
        author: User
    ): Flow<RequestState<FeedPopulationStatus>>

    suspend fun getFollowerIds(userId: String): Flow<RequestState<List<String>>>
    fun getOnlyFollowingFeedFlow(
        userId: String,
        limit: Int,
        lastPostTimestamp: String? = null
    ): Flow<RequestState<List<Post>>>

    suspend fun pinPost(
        postId: String,
        userId: String,
        isPinned: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    )

    suspend fun deletePost(
        postId: String,
        userId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    )

    suspend fun hidePost(
        postId: String,
        userId: String,
        isHidden: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    )

    suspend fun updateCommentLike(
        postId: String,
        commentId: String,
        userId: String,
        like: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    )

    suspend fun addReplyToComment(
        postId: String,
        parentCommentId: String,
        replyText: String,
        replyById: String,
        replyByHandle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    )

    suspend fun getPostLikes(postId: String, limit: Int = 50): Result<List<PostLike>>
    suspend fun getRecentCommentsRaw(postId: String, limit: Int = 10): Result<List<Comment>>

    suspend fun reportPost(
        postId: String,
        reportedByUserId: String,
        reportedAuthorId: String? = null
    ): RequestState<Unit>
}
