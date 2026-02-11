package org.dals.project.utils

import kotlinx.datetime.Clock

// Platform-specific file picker and camera utilities
interface FilePickerManager {
    suspend fun pickImage(): ByteArray?
    suspend fun takePhoto(): ByteArray?
    suspend fun pickDocument(): Pair<String, ByteArray>?
    suspend fun requestPermissions(): Boolean
}

interface CameraManager {
    suspend fun capturePhoto(): ByteArray?
    suspend fun requestCameraPermission(): Boolean
}

interface LocationManager {
    suspend fun getCurrentLocation(): Pair<Double, Double>?
    suspend fun requestLocationPermission(): Boolean
}

// Common interface for file operations
interface FileManager {
    suspend fun saveFile(fileName: String, data: ByteArray): String
    suspend fun deleteFile(filePath: String): Boolean
    suspend fun getFileSize(filePath: String): Long
}

// Expect/Actual declarations for platform-specific implementations
expect class PlatformFilePickerManager() : FilePickerManager

expect class PlatformCameraManager() : CameraManager

expect class PlatformLocationManager() : LocationManager

expect class PlatformFileManager() : FileManager

// File validation utilities
object FileValidator {
    private val allowedImageExtensions = setOf("jpg", "jpeg", "png", "webp")
    private val allowedDocumentExtensions = setOf("pdf", "doc", "docx", "jpg", "jpeg", "png")

    private const val MAX_IMAGE_SIZE = 5 * 1024 * 1024 // 5MB
    private const val MAX_DOCUMENT_SIZE = 10 * 1024 * 1024 // 10MB

    fun isValidImageFile(fileName: String, size: Long): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in allowedImageExtensions && size <= MAX_IMAGE_SIZE
    }

    fun isValidDocumentFile(fileName: String, size: Long): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in allowedDocumentExtensions && size <= MAX_DOCUMENT_SIZE
    }

    fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "").lowercase()
    }

    fun generateFileName(prefix: String, extension: String): String {
        val timestamp = kotlin.random.Random.nextLong(100000, 999999)
        return "${prefix}_${timestamp}.${extension}"
    }
}

// Permission status
enum class PermissionStatus {
    GRANTED,
    DENIED,
    DENIED_PERMANENTLY,
    NOT_REQUESTED
}