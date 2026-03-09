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
    private var context: Context? = null
    private var imagePickerLauncher: ActivityResultLauncher<String>? = null
    private var imageContinuation: ((ByteArray?) -> Unit)? = null

    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var permissionContinuation: ((Boolean) -> Unit)? = null

    actual constructor()

    fun setContext(context: Context) {
        this.context = context
    }

    // New method: Set launchers directly (for Compose with rememberLauncherForActivityResult)
    fun setLaunchers(
        imagePicker: ActivityResultLauncher<String>,
        permissions: ActivityResultLauncher<Array<String>>
    ) {
        this.imagePickerLauncher = imagePicker
        this.permissionLauncher = permissions
    }

    // Methods to trigger callbacks from external launchers
    fun triggerImageCallback(data: ByteArray?) {
        imageContinuation?.invoke(data)
        imageContinuation = null
    }

    fun triggerPermissionCallback(granted: Boolean) {
        permissionContinuation?.invoke(granted)
        permissionContinuation = null
    }

    @Deprecated("Use setLaunchers() instead to avoid lifecycle issues")
    fun initialize(activity: ComponentActivity) {
        // Kept for backward compatibility but should not be used in Compose
        println("⚠️ Warning: initialize() is deprecated. Use setLaunchers() instead")
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

        // Request appropriate permissions based on Android version
        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.CAMERA
            )
        } else {
            // Android 12 and below
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.CAMERA
            )
        }

        permissionLauncher?.launch(permissions)
    }

    private fun readBytesFromUri(uri: Uri): ByteArray? {
        return try {
            context?.contentResolver?.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            }
        } catch (e: IOException) {
            null
        }
    }
}

actual class PlatformCameraManager : CameraManager {
    private var context: Context? = null
    private var cameraLauncher: ActivityResultLauncher<Uri>? = null
    private var cameraContinuation: ((ByteArray?) -> Unit)? = null
    private var photoUri: Uri? = null

    private var permissionLauncher: ActivityResultLauncher<String>? = null
    private var permissionContinuation: ((Boolean) -> Unit)? = null

    actual constructor()

    fun setContext(context: Context) {
        this.context = context
    }

    // New method: Set launchers directly (for Compose)
    fun setLaunchers(
        camera: ActivityResultLauncher<Uri>,
        permission: ActivityResultLauncher<String>
    ) {
        this.cameraLauncher = camera
        this.permissionLauncher = permission
    }

    // Methods to trigger callbacks
    fun triggerCameraCallback(success: Boolean) {
        val data = if (success) {
            photoUri?.let { readBytesFromUri(it) }
        } else {
            null
        }
        cameraContinuation?.invoke(data)
        cameraContinuation = null
    }

    fun triggerPermissionCallback(granted: Boolean) {
        permissionContinuation?.invoke(granted)
        permissionContinuation = null
    }

    @Deprecated("Use setLaunchers() instead to avoid lifecycle issues")
    fun initialize(activity: ComponentActivity) {
        println("⚠️ Warning: initialize() is deprecated. Use setLaunchers() instead")
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
            val ctx = context ?: run {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            val photoFile = File.createTempFile(
                "KYC_",
                ".jpg",
                ctx.cacheDir
            )

            photoUri = FileProvider.getUriForFile(
                ctx,
                "${ctx.packageName}.fileprovider",
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
            context?.contentResolver?.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            }
        } catch (e: IOException) {
            null
        }
    }
}

actual class PlatformLocationManager : LocationManager {
    private var context: Context? = null

    actual constructor()

    fun setContext(context: Context) {
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
    private var context: Context? = null

    actual constructor()

    fun setContext(context: Context) {
        this.context = context
    }

    override suspend fun saveFile(fileName: String, data: ByteArray): String {
        val ctx = context ?: throw IllegalStateException("Context not set")
        val file = File(ctx.filesDir, fileName)
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

actual class PlatformShareManager : ShareManager {
    private var context: android.content.Context? = null

    actual constructor()

    fun setContext(context: android.content.Context) {
        this.context = context
    }

    override fun shareText(text: String, title: String) {
        val ctx = context ?: return
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, text)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(android.content.Intent.createChooser(intent, title).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun shareImage(bytes: ByteArray, fileName: String, title: String) {
        val ctx = context ?: return
        try {
            val cacheFile = File(ctx.cacheDir, fileName)
            cacheFile.writeBytes(bytes)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                ctx,
                "${ctx.packageName}.fileprovider",
                cacheFile
            )

            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(android.content.Intent.createChooser(intent, title).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            println("Error sharing image: ${e.message}")
        }
    }
}
