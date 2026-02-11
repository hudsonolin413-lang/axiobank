package org.dals.project.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.dals.project.utils.CameraManager
import org.dals.project.utils.FilePickerManager
import org.dals.project.utils.PlatformCameraManager
import org.dals.project.utils.PlatformFilePickerManager

@Composable
actual fun rememberFilePickerManager(): FilePickerManager {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    return remember(context) {
        PlatformFilePickerManager(context).apply {
            activity?.let { initialize(it) }
        }
    }
}

@Composable
actual fun rememberCameraManager(): CameraManager {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    return remember(context) {
        PlatformCameraManager(context).apply {
            activity?.let { initialize(it) }
        }
    }
}
