package com.flipverse.shared.util

/**
 * Cross-platform network connectivity checker
 */
expect class NetworkConnectivity() {
    /**
     * Check if device has internet connection
     * @return true if connected, false otherwise
     */
    fun isConnected(): Boolean

    /**
     * Check if device has internet connection (suspend version for coroutines)
     * @return true if connected, false otherwise
     */
    suspend fun isConnectedAsync(): Boolean

    /**
     * Observe network connectivity changes
     * @param onConnectivityChanged callback invoked when connectivity changes
     */
    fun observeConnectivity(onConnectivityChanged: (Boolean) -> Unit)

    /**
     * Stop observing network connectivity changes
     */
    fun stopObserving()
}

/**
 * Global helper to check internet connection
 */
expect suspend fun hasInternetConnection(): Boolean