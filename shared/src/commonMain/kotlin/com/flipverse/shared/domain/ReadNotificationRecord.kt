package com.flipverse.shared.domain

import kotlinx.serialization.Serializable


@Serializable
data class ReadNotificationRecord(
    val notificationIds: List<String> = emptyList(),
    val read: Boolean = false,
    val timestamp: String = ""
)