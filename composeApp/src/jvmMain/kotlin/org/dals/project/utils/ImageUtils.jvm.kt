package org.dals.project.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image as SkiaImage
import java.util.Base64

actual fun decodeBase64ToImageBitmap(base64String: String): ImageBitmap? {
    return try {
        // Remove data:image/...;base64, prefix if present
        val base64Data = if (base64String.contains("base64,")) {
            base64String.substringAfter("base64,")
        } else {
            base64String
        }

        val imageBytes = Base64.getDecoder().decode(base64Data)
        SkiaImage.makeFromEncoded(imageBytes).toComposeImageBitmap()
    } catch (e: Exception) {
        println("Error decoding base64 image on JVM: ${e.message}")
        null
    }
}
