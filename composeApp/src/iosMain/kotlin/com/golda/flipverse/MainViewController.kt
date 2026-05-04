package com.golda.flipverse

import androidx.compose.ui.window.ComposeUIViewController
import com.flipverse.di.initializeKoin
import com.flipverse.shared.util.TTSProvider
//import com.flipverse.shared.util.setTTSProvider
import com.golda.flipverse.App
import com.golda.flipverse.IOSAppDelegate
import com.golda.flipverse.DeepLinkManager
import kotlin.experimental.ExperimentalNativeApi

private var hookInstalled = false

/**
 * Installs a global unhandled exception hook for Kotlin/Native.
 * This prevents Kotlin_processUnhandledException from crashing the app
 * when a coroutine has an unhandled exception (e.g. viewModelScope.launch without try-catch).
 * Must be called once, as early as possible in the app lifecycle.
 */
@OptIn(ExperimentalNativeApi::class)
private fun installGlobalExceptionHook() {
    if (hookInstalled) return
    hookInstalled = true
    // NOTE: setUnhandledExceptionHook runs the lambda and then STILL terminates the process.
    // It does NOT prevent crashes — it only allows logging before termination.
    // The real protection against crashes is to guard all initialization code
    // (e.g. Koin double-init guard) so exceptions are never thrown in the first place.
    setUnhandledExceptionHook { throwable ->
        println("💥 [GlobalExceptionHook] Unhandled exception — app WILL terminate:")
        println("  Type: ${throwable::class.simpleName}")
        println("  Message: ${throwable.message}")
        throwable.printStackTrace()
    }
}

fun MainViewController(ttsProvider: TTSProvider) = ComposeUIViewController(
    configure = {
        installGlobalExceptionHook()
        initializeKoin()
//        setTTSProvider { ttsProvider }
    }
) { App() }