package com.flipverse.data.domain

import com.flipverse.shared.RequestState
import com.flipverse.shared.domain.LiveBook
import com.flipverse.shared.domain.User

interface LiveBookRepository {
    suspend fun publishNewStory(
        currentUserId: String,
        title: String,
        genre: String,
        initialParagraph: String,
        taggedUsers: List<User>,
        isLiked: Boolean = false,
        initialParagraphWritingTimeSeconds: Long = 0
    ): RequestState<String>

    suspend fun fetchActiveLiveBooks(userEmail: String): RequestState<List<LiveBook>>

    suspend fun fetchCompletedLiveBooks(userEmail: String): RequestState<List<LiveBook>>

    suspend fun fetchArchivedLiveBooks(userEmail: String): RequestState<List<LiveBook>>

    suspend fun getUserLiveBooks(userEmail: String): RequestState<List<LiveBook>>

    suspend fun fetchLiveBookById(liveBookId: String): RequestState<LiveBook>

    suspend fun updateParagraphLikes(
        liveBookId: String,
        paragraphNumber: Int,
        userId: String,
        isLiked: Boolean
    ): RequestState<Unit>

    suspend fun getParagraphComments(
        liveBookId: String,
        paragraphNumber: Int
    ): RequestState<List<com.flipverse.shared.domain.ParagraphComment>>

    suspend fun addParagraphComment(
        liveBookId: String,
        paragraphNumber: Int,
        userId: String,
        userName: String,
        userThumbnail: String,
        commentText: String
    ): RequestState<Unit>

    suspend fun updateLiveBookFavoriteStatus(
        liveBookId: String,
        userId: String,
        isLiked: Boolean
    ): RequestState<Unit>

    suspend fun getUserFavoriteStatus(
        liveBookId: String,
        userId: String
    ): RequestState<Boolean>

    suspend fun checkUserParticipation(userEmail: String): RequestState<UserParticipationStatus>

    suspend fun fetchAllLiveBooksForAnalytics(): RequestState<List<LiveBook>>

    suspend fun submitContribution(
        liveBookId: String,
        contributorId: String,
        paragraphContent: String,
        writingTimeSeconds: Long = 0
    ): RequestState<Unit>
}

data class UserParticipationStatus(
    val isParticipant: Boolean,
    val authoredBooks: List<String> = emptyList(),
    val contributedBooks: List<String> = emptyList(),
    val currentWritingTurn: LiveBook? = null
)