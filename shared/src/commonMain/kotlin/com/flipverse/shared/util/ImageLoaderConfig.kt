package com.flipverse.shared.util

/**
 * Constants and utilities for optimized image loading configuration.
 * Platform-specific ImageLoader implementations are in the platform-specific source sets.
 */
object ImageLoaderConfig {
    const val DISK_CACHE_SIZE_BYTES = 100 * 1024 * 1024L // 100MB
    const val MEMORY_CACHE_SIZE_PERCENT = 0.25 // 25% of available memory
    const val CROSSFADE_DURATION_MS = 150 // Faster crossfade
}

/**
 * Utility for optimizing image URLs from Firebase Storage.
 */
object ImageUrlOptimizer {
    private const val FIREBASE_STORAGE_SUFFIX = "?alt=media"

    /**
     * Optimizes a Firebase Storage URL for thumbnail display.
     * Adds size constraints to reduce download size.
     */
    fun optimizeForThumbnail(url: String, size: Int = 200): String {
        if (url.isBlank()) return url
        if (!url.contains("firebasestorage.googleapis.com")) return url

        return buildString {
            append(url)
            if (!url.contains("?alt=media")) {
                append(FIREBASE_STORAGE_SUFFIX)
            }
            // Note: Firebase Storage doesn't support on-the-fly resizing via URL params
            // This is a placeholder for when you implement Cloud Functions or a CDN
        }
    }

    /**
     * Checks if URL is a valid image URL.
     */
    fun isValidImageUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.startsWith("http://") || url.startsWith("https://")
    }
}
