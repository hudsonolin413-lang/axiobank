package org.dals.project.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSUserDefaults

class IosPreferencesStorage : PreferencesStorage {
    private val userDefaults = NSUserDefaults.standardUserDefaults

    override suspend fun saveString(key: String, value: String): Unit = withContext(Dispatchers.Main) {
        userDefaults.setObject(value, key)
        userDefaults.synchronize()
    }

    override suspend fun getString(key: String): String? = withContext(Dispatchers.Main) {
        userDefaults.stringForKey(key)
    }

    override suspend fun saveBoolean(key: String, value: Boolean): Unit = withContext(Dispatchers.Main) {
        userDefaults.setBool(value, key)
        userDefaults.synchronize()
    }

    override suspend fun getBoolean(key: String): Boolean = withContext(Dispatchers.Main) {
        userDefaults.boolForKey(key)
    }

    override suspend fun clear(): Unit = withContext(Dispatchers.Main) {
        val bundleId = platform.Foundation.NSBundle.mainBundle.bundleIdentifier
        if (bundleId != null) {
            userDefaults.removePersistentDomainForName(bundleId)
        }
        userDefaults.synchronize()
    }

    override suspend fun remove(key: String): Unit = withContext(Dispatchers.Main) {
        userDefaults.removeObjectForKey(key)
        userDefaults.synchronize()
    }
}

actual fun createPreferencesStorage(): PreferencesStorage {
    return IosPreferencesStorage()
}