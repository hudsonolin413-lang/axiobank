package org.dals.project.utils

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Cross-platform QR Code Generator
 */
expect object QRCodeGenerator {
    /**
     * Generate QR code as ImageBitmap with optional logo in center
     * @param data The data to encode in QR code (JSON string with account info)
     * @param size Size of the QR code in pixels
     * @param logoPath Optional path to logo image to place in center
     * @return ImageBitmap of the generated QR code
     */
    fun generateQRCode(
        data: String,
        size: Int = 512,
        logoPath: String? = null
    ): ImageBitmap?

    /**
     * Save QR code to file
     * @param data The data to encode
     * @param size Size in pixels
     * @param logoPath Optional logo path
     * @param outputPath Where to save the file
     * @return true if successful
     */
    fun saveQRCodeToFile(
        data: String,
        size: Int = 512,
        logoPath: String? = null,
        outputPath: String
    ): Boolean

    /**
     * Get QR code as byte array for sharing
     * @param data The data to encode
     * @param size Size in pixels
     * @param logoPath Optional logo path
     * @return PNG byte array
     */
    fun getQRCodeBytes(
        data: String,
        size: Int = 512,
        logoPath: String? = null
    ): ByteArray?
}
