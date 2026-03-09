package org.dals.project.utils

/**
 * Cross-platform QR Code Scanner
 */
expect object QRCodeScanner {
    /**
     * Scan QR code from image file
     * @param filePath Path to image file
     * @return Decoded QR code data or null if not found
     */
    fun scanQRCodeFromFile(filePath: String): String?

    /**
     * Scan QR code from image object
     * @param image Platform-specific image object
     * @return Decoded QR code data or null
     */
    fun scanQRCodeFromImage(image: Any): String?

    /**
     * Validate if data is valid QR payment data
     * @param data String to validate
     * @return true if valid payment QR data
     */
    fun validateQRPaymentData(data: String): Boolean
}
