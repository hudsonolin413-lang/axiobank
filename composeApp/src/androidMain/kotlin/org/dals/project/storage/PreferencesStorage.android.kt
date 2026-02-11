package org.dals.project.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidPreferencesStorage(context: Context) : PreferencesStorage {
    private val preferences: SharedPreferences = context.getSharedPreferences(
        "dals_preferences",
        Context.MODE_PRIVATE
    )

    override suspend fun saveString(key: String, value: String) = withContext(Dispatchers.IO) {
        preferences.edit {
            putString(key, value)
        }
    }

    override suspend fun getString(key: String): String? = withContext(Dispatchers.IO) {
        preferences.getString(key, null)
    }

    override suspend fun saveBoolean(key: String, value: Boolean) = withContext(Dispatchers.IO) {
        preferences.edit {
            putBoolean(key, value)
        }
    }

    override suspend fun getBoolean(key: String): Boolean = withContext(Dispatchers.IO) {
        preferences.getBoolean(key, false)
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        preferences.edit {
            clear()
        }
    }

    override suspend fun remove(key: String) = withContext(Dispatchers.IO) {
        preferences.edit {
            remove(key)
        }
    }
}

actual fun createPreferencesStorage(): PreferencesStorage {
    // This will be initialized later with proper context
    throw IllegalStateException("PreferencesStorage not initialized. Use PreferencesStorageProvider.initialize() first.")
}

object PreferencesStorageProvider {
    private var storage: PreferencesStorage? = null

    fun initialize(context: Context) {
        storage = AndroidPreferencesStorage(context)
    }

    fun get(): PreferencesStorage {
        return storage ?: throw IllegalStateException("PreferencesStorage not initialized")
    }
}