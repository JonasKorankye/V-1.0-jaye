package com.flipverse.shared.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class NetworkConnectivity {
    private val context: Context
        get() = AndroidShareManagerContext.context
            ?: throw IllegalStateException("AndroidShareManagerContext not initialized. Call AndroidShareManagerContext.init() in Application.onCreate()")

    private val connectivityManager: ConnectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    actual fun isConnected(): Boolean {
        // Check if permission is granted
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_NETWORK_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            println("⚠️ ACCESS_NETWORK_STATE permission not granted")
            return false
        }

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities =
                    connectivityManager.getNetworkCapabilities(network) ?: return false

                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                @Suppress("DEPRECATION")
                networkInfo?.isConnected == true
            }
        } catch (e: Exception) {
            println("⚠️ Error checking network connectivity: ${e.message}")
            false
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    actual suspend fun isConnectedAsync(): Boolean = withContext(Dispatchers.IO) {
        isConnected()
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    actual fun observeConnectivity(onConnectivityChanged: (Boolean) -> Unit) {
        // Check if permission is granted
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_NETWORK_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            println("⚠️ ACCESS_NETWORK_STATE permission not granted - cannot observe connectivity")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    println("🌐 Network available")
                    onConnectivityChanged(true)
                }
                
                override fun onLost(network: Network) {
                    super.onLost(network)
                    println("🌐 Network lost")
                    onConnectivityChanged(false)
                }
                
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                     networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    println("🌐 Network capabilities changed - Has Internet: $hasInternet")
                    onConnectivityChanged(hasInternet)
                }
            }
            
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            try {
                connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
                println("🌐 Network callback registered")
            } catch (e: Exception) {
                println("⚠️ Error registering network callback: ${e.message}")
            }
        }
    }

    actual fun stopObserving() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
                println("🌐 Network callback unregistered")
            } catch (e: Exception) {
                println("⚠️ Error unregistering network callback: ${e.message}")
            }
            networkCallback = null
        }
    }
}

@RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
actual suspend fun hasInternetConnection(): Boolean {
    return NetworkConnectivity().isConnectedAsync()
}