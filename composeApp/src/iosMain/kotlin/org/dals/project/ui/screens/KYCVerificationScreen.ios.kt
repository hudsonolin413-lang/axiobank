package org.dals.project.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.dals.project.utils.CameraManager
import org.dals.project.utils.FilePickerManager
import org.dals.project.utils.PlatformCameraManager
import org.dals.project.utils.PlatformFilePickerManager

@Composable
actual fun rememberFilePickerManager(): FilePickerManager {
    return remember { PlatformFilePickerManager() }
}

@Composable
actual fun rememberCameraManager(): CameraManager {
    return remember { PlatformCameraManager() }
}
