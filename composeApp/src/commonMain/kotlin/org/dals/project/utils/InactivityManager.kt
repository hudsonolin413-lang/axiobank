package org.dals.project.utils

import androidx.compose.runtime.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages user inactivity timeout and automatic logout
 */
class InactivityManager(
    private val timeoutMillis: Long = 20_000L, // 20 seconds default
    private val onTimeout: () -> Unit
) {
    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private var timeoutJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Start monitoring for inactivity
     */
    fun start() {
        _isActive.value = true
        resetTimer()
    }

    /**
     * Stop monitoring for inactivity
     */
    fun stop() {
        _isActive.value = false
        timeoutJob?.cancel()
        timeoutJob = null
    }

    /**
     * Reset the inactivity timer (call this on user interaction)
     */
    fun resetTimer() {
        if (!_isActive.value) return

        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(timeoutMillis)
            if (_isActive.value) {
//                println("⏰ Inactivity timeout reached - logging out user")
                onTimeout()
                stop()
            }
        }
    }

    /**
     * Clean up resources
     */
    fun dispose() {
        stop()
        scope.cancel()
    }
}

/**
 * Composable that wraps content and tracks user interactions for inactivity timeout
 */
@Composable
fun InactivityTracker(
    inactivityManager: InactivityManager,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val isActive by inactivityManager.isActive.collectAsState()

    // Start/stop based on enabled state
    LaunchedEffect(enabled) {
        if (enabled) {
            inactivityManager.start()
        } else {
            inactivityManager.stop()
        }
    }

    // Clean up when disposed
    DisposableEffect(Unit) {
        onDispose {
            inactivityManager.dispose()
        }
    }

    // Wrap content in a modifier that resets timer on any interaction
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isActive) {
                if (isActive) {
                    detectTapGestures(
                        onTap = { inactivityManager.resetTimer() },
                        onPress = { inactivityManager.resetTimer() }
                    )
                }
            }
    ) {
        content()
    }
}
