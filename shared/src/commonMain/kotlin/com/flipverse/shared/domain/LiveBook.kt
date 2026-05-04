package com.flipverse.shared.domain

import kotlinx.serialization.Serializable

@Serializable
data class LiveBook(
    val id: String = "",
    val title: String = "",
    val genre: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorThumbnail: String = "",
    val initialParagraph: String = "",
    val initialParagraphWritingTime: Long = 0, // in milliseconds
    val initialParagraphLikes: Int = 0, // Number of likes on initial paragraph
    val initialParagraphLikedBy: List<String> = emptyList(), // User IDs who liked the initial paragraph
    val initialParagraphCommentsCount: Int = 0, // Number of comments on initial paragraph
    val paragraph1: String = "",
    val paragraph1ContributorId: String = "",
    val paragraph1ContributorName: String = "",
    val paragraph1Likes: Int = 0,
    val paragraph1WritingTime: Long = 0, // in milliseconds
    val paragraph1LikedBy: List<String> = emptyList(), // User IDs who liked paragraph 1
    val paragraph1CommentsCount: Int = 0,
    val paragraph2: String = "",
    val paragraph2ContributorId: String = "",
    val paragraph2ContributorName: String = "",
    val paragraph2Likes: Int = 0,
    val paragraph2WritingTime: Long = 0,
    val paragraph2LikedBy: List<String> = emptyList(),
    val paragraph2CommentsCount: Int = 0,
    val paragraph3: String = "",
    val paragraph3ContributorId: String = "",
    val paragraph3ContributorName: String = "",
    val paragraph3Likes: Int = 0,
    val paragraph3WritingTime: Long = 0,
    val paragraph3LikedBy: List<String> = emptyList(),
    val paragraph3CommentsCount: Int = 0,
    val paragraph4: String = "",
    val paragraph4ContributorId: String = "",
    val paragraph4ContributorName: String = "",
    val paragraph4Likes: Int = 0,
    val paragraph4WritingTime: Long = 0,
    val paragraph4LikedBy: List<String> = emptyList(),
    val paragraph4CommentsCount: Int = 0,
    val paragraph5: String = "",
    val paragraph5ContributorId: String = "",
    val paragraph5ContributorName: String = "",
    val paragraph5Likes: Int = 0,
    val paragraph5WritingTime: Long = 0,
    val paragraph5LikedBy: List<String> = emptyList(),
    val paragraph5CommentsCount: Int = 0,
    val paragraph6: String = "",
    val paragraph6ContributorId: String = "",
    val paragraph6ContributorName: String = "",
    val paragraph6Likes: Int = 0,
    val paragraph6WritingTime: Long = 0,
    val paragraph6LikedBy: List<String> = emptyList(),
    val paragraph6CommentsCount: Int = 0,
    val taggedUsers: List<TaggedUser> = emptyList(),
    val createdAt: String = "",
    val timestamp: String = "",
    val status: String = Status.Active.value, // active, completed, archived
    val participantsCount: Int = 0,
    val totalContributions: Int = 0,
    val totalLikes: Int = 0,
    val totalReadTime: Long = 0, // estimated reading time in milliseconds
    val isPublic: Boolean = true,
    val isLiked: Boolean = false,
    val completedAt: String = "",
    val lastUpdatedAt: String = "",
    // New fields for sequential turn-based contributions
    val contributorTurnOrder: List<String> = emptyList(), // Ordered list of user IDs (tagged users in sequence)
    val currentTurnIndex: Int = 0, // Index in contributorTurnOrder indicating whose turn it is
    val currentTurnStartTime: String = "", // Timestamp when current turn started
    val turnDurationHours: Int = 3, // Number of hours each contributor has
    val maxParticipants: Int = 8, // Maximum total participants (1 initiator + 7 contributors)
    val skippedContributors: List<String> = emptyList() // User IDs who were skipped due to timeout
)

@Serializable
data class TaggedUser(
    val id: String = "",
    val username: String = "",
    val fullname: String = "",
    val thumbnail: String = "",
    val email: String
)

@Serializable
data class ParagraphComment(
    val id: String = "",
    val liveBookId: String = "",
    val paragraphNumber: Int = 0, // 0 for initial, 1-6 for contributor paragraphs
    val userId: String = "",
    val userName: String = "",
    val userThumbnail: String = "",
    val commentText: String = "",
    val timestamp: String = "",
    val createdAt: Long = 0L // Epoch milliseconds for sorting
)

sealed class Status(
    val value: String
) {
    data object Active : Status("active")
    data object Completed : Status("completed")
    data object Archived : Status("archived")
}