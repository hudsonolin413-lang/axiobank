package org.dals.project.utils

import androidx.compose.runtime.Composable

interface BiometricManager {
    fun isBiometricAvailable(): Boolean
    fun authenticate(
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )
}

@Composable
expect fun rememberBiometricManager(): BiometricManager
