package com.flipverse.shared.location

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.countryCode
import platform.Foundation.localizedStringForCountryCode

actual object LocaleHelper {
    actual fun getDeviceCountry(): String? {
        return try {
            NSLocale.currentLocale.countryCode
        } catch (e: Exception) {
            null
        }
    }

    actual fun getDeviceCountryName(): String? {
        return try {
            val countryCode = NSLocale.currentLocale.countryCode
            countryCode?.let {
                NSLocale.currentLocale.localizedStringForCountryCode(it)
            }
        } catch (e: Exception) {
            null
        }
    }
}