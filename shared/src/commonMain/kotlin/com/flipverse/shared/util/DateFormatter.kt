package com.flipverse.shared.util

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

object DateFormatter {

    /**
     * Formats an ISO 8601 date string to a human-readable format
     * Example: "2024-01-15T10:30:00" -> "January 15, 2024"
     */
    fun formatToReadableDate(isoDateString: String): String {
        if (isoDateString.isEmpty()) return "Unknown"

        return try {
            val localDateTime = parseIsoDateTime(isoDateString)
            val monthName = getMonthName(localDateTime.monthNumber)
            val day = localDateTime.dayOfMonth
            val year = localDateTime.year

            "$monthName $day, $year"
        } catch (e: Exception) {
            println("Error parsing date: ${e.message}")
            "Unknown"
        }
    }

    /**
     * Formats an ISO 8601 date string to a full format with time
     * Example: "2024-01-15T10:30:00" -> "January 15, 2024 at 10:30 AM"
     */
    fun formatToReadableDateWithTime(isoDateString: String): String {
        if (isoDateString.isEmpty()) return "Unknown"

        return try {
            val localDateTime = parseIsoDateTime(isoDateString)
            val monthName = getMonthName(localDateTime.monthNumber)
            val day = localDateTime.dayOfMonth
            val year = localDateTime.year
            val time = formatTime(localDateTime.hour, localDateTime.minute)

            "$monthName $day, $year at $time"
        } catch (e: Exception) {
            println("Error parsing date with time: ${e.message}")
            "Unknown"
        }
    }

    /**
     * Formats an ISO 8601 date string to a short format
     * Example: "2024-01-15T10:30:00" -> "Jan 15, 2024"
     */
    fun formatToShortDate(isoDateString: String): String {
        if (isoDateString.isEmpty()) return "Unknown"

        return try {
            val localDateTime = parseIsoDateTime(isoDateString)
            val monthName = getShortMonthName(localDateTime.monthNumber)
            val day = localDateTime.dayOfMonth
            val year = localDateTime.year

            "$monthName $day, $year"
        } catch (e: Exception) {
            println("Error parsing short date: ${e.message}")
            "Unknown"
        }
    }

    private fun parseIsoDateTime(isoDateString: String): LocalDateTime {
        // Handle various ISO formats
        val normalizedString = if (!isoDateString.contains('T')) {
            // If it's just a date, add midnight time
            "${isoDateString}T00:00:00"
        } else {
            isoDateString
        }

        // Try parsing as LocalDateTime first
        return try {
            LocalDateTime.parse(normalizedString)
        } catch (e: Exception) {
            // If that fails, try parsing as Instant and convert
            try {
                Instant.parse(normalizedString).toLocalDateTime(TimeZone.currentSystemDefault())
            } catch (e2: Exception) {
                // Last resort: try with Z timezone indicator
                val withZ =
                    if (!normalizedString.endsWith('Z')) "${normalizedString}Z" else normalizedString
                Instant.parse(withZ).toLocalDateTime(TimeZone.currentSystemDefault())
            }
        }
    }

    private fun getMonthName(month: Int): String {
        return when (month) {
            1 -> "January"
            2 -> "February"
            3 -> "March"
            4 -> "April"
            5 -> "May"
            6 -> "June"
            7 -> "July"
            8 -> "August"
            9 -> "September"
            10 -> "October"
            11 -> "November"
            12 -> "December"
            else -> "Unknown"
        }
    }

    private fun getShortMonthName(month: Int): String {
        return when (month) {
            1 -> "Jan"
            2 -> "Feb"
            3 -> "Mar"
            4 -> "Apr"
            5 -> "May"
            6 -> "Jun"
            7 -> "Jul"
            8 -> "Aug"
            9 -> "Sep"
            10 -> "Oct"
            11 -> "Nov"
            12 -> "Dec"
            else -> "Unknown"
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val period = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val displayMinute = minute.toString().padStart(2, '0')
        return "$displayHour:$displayMinute $period"
    }
}
