package org.dals.project.utils

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Platform-specific function to decode base64 image data to ImageBitmap
 */
expect fun decodeBase64ToImageBitmap(base64String: String): ImageBitmap?
