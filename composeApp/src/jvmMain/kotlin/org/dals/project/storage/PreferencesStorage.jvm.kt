package org.dals.project.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Properties

class JvmPreferencesStorage : PreferencesStorage {
    private val properties = Properties()
    private val prefsFile = File(System.getProperty("user.home"), ".dals_preferences")

    init {
        loadProperties()
    }

    private fun loadProperties() {
        if (prefsFile.exists()) {
            try {
                prefsFile.inputStream().use { input ->
                    properties.load(input)
                }
            } catch (e: Exception) {
                // If loading fails, start with empty properties
                properties.clear()
            }
        }
    }

    private suspend fun saveProperties() = withContext(Dispatchers.IO) {
        try {
            prefsFile.outputStream().use { output ->
                properties.store(output, "DALS App Preferences")
            }
        } catch (e: Exception) {
            // Handle save error silently or log
        }
    }

    override suspend fun saveString(key: String, value: String) {
        properties.setProperty(key, value)
        saveProperties()
    }

    override suspend fun getString(key: String): String? {
        return properties.getProperty(key)
    }

    override suspend fun saveBoolean(key: String, value: Boolean) {
        properties.setProperty(key, value.toString())
        saveProperties()
    }

    override suspend fun getBoolean(key: String): Boolean {
        return properties.getProperty(key)?.toBoolean() ?: false
    }

    override suspend fun clear() {
        properties.clear()
        saveProperties()
    }

    override suspend fun remove(key: String) {
        properties.remove(key)
        saveProperties()
    }
}

actual fun createPreferencesStorage(): PreferencesStorage {
    return JvmPreferencesStorage()
}