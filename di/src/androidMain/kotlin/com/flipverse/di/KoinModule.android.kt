package com.flipverse.di

import com.flipverse.data.domain.UserRepository
import com.flipverse.shared.navigation.NotificationNavigationBridge
import com.flipverse.shared.util.AndroidShareManager
import com.flipverse.shared.util.PhotoPicker
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.PayloadData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.module

actual val targetModule = module {
    factory<PhotoPicker> { PhotoPicker() }
    single{ AndroidShareManager(context = get()) }
}

/**
 * Android-specific notification setup using KMPNotifier
 */
actual fun KoinApplication.setupPlatformNotifications() {
    println("🤖 Android: Setting up KMPNotifier")
    val pushNotifier = NotifierManager.getPushNotifier()

    NotifierManager.addListener(object : NotifierManager.Listener {
        override fun onNewToken(token: String) {
            println("🔑 KMPNotifier onNewToken: $token")
            // Get UserRepository from Koin context and save token to Firebase
            val userRepository = this@setupPlatformNotifications.koin.get<UserRepository>()
            val localScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            localScope.launch {
                try {
                    val currentUserId = userRepository.getCurrentUserId()
                    if (currentUserId != null) {
                        println("💾 Saving push token for user: $currentUserId")
                        userRepository.updateMessageToken(
                            userId = currentUserId,
                            token = token,
                            onSuccess = {
                                println("✅ Push token saved to Firebase user collection successfully")
                            },
                            onError = { error ->
                                println("❌ Failed to save push token to Firebase: $error")
                            }
                        )
                    } else {
                        println("⚠️ No current user found, push token not saved")
                    }
                } catch (e: Exception) {
                    println("❌ Exception saving push token: ${e.message}")
                }
            }
        }

        override fun onNotificationClicked(data: PayloadData) {
            super.onNotificationClicked(data)
            println("👆 KMPNotifier notification clicked with data: $data")
            println("🔄 Notification click event received - routing to NotificationNavigationBridge")
            
            try {
                println("🔍 Debug: Notification clicked - processing payload data")
                data.forEach { (key, value) ->
                    println("   📦 Payload[$key]: $value")
                }

                println("🚀 CRITICAL: App may have been launched from KILLED state")
                println("🚀 This is the PRIMARY entry point for killed-state notifications")

                // Route notification click to NotificationNavigationBridge
                NotificationNavigationBridge.handleNotificationClick(data)
                println("✅ Notification click processed successfully through NotificationNavigationBridge")
            } catch (e: Exception) {
                println("❌ Error processing notification click: ${e.message}")
                e.printStackTrace()

                // Fallback: Try to at least navigate to home
                try {
                    println("🏠 Attempting fallback navigation to home screen")
                    val fallbackData = mapOf("type" to "home_fallback")
                    NotificationNavigationBridge.handleNotificationClick(fallbackData)
                } catch (fallbackError: Exception) {
                    println("❌ Even fallback navigation failed: ${fallbackError.message}")
                }
            }
        }

        override fun onPushNotificationWithPayloadData(
            title: String?,
            body: String?,
            data: PayloadData
        ) {
            println("📬 KMPNotifier push notification with payload received:")
            println("   Title: $title")
            println("   Body: $body")
            println("   Data: $data")

            data.forEach { (key, value) ->
                println("   Payload[$key]: $value")
            }

            println("📱 Storing notification payload for potential background processing")
            println("📱 KMPNotifier will display notification automatically")
            println("📱 Click handling will be processed through onNotificationClicked callback")
        }

        override fun onPushNotification(title: String?, body: String?) {
            super.onPushNotification(title, body)
            println("📨 Basic push notification received:")
            println("   Title: $title")
            println("   Body: $body")
        }

        override fun onPayloadData(data: PayloadData) {
            super.onPayloadData(data)
            println("📦 Payload data received independently: $data")
            data.forEach { (key, value) ->
                println("   Independent Payload[$key]: $value")
            }
        }
    })

    // Get initial token
    val koinScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    koinScope.launch {
        try {
            val initialToken = pushNotifier.getToken()
            if (initialToken != null) {
                println("🎯 Initial push token retrieved: $initialToken")
                val userRepository = this@setupPlatformNotifications.koin.get<UserRepository>()
                val currentUserId = userRepository.getCurrentUserId()
                if (currentUserId != null) {
                    println("💾 Saving push token for user: $currentUserId")
                    userRepository.updateMessageToken(
                        userId = currentUserId,
                        token = initialToken,
                        onSuccess = {
                            println("✅ Push token saved to Firebase user collection successfully")
                        },
                        onError = { error ->
                            println("❌ Failed to save push token to Firebase: $error")
                        }
                    )
                } else {
                    println("⚠️ No current user found, push token not saved")
                }
            } else {
                println("⏳ Initial push token not available yet, waiting for onNewToken callback")
            }
        } catch (e: Exception) {
            println("⚠️ Could not retrieve initial push token: ${e.message}")
        }
    }
}