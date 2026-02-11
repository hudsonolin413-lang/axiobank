package org.dals.project.services

import java.text.SimpleDateFormat
import java.util.Date

actual fun generateTimestamp(): String {
    // Format: YYYYMMDDHHmmss
    val dateFormat = SimpleDateFormat("yyyyMMddHHmmss")
    return dateFormat.format(Date())
}
