package com.flipverse.shared.util

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent

class AndroidNotifier(
    private val context: Context,
    private val icNotification: Int,
    private val customNotificationSound: Uri
) : Notifier {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun showNotification(title: String, message: String) {
        showNotificationWithIntent(title, message)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showNotificationWithIntent(
        title: String,
        message: String,
        pendingIntent: PendingIntent? = null,
    ) {
        println("🔔 AndroidNotifier: Starting to show notification")
        println("   - Title: $title")
        println("   - Message: $message")
        println("   - PendingIntent: ${if (pendingIntent != null) "Present" else "NULL"}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "default_channel",
                "General",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "FlipVerse notifications"
            channel.enableLights(true)
            channel.enableVibration(true)

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            println("🔔 Notification channel created/updated")
        }

        val builder = NotificationCompat.Builder(context, "default_channel")
            .setSmallIcon(icNotification)
            .setContentTitle(title)
            .setContentText(message)
            .setSound(customNotificationSound)
            .setAutoCancel(true) // Automatically remove notification when clicked
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)

        // CRITICAL: Set the content intent if provided
        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
            println("🔔 PendingIntent attached to notification")
        } else {
            println("⚠️ WARNING: PendingIntent is NULL - notification won't be clickable!")
        }

        val notification = builder.build()
        val notificationId = System.currentTimeMillis().toInt()

        println("🔔 Displaying notification with ID: $notificationId")

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            println("✅ Notification displayed successfully")
        } catch (e: Exception) {
            println("❌ Failed to display notification: ${e.message}")
            e.printStackTrace()
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun scheduleNotification(title: String, message: String, delayMillis: Long) {
        Handler(Looper.getMainLooper()).postDelayed({
            showNotification(title, message)
        }, delayMillis)
    }
}