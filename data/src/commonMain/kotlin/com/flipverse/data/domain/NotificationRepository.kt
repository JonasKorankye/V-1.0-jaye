package com.flipverse.data.domain

import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    suspend fun markNotificationAsRead(userId: String, notificationId: String)
    suspend fun markAllNotificationsAsRead(userId: String, notificationIds: List<String>)
    suspend fun getReadNotificationIds(userId: String): List<String>
    fun getReadNotificationIdsFlow(userId: String): Flow<List<String>>
}