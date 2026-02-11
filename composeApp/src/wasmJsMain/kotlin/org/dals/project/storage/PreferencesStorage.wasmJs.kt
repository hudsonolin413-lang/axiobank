package org.dals.project.storage

import kotlinx.browser.localStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WasmJsPreferencesStorage : PreferencesStorage {
    private val keyPrefix = "dals_"

    override suspend fun saveString(key: String, value: String): Unit = withContext(Dispatchers.Main) {
        localStorage.setItem(keyPrefix + key, value)
    }

    override suspend fun getString(key: String): String? = withContext(Dispatchers.Main) {
        localStorage.getItem(keyPrefix + key)
    }

    override suspend fun saveBoolean(key: String, value: Boolean): Unit = withContext(Dispatchers.Main) {
        localStorage.setItem(keyPrefix + key, value.toString())
    }

    override suspend fun getBoolean(key: String): Boolean = withContext(Dispatchers.Main) {
        localStorage.getItem(keyPrefix + key)?.toBoolean() ?: false
    }

    override suspend fun clear(): Unit = withContext(Dispatchers.Main) {
        val keysToRemove = mutableListOf<String>()
        for (i in 0 until localStorage.length) {
            val key = localStorage.key(i)
            if (key != null && key.startsWith(keyPrefix)) {
                keysToRemove.add(key)
            }
        }
        keysToRemove.forEach { localStorage.removeItem(it) }
    }

    override suspend fun remove(key: String): Unit = withContext(Dispatchers.Main) {
        localStorage.removeItem(keyPrefix + key)
    }
}

actual fun createPreferencesStorage(): PreferencesStorage {
    return WasmJsPreferencesStorage()
}