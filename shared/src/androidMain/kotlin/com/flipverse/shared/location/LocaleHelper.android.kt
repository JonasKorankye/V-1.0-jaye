package com.flipverse.shared.location

import java.util.Locale

actual object LocaleHelper {
    actual fun getDeviceCountry(): String? {
        return try {
            Locale.getDefault().country.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }

    actual fun getDeviceCountryName(): String? {
        return try {
            Locale.getDefault().displayCountry.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }
}