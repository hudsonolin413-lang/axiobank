package org.dals.project.utils

import org.dals.project.model.Currency

object CurrencyUtils {
    // Mock exchange rates - in a real app, these would come from an API
    private val exchangeRates = mapOf(
        "USD" to 1.0,
        "EUR" to 0.85,
        "GBP" to 0.73,
        "KES" to 150.0,
        "NGN" to 1650.0,
        "ZAR" to 18.5,
        "GHS" to 15.8,
        "UGX" to 3700.0,
        "TZS" to 2500.0,
        "RWF" to 1300.0,
        "ETB" to 120.0,
        "JPY" to 150.0,
        "CNY" to 7.3,
        "INR" to 83.0
    )

    private val currencySymbols = mapOf(
        "USD" to "$",
        "EUR" to "€",
        "GBP" to "£",
        "KES" to "KSh",
        "NGN" to "₦",
        "ZAR" to "R",
        "GHS" to "₵",
        "UGX" to "USh",
        "TZS" to "TSh",
        "RWF" to "RF",
        "ETB" to "Br",
        "JPY" to "¥",
        "CNY" to "¥",
        "INR" to "₹"
    )

    /**
     * Convert amount from USD to target currency
     */
    fun convertFromUSD(amountInUSD: Double, targetCurrency: String): Double {
        val rate = exchangeRates[targetCurrency] ?: 1.0
        return amountInUSD * rate
    }

    /**
     * Convert amount from source currency to target currency
     */
    fun convertCurrency(amount: Double, fromCurrency: String, toCurrency: String): Double {
        if (fromCurrency == toCurrency) return amount

        val fromRate = exchangeRates[fromCurrency] ?: 1.0
        val toRate = exchangeRates[toCurrency] ?: 1.0

        // Convert to USD first, then to target currency
        val amountInUSD = amount / fromRate
        return amountInUSD * toRate
    }

    /**
     * Format amount with currency symbol
     */
    fun formatAmount(amount: Double, currency: String): String {
        val symbol = currencySymbols[currency] ?: "$"

        return when (currency) {
            "JPY", "KRW" -> "$symbol${amount.toInt()}" // No decimal places for some currencies
            "KES", "NGN", "UGX", "TZS", "RWF", "ETB" -> {
                // Format with commas for large amounts
                "$symbol${formatWithCommas(amount)}"
            }

            else -> {
                if (amount >= 1000) {
                    "$symbol${formatWithCommas(amount)}"
                } else {
                    val rounded = (amount * 100).toInt() / 100.0
                    "$symbol$rounded"
                }
            }
        }
    }

    /**
     * Get currency symbol
     */
    fun getCurrencySymbol(currency: String): String {
        return currencySymbols[currency] ?: "$"
    }

    /**
     * Format number with commas for thousands separator
     */
    private fun formatWithCommas(amount: Double): String {
        val formatted = if (amount % 1.0 == 0.0) {
            amount.toInt().toString()
        } else {
            val str = amount.toString()
            if (str.contains('.')) {
                val parts = str.split('.')
                "${parts[0]}.${parts[1].take(2).padEnd(2, '0')}"
            } else {
                str
            }
        }

        val parts = formatted.split(".")
        val integerPart = parts[0]
        val decimalPart = if (parts.size > 1) ".${parts[1]}" else ""

        val formattedInteger = integerPart.reversed().chunked(3).joinToString(",").reversed()
        return formattedInteger + decimalPart
    }

    /**
     * Get all supported currencies
     */
    fun getSupportedCurrencies(): List<Currency> {
        return listOf(
            Currency("USD", "US Dollar", "$", "🇺🇸"),
            Currency("EUR", "Euro", "€", "🇪🇺"),
            Currency("GBP", "British Pound", "£", "🇬🇧"),
            Currency("KES", "Kenyan Shilling", "KSh", "🇰🇪"),
            Currency("NGN", "Nigerian Naira", "₦", "🇳🇬"),
            Currency("ZAR", "South African Rand", "R", "🇿🇦"),
            Currency("GHS", "Ghanaian Cedi", "₵", "🇬🇭"),
            Currency("UGX", "Ugandan Shilling", "USh", "🇺🇬"),
            Currency("TZS", "Tanzanian Shilling", "TSh", "🇹🇿"),
            Currency("RWF", "Rwandan Franc", "RF", "🇷🇼"),
            Currency("ETB", "Ethiopian Birr", "Br", "🇪🇹"),
            Currency("JPY", "Japanese Yen", "¥", "🇯🇵"),
            Currency("CNY", "Chinese Yuan", "¥", "🇨🇳"),
            Currency("INR", "Indian Rupee", "₹", "🇮🇳")
        )
    }

    /**
     * Get exchange rate for display purposes
     */
    fun getExchangeRate(fromCurrency: String, toCurrency: String): Double {
        if (fromCurrency == toCurrency) return 1.0

        val fromRate = exchangeRates[fromCurrency] ?: 1.0
        val toRate = exchangeRates[toCurrency] ?: 1.0

        return toRate / fromRate
    }
}