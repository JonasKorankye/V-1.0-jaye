package com.flipverse.shared.util

import android.content.Context
import coil3.ImageLoader

/**
 * Provides an optimized ImageLoader instance for Android.
 * Uses Coil's default configuration with built-in caching.
 */
actual object ImageLoaderProvider {
    private var imageLoader: ImageLoader? = null

    /**
     * Gets or creates the singleton optimized ImageLoader instance.
     */
    fun getImageLoader(context: Context): ImageLoader {
        return imageLoader ?: synchronized(this) {
            imageLoader ?: ImageLoader.Builder(context).build().also { imageLoader = it }
        }
    }

    /**
     * Clears the image cache (useful for logout or memory pressure).
     */
    actual suspend fun clearCache() {
        imageLoader?.diskCache?.clear()
        imageLoader?.memoryCache?.clear()
    }
}
