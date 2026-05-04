package com.flipverse.data.util

import com.benasher44.uuid.uuid4
import dev.gitlive.firebase.firestore.Timestamp
import korlibs.crypto.SHA256
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalEncodingApi::class)
fun hashPassword(password: String, salt: String): String {
    val saltedPassword = password + salt
    val hash = SHA256.digest(saltedPassword.encodeToByteArray())
    return Base64.encode(hash.bytes)
}

fun verifyPassword(password: String, hashedPassword: String, salt: String): Boolean {
    val computedHash = hashPassword(password, salt)
    return computedHash == hashedPassword
}

@OptIn(ExperimentalEncodingApi::class)
fun generateSalt(): String {
    val saltBytes = ByteArray(16)
    repeat(16) { i ->
        saltBytes[i] = Random.nextInt(256).toByte()
    }
    return Base64.encode(saltBytes)
}

fun generateUserId(): String {
    return uuid4().toString()
}

fun generateOtp(): String {
    return (100000..999999).random().toString()
}

// Convert Firebase Timestamp → Instant
fun Timestamp.toInstant(): Instant {
    return Instant.fromEpochSeconds(seconds, nanoseconds)
}

/**
 * Platform-consistent string representation of a Timestamp.
 * Always produces: Timestamp(seconds=..., nanoseconds=...)
 * regardless of whether it's Android or iOS.
 */
fun Timestamp.toFormattedString(): String {
    return "Timestamp(seconds=$seconds, nanoseconds=$nanoseconds)"
}

// Convert Instant → Firebase Timestamp
fun Instant.toFirebaseTimestamp(): Timestamp {
    return Timestamp(epochSeconds, (nanosecondsOfSecond % 1_000_000_000).toLong().toInt())
}

// Convert Firebase Timestamp → LocalDateTime (for display)
fun Timestamp.toLocalDateTime(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDateTime {
    return this.toInstant().toLocalDateTime(timeZone)
}

fun formatChatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return ""

    val now = getCurrentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Now"
        diff < 3600000 -> "${diff / 60000}m"
        diff < 86400000 -> "${diff / 3600000}h"
        diff < 604800000 -> "${diff / 86400000}d"
        else -> "${diff / 604800000}w"
    }
}

/**
 * Returns the current timestamp in milliseconds
 */
fun getCurrentTimeMillis(): Long {
    return Clock.System.now().toEpochMilliseconds()
}

fun displayCurrentDateTime(): String {
    val currentTimestamp = Clock.System.now()
    val date = currentTimestamp.toLocalDateTime(TimeZone.currentSystemDefault())

    // Format the LocalDate into the desired representation
    val dayOfMonth = date.dayOfMonth
    val month =
        date.month.toString().lowercase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    val year = date.year

    // Determine the suffix for the day of the month
    val suffix = when {
        dayOfMonth in 11..13 -> "th" // Special case for 11th, 12th, and 13th
        dayOfMonth % 10 == 1 -> "st"
        dayOfMonth % 10 == 2 -> "nd"
        dayOfMonth % 10 == 3 -> "rd"
        else -> "th"
    }

    // Format the date in the desired representation
    return "$dayOfMonth$suffix $month, $year."
}

fun String.maskEmailSimple(): String {
    val atIndex = this.indexOf('@')

    // Basic validation for an email structure
    if (atIndex <= 0 || atIndex == this.length - 1) {
        return this // Return original if not a basic email format
    }

    val usernamePart = this.substring(0, atIndex)
    val domainPart = this.substring(atIndex) // Includes '@' and the domain

    // Get the first character of the username
    val firstChar = usernamePart.firstOrNull()?.toString() ?: ""

    // Determine the number of asterisks.
    // If the username has 1 char, no mask (e.g., "a@b.com" -> "a@b.com")
    // If the username has more than 1 char, mask all but the first.
    val numAsterisks = if (usernamePart.length > 1) usernamePart.length - 1 else 0

    val maskedUsername = if (numAsterisks > 0) {
        "$firstChar${"*".repeat(numAsterisks)}"
    } else {
        firstChar // If username is just one char, just return that char
    }

    return "$maskedUsername$domainPart"
}

fun randomFirestoreId(length: Int = 20): String {
    val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    return (1..length).map { allowedChars.random() }.joinToString("")
}

fun normalizeUsername(username: String): String {
    // Replace 1 or more leading @ with a single @
    return username.replace(Regex("^@+"), "@")
}

/**
 * Returns this string with the first character uppercased and the rest lowercased.
 * If the string is empty, it is returned unchanged.
 */
fun String.capitalizeFirstLetter(): String {
    if (isEmpty()) return this
    val firstUpper = this[0].uppercaseChar()
    val restLower = this.substring(1).lowercase()
    return firstUpper + restLower
}

fun formatCount(count: Int): String {
    return when {
        count >= 1000000 -> "${count / 1000000}M"
        count >= 1000 -> "${count / 1000}K"
        else -> count.toString()
    }
}

private val firestoreToStringPattern =
    Regex("""^Timestamp\(seconds=(\d+),\s*nanoseconds=(\d+)\)$""")
private val firestoreJsonPattern =
    Regex("""^\{\s*"_?seconds"\s*:\s*\d+\s*,\s*"_?nanoseconds"\s*:\s*\d+\s*\}$""")
private val isoInstantPattern =
    Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?Z$""")
private val firTimestampPattern =
    Regex("""^<FIRTimestamp:\s*seconds=(\d+)\s+nanoseconds=(\d+)>$""")

fun isFirebaseTimestampString(value: String?): Boolean {
    if (value.isNullOrBlank()) return false
    return firestoreToStringPattern.matches(value) ||
           firestoreJsonPattern.matches(value) ||
           isoInstantPattern.matches(value) ||
           firTimestampPattern.matches(value)
}

fun formatTimestamp(timestamp: String): String {
    println("Timestamp: " + timestamp)
    val toEpochMilliseconds =
        convertTimestampStringToEpochMilliseconds(timestamp)

    val now = Clock.System.now().toEpochMilliseconds()
    val diff = now - toEpochMilliseconds!!

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        diff < 604800000 -> "${diff / 86400000}d ago"
        else -> "${diff / 604800000}w ago"
    }
}

fun formatTimestamp(timestampMillis: Long): String {
    val now = getCurrentTimeMillis()
    val diff = now - timestampMillis

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        diff < 604800000 -> "${diff / 86400000}d ago"
        else -> "${diff / 604800000}w ago"
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val k = bytes / 1024.0
    if (k < 1024) {
        val rounded = (k * 10).toInt() / 10.0
        return "$rounded KB"
    }
    val m = k / 1024.0
    val rounded = (m * 10).toInt() / 10.0
    return "$rounded MB"
}

// Function to convert the full timestamp string directly to Long
fun convertTimestampStringToEpochMilliseconds(timestampString: String): Long? {
    val timestamp = parseTimestampString(timestampString)
    return timestamp?.toEpochMilliseconds()
}

/**
 * Parses an ISO 8601 or Firebase timestamp string to epoch milliseconds.
 * @return epoch milliseconds, or 0 if parsing fails
 */
fun parseAnyTimestampToEpochMillis(timestampString: String): Long {
    if (timestampString.isBlank()) return 0L
    return try {
        kotlinx.datetime.Instant.parse(timestampString).toEpochMilliseconds()
    } catch (_: Exception) {
        convertTimestampStringToEpochMilliseconds(timestampString) ?: 0L
    }
}

/**
 * Formats an ISO 8601 or Firebase timestamp string into a relative time string.
 * Supports formats like "2026-02-09T12:35:12.556693Z" and Firebase Timestamp strings.
 * @return A relative time string (e.g., "Just now", "5m ago", "3h ago", "2d ago", "1w ago")
 */
fun formatRelativeTime(timestampString: String): String {
    return try {
        // Try ISO 8601 instant format first
        val epochMillis = try {
            kotlinx.datetime.Instant.parse(timestampString).toEpochMilliseconds()
        } catch (_: Exception) {
            // Fall back to Firebase timestamp parsing
            convertTimestampStringToEpochMilliseconds(timestampString) ?: return "Unknown"
        }

        val now = Clock.System.now().toEpochMilliseconds()
        val diff = now - epochMillis

        when {
            diff < 60_000L -> "Just now"
            diff < 3_600_000L -> "${diff / 60_000L}m ago"
            diff < 86_400_000L -> "${diff / 3_600_000L}h ago"
            diff < 604_800_000L -> "${diff / 86_400_000L}d ago"
            else -> "${diff / 604_800_000L}w ago"
        }
    } catch (e: Exception) {
        "Unknown"
    }
}

// Extension function to convert a Timestamp object to milliseconds since epoch (Long)
fun Timestamp.toEpochMilliseconds(): Long {
    // Convert seconds to milliseconds
    val secondsToMillis = this.seconds * 1000L
    // Convert nanoseconds to milliseconds (there are 1,000,000 nanoseconds in 1 millisecond)
    val nanosecondsToMillis = this.nanoseconds / 1_000_000L


    return secondsToMillis + nanosecondsToMillis
}

// Function to parse the string and return a Timestamp object
// Supports both Android format: "Timestamp(seconds=..., nanoseconds=...)"
// and iOS format: "<FIRTimestamp: seconds=... nanoseconds=...>"
fun parseTimestampString(timestampString: String): Timestamp? {
    val androidRegex = """Timestamp\(seconds=(\d+),\s*nanoseconds=(\d+)\)""".toRegex()
    val iosRegex = """<FIRTimestamp:\s*seconds=(\d+)\s+nanoseconds=(\d+)>""".toRegex()

    val matchResult = androidRegex.find(timestampString)
        ?: iosRegex.find(timestampString)

    return if (matchResult != null) {
        val (secondsStr, nanosecondsStr) = matchResult.destructured
        try {
            val seconds = secondsStr.toLong()
            val nanoseconds = nanosecondsStr.toInt()
            Timestamp(seconds, nanoseconds)
        } catch (e: NumberFormatException) {
            println("Error parsing numbers from timestamp string: $e")
            null
        }
    } else {
        println("Timestamp string format mismatch: $timestampString")
        null
    }
}

/**
 * Calculates the completion percentage based on the number of completed paragraphs
 * @param count The number of completed paragraphs (1-7)
 * @return The percentage of completion (0-100)
 */
fun calculateContributionPercentage(count: Int): Int {
    return if (count <= 0) 0 else (count * 100) / 7
}

/**
 * Calculates the completion percentage for a LiveBook based on its paragraph content
 * @param liveBook The LiveBook to analyze
 * @return The percentage of completion (0-100)
 */
fun calculateLiveBookProgress(liveBook: com.flipverse.shared.domain.LiveBook): Int {
    var count = 0

    // Check if initial paragraph is completed (not empty)
    if (liveBook.initialParagraph.isNotBlank()) {
        count++
    }

    // Check paragraphs 1-6
    if (liveBook.paragraph1.isNotBlank()) count++
    if (liveBook.paragraph2.isNotBlank()) count++
    if (liveBook.paragraph3.isNotBlank()) count++
    if (liveBook.paragraph4.isNotBlank()) count++
    if (liveBook.paragraph5.isNotBlank()) count++
    if (liveBook.paragraph6.isNotBlank()) count++

    return calculateContributionPercentage(count)
}

/**
 * Calculates the time remaining within a 24-hour period from when the LiveBook was created
 * @param timestamp The LiveBook timestamp string
 * @return A formatted string showing time remaining (e.g., "5h left", "30m left", "Expired")
 */
fun calculateTimeRemaining(timestamp: String): String {
    return try {
        val timestampMillis = convertTimestampStringToEpochMilliseconds(timestamp)
            ?: return "Invalid time"

        val currentMillis = Clock.System.now().toEpochMilliseconds()
        val elapsedMillis = currentMillis - timestampMillis
        val twentyFourHoursMillis = 24 * 60 * 60 * 1000L // 24 hours in milliseconds
        val remainingMillis = twentyFourHoursMillis - elapsedMillis

        when {
            remainingMillis <= 0 -> "Expired"
            remainingMillis < 60 * 1000 -> "< 1m left" // Less than 1 minute
            remainingMillis < 60 * 60 * 1000 -> {
                val minutes = remainingMillis / (60 * 1000)
                "${minutes}m left"
            }

            else -> {
                val hours = remainingMillis / (60 * 60 * 1000)
                val minutes = (remainingMillis % (60 * 60 * 1000)) / (60 * 1000)
                if (minutes > 0) {
                    "${hours}h ${minutes}m left"
                } else {
                    "${hours}h left"
                }
            }
        }
    } catch (e: Exception) {
        "Invalid time"
    }
}

/**
 * Formats elapsed time in seconds to HH:MM:SS or MM:SS format
 * @param seconds The elapsed time in seconds
 * @return A formatted string (e.g., "01:23:45" or "23:45")
 */
fun formatElapsedTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secondsRemaining = seconds % 60
    return if (hours > 0) {
        "${hours.toString().padStart(2, '0')}:${
            minutes.toString().padStart(2, '0')
        }:${secondsRemaining.toString().padStart(2, '0')}"
    } else {
        "${minutes.toString().padStart(2, '0')}:${secondsRemaining.toString().padStart(2, '0')}"
    }
}

/**
 * Calculates the time remaining for a contributor's turn based on turn start time and duration
 * @param turnStartTime The timestamp string when the turn started
 * @param turnDurationHours The duration of the turn in hours (default 3)
 * @return A formatted string showing time remaining (e.g., "2h 30m left", "30m left", "Expired")
 */
fun calculateTurnTimeRemaining(turnStartTime: String, turnDurationHours: Int = 3): String {
    return try {
        // Try to parse as ISO instant first
        val turnStartInstant = try {
            kotlinx.datetime.Instant.parse(turnStartTime)
        } catch (e: Exception) {
            // If ISO parsing fails, try Firebase timestamp parsing
            val timestampMillis = convertTimestampStringToEpochMilliseconds(turnStartTime)
                ?: return "Invalid time"
            kotlinx.datetime.Instant.fromEpochMilliseconds(timestampMillis)
        }

        val currentInstant = Clock.System.now()
        val elapsedMillis =
            currentInstant.toEpochMilliseconds() - turnStartInstant.toEpochMilliseconds()
        val turnDurationMillis = turnDurationHours * 60 * 60 * 1000L
        val remainingMillis = turnDurationMillis - elapsedMillis

        when {
            remainingMillis <= 0 -> "Expired"
            remainingMillis < 60 * 1000 -> "< 1m left"
            remainingMillis < 60 * 60 * 1000 -> {
                val minutes = remainingMillis / (60 * 1000)
                "${minutes}m left"
            }

            else -> {
                val hours = remainingMillis / (60 * 60 * 1000)
                val minutes = (remainingMillis % (60 * 60 * 1000)) / (60 * 1000)
                if (minutes > 0) {
                    "${hours}h ${minutes}m left"
                } else {
                    "${hours}h left"
                }
            }
        }
    } catch (e: Exception) {
        "Invalid time"
    }
}

/**
 * Gets the name of the current turn holder from LiveBook
 * @param liveBook The LiveBook to check
 * @return The name of the current contributor whose turn it is, or null if no valid turn
 */
fun getCurrentTurnHolderName(liveBook: com.flipverse.shared.domain.LiveBook): String? {
    if (liveBook.contributorTurnOrder.isEmpty()) return null

    val currentTurnIndex = liveBook.currentTurnIndex
    val contributorId = liveBook.contributorTurnOrder.getOrNull(currentTurnIndex) ?: return null

    val contributor = liveBook.taggedUsers.find { it.id == contributorId }
    val name = contributor?.fullname?.takeIf { it.isNotBlank() } ?: contributor?.username
    val firstName = name?.split(" ")?.firstOrNull() ?: name
    return firstName?.replaceFirstChar { it.uppercaseChar() }
}
