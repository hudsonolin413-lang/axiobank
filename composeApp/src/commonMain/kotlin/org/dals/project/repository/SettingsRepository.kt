package org.dals.project.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
data class AppSettings(
    val userId: String,
    val language: String = "en",
    val currency: String = "USD",
    val isBiometricEnabled: Boolean = false,
    val theme: String = "system"
)

class SettingsRepository {
    private val _appSettings = MutableStateFlow(AppSettings(userId = "default"))
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    // In a real app, these would be stored in SharedPreferences/UserDefaults/DataStore
    private var storedLanguage = "en"
    private var storedCurrency = "USD"

    init {
        // Load initial settings
        _appSettings.value = AppSettings(
            userId = "default",
            language = storedLanguage,
            currency = storedCurrency
        )
    }

    fun updateLanguage(languageCode: String) {
        storedLanguage = languageCode
        _appSettings.value = _appSettings.value.copy(language = languageCode)
        // In a real app, save to persistent storage here
        println("Language saved: $languageCode")
    }

    fun updateCurrency(currencyCode: String) {
        storedCurrency = currencyCode
        _appSettings.value = _appSettings.value.copy(currency = currencyCode)
        // In a real app, save to persistent storage here
        println("Currency saved: $currencyCode")
    }

    fun updateBiometricEnabled(enabled: Boolean) {
        _appSettings.value = _appSettings.value.copy(isBiometricEnabled = enabled)
        println("Biometric enabled: $enabled")
    }

    fun getCurrentLanguage(): String = _appSettings.value.language
    fun getCurrentCurrency(): String = _appSettings.value.currency

    // Get display name for current language
    fun getCurrentLanguageName(): String {
        return when (_appSettings.value.language) {
            "en" -> "English"
            "sw" -> "Kiswahili"
            "fr" -> "Français"
            "es" -> "Español"
            "ar" -> "العربية"
            "pt" -> "Português"
            "zh" -> "中文"
            "hi" -> "हिन्दी"
            else -> "English"
        }
    }

    // Get display name for current currency
    fun getCurrentCurrencyName(): String {
        return when (_appSettings.value.currency) {
            "USD" -> "USD ($)"
            "EUR" -> "EUR (€)"
            "GBP" -> "GBP (£)"
            "KES" -> "KES (KSh)"
            "NGN" -> "NGN (₦)"
            "ZAR" -> "ZAR (R)"
            "GHS" -> "GHS (₵)"
            "UGX" -> "UGX (USh)"
            "TZS" -> "TZS (TSh)"
            "RWF" -> "RWF (RF)"
            "ETB" -> "ETB (Br)"
            "JPY" -> "JPY (¥)"
            "CNY" -> "CNY (¥)"
            "INR" -> "INR (₹)"
            else -> "USD ($)"
        }
    }
}