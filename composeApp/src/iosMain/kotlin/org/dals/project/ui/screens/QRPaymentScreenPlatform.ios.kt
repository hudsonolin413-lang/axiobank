package org.dals.project.ui.screens

// TODO: Implement iOS camera scanner using AVFoundation
actual suspend fun openCameraScanner(onResult: (String?) -> Unit) {
    // iOS implementation would use AVCaptureSession and Vision framework
    println("⚠️ Camera scanning not yet implemented for iOS")
    onResult(null)
}
