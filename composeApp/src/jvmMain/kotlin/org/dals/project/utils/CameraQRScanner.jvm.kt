package org.dals.project.utils

import com.github.sarxos.webcam.Webcam
import com.google.zxing.*
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.awt.Dimension
import java.awt.image.BufferedImage
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.ImageIcon
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel
import java.awt.FlowLayout
import javax.swing.SwingUtilities

/**
 * Desktop camera-based QR code scanner using webcam
 */
object CameraQRScanner {
    private var isScanning = false
    private var webcam: Webcam? = null
    private var scannerFrame: JFrame? = null

    /**
     * Opens camera window and continuously scans for QR codes
     * Returns the first valid QR code data found
     */
    suspend fun scanQRCodeWithCamera(onResult: (String?) -> Unit) = withContext(Dispatchers.IO) {
        try {
            // Get default webcam
            webcam = Webcam.getDefault()

            if (webcam == null) {
                println("❌ No webcam found")
                withContext(Dispatchers.Main) {
                    onResult(null)
                }
                return@withContext
            }

            // Set resolution
            webcam?.viewSize = Dimension(640, 480)

            // Open webcam
            webcam?.open()
            println("✅ Webcam opened successfully")

            // Create UI on EDT
            withContext(Dispatchers.Main) {
                createScannerWindow(onResult)
            }

        } catch (e: Exception) {
            println("❌ Camera error: ${e.message}")
            e.printStackTrace()
            closeCamera()
            withContext(Dispatchers.Main) {
                onResult(null)
            }
        }
    }

    private fun createScannerWindow(onResult: (String?) -> Unit) {
        SwingUtilities.invokeLater {
            scannerFrame = JFrame("Scan QR Code").apply {
                defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
                setSize(680, 580)
                setLocationRelativeTo(null)

                val imageLabel = JLabel()
                val buttonPanel = JPanel(FlowLayout()).apply {
                    val cancelButton = JButton("Cancel").apply {
                        addActionListener {
                            isScanning = false
                            closeCamera()
                            onResult(null)
                            dispose()
                        }
                    }
                    add(cancelButton)
                }

                layout = BorderLayout()
                add(imageLabel, BorderLayout.CENTER)
                add(buttonPanel, BorderLayout.SOUTH)

                isVisible = true

                // Start scanning in background
                isScanning = true
                Thread {
                    startContinuousScanning(imageLabel) { qrData ->
                        isScanning = false
                        closeCamera()
                        SwingUtilities.invokeLater {
                            dispose()
                        }
                        onResult(qrData)
                    }
                }.start()
            }
        }
    }

    private fun startContinuousScanning(imageLabel: JLabel, onQRFound: (String) -> Unit) {
        val reader = MultiFormatReader().apply {
            setHints(mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true
            ))
        }

        while (isScanning && webcam?.isOpen == true) {
            try {
                val image: BufferedImage? = webcam?.image

                if (image != null) {
                    // Update preview
                    SwingUtilities.invokeLater {
                        imageLabel.icon = ImageIcon(image)
                    }

                    // Try to decode QR code
                    try {
                        val source = BufferedImageLuminanceSource(image)
                        val bitmap = BinaryBitmap(HybridBinarizer(source))
                        val result = reader.decode(bitmap)

                        if (result != null && result.text.isNotBlank()) {
                            println("✅ QR Code detected: ${result.text}")

                            // Validate if it's a payment QR
                            if (QRCodeScanner.validateQRPaymentData(result.text)) {
                                onQRFound(result.text)
                                return
                            } else {
                                println("⚠️ QR code is not a valid payment QR")
                            }
                        }
                    } catch (e: NotFoundException) {
                        // No QR code found in this frame, continue
                    } catch (e: Exception) {
                        // Other decode errors, continue
                    }
                }

                Thread.sleep(100) // Scan 10 times per second
            } catch (e: Exception) {
                println("❌ Scanning error: ${e.message}")
                break
            }
        }

        if (isScanning) {
            // If we exit loop but still scanning, no QR found
            onQRFound("")
        }
    }

    private fun closeCamera() {
        try {
            isScanning = false
            if (webcam?.isOpen == true) {
                webcam?.close()
                println("✅ Webcam closed")
            }
            webcam = null
            scannerFrame = null
        } catch (e: Exception) {
            println("❌ Error closing camera: ${e.message}")
        }
    }

    /**
     * Check if a webcam is available
     */
    fun isCameraAvailable(): Boolean {
        return try {
            Webcam.getDefault() != null
        } catch (e: Exception) {
            false
        }
    }
}
