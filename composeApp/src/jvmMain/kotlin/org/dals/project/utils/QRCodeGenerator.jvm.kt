package org.dals.project.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

actual object QRCodeGenerator {

    /**
     * Generate QR code with logo in the center
     */
    actual fun generateQRCode(
        data: String,
        size: Int,
        logoPath: String?
    ): ImageBitmap? {
        return try {
            // Set QR code parameters with high error correction to allow logo overlay
            val hints = hashMapOf<EncodeHintType, Any>()
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H // High error correction
            hints[EncodeHintType.MARGIN] = 1

            // Generate QR code
            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, size, size, hints)

            // Create BufferedImage
            val width = bitMatrix.width
            val height = bitMatrix.height
            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

            // Fill QR code pixels
            for (x in 0 until width) {
                for (y in 0 until height) {
                    image.setRGB(x, y, if (bitMatrix[x, y]) Color.BLACK.rgb else Color.WHITE.rgb)
                }
            }

            // Add logo if provided
            if (logoPath != null) {
                addLogoToQRCode(image, logoPath)
            }

            // Convert BufferedImage to ImageBitmap
            bufferedImageToComposeBitmap(image)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Save QR code to file
     */
    actual fun saveQRCodeToFile(
        data: String,
        size: Int,
        logoPath: String?,
        outputPath: String
    ): Boolean {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>()
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
            hints[EncodeHintType.MARGIN] = 1

            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, size, size, hints)

            val width = bitMatrix.width
            val height = bitMatrix.height
            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    image.setRGB(x, y, if (bitMatrix[x, y]) Color.BLACK.rgb else Color.WHITE.rgb)
                }
            }

            if (logoPath != null) {
                addLogoToQRCode(image, logoPath)
            }

            val outputFile = File(outputPath)
            ImageIO.write(image, "PNG", outputFile)
            println("✅ QR Code saved to: $outputPath")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Get QR code as PNG byte array for sharing
     */
    actual fun getQRCodeBytes(
        data: String,
        size: Int,
        logoPath: String?
    ): ByteArray? {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>()
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
            hints[EncodeHintType.MARGIN] = 1

            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, size, size, hints)

            val width = bitMatrix.width
            val height = bitMatrix.height
            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    image.setRGB(x, y, if (bitMatrix[x, y]) Color.BLACK.rgb else Color.WHITE.rgb)
                }
            }

            if (logoPath != null) {
                addLogoToQRCode(image, logoPath)
            }

            val baos = ByteArrayOutputStream()
            ImageIO.write(image, "PNG", baos)
            baos.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Add logo to the center of QR code
     */
    private fun addLogoToQRCode(qrImage: BufferedImage, logoPath: String) {
        try {
            // Load logo
            val logoFile = File(logoPath)
            if (!logoFile.exists()) {
                println("⚠️ Logo file not found: $logoPath")
                return
            }

            val logo = ImageIO.read(logoFile)

            // Calculate logo size (20% of QR code size)
            val logoSize = (qrImage.width * 0.2).toInt()

            // Calculate position (center)
            val logoX = (qrImage.width - logoSize) / 2
            val logoY = (qrImage.height - logoSize) / 2

            // Create graphics context
            val g2d: Graphics2D = qrImage.createGraphics()
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            // Draw white background circle for logo (for better visibility)
            g2d.color = Color.WHITE
            val padding = 4
            g2d.fillRoundRect(
                logoX - padding,
                logoY - padding,
                logoSize + (padding * 2),
                logoSize + (padding * 2),
                20,
                20
            )

            // Draw logo
            g2d.drawImage(logo, logoX, logoY, logoSize, logoSize, null)
            g2d.dispose()
        } catch (e: Exception) {
            e.printStackTrace()
            println("⚠️ Failed to add logo to QR code: ${e.message}")
        }
    }

    /**
     * Convert BufferedImage to Compose ImageBitmap
     */
    private fun bufferedImageToComposeBitmap(bufferedImage: BufferedImage): ImageBitmap {
        val width = bufferedImage.width
        val height = bufferedImage.height

        // Get pixel data
        val pixels = IntArray(width * height)
        bufferedImage.getRGB(0, 0, width, height, pixels, 0, width)

        // Convert to byte array (RGBA format)
        val bytes = ByteArray(width * height * 4)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val offset = i * 4
            bytes[offset] = ((pixel shr 16) and 0xFF).toByte() // R
            bytes[offset + 1] = ((pixel shr 8) and 0xFF).toByte() // G
            bytes[offset + 2] = (pixel and 0xFF).toByte() // B
            bytes[offset + 3] = ((pixel shr 24) and 0xFF).toByte() // A
        }

        // Create Skia Bitmap
        val bitmap = Bitmap()
        val imageInfo = ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL)
        bitmap.installPixels(imageInfo, bytes, width * 4)

        return bitmap.asComposeImageBitmap()
    }
}
