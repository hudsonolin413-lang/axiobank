package org.dals.project.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.dals.project.config.MastercardConfig
import java.io.FileInputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.util.*

/**
 * Mastercard Card Issuance Service
 * Allows AxionBank to issue real Mastercard debit and credit cards to customers
 *
 * This service integrates with:
 * - Mastercard Processing Debit API (for debit cards)
 * - Mastercard Processing Credit API (for credit cards)
 * - Mastercard Processing Authentication API (for 3DS)
 * - Mastercard Account Validation API (for account verification)
 */
class MastercardIssuanceService {

    // Load configuration from mastercard.properties
    private val config = MastercardConfig

    init {
        println("🔧 Initializing Mastercard Issuance Service...")
        config.printConfig()

        // Validate configuration
        if (!config.validate() && !config.isSandbox) {
            throw IllegalStateException("Invalid Mastercard configuration for production environment")
        }
    }

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }

        // Configure timeouts from config
        engine {
            requestTimeout = config.readTimeout
        }
    }

    /**
     * Step 1: Create a new Mastercard Debit Card for a customer
     */
    suspend fun issueDebitCard(request: CardIssuanceRequest): CardIssuanceResponse {
        if (config.loggingEnabled) {
            println("💳 [Mastercard] Issuing Debit Card for customer: ${request.customerId}")
        }

        // Generate card number using BIN range from config
        val cardNumber = generateCardNumber(config.binRange, request.customerId)
        val cvv = generateCVV()
        val expiryDate = calculateExpiryDate(yearsFromNow = 3)

        val issuanceRequest = MastercardDebitCardRequest(
            cardHolderName = request.cardHolderName,
            primaryAccountNumber = cardNumber,
            expirationDate = expiryDate,
            cvv2 = cvv,
            cardType = "DEBIT",
            cardSubType = "STANDARD", // or "GOLD", "PLATINUM"
            accountNumber = request.linkedAccountNumber,
            customerId = request.customerId,
            issuerICA = config.ica,
            billingAddress = request.billingAddress,
            shippingAddress = request.shippingAddress,
            deliveryMethod = request.deliveryMethod, // "PHYSICAL" or "VIRTUAL"
            activationMethod = "PIN", // or "OTP", "BIOMETRIC"
            dailyLimits = CardLimits(
                dailyWithdrawal = config.debitDailyWithdrawal,
                dailyPurchase = config.debitDailyPurchase,
                dailyTransaction = config.debitDailyTransaction
            )
        )

        return try {
            if (config.loggingEnabled) {
                println("📤 [Mastercard] Sending debit card issuance request to ${config.baseUrl}")
            }

            val response = httpClient.post("${config.baseUrl}/mdes/digitization/1/0/tokenize") {
                headers {
                    append("Authorization", generateOAuthHeader("POST", "/mdes/digitization/1/0/tokenize"))
                    append("Content-Type", "application/json")
                }
                setBody(issuanceRequest)
            }

            val result = response.body<MastercardIssuanceResult>()

            if (config.loggingEnabled) {
                println("✅ [Mastercard] Debit card issued successfully: ${maskCardNumber(cardNumber)}")
            }

            CardIssuanceResponse(
                success = true,
                cardId = result.tokenUniqueReference,
                cardNumber = maskCardNumber(cardNumber),
                maskedCardNumber = maskCardNumber(cardNumber),
                lastFourDigits = cardNumber.takeLast(4),
                expiryMonth = expiryDate.take(2).toInt(),
                expiryYear = expiryDate.takeLast(2).toInt() + 2000,
                cardType = "DEBIT",
                cardBrand = "MASTERCARD",
                status = "PENDING_ACTIVATION",
                trackingNumber = if (request.deliveryMethod == "PHYSICAL") generateTrackingNumber() else null,
                estimatedDelivery = if (request.deliveryMethod == "PHYSICAL") "7-10 business days" else "Instant",
                message = "Debit card created successfully. " +
                        if (request.deliveryMethod == "PHYSICAL")
                            "Card will be shipped to ${request.shippingAddress?.city}"
                        else
                            "Virtual card is available for immediate use after activation"
            )
        } catch (e: Exception) {
            println("❌ Error issuing debit card: ${e.message}")
            e.printStackTrace()
            CardIssuanceResponse(
                success = false,
                cardId = null,
                cardNumber = null,
                maskedCardNumber = null,
                lastFourDigits = null,
                expiryMonth = null,
                expiryYear = null,
                cardType = null,
                cardBrand = null,
                status = "FAILED",
                message = "Failed to issue card: ${e.message}"
            )
        }
    }

    /**
     * Step 2: Create a new Mastercard Credit Card for a customer
     */
    suspend fun issueCreditCard(request: CardIssuanceRequest): CardIssuanceResponse {
        if (config.loggingEnabled) {
            println("💳 [Mastercard] Issuing Credit Card for customer: ${request.customerId}")
        }

        // Generate card number using BIN range from config
        val cardNumber = generateCardNumber(config.binRange, request.customerId)
        val cvv = generateCVV()
        val expiryDate = calculateExpiryDate(yearsFromNow = 5) // Credit cards typically 5 years

        // Determine credit limit based on tier
        val creditLimit = when (request.creditCardTier?.uppercase()) {
            "GOLD" -> config.creditGoldLimit
            "PLATINUM" -> config.creditPlatinumLimit
            "WORLD" -> config.creditWorldLimit
            else -> request.creditLimit ?: config.creditDefaultLimit
        }

        val issuanceRequest = MastercardCreditCardRequest(
            cardHolderName = request.cardHolderName,
            primaryAccountNumber = cardNumber,
            expirationDate = expiryDate,
            cvv2 = cvv,
            cardType = "CREDIT",
            cardSubType = request.creditCardTier ?: "STANDARD", // STANDARD, GOLD, PLATINUM, WORLD
            customerId = request.customerId,
            issuerICA = config.ica,
            creditLimit = creditLimit,
            billingAddress = request.billingAddress,
            shippingAddress = request.shippingAddress,
            deliveryMethod = request.deliveryMethod,
            activationMethod = "PIN",
            creditLimits = CreditCardLimits(
                creditLimit = creditLimit,
                availableCredit = creditLimit,
                cashAdvanceLimit = creditLimit * 0.3 // 30% of credit limit
            )
        )

        return try {
            if (config.loggingEnabled) {
                println("📤 [Mastercard] Sending credit card issuance request to ${config.baseUrl}")
            }

            val response = httpClient.post("${config.baseUrl}/mdes/digitization/1/0/tokenize") {
                headers {
                    append("Authorization", generateOAuthHeader("POST", "/mdes/digitization/1/0/tokenize"))
                    append("Content-Type", "application/json")
                }
                setBody(issuanceRequest)
            }

            val result = response.body<MastercardIssuanceResult>()

            if (config.loggingEnabled) {
                println("✅ [Mastercard] Credit card issued successfully: ${maskCardNumber(cardNumber)}")
            }

            CardIssuanceResponse(
                success = true,
                cardId = result.tokenUniqueReference,
                cardNumber = maskCardNumber(cardNumber),
                maskedCardNumber = maskCardNumber(cardNumber),
                lastFourDigits = cardNumber.takeLast(4),
                expiryMonth = expiryDate.take(2).toInt(),
                expiryYear = expiryDate.takeLast(2).toInt() + 2000,
                cardType = "CREDIT",
                cardBrand = "MASTERCARD",
                status = "PENDING_ACTIVATION",
                trackingNumber = if (request.deliveryMethod == "PHYSICAL") generateTrackingNumber() else null,
                estimatedDelivery = if (request.deliveryMethod == "PHYSICAL") "7-10 business days" else "Instant",
                message = "Credit card with $${String.format("%.2f", creditLimit)} ${request.creditCardTier ?: "STANDARD"} credit limit created successfully"
            )
        } catch (e: Exception) {
            println("❌ Error issuing credit card: ${e.message}")
            e.printStackTrace()
            CardIssuanceResponse(
                success = false,
                cardId = null,
                cardNumber = null,
                maskedCardNumber = null,
                lastFourDigits = null,
                expiryMonth = null,
                expiryYear = null,
                cardType = null,
                cardBrand = null,
                status = "FAILED",
                message = "Failed to issue card: ${e.message}"
            )
        }
    }

    /**
     * Step 3: Activate a card and set PIN
     */
    suspend fun activateCard(cardId: String, pin: String): ActivationResponse {
        println("🔓 Activating card: $cardId")

        val activationRequest = mapOf(
            "tokenUniqueReference" to cardId,
            "pin" to hashPin(pin),
            "activationMethod" to "PIN",
            "timestamp" to System.currentTimeMillis().toString()
        )

        return try {
            val response = httpClient.post("${config.baseUrl}/mdes/digitization/1/0/activate") {
                headers {
                    append("Authorization", generateOAuthHeader("POST", "/mdes/digitization/1/0/activate"))
                    append("Content-Type", "application/json")
                }
                setBody(activationRequest)
            }

            println("✅ Card activated successfully")

            ActivationResponse(
                success = true,
                message = "Card activated successfully. You can now use your Mastercard.",
                status = "ACTIVE"
            )
        } catch (e: Exception) {
            println("❌ Error activating card: ${e.message}")
            ActivationResponse(
                success = false,
                message = "Failed to activate card: ${e.message}",
                status = "PENDING_ACTIVATION"
            )
        }
    }

    /**
     * Step 4: Get card details
     */
    suspend fun getCardDetails(cardId: String): CardDetailsResponse {
        println("📋 Fetching card details for: $cardId")

        return try {
            val response = httpClient.get("${config.baseUrl}/mdes/digitization/1/0/tokenInfo/$cardId") {
                headers {
                    append("Authorization", generateOAuthHeader("GET", "/mdes/digitization/1/0/tokenInfo/$cardId"))
                }
            }

            val result = response.body<MastercardCardDetails>()

            CardDetailsResponse(
                success = true,
                cardId = result.tokenUniqueReference,
                status = result.tokenStatus,
                expiryDate = result.tokenExpirationDate,
                lastUsed = result.lastUsedTimestamp,
                transactionCount = result.transactionCount
            )
        } catch (e: Exception) {
            println("❌ Error fetching card details: ${e.message}")
            CardDetailsResponse(
                success = false,
                cardId = cardId,
                status = "UNKNOWN",
                expiryDate = null,
                lastUsed = null,
                transactionCount = 0
            )
        }
    }

    /**
     * Step 5: Suspend or block a card
     */
    suspend fun suspendCard(cardId: String, reason: String): SuspendResponse {
        println("🚫 Suspending card: $cardId - Reason: $reason")

        val suspendRequest = mapOf(
            "tokenUniqueReference" to cardId,
            "reason" to reason,
            "timestamp" to System.currentTimeMillis().toString()
        )

        return try {
            val response = httpClient.post("${config.baseUrl}/mdes/digitization/1/0/suspend") {
                headers {
                    append("Authorization", generateOAuthHeader("POST", "/mdes/digitization/1/0/suspend"))
                    append("Content-Type", "application/json")
                }
                setBody(suspendRequest)
            }

            println("✅ Card suspended successfully")

            SuspendResponse(
                success = true,
                message = "Card suspended successfully",
                status = "SUSPENDED"
            )
        } catch (e: Exception) {
            println("❌ Error suspending card: ${e.message}")
            SuspendResponse(
                success = false,
                message = "Failed to suspend card: ${e.message}",
                status = "ACTIVE"
            )
        }
    }

    // Helper Functions

    private fun generateCardNumber(binRange: String, customerId: String): String {
        // Generate a valid Mastercard number using Luhn algorithm
        val bin = binRange.take(6) // First 6 digits (BIN)
        val accountNumber = customerId.hashCode().toString().takeLast(9).padStart(9, '0')
        val partialCard = bin + accountNumber

        // Calculate Luhn check digit
        val checkDigit = calculateLuhnCheckDigit(partialCard)
        return partialCard + checkDigit
    }

    private fun calculateLuhnCheckDigit(number: String): Int {
        var sum = 0
        var alternate = false

        for (i in number.length - 1 downTo 0) {
            var digit = number[i].toString().toInt()
            if (alternate) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
            alternate = !alternate
        }

        return (10 - (sum % 10)) % 10
    }

    private fun generateCVV(): String {
        return (100..999).random().toString()
    }

    private fun calculateExpiryDate(yearsFromNow: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, yearsFromNow)
        val month = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
        val year = String.format("%02d", calendar.get(Calendar.YEAR) % 100)
        return "$month$year" // Format: MMYY
    }

    private fun maskCardNumber(cardNumber: String): String {
        return "**** **** **** " + cardNumber.takeLast(4)
    }

    private fun hashPin(pin: String): String {
        // Hash PIN using SHA-256
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray())
        return Base64.getEncoder().encodeToString(hash)
    }

    private fun generateTrackingNumber(): String {
        // Generate a realistic tracking number
        val prefix = "MC"
        val randomNumber = (100000000..999999999).random()
        return "$prefix$randomNumber"
    }

    private fun generateOAuthHeader(method: String, uri: String): String {
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val nonce = UUID.randomUUID().toString().replace("-", "")

        if (config.loggingEnabled && config.loggingLevel == "DEBUG") {
            println("🔐 [Mastercard] Generating OAuth header for $method $uri")
        }

        // Load private key from keystore using config
        val keyStore = KeyStore.getInstance("PKCS12")
        FileInputStream(config.keystorePath).use { fis ->
            keyStore.load(fis, config.keystorePassword.toCharArray())
        }

        val alias = config.keystoreAlias.takeIf { it != "keyalias" }
            ?: keyStore.aliases().nextElement()
        val privateKey = keyStore.getKey(alias, config.keystorePassword.toCharArray()) as PrivateKey

        // Create signature base string
        val signatureBaseString = buildString {
            append(method.uppercase())
            append("&")
            append(urlEncode("${config.baseUrl}$uri"))
            append("&")
            val params = "oauth_consumer_key=${config.consumerKey}" +
                        "&oauth_nonce=$nonce" +
                        "&oauth_signature_method=RSA-SHA256" +
                        "&oauth_timestamp=$timestamp" +
                        "&oauth_version=1.0"
            append(urlEncode(params))
        }

        // Sign with private key
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(signatureBaseString.toByteArray())
        val signatureBytes = signature.sign()
        val encodedSignature = Base64.getEncoder().encodeToString(signatureBytes)

        // Build OAuth header
        val oauthHeader = buildString {
            append("OAuth ")
            append("oauth_consumer_key=\"${config.consumerKey}\",")
            append("oauth_nonce=\"$nonce\",")
            append("oauth_timestamp=\"$timestamp\",")
            append("oauth_signature_method=\"RSA-SHA256\",")
            append("oauth_version=\"1.0\",")
            append("oauth_signature=\"${urlEncode(encodedSignature)}\"")
        }

        if (config.loggingEnabled && config.loggingLevel == "DEBUG") {
            println("✅ [Mastercard] OAuth header generated successfully")
        }

        return oauthHeader
    }

    private fun urlEncode(value: String): String {
        return java.net.URLEncoder.encode(value, "UTF-8")
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")
    }
}

// Data Models
@Serializable
data class CardIssuanceRequest(
    val customerId: String,
    val cardHolderName: String,
    val linkedAccountNumber: String,
    val cardType: String, // "DEBIT" or "CREDIT"
    val creditCardTier: String? = null, // For credit cards: "STANDARD", "GOLD", "PLATINUM"
    val creditLimit: Double? = null, // For credit cards
    val billingAddress: Address,
    val shippingAddress: Address? = null,
    val deliveryMethod: String = "PHYSICAL" // "PHYSICAL" or "VIRTUAL"
)

@Serializable
data class Address(
    val street: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String
)

@Serializable
data class CardLimits(
    val dailyWithdrawal: Double,
    val dailyPurchase: Double,
    val dailyTransaction: Double
)

@Serializable
data class CreditCardLimits(
    val creditLimit: Double,
    val availableCredit: Double,
    val cashAdvanceLimit: Double
)

@Serializable
data class MastercardDebitCardRequest(
    val cardHolderName: String,
    val primaryAccountNumber: String,
    val expirationDate: String,
    val cvv2: String,
    val cardType: String,
    val cardSubType: String,
    val accountNumber: String,
    val customerId: String,
    val issuerICA: String, // Issuer ICA number
    val billingAddress: Address,
    val shippingAddress: Address?,
    val deliveryMethod: String,
    val activationMethod: String,
    val dailyLimits: CardLimits
)

@Serializable
data class MastercardCreditCardRequest(
    val cardHolderName: String,
    val primaryAccountNumber: String,
    val expirationDate: String,
    val cvv2: String,
    val cardType: String,
    val cardSubType: String,
    val customerId: String,
    val issuerICA: String, // Issuer ICA number
    val creditLimit: Double,
    val billingAddress: Address,
    val shippingAddress: Address?,
    val deliveryMethod: String,
    val activationMethod: String,
    val creditLimits: CreditCardLimits
)

@Serializable
data class MastercardIssuanceResult(
    val tokenUniqueReference: String,
    val tokenStatus: String,
    val tokenExpirationDate: String
)

@Serializable
data class MastercardCardDetails(
    val tokenUniqueReference: String,
    val tokenStatus: String,
    val tokenExpirationDate: String,
    val lastUsedTimestamp: String? = null,
    val transactionCount: Int = 0
)

@Serializable
data class CardIssuanceResponse(
    val success: Boolean,
    val cardId: String?,
    val cardNumber: String?,
    val maskedCardNumber: String?,
    val lastFourDigits: String?,
    val expiryMonth: Int?,
    val expiryYear: Int?,
    val cardType: String?,
    val cardBrand: String?,
    val status: String,
    val trackingNumber: String? = null,
    val estimatedDelivery: String? = null,
    val message: String
)

@Serializable
data class ActivationResponse(
    val success: Boolean,
    val message: String,
    val status: String,
    val activatedAt: String = java.time.Instant.now().toString()
)

@Serializable
data class CardDetailsResponse(
    val success: Boolean,
    val cardId: String,
    val status: String,
    val expiryDate: String?,
    val lastUsed: String?,
    val transactionCount: Int
)

@Serializable
data class SuspendResponse(
    val success: Boolean,
    val message: String,
    val status: String,
    val suspendedAt: String = java.time.Instant.now().toString()
)
