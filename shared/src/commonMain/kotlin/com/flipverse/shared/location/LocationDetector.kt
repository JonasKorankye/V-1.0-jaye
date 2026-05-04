package com.flipverse.shared.location

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

// Expect/actual for platform-specific locale detection
expect object LocaleHelper {
    fun getDeviceCountry(): String?
    fun getDeviceCountryName(): String?
}

class LocationDetector {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            })
        }
    }

    /**
     * Detects user location using multiple fallback methods
     * Priority: IP Geolocation -> Locale -> Default
     */
    suspend fun detectUserLocation(): LocationInfo {
        return try {
            println("LocationDetector: Starting detection process...")

            // Primary: IP-based geolocation (most accurate for country)
            val ipResult = detectLocationByIP()
            if (ipResult != null && ipResult.countryName.isNotEmpty() && ipResult.countryName != "Unknown") {
                println("LocationDetector: Using IP-based result: ${ipResult.countryName}")
                return ipResult
            }

            // Fallback: Locale-based detection
            val localeResult = detectLocationByLocale()
            if (localeResult != null && localeResult.countryName.isNotEmpty() && localeResult.countryName != "Unknown") {
                println("LocationDetector: Using Locale-based result: ${localeResult.countryName}")
                return localeResult
            }

            // Last resort
            println("LocationDetector: All detection methods failed, using Unknown")
            LocationInfo(countryName = "Unknown", countryCode = "XX")
        } catch (e: Exception) {
            println("LocationDetector: Error detecting location - ${e.message}")
            e.printStackTrace()
            detectLocationByLocale() ?: LocationInfo(countryName = "Unknown", countryCode = "XX")
        }
    }

    /**
     * Detects location using IP geolocation service
     * Free tier: 1000 requests/day should be sufficient for most apps
     */
    private suspend fun detectLocationByIP(): LocationInfo? {
        return withContext(Dispatchers.Default) {
            try {
                println("LocationDetector: Attempting IP-based location detection...")
                val response = httpClient.get("https://ipapi.co/json/")
                val locationInfo = response.body<LocationInfo>()

                println("LocationDetector: IP-based response - $locationInfo")

                if (locationInfo.countryName.isNotEmpty()) {
                    println("LocationDetector: IP-based detection successful - ${locationInfo.countryName}")
                    locationInfo
                } else {
                    null
                }
            } catch (e: Exception) {
                println("LocationDetector: IP-based detection failed - ${e.message}")
                null
            }
        }
    }

    /**
     * Fallback: Detect country from device locale settings
     */
    private fun detectLocationByLocale(): LocationInfo? {
        return try {
            val countryCode = LocaleHelper.getDeviceCountry()
            val countryName = LocaleHelper.getDeviceCountryName()

            if (!countryCode.isNullOrEmpty() && !countryName.isNullOrEmpty()) {
                println("LocationDetector: Locale-based detection successful - $countryName")
                LocationInfo(
                    countryName = countryName,
                    countryCode = countryCode,
                    region = "",
                    city = "",
                    timezone = ""
                )
            } else {
                null
            }
        } catch (e: Exception) {
            println("LocationDetector: Locale-based detection failed - ${e.message}")
            null
        }
    }

    /**
     * Alternative IP geolocation service (backup)
     */
    private suspend fun detectLocationByIPInfo(): LocationInfo? {
        return withContext(Dispatchers.Default) {
            try {
                val response = httpClient.get("https://ipinfo.io/json")
                val data = response.body<Map<String, String>>()

                val country = data["country"] ?: return@withContext null
                val countryName = getCountryNameFromCode(country)

                LocationInfo(
                    countryName = countryName,
                    countryCode = country,
                    region = data["region"] ?: "",
                    city = data["city"] ?: "",
                    timezone = data["timezone"] ?: ""
                )
            } catch (e: Exception) {
                println("LocationDetector: IPInfo detection failed - ${e.message}")
                null
            }
        }
    }

    /**
     * Basic country code to name mapping for common countries
     */
    private fun getCountryNameFromCode(countryCode: String): String {
        return when (countryCode.uppercase()) {
            "US" -> "United States"
            "CA" -> "Canada"
            "GB" -> "United Kingdom"
            "FR" -> "France"
            "DE" -> "Germany"
            "JP" -> "Japan"
            "AU" -> "Australia"
            "BR" -> "Brazil"
            "IN" -> "India"
            "CN" -> "China"
            "GH" -> "Ghana"
            "RU" -> "Russia"
            "IT" -> "Italy"
            "ES" -> "Spain"
            "MX" -> "Mexico"
            "ZA" -> "South Africa"
            "KR" -> "South Korea"
            "NG" -> "Nigeria"
            "EG" -> "Egypt"
            "AR" -> "Argentina"
            "TR" -> "Turkey"
            else -> countryCode
        }
    }

    /**
     * Cache the detected location for session
     */
    companion object {
        private var cachedLocation: LocationInfo? = null

        suspend fun getCachedOrDetectLocation(): LocationInfo {
            return cachedLocation ?: run {
                val detector = LocationDetector()
                val location = detector.detectUserLocation()
                cachedLocation = location
                location
            }
        }

        fun clearLocationCache() {
            cachedLocation = null
        }
    }
}