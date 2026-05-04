package com.flipverse.shared.domain

import kotlinx.serialization.Serializable

@Serializable
data class PostLike(
    val userId: String = "",
    val timestamp: String = ""
)