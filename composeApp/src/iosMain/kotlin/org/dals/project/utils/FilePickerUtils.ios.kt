package org.dals.project.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerSourceType
import platform.darwin.NSObject
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
actual class PlatformFilePickerManager : FilePickerManager {
    private var imageContinuation: ((ByteArray?) -> Unit)? = null

    override suspend fun pickImage(): ByteArray? = suspendCancellableCoroutine { continuation ->
        imageContinuation = { data ->
            continuation.resume(data)
        }

        // Note: On iOS, you need to present UIImagePickerController from your view controller
        // This is a simplified version. You'll need to integrate with your iOS view controller
        val picker = UIImagePickerController()
        picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
        picker.allowsEditing = false

        // You need to set delegate and present the picker from your view controller
        // imageContinuation will be called when image is selected
    }

    override suspend fun takePhoto(): ByteArray? {
        // Deprecated - use CameraManager
        return null
    }

    override suspend fun pickDocument(): Pair<String, ByteArray>? {
        // Implementation for document picker
        return null
    }

    override suspend fun requestPermissions(): Boolean {
        // iOS handles permissions automatically when accessing photo library
        return true
    }

    fun imagePickerDidFinishPicking(image: UIImage?) {
        val data = image?.let { uiImageToByteArray(it) }
        imageContinuation?.invoke(data)
        imageContinuation = null
    }

    private fun uiImageToByteArray(image: UIImage): ByteArray? {
        val imageData = UIImageJPEGRepresentation(image, 0.8) ?: return null
        val bytes = ByteArray(imageData.length.toInt())
        imageData.bytes?.let { ptr ->
            bytes.usePinned { pinned ->
                platform.posix.memcpy(pinned.addressOf(0), ptr, imageData.length)
            }
        }
        return bytes
    }
}

@OptIn(ExperimentalForeignApi::class)
actual class PlatformCameraManager : CameraManager {
    private var cameraContinuation: ((ByteArray?) -> Unit)? = null

    override suspend fun capturePhoto(): ByteArray? = suspendCancellableCoroutine { continuation ->
        cameraContinuation = { data ->
            continuation.resume(data)
        }

        // Note: On iOS, you need to present UIImagePickerController from your view controller
        val picker = UIImagePickerController()
        picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        picker.allowsEditing = false

        // You need to set delegate and present the picker from your view controller
    }

    override suspend fun requestCameraPermission(): Boolean {
        // iOS handles permissions automatically when accessing camera
        return true
    }

    fun imagePickerDidFinishPicking(image: UIImage?) {
        val data = image?.let { uiImageToByteArray(it) }
        cameraContinuation?.invoke(data)
        cameraContinuation = null
    }

    private fun uiImageToByteArray(image: UIImage): ByteArray? {
        val imageData = UIImageJPEGRepresentation(image, 0.8) ?: return null
        val bytes = ByteArray(imageData.length.toInt())
        imageData.bytes?.let { ptr ->
            bytes.usePinned { pinned ->
                platform.posix.memcpy(pinned.addressOf(0), ptr, imageData.length)
            }
        }
        return bytes
    }
}

actual class PlatformLocationManager : LocationManager {
    override suspend fun getCurrentLocation(): Pair<Double, Double>? {
        // Implementation for iOS location services
        return null
    }

    override suspend fun requestLocationPermission(): Boolean {
        return false
    }
}

@OptIn(ExperimentalForeignApi::class)
actual class PlatformFileManager : FileManager {
    override suspend fun saveFile(fileName: String, data: ByteArray): String {
        val documentsDirectory = NSFileManager.defaultManager.URLsForDirectory(
            platform.Foundation.NSDocumentDirectory,
            platform.Foundation.NSUserDomainMask
        ).first() as NSURL

        val fileURL = documentsDirectory.URLByAppendingPathComponent(fileName)
        val nsData = data.toNSData()
        nsData?.writeToURL(fileURL!!, atomically = true)

        return fileURL?.path ?: ""
    }

    override suspend fun deleteFile(filePath: String): Boolean {
        return try {
            NSFileManager.defaultManager.removeItemAtPath(filePath, null)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getFileSize(filePath: String): Long {
        val attributes = NSFileManager.defaultManager.attributesOfItemAtPath(filePath, null)
        return (attributes?.get(platform.Foundation.NSFileSize) as? Number)?.toLong() ?: 0L
    }

    private fun ByteArray.toNSData(): NSData? {
        return if (isEmpty()) {
            NSData()
        } else {
            usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
            }
        }
    }
}
