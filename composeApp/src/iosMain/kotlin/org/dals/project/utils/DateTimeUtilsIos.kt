package org.dals.project.utils

import platform.Foundation.*

actual fun platformGetCurrentTimestamp(): String {
    return try {
        val date = NSDate()
        val formatter = NSDateFormatter().apply {
            dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
        }
        formatter.stringFromDate(date)
    } catch (e: Exception) {
        "2025-11-30T12:00:00"
    }
}
