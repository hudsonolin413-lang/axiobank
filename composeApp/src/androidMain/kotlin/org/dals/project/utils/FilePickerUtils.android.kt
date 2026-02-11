package org.dals.project.utils

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume

actual class PlatformFilePickerManager : FilePickerManager {
    private lateinit var context: Context
    private var imagePickerLauncher: ActivityResultLauncher<String>? = null
    private var imageContinuation: ((ByteArray?) -> Unit)? = null

    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var permissionContinuation: ((Boolean) -> Unit)? = null

    actual constructor()

    constructor(context: Context) {
        this.context = context
    }

    fun initialize(activity: ComponentActivity) {
        imagePickerLauncher = activity.registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            val data = uri?.let { readBytesFromUri(it) }
            imageContinuation?.invoke(data)
            imageContinuation = null
        }

        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions.values.all { it }
            permissionContinuation?.invoke(granted)
            permissionContinuation = null
        }
    }

    override suspend fun pickImage(): ByteArray? = suspendCancellableCoroutine { continuation ->
        imageContinuation = { data ->
            continuation.resume(data)
        }
        imagePickerLauncher?.launch("image/*")
    }

    override suspend fun takePhoto(): ByteArray? {
        // This method is deprecated in favor of using CameraManager
        return null
    }

    override suspend fun pickDocument(): Pair<String, ByteArray>? = suspendCancellableCoroutine { continuation ->
        // Similar implementation for documents
        continuation.resume(null)
    }

    override suspend fun requestPermissions(): Boolean = suspendCancellableCoroutine { continuation ->
        permissionContinuation = { granted ->
            continuation.resume(granted)
        }
        permissionLauncher?.launch(
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_MEDIA_IMAGES
            )
        )
    }

    private fun readBytesFromUri(uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            }
        } catch (e: IOException) {
            null
        }
    }
}

actual class PlatformCameraManager : CameraManager {
    private lateinit var context: Context
    private var cameraLauncher: ActivityResultLauncher<Uri>? = null
    private var cameraContinuation: ((ByteArray?) -> Unit)? = null
    private var photoUri: Uri? = null

    private var permissionLauncher: ActivityResultLauncher<String>? = null
    private var permissionContinuation: ((Boolean) -> Unit)? = null

    actual constructor()

    constructor(context: Context) {
        this.context = context
    }

    fun initialize(activity: ComponentActivity) {
        cameraLauncher = activity.registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            val data = if (success) {
                photoUri?.let { readBytesFromUri(it) }
            } else {
                null
            }
            cameraContinuation?.invoke(data)
            cameraContinuation = null
        }

        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            permissionContinuation?.invoke(granted)
            permissionContinuation = null
        }
    }

    override suspend fun capturePhoto(): ByteArray? = suspendCancellableCoroutine { continuation ->
        try {
            val photoFile = File.createTempFile(
                "KYC_",
                ".jpg",
                context.cacheDir
            )

            photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )

            cameraContinuation = { data ->
                continuation.resume(data)
            }

            photoUri?.let { cameraLauncher?.launch(it) }
        } catch (e: Exception) {
            continuation.resume(null)
        }
    }

    override suspend fun requestCameraPermission(): Boolean = suspendCancellableCoroutine { continuation ->
        permissionContinuation = { granted ->
            continuation.resume(granted)
        }
        permissionLauncher?.launch(android.Manifest.permission.CAMERA)
    }

    private fun readBytesFromUri(uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            }
        } catch (e: IOException) {
            null
        }
    }
}

actual class PlatformLocationManager : LocationManager {
    private lateinit var context: Context

    actual constructor()

    constructor(context: Context) {
        this.context = context
    }

    override suspend fun getCurrentLocation(): Pair<Double, Double>? {
        // Implementation for location services
        return null
    }

    override suspend fun requestLocationPermission(): Boolean {
        // Implementation for location permission
        return false
    }
}

actual class PlatformFileManager : FileManager {
    private lateinit var context: Context

    actual constructor()

    constructor(context: Context) {
        this.context = context
    }

    override suspend fun saveFile(fileName: String, data: ByteArray): String {
        val file = File(context.filesDir, fileName)
        file.writeBytes(data)
        return file.absolutePath
    }

    override suspend fun deleteFile(filePath: String): Boolean {
        val file = File(filePath)
        return file.delete()
    }

    override suspend fun getFileSize(filePath: String): Long {
        val file = File(filePath)
        return if (file.exists()) file.length() else 0L
    }
}
