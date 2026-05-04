package com.flipverse.shared.util

import android.content.Context

actual fun createShareManager(): ShareManager {

    return AndroidShareManager(
        AndroidShareManagerContext.context
            ?: throw IllegalStateException("Context not initialized")
    )
}