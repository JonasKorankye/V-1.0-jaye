package com.flipverse.shared.util

/**
 * Platform-agnostic interface for providing optimized ImageLoader instances.
 * Implementations are provided in platform-specific source sets.
 */
expect object ImageLoaderProvider {
    /**
     * Clears the image cache (useful for logout or memory pressure).
     */
    suspend fun clearCache()
}
