package org.dals.project.services

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun generateTimestamp(): String {
    // Format: YYYYMMDDHHmmss
    val dateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
    return dateFormat.format(Date())
}
