package org.dals.project.utils

import kotlin.js.Date

actual fun platformGetCurrentTimestamp(): String {
    return try {
        val now = Date()
        now.toISOString()
    } catch (e: Exception) {
        "2025-11-30T12:00:00"
    }
}
