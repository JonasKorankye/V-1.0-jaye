package com.flipverse.shared.util

import kotlinx.cinterop.*
import platform.SystemConfiguration.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
actual class NetworkConnectivity {
    
    actual fun isConnected(): Boolean {
        val reachability = SCNetworkReachabilityCreateWithName(null, "www.google.com")
            ?: return false
        
        return memScoped {
            val flags = alloc<UIntVar>()
            val success = SCNetworkReachabilityGetFlags(reachability, flags.ptr)
            
            if (!success) {
                return@memScoped false
            }

            val flagsValue = flags.value
            val isReachable = (flagsValue and kSCNetworkReachabilityFlagsReachable) != 0u
            val needsConnection =
                (flagsValue and kSCNetworkReachabilityFlagsConnectionRequired) != 0u
            
            isReachable && !needsConnection
        }
    }
    
    actual suspend fun isConnectedAsync(): Boolean = withContext(Dispatchers.Default) {
        isConnected()
    }
    
    actual fun observeConnectivity(onConnectivityChanged: (Boolean) -> Unit) {
        println("🌐 Network observation not yet implemented for iOS")
        // Note: iOS network observation requires SCNetworkReachability callbacks
        // This is more complex and would require C callbacks bridging
        // For now, we can implement polling or manual checks
    }
    
    actual fun stopObserving() {
        println("🌐 Network observation stopping not yet implemented for iOS")
    }
}

actual suspend fun hasInternetConnection(): Boolean {
    return NetworkConnectivity().isConnectedAsync()
}