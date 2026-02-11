package org.dals.project.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.FileInputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Mastercard 3DS Authentication Service
 * Handles Mastercard Identity Check for secure card authentication
 */
class MastercardAuthService {

    // TODO: Set these from your Mastercard Developer Portal
    private val consumerKey = System.getenv("MASTERCARD_CONSUMER_KEY") ?: "YOUR_CONSUMER_KEY_HERE"
    private val keystorePath = System.getenv("MASTERCARD_KEYSTORE_PATH") ?: "path/to/your-keystore.p12"
    private val keystorePassword = System.getenv("MASTERCARD_KEYSTORE_PASSWORD") ?: "YOUR_KEYSTORE_PASSWORD"
    private val baseUrl = "https://sandbox.api.mastercard.com" // Use production later

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    /**
     * Step 1: Check if card is enrolled in 3DS
     */
    suspend fun checkCardEnrollment(cardNumber: String): EnrollmentStatus {
        println("🔍 Checking Mastercard enrollment for card ending in ${cardNumber.takeLast(4)}")

        try {
            val response = httpClient.get("$baseUrl/ics/pa/api/v1/card-range") {
                headers {
                    append("Authorization", generateOAuthHeader("GET", "/ics/pa/api/v1/card-range"))
                    append("Content-Type", "application/json")
                }
                parameter("accountNumber", cardNumber.take(8)) // First 8 digits for BIN lookup
            }

            val result = response.body<CardRangeResponse>()
            return EnrollmentStatus(
                enrolled = result.enrolled,
                acsUrl = result.acsUrl,
                protocolVersion = result.protocolVersion
            )
        } catch (e: Exception) {
            println("❌ Error checking enrollment: ${e.message}")
            return EnrollmentStatus(enrolled = false, acsUrl = null, protocolVersion = null)
        }
    }

    /**
     * Step 2: Initiate 3DS Authentication
     */
    suspend fun initiate3DSAuthentication(request: ThreeDSAuthRequest): ThreeDSAuthResponse {
        println("🔐 Initiating Mastercard 3DS authentication")

        val transactionId = UUID.randomUUID().toString()

        val authRequest = MastercardAuthenticationRequest(
            messageVersion = "2.2.0",
            threeDSServerTransID = transactionId,
            threeDSRequestorID = request.merchantId,
            threeDSRequestorName = request.merchantName,
            messageCategory = "01", // PA - Payment Authentication
            acquirerBIN = "408999", // Your acquiring bank BIN
            acquirerMerchantID = request.merchantId,
            transType = "01", // Goods/Service Purchase
            purchaseAmount = (request.amount * 100).toInt().toString(), // In cents
            purchaseCurrency = getCurrencyCode(request.currency),
            purchaseExponent = "2",
            purchaseDate = getCurrentTimestamp(),
            acctNumber = request.cardNumber,
            cardExpiryDate = request.cardExpiry,
            notificationURL = request.callbackUrl,
            deviceChannel = "02", // Browser
            browserInfo = request.browserInfo
        )

        return try {
            val response = httpClient.post("$baseUrl/ics/pa/api/v1/authentications") {
                headers {
                    append("Authorization", generateOAuthHeader("POST", "/ics/pa/api/v1/authentications"))
                    append("Content-Type", "application/json")
                }
                setBody(authRequest)
            }

            val result = response.body<MastercardAuthenticationResponse>()

            println("✅ 3DS Authentication initiated. Status: ${result.transStatus}")

            ThreeDSAuthResponse(
                transactionId = result.threeDSServerTransID,
                status = result.transStatus,
                acsUrl = result.acsURL,
                challengeRequest = result.acsTransID,
                authenticationValue = result.authenticationValue,
                eci = result.eci,
                protocolVersion = result.messageVersion
            )
        } catch (e: Exception) {
            println("❌ Error initiating 3DS: ${e.message}")
            throw Exception("Failed to initiate 3DS authentication: ${e.message}")
        }
    }

    /**
     * Step 3: Handle Challenge Response (after user verifies with bank)
     */
    suspend fun processChallengeResponse(
        transactionId: String,
        challengeResponse: String
    ): ChallengeResult {
        println("🔄 Processing challenge response for transaction: $transactionId")

        try {
            val response = httpClient.post("$baseUrl/ics/pa/api/v1/authentications/$transactionId/results") {
                headers {
                    append("Authorization", generateOAuthHeader("POST", "/ics/pa/api/v1/authentications/$transactionId/results"))
                    append("Content-Type", "application/json")
                }
                setBody(mapOf("challengeResponse" to challengeResponse))
            }

            val result = response.body<MastercardAuthenticationResponse>()

            return ChallengeResult(
                success = result.transStatus == "Y",
                authenticationValue = result.authenticationValue,
                eci = result.eci,
                transactionId = result.dsTransID
            )
        } catch (e: Exception) {
            println("❌ Error processing challenge: ${e.message}")
            throw Exception("Failed to process challenge response: ${e.message}")
        }
    }

    /**
     * Step 4: Verify Authentication Result
     */
    suspend fun verifyAuthentication(transactionId: String): AuthenticationResult {
        println("✔️ Verifying authentication for transaction: $transactionId")

        try {
            val response = httpClient.get("$baseUrl/ics/pa/api/v1/authentications/$transactionId/results") {
                headers {
                    append("Authorization", generateOAuthHeader("GET", "/ics/pa/api/v1/authentications/$transactionId/results"))
                }
            }

            val result = response.body<MastercardAuthenticationResponse>()

            return when (result.transStatus) {
                "Y" -> AuthenticationResult.Success(
                    cavv = result.authenticationValue ?: "",
                    eci = result.eci ?: "",
                    xid = result.dsTransID ?: ""
                )
                "N" -> AuthenticationResult.Failed("Authentication failed")
                "U" -> AuthenticationResult.Unavailable
                "A" -> AuthenticationResult.AttemptedAuthentication
                "C" -> AuthenticationResult.ChallengeRequired(
                    acsUrl = result.acsURL ?: "",
                    creq = result.acsTransID ?: ""
                )
                else -> AuthenticationResult.Error("Unknown status: ${result.transStatus}")
            }
        } catch (e: Exception) {
            println("❌ Error verifying authentication: ${e.message}")
            return AuthenticationResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Generate OAuth 1.0a signature for Mastercard API
     */
    private fun generateOAuthHeader(method: String, uri: String): String {
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val nonce = UUID.randomUUID().toString().replace("-", "")

        // Load private key from keystore
        val keyStore = KeyStore.getInstance("PKCS12")
        FileInputStream(keystorePath).use { fis ->
            keyStore.load(fis, keystorePassword.toCharArray())
        }

        val alias = keyStore.aliases().nextElement()
        val privateKey = keyStore.getKey(alias, keystorePassword.toCharArray()) as PrivateKey

        // Create signature base string
        val signatureBaseString = buildString {
            append(method.uppercase())
            append("&")
            append(urlEncode("$baseUrl$uri"))
            append("&")
            val params = "oauth_consumer_key=$consumerKey" +
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
        return buildString {
            append("OAuth ")
            append("oauth_consumer_key=\"$consumerKey\",")
            append("oauth_nonce=\"$nonce\",")
            append("oauth_timestamp=\"$timestamp\",")
            append("oauth_signature_method=\"RSA-SHA256\",")
            append("oauth_version=\"1.0\",")
            append("oauth_signature=\"${urlEncode(encodedSignature)}\"")
        }
    }

    private fun urlEncode(value: String): String {
        return java.net.URLEncoder.encode(value, "UTF-8")
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")
    }

    private fun getCurrencyCode(currency: String): String {
        return when (currency.uppercase()) {
            "USD" -> "840"
            "EUR" -> "978"
            "GBP" -> "826"
            "KES" -> "404"
            else -> "840" // Default to USD
        }
    }

    private fun getCurrentTimestamp(): String {
        val dateFormat = java.text.SimpleDateFormat("yyyyMMddHHmmss")
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.format(Date())
    }
}

// Request/Response Models
@Serializable
data class ThreeDSAuthRequest(
    val cardNumber: String,
    val cardExpiry: String,
    val amount: Double,
    val currency: String,
    val merchantId: String,
    val merchantName: String,
    val callbackUrl: String,
    val browserInfo: BrowserInfo
)

@Serializable
data class BrowserInfo(
    val browserAcceptHeader: String = "text/html,application/xhtml+xml",
    val browserIP: String,
    val browserJavaEnabled: Boolean = false,
    val browserLanguage: String = "en-US",
    val browserColorDepth: String = "24",
    val browserScreenHeight: String = "1080",
    val browserScreenWidth: String = "1920",
    val browserTZ: String = "0",
    val browserUserAgent: String
)

@Serializable
data class MastercardAuthenticationRequest(
    val messageVersion: String,
    val threeDSServerTransID: String,
    val threeDSRequestorID: String,
    val threeDSRequestorName: String,
    val messageCategory: String,
    val acquirerBIN: String,
    val acquirerMerchantID: String,
    val transType: String,
    val purchaseAmount: String,
    val purchaseCurrency: String,
    val purchaseExponent: String,
    val purchaseDate: String,
    val acctNumber: String,
    val cardExpiryDate: String,
    val notificationURL: String,
    val deviceChannel: String,
    val browserInfo: BrowserInfo
)

@Serializable
data class MastercardAuthenticationResponse(
    val threeDSServerTransID: String,
    val transStatus: String,
    val acsURL: String? = null,
    val acsTransID: String? = null,
    val authenticationValue: String? = null,
    val eci: String? = null,
    val messageVersion: String,
    val dsTransID: String? = null
)

@Serializable
data class CardRangeResponse(
    val enrolled: Boolean,
    val acsUrl: String? = null,
    val protocolVersion: String? = null
)

@Serializable
data class ThreeDSAuthResponse(
    val transactionId: String,
    val status: String,
    val acsUrl: String? = null,
    val challengeRequest: String? = null,
    val authenticationValue: String? = null,
    val eci: String? = null,
    val protocolVersion: String
)

data class EnrollmentStatus(
    val enrolled: Boolean,
    val acsUrl: String?,
    val protocolVersion: String?
)

@Serializable
data class ChallengeResult(
    val success: Boolean,
    val authenticationValue: String?,
    val eci: String?,
    val transactionId: String?
)

sealed class AuthenticationResult {
    data class Success(val cavv: String, val eci: String, val xid: String) : AuthenticationResult()
    data class ChallengeRequired(val acsUrl: String, val creq: String) : AuthenticationResult()
    data class Failed(val reason: String) : AuthenticationResult()
    object AttemptedAuthentication : AuthenticationResult()
    object Unavailable : AuthenticationResult()
    data class Error(val message: String) : AuthenticationResult()
}
