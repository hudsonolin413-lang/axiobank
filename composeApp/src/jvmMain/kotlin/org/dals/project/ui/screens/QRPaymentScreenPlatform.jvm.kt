package org.dals.project.ui.screens

import org.dals.project.utils.CameraQRScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun openCameraScanner(onResult: (String?) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            if (CameraQRScanner.isCameraAvailable()) {
                CameraQRScanner.scanQRCodeWithCamera(onResult)
            } else {
                println("❌ No camera available")
                withContext(Dispatchers.Main) {
                    onResult(null)
                }
            }
        } catch (e: Exception) {
            println("❌ Camera scanner error: ${e.message}")
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onResult(null)
            }
        }
    }
}
