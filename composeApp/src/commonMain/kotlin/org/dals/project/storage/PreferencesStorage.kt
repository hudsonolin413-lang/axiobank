package org.dals.project.storage

interface PreferencesStorage {
    suspend fun saveString(key: String, value: String)
    suspend fun getString(key: String): String?
    suspend fun saveBoolean(key: String, value: Boolean)
    suspend fun getBoolean(key: String): Boolean
    suspend fun clear()
    suspend fun remove(key: String)
}

object PreferencesKeys {
    const val SAVED_USERNAME = "saved_username"
    const val SAVED_PASSWORD = "saved_password"
    const val REMEMBER_ME = "remember_me"
    const val AUTO_LOGIN = "auto_login"
    const val USER_TOKEN = "user_token"
    const val USER_ID = "user_id"
    const val LAST_USERNAME = "last_username"
}

expect fun createPreferencesStorage(): PreferencesStorage