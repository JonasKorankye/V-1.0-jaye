package com.flipverse.data

import com.flipverse.data.domain.PostRepository
import com.flipverse.data.util.formatTimestamp
import com.flipverse.data.util.generateUserId
import com.flipverse.data.util.randomFirestoreId
import com.flipverse.data.util.toFirebaseTimestamp
import com.flipverse.data.util.toFormattedString
import com.flipverse.data.util.CrashlyticsLogger
import com.flipverse.shared.PreferencesRepository.getEmail
import com.flipverse.shared.PreferencesRepository.loadFlipAccounts
import com.flipverse.shared.PreferencesRepository.loadFlipInterests
import com.flipverse.shared.RequestState
import com.flipverse.shared.domain.Comment
import com.flipverse.shared.domain.CreatePostRequest
import com.flipverse.shared.domain.FeedPopulationStatus
import com.flipverse.shared.domain.Follow
import com.flipverse.shared.domain.MediaType
import com.flipverse.shared.domain.MediaUploadResult
import com.flipverse.shared.domain.ModerationStatus
import com.flipverse.shared.domain.PinnedPosts
import com.flipverse.shared.domain.Post
import com.flipverse.shared.domain.PostEngagement
import com.flipverse.shared.domain.PostLike
import com.flipverse.shared.domain.PostMetadata
import com.flipverse.shared.domain.PostType
import com.flipverse.shared.domain.PostWithComments
import com.flipverse.shared.domain.PrivacyLevel
import com.flipverse.shared.domain.User
import com.flipverse.shared.util.PlatformType
import com.flipverse.shared.util.getPlatformType
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.where
import dev.gitlive.firebase.functions.functions
import dev.gitlive.firebase.storage.File
import dev.gitlive.firebase.storage.storage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class PostRepositoryImpl : PostRepository {
    private val followsCollection = Firebase.firestore.collection("follows")
    private val postsCollection = Firebase.firestore.collection("post")
    private val usersCollection = Firebase.firestore.collection("user")
    private val feedCollection = Firebase.firestore.collection("user_feeds")

    private val dateString =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString()

    /**
     * Helper function to check if the current user has liked a specific post
     * by querying the likes subcollection
     */
    private suspend fun isPostLikedByUser(postId: String, userId: String): Boolean {
        return try {
            val likeDoc = postsCollection.document(postId)
                .collection("likes")
                .document(userId)
                .get()
            likeDoc.exists
        } catch (e: Exception) {
            println("Error checking if post $postId is liked by user $userId: ${e.message}")
            false
        }
    }

    //region PostFeeds
    private suspend fun getFeedPostsFromDenormalizedFeed(
        userId: String,
        limit: Int,
        lastPostTimestamp: String?
    ): Result<List<Post>> {
        return try {
            var query = feedCollection
                .where { "authorId" equalTo userId }
                .orderBy("timestamp", Direction.DESCENDING)
                .limit(limit)

            // If we have a last timestamp, filter
            if (lastPostTimestamp != null) {
                query =
                    feedCollection.where { "timestamp" lessThan lastPostTimestamp }
            }

            val feedSnapshot = query.get()
            val feedPosts = feedSnapshot.documents.mapNotNull { doc ->
                try {
                    doc.data(Post.serializer()).copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(feedPosts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun getPostsFeedFlow(
        currentUserId: String, limit: Int, lastPostTimestamp: String?
    ): Flow<RequestState<List<Post>>> = channelFlow {
        try {
            val userId = currentUserId.ifEmpty { getEmail() }

            // --- Phase 1: Load user doc, interests, hidden posts, followers ALL in parallel ---
            val userInterestsLocal = try { loadFlipInterests() } catch (_: Exception) { emptyList<String>() }

            val currentUserQuery = usersCollection
                .where { "email" equalTo userId.lowercase().trim() }
                .limit(1)
                .get()

            if (currentUserQuery.documents.isEmpty()) {
                send(RequestState.Error("Current user not found"))
                return@channelFlow
            }

            val currentUserDoc = currentUserQuery.documents.first()
            val currentUserDocId = currentUserDoc.id

            // Launch hidden posts, followers, and Firebase interests fallback in parallel
            val hiddenPostsDeferred = async {
                try {
                    usersCollection.document(currentUserDocId)
                        .collection("hidden_posts")
                        .get()
                        .documents.map { it.id }.toSet()
                } catch (_: Exception) { emptySet<String>() }
            }

            val followerIdsDeferred = async {
                try {
                    val followersResult = getFollowerIds(userId).first()
                    if (followersResult is RequestState.Success) {
                        followersResult.getSuccessData().toSet()
                    } else emptySet()
                } catch (_: Exception) { emptySet<String>() }
            }

            // Resolve interests: prefer local, fall back to Firebase
            val userInterests = if (userInterestsLocal.isNotEmpty()) {
                userInterestsLocal
            } else {
                try {
                    val interests = currentUserDoc.get<List<String>>("selectedInterests")
                    if (interests.isNotEmpty()) {
                        com.flipverse.shared.PreferencesRepository.saveFlipInterests(interests)
                    }
                    interests
                } catch (_: Exception) { emptyList() }
            }

            val interestsSet = userInterests.toSet()

            // --- Phase 2: Fetch posts (can run while parallel tasks finish) ---
            val baseQuery = postsCollection
                .where { "privacy" equalTo "PUBLIC" }
                .orderBy("timestamp", Direction.DESCENDING)

            val query = if (lastPostTimestamp != null) {
                baseQuery
                    .where { "timestamp" lessThan lastPostTimestamp }
                    .limit(limit * 10)
            } else {
                baseQuery.limit(limit * 10)
            }

            val allPostsSnapshot = query.get()

            if (allPostsSnapshot.documents.isEmpty()) {
                send(RequestState.Success(data = emptyList()))
                return@channelFlow
            }

            // Await parallel results
            val hiddenPostIds = hiddenPostsDeferred.await()
            val followerIds = followerIdsDeferred.await()

            // --- Phase 3: Filter posts by interest/follower (no network calls) ---
            val filteredPosts = allPostsSnapshot.documents
                .mapNotNull { doc ->
                    if (hiddenPostIds.contains(doc.id)) return@mapNotNull null
                    try {
                        val post = doc.data(Post.serializer()).copy(id = doc.id)

                        // Include own posts
                        if (post.authorId == userId) return@mapNotNull post
                        // Include posts from followers
                        if (followerIds.contains(post.authorId)) return@mapNotNull post
                        // Include posts with shared interests
                        if (interestsSet.isNotEmpty()) {
                            if (post.selectedInterests.any { interestsSet.contains(it) }) post else null
                        } else {
                            post // No interests = show all
                        }
                    } catch (e: Exception) {
                        CrashlyticsLogger.logNonFatal(
                            error = e,
                            context = "getPostsFeedFlow - Post Parsing",
                            additionalInfo = mapOf("document_id" to doc.id, "current_user" to userId)
                        )
                        null
                    }
                }
                .sortedByDescending { it.timestamp }
                .take(limit)

            // --- Phase 4: Batch check likes for only the final filtered posts ---
            val likedPostIds = filteredPosts.map { it.id }.let { postIds ->
                postIds.map { postId ->
                    async { if (isPostLikedByUser(postId, userId)) postId else null }
                }.awaitAll().filterNotNull().toSet()
            }

            val feedPosts = filteredPosts.map { post ->
                post.copy(
                    engagement = post.engagement.copy(
                        isLikedByCurrentUser = likedPostIds.contains(post.id)
                    )
                )
            }

            send(RequestState.Success(data = feedPosts))

        } catch (e: Exception) {
            CrashlyticsLogger.logNonFatal(
                error = e,
                context = "getPostsFeedFlow",
                additionalInfo = mapOf(
                    "current_user" to currentUserId,
                    "last_timestamp" to (lastPostTimestamp ?: "null"),
                    "limit" to limit.toString()
                )
            )
            send(RequestState.Error("${e.message}"))
        }
    }


    override suspend fun refreshPostsFeedFlow(
        currentUserId: String,
        limit: Int
    ): Flow<RequestState<List<Post>>> = channelFlow {
        getPostsFeedFlow(currentUserId, limit, null)
    }

    override suspend fun getFollowingIds(userId: String): Flow<RequestState<List<String>>> =
        channelFlow {
            try {
                val followsSnapshot = followsCollection
                    .where { "followerId" equalTo userId }
                    .where { "isActive" equalTo true }
                    .get()

                // Extract 'followingId' (the followed users' IDs, typically emails)
                val followingIds = followsSnapshot.documents.map { doc ->
                    doc.data<Follow>().followingId
                }
                println("Get Following IDs: $followingIds")
                send(RequestState.Success(data = followingIds))
            } catch (e: Exception) {
                println("Get Following IDs Error: ${e.message}")
                send(RequestState.Error("${e.message}"))
            }
        }


    override suspend fun getFollowingCount(userId: String): Flow<RequestState<Int>> =
        channelFlow {
            try {
                val count = followsCollection
                    .where { "followerId" equalTo userId }
                    .where { "isActive" equalTo true }
                    .get()
                    .documents
                    .size
                send(RequestState.Success(data = count))
            } catch (e: Exception) {
                send(RequestState.Error("${e.message}"))
            }
        }

    //endregion

//region Create Posts

    override suspend fun uploadMedia(
        files: List<File>,
        types: List<MediaType>
    ): Flow<RequestState<List<MediaUploadResult>>> =
        channelFlow {
            try {
                val uploadTasks = files.mapIndexed { index, file ->
                    async {
                        val fileId = generateUserId()
                        val extension = when (types[index]) {
                            MediaType.IMAGE -> "jpg"
                            MediaType.VIDEO -> "mp4"
                            MediaType.THUMBNAIL -> "jpg"
                        }
                        val fileName = "$fileId.$extension"
                        val folderPath = when (types[index]) {
                            MediaType.IMAGE -> "images"
                            MediaType.VIDEO -> "videos"
                            MediaType.THUMBNAIL -> "thumbnails"
                        }

                        val storageRef =
                            Firebase.storage.reference.child("posts/$folderPath/$fileName")

                        storageRef.putFile(file)

                        // Get download URL
                        val downloadUrl = storageRef.getDownloadUrl()

                        MediaUploadResult(
                            url = downloadUrl,
                            type = types[index]
                        )
                    }
                }

                val results = uploadTasks.awaitAll()
                send(RequestState.Success(results))
            } catch (e: Exception) {
                send(RequestState.Error("${e.message}"))
            }
        }

    override suspend fun createPost(
        request: CreatePostRequest,
        currentUser: User
    ): Flow<RequestState<Post>> = channelFlow {
        try {
            // Generate post ID
            val postId = generateUserId()
            val timestamp = Clock.System.now().toFirebaseTimestamp().toFormattedString()
            val createdAt = dateString

            // Upload media if present
            val mediaUrls = mutableListOf<String>()
            var videoUrl: String? = null
            var thumbnailUrl: String? = null

            if (request.imageFiles.isNotEmpty()) {
                val imageTypes = request.imageFiles.map { MediaType.IMAGE }
                val uploadResult = uploadMedia(request.imageFiles, imageTypes)
                val data = uploadResult.first()
                if (data.isSuccess()) {
                    mediaUrls.addAll(data.getSuccessData().map { it.url })
                } else {
                    send(RequestState.Error(data.getErrorMessage()))
                    return@channelFlow
                }
            }

            if (request.videoFiles.isNotEmpty()) {
                val uploadVideoResult =
                    uploadMedia(request.videoFiles, listOf(MediaType.VIDEO))
                val data = uploadVideoResult.first()
                if (data.isSuccess()) {
                    videoUrl = data.getSuccessData().firstOrNull()?.url
                    thumbnailUrl = videoUrl // todo:: In prod app, generate actual thumbnail
                } else {
                    send(RequestState.Error(data.getErrorMessage()))
                    return@channelFlow
                }
            }

            // Create post object
            val post = Post(
                id = postId,
                postId = postId,
                whatsNew = request.whatsNew,
                source = request.source,
                authorId = currentUser.email,
                authorHandle = currentUser.username,
                authorName = if(currentUser.firstName == "Unknown") currentUser.lastName else currentUser.firstName,
                authorProfileImage = currentUser.thumbnail,
                timestamp = timestamp.toString(),
                createdAt = createdAt,
                imageUrls = mediaUrls,
                videoUrl = videoUrl,
                thumbnailUrl = thumbnailUrl,
                postType = request.postType,
                engagementScore = 5.0,
                trendingScore = 4.0,
                qualityScore = 2.0,
                tags = request.tags,
                location = request.location,
                privacy = request.privacy,
                engagement = PostEngagement(),
                isEdited = false,
                isPinned = false,
                isArchived = false,
                selectedSuggestions = loadFlipAccounts(),
                selectedInterests = loadFlipInterests(),
                metadata = PostMetadata(
                    wordCount = request.whatsNew.split("\\s+".toRegex()).size,
                    deviceInfo = if (getPlatformType() == PlatformType.Android) "Android" else "iOS",
                    moderationStatus = ModerationStatus.APPROVED,
                    searchKeywords = generateSearchKeywords(
                        request.source,
                        request.whatsNew,
                        request.tags
                    )
                )
            )

            println("Post data-->: $post")
            // Save post to Firestore
            postsCollection.document(postId).set(post)


            // Update user's post count
            usersCollection.document(currentUser.id).update(
                mapOf("postsCount" to FieldValue.increment(1))
            )

            //populate user feeds
            val feedItem = createFeedItem(post, currentUser, currentUser.id)
            feedCollection.document(feedItem.id).set(feedItem)

            println("Post created successfully: $post")

            send(RequestState.Success(post))

        } catch (e: Exception) {
            println("Error creating post: ${e.message}")
            // Log to Crashlytics for debugging
            CrashlyticsLogger.logNonFatal(
                error = e,
                context = "createPost (Dashboard)",
                additionalInfo = mapOf(
                    "author_id" to currentUser.email,
                    "post_type" to request.postType.name,
                    "has_images" to request.imageFiles.isNotEmpty().toString(),
                    "has_video" to request.videoFiles.isNotEmpty().toString(),
                    "tags_count" to request.tags.size.toString()
                )
            )
            send(RequestState.Error(e.message.toString()))
        }

    }

    override suspend fun addCommentToPost(
        postId: String,
        commentText: String,
        commentById: String,
        commentByHandle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        try {
            // 1. Prepare data
            val commentData = mapOf(
                "commentById" to commentById,
                "commentBy" to commentByHandle,
                "commentText" to commentText,
                "timestamp" to Clock.System.now().toFirebaseTimestamp().toFormattedString()
            )

            // 2. Add to subcollection "comments" under the given post
            postsCollection.document(postId).collection("comments").add(commentData)

            // 3. Increment the post's "engagement.commentsCount" field (handle null gracefully!)
            postsCollection.document(postId).update(
                mapOf("engagement.commentsCount" to FieldValue.increment(1))
            )
            // 4. Send push notification to the one who originally posted

            try {
                // Fetch the post to get its author
                val postDoc = postsCollection.document(postId).get()
                val postData = postDoc.data(Post.serializer())
                val recipientId = postData.authorId // original post creator

                // Only send notification if user is not commenting on their own post
                if (commentById != recipientId) {
                    // Notification content
                    val senderName = commentByHandle
                    val notificationContent =
                        "${commentByHandle} commented on your post: "

                    val data = mapOf(
                        "recipientId" to recipientId,
                        "title" to notificationContent,
                        "body" to commentText,
                        "type" to "comment_notification",
                        "senderId" to commentById,
                        "messageId" to postId,
                        "customData" to "comment_notification"
                    )
                    println("Sending push notification to $data")

                    val callable = Firebase.functions.httpsCallable("sendPushNotification")
                    callable(data)

                    println("✅ Push notification triggered for recipient: $recipientId from $senderName")
                }
            } catch (e: Exception) {
                println("❌ Failed to send push notification: ${e.message}")
                // Don't fail the message sending if push notification fails
            }
            onSuccess()
        } catch (e: Exception) {
            onError(e.message.toString())
        }
    }

    override suspend fun getPostWithComments(postId: String): Result<PostWithComments> {
        return try {
            // 1. Fetch the post
            val postDoc = postsCollection.document(postId).get()
            val post = postDoc.data(Post.serializer()).copy(id = postDoc.id)
            println("raw response POST:: $post")
            
            // Check if current user has liked this post
            val currentUserId = getEmail()
            val isLiked = isPostLikedByUser(postId, currentUserId)
            
            val formattedPost = post.copy(
                timestamp = formatTimestamp(post.timestamp),
                engagement = post.engagement.copy(
                    isLikedByCurrentUser = isLiked
                )
            )

            // 2. Fetch all comments
            val commentsSnapshot = postsCollection.document(postId).collection("comments").get()
            val comments = commentsSnapshot.documents.map { doc ->
                val data = doc.data<Comment>()
                // Fetch user's profile thumbnail using the email (commentById)
                val userQuery = usersCollection.where { "email" equalTo data.commentById }.get()
                val profileThumbnail = try {
                    userQuery.documents.firstOrNull()?.data<User>()?.thumbnail
                } catch (e: Exception) {
                    null
                }
                val fullname = try {
                    userQuery.documents.firstOrNull()?.data<User>()?.fullname
                } catch (e: Exception) {
                    null
                }
                Comment(
                    id = doc.id,
                    commentById = data.commentById,
                    commentBy = data.commentBy,
                    commentText = data.commentText,
                    likesCount = data.likesCount,
                    repliesCount = data.repliesCount,
                    reply = data.reply,
                    likedBy = data.likedBy,
                    timestamp = formatTimestamp(data.timestamp),
                    profileThumbnail = profileThumbnail,
                    fullname = fullname
                )
            }
            println("Post with comments: $formattedPost, $comments")
            Result.success(PostWithComments(formattedPost, comments))
        } catch (e: Exception) {
            println("Error fetching post with comments: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun populateUserFeeds(
        post: Post,
        author: User
    ): Flow<RequestState<FeedPopulationStatus>> = channelFlow<RequestState<FeedPopulationStatus>> {
        try {
            // Only populate feeds for public posts
            if (post.privacy != PrivacyLevel.PUBLIC) {
                send(RequestState.Success(FeedPopulationStatus.Completed))
            }

            // Get all followers
            val followersResult = getFollowerIds(post.authorId)
            followersResult.collectLatest { data ->
                println("Collecting user feeds for post: $data")
                if (data is RequestState.Success) {
                    val followerIds = data.getSuccessData()
                    println("Follower IDs: $followerIds")
                    if (followerIds.isEmpty()) {
//                        val feedItem = createFeedItem(post, author, author.id)
//                        feedCollection.document(feedItem.id).set(feedItem)
                        send(RequestState.Success(FeedPopulationStatus.Completed))
                    }

                    // Create feed items for each follower in batches
                    val batchSize = 500 // Firestore batch limit
                    val batches = followerIds.chunked(batchSize)

                    for (batch in batches) {
                        val feedItemTasks = batch.map { followerId ->
                            async {
                                try {
                                    val feedItem = createFeedItem(post, author, followerId)
                                    feedCollection.document(feedItem.id).set(feedItem)
                                    true
                                } catch (e: Exception) {
                                    println("Error creating feed item: ${e.message}")
                                    false
                                }
                            }
                        }

                        // Wait for batch to complete
                        feedItemTasks.awaitAll()
                    }


                } else {
                    send(RequestState.Error(data.toString()))
                    println("Error fetching followers: $data")
                }
            }
            send(RequestState.Success(FeedPopulationStatus.Completed))
        } catch (e: Exception) {
            send(RequestState.Error("Failed to populate feed${e.message}"))
        }
    }

    override suspend fun getFollowerIds(userId: String): Flow<RequestState<List<String>>> =
        channelFlow<RequestState<List<String>>> {
            try {
                val followersSnapshot = followsCollection
                    .where { "followingId" equalTo userId } // Users who follow this user
                    .where { "isActive" equalTo true }
                    .get()

                val followerIds = followersSnapshot.documents.map { doc ->
                    doc.data<Follow>().followerId
                }
                println("Follower IDs: $followerIds")

                send(RequestState.Success(followerIds))
            } catch (e: Exception) {
                send(RequestState.Error("Failed to fetch follower IDs${e.message}"))
            }
        }

    override fun getOnlyFollowingFeedFlow(
        userId: String,
        limit: Int,
        lastPostTimestamp: String?
    ): Flow<RequestState<List<Post>>> = channelFlow {
        try {
            // 1. Get IDs of users you're following
            val following = getUserFollowing(userId).getOrElse { emptyList() }

            println("Following IDs: $following")

            if (following.isEmpty()) {
                send(RequestState.Error("No users are being followed"))
                return@channelFlow
            }

            // 2. Firestore limitation: "authorId in [...]" max 10 at a time
            val postSnapshots = mutableListOf<Post>()
            val batches = following.chunked(10)
            println("Batch size: ${batches.size}")
            println("Batches list: $batches")

            for (batch in batches) {
                // If there is only one followed user in the batch, use equality instead of 'in'
                val baseQuery = if (batch.size == 1) {
                    postsCollection
                        .where { "authorId" equalTo batch.first() }
                } else {
                    postsCollection
                        .where { "authorId" inArray batch }
                }

                // 3. Support pagination - chain the where clause properly
                var query = if (lastPostTimestamp != null) {
                    baseQuery.where { "timestamp" lessThan lastPostTimestamp }
                } else {
                    baseQuery
                }
                
                // Apply ordering and limit at the end
                query = query
                    .orderBy("timestamp", Direction.DESCENDING)
                    .limit(limit)

                val docs = query.get().documents
                postSnapshots += docs.mapNotNull { doc ->
                    try {
                        doc.data(Post.serializer()).copy(id = doc.id)
                    } catch (_: Exception) {
                        null
                    }
                }
            }
            
            // Filter out posts by the current user and sort
            val resultPosts = postSnapshots
                .filter { post -> post.authorId != userId }
                .sortedByDescending { it.timestamp }
                .take(limit)
            println("Fetched ${resultPosts.size} posts (excluding current user's posts)")

            send(RequestState.Success(resultPosts))
        } catch (e: Exception) {
            send(RequestState.Error("Failed to fetch posts${e.message}"))
            println("Error fetching posts: ${e.message}")
        }
    }

    fun getPostWithCommentsFlow(postId: String): Flow<RequestState<Pair<Post, List<Comment>>>> =
        channelFlow {
            try {
                // Fetch the post
                val postDoc = postsCollection.document(postId).get()
                val post = postDoc.data(Post.serializer()).copy(id = postDoc.id)

                // Fetch all comments
                val commentsSnapshot = postsCollection.document(postId).collection("comments").get()
                val comments = commentsSnapshot.documents.map { doc ->
                    val data = doc.data() as Map<String, Any?>
                    Comment(
                        id = doc.id,
                        commentById = data["commentById"] as? String ?: "",
                        commentBy = data["commentBy"] as? String ?: "",
                        commentText = data["commentText"] as? String ?: "",
                        timestamp = data["timestamp"] as? String ?: ""
                    )
                }

                send(RequestState.Success(post to comments))
            } catch (e: Exception) {
                send(RequestState.Error(e.message ?: "Failed to fetch post or comments"))
            }
        }

    private fun createFeedItem(post: Post, author: User, userId: String): Post {
        return Post(
            id = "${userId}_${post.id}", // Composite ID for easy querying
            postId = post.id,
            authorId = post.authorId,
            authorName = author.fullname,
            authorHandle = author.username,
            authorProfileImage = author.thumbnail,
            authorIsVerified = author.isVerified,
            source = post.source,
            whatsNew = post.whatsNew,
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

    private fun generateSearchKeywords(
        title: String,
        content: String,
        tags: List<String>
    ): List<String> {
        val words = mutableSetOf<String>()

        // Add words from title and content
        ("$title $content").lowercase()
            .split("\\s+".toRegex())
            .filter { it.length > 2 }
            .forEach { words.add(it) }

        // Add tags
        tags.forEach { words.add(it.lowercase()) }

        return words.toList()
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

    override suspend fun getUserPosts(userId: String): Flow<RequestState<List<Post>>> =
        channelFlow {
            try {
                val postsQuery = postsCollection
                    .where { "authorId" equalTo userId }
                    .orderBy("timestamp", Direction.DESCENDING)
                    .limit(50)

            val snapshot = postsQuery.get()
            val currentUserId = getEmail()
            val userPosts = snapshot.documents.mapNotNull { doc ->
                try {
                    val post = doc.data(Post.serializer()).copy(id = doc.id)
                    // Check if current user has liked this post
                    val isLiked = isPostLikedByUser(post.id, currentUserId)
                    post.copy(
                        engagement = post.engagement.copy(
                            isLikedByCurrentUser = isLiked
                        )
                    )
                } catch (e: Exception) {
                    println("Error parsing post: ${e.message}")
                    null
                }
            }

            send(RequestState.Success(userPosts))
        } catch (e: Exception) {
            println("Error fetching user posts: ${e.message}")
            send(RequestState.Error(e.message ?: "Failed to fetch user posts"))
        }
    }

    override suspend fun getUserLikedPosts(userId: String): Flow<RequestState<List<Post>>> =
        channelFlow {
            try {
                // First, get the user document to access liked posts
                val userQuery = usersCollection
                    .where { "email" equalTo userId.lowercase().trim() }
                    .limit(1)
                    .get()

                if (userQuery.documents.isEmpty()) {
                    send(RequestState.Success(emptyList()))
                    return@channelFlow
                }

                val userDoc = userQuery.documents.first()
                val currentUser = userDoc.data<User>().copy(id = userDoc.id)

                // Get user's liked posts from their liked_posts subcollection
                val likedPostsSnapshot = usersCollection.document(currentUser.id)
                    .collection("liked_posts")
                    .orderBy("timestamp", Direction.DESCENDING)
                    .limit(50)
                    .get()

                if (likedPostsSnapshot.documents.isEmpty()) {
                    send(RequestState.Success(emptyList()))
                    return@channelFlow
                }

                val likedPostIds = likedPostsSnapshot.documents.mapNotNull { doc ->
                    try {
                        doc.data<Map<String, Any>>()["postId"] as? String
                    } catch (e: Exception) {
                        null
                    }
                }

                if (likedPostIds.isEmpty()) {
                    send(RequestState.Success(emptyList()))
                    return@channelFlow
                }

                // Fetch the actual posts in batches (Firestore 'in' limit is 10)
                val allLikedPosts = mutableListOf<Post>()
                val batches = likedPostIds.chunked(10)

                for (batch in batches) {
                    val query = if (batch.size == 1) {
                        postsCollection.where { "id" equalTo batch.first() }
                    } else {
                        postsCollection.where { "id" inArray batch }
                    }

                    val postsSnapshot = query.get()
                    val currentUserId = getEmail()
                    val posts = postsSnapshot.documents.mapNotNull { doc ->
                        try {
                            val post = doc.data(Post.serializer()).copy(id = doc.id)
                            // These are liked posts, so set isLikedByCurrentUser to true
                            post.copy(
                                engagement = post.engagement.copy(
                                    isLikedByCurrentUser = true
                                )
                            )
                        } catch (e: Exception) {
                            println("Error parsing liked post: ${e.message}")
                            null
                        }
                    }
                    allLikedPosts.addAll(posts)
                }

                // Sort by timestamp (newest first)
                val sortedLikedPosts = allLikedPosts.sortedByDescending { it.timestamp }

                send(RequestState.Success(sortedLikedPosts))
            } catch (e: Exception) {
                println("Error fetching user liked posts: ${e.message}")
                send(RequestState.Error(e.message ?: "Failed to fetch user liked posts"))
            }
        }

    override suspend fun pinPost(
        postId: String,
        userId: String,
        isPinned: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        try {
            val pinnedPostsCollection = usersCollection.document(userId).collection("pinned_posts")
            if (isPinned) {
                pinnedPostsCollection.document(postId).set(
                    mapOf(
                        "postId" to postId,
                        "timestamp" to Clock.System.now().toFirebaseTimestamp().toFormattedString()
                    )
                )
                println("Post pinned successfully in  Firebase")
            } else {
                pinnedPostsCollection.document(postId).delete()
                println("Post unpinned successfully in Firebase")
            }
            // Reflect pin state in the main post document
            postsCollection.document(postId).update(
                mapOf("isPinned" to isPinned)
            )
            onSuccess()
        } catch (e: Exception) {
            onError(e.message ?: "An error occurred")
        }
    }

    override suspend fun deletePost(
        postId: String,
        userId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        try {
            // 1) Delete comments subcollection documents
            val commentsRef = postsCollection.document(postId).collection("comments")
            val commentsSnap = commentsRef.get()
            commentsSnap.documents.forEach { doc ->
                commentsRef.document(doc.id).delete()
            }

            // 2) Delete from denormalized feeds
            val feedsSnap = feedCollection.where { "postId" equalTo postId }.get()
            feedsSnap.documents.forEach { doc ->
                feedCollection.document(doc.id).delete()
            }

            // 3) Remove from current user's pinned posts (if present)
            usersCollection.document(userId)
                .collection("pinned_posts")
                .document(postId)
                .delete()

            // 4) Delete the post document
            postsCollection.document(postId).delete()

            // 5) Decrement user's post count
            usersCollection.document(userId).update(
                mapOf("postsCount" to FieldValue.increment(-1))
            )

            onSuccess()
        } catch (e: Exception) {
            onError(e.message ?: "Failed to delete post")
        }
    }

    override suspend fun hidePost(
        postId: String,
        userId: String,
        isHidden: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        try {
            val hiddenRef =
                usersCollection.document(userId).collection("hidden_posts").document(postId)
            if (isHidden) {
                hiddenRef.set(
                    mapOf(
                        "postId" to postId,
                        "timestamp" to Clock.System.now().toFirebaseTimestamp().toFormattedString()
                    )
                )
            } else {
                hiddenRef.delete()
            }
            onSuccess()
        } catch (e: Exception) {
            onError(e.message ?: "Failed to update hidden post state")
        }
    }

    override suspend fun updateCommentLike(
        postId: String,
        commentId: String,
        userId: String,
        like: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        try {
            println("like boolean is $like")
            println("userId is $userId")
            println("commentId is $commentId")
            println("postId is $postId")

            val commentRef = postsCollection
                .document(postId)
                .collection("comments")
                .document(commentId)

            val snapshot = commentRef.get()
            val data = try {
                snapshot.data() as Map<String, Any?>
            } catch (_: Exception) {
                emptyMap()
            }
            val initFields = mutableMapOf<String, Any>()
            if (!data.containsKey("likesCount")) initFields["likesCount"] = 0
            if (!data.containsKey("likedBy")) initFields["likedBy"] = emptyList<String>()
            if (initFields.isNotEmpty()) {
                commentRef.update(initFields)
            }

            val updateMap = if (like) {
                mapOf(
                    "likesCount" to FieldValue.increment(1),
                    "likedBy" to FieldValue.arrayUnion(userId)
                )
            } else {
                mapOf(
                    "likesCount" to FieldValue.increment(-1),
                    "likedBy" to FieldValue.arrayRemove(userId)
                )
            }
            commentRef.update(updateMap)
            if (like) {
                println("Comment like updated successfully")
            } else {
                println("Comment unliked successfully")
            }
            onSuccess()
        } catch (e: Exception) {
            println("Error updating comment like: ${e.message}")
            onError(e.message ?: "Failed to update comment like")
        }
    }

    override suspend fun addReplyToComment(
        postId: String,
        parentCommentId: String,
        replyText: String,
        replyById: String,
        replyByHandle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        try {
            val parentCommentRef = postsCollection
                .document(postId)
                .collection("comments")
                .document(parentCommentId)

            val repliesRef = parentCommentRef
                .collection("replies")

            val replyId = generateUserId()
            val data = mapOf(
                "id" to replyId,
                "replyById" to replyById,
                "replyBy" to replyByHandle,
                "replyText" to replyText,
                "timestamp" to Clock.System.now().toFirebaseTimestamp().toFormattedString()
            )

            repliesRef.document(replyId).set(data)

            // Ensure repliesCount exists on parent comment, then increment
            val parentSnap = parentCommentRef.get()
            val parentData = try {
                parentSnap.data<Comment>()
            } catch (_: Exception) {
                null
            }
            println("Parent data: $parentData")

            if (parentData?.repliesCount == null) {
                parentCommentRef.update(mapOf("repliesCount" to 0))
            }
            parentCommentRef.update(mapOf("repliesCount" to FieldValue.increment(1)))
            println("Reply added successfully")
            onSuccess()
        } catch (e: Exception) {
            println("Error adding reply: ${e.message}")
            onError(e.message ?: "Failed to add reply")
        }
    }

    override suspend fun getPostLikes(postId: String, limit: Int): Result<List<PostLike>> {
        return try {
            val likesSnapshot = postsCollection
                .document(postId)
                .collection("likes")
                .orderBy("timestamp", Direction.DESCENDING)
                .limit(limit)
                .get()

            val postLikes = likesSnapshot.documents.mapNotNull { doc ->
                try {
                    doc.data(PostLike.serializer())
                } catch (e: Exception) {
                    println("Error parsing like document: ${e.message}")
                    null
                }
            }

            Result.success(postLikes)
        } catch (e: Exception) {
            println("Error fetching post likes: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getRecentCommentsRaw(postId: String, limit: Int): Result<List<Comment>> {
        return try {
            val commentsSnapshot = postsCollection
                .document(postId)
                .collection("comments")
                .orderBy("timestamp", Direction.DESCENDING)
                .limit(limit)
                .get()

            val comments = commentsSnapshot.documents.mapNotNull { doc ->
                try {
                    // Read raw stored fields without formatting
                    val data = doc.data(Comment.serializer())
                    Comment(
                        id = doc.id,
                        commentById = data.commentById as? String ?: "",
                        commentBy = data.commentBy as? String ?: "",
                        commentText = data.commentText as? String ?: "",
                        likedBy = (data.likedBy as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        likesCount = (data.likesCount as? Number)?.toInt() ?: 0,
                        repliesCount = (data.repliesCount as? Number)?.toInt() ?: 0,
                        timestamp = (data.timestamp as? String) ?: "",
                        // keep defaults for fields we are not hydrating here
                    )
                } catch (_: Exception) {
                    null
                }
            }
            println("Recent comments: $comments")
            Result.success(comments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reportPost(
        postId: String,
        reportedByUserId: String,
        reportedAuthorId: String?
    ): RequestState<Unit> {
        if (postId.isBlank() || reportedByUserId.isBlank()) {
            return RequestState.Error("Post ID and reporting user are required.")
        }

        return try {
            val reportId = randomFirestoreId()
            val timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString()

            println(
                "🚩 reportPost called with postId=$postId, reportedByUserId=$reportedByUserId, reportedAuthorId=$reportedAuthorId, reportId=$reportId"
            )
            println("🚩 Writing Firestore path: post_reports/$reportId")

            Firebase.firestore.collection("post_reports")
                .document(reportId)
                .set(
                    mapOf(
                        "id" to reportId,
                        "postId" to postId,
                        "reportedByUserId" to reportedByUserId,
                        "reportedAuthorId" to (reportedAuthorId ?: ""),
                        "status" to "pending",
                        "source" to "app",
                        "createdAt" to timestamp,
                        "updatedAt" to timestamp
                    )
                )

            println("✅ reportPost report document write completed for reportId=$reportId")
            println("🚩 Updating Firestore path: posts/$postId metadata.reportCount + moderationStatus")

            postsCollection.document(postId).update(
                mapOf(
                    "metadata.reportCount" to FieldValue.increment(1),
                    "metadata.moderationStatus" to ModerationStatus.FLAGGED.name
                )
            )

            println("✅ reportPost post metadata update completed for postId=$postId")

            RequestState.Success(Unit)
        } catch (e: Exception) {
            println("❌ reportPost failed for postId=$postId, reportedByUserId=$reportedByUserId: ${e.message}")
            e.printStackTrace()
            RequestState.Error("Failed to report post: ${e.message}")
        }
    }
}
