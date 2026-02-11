package org.dals.project.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.coroutines.resume

actual class PlatformFilePickerManager : FilePickerManager {
    override suspend fun pickImage(): ByteArray? = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                val fileDialog = FileDialog(null as Frame?, "Select Image", FileDialog.LOAD)
                fileDialog.setFilenameFilter { _, name ->
                    name.lowercase().endsWith(".jpg") ||
                            name.lowercase().endsWith(".jpeg") ||
                            name.lowercase().endsWith(".png") ||
                            name.lowercase().endsWith(".webp")
                }
                fileDialog.isVisible = true

                val file = fileDialog.file
                val directory = fileDialog.directory

                if (file != null && directory != null) {
                    val selectedFile = File(directory, file)
                    val bytes = selectedFile.readBytes()
                    continuation.resume(bytes)
                } else {
                    continuation.resume(null)
                }
            } catch (e: Exception) {
                continuation.resume(null)
            }
        }
    }

    override suspend fun takePhoto(): ByteArray? {
        // Desktop doesn't typically support camera through this interface
        return null
    }

    override suspend fun pickDocument(): Pair<String, ByteArray>? = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                val fileDialog = FileDialog(null as Frame?, "Select Document", FileDialog.LOAD)
                fileDialog.isVisible = true

                val file = fileDialog.file
                val directory = fileDialog.directory

                if (file != null && directory != null) {
                    val selectedFile = File(directory, file)
                    val bytes = selectedFile.readBytes()
                    continuation.resume(Pair(file, bytes))
                } else {
                    continuation.resume(null)
                }
            } catch (e: Exception) {
                continuation.resume(null)
            }
        }
    }

    override suspend fun requestPermissions(): Boolean {
        // Desktop doesn't need runtime permissions
        return true
    }
}

actual class PlatformCameraManager : CameraManager {
    override suspend fun capturePhoto(): ByteArray? {
        // Desktop camera access would require additional libraries like webcam-capture
        // For now, returning null as it's not commonly used in desktop apps
        return null
    }

    override suspend fun requestCameraPermission(): Boolean {
        // Desktop doesn't need runtime permissions
        return true
    }
}

actual class PlatformLocationManager : LocationManager {
    override suspend fun getCurrentLocation(): Pair<Double, Double>? {
        // Desktop location would typically use IP-based geolocation
        return null
    }

    override suspend fun requestLocationPermission(): Boolean {
        return true
    }
}

actual class PlatformFileManager : FileManager {
    override suspend fun saveFile(fileName: String, data: ByteArray): String = withContext(Dispatchers.IO) {
        val userHome = System.getProperty("user.home")
        val appDir = File(userHome, ".axiobank")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }

        val file = File(appDir, fileName)
        file.writeBytes(data)
        file.absolutePath
    }

    override suspend fun deleteFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getFileSize(filePath: String): Long = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists()) file.length() else 0L
        } catch (e: Exception) {
            0L
        }
    }
}
