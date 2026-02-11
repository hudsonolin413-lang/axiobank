package org.dals.project.config

import java.io.FileInputStream
import java.util.Properties

/**
 * Mastercard API Configuration
 * Loads configuration from mastercard.properties file
 */
object MastercardConfig {
    private val properties = Properties()

    init {
        try {
            // Try to load from resources
            val inputStream = this::class.java.classLoader.getResourceAsStream("mastercard.properties")
            if (inputStream != null) {
                properties.load(inputStream)
                println("✅ Mastercard configuration loaded from resources")
            } else {
                // Try to load from file system
                val fileInputStream = FileInputStream("server/src/main/resources/mastercard.properties")
                properties.load(fileInputStream)
                println("✅ Mastercard configuration loaded from file system")
            }
        } catch (e: Exception) {
            println("⚠️ Failed to load Mastercard configuration: ${e.message}")
            println("⚠️ Using default configuration values")
        }
    }

    // Environment
    val environment: String
        get() = properties.getProperty("mastercard.environment", "sandbox")

    val isSandbox: Boolean
        get() = environment == "sandbox"

    // Base URLs
    val baseUrl: String
        get() = if (isSandbox) {
            properties.getProperty("mastercard.sandbox.url", "https://sandbox.api.mastercard.com")
        } else {
            properties.getProperty("mastercard.production.url", "https://api.mastercard.com")
        }

    // Consumer Key
    val consumerKey: String
        get() = properties.getProperty("mastercard.consumer.key", "YOUR_CONSUMER_KEY_HERE")

    // Keystore Configuration
    val keystorePath: String
        get() = properties.getProperty("mastercard.keystore.path",
            "C:/Users/ADMIN/AxionBank/Axio Bank/api/axiobank-sandbox.p12")

    val keystorePassword: String
        get() = properties.getProperty("mastercard.keystore.password", "keystorepassword")

    val keystoreAlias: String
        get() = properties.getProperty("mastercard.keystore.alias", "keyalias")

    // Certificate Paths
    val debitCertPath: String
        get() = properties.getProperty("mastercard.cert.debit.path",
            "C:/Users/ADMIN/AxionBank/Axio Bank/api/mastercard-processing-debitClientEnc1770499674069.pem")

    val creditCertPath: String
        get() = properties.getProperty("mastercard.cert.credit.path",
            "C:/Users/ADMIN/AxionBank/Axio Bank/api/mastercard-processing-creditClientEnc1770499633990.pem")

    val authenticationCertPath: String
        get() = properties.getProperty("mastercard.cert.authentication.path",
            "C:/Users/ADMIN/AxionBank/Axio Bank/api/mastercard-processing-authenticationClientEnc1770499591539.pem")

    val accountValidationCertPath: String
        get() = properties.getProperty("mastercard.cert.account.validation.path",
            "C:/Users/ADMIN/AxionBank/Axio Bank/api/mastercard-account-validationClientEnc1770499573730.pem")

    val encryptionCertPath: String
        get() = properties.getProperty("mastercard.cert.encryption.path",
            "C:/Users/ADMIN/AxionBank/Axio Bank/api/axiobank-mastercard-encryption-key.p12")

    // Card Issuance Configuration
    val binRange: String
        get() = properties.getProperty("mastercard.bin.range", "555555")

    val ica: String
        get() = properties.getProperty("mastercard.ica", "12345")

    // Card Limits
    val debitDailyWithdrawal: Double
        get() = properties.getProperty("mastercard.debit.daily.withdrawal", "5000.00").toDouble()

    val debitDailyPurchase: Double
        get() = properties.getProperty("mastercard.debit.daily.purchase", "10000.00").toDouble()

    val debitDailyTransaction: Double
        get() = properties.getProperty("mastercard.debit.daily.transaction", "15000.00").toDouble()

    val creditDefaultLimit: Double
        get() = properties.getProperty("mastercard.credit.default.limit", "5000.00").toDouble()

    val creditGoldLimit: Double
        get() = properties.getProperty("mastercard.credit.gold.limit", "10000.00").toDouble()

    val creditPlatinumLimit: Double
        get() = properties.getProperty("mastercard.credit.platinum.limit", "25000.00").toDouble()

    val creditWorldLimit: Double
        get() = properties.getProperty("mastercard.credit.world.limit", "50000.00").toDouble()

    // API Timeouts
    val connectTimeout: Long
        get() = properties.getProperty("mastercard.timeout.connect", "30").toLong() * 1000

    val readTimeout: Long
        get() = properties.getProperty("mastercard.timeout.read", "60").toLong() * 1000

    // Logging
    val loggingEnabled: Boolean
        get() = properties.getProperty("mastercard.logging.enabled", "true").toBoolean()

    val loggingLevel: String
        get() = properties.getProperty("mastercard.logging.level", "DEBUG")

    /**
     * Validate configuration
     */
    fun validate(): Boolean {
        val errors = mutableListOf<String>()

        if (consumerKey == "YOUR_CONSUMER_KEY_HERE") {
            errors.add("Consumer key not configured")
        }

        if (binRange == "555555" && !isSandbox) {
            errors.add("BIN range not configured for production")
        }

        if (ica == "12345" && !isSandbox) {
            errors.add("ICA not configured for production")
        }

        if (errors.isNotEmpty()) {
            println("❌ Mastercard configuration validation failed:")
            errors.forEach { println("   - $it") }
            return false
        }

        println("✅ Mastercard configuration validated successfully")
        return true
    }

    /**
     * Print current configuration (for debugging)
     */
    fun printConfig() {
        println("=== Mastercard Configuration ===")
        println("Environment: $environment")
        println("Base URL: $baseUrl")
        println("Consumer Key: ${consumerKey.take(10)}...")
        println("Keystore Path: $keystorePath")
        println("BIN Range: $binRange")
        println("ICA: $ica")
        println("Debit Daily Limit: $debitDailyWithdrawal")
        println("Credit Default Limit: $creditDefaultLimit")
        println("===============================")
    }
}
