package com.flipverse.shared.navigation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Simple typealias for notification payload data
 * Replaces KMPNotifier's PayloadData to avoid dependency on Android-only library
 */
typealias PayloadData = Map<String, Any?>

/**
 * Bridge for handling notification navigation without module dependencies
 */
object NotificationNavigationBridge {

    private var navigationHandler: ((NotificationNavigationInfo) -> Unit)? = null

    // Create a proper coroutine scope for handling async operations
    private val bridgeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun setNavigationHandler(handler: (NotificationNavigationInfo) -> Unit) {
        navigationHandler = handler
        println("🌉 NotificationNavigationBridge: Navigation handler set")
    }

    fun handleNotificationClick(data: PayloadData) {
        println("🌉 NotificationNavigationBridge: Processing notification click")
        println("🌉 Raw payload data: $data")
        println("🌉 Navigation handler available: ${navigationHandler != null}")

        if (navigationHandler == null) {
            println("⚠️ CRITICAL: NotificationNavigationBridge: Navigation handler is null!")
            println("⚠️ This means the handler wasn't set in App.kt or was reset")
            println("⚠️ Attempting to wait for handler initialization...")

            // Try to wait a bit for handler to be set (app might still be initializing)
            // This is a fallback for when app is launched from killed state
            bridgeScope.launch {
                delay(1000) // Wait 1 second
                if (navigationHandler != null) {
                    println("✅ Handler now available after delay, retrying...")
                    handleNotificationClick(data) // Retry
                } else {
                    println("❌ Handler still not available after delay")
                }
            }
            return
        }

        val navigationInfo = parseNotificationData(data)
        if (navigationInfo != null) {
            println("🌉 NotificationNavigationBridge: Created navigation info: $navigationInfo")
            println("🌉 Attempting to invoke navigation handler...")

            try {
                navigationHandler?.invoke(navigationInfo)
                println("✅ NotificationNavigationBridge: Navigation handler invoked successfully")
            } catch (e: Exception) {
                println("❌ NotificationNavigationBridge: Error invoking navigation handler: ${e.message}")
                e.printStackTrace()

                // Fallback: try navigating to home
                println("🏠 NotificationNavigationBridge: Attempting fallback navigation to home")
                try {
                    navigationHandler?.invoke(NotificationNavigationInfo.Home)
                    println("✅ NotificationNavigationBridge: Fallback home navigation successful")
                } catch (fallbackError: Exception) {
                    println("❌ NotificationNavigationBridge: Fallback navigation also failed: ${fallbackError.message}")
                }
            }
        } else {
            println("🌉 NotificationNavigationBridge: Could not parse notification data, navigating to home")
            // Fallback to home
            try {
                navigationHandler?.invoke(NotificationNavigationInfo.Home)
                println("✅ NotificationNavigationBridge: Home navigation successful")
            } catch (e: Exception) {
                println("❌ NotificationNavigationBridge: Home navigation failed: ${e.message}")
            }
        }
    }

    private fun parseNotificationData(data: PayloadData): NotificationNavigationInfo? {
        // Log all payload data for debugging
        println("🔍 Parsing notification data:")
        data.forEach { (key, value) ->
            println("   $key: $value")
        }

        // Get notification type from multiple possible fields
        val messageType = data["type"]?.toString() ?: data["customData"]?.toString()
        val conversationId = data["conversationId"]?.toString()
        val otherUserId = data["senderId"]?.toString()
            ?: data["recipientId"]?.toString()
            ?: data["user_id"]?.toString()
        val postId = data["postId"]?.toString()
            ?: data["post_id"]?.toString()
            ?: data["messageId"]?.toString() // Used in original Firebase implementation
        val liveBookId = data["liveBookId"]?.toString()

        println("🔍 Parsed values:")
        println("   messageType: $messageType")
        println("   conversationId: $conversationId")
        println("   otherUserId: $otherUserId")
        println("   postId: $postId")
        println("   liveBookId: $liveBookId")

        return when (messageType) {
            "chat_message", "message", "new_message", "chat_notification" -> {
                if (conversationId != null && otherUserId != null) {
                    println("🗨️ Creating Chat navigation")
                    NotificationNavigationInfo.Chat(conversationId, otherUserId)
                } else {
                    println("⚠️ Chat notification missing required data")
                    null
                }
            }

            "post_like", "post_comment", "post_mention", "like_notification", "comment_notification" -> {
                if (postId != null) {
                    println("📄 Creating Post navigation")
                    NotificationNavigationInfo.Post(postId)
                } else {
                    println("⚠️ Post notification missing postId")
                    null
                }
            }

            "profile_follow", "profile_mention", "follow_notification" -> {
                if (otherUserId != null) {
                    println("👤 Creating Profile navigation")
                    NotificationNavigationInfo.Profile(otherUserId)
                } else {
                    println("⚠️ Profile notification missing userId")
                    null
                }
            }

            "livebook_message", "livebook_contribution", "livebook_mention", "livebook_notification" -> {
                if (conversationId != null) {
                    println("📖 Creating LiveBook navigation")
                    NotificationNavigationInfo.LiveBook(conversationId)
                } else {
                    println("⚠️ LiveBook notification missing conversationId")
                    null
                }
            }

            else -> {
                println("🔍 Unknown message type '$messageType', trying fallback parsing")
                // Fallback based on available data
                when {
                    conversationId != null && otherUserId != null -> {
                        println("🗨️ Fallback: Creating Chat navigation")
                        NotificationNavigationInfo.Chat(conversationId, otherUserId)
                    }

                    postId != null -> {
                        println("📄 Fallback: Creating Post navigation")
                        NotificationNavigationInfo.Post(postId)
                    }

                    otherUserId != null -> {
                        println("👤 Fallback: Creating Profile navigation")
                        NotificationNavigationInfo.Profile(otherUserId)
                    }

                    liveBookId != null -> {
                        println("📖 Fallback: Creating LiveBook navigation")
                        NotificationNavigationInfo.LiveBook(liveBookId)
                    }

                    else -> {
                        println("❌ No valid navigation data found")
                        null
                    }
                }
            }
        }
    }
}

sealed class NotificationNavigationInfo {
    abstract val targetRoute: MainDashboardRoutes
    abstract val path: String
    abstract val parameters: Map<String, String>

    data class Chat(val conversationId: String, val otherUserId: String) :
        NotificationNavigationInfo() {
        override val targetRoute = MainDashboardRoutes.FlipChatPages
        override val path = "/chat_dm"
        override val parameters =
            mapOf("conversationId" to conversationId, "otherUserId" to otherUserId)
    }

    data class Post(val postId: String) : NotificationNavigationInfo() {
        override val targetRoute = MainDashboardRoutes.FlipHomePages
        override val path = "/post_details"
        override val parameters = mapOf("postId" to postId)
    }

    data class Profile(val userId: String) : NotificationNavigationInfo() {
        override val targetRoute = MainDashboardRoutes.FlipHomePages
        override val path = "/view_profile"
        override val parameters = mapOf("userId" to userId)
    }

    data class LiveBook(val liveBookId: String) : NotificationNavigationInfo() {
        override val targetRoute = MainDashboardRoutes.FlipLiveBookPages
        override val path = "/view_livebook"
        override val parameters = mapOf("liveBookId" to liveBookId)
    }

    object Home : NotificationNavigationInfo() {
        override val targetRoute = MainDashboardRoutes.FlipHomePages
        override val path = "/home"
        override val parameters = emptyMap<String, String>()
    }
}