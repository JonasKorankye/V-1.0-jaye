package com.flipverse.data

import com.flipverse.data.domain.NotificationRepository
import com.flipverse.data.util.CrashlyticsLogger
import com.flipverse.shared.domain.ReadNotificationRecord
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


class NotificationRepositoryImpl : NotificationRepository {
    private val dateString =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString()

    override suspend fun markNotificationAsRead(userId: String, notificationId: String) {
        try {
            val notificationCollection = Firebase.firestore.collection("read_notifications")
            val docRef = notificationCollection.document(userId)
            val doc = docRef.get()
            val exists = doc.exists
            if (exists) {
                val currentRecord = doc.data<ReadNotificationRecord>()
                val updatedList =
                    if (!currentRecord.notificationIds.contains(notificationId)) currentRecord.notificationIds + notificationId else currentRecord.notificationIds
                val newRecord = ReadNotificationRecord(
                    notificationIds = updatedList,
                    read = true,
                    timestamp = dateString
                )
                docRef.update(newRecord)
            } else {
                docRef.set(
                    ReadNotificationRecord(
                        notificationIds = listOf(notificationId),
                        read = true,
                        timestamp = dateString
                    )
                )
            }
        } catch (e: Exception) {
            println("Mark Notification As Read Error : ${e.message}")
            // Log to Crashlytics for debugging
            CrashlyticsLogger.logNonFatal(
                error = e,
                context = "markNotificationAsRead",
                additionalInfo = mapOf(
                    "user_id" to userId,
                    "notification_id" to notificationId
                )
            )
        }
    }

    override suspend fun markAllNotificationsAsRead(userId: String, notificationIds: List<String>) {
        try {
            val notificationCollection = Firebase.firestore.collection("read_notifications")
            val docRef = notificationCollection.document(userId)
            val doc = docRef.get()
            val exists = doc.exists
            val newRecord = ReadNotificationRecord(
                notificationIds = notificationIds,
                read = true,
                timestamp = dateString
            )
            if (exists) {
                docRef.update(newRecord)
            } else {
                docRef.set(newRecord)
            }
        } catch (e: Exception) {
            println("Mark All Notifications As Read Error : ${e.message}")
            // Log to Crashlytics for debugging
            CrashlyticsLogger.logNonFatal(
                error = e,
                context = "markAllNotificationsAsRead",
                additionalInfo = mapOf(
                    "user_id" to userId,
                    "notification_count" to notificationIds.size.toString()
                )
            )
        }
    }

    override suspend fun getReadNotificationIds(userId: String): List<String> {
        return try {
            val notificationCollection = Firebase.firestore.collection("read_notifications")
            val doc = notificationCollection.document(userId).get()
            if (doc.exists) {
                val currentRecord = doc.data<ReadNotificationRecord>()
                currentRecord.notificationIds
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            println("Get Read Notification Ids Error : ${e.message}")
            // Log to Crashlytics for debugging
            CrashlyticsLogger.logNonFatal(
                error = e,
                context = "getReadNotificationIds",
                additionalInfo = mapOf("user_id" to userId)
            )
            emptyList()
        }
    }

    override fun getReadNotificationIdsFlow(userId: String): Flow<List<String>> {
        return flow {
            try {
                val notificationCollection = Firebase.firestore.collection("read_notifications")
                notificationCollection.document(userId).snapshots.collect { snapshot ->
                    try {
                        if (snapshot.exists) {
                            val record = snapshot.data<ReadNotificationRecord>()
                            emit(record.notificationIds)
                        } else {
                            emit(emptyList())
                        }
                    } catch (e: Exception) {
                        println("Error parsing read notifications: ${e.message}")
                        emit(emptyList())
                    }
                }
            } catch (e: Exception) {
                println("Read notifications flow error: ${e.message}")
                emit(emptyList())
            }
        }
    }
}