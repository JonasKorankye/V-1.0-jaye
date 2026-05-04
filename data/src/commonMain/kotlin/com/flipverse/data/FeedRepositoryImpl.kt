package com.flipverse.data

import com.flipverse.data.domain.FeedRepository
import com.flipverse.data.util.normalizeUsername
import com.flipverse.data.util.randomFirestoreId
import com.flipverse.data.util.toFirebaseTimestamp
import com.flipverse.data.util.toFormattedString
import com.flipverse.data.util.CrashlyticsLogger
import com.flipverse.shared.PreferencesRepository.getEmail
import com.flipverse.shared.RequestState
import com.flipverse.shared.domain.FeedItem
import com.flipverse.shared.domain.Follow
import com.flipverse.shared.domain.Post
import com.flipverse.shared.domain.SuggestionCriteria
import com.flipverse.shared.domain.TrendingCriteria
import com.flipverse.shared.domain.User
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.where
import dev.gitlive.firebase.functions.functions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours

class FeedRepositoryImpl : FeedRepository {

    private val usersCollection = Firebase.firestore.collection("user")
    private val postCollection = Firebase.firestore.collection("post")
    private val feedCollection = Firebase.firestore.collection("user_feeds")
    private val followsCollection = Firebase.firestore.collection("follows")

    /**
     * Helper function to check if the current user has liked a specific post
     * by querying the likes subcollection
     */
    private suspend fun isPostLikedByUser(postId: String, userId: String): Boolean {
        return try {
            val likeDoc = postCollection.document(postId)
                .collection("likes")
                .document(userId)
                .get()
            likeDoc.exists
        } catch (e: Exception) {
            println("Error checking if post $postId is liked by user $userId: ${e.message}")
            false
        }
    }

    override suspend fun getTrendingPosts(
        userId: String,
        limit: Int,
        criteria: TrendingCriteria
    ): Result<List<Post>> {
        return try {
            val now = Clock.System.now()
            val currentTimestamp = now.toFirebaseTimestamp()
            val timeThreshold = now.minus(criteria.timeWindowHours.hours)
            val timeThresholdTimestamp = Timestamp(timeThreshold.epochSeconds, 0)

            // 1. Get current user's interests
            val userInterests = getUserInterests(userId).getOrElse { emptyList() }
            val following = getUserFollowing(userId).getOrElse { emptyList() }

            // If nothing to show, short-circuit
            if (userInterests.isEmpty() && following.isEmpty())
                return Result.success(emptyList())

            // 2. Query posts matching interests/recency (trending)
            val trendingPosts = if (userInterests.isNotEmpty()) {
                postCollection
//                    .where("selectedInterests", "array-contains-any", userInterests)
                    .where("selectedInterests", arrayContainsAny = userInterests)
                    .where { "timestamp" greaterThanOrEqualTo timeThresholdTimestamp.toFormattedString() }
                    .orderBy("timestamp", Direction.DESCENDING)
                    .limit(limit * 2)
                    .get()
                    .documents
                    .mapNotNull { doc -> doc.data<Post>().copy(id = doc.id) }
            } else emptyList()

            // 3. Query posts from followed users (recency-based)
            val followedPosts = if (following.isNotEmpty()) {
                // Firestore requires <= 10 for "in" clause, so handle batching
                following.chunked(10).flatMap { batch ->
                    postCollection
                        .where("authorId", "in", batch)
                        .where { "timestamp" greaterThanOrEqualTo timeThresholdTimestamp.toFormattedString() }
                        .orderBy("timestamp", Direction.DESCENDING)
                        .limit(limit)
                        .get()
                        .documents
                        .mapNotNull { doc -> doc.data<Post>().copy(id = doc.id) }
                }
            } else emptyList()

            // 4. Merge & deduplicate posts by id, sort by timestamp or engagement
            val allPosts = (trendingPosts + followedPosts)
                .distinctBy { it.id }
                .sortedByDescending { it.timestamp }
                .take(limit)
                .map { post ->
                    // Check if current user has liked this post
                    val isLiked = isPostLikedByUser(post.id, userId)
                    post.copy(
                        engagement = post.engagement.copy(
                            isLikedByCurrentUser = isLiked
                        )
                    )
                }

            // Update last active timestamp
            try {
                val userQuery = usersCollection
                    .where { "email" equalTo userId.lowercase() }
                    .get()
                if (userQuery.documents.isNotEmpty()) {
                    val userDoc = userQuery.documents.first()
                    usersCollection
                        .document(userDoc.id)
                        .update("lastActiveAt" to currentTimestamp.toFormattedString())
                }
            } catch (e: Exception) {
                // just log, don't fail trending due to this
            }

            println("Trending (with following) posts: $allPosts")

            Result.success(allPosts)
        } catch (e: Exception) {
            // Log to Crashlytics for debugging
            CrashlyticsLogger.logNonFatal(
                error = e,
                context = "getTrendingPosts (Dashboard Feed)",
                additionalInfo = mapOf(
                    "user_id" to userId,
                    "limit" to limit.toString(),
                    "time_window_hours" to criteria.timeWindowHours.toString()
                )
            )
            Result.failure(e)
        }
    }


    override suspend fun getSuggestedUsers(
        userId: String,
        limit: Int,
        criteria: SuggestionCriteria
    ): Result<List<User>> {
        return try {
            // 1. Get current user's interests
            val userInterests = getUserInterests(userId).getOrElse { emptyList() }
            println("User interests FeedRepoImpl : $userInterests")

            // 2. Get user's current following list
            val following = getUserFollowing(userId).getOrElse { emptyList() }
            println("User following FeedRepoImpl : $following")

            // 3. Query users — by shared interests if available, otherwise fetch all users
            val users = if (userInterests.isNotEmpty()) {
                // Firestore array-contains-any supports only up to 10 values!
                userInterests.chunked(10).flatMap { chunk ->
                    usersCollection
                        .where("selectedInterests", arrayContainsAny = chunk)
                        .limit(limit * 5)
                        .get()
                        .documents
                        .mapNotNull { doc ->
                            try {
                                val user = doc.data<User>().copy(id = doc.id)
                                // Exclude self and already-followed users
                                if (user.email != userId && !following.contains(user.email))
                                    user
                                else null
                            } catch (e: Exception) {
                                null
                            }
                        }
                }.distinctBy { it.id }
            } else {
                // No interests set — fall back to fetching all users so suggestions aren't empty
                usersCollection
                    .orderBy("createdAt", Direction.DESCENDING)
                    .limit(limit * 3)
                    .get()
                    .documents
                    .mapNotNull { doc ->
                        try {
                            val user = doc.data<User>().copy(id = doc.id)
                            if (user.email != userId && !following.contains(user.email))
                                user
                            else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                    .distinctBy { it.id }
            }

            // Limit to the supplied limit
            println("Suggested users from FeedRepoImpl: $users")
            Result.success(users.take(limit))
        } catch (e: Exception) {
            // Log to Crashlytics for debugging
            CrashlyticsLogger.logNonFatal(
                error = e,
                context = "getSuggestedUsers (Home/Explore)",
                additionalInfo = mapOf(
                    "user_id" to userId,
                    "limit" to limit.toString(),
                    "min_followers" to criteria.minFollowers.toString()
                )
            )
            Result.failure(e)
        }
    }

    override suspend fun getUserInterests(userId: String): Result<List<String>> {
        return try {
            val userQuery = usersCollection
                .where { "email" equalTo userId.lowercase().trim() }
                .limit(1)
                .get()

            if (userQuery.documents.isEmpty()) {
                return Result.failure(Exception("User not found"))
            }

            val userDoc = userQuery.documents.first()
            val selectedInterests = userDoc.get("selectedInterests") as? List<String> ?: emptyList()

            Result.success(selectedInterests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserFollowing(userId: String): Result<List<String>> {
        return try {
            val followingQuery = followsCollection
                .where { "followerId" equalTo userId }
                .limit(1000)

            val following = followingQuery.get().documents.mapNotNull { doc ->
                try {
                    doc.data<Follow>().followingId
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(following)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserFollowers(userId: String): Result<List<String>> {
        return try {
            // Get all follow documents where followingId = userId
            val followersQuery = followsCollection
                .where { "followingId" equalTo userId }
                .limit(1000)
                .get()

            // Extract follower IDs (emails)
            val followerIds = followersQuery.documents.mapNotNull { doc ->
                try {
                    doc.data<Follow>().followerId
                } catch (e: Exception) {
                    null
                }
            }

            if (followerIds.isEmpty()) {
                return Result.success(emptyList())
            }


            Result.success(followerIds)

        } catch (e: Exception) {
            println("Error fetching followers: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun updatePostEngagement(
        post: Post,
        interactionType: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            println("post: $post")

            val postRef = postCollection.document(post.id)
            //use Post Id to fetch user_feed Id
//            val feedQuery = feedCollection
//                .where { "postId" equalTo post.id }
//                .limit(1)
//                .get()
//            val userFeedDoc = feedQuery.documents.first()
//            val userFeedItem = userFeedDoc.data<Post>()
//            val feedRef = feedCollection.document(userFeedItem.id)


            when (interactionType) {
                "like" -> {
                    postRef.update(
                        "engagement.likesCount" to post.engagement.likesCount,
                        "engagement.isLikedByCurrentUser" to post.engagement.isLikedByCurrentUser
                    )

//                    feedRef.update(
//                        "engagement.likesCount" to post.engagement.likesCount,
//                        "engagement.isLikedByCurrentUser" to post.engagement.isLikedByCurrentUser
//                    )
                    // Persist per-user like/unlike
                    try {
                        val currentUserId = getEmail()
                        if (currentUserId.isNotEmpty()) {
                            // Resolve current user doc id
                            val currentUserQuery = usersCollection
                                .where { "email" equalTo currentUserId.lowercase().trim() }
                                .limit(1)
                                .get()

                            val currentUserDocId = currentUserQuery.documents.firstOrNull()?.id

                            val nowTs = Clock.System.now().toFirebaseTimestamp().toFormattedString()
                            val postLikesRef = postCollection.document(post.id).collection("likes")
                                .document(currentUserId)

                            if (post.engagement.isLikedByCurrentUser) {
                                // Like: add to post likes and user's liked_posts
                                postLikesRef.set(
                                    mapOf(
                                        "userId" to currentUserId,
                                        "timestamp" to nowTs
                                    )
                                )

                                if (currentUserDocId != null) {
                                    usersCollection.document(currentUserDocId)
                                        .collection("liked_posts")
                                        .document(post.id)
                                        .set(
                                            mapOf(
                                                "postId" to post.id,
                                                "timestamp" to nowTs
                                            )
                                        )
                                }
                            } else {
                                // Unlike: remove from post likes and user's liked_posts
                                postLikesRef.delete()
                                if (currentUserDocId != null) {
                                    usersCollection.document(currentUserDocId)
                                        .collection("liked_posts")
                                        .document(post.id)
                                        .delete()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Do not fail engagement update due to like persistence issues
                        println("Error persisting per-user like: ${e.message}")
                    }
                    
                    //send a like push notification to original user who posted
                    try {
                        val postDoc = postCollection.document(post.id).get()
                        val postData = postDoc.data(Post.serializer())
                        val recipientId = postData.authorId

                        val currentUserId = getEmail()
                        
                        // Only send notification if user is not liking their own post
                        if (currentUserId != recipientId && post.engagement.isLikedByCurrentUser) {
                            var likerHandle = currentUserId // fallback
                            // Fetch handle if possible
                            val currentUserQuery = usersCollection
                                .where { "email" equalTo currentUserId.lowercase().trim() }
                                .limit(1)
                                .get()
                            val currentUser = currentUserQuery.documents.firstOrNull()?.data<User>()
                            likerHandle = normalizeUsername(currentUser?.username ?: currentUserId)
                            val likerId = currentUserId

                            val notificationContent = "$likerHandle liked your post: "

                            val data = mapOf(
                                "recipientId" to recipientId,
                                "title" to notificationContent,
                                "body" to postData.whatsNew,
                                "type" to "like_notification",
                                "senderId" to likerId,
                                "messageId" to post.id,
                                "customData" to "like_notification"
                            )

                            val callable = Firebase.functions.httpsCallable("sendPushNotification")
                            callable(data)
                        }
                    } catch (e: Exception) {
                        println("❌ Failed to send like/share notification: ${e.message}")
                    }


                }

                "comment" -> {
                    postRef.update("engagement.commentsCount" to post.engagement.commentsCount)
//                    feedRef.update("engagement.commentsCount" to post.engagement.commentsCount)
                }

                "share" -> {
                    postRef.update(
                        "engagement.sharesCount" to post.engagement.sharesCount,
                        "engagement.isSharedByCurrentUser" to post.engagement.isSharedByCurrentUser
                    )
//                    feedRef.update(
//                        "engagement.sharesCount" to post.engagement.sharesCount,
//                        "engagement.isSharedByCurrentUser" to post.engagement.isSharedByCurrentUser
//                    )
                }

                "view" -> {
                    postRef.update("engagement.viewsCount" to post.engagement.viewsCount)
//                    feedRef.update("engagement.viewsCount" to post.engagement.viewsCount)
                }
            }

            // Recalculate engagement score
//            val posts = postRef.get().data<Post>()
//            val newEngagementScore = calculateEngagementScore(posts)
//            postRef.update("engagementScore" to newEngagementScore)
//
//            val feeds = feedRef.get().data<Post>()
//            val newFeedEngagementScore = calculateEngagementScore(feeds)
//            feedRef.update(
//                "engagementScore" to newFeedEngagementScore
//            )

            onSuccess()

        } catch (e: Exception) {
            // Handle error silently for engagement updates
            println("Error updating post engagement: ${e.message}")
            onError("Error updating post engagement: ${e.message}")
        }

    }

    override suspend fun updateFollowingStatus(
        currentUserId: String,
        targetUserId: String,
        isFollowing: Boolean
    ): Result<Unit> {
        return try {
            println("Current User ID: $currentUserId")
            println("Target User ID: $targetUserId")
            println("Is Following: $isFollowing")

            // Query for current user's doc ID
            val currentUserQuery = usersCollection
                .where { "email" equalTo currentUserId.lowercase().trim() }
                .limit(1)
                .get()
            if (currentUserQuery.documents.isEmpty()) {
                return Result.failure(Exception("Current user not found"))
            }
            val currentUserDocId = currentUserQuery.documents.first().id

            // Query for target user's doc ID
            val targetUserQuery = usersCollection
                .where { "email" equalTo targetUserId.lowercase().trim() }
                .limit(1)
                .get()
            if (targetUserQuery.documents.isEmpty()) {
                return Result.failure(Exception("Target user not found"))
            }
            val targetUserDocId = targetUserQuery.documents.first().id

            val userRef = usersCollection.document(currentUserDocId)
            val targetUserRef = usersCollection.document(targetUserDocId)

            // Each follow relationship is a single document: { followerId: currentUserId, followingId: targetUserId }
            val followQuery = followsCollection
                .where { "followerId" equalTo currentUserId }
                .where { "followingId" equalTo targetUserId }
                .limit(1)

            val existingDocs = followQuery.get().documents
            val batch = Firebase.firestore.batch()

            println("Following: $isFollowing")

            if (isFollowing) {
                // Only add if not already following
                println("Existing Docs: ${existingDocs.size}")
                if (existingDocs.isEmpty()) {
                    val newDocId = randomFirestoreId()
                    val newDoc = followsCollection.document(newDocId)
                    batch.set(
                        newDoc, mapOf(
                            "followerId" to currentUserId,
                            "followingId" to targetUserId,
                            "followedAt" to Clock.System.now().toFirebaseTimestamp(),
                            "isActive" to true
                        )
                    )
                    batch.update(
                        userRef, mapOf(
                            "followingCount" to FieldValue.increment(1)
                        )
                    )
                    batch.update(
                        targetUserRef, mapOf(
                            "followersCount" to FieldValue.increment(1)
                        )
                    )

                    // Send push notification to the user who is being followed
                    try {
                        // Fetch sender's username
                        val senderQuery = usersCollection
                            .where { "email" equalTo currentUserId.lowercase().trim() }
                            .limit(1)
                            .get()
                        val senderUser = senderQuery.documents.firstOrNull()?.data<User>()
                        val senderHandle = normalizeUsername(senderUser?.username ?: currentUserId)

                        val notificationBody = "${senderHandle} started following you."
                        val notificationTitle = "New follower"
                        val data = mapOf(
                            "recipientId" to targetUserId,
                            "title" to notificationTitle,
                            "body" to notificationBody,
                            "type" to "follow_notification",
                            "senderId" to currentUserId,
                            "customData" to "follow_notification"
                        )
                        val callable = Firebase.functions.httpsCallable("sendPushNotification")
                        callable(data)
                    } catch (e: Exception) {
                        println("❌ Failed to send follow notification: ${e.message}")
                    }
                }
            } else {
                // Only remove if currently following
                if (existingDocs.isNotEmpty()) {
                    val toDelete = existingDocs.first().reference
                    batch.delete(toDelete)
                    batch.update(
                        userRef, mapOf(
                            "followingCount" to FieldValue.increment(-1)
                        )
                    )
                    batch.update(
                        targetUserRef, mapOf(
                            "followersCount" to FieldValue.increment(-1)
                        )
                    )
                }
            }

            // Optionally: update followsCount in followsCollection if modeled elsewhere

            batch.commit()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // Helper Methods for Scoring Algorithms
    private fun calculateTrendingScore(
        post: Post,
        criteria: TrendingCriteria,
        userInterests: List<String>,
        currentTime: Instant
    ): Double {
        val postTime = Instant.parse(post.createdAt)
        val ageHours = (currentTime - postTime).inWholeHours.toDouble()

        // Recency score (higher for newer posts, decay over time)
        val recencyScore = kotlin.math.exp(-ageHours / 12.0) // 12-hour half-life

        // Engagement score (normalized)
        val totalEngagements =
            post.engagement.likesCount + post.engagement.commentsCount + (post.engagement.sharesCount * 2)
        val engagementRate = if (post.engagement.viewsCount > 0) {
            totalEngagements.toDouble() / post.engagement.viewsCount
        } else {
            totalEngagements.toDouble() / 100.0 // baseline assumption
        }
        val engagementScore = kotlin.math.min(engagementRate * 10, 1.0)

        // Quality score (from content analysis, user reputation, etc.)
        val qualityScore = post.qualityScore

        // Category relevance score
        val categoryScore = if (userInterests.isEmpty()) {
            0.5 // neutral if no interests known
        } else {
            val matchingCategories = post.selectedInterests.intersect(userInterests.toSet()).size
            kotlin.math.min(matchingCategories.toDouble() / userInterests.size, 1.0)
        }

        // Weighted final score
        return (recencyScore * criteria.recencyWeight) +
                (engagementScore * criteria.engagementWeight) +
                (qualityScore * criteria.qualityWeight) +
                (categoryScore * 0.2) // Additional personalization weight
    }


    private suspend fun calculateSuggestionScore(
        user: User,
        currentUserId: String,
        userInterests: List<String>,
        following: List<String>,
        criteria: SuggestionCriteria
    ): Double {
        // Category match score
        val categoryScore = if (userInterests.isEmpty()) {
            0.5
        } else {
            val matchingCategories = user.selectedInterests.intersect(userInterests.toSet()).size
            kotlin.math.min(matchingCategories.toDouble() / userInterests.size, 1.0)
        }

        // Activity score (based on recent activity)
        val now = Clock.System.now()
        val lastActiveHours = (now - Instant.parse(user.lastActiveAt)).inWholeHours.toDouble()
        val activityScore = kotlin.math.exp(-lastActiveHours / 168.0) // 1-week half-life

        // Follower ratio score (avoid too popular or too small accounts)
        val followerScore = when {
            user.followersCount < criteria.minFollowers -> 0.0
            user.followersCount > criteria.maxFollowers -> 0.3
            user.followersCount in 1000..50000 -> 1.0 // sweet spot
            else -> 0.7
        }

        // Engagement and quality scores
        val engagementScore = kotlin.math.min(user.engagementRate * 50, 1.0)
        val qualityScore = user.qualityScore

        return (categoryScore * criteria.categoryMatchWeight) +
                (activityScore * criteria.activityWeight) +
                (followerScore * 0.2) +
                (engagementScore * 0.2) +
                (qualityScore * 0.2)
    }

    private suspend fun calculateMutualConnectionScore(
        targetUserId: String,
        userFollowing: List<String>
    ): Double {
        return try {
            // Get who the target user follows
            val targetFollowing = getUserFollowing(targetUserId).getOrElse { emptyList() }

            // Calculate mutual connections
            val mutualConnections = userFollowing.intersect(targetFollowing.toSet()).size

            // Score based on mutual connections (logarithmic scale)
            kotlin.math.min(kotlin.math.ln(mutualConnections.toDouble() + 1) / 3.0, 1.0)

        } catch (e: Exception) {
            0.0
        }
    }

    private fun calculateEngagementScore(post: Post): Double {
        val totalEngagements =
            post.engagement.likesCount + post.engagement.commentsCount + (post.engagement.sharesCount * 2)
        val views =
            kotlin.math.max(post.engagement.viewsCount, totalEngagements * 10) // minimum assumption

        return kotlin.math.min(totalEngagements.toDouble() / views, 1.0)
    }

    private fun createPostItem(post: FeedItem, author: User): Post {
        return Post(
            id = post.id, // Composite ID for easy querying
            authorId = post.authorId,
            authorName = author.fullname,
            authorHandle = author.username,
            authorProfileImage = post.authorProfileImage ?: author.thumbnail,
            authorIsVerified = author.isVerified,
            source = post.source,
            whatsNew = post.content,
            timestamp = post.timestamp,
            createdAt = post.createdAt,
            imageUrls = post.imageUrls,
            videoUrl = post.videoUrl,
            thumbnailUrl = post.thumbnailUrl,
            postType = post.postType,
            tags = post.tags,
            engagement = post.engagement.copy(),
            privacy = post.privacy,
        )
    }



    override fun readProfileFlow(email: String): Flow<RequestState<User>> = channelFlow {
        try {
            val database = Firebase.firestore
            val userCollection = database.collection(collectionPath = "user")


            // Check if the email parameter contains '@' character
            val actualEmail = if (email.contains("@")) {
                // It's already an email, use as is
                email.lowercase().trim()
            } else {
                // It might be a username or user ID, query to find the email
                try {
                    val usernameQuery = userCollection
                        .where { "username" equalTo email.lowercase().trim() }
                        .limit(1)
                        .get()

                    if (usernameQuery.documents.isNotEmpty()) {
                        val userDoc = usernameQuery.documents.first()
                        val user = userDoc.data<User>()
                        user.email.lowercase().trim()
                    } else {
                        // If not found by username, try by user ID
                        val idQuery = userCollection
                            .where { "id" equalTo email.trim() }
                            .limit(1)
                            .get()

                        if (idQuery.documents.isNotEmpty()) {
                            val userDoc = idQuery.documents.first()
                            val user = userDoc.data<User>()
                            user.email.lowercase().trim()
                        } else {
                            // Not found, use original parameter as fallback
                            email.lowercase().trim()
                        }
                    }
                } catch (e: Exception) {
                    println("Error resolving user identifier: ${e.message}")
                    email.lowercase().trim()
                }
            }

            val userQuery = userCollection
                .where { "email" equalTo actualEmail }
                .limit(1)
                .get()

            if (userQuery.documents.isEmpty()) {
                println("Empty user query for email: $actualEmail")
                send(RequestState.Error("Queried FlipVerse user profile not found."))
                return@channelFlow
            }

            val userDoc = userQuery.documents.first()
            val user = userDoc.data<User>()

            if (userDoc.exists) {
                val selectedInterests =
                    userDoc.get("selectedInterests") as? List<String> ?: emptyList()
                val selectedSuggestions =
                    userDoc.get("selectedSuggestions") as? List<String> ?: emptyList()
                println("selectedInterests: $selectedInterests")
                println("selectedSuggestions: $selectedSuggestions")

                val userDetails = User(
                    id = userDoc.id, // Use the document ID instead of user.id
                    firstName = user.firstName,
                    lastName = user.lastName,
                    email = user.email,
                    username = user.username,
                    bio = user.bio,
                    imageUrl = user.imageUrl,
                    phoneNumber = user.phoneNumber,
                    isFollowing = user.isFollowing,
                    avatar = user.avatar,
                    createdAt = user.createdAt,
                    followersCount = user.followersCount,
                    followingCount = user.followingCount,
                    hashedPasscode = user.hashedPasscode,
                    interests = selectedInterests,
                    selectedAccounts = selectedSuggestions,
                    salt = user.salt,
                    lastLogin = user.lastLogin,
                    isOwnProfile = user.isOwnProfile,
                    passCode = user.passCode,
                    loginType = user.loginType,
                    fullname = user.fullname,
                    thumbnail = user.thumbnail,
                    postsCount = user.postsCount,
                    firstTimeLogin = user.firstTimeLogin,
                    isVerified = user.isVerified,
                )
                send(RequestState.Success(data = userDetails))
                println("Success user query: $userDetails")

            } else {
                send(RequestState.Error("User is not available."))
                println("User is not available.")
            }
        } catch (e: Exception) {
            send(RequestState.Error("Error while reading User information: ${e.message}"))
            println("Error while reading User information: ${e.message}")
        }
    }


    override suspend fun getAllUsers(
        limit: Int,
        lastUserId: String?
    ): Result<List<User>> {
        return try {
            val query = if (lastUserId != null) {
                // For pagination, start after the last user ID
                usersCollection
                    .orderBy("createdAt", Direction.ASCENDING)
                    .startAfter(lastUserId)
                    .limit(limit)
            } else {
                // First page
                usersCollection
                    .orderBy("createdAt", Direction.ASCENDING)
                    .limit(limit)
            }

            // Return ALL successfully parsed users. Filtering (e.g. firstTimeLogin)
            // must happen in the caller AFTER pagination is complete — filtering here
            // would cause hasMore checks to undercount and stop pagination early.
            val users = query.get().documents.mapNotNull { doc ->
                try {
                    doc.data<User>().copy(id = doc.id)
                } catch (e: Exception) {
                    println("Error parsing user document ${doc.id}: ${e.message}")
                    null
                }
            }

            println("Fetched ${users.size} users from FeedRepoImpl")
            Result.success(users)
        } catch (e: Exception) {
            println("Error fetching all users: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Prefix-searches users by username OR fullname.
     *
     * Key fact: usernames are stored in Firestore with a leading "@"
     * (e.g. "@alice123"). Prefix queries MUST include the "@" prefix or
     * they will never match (@ sorts before all letters in Unicode).
     *
     * Three queries are run and merged to maximise coverage:
     *  1. Username with "@" prefix + lowercase query  → "@alice"
     *  2. Fullname capitalised                        → "Alice"
     *  3. Fullname lowercase (fallback)               → "alice"  (skipped if same as #2)
     */
    override suspend fun searchUsers(query: String, limit: Int): Result<List<User>> {
        return try {
            val cleanQuery = query.trim().removePrefix("@")
            if (cleanQuery.isEmpty()) return Result.success(emptyList())

            val lowerQuery = cleanQuery.lowercase()
            val capitalQuery = cleanQuery.replaceFirstChar { it.uppercase() }

            // --- Query 1: username prefix (stored as "@alice123", must include "@") ---
            val usernamePrefix = "@$lowerQuery"
            val byUsername = usersCollection
                .orderBy("username", Direction.ASCENDING)
                .startAt(usernamePrefix)
                .endAt(usernamePrefix + "\uf8ff")
                .limit(limit)
                .get()
                .documents
                .mapNotNull { doc ->
                    try { doc.data<User>().copy(id = doc.id) } catch (e: Exception) { null }
                }

            // --- Query 2: fullname prefix, capitalised ("Alice Smith") ---
            val byFullnameCapital = usersCollection
                .orderBy("fullname", Direction.ASCENDING)
                .startAt(capitalQuery)
                .endAt(capitalQuery + "\uf8ff")
                .limit(limit)
                .get()
                .documents
                .mapNotNull { doc ->
                    try { doc.data<User>().copy(id = doc.id) } catch (e: Exception) { null }
                }

            // --- Query 3: fullname prefix, lowercase fallback ("alice smith") ---
            val byFullnameLower = if (lowerQuery != capitalQuery) {
                usersCollection
                    .orderBy("fullname", Direction.ASCENDING)
                    .startAt(lowerQuery)
                    .endAt(lowerQuery + "\uf8ff")
                    .limit(limit)
                    .get()
                    .documents
                    .mapNotNull { doc ->
                        try { doc.data<User>().copy(id = doc.id) } catch (e: Exception) { null }
                    }
            } else emptyList()

            val combined = (byUsername + byFullnameCapital + byFullnameLower)
                .distinctBy { it.id }
                .take(limit)

            println("searchUsers('$query'): ${combined.size} results (username=${byUsername.size}, fullnameCapital=${byFullnameCapital.size}, fullnameLower=${byFullnameLower.size})")
            Result.success(combined)
        } catch (e: Exception) {
            println("searchUsers error: ${e.message}")
            Result.failure(e)
        }
    }
}
