package com.flipverse.data

import com.flipverse.data.domain.LiveBookRepository
import com.flipverse.data.util.normalizeUsername
import com.flipverse.data.util.randomFirestoreId
import com.flipverse.data.util.toFirebaseTimestamp
import com.flipverse.data.util.calculateTimeRemaining
import com.flipverse.data.util.toEpochMilliseconds
import com.flipverse.data.util.CrashlyticsLogger
import com.flipverse.shared.RequestState
import com.flipverse.shared.domain.LiveBook
import com.flipverse.shared.domain.TaggedUser
import com.flipverse.shared.domain.User
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.functions
import kotlinx.datetime.Clock

class LiveBookRepositoryImpl: LiveBookRepository {
    
    private val usersCollection = Firebase.firestore.collection("user")
    private val liveBooksCollection = Firebase.firestore.collection("livebooks")

    override suspend fun publishNewStory(
        currentUserId: String,
        title: String,
        genre: String,
        initialParagraph: String,
        taggedUsers: List<User>,
        isLiked: Boolean,
        initialParagraphWritingTimeSeconds: Long
    ): RequestState<String> {
        return try {
            val now = Clock.System.now()
            val timestamp = now.toFirebaseTimestamp()

            // Fetch author user info using provided authorId
            val userQuery = usersCollection
                .where { "email" equalTo currentUserId.lowercase().trim() }
                .limit(1)
                .get()

            if (userQuery.documents.isEmpty()) {
                return RequestState.Error("Author user not found")
            }
            val userDoc = userQuery.documents.first()
            val currentUser = userDoc.data<User>().copy(id = userDoc.id)

            // Convert taggedUsers to TaggedUser data class (from shared.domain.TaggedUser)
            val taggedUserObjs: List<TaggedUser> = taggedUsers.map {
                TaggedUser(
                    id = it.id,
                    username = normalizeUsername(it.username),
                    fullname = it.fullname,
                    thumbnail = it.thumbnail,
                    email = it.email
                )
            }

            // Convert seconds to milliseconds for storage
            val writingTimeMillis = initialParagraphWritingTimeSeconds * 1000L
            val id = randomFirestoreId()

            // Create ordered list of contributor IDs (tagged users)
            val contributorTurnOrder = taggedUserObjs.map { it.id }

            // Prepare LiveBook instance
            val liveBook = LiveBook(
                id = id,
                title = title,
                genre = genre,
                initialParagraph = initialParagraph,
                initialParagraphWritingTime = writingTimeMillis,
                taggedUsers = taggedUserObjs,
                createdAt = timestamp.toString(),
                timestamp = timestamp.toString(),
                status = "active",
                participantsCount = taggedUsers.size,
                totalContributions = 0,
                authorId = currentUser.id,
                authorName = currentUser.fullname,
                authorUsername = normalizeUsername(currentUser.username),
                authorThumbnail = currentUser.thumbnail,
                lastUpdatedAt = timestamp.toString(),
                isLiked = isLiked,
                contributorTurnOrder = contributorTurnOrder,
                currentTurnIndex = 0,
                currentTurnStartTime = timestamp.toString(),
                turnDurationHours = 3,
                maxParticipants = 8
            )

            liveBooksCollection.document(liveBook.id).set(liveBook)

            // Call sendPushNotification Cloud Function to send a push notification
            try {
//                // Get sender's name for personalized notification
//                val senderName = try {
//                    val senderDetails = fetchUserDetails(senderId)
//                    senderDetails?.fullName ?: "Someone"
//                } catch (e: Exception) {
//                    "Someone"
//                }
                val taggedUserEmails: List<String> = taggedUserObjs.map { it.email }.filter { it.isNotBlank() }
                println("Tagged user emails: $taggedUserEmails")

                val data = mapOf(
                    "recipientIds" to taggedUserEmails,
                    "title" to "LiveBook Invitation",
                    "body" to "You've been invited to co-write '${liveBook.title}'! Join the collaborative storytelling adventure.",
                    "type" to "livebook_message",
                    "senderId" to currentUserId,//livebook author's email
                    "conversationId" to id,
                    "customData" to "livebook_notification"
                )

                val callable = Firebase.functions.httpsCallable("sendMulticastNotification")
                callable(data)

                println("✅ Push notification triggered for recipient: $taggedUserEmails from $currentUserId")
            } catch (e: Exception) {
                println("❌ Failed to send push notification: ${e.stackTraceToString()}")
                // Don't fail the message sending if push notification fails
            }

            RequestState.Success(liveBook.id)
        } catch (e: Exception) {
            // Log to Crashlytics for debugging
            CrashlyticsLogger.logLiveBookError(
                liveBookId = "new_story",
                turnIndex = 0,
                error = e,
                additionalInfo = mapOf(
                    "operation" to "publishNewStory",
                    "author_id" to currentUserId,
                    "title" to title,
                    "tagged_users_count" to taggedUsers.size.toString()
                )
            )
            RequestState.Error(e.message ?: "Failed to publish story")
        }
    }

    override suspend fun fetchActiveLiveBooks(userEmail: String): RequestState<List<LiveBook>> {
        return try {
            // First, get the user ID from email
            val userQuery = usersCollection
                .where { "email" equalTo userEmail.lowercase().trim() }
                .limit(1)
                .get()

            if (userQuery.documents.isEmpty()) {
                return RequestState.Error("User not found")
            }

            val userDoc = userQuery.documents.first()
            val currentUser = userDoc.data<User>().copy(id = userDoc.id)

            // Query live books where status is "active"
            val liveBooksQuery = liveBooksCollection
                .where { "status" equalTo "active" }
                .get()

            val allActiveBooks = liveBooksQuery.documents.map { doc ->
                doc.data<LiveBook>().copy(id = doc.id)
            }

            val userRelatedBooks = mutableListOf<LiveBook>()

            for (liveBook in allActiveBooks) {
                // Check if user is the author
                val isAuthor = liveBook.authorId == currentUser.id

                // Check if user is tagged as participant
                val isTaggedParticipant = liveBook.taggedUsers.any { taggedUser ->
                    taggedUser.id == currentUser.id ||
                            taggedUser.username == currentUser.username
                }

                // Check if book is expired using calculateTimeRemaining
                val timeRemaining = calculateTimeRemaining(liveBook.timestamp)
                if (timeRemaining == "Expired") {
                    // Update status to expired
                    try {
                        liveBooksCollection.document(liveBook.id).update("status" to "archived")
                    } catch (_: Exception) {
                        // Ignore error and continue processing
                    }
                    continue // Skip this expired book
                }

                // Include book if user is author OR tagged participant
                if (isAuthor || isTaggedParticipant) {
                    // Calculate total likes from all paragraphs (including initial paragraph)
                    val calculatedTotalLikes = liveBook.initialParagraphLikes +
                            liveBook.paragraph1Likes +
                            liveBook.paragraph2Likes +
                            liveBook.paragraph3Likes +
                            liveBook.paragraph4Likes +
                            liveBook.paragraph5Likes +
                            liveBook.paragraph6Likes

                    // Update the LiveBook with calculated total likes
                    val updatedLiveBook = if (calculatedTotalLikes != liveBook.totalLikes) {
                        try {
                            // Update the database with the calculated total likes
                            liveBooksCollection.document(liveBook.id)
                                .update("totalLikes" to calculatedTotalLikes)
                            liveBook.copy(totalLikes = calculatedTotalLikes)
                        } catch (_: Exception) {
                            // If update fails, just use the calculated value locally
                            liveBook.copy(totalLikes = calculatedTotalLikes)
                        }
                    } else {
                        liveBook
                    }

                    userRelatedBooks.add(updatedLiveBook)
                }
            }

            RequestState.Success(userRelatedBooks)
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to fetch active live books")
        }
    }


    override suspend fun fetchCompletedLiveBooks(userEmail: String): RequestState<List<LiveBook>> {
        return try {
            // First, get the user ID from email
            val userQuery = usersCollection
                .where { "email" equalTo userEmail.lowercase().trim() }
                .limit(1)
                .get()

            if (userQuery.documents.isEmpty()) {
                return RequestState.Error("User not found")
            }

            val userDoc = userQuery.documents.first()
            val currentUser = userDoc.data<User>().copy(id = userDoc.id)

            // Query live books where status is "completed" and user is in taggedUsers
            val liveBooksQuery = liveBooksCollection
                .where { "status" equalTo "completed" }
                .get()

            val allCompletedBooks = liveBooksQuery.documents.map { doc ->
                doc.data<LiveBook>().copy(id = doc.id)
            }

            // Filter books where current user is tagged
            val userTaggedBooks = allCompletedBooks.filter { liveBook ->
                liveBook.taggedUsers.any { taggedUser ->
                    taggedUser.id == currentUser.id ||
                            taggedUser.username == currentUser.username
                }
            }

            RequestState.Success(userTaggedBooks)
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to fetch completed live books")
        }
    }

    override suspend fun fetchArchivedLiveBooks(userEmail: String): RequestState<List<LiveBook>> {
        return try {
            // First, get the user ID from email
            val userQuery = usersCollection
                .where { "email" equalTo userEmail.lowercase().trim() }
                .limit(1)
                .get()

            if (userQuery.documents.isEmpty()) {
                return RequestState.Error("User not found")
            }

            val userDoc = userQuery.documents.first()
            val currentUser = userDoc.data<User>().copy(id = userDoc.id)

            // Query live books where status is "archived" and user is in taggedUsers
            val liveBooksQuery = liveBooksCollection
                .where { "status" equalTo "archived" }
                .get()

            val allArchivedBooks = liveBooksQuery.documents.map { doc ->
                doc.data<LiveBook>().copy(id = doc.id)
            }

            // Filter books where current user is tagged
            val userTaggedBooks = allArchivedBooks.filter { liveBook ->
                liveBook.taggedUsers.any { taggedUser ->
                    taggedUser.id == currentUser.id ||
                            taggedUser.username == currentUser.username
                }
            }

            RequestState.Success(userTaggedBooks)
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to fetch archived live books")
        }
    }

    override suspend fun getUserLiveBooks(userEmail: String): RequestState<List<LiveBook>> {
        return try {
            // Get all user's LiveBooks by combining active, completed, and archived
            val activeResult = fetchActiveLiveBooks(userEmail)
            val completedResult = fetchCompletedLiveBooks(userEmail)
            val archivedResult = fetchArchivedLiveBooks(userEmail)

            val allBooks = mutableListOf<LiveBook>()

            if (activeResult.isSuccess()) {
                allBooks.addAll(activeResult.getSuccessData())
            }

            if (completedResult.isSuccess()) {
                allBooks.addAll(completedResult.getSuccessData())
            }

            if (archivedResult.isSuccess()) {
                allBooks.addAll(archivedResult.getSuccessData())
            }

            // Remove duplicates based on LiveBook ID and sort by creation date (newest first)
            val uniqueBooks = allBooks
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt }

            RequestState.Success(uniqueBooks)
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to fetch user live books")
        }
    }

    override suspend fun fetchLiveBookById(liveBookId: String): RequestState<LiveBook> {
        return try {
            // Query live book by document ID
            val documentSnapshot = liveBooksCollection.document(liveBookId).get()

            if (documentSnapshot.exists) {
                val liveBook = documentSnapshot.data<LiveBook>().copy(id = documentSnapshot.id)
                RequestState.Success(liveBook)
            } else {
                RequestState.Error("LiveBook not found")
            }
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to fetch LiveBook by ID")
        }
    }

    override suspend fun updateParagraphLikes(
        liveBookId: String,
        paragraphNumber: Int,
        userId: String,
        isLiked: Boolean
    ): RequestState<Unit> {
        return try {
            val likedByFieldName = when (paragraphNumber) {
                0 -> "initialParagraphLikedBy"
                1 -> "paragraph1LikedBy"
                2 -> "paragraph2LikedBy"
                3 -> "paragraph3LikedBy"
                4 -> "paragraph4LikedBy"
                5 -> "paragraph5LikedBy"
                6 -> "paragraph6LikedBy"
                else -> return RequestState.Error("Invalid paragraph number: $paragraphNumber")
            }

            // Get the current LiveBook to check current likedBy list
            val documentSnapshot = liveBooksCollection.document(liveBookId).get()
            if (!documentSnapshot.exists) {
                return RequestState.Error("LiveBook not found")
            }

            val currentLiveBook = documentSnapshot.data<LiveBook>().copy(id = documentSnapshot.id)

            // Get current likedBy list for this paragraph
            val currentLikedBy = try {
                @Suppress("UNCHECKED_CAST")
                (documentSnapshot.data<Map<String, Any>>()[likedByFieldName] as? List<String>)
                    ?: emptyList()
            } catch (e: Exception) {
                emptyList<String>()
            }

            // Calculate new liked by list
            val newLikedBy = if (isLiked) {
                if (userId !in currentLikedBy) currentLikedBy + userId else currentLikedBy
            } else {
                currentLikedBy.filter { it != userId }
            }

            // Calculate new likes count for this paragraph
            val newLikesCount = newLikedBy.size

            // Get the likes count field name
            val likesCountFieldName = when (paragraphNumber) {
                0 -> "initialParagraphLikes" // For initial paragraph (author's paragraph)
                1 -> "paragraph1Likes"
                2 -> "paragraph2Likes"
                3 -> "paragraph3Likes"
                4 -> "paragraph4Likes"
                5 -> "paragraph5Likes"
                6 -> "paragraph6Likes"
                else -> return RequestState.Error("Invalid paragraph number: $paragraphNumber")
            }

            // Calculate total likes from all paragraphs
            val totalLikes = when (paragraphNumber) {
                0 -> newLikesCount + currentLiveBook.paragraph1Likes + currentLiveBook.paragraph2Likes +
                        currentLiveBook.paragraph3Likes + currentLiveBook.paragraph4Likes +
                        currentLiveBook.paragraph5Likes + currentLiveBook.paragraph6Likes

                1 -> currentLiveBook.initialParagraphLikes + newLikesCount + currentLiveBook.paragraph2Likes +
                        currentLiveBook.paragraph3Likes + currentLiveBook.paragraph4Likes +
                        currentLiveBook.paragraph5Likes + currentLiveBook.paragraph6Likes

                2 -> currentLiveBook.initialParagraphLikes + currentLiveBook.paragraph1Likes + newLikesCount +
                        currentLiveBook.paragraph3Likes + currentLiveBook.paragraph4Likes +
                        currentLiveBook.paragraph5Likes + currentLiveBook.paragraph6Likes

                3 -> currentLiveBook.initialParagraphLikes + currentLiveBook.paragraph1Likes + currentLiveBook.paragraph2Likes +
                        newLikesCount + currentLiveBook.paragraph4Likes +
                        currentLiveBook.paragraph5Likes + currentLiveBook.paragraph6Likes

                4 -> currentLiveBook.initialParagraphLikes + currentLiveBook.paragraph1Likes + currentLiveBook.paragraph2Likes +
                        currentLiveBook.paragraph3Likes + newLikesCount +
                        currentLiveBook.paragraph5Likes + currentLiveBook.paragraph6Likes

                5 -> currentLiveBook.initialParagraphLikes + currentLiveBook.paragraph1Likes + currentLiveBook.paragraph2Likes +
                        currentLiveBook.paragraph3Likes + currentLiveBook.paragraph4Likes +
                        newLikesCount + currentLiveBook.paragraph6Likes

                6 -> currentLiveBook.initialParagraphLikes + currentLiveBook.paragraph1Likes + currentLiveBook.paragraph2Likes +
                        currentLiveBook.paragraph3Likes + currentLiveBook.paragraph4Likes +
                        currentLiveBook.paragraph5Likes + newLikesCount

                else -> 0
            }

            // Update both the likedBy array, the likes count, and total likes
            val updates = mapOf(
                likedByFieldName to newLikedBy,
                likesCountFieldName to newLikesCount,
                "totalLikes" to totalLikes,
                "lastUpdatedAt" to Clock.System.now().toString()
            )

            liveBooksCollection.document(liveBookId).update(updates)

            RequestState.Success(Unit)
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to update paragraph likes")
        }
    }

    override suspend fun getParagraphComments(
        liveBookId: String,
        paragraphNumber: Int
    ): RequestState<List<com.flipverse.shared.domain.ParagraphComment>> {
        return try {
            val commentsQuery = liveBooksCollection
                .document(liveBookId)
                .collection("comments")
                .where { "paragraphNumber" equalTo paragraphNumber }
                .get()

            val comments = commentsQuery.documents.map { doc ->
                doc.data<com.flipverse.shared.domain.ParagraphComment>().copy(id = doc.id)
            }.sortedByDescending { it.createdAt } // Most recent first

            RequestState.Success(comments)
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to fetch comments")
        }
    }

    override suspend fun addParagraphComment(
        liveBookId: String,
        paragraphNumber: Int,
        userId: String,
        userName: String,
        userThumbnail: String,
        commentText: String
    ): RequestState<Unit> {
        return try {
            val now = Clock.System.now().toFirebaseTimestamp()
            val commentId = com.flipverse.data.util.randomFirestoreId()

            val comment = com.flipverse.shared.domain.ParagraphComment(
                id = commentId,
                liveBookId = liveBookId,
                paragraphNumber = paragraphNumber,
                userId = userId,
                userName = userName,
                userThumbnail = userThumbnail,
                commentText = commentText,
                timestamp = now.toString(),
                createdAt = now.toEpochMilliseconds()
            )

            // Add comment to subcollection
            liveBooksCollection
                .document(liveBookId)
                .collection("comments")
                .document(commentId)
                .set(comment)

            // Increment comment count for the paragraph
            val commentCountField = when (paragraphNumber) {
                0 -> "initialParagraphCommentsCount"
                1 -> "paragraph1CommentsCount"
                2 -> "paragraph2CommentsCount"
                3 -> "paragraph3CommentsCount"
                4 -> "paragraph4CommentsCount"
                5 -> "paragraph5CommentsCount"
                6 -> "paragraph6CommentsCount"
                else -> return RequestState.Error("Invalid paragraph number: $paragraphNumber")
            }

            // Get current count and increment
            val liveBookDoc = liveBooksCollection.document(liveBookId).get()
            if (liveBookDoc.exists) {
                val currentLiveBook = liveBookDoc.data<LiveBook>().copy(id = liveBookDoc.id)
                val currentCount = when (paragraphNumber) {
                    0 -> currentLiveBook.initialParagraphCommentsCount
                    1 -> currentLiveBook.paragraph1CommentsCount
                    2 -> currentLiveBook.paragraph2CommentsCount
                    3 -> currentLiveBook.paragraph3CommentsCount
                    4 -> currentLiveBook.paragraph4CommentsCount
                    5 -> currentLiveBook.paragraph5CommentsCount
                    6 -> currentLiveBook.paragraph6CommentsCount
                    else -> 0
                }

                liveBooksCollection.document(liveBookId).update(
                    mapOf(
                        commentCountField to (currentCount + 1),
                        "lastUpdatedAt" to now.toString()
                    )
                )
            }

            RequestState.Success(Unit)
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to add comment")
        }
    }

    override suspend fun updateLiveBookFavoriteStatus(
        liveBookId: String,
        userId: String,
        isLiked: Boolean
    ): RequestState<Unit> {
        return try {
            // For now, we'll store the favorite status in the LiveBook document itself
            // In a production app, you might want a separate collection for user-specific favorites

            // Get current LiveBook to access existing favorites list
            val documentSnapshot = liveBooksCollection.document(liveBookId).get()

            if (!documentSnapshot.exists) {
                return RequestState.Error("LiveBook not found")
            }

            val currentLiveBook = documentSnapshot.data<LiveBook>().copy(id = documentSnapshot.id)

            // For simplicity, we'll use a favorites field as a list of user IDs
            // You might want to create a separate favorites subcollection in production
            val currentFavorites = try {
                @Suppress("UNCHECKED_CAST")
                (documentSnapshot.data<Map<String, Any>>()["favoritedByUsers"] as? List<String>)
                    ?: emptyList()
            } catch (e: Exception) {
                emptyList<String>()
            }

            val updatedFavorites = if (isLiked) {
                if (userId !in currentFavorites) {
                    currentFavorites + userId
                } else {
                    currentFavorites
                }
            } else {
                currentFavorites.filter { it != userId }
            }

            // Update the document with the new favorites list
            val updates = mapOf(
                "favoritedByUsers" to updatedFavorites,
                "lastUpdatedAt" to Clock.System.now().toString()
            )

            liveBooksCollection.document(liveBookId).update(updates)

            RequestState.Success(Unit)
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to update favorite status")
        }
    }

    override suspend fun getUserFavoriteStatus(
        liveBookId: String,
        userId: String
    ): RequestState<Boolean> {
        return try {
            val documentSnapshot = liveBooksCollection.document(liveBookId).get()

            if (!documentSnapshot.exists) {
                return RequestState.Error("LiveBook not found")
            }

            val currentFavorites = try {
                @Suppress("UNCHECKED_CAST")
                (documentSnapshot.data<Map<String, Any>>()["favoritedByUsers"] as? List<String>)
                    ?: emptyList()
            } catch (e: Exception) {
                emptyList<String>()
            }

            val isFavorited = userId in currentFavorites
            RequestState.Success(isFavorited)
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to get favorite status")
        }
    }

    override suspend fun checkUserParticipation(userEmail: String): RequestState<com.flipverse.data.domain.UserParticipationStatus> {
        return try {
            // First, get the user ID from email
            val userQuery = usersCollection
                .where { "email" equalTo userEmail.lowercase().trim() }
                .limit(1)
                .get()

            if (userQuery.documents.isEmpty()) {
                return RequestState.Error("User not found")
            }

            val userDoc = userQuery.documents.first()
            val currentUser = userDoc.data<User>().copy(id = userDoc.id)

            // Query all LiveBooks to check participation
            val allLiveBooksQuery = liveBooksCollection.get()
            val allLiveBooks = allLiveBooksQuery.documents.map { doc ->
                doc.data<LiveBook>().copy(id = doc.id)
            }

            val authoredBooks = mutableListOf<String>()
            val contributedBooks = mutableListOf<String>()
            var currentWritingTurn: LiveBook? = null

            allLiveBooks.forEach { liveBook ->
                // Check if user is the author
                if (liveBook.authorId == currentUser.id) {
                    authoredBooks.add(liveBook.id)
                }

                // Check if user is a tagged participant
                val isTaggedParticipant = liveBook.taggedUsers.any { taggedUser ->
                    taggedUser.id == currentUser.id || taggedUser.username == currentUser.username
                }

                // Check if user has contributed to any paragraph
                val hasContributed = listOf(
                    liveBook.paragraph1ContributorId,
                    liveBook.paragraph2ContributorId,
                    liveBook.paragraph3ContributorId,
                    liveBook.paragraph4ContributorId,
                    liveBook.paragraph5ContributorId,
                    liveBook.paragraph6ContributorId
                ).contains(currentUser.id)

                if (isTaggedParticipant || hasContributed) {
                    contributedBooks.add(liveBook.id)
                }

                // Check if it's user's turn to write (active book, tagged, hasn't contributed, and it's their turn)
                if (liveBook.status == "active" && isTaggedParticipant && !hasContributed) {
                    // Check if contributor turn order is initialized
                    if (liveBook.contributorTurnOrder.isNotEmpty()) {
                        val currentTurnIndex = liveBook.currentTurnIndex
                        val contributorTurnOrder = liveBook.contributorTurnOrder
                        val skippedContributors = liveBook.skippedContributors

                        // Check if 3 hours have passed since current turn started
                        val turnStartTime = try {
                            kotlinx.datetime.Instant.parse(liveBook.currentTurnStartTime)
                        } catch (e: Exception) {
                            Clock.System.now()
                        }

                        val now = Clock.System.now()
                        val hoursSinceTurnStart =
                            (now.toEpochMilliseconds() - turnStartTime.toEpochMilliseconds()) / (1000.0 * 60.0 * 60.0)
                        val turnExpired = hoursSinceTurnStart >= liveBook.turnDurationHours

                        // Determine whose turn it is
                        var effectiveTurnIndex = currentTurnIndex
                        var expectedContributorId =
                            contributorTurnOrder.getOrNull(effectiveTurnIndex)

                        // If turn expired, advance to next non-skipped contributor
                        if (turnExpired && expectedContributorId != null) {
                            val tempSkipped = skippedContributors.toMutableList()
                            if (expectedContributorId !in tempSkipped) {
                                tempSkipped.add(expectedContributorId)
                            }

                            effectiveTurnIndex =
                                (effectiveTurnIndex + 1) % contributorTurnOrder.size
                            expectedContributorId =
                                contributorTurnOrder.getOrNull(effectiveTurnIndex)

                            val startIndex = effectiveTurnIndex
                            var cycleComplete = false

                            while (expectedContributorId in tempSkipped && !cycleComplete) {
                                effectiveTurnIndex =
                                    (effectiveTurnIndex + 1) % contributorTurnOrder.size
                                expectedContributorId =
                                    contributorTurnOrder.getOrNull(effectiveTurnIndex)

                                if (effectiveTurnIndex == startIndex) {
                                    cycleComplete = true
                                }
                            }

                            // If everyone skipped, allow any contributor
                            if (expectedContributorId in tempSkipped) {
                                expectedContributorId = null
                            }
                        }

                        // Check if it's the current user's turn
                        val isUsersTurn =
                            expectedContributorId == null || expectedContributorId == currentUser.id

                        if (isUsersTurn) {
                            // Find the next empty paragraph slot
                            val nextEmptyParagraph = when {
                                liveBook.paragraph1.isBlank() -> 1
                                liveBook.paragraph2.isBlank() -> 2
                                liveBook.paragraph3.isBlank() -> 3
                                liveBook.paragraph4.isBlank() -> 4
                                liveBook.paragraph5.isBlank() -> 5
                                liveBook.paragraph6.isBlank() -> 6
                                else -> -1
                            }

                            if (nextEmptyParagraph != -1) {
                                currentWritingTurn = liveBook
                            }
                        }
                    } else {
                        // Legacy support: If turn order not initialized, use old logic
                        val nextEmptyParagraph = when {
                            liveBook.paragraph1.isBlank() -> 1
                            liveBook.paragraph2.isBlank() -> 2
                            liveBook.paragraph3.isBlank() -> 3
                            liveBook.paragraph4.isBlank() -> 4
                            liveBook.paragraph5.isBlank() -> 5
                            liveBook.paragraph6.isBlank() -> 6
                            else -> -1
                        }

                        if (nextEmptyParagraph != -1) {
                            currentWritingTurn = liveBook
                        }
                    }
                }
            }

            val isParticipant = authoredBooks.isNotEmpty() || contributedBooks.isNotEmpty()

            RequestState.Success(
                com.flipverse.data.domain.UserParticipationStatus(
                    isParticipant = isParticipant,
                    authoredBooks = authoredBooks,
                    contributedBooks = contributedBooks,
                    currentWritingTurn = currentWritingTurn
                )
            )

        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to check user participation")
        }
    }

    override suspend fun fetchAllLiveBooksForAnalytics(): RequestState<List<LiveBook>> {
        return try {
            // Query all LiveBooks for analytics regardless of status
            val allLiveBooksQuery = liveBooksCollection.get()
            val allLiveBooks = allLiveBooksQuery.documents.map { doc ->
                doc.data<LiveBook>().copy(id = doc.id)
            }

            RequestState.Success(allLiveBooks)
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Failed to fetch LiveBooks for analytics")
        }
    }

    override suspend fun submitContribution(
        liveBookId: String,
        contributorId: String,
        paragraphContent: String,
        writingTimeSeconds: Long
    ): RequestState<Unit> {
        return try {
            // First, get the current LiveBook to determine which paragraph slot to update
            val documentSnapshot = liveBooksCollection.document(liveBookId).get()

            if (!documentSnapshot.exists) {
                return RequestState.Error("LiveBook not found")
            }

            val currentLiveBook = documentSnapshot.data<LiveBook>().copy(id = documentSnapshot.id)

            // Check if all paragraphs are filled
            val allParagraphsFilled = currentLiveBook.paragraph1.isNotBlank() &&
                    currentLiveBook.paragraph2.isNotBlank() &&
                    currentLiveBook.paragraph3.isNotBlank() &&
                    currentLiveBook.paragraph4.isNotBlank() &&
                    currentLiveBook.paragraph5.isNotBlank() &&
                    currentLiveBook.paragraph6.isNotBlank()

            if (allParagraphsFilled) {
                return RequestState.Error("All paragraph slots are filled")
            }

            // Check if contributor turn order is initialized
            if (currentLiveBook.contributorTurnOrder.isEmpty()) {
                return RequestState.Error("No contributors tagged for this LiveBook")
            }

            // Get the current turn info
            val currentTurnIndex = currentLiveBook.currentTurnIndex
            val contributorTurnOrder = currentLiveBook.contributorTurnOrder
            val skippedContributors = currentLiveBook.skippedContributors.toMutableList()

            // Check if 3 hours have passed since current turn started
            val turnStartTime = try {
                kotlinx.datetime.Instant.parse(currentLiveBook.currentTurnStartTime)
            } catch (e: Exception) {
                Clock.System.now() // Fallback to current time if parsing fails
            }

            val now = Clock.System.now()
            val hoursSinceTurnStart =
                (now.toEpochMilliseconds() - turnStartTime.toEpochMilliseconds()) / (1000.0 * 60.0 * 60.0)
            val turnExpired = hoursSinceTurnStart >= currentLiveBook.turnDurationHours

            // Find whose turn it is (accounting for skipped contributors)
            var effectiveTurnIndex = currentTurnIndex
            var expectedContributorId = contributorTurnOrder.getOrNull(effectiveTurnIndex)

            // If turn expired, skip the current contributor and move to next
            if (turnExpired && expectedContributorId != null) {
                // Add current turn holder to skipped list if not already there
                if (expectedContributorId !in skippedContributors) {
                    skippedContributors.add(expectedContributorId)
                }

                // Move to next contributor in turn order
                effectiveTurnIndex = (effectiveTurnIndex + 1) % contributorTurnOrder.size
                expectedContributorId = contributorTurnOrder.getOrNull(effectiveTurnIndex)

                // Keep advancing until we find a non-skipped contributor or complete the cycle
                val startIndex = effectiveTurnIndex
                var cycleComplete = false

                while (expectedContributorId in skippedContributors && !cycleComplete) {
                    effectiveTurnIndex = (effectiveTurnIndex + 1) % contributorTurnOrder.size
                    expectedContributorId = contributorTurnOrder.getOrNull(effectiveTurnIndex)

                    if (effectiveTurnIndex == startIndex) {
                        cycleComplete = true
                    }
                }

                // If everyone has been skipped, allow anyone to contribute
                if (expectedContributorId in skippedContributors) {
                    expectedContributorId = null // Allow any contributor
                }
            }

            // Validate it's the contributor's turn (unless everyone was skipped)
            if (expectedContributorId != null && contributorId != expectedContributorId) {
                // Check if the contributor has already contributed
                val alreadyContributed = listOf(
                    currentLiveBook.paragraph1ContributorId,
                    currentLiveBook.paragraph2ContributorId,
                    currentLiveBook.paragraph3ContributorId,
                    currentLiveBook.paragraph4ContributorId,
                    currentLiveBook.paragraph5ContributorId,
                    currentLiveBook.paragraph6ContributorId
                ).contains(contributorId)

                if (alreadyContributed) {
                    return RequestState.Error("You have already contributed to this story")
                }

                // Get contributor name for error message
                val expectedContributor =
                    currentLiveBook.taggedUsers.find { it.id == expectedContributorId }
                val contributorName = expectedContributor?.fullname?.takeIf { it.isNotBlank() }
                    ?: expectedContributor?.username
                    ?: "another contributor"

                return RequestState.Error("It's currently $contributorName's turn to contribute. Please wait for your turn.")
            }

            // Check if this contributor has already contributed
            val alreadyContributed = listOf(
                currentLiveBook.paragraph1ContributorId,
                currentLiveBook.paragraph2ContributorId,
                currentLiveBook.paragraph3ContributorId,
                currentLiveBook.paragraph4ContributorId,
                currentLiveBook.paragraph5ContributorId,
                currentLiveBook.paragraph6ContributorId
            ).contains(contributorId)

            if (alreadyContributed) {
                return RequestState.Error("You have already contributed to this story")
            }

            // Find the next available paragraph slot
            val (fieldName, contributorFieldName, writingTimeFieldName) = when {
                currentLiveBook.paragraph1.isBlank() -> Triple(
                    "paragraph1",
                    "paragraph1ContributorId",
                    "paragraph1WritingTime"
                )

                currentLiveBook.paragraph2.isBlank() -> Triple(
                    "paragraph2",
                    "paragraph2ContributorId",
                    "paragraph2WritingTime"
                )

                currentLiveBook.paragraph3.isBlank() -> Triple(
                    "paragraph3",
                    "paragraph3ContributorId",
                    "paragraph3WritingTime"
                )

                currentLiveBook.paragraph4.isBlank() -> Triple(
                    "paragraph4",
                    "paragraph4ContributorId",
                    "paragraph4WritingTime"
                )

                currentLiveBook.paragraph5.isBlank() -> Triple(
                    "paragraph5",
                    "paragraph5ContributorId",
                    "paragraph5WritingTime"
                )

                currentLiveBook.paragraph6.isBlank() -> Triple(
                    "paragraph6",
                    "paragraph6ContributorId",
                    "paragraph6WritingTime"
                )
                else -> return RequestState.Error("All paragraph slots are filled")
            }

            // Convert seconds to milliseconds for storage
            val writingTimeMillis = writingTimeSeconds * 1000L

            // Calculate next turn index (move to next contributor who hasn't been skipped)
            var nextTurnIndex = (effectiveTurnIndex + 1) % contributorTurnOrder.size
            var nextContributorId = contributorTurnOrder.getOrNull(nextTurnIndex)

            // Skip contributors who have already contributed or been skipped
            val contributedIds = listOf(
                currentLiveBook.paragraph1ContributorId,
                currentLiveBook.paragraph2ContributorId,
                currentLiveBook.paragraph3ContributorId,
                currentLiveBook.paragraph4ContributorId,
                currentLiveBook.paragraph5ContributorId,
                currentLiveBook.paragraph6ContributorId,
                contributorId // Include current contributor
            ).filter { it.isNotBlank() }.toSet()

            val startIndex = nextTurnIndex
            var cycleComplete = false

            while (nextContributorId != null &&
                (nextContributorId in contributedIds || nextContributorId in skippedContributors) &&
                !cycleComplete
            ) {
                nextTurnIndex = (nextTurnIndex + 1) % contributorTurnOrder.size
                nextContributorId = contributorTurnOrder.getOrNull(nextTurnIndex)

                if (nextTurnIndex == startIndex) {
                    cycleComplete = true
                    break
                }
            }

            // Create update map
            val updates = mutableMapOf<String, Any>(
                fieldName to paragraphContent,
                contributorFieldName to contributorId,
                writingTimeFieldName to writingTimeMillis,
                "lastUpdatedAt" to now.toString(),
                "currentTurnIndex" to nextTurnIndex,
                "currentTurnStartTime" to now.toString(),
                "skippedContributors" to skippedContributors
            )

            // Update participant count (unique contributors including author)
            val uniqueParticipants = contributedIds.size + 1 // +1 for author
            updates["participantsCount"] = uniqueParticipants.coerceAtMost(8)

            // If this is the final paragraph (paragraph6) or all contributors have contributed, mark as completed
            val allContributorsParticipated = contributedIds.size >= contributorTurnOrder.size
            if (fieldName == "paragraph6" || allContributorsParticipated) {
                updates["status"] = "completed"
                updates["completedAt"] = now.toString()
            }

            // Update the document
            liveBooksCollection.document(liveBookId).update(updates)

            // Send push notification to next turn holder
            try {
                // Only send notification if there's a next contributor and story is still active
                if (nextContributorId != null && !cycleComplete && fieldName != "paragraph6") {
                    val nextContributor = currentLiveBook.taggedUsers.find { it.id == nextContributorId }
                    
                    if (nextContributor != null && nextContributor.email.isNotBlank()) {
                        val data = mapOf(
                            "recipientId" to nextContributor.email,
                            "title" to "It's Your Turn! ✍️",
                            "body" to "Continue the story: \"${currentLiveBook.title}\"",
                            "type" to "livebook_turn",
                            "liveBookId" to liveBookId,
                            "customData" to "livebook_turn_notification"
                        )

                        val callable = Firebase.functions.httpsCallable("sendPushNotification")
                        callable(data)

                        println("✅ Turn notification sent to next contributor: ${nextContributor.email}")
                    }
                }
            } catch (e: Exception) {
                println("❌ Failed to send turn notification: ${e.message}")
                // Don't fail the contribution if notification fails
            }

            RequestState.Success(Unit)
        } catch (e: Exception) {
            // Log to Crashlytics for debugging
            CrashlyticsLogger.logLiveBookError(
                liveBookId = liveBookId,
                turnIndex = 0, // Will be updated with actual index if available
                error = e,
                additionalInfo = mapOf(
                    "operation" to "submitContribution",
                    "contributor_id" to contributorId
                )
            )
            RequestState.Error(e.message ?: "Failed to submit contribution")
        }
    }
}