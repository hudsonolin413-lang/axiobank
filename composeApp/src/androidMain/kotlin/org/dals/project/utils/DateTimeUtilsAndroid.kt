package org.dals.project.utils

import java.time.LocalDateTime as JavaLocalDateTime

actual fun platformGetCurrentTimestamp(): String {
    return try {
        JavaLocalDateTime.now().toString()
    } catch (e: Exception) {
        "2025-11-30T12:00:00"
    }
}
