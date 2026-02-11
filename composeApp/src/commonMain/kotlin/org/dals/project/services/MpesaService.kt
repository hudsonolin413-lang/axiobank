package org.dals.project.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.delay
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
data class MpesaAuthResponse(
    val access_token: String,
    val expires_in: String
)

@Serializable
data class StkPushRequest(
    val BusinessShortCode: String,
    val Password: String,
    val Timestamp: String,
    val TransactionType: String,
    val Amount: String,
    val PartyA: String,
    val PartyB: String,
    val PhoneNumber: String,
    val CallBackURL: String,
    val AccountReference: String,
    val TransactionDesc: String
)

@Serializable
data class StkPushResponse(
    val MerchantRequestID: String? = null,
    val CheckoutRequestID: String? = null,
    val ResponseCode: String? = null,
    val ResponseDescription: String? = null,
    val CustomerMessage: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null
)

@Serializable
data class StkQueryRequest(
    val BusinessShortCode: String,
    val Password: String,
    val Timestamp: String,
    val CheckoutRequestID: String
)

@Serializable
data class StkQueryResponse(
    val ResponseCode: String,
    val ResponseDescription: String,
    val MerchantRequestID: String? = null,
    val CheckoutRequestID: String? = null,
    val ResultCode: String? = null,
    val ResultDesc: String? = null
)

class MpesaService {
    private val consumerKey = "AuiKNcCSr1WibCHsJ56NlNmS8urQPLbp5qvwOG9iggUnsr1V"
    private val consumerSecret = "2z1RoAe9ciA48qG09uJLJtGgA8gcpoG0AOVR30ctRxWVuTabKSWLsSpi2cPGwTp0"
    private val stkPushUrl = "https://sandbox.safaricom.co.ke/mpesa/stkpush/v1/processrequest"
    private val stkQueryUrl = "https://sandbox.safaricom.co.ke/mpesa/stkpushquery/v1/query"
    private val authUrl = "https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials"

    // M-Pesa credentials for sandbox
    private val businessShortCode = "174379"
    private val passkey = "bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919"
    private val callbackUrl = "https://your-callback-url.com/mpesa/callback" // Replace with actual callback URL

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    private var cachedAccessToken: String? = null
    private var tokenExpiryTime: Long = 0

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun getAccessToken(): String {
        // Return cached token if still valid
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpiryTime) {
            return cachedAccessToken!!
        }

        try {
            val credentials = "$consumerKey:$consumerSecret"
            val encodedCredentials = Base64.encode(credentials.encodeToByteArray())

            val response = httpClient.get(authUrl) {
                header("Authorization", "Basic $encodedCredentials")
            }

            if (response.status == HttpStatusCode.OK) {
                val authResponse = response.body<MpesaAuthResponse>()
                cachedAccessToken = authResponse.access_token
                // Cache token for slightly less than expiry time (3600 seconds = 1 hour)
                tokenExpiryTime = System.currentTimeMillis() + (3500 * 1000)
                return authResponse.access_token
            } else {
                throw Exception("Failed to get M-Pesa access token: ${response.status}")
            }
        } catch (e: Exception) {
            println("❌ Error getting M-Pesa access token: ${e.message}")
            throw e
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun generatePassword(timestamp: String): String {
        val rawPassword = "$businessShortCode$passkey$timestamp"
        return Base64.encode(rawPassword.encodeToByteArray())
    }


    /**
     * Initiate STK Push to customer's phone
     * @param phoneNumber Customer phone number in format 254XXXXXXXXX
     * @param amount Amount to deposit
     * @param accountNumber Customer's account number for reference
     * @return StkPushResponse containing CheckoutRequestID for querying status
     */
    suspend fun initiateDeposit(
        phoneNumber: String,
        amount: Double,
        accountNumber: String
    ): Result<StkPushResponse> {
        return try {
            println("🔄 Initiating M-Pesa STK Push for phone: $phoneNumber, amount: $amount")

            val accessToken = getAccessToken()
            val timestamp = generateTimestamp()
            val password = generatePassword(timestamp)

            // Format phone number to ensure it's in correct format (254XXXXXXXXX)
            val formattedPhone = if (phoneNumber.startsWith("+")) {
                phoneNumber.substring(1)
            } else if (phoneNumber.startsWith("0")) {
                "254" + phoneNumber.substring(1)
            } else {
                phoneNumber
            }

            val stkRequest = StkPushRequest(
                BusinessShortCode = businessShortCode,
                Password = password,
                Timestamp = timestamp,
                TransactionType = "CustomerPayBillOnline",
                Amount = amount.toInt().toString(),
                PartyA = formattedPhone,
                PartyB = businessShortCode,
                PhoneNumber = formattedPhone,
                CallBackURL = callbackUrl,
                AccountReference = accountNumber,
                TransactionDesc = "Deposit to AxionBank Account $accountNumber"
            )

            val response = httpClient.post(stkPushUrl) {
                header("Authorization", "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(stkRequest)
            }

            if (response.status == HttpStatusCode.OK) {
                val stkResponse = response.body<StkPushResponse>()
                println("✅ STK Push initiated: ${stkResponse.ResponseDescription}")
                Result.success(stkResponse)
            } else {
                val errorBody = response.body<String>()
                println("❌ STK Push failed: ${response.status} - $errorBody")
                Result.failure(Exception("STK Push failed: ${response.status}"))
            }
        } catch (e: Exception) {
            println("❌ Error initiating STK Push: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Query the status of an STK Push transaction
     * @param checkoutRequestId The CheckoutRequestID from initiateDeposit response
     * @return StkQueryResponse with transaction status
     */
    suspend fun queryDepositStatus(checkoutRequestId: String): Result<StkQueryResponse> {
        return try {
            println("🔄 Querying M-Pesa transaction status for: $checkoutRequestId")

            val accessToken = getAccessToken()
            val timestamp = generateTimestamp()
            val password = generatePassword(timestamp)

            val queryRequest = StkQueryRequest(
                BusinessShortCode = businessShortCode,
                Password = password,
                Timestamp = timestamp,
                CheckoutRequestID = checkoutRequestId
            )

            val response = httpClient.post(stkQueryUrl) {
                header("Authorization", "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(queryRequest)
            }

            if (response.status == HttpStatusCode.OK) {
                val queryResponse = response.body<StkQueryResponse>()
                println("✅ Transaction status: ${queryResponse.ResultDesc}")
                Result.success(queryResponse)
            } else {
                val errorBody = response.body<String>()
                println("❌ Query failed: ${response.status} - $errorBody")
                Result.failure(Exception("Query failed: ${response.status}"))
            }
        } catch (e: Exception) {
            println("❌ Error querying transaction status: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Poll transaction status until completed or timeout
     * @param checkoutRequestId The CheckoutRequestID to poll
     * @param maxAttempts Maximum number of polling attempts (default 30)
     * @param delayMs Delay between attempts in milliseconds (default 15000 = 15 seconds to avoid rate limits)
     * @return Result with final status
     */
    suspend fun waitForDepositConfirmation(
        checkoutRequestId: String,
        maxAttempts: Int = 30,
        delayMs: Long = 15000
    ): Result<StkQueryResponse> {
        repeat(maxAttempts) { attempt ->
            println("🔄 Poll attempt ${attempt + 1}/$maxAttempts")

            // Wait before querying (except first attempt)
            if (attempt > 0) {
                delay(delayMs)
            }

            val result = queryDepositStatus(checkoutRequestId)
            if (result.isSuccess) {
                val response = result.getOrNull()
                // ResultCode "0" means success
                if (response?.ResultCode == "0") {
                    println("✅ Payment confirmed successfully!")
                    return Result.success(response)
                } else if (response?.ResultCode != null && response.ResultCode != "0") {
                    // Check if it's still processing
                    val description = response.ResultDesc ?: ""
                    if (description.contains("still under processing", ignoreCase = true) ||
                        description.contains("processing", ignoreCase = true)) {
                        println("⏳ Payment still processing, waiting ${delayMs/1000} seconds...")
                        // Continue polling
                    } else {
                        // Transaction failed or was cancelled
                        println("❌ Transaction completed with status: $description")
                        return Result.failure(Exception(description))
                    }
                }
            } else {
                // If we hit rate limit, wait longer
                val error = result.exceptionOrNull()?.message ?: ""
                if (error.contains("429", ignoreCase = true) ||
                    error.contains("rate", ignoreCase = true)) {
                    println("⏸️ Rate limit hit, waiting 20 seconds...")
                    delay(20000)
                }
            }
        }

        return Result.failure(Exception("Payment verification timeout. Please check your M-Pesa messages to confirm if payment was successful."))
    }

    fun cleanup() {
        httpClient.close()
    }
}
