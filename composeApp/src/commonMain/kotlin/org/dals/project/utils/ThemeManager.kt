package org.dals.project.utils

import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class ThemeManager {
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        updateDarkMode(mode)
    }

    private fun updateDarkMode(mode: ThemeMode) {
        _isDarkMode.value = when (mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> false // Default to light for system mode in this implementation
        }
    }

    fun toggleDarkMode() {
        val currentMode = _themeMode.value
        val newMode = when (currentMode) {
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.SYSTEM -> if (_isDarkMode.value) ThemeMode.LIGHT else ThemeMode.DARK
        }
        setThemeMode(newMode)
    }
}

// Global theme manager instance
val themeManager = ThemeManager()