package org.dals.project.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class AndroidBiometricManager : BiometricManager {
    override fun isBiometricAvailable(): Boolean {
        // Implementation for Android
        return true 
    }

    override fun authenticate(
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // In a real Android app, this would use BiometricPrompt
        // For simulation purposes:
        onSuccess()
    }
}

@Composable
actual fun rememberBiometricManager(): BiometricManager {
    return remember { AndroidBiometricManager() }
}
