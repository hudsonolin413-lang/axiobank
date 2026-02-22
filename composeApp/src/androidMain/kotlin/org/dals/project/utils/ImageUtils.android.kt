package org.dals.project.utils

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.util.Base64

actual fun decodeBase64ToImageBitmap(base64String: String): ImageBitmap? {
    return try {
        // Remove data:image/...;base64, prefix if present
        val base64Data = if (base64String.contains("base64,")) {
            base64String.substringAfter("base64,")
        } else {
            base64String
        }

        val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        bitmap?.asImageBitmap()
    } catch (e: Exception) {
        println("Error decoding base64 image on Android: ${e.message}")
        null
    }
}
