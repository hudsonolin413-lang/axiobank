package org.dals.project.utils

import java.time.LocalDateTime as JavaLocalDateTime
import java.time.format.DateTimeFormatter

/**
 * JVM-specific DateTimeUtils implementation using Java time API
 * This is a workaround when kotlinx-datetime is not properly loaded
 */
actual fun platformGetCurrentTimestamp(): String {
    return try {
        JavaLocalDateTime.now().toString()
    } catch (e: Exception) {
        println("Warning: Unable to get system time: ${e.message}")
        // Fallback timestamp
        "2025-11-30T12:00:00"
    }
}
