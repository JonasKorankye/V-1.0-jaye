package com.flipverse.shared.domain

import dev.gitlive.firebase.storage.File
import kotlinx.serialization.Serializable


@Serializable
data class Post(
    val id: String = "",
    val postId: String = "",
    val whatsNew: String = "",
    val source: String = "",
    val authorId: String = "", //use email as authorId
    val authorName: String = "",
    val authorHandle: String = "",
    val authorProfileImage: String? = null,
    val timestamp: String = "",
    val createdAt: String = "", // ISO format: "2024-01-15T10:30:00Z"
    val updatedAt: String? = null,
    val imageUrls: List<String> = emptyList(), // Multiple images support
    val videoUrl: String? = null,
    val thumbnailUrl: String? = null,
    val authorIsVerified: Boolean = false,
    val postType: PostType = PostType.RECOMMENDATION,
    val tags: List<String> = emptyList(),
    val location: Location? = null,
    val engagement: PostEngagement = PostEngagement(),
    val privacy: PrivacyLevel = PrivacyLevel.PUBLIC,
    val isPromoted: Boolean = false, // For sponsored/promoted posts
    val promotionScore: Double = 0.0, // For ranking promoted posts
    val hashtags: List<String> = emptyList(),
    val mentions: List<String> = emptyList(),
    val engagementScore: Double = 0.0, // calculated engagement score
    val trendingScore: Double = 0.0, // calculated trending score
    val qualityScore: Double = 0.0, // content quality score
    val language: String = "en",
    val selectedInterests: List<String> = emptyList(),
    val selectedSuggestions: List<String> = emptyList(),
    val isEdited: Boolean = false,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val metadata: PostMetadata = PostMetadata()
)

@Serializable
enum class PostType {
    RECOMMENDATION,
    REVIEW,
    QUOTE,
    EVENT,
    ARTICLE,
    STORY
}

@Serializable
enum class PrivacyLevel {
    PUBLIC,
    FRIENDS,
    PRIVATE
}

@Serializable
data class PostEngagement(
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val viewsCount: Int = 0,
    val bookmarksCount: Int = 0,
    // Track user interactions
    val isLikedByCurrentUser: Boolean = false,
    val isBookmarkedByCurrentUser: Boolean = false,
    var isSharedByCurrentUser: Boolean = false
)

@Serializable
data class Location(
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val city: String = "",
    val country: String = ""
)

@Serializable
data class PostMetadata(
    val readTimeMinutes: Int = 0, // For articles
    val wordCount: Int = 0,
    val deviceInfo: String = "", // "iOS", "Android", "Web"
    val appVersion: String = "",
    val moderationStatus: ModerationStatus = ModerationStatus.APPROVED,
    val reportCount: Int = 0,
    val featuredScore: Double = 0.0, // For trending/featured posts
    val searchKeywords: List<String> = emptyList() // For search optimization
)

@Serializable
enum class ModerationStatus {
    PENDING,
    APPROVED,
    REJECTED,
    FLAGGED
}


@Serializable
data class Follow(
    val id: String = "",
    val followerId: String = "", // Current user ID eg. email
    val followingId: String = "", // ID of user being followed
    val followedAt: String = "",
    val isActive: Boolean = true
)


data class CreatePostRequest(
    val whatsNew: String = "",
    val source: String = "",
    val imageFiles: List<File> = emptyList(),
    val videoFiles: List<File> = emptyList(),
    val postType: PostType = PostType.RECOMMENDATION,
    val category: String? = "",
    val timestamp: Long = 0L,
    val tags: List<String> = emptyList(),
    val location: Location? = null,
    val privacy: PrivacyLevel = PrivacyLevel.PUBLIC,
    val scheduledAt: Long? = null // For scheduled posts
)

@Serializable
data class Comment(
    val id: String = "",
    val commentById: String = "",
    val commentBy: String = "",
    val commentThumbnail: String = "",
    val commentText: String = "",
    val likedBy: List<String> = emptyList(),
    val likesCount: Int = 0,
    val timestamp: String = "",
    val engagement: PostEngagement = PostEngagement(),
    val reply: Reply = Reply(),
    val repliesCount: Int = 0,
    val profileThumbnail: String? = null,
    val fullname: String? = null
)

@Serializable
data class Reply (
    val id: String = "",
    val replyById: String = "",
    val replyBy: String = "",
    val replyText: String = "",
    val timestamp: String = "",
)

@Serializable
data class PostWithComments(
    val post: Post,
    val comments: List<Comment>
)

@Serializable
data class PinnedPosts(
    val postId: String,
    val timestamp: String
)

@Serializable
data class CreatePostState(
    val isCreating: Boolean = false,
    val isUploadingMedia: Boolean = false,
    val uploadProgress: Float = 0f,
    val createdPost: Post? = null,
    val currentUser: User? = null,
    val error: String? = null,
    val mediaUploadError: String? = null,
    val feedPopulationStatus: FeedPopulationStatus = FeedPopulationStatus.NotStarted
)

@Serializable
enum class FeedPopulationStatus {
    NotStarted,
    InProgress,
    Completed,
    Failed
}

@Serializable
data class MediaUploadResult(
    val url: String,
    val thumbnailUrl: String? = null,
    val type: MediaType
)

@Serializable
enum class MediaType {
    IMAGE, VIDEO, THUMBNAIL
}


@Serializable
data class FeedItem(
    val id: String = "",
    val userId: String = "", // Feed owner (follower)
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorProfileImage: String? = null,
    val authorIsVerified: Boolean = false,
    val title: String = "",
    val source: String = "",
    val content: String = "",
    val timestamp: String = "",
    val createdAt: String = "",
    val imageUrls: List<String> = emptyList(),
    val videoUrl: String? = null,
    val thumbnailUrl: String? = null,
    val postType: PostType = PostType.RECOMMENDATION,
    val category: String = "",
    val tags: List<String> = emptyList(),
    val engagement: PostEngagement = PostEngagement(),
    val privacy: PrivacyLevel = PrivacyLevel.PUBLIC,
    val feedItemCreatedAt: String
)

@Serializable
data class UserInteraction(
    val userId: String = "",
    val targetUserId: String? = null,
    val postId: String? = null,
    val interactionType: String = "", // "like", "comment", "share", "follow", "view"
    val timestamp: Long = 0L,
    val interests: List<String> = emptyList(),
    val duration: Long? = null // for view interactions
)

@Serializable
data class TrendingCriteria(
    val timeWindowHours: Int = 24,
    val minEngagementScore: Double = 0.1,
    val minQualityScore: Double = 0.3,
    val categoryWeights: Map<String, Double> = emptyMap(),
    val recencyWeight: Double = 0.3,
    val engagementWeight: Double = 0.4,
    val qualityWeight: Double = 0.3
)

@Serializable
data class SuggestionCriteria(
    val minFollowers: Int = 50,
    val maxFollowers: Int = 1000,
    val minPostCount: Int = 1000,
    val minQualityScore: Double = 0.4,
    val minEngagementRate: Double = 0.02,
    val categoryMatchWeight: Double = 0.4,
    val mutualConnectionsWeight: Double = 0.3,
    val activityWeight: Double = 0.3
)


