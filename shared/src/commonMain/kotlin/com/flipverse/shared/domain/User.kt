package com.flipverse.shared.domain

import kotlinx.serialization.Serializable


@Serializable
data class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val username: String,
    val fullname: String,
    val bio:  String? = null,
    val createdAt: String,
    val hashedPasscode: String? = null,
    val salt:  String? = null,
    val lastLogin: String,
    val loginType: String,
    val imageUrl: String?,
    val thumbnail: String,
    val followersCount: Int,
    val followingCount: Int,
    val postsCount: Int,
    val selectedInterests: List<FlipNomenclatures.FlipInterests> = emptyList(),
    val selectedSuggestions: List<FlipNomenclatures.SuggestedFlipAccounts> = emptyList(),
    val firstTimeLogin: Boolean = false,
    val interests: List<String> = emptyList(),
    val selectedAccounts: List<String> = emptyList(),
    val isFollowing: Boolean,
    val avatar: String? = null,
    val isOwnProfile: Boolean = true,
    val passCode: String? = null,
    val isVerified: Boolean = false,
    val lastActiveAt: String = "",
    val engagementRate: Double = 0.0, // average engagement rate
    val qualityScore: Double = 0.0, // content quality score
    val location: String? = null,
    val language: String = "en", // default language
    val phoneNumber: PhoneNumber? = null
)

@Serializable
data class PhoneNumber(
    val dialCode: Int,
    val number: String
)

