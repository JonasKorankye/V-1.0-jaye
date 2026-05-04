package com.flipverse.shared.util

import coil3.ImageLoader
import coil3.PlatformContext
import kotlinx.atomicfu.atomic

/**
 * Provides an optimized ImageLoader instance for iOS.
 * Uses Coil's default configuration with built-in caching.
 */
actual object ImageLoaderProvider {
    private val imageLoaderRef = atomic<ImageLoader?>(null)

    /**
     * Gets or creates the singleton optimized ImageLoader instance.
     */
    fun getImageLoader(): ImageLoader {
        imageLoaderRef.value?.let { return it }

        val newLoader = createImageLoader()
        if (imageLoaderRef.compareAndSet(null, newLoader)) {
            return newLoader
        }
        return imageLoaderRef.value!!
    }

    private fun createImageLoader(): ImageLoader {
        // Use Coil's default ImageLoader which includes built-in memory and disk caching
        return ImageLoader.Builder(PlatformContext.INSTANCE)
            .build()
    }

    /**
     * Clears the image cache (useful for logout or memory pressure).
     */
    actual suspend fun clearCache() {
        getImageLoader().diskCache?.clear()
        getImageLoader().memoryCache?.clear()
    }
}
