package org.dals.project.utils

import com.google.zxing.*
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

actual object QRCodeScanner {

    /**
     * Scan QR code from image file
     */
    actual fun scanQRCodeFromFile(filePath: String): String? {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                println("❌ QR Scanner: File not found: $filePath")
                return null
            }

            val bufferedImage: BufferedImage = ImageIO.read(file)
            val result = decodeQRCode(bufferedImage)

            if (result != null) {
                println("✅ QR Scanner: Successfully decoded QR code")
            } else {
                println("⚠️ QR Scanner: No QR code found in image")
            }

            result
        } catch (e: Exception) {
            println("❌ QR Scanner: Error scanning QR code: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Scan QR code from BufferedImage
     */
    actual fun scanQRCodeFromImage(image: Any): String? {
        return try {
            if (image !is BufferedImage) {
                println("❌ QR Scanner: Invalid image type")
                return null
            }

            decodeQRCode(image)
        } catch (e: Exception) {
            println("❌ QR Scanner: Error decoding image: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Decode QR code from BufferedImage using ZXing
     */
    private fun decodeQRCode(bufferedImage: BufferedImage): String? {
        return try {
            val source = BufferedImageLuminanceSource(bufferedImage)
            val bitmap = BinaryBitmap(HybridBinarizer(source))

            val hints = mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true
            )

            val reader = MultiFormatReader()
            reader.setHints(hints)

            val result = reader.decode(bitmap)
            result.text
        } catch (e: NotFoundException) {
            null
        } catch (e: Exception) {
            println("❌ QR Scanner: Decode error: ${e.message}")
            null
        }
    }

    /**
     * Validate if string is valid QR payment data
     */
    actual fun validateQRPaymentData(data: String): Boolean {
        return try {
            // Check if it's valid JSON with required fields
            data.contains("accountNumber") &&
            data.contains("accountName") &&
            data.contains("customerId") &&
            data.startsWith("{") &&
            data.endsWith("}")
        } catch (e: Exception) {
            false
        }
    }
}
