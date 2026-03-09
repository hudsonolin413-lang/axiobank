package org.dals.project.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.dals.project.utils.CameraManager
import org.dals.project.utils.FilePickerManager
import org.dals.project.utils.PlatformCameraManager
import org.dals.project.utils.PlatformFilePickerManager

@Composable
actual fun rememberFilePickerManager(): FilePickerManager {
    println("📸 Android: rememberFilePickerManager called")
    val context = LocalContext.current

    // Create the manager first to share state
    val manager = remember(context) {
        PlatformFilePickerManager().apply {
            setContext(context)
        }
    }

    // Use rememberLauncherForActivityResult to properly handle lifecycle
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // Read bytes and trigger the manager's callback
        val data = uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    stream.readBytes()
                }
            } catch (e: Exception) {
                println("❌ Error reading image: ${e.message}")
                null
            }
        }
        manager.triggerImageCallback(data)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Log permission results for debugging
        permissions.forEach { (permission, granted) ->
            println("📋 Permission: $permission = $granted")
        }
        val allGranted = permissions.values.all { it }
        println("📋 All permissions granted: $allGranted")
        manager.triggerPermissionCallback(allGranted)
    }

    // Set the launchers after creation
    manager.setLaunchers(imagePickerLauncher, permissionLauncher)
    println("📸 Android: PlatformFilePickerManager created successfully")

    return manager
}

@Composable
actual fun rememberCameraManager(): CameraManager {
    println("📷 Android: rememberCameraManager called")
    val context = LocalContext.current

    // Create the manager first
    val manager = remember(context) {
        PlatformCameraManager().apply {
            setContext(context)
        }
    }

    // Use rememberLauncherForActivityResult for camera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        println("📷 Camera capture result: $success")
        manager.triggerCameraCallback(success)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        println("📷 Camera permission granted: $granted")
        manager.triggerPermissionCallback(granted)
    }

    // Set the launchers
    manager.setLaunchers(cameraLauncher, permissionLauncher)
    println("📷 Android: PlatformCameraManager created successfully")

    return manager
}

@Composable
actual fun rememberShareManager(): org.dals.project.utils.ShareManager {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(context) {
        org.dals.project.utils.PlatformShareManager().apply {
            setContext(context)
        }
    }
}

@Composable
actual fun rememberFileManager(): org.dals.project.utils.FileManager {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(context) {
        org.dals.project.utils.PlatformFileManager().apply {
            setContext(context)
        }
    }
}
