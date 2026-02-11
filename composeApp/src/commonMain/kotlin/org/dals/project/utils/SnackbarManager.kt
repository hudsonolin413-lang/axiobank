package org.dals.project.utils

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Global Snackbar manager for showing immediate feedback to users
 */
object SnackbarManager {
    private var snackbarHostState: SnackbarHostState? = null
    private var coroutineScope: CoroutineScope? = null

    fun initialize(hostState: SnackbarHostState, scope: CoroutineScope) {
        snackbarHostState = hostState
        coroutineScope = scope
    }

    fun showSuccess(message: String, duration: SnackbarDuration = SnackbarDuration.Short) {
        coroutineScope?.launch {
            snackbarHostState?.showSnackbar(
                message = "✓ $message",
                duration = duration
            )
        }
    }

    fun showError(message: String, duration: SnackbarDuration = SnackbarDuration.Long) {
        coroutineScope?.launch {
            snackbarHostState?.showSnackbar(
                message = "✗ $message",
                duration = duration
            )
        }
    }

    fun showInfo(message: String, duration: SnackbarDuration = SnackbarDuration.Short) {
        coroutineScope?.launch {
            snackbarHostState?.showSnackbar(
                message = message,
                duration = duration
            )
        }
    }
}
