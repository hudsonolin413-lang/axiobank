package org.dals.project.utils

import kotlinx.datetime.*

/**
 * Platform-specific timestamp implementation
 * This allows JVM to use Java time API as fallback when kotlinx-datetime is not loaded
 */
expect fun platformGetCurrentTimestamp(): String

object DateTimeUtils {

    /**
     * Gets current timestamp as ISO string using actual system time
     */
    fun getCurrentTimestamp(): String {
        return try {
            // Use Java's System.currentTimeMillis() which is available on all platforms
            val currentMillis = System.currentTimeMillis()
            val instant = Instant.fromEpochMilliseconds(currentMillis)
            val currentDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            currentDateTime.toString()
        } catch (e: NoClassDefFoundError) {
            // Fallback to platform-specific implementation if kotlinx.datetime not loaded
            println("Warning: kotlinx.datetime not loaded, using platform fallback")
            try {
                platformGetCurrentTimestamp()
            } catch (e2: Exception) {
                println("Warning: Platform fallback failed: ${e2.message}")
                generateFallbackTimestamp()
            }
        } catch (e: Exception) {
            // Fallback if even System.currentTimeMillis fails
            println("Warning: Unable to get system time: ${e.message}")
            generateFallbackTimestamp()
        }
    }

    /**
     * Generate a fallback timestamp (only used if system clock fails)
     */
    private fun generateFallbackTimestamp(): String {
        // Current fallback timestamp
        val baseYear = 2025
        val baseMonth = 11
        val baseDay = 30
        val randomHour = (8..20).random()
        val randomMinute = (0..59).random()
        val randomSecond = (0..59).random()

        return LocalDateTime(
            baseYear, baseMonth, baseDay,
            randomHour, randomMinute, randomSecond
        ).toString()
    }

    /**
     * Formats datetime for display (e.g., "Jan 15, 2024 10:30 AM")
     */
    fun formatForDisplay(dateTime: LocalDateTime): String {
        return try {
            val date = dateTime.date
            val time = dateTime.time

            val monthName = getMonthName(date.monthNumber)
            val day = date.dayOfMonth
            val year = date.year

            val hour = if (time.hour == 0) 12 else if (time.hour > 12) time.hour - 12 else time.hour
            val minute = time.minute.toString().padStart(2, '0')
            val amPm = if (time.hour < 12) "AM" else "PM"

            "$monthName $day, $year $hour:$minute $amPm"
        } catch (e: Exception) {
            "Invalid Date"
        }
    }

    /**
     * Formats datetime for transaction display (e.g., "15 Jan 2024, 10:30")
     */
    fun formatForTransaction(dateTime: LocalDateTime): String {
        return try {
            val date = dateTime.date
            val time = dateTime.time

            val monthName = getShortMonthName(date.monthNumber)
            val day = date.dayOfMonth
            val year = date.year

            val hour = time.hour.toString().padStart(2, '0')
            val minute = time.minute.toString().padStart(2, '0')

            "$day $monthName $year, $hour:$minute"
        } catch (e: Exception) {
            "Invalid Date"
        }
    }

    /**
     * Formats datetime for statements (e.g., "2024-01-15 10:30:45")
     */
    fun formatForStatement(dateTime: LocalDateTime): String {
        return try {
            val date = dateTime.date
            val time = dateTime.time

            val year = date.year
            val month = date.monthNumber.toString().padStart(2, '0')
            val day = date.dayOfMonth.toString().padStart(2, '0')

            val hour = time.hour.toString().padStart(2, '0')
            val minute = time.minute.toString().padStart(2, '0')
            val second = time.second.toString().padStart(2, '0')

            "$year-$month-$day $hour:$minute:$second"
        } catch (e: Exception) {
            "Invalid Date"
        }
    }

    /**
     * Formats datetime for file names (e.g., "2024-01-15_10-30-45")
     */
    fun formatForFileName(dateTime: LocalDateTime): String {
        return try {
            val date = dateTime.date
            val time = dateTime.time

            val year = date.year
            val month = date.monthNumber.toString().padStart(2, '0')
            val day = date.dayOfMonth.toString().padStart(2, '0')

            val hour = time.hour.toString().padStart(2, '0')
            val minute = time.minute.toString().padStart(2, '0')
            val second = time.second.toString().padStart(2, '0')

            "${year}-${month}-${day}_${hour}-${minute}-${second}"
        } catch (e: Exception) {
            "invalid-date"
        }
    }

    /**
     * Formats date only (e.g., "January 15, 2024")
     */
    fun formatDateOnly(dateTime: LocalDateTime): String {
        return try {
            val date = dateTime.date
            val monthName = getMonthName(date.monthNumber)
            val day = date.dayOfMonth
            val year = date.year

            "$monthName $day, $year"
        } catch (e: Exception) {
            "Invalid Date"
        }
    }

    /**
     * Formats time only (e.g., "10:30 AM")
     */
    fun formatTimeOnly(dateTime: LocalDateTime): String {
        return try {
            val time = dateTime.time
            val hour = if (time.hour == 0) 12 else if (time.hour > 12) time.hour - 12 else time.hour
            val minute = time.minute.toString().padStart(2, '0')
            val amPm = if (time.hour < 12) "AM" else "PM"

            "$hour:$minute $amPm"
        } catch (e: Exception) {
            "Invalid Time"
        }
    }


    /**
     * Converts string timestamp to LocalDateTime
     */
    fun parseDateTime(timestamp: String): LocalDateTime? {
        return try {
            // Try different formats
            when {
                timestamp.contains('T') -> {
                    // ISO format: "2024-01-15T10:30:00"
                    LocalDateTime.parse(timestamp)
                }

                timestamp.contains(' ') -> {
                    // SQL format: "2024-01-15 10:30:00"
                    val parts = timestamp.split(' ')
                    if (parts.size == 2) {
                        val dateParts = parts[0].split('-')
                        val timeParts = parts[1].split(':')

                        if (dateParts.size == 3 && timeParts.size >= 2) {
                            val year = dateParts[0].toInt()
                            val month = dateParts[1].toInt()
                            val day = dateParts[2].toInt()
                            val hour = timeParts[0].toInt()
                            val minute = timeParts[1].toInt()
                            val second = if (timeParts.size > 2) timeParts[2].toInt() else 0

                            LocalDateTime(year, month, day, hour, minute, second)
                        } else null
                    } else null
                }

                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Converts LocalDateTime to ISO string
     */
    fun toIsoString(dateTime: LocalDateTime): String {
        return dateTime.toString()
    }


    private fun getMonthName(monthNumber: Int): String {
        return when (monthNumber) {
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

    private fun getShortMonthName(monthNumber: Int): String {
        return when (monthNumber) {
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
}

enum class DateRangePeriod {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    LAST_30_DAYS,
    THIS_YEAR
}