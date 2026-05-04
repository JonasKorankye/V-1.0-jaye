package com.golda.flipverse

import android.app.Application
import android.content.ContentResolver
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.flipverse.di.initializeKoin
import com.flipverse.shared.util.AndroidShareManagerContext
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize
import org.koin.android.ext.koin.androidContext
import androidx.core.net.toUri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

enum class AppState {
    FOREGROUND,
    BACKGROUND,
    INACTIVE
}

object AppStateManager : DefaultLifecycleObserver {
    private var currentState: AppState = AppState.INACTIVE
    private val listeners = mutableListOf<(AppState) -> Unit>()

    fun getCurrentState(): AppState = currentState

    fun addListener(listener: (AppState) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (AppState) -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners(newState: AppState) {
        listeners.forEach { it(newState) }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        currentState = AppState.FOREGROUND
        println("🟢 AppStateManager: App is now in FOREGROUND")
        notifyListeners(currentState)
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        currentState = AppState.BACKGROUND
        println("🟡 AppStateManager: App is now in BACKGROUND")
        notifyListeners(currentState)
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        // App is becoming inactive (partially visible or obscured)
        if (currentState == AppState.FOREGROUND) {
            currentState = AppState.INACTIVE
            println("🟠 AppStateManager: App is now INACTIVE")
            notifyListeners(currentState)
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        if (currentState == AppState.INACTIVE) {
            currentState = AppState.FOREGROUND
            println("🟢 AppStateManager: App resumed to FOREGROUND")
            notifyListeners(currentState)
        }
    }
}

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Register app lifecycle observer
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppStateManager)
        println("📱 AppStateManager registered")
        PDFBoxResourceLoader.init(applicationContext)



        Firebase.initialize(context = this)

        val customNotificationSound =
            (ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + "com.golda.flipverse" + "/" + R.raw.notification_sound).toUri()


        NotifierManager.initialize(
            configuration = NotificationPlatformConfiguration.Android(
                notificationIconResId = R.drawable.ic_notification,
                showPushNotification = true, // MUST be true for background/killed notifications
                notificationChannelData = NotificationPlatformConfiguration.Android.NotificationChannelData(
                    id = "default_channel",
                    name = "General",
                    soundUri = customNotificationSound.toString()
                )
            )
        )

        println("🔔 KMPNotifier initialized with showPushNotification=true for proper notification handling")

        // Initialize Koin first - this will set up the main notification listener
        initializeKoin(
            config = {
                androidContext(this@MyApplication)
            }
        )

        AndroidShareManagerContext.init(this)
    }
}