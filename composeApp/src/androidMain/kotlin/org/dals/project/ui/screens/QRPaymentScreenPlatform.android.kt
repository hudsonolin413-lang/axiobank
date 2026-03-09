package org.dals.project.ui.screens

// TODO: Implement Android camera scanner using CameraX
actual suspend fun openCameraScanner(onResult: (String?) -> Unit) {
    // Android implementation would use CameraX and ML Kit for QR scanning
    println("⚠️ Camera scanning not yet implemented for Android")
    onResult(null)
}
