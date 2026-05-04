package com.golda.flipverse

import android.Manifest
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.annotation.RequiresPermission
import androidx.core.net.toUri
import com.flipverse.shared.util.AndroidNotifier
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class MyFirebaseMessagingService : FirebaseMessagingService() {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        println("🔥 FCM Data Received: ${remoteMessage.data}")

        // Detect current app state
        val appState = AppStateManager.getCurrentState()
        println("📱 App State: $appState")
        println(
            "📱 App is in ${
                when (appState) {
                    AppState.FOREGROUND -> "FOREGROUND - App is active and visible"
                    AppState.BACKGROUND -> "BACKGROUND - App is not visible but running"
                    AppState.INACTIVE -> "INACTIVE - App is transitioning or partially visible"
                }
            }"
        )

        val customData = remoteMessage.data["customData"]
        println("📋 CustomData Type: $customData")

        val title = remoteMessage.notification?.title ?: "New Message"
        val body = remoteMessage.notification?.body?.let { notificationBody ->
            if (notificationBody.startsWith("TEXT:")) {
                notificationBody.removePrefix("TEXT:")
            } else {
                notificationBody
            }
        } ?: ""
        val customNotificationSound =
            (ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + "com.golda.flipverse" + "/" + R.raw.notification_sound).toUri()

        var deepLinkUri = constructDeepLink(remoteMessage.data)
        println("🔗 Original DeepLink: $deepLinkUri")
        

        // If the encoded path contains %40 (at sign), replace it in the path and rebuild the URI – keeping its type
        if (deepLinkUri.encodedPath?.contains("%40") == true) {
            val newPath = deepLinkUri.encodedPath!!.replace("%40", "@")
            deepLinkUri = deepLinkUri.buildUpon().encodedPath(newPath).build()
            println("🔄 Updated DeepLink (after %40 replacement): $deepLinkUri")
        }

        println("✅ Final DeepLink URI: $deepLinkUri")
        println("   - Scheme: ${deepLinkUri.scheme}")
        println("   - Host: ${deepLinkUri.host}")
        println("   - Path: ${deepLinkUri.path}")
        println("   - Query: ${deepLinkUri.query}")

        // CRITICAL FIX: ALWAYS create a PendingIntent for notification click handling
        // This ensures the app can be launched from killed state
        println("🔧 CREATING PENDINGINTENT FOR ALL APP STATES (including background/killed)")

        // Create pending intent with deep link for ALL app states
        val intent = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
            setClass(applicationContext, MainActivity::class.java)
            setPackage(packageName)
            data = deepLinkUri

            // Store deep link in extras as backup
            putExtra("DEEP_LINK_URI", deepLinkUri.toString())
            putExtra("DEEP_LINK_SCHEME", deepLinkUri.scheme)
            putExtra("DEEP_LINK_HOST", deepLinkUri.host)
            putExtra("DEEP_LINK_PATH", deepLinkUri.path)
            putExtra("DEEP_LINK_QUERY", deepLinkUri.query)

            // Add all FCM data as extras for reliability
            remoteMessage.data.forEach { (key, value) ->
                putExtra("FCM_$key", value)
            }

            // CRITICAL: Flags to ensure app launches from killed state
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val requestCode = deepLinkUri.toString().hashCode()
        val pendingIntent = try {
            PendingIntent.getActivity(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } catch (e: Exception) {
            println("❌ Failed to create PendingIntent: ${e.message}")
            null
        }

        if (pendingIntent != null) {
            println("✅ PendingIntent created successfully for app state: $appState")

            val notifier = AndroidNotifier(
                applicationContext,
                R.drawable.ic_notification,
                customNotificationSound
            )
            notifier.showNotificationWithIntent(title, body, pendingIntent)

            println("📱 Custom notification displayed with clickable PendingIntent")
        } else {
            println("❌ CRITICAL ERROR: Failed to create PendingIntent - notification won't launch app!")
        }

        // Also store data for KMPNotifier callback (as backup)
        println("📦 FCM Data for KMPNotifier processing: ${remoteMessage.data}")
        println("📦 KMPNotifier callback available as secondary processing method")

        println("=".repeat(80))
        println("📋 NOTIFICATION HANDLING SUMMARY:")
        println("   App State: $appState")
        println("   Deep Link: $deepLinkUri")
        println("   PendingIntent: ${if (pendingIntent != null) "✅ CREATED" else "❌ FAILED"}")
        println("   Strategy: ALWAYS create PendingIntent for app launch capability")
        println("   Notification Display: Custom notification with launch capability")
        println("=".repeat(80))
    }

    // Construct a deep link Uri from push notification data
    private fun constructDeepLink(data: Map<String, String>): Uri {
        println("📋 Constructing deep link from data: $data")

        val customData = data["customData"]
        println("🏷️ Custom data type: $customData")

        return when (customData) {
            "livebook_notification" -> {
                val liveBookId = data["conversationId"] ?: com.flipverse.shared.Strings.unknown_lower
                println("📖 Constructing deep link for livebook notification - liveBookId: $liveBookId")
                "app://flipverse.com/view_livebook".toUri().buildUpon()
                    .appendQueryParameter("liveBookId", liveBookId)
                    .build().also {
                        println("📖 LiveBook deep link created: $it")
                    }
            }
            "chat_notification" -> {
                val conversationId = data["conversationId"] ?: com.flipverse.shared.Strings.unknown_lower
                val otherUserId = data["recipientId"] ?: com.flipverse.shared.Strings.unknown_lower
                println("💬 Constructing deep link for chat notification - conversationId: $conversationId, otherUserId: $otherUserId")
                "app://flipverse.com/chat_dm".toUri().buildUpon()
                    .appendQueryParameter("conversationId", conversationId)
                    .appendQueryParameter("otherUserId", otherUserId)
                    .build().also {
                        println("💬 Chat deep link created: $it")
                    }
            }
            "like_notification", "comment_notification" -> {
                val type = if (customData == "like_notification") "like" else "comment"
                val postId = data["messageId"] ?: com.flipverse.shared.Strings.unknown_lower
                println("👍 Constructing deep link for $type notification - postId: $postId")
                "app://flipverse.com/post_details".toUri().buildUpon()
                    .appendQueryParameter("postId", postId)
                    .build().also {
                        println("👍 Post details deep link created: $it")
                    }
            }
            "follow_notification" -> {
                val userId = data["recipientId"] ?: com.flipverse.shared.Strings.unknown_lower
                println("👥 Constructing deep link for follow notification - userId: $userId")
                "app://flipverse.com/view_profile".toUri().buildUpon()
                    .appendQueryParameter("userId", userId)
                    .build().also {
                        println("👥 Profile deep link created: $it")
                    }
            }
            else -> {
                println("🏠 Constructing deep link for default/unknown notification type: $customData")
                "app://flipverse.com/home".toUri().also {
                    println("🏠 Default deep link created: $it")
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onNewToken(token: String) {
        println("FCM Token: $token")
    }
}