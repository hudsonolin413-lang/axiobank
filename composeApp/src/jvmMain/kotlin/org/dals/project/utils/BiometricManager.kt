package org.dals.project.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class JvmBiometricManager : BiometricManager {
    override fun isBiometricAvailable(): Boolean {
        // Biometrics are typically not available on standard JVM (Desktop) without native libs
        return false
    }

    override fun authenticate(
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        onError("Biometric authentication not supported on this platform")
    }
}

@Composable
actual fun rememberBiometricManager(): BiometricManager {
    return remember { JvmBiometricManager() }
}
