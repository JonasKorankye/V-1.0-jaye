package com.flipverse.shared

import android.content.Context
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

class GooglePlayServicesChecker {
    fun checkGooglePlayServices(context: Context): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)

        return when (resultCode) {
            ConnectionResult.SUCCESS -> {
                Log.d("GooglePlayServices", "✅ Google Play Services is available")
                true
            }
            ConnectionResult.SERVICE_MISSING -> {
                Log.e("GooglePlayServices", "❌ Google Play Services is missing")
                false
            }
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> {
                Log.e("GooglePlayServices", "⚠️ Google Play Services needs update")
                false
            }
            ConnectionResult.SERVICE_DISABLED -> {
                Log.e("GooglePlayServices", "❌ Google Play Services is disabled")
                false
            }
            else -> {
                Log.e("GooglePlayServices", "❌ Google Play Services error: $resultCode")
                false
            }
        }
    }

    fun getGooglePlayServicesVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE, 0
            )
            packageInfo.versionName ?: com.flipverse.shared.Strings.unknown
        } catch (e: Exception) {
            Log.e("GooglePlayServices", "Error getting version: ${e.message}")
            "Not installed"
        }
    }

    fun handleGooglePlayServicesError(context: Context, resultCode: Int) {
        val googleApiAvailability = GoogleApiAvailability.getInstance()

        if (googleApiAvailability.isUserResolvableError(resultCode)) {
            // This would need to be called from an Activity
            Log.i("GooglePlayServices", "Error is user resolvable")
        } else {
            Log.e("GooglePlayServices", "This device is not supported")
        }
    }
}