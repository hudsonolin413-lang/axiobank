package org.dals.project.services

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import org.dals.project.database.*
import org.dals.project.utils.IdGenerator
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*
import kotlin.collections.mutableMapOf
import kotlin.random.Random
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.Instant
import java.time.format.DateTimeFormatter
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import java.util.Base64
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*

/**
 * Enhanced M-Pesa Service for AxionBank Server with Real Safaricom API Integration
 */
class MpesaService(
    private val database: Database,
    private val masterWalletService: MasterWalletService
) {
    private val notificationService = NotificationService()

    // Real Safaricom M-Pesa API Configuration
    private val mpesaConfig = MpesaConfiguration(
        consumerKey = "AuiKNcCSr1WibCHsJ56NlNmS8urQPLbp5qvwOG9iggUnsr1V",
        consumerSecret = "2z1RoAe9ciA48qG09uJLJtGgA8gcpoG0AOVR30ctRxWVuTabKSWLsSpi2cPGwTp0",
        passkey = "bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919",
        shortcode = "174379",
        environment = "sandbox", // Use "production" for live environment
        callbackUrl = "https://webhook.site/7c6b0e3d-6b8c-4a2d-9b4e-1a2b3c4d5e6f" // Replace with your actual webhook URL
    )

    private val baseUrl = if (mpesaConfig.environment == "production") {
        "https://api.safaricom.co.ke"
    } else {
        "https://sandbox.safaricom.co.ke"
    }

    private val accessTokenCache = mutableMapOf<String, AccessTokenCache>()
    private val tokenMutex = Mutex()

    // HTTP Client for API calls (explicitly using CIO engine)
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    /**
     * Validate and format phone number for M-Pesa
     */
    private fun formatPhoneNumber(phoneNumber: String): String {
        val cleaned = phoneNumber.replace(Regex("[^0-9]"), "")

        val formatted = when {
            cleaned.startsWith("254") -> cleaned
            cleaned.startsWith("0") -> "254" + cleaned.substring(1)
            cleaned.startsWith("7") || cleaned.startsWith("1") -> "254$cleaned"
            else -> throw IllegalArgumentException("Invalid phone number format. Expected Kenyan number (07XXXXXXXX or 2547XXXXXXXX)")
        }

        // Validate phone number length (should be 12 digits: 254 + 9 digits)
        if (formatted.length != 12) {
            throw IllegalArgumentException("Invalid phone number length. Expected 12 digits (254XXXXXXXXX), got ${formatted.length} digits: $formatted")
        }

        // For sandbox environment, validate against test numbers
        if (mpesaConfig.environment == "sandbox") {
            val validTestNumbers = listOf("254708374149", "254799999999", "254711111111")
            if (formatted !in validTestNumbers) {
    //            println("⚠️ WARNING: Phone number $formatted is not a valid Safaricom sandbox test number")
    //            println("   Valid test numbers: ${validTestNumbers.joinToString(", ")}")
    //            println("   The transaction may fail. For testing, use: 254708374149")
            }
        }

        return formatted
    }

    /**
     * Get access token from Safaricom API
     */
    private suspend fun getAccessToken(): String {
        return tokenMutex.withLock {
            val cachedToken = accessTokenCache["current"]
            if (cachedToken != null && !isTokenExpired(cachedToken.expiresAt)) {
                return@withLock cachedToken.token
            }

            try {
                val credentials = Base64.getEncoder().encodeToString(
                    "${mpesaConfig.consumerKey}:${mpesaConfig.consumerSecret}".toByteArray()
                )

//                println(" Requesting M-Pesa access token...")
                val response = httpClient.get("$baseUrl/oauth/v1/generate?grant_type=client_credentials") {
                    header(HttpHeaders.Authorization, "Basic $credentials")
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                }

                if (response.status == HttpStatusCode.OK) {
                    val responseText = response.bodyAsText()
//                    println(" Access token response: $responseText")

                    val json = Json { ignoreUnknownKeys = true }
                    val tokenResponse = json.decodeFromString<TokenResponse>(responseText)
                    val expiresAt = LocalDateTime.now().plusSeconds(tokenResponse.expires_in.toLong() - 60)

                    accessTokenCache["current"] = AccessTokenCache(
                        token = tokenResponse.access_token,
                        expiresAt = expiresAt.toString()
                    )

//                    println(" Access token obtained successfully")
                    tokenResponse.access_token
                } else {
                    val errorText = response.bodyAsText()
//                    println(" Failed to get access token: ${response.status} - $errorText")
                    throw Exception("Failed to get access token: ${response.status} - $errorText")
                }
            } catch (e: Exception) {
//                println(" Error getting access token: ${e.message}")
                throw Exception("Error getting access token: ${e.message}")
            }
        }
    }

    private fun isTokenExpired(expiresAt: String): Boolean {
        return try {
            val expiry = LocalDateTime.parse(expiresAt)
            LocalDateTime.now().isAfter(expiry)
        } catch (e: Exception) {
            true
        }
    }

    /**
     * Simplified STK Push implementation
     */
    suspend fun initiateStkPush(request: MpesaDepositRequest): MpesaDepositResponse {
        return try {
            println(" Initiating STK Push for ${request.phoneNumber}, amount: ${request.amount}")

            // Input validation
            validateDepositRequest(request)

            // Format phone number
            val formattedPhone = formatPhoneNumber(request.phoneNumber)
            println(" Formatted phone number: $formattedPhone")

            // Get access token
            val accessToken = getAccessToken()

            // Generate transaction ID and other identifiers
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            val password = Base64.getEncoder().encodeToString(
                "${mpesaConfig.shortcode}${mpesaConfig.passkey}$timestamp".toByteArray()
            )

            // Prepare STK Push request
            val stkPushRequest = StkPushRequest(
                BusinessShortCode = mpesaConfig.shortcode,
                Password = password,
                Timestamp = timestamp,
                TransactionType = "CustomerPayBillOnline",
                Amount = request.amount.toInt(),
                PartyA = formattedPhone,
                PartyB = mpesaConfig.shortcode,
                PhoneNumber = formattedPhone,
                CallBackURL = mpesaConfig.callbackUrl,
                AccountReference = request.accountNumber,
                TransactionDesc = request.description
            )

            // Make API call to Safaricom
            println(" Sending STK Push request to Safaricom...")
            val json = Json { ignoreUnknownKeys = true }
            val requestBody = json.encodeToString(stkPushRequest)
            println(" Request body: $requestBody")

            val response = httpClient.post("$baseUrl/mpesa/stkpush/v1/processrequest") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(requestBody)
            }

            val responseText = response.bodyAsText()
            println(" STK Push response: ${response.status} - $responseText")

            if (response.status == HttpStatusCode.OK) {
                val stkResponse = json.decodeFromString<StkPushApiResponse>(responseText)

                if (stkResponse.ResponseCode == "0") {
                    // Store pending transaction
                    val transactionId = "TXN_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}"
                    storePendingTransaction(
                        request,
                        transactionId,
                        stkResponse.MerchantRequestID,
                        stkResponse.CheckoutRequestID
                    )

                    println(" STK Push initiated successfully: ${stkResponse.CheckoutRequestID}")
                    MpesaDepositResponse(
                        success = true,
                        message = "STK Push initiated successfully. Please check your phone and enter M-Pesa PIN.",
                        merchantRequestID = stkResponse.MerchantRequestID,
                        checkoutRequestID = stkResponse.CheckoutRequestID,
                        responseCode = stkResponse.ResponseCode,
                        responseDescription = stkResponse.ResponseDescription,
                        customerMessage = stkResponse.CustomerMessage,
                        transactionId = transactionId
                    )
                } else {
                    println(" STK Push failed: ${stkResponse.ResponseDescription}")
                    MpesaDepositResponse(
                        success = false,
                        message = "STK Push failed: ${stkResponse.ResponseDescription}",
                        responseCode = stkResponse.ResponseCode,
                        responseDescription = stkResponse.ResponseDescription
                    )
                }
            } else {
                println(" STK Push API call failed: HTTP ${response.status.value}")
                MpesaDepositResponse(
                    success = false,
                    message = "STK Push API call failed: HTTP ${response.status.value}",
                    responseDescription = responseText
                )
            }
        } catch (e: Exception) {
            println(" STK Push failed with exception: ${e.message}")
            e.printStackTrace()
            MpesaDepositResponse(
                success = false,
                message = "STK Push failed: ${e.message}",
                responseCode = "500",
                responseDescription = e.message
            )
        }
    }

    /**
     * Simple STK Push method for direct API calls
     */
    suspend fun initiateStkPush(
        phoneNumber: String,
        amount: Double,
        description: String
    ): MpesaResponse {
        return try {
            println(" Initiating simple STK Push for $phoneNumber, amount: $amount")

            val accessToken = getAccessToken()

            if (accessToken.isEmpty()) {
                return MpesaResponse(
                    success = false,
                    message = "Failed to get access token from Safaricom",
                    error = "AUTHENTICATION_ERROR"
                )
            }

            val formattedPhone = formatPhoneNumber(phoneNumber)
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            val password = Base64.getEncoder().encodeToString(
                "${mpesaConfig.shortcode}${mpesaConfig.passkey}$timestamp".toByteArray()
            )

            val stkPushRequest = StkPushRequest(
                BusinessShortCode = mpesaConfig.shortcode,
                Password = password,
                Timestamp = timestamp,
                TransactionType = "CustomerPayBillOnline",
                Amount = amount.toInt(),
                PartyA = formattedPhone,
                PartyB = mpesaConfig.shortcode,
                PhoneNumber = formattedPhone,
                CallBackURL = mpesaConfig.callbackUrl,
                AccountReference = "AXIONBANK",
                TransactionDesc = description
            )

            val json = Json { ignoreUnknownKeys = true }
            val requestBody = json.encodeToString(stkPushRequest)

            val response = httpClient.post("$baseUrl/mpesa/stkpush/v1/processrequest") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(requestBody)
            }

            val responseText = response.bodyAsText()
//            println(" Simple STK Push response: ${response.status} - $responseText")

            if (response.status == HttpStatusCode.OK) {
                val stkResponse = json.decodeFromString<StkPushApiResponse>(responseText)

//                println(" Simple STK Push successful: ${stkResponse.CheckoutRequestID}")
                MpesaResponse(
                    success = true,
                    message = stkResponse.CustomerMessage ?: "STK Push initiated successfully",
                    transactionId = UUID.randomUUID().toString(),
                    checkoutRequestID = stkResponse.CheckoutRequestID,
                    merchantRequestID = stkResponse.MerchantRequestID,
                    customerMessage = stkResponse.CustomerMessage
                )
            } else {
//                println(" Simple STK Push failed: $responseText")
                MpesaResponse(
                    success = false,
                    message = "STK Push failed: $responseText",
                    error = "API_ERROR"
                )
            }
        } catch (e: Exception) {
//            println(" Simple STK Push failed with exception: ${e.message}")
            e.printStackTrace()
            MpesaResponse(
                success = false,
                message = "STK Push failed: ${e.message}",
                error = "SYSTEM_ERROR"
            )
        }
    }

    /**
     * Handle M-Pesa callback
     */
    suspend fun handleMpesaCallback(callbackData: MpesaCallbackData): Boolean {
        return try {
            val stkCallback = callbackData.body.stkCallback
            val checkoutRequestID = stkCallback.checkoutRequestID
            val resultCode = stkCallback.resultCode

            when (resultCode) {
                0 -> {
                    // Transaction successful
                    handleSuccessfulCallback(stkCallback)
                    true
                }
                1032 -> {
                    // User cancelled transaction
                    updateTransactionStatus(checkoutRequestID, "CANCELLED", "User cancelled transaction")
                    true
                }
                1037 -> {
                    // User timeout
                    updateTransactionStatus(checkoutRequestID, "TIMEOUT", "User did not enter PIN in time")
                    true
                }
                1001 -> {
                    // Insufficient funds
                    updateTransactionStatus(checkoutRequestID, "FAILED", "Insufficient funds in M-Pesa account")
                    true
                }
                else -> {
                    // Other failure
                    updateTransactionStatus(checkoutRequestID, "FAILED", stkCallback.resultDesc)
                    true
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Initiate M-Pesa deposit into a master wallet
     */
    suspend fun initiateWalletDeposit(
        walletId: String,
        phoneNumber: String,
        amount: Double,
        description: String
    ): MpesaResponse {
        return try {
            // Verify wallet exists by calling the service properly
            val walletUUID = try {
                UUID.fromString(walletId)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid wallet ID format: $walletId")
            }

            val walletResponse = masterWalletService.getWalletById(walletUUID)
            if (!walletResponse.success || walletResponse.data == null) {
                throw IllegalArgumentException("Wallet not found: $walletId")
            }

            // Create deposit request
            val depositRequest = MpesaDepositRequest(
                phoneNumber = phoneNumber,
                accountNumber = "WALLET_$walletId",
                amount = amount,
                description = description
            )

            // Initiate STK Push
            val stkResponse = initiateStkPush(depositRequest)

            if (stkResponse.success) {
                // Schedule automatic status checking
                GlobalScope.launch {
                    delay(2000) // Wait 2 seconds then check status (reduced from 5)
                    checkAndProcessTransaction(stkResponse.checkoutRequestID ?: "", walletId, amount, phoneNumber)
                }

                MpesaResponse(
                    success = true,
                    message = "M-Pesa STK Push sent successfully. Please check your phone and enter PIN.",
                    transactionId = stkResponse.transactionId,
                    checkoutRequestID = stkResponse.checkoutRequestID,
                    merchantRequestID = stkResponse.merchantRequestID,
                    customerMessage = "Please enter your M-Pesa PIN to complete the wallet deposit of KES ${amount.toInt()}"
                )
            } else {
                MpesaResponse(
                    success = false,
                    message = stkResponse.message,
                    error = "WALLET_DEPOSIT_ERROR"
                )
            }

        } catch (e: Exception) {
            MpesaResponse(
                success = false,
                message = "Failed to initiate wallet deposit: ${e.message}",
                error = "WALLET_DEPOSIT_ERROR"
            )
        }
    }

    /**
     * Check transaction status and process if completed
     */
    private suspend fun checkAndProcessTransaction(
        checkoutRequestID: String,
        walletId: String,
        amount: Double,
        phoneNumber: String
    ) {
        try {
            println("🔍 Checking transaction status for: $checkoutRequestID")

            // Reduced wait time - check after 3 seconds instead of 10
            delay(3000)

            // For sandbox testing, we'll simulate completion after a delay
            // In production, you would rely on callbacks or periodic status checks
            val isSimulatedCompletion = true // Change to false in production

            if (isSimulatedCompletion) {
                println("🎯 Simulating successful M-Pesa completion for testing...")
                processWalletDeposit(checkoutRequestID, walletId, amount, phoneNumber)
                return
            }

            // Query transaction status from Safaricom (limited by rate limiting)
            try {
                val statusResponse = queryStkPushStatus(checkoutRequestID)

                if (statusResponse.success && statusResponse.responseCode == "0") {
                    // Transaction completed successfully, process wallet deposit
                    processWalletDeposit(checkoutRequestID, walletId, amount, phoneNumber)
                } else if (statusResponse.responseCode == "1032") {
                    // User cancelled
                    updateTransactionStatus(checkoutRequestID, "CANCELLED", "User cancelled transaction")
                } else if (statusResponse.responseCode == "1037") {
                    // Timeout
                    updateTransactionStatus(checkoutRequestID, "TIMEOUT", "User did not enter PIN in time")
                } else {
                    // Still pending or failed, try one more time after delay
                    delay(5000) // Wait 5 more seconds (reduced from 15)
                    val secondCheck = queryStkPushStatus(checkoutRequestID)

                    if (secondCheck.success && secondCheck.responseCode == "0") {
                        processWalletDeposit(checkoutRequestID, walletId, amount, phoneNumber)
                    } else {
                        println("💔 Transaction not completed after second check: ${secondCheck.message}")
                        updateTransactionStatus(
                            checkoutRequestID,
                            "FAILED",
                            "Transaction was not completed within time limit"
                        )
                    }
                }
            } catch (e: Exception) {
                println("⚠️ Error checking transaction status (rate limited?): ${e.message}")
                // Fallback: For testing, process the deposit anyway after delay
                delay(2000) // Reduced from 5 seconds
                println("🔄 Processing deposit despite status check failure (testing mode)")
                processWalletDeposit(checkoutRequestID, walletId, amount, phoneNumber)
            }
        } catch (e: Exception) {
            println("❌ Error in checkAndProcessTransaction: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Check wallet deposit transaction status
     */
    suspend fun checkWalletDepositStatus(transactionId: String): TransactionStatusResult {
        return try {
            println(" Checking transaction status for: $transactionId")

            // Strategy 1: Try to find by transaction ID (UUID)
            val transactionById = try {
                newSuspendedTransaction(db = database) {
                    MpesaTransactions.select { MpesaTransactions.id eq UUID.fromString(transactionId) }.singleOrNull()
                }
            } catch (e: Exception) {
                println(" Transaction ID is not a valid UUID, trying other methods")
                null
            }

            if (transactionById != null) {
                println(" Found transaction by ID")
                return TransactionStatusResult(
                    success = true,
                    transactionId = transactionId,
                    status = transactionById[MpesaTransactions.status],
                    amount = transactionById[MpesaTransactions.amount].toDouble(),
                    phoneNumber = transactionById[MpesaTransactions.phoneNumber],
                    walletId = transactionById[MpesaTransactions.accountNumber]?.removePrefix("WALLET_") ?: "",
                    createdAt = transactionById[MpesaTransactions.createdAt].toString()
                )
            }

            // Strategy 2: Try to find by checkout request ID
            val transactionByCheckout = newSuspendedTransaction(db = database) {
                MpesaTransactions.select { MpesaTransactions.checkoutRequestId eq transactionId }.singleOrNull()
            }

            if (transactionByCheckout != null) {
                println(" Found transaction by checkout request ID")
                return TransactionStatusResult(
                    success = true,
                    transactionId = transactionByCheckout[MpesaTransactions.id].toString(),
                    status = transactionByCheckout[MpesaTransactions.status],
                    amount = transactionByCheckout[MpesaTransactions.amount].toDouble(),
                    phoneNumber = transactionByCheckout[MpesaTransactions.phoneNumber],
                    walletId = transactionByCheckout[MpesaTransactions.accountNumber]?.removePrefix("WALLET_") ?: "",
                    createdAt = transactionByCheckout[MpesaTransactions.createdAt].toString()
                )
            }

            // Strategy 3: Try to find by M-Pesa receipt number
            val transactionByReceipt = newSuspendedTransaction(db = database) {
                MpesaTransactions.select { MpesaTransactions.mpesaReceiptNumber eq transactionId }
                    .orderBy(MpesaTransactions.createdAt, SortOrder.DESC)
                    .firstOrNull()
            }

            if (transactionByReceipt != null) {
                println(" Found transaction by M-Pesa receipt number")
                return TransactionStatusResult(
                    success = true,
                    transactionId = transactionByReceipt[MpesaTransactions.id].toString(),
                    status = transactionByReceipt[MpesaTransactions.status],
                    amount = transactionByReceipt[MpesaTransactions.amount].toDouble(),
                    phoneNumber = transactionByReceipt[MpesaTransactions.phoneNumber],
                    walletId = transactionByReceipt[MpesaTransactions.accountNumber]?.removePrefix("WALLET_") ?: "",
                    createdAt = transactionByReceipt[MpesaTransactions.createdAt].toString()
                )
            }

            // Strategy 4: Check all recent transactions for wallet
            if (transactionId.contains("WALLET_") || transactionId.startsWith("TK")) {
                println(" Searching recent transactions...")
                val recentTransactions = newSuspendedTransaction(db = database) {
                    MpesaTransactions
                        .selectAll()
                        .orderBy(MpesaTransactions.createdAt, SortOrder.DESC)
                        .limit(50)
                        .toList()
                }

                for (transaction in recentTransactions) {
                    val accountNumber = transaction[MpesaTransactions.accountNumber]
                    val receiptNumber = transaction[MpesaTransactions.mpesaReceiptNumber]
                    val checkoutId = transaction[MpesaTransactions.checkoutRequestId]

                    // Check if this transaction matches our search criteria
                    if (accountNumber.contains(transactionId.replace("WALLET_", "")) ||
                        receiptNumber == transactionId ||
                        checkoutId == transactionId
                    ) {
                        println(" Found matching transaction in recent history")
                        return TransactionStatusResult(
                            success = true,
                            transactionId = transaction[MpesaTransactions.id].toString(),
                            status = transaction[MpesaTransactions.status],
                            amount = transaction[MpesaTransactions.amount].toDouble(),
                            phoneNumber = transaction[MpesaTransactions.phoneNumber],
                            walletId = accountNumber?.removePrefix("WALLET_") ?: "",
                            createdAt = transaction[MpesaTransactions.createdAt].toString()
                        )
                    }
                }
            }

            // Strategy 5: If it looks like a CheckoutRequestID, query M-Pesa directly
            if (transactionId.length > 20) {
                println(" Checking M-Pesa API directly for checkout request ID")
                val mpesaStatus = queryStkPushStatus(transactionId)
                if (mpesaStatus.success) {
                    return TransactionStatusResult(
                        success = true,
                        transactionId = transactionId,
                        status = if (mpesaStatus.responseCode == "0") "COMPLETED" else "PENDING",
                        error = if (mpesaStatus.success) null else "Transaction status: ${mpesaStatus.message}"
                    )
                }
            }

            println(" Transaction not found in database: $transactionId")
            TransactionStatusResult(
                success = false,
                transactionId = transactionId,
                error = "Transaction not found. Receipt: $transactionId may not be in our system yet."
            )
        } catch (e: Exception) {
            println(" Error checking transaction status: ${e.message}")
            e.printStackTrace()
            TransactionStatusResult(
                success = false,
                transactionId = transactionId,
                error = "Failed to check transaction status: ${e.message}"
            )
        }
    }

    /**
     * Manually register a completed M-Pesa transaction using receipt number
     * This is useful when the callback is missed but the transaction was completed
     */
    suspend fun registerCompletedTransaction(
        mpesaReceiptNumber: String,
        walletId: String,
        phoneNumber: String,
        amount: Double,
        description: String = "Manual registration of completed M-Pesa transaction"
    ): TransactionStatusResult {
        return try {
            println(" Manually registering completed transaction: $mpesaReceiptNumber")

            // Check if transaction already exists
            val existingTransaction = newSuspendedTransaction(db = database) {
                MpesaTransactions.select {
                    MpesaTransactions.mpesaReceiptNumber eq mpesaReceiptNumber
                }.orderBy(MpesaTransactions.createdAt, SortOrder.DESC)
                 .firstOrNull()
            }

            if (existingTransaction != null) {
                println(" Transaction already exists, updating status")
                // Update existing transaction
                newSuspendedTransaction(db = database) {
                    MpesaTransactions.update({ MpesaTransactions.id eq existingTransaction[MpesaTransactions.id] }) {
                        it[MpesaTransactions.status] = "COMPLETED"
                        it[MpesaTransactions.mpesaReceiptNumber] = mpesaReceiptNumber
                    }
                }

                // Process wallet deposit
                processWalletDeposit(mpesaReceiptNumber, walletId, amount, phoneNumber)

                return TransactionStatusResult(
                    success = true,
                    transactionId = existingTransaction[MpesaTransactions.id].toString(),
                    status = "COMPLETED",
                    amount = amount,
                    phoneNumber = phoneNumber,
                    walletId = walletId,
                    createdAt = existingTransaction[MpesaTransactions.createdAt].toString()
                )
            } else {
                println(" Creating new transaction record")
                // Create new transaction record
                val newTransactionId = newSuspendedTransaction(db = database) {
                    MpesaTransactions.insert {
                        it[MpesaTransactions.phoneNumber] = formatPhoneNumber(phoneNumber)
                        it[MpesaTransactions.accountNumber] = "WALLET_$walletId"
                        it[MpesaTransactions.amount] = BigDecimal.valueOf(amount)
                        it[MpesaTransactions.description] = description
                        it[MpesaTransactions.status] = "COMPLETED"
                        it[MpesaTransactions.mpesaReceiptNumber] = mpesaReceiptNumber
                        it[MpesaTransactions.checkoutRequestId] = "MANUAL_$mpesaReceiptNumber"
                        it[MpesaTransactions.merchantRequestId] = "MANUAL_${System.currentTimeMillis()}"
                    } get MpesaTransactions.id
                }

                // Process wallet deposit
                processWalletDeposit(mpesaReceiptNumber, walletId, amount, phoneNumber)

                println(" Transaction registered and wallet deposit processed")
                TransactionStatusResult(
                    success = true,
                    transactionId = newTransactionId.toString(),
                    status = "COMPLETED",
                    amount = amount,
                    phoneNumber = phoneNumber,
                    walletId = walletId,
                    createdAt = LocalDateTime.now().toString()
                )
            }
        } catch (e: Exception) {
            println(" Error registering completed transaction: ${e.message}")
            e.printStackTrace()
            TransactionStatusResult(
                success = false,
                transactionId = mpesaReceiptNumber,
                error = "Failed to register transaction: ${e.message}"
            )
        }
    }

    /**
     * Query STK Push transaction status from Safaricom API
     */
    suspend fun queryStkPushStatus(checkoutRequestID: String): MpesaDepositResponse {
        return try {
            // Get access token
            val accessToken = getAccessToken()

            // Generate timestamp and password for query
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            val password = Base64.getEncoder().encodeToString(
                "${mpesaConfig.shortcode}${mpesaConfig.passkey}$timestamp".toByteArray()
            )

            // Prepare STK Push Query request
            val queryRequest = StkPushQueryRequest(
                BusinessShortCode = mpesaConfig.shortcode,
                Password = password,
                Timestamp = timestamp,
                CheckoutRequestID = checkoutRequestID
            )

            // Make API call to Safaricom STK Push Query endpoint
            val json = Json { ignoreUnknownKeys = true }
            val requestBody = json.encodeToString(queryRequest)

            val response = httpClient.post("$baseUrl/mpesa/stkpushquery/v1/query") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status == HttpStatusCode.OK) {
                val responseText = response.bodyAsText()
                val queryResponse = json.decodeFromString<StkPushQueryResponse>(responseText)

                // Update local transaction status based on Safaricom response
                when (queryResponse.ResultCode) {
                    "0" -> {
                        updateTransactionStatus(checkoutRequestID, "COMPLETED", "Transaction completed successfully")
                        MpesaDepositResponse(
                            success = true,
                            message = "Transaction completed successfully",
                            checkoutRequestID = checkoutRequestID,
                            responseCode = queryResponse.ResultCode,
                            responseDescription = queryResponse.ResultDesc
                        )
                    }

                    "1032" -> {
                        updateTransactionStatus(checkoutRequestID, "CANCELLED", "User cancelled transaction")
                        MpesaDepositResponse(
                            success = false,
                            message = "Transaction was cancelled by user",
                            checkoutRequestID = checkoutRequestID,
                            responseCode = queryResponse.ResultCode,
                            responseDescription = queryResponse.ResultDesc
                        )
                    }

                    "1037" -> {
                        updateTransactionStatus(checkoutRequestID, "TIMEOUT", "Transaction timed out")
                        MpesaDepositResponse(
                            success = false,
                            message = "Transaction timed out",
                            checkoutRequestID = checkoutRequestID,
                            responseCode = queryResponse.ResultCode,
                            responseDescription = queryResponse.ResultDesc
                        )
                    }

                    else -> {
                        // Still pending or other status
                        MpesaDepositResponse(
                            success = false,
                            message = queryResponse.ResultDesc,
                            checkoutRequestID = checkoutRequestID,
                            responseCode = queryResponse.ResultCode,
                            responseDescription = queryResponse.ResultDesc
                        )
                    }
                }
            } else {
                val errorText = response.bodyAsText()
                MpesaDepositResponse(
                    success = false,
                    message = "STK Push Query failed: HTTP ${response.status.value}",
                    responseDescription = errorText
                )
            }
        } catch (e: Exception) {
            MpesaDepositResponse(
                success = false,
                message = "STK Push Query failed: ${e.message}",
                responseCode = "500",
                responseDescription = e.message
            )
        }
    }

    /**
     * Manually complete a pending M-Pesa transaction (for testing/admin purposes)
     */
    suspend fun manuallyCompleteTransaction(
        checkoutRequestID: String,
        forceComplete: Boolean = false
    ): TransactionStatusResult {
        return try {
            println("🔧 Manually completing transaction: $checkoutRequestID")

            // Find the pending transaction
            val transaction = newSuspendedTransaction(db = database) {
                MpesaTransactions.select {
                    MpesaTransactions.checkoutRequestId eq checkoutRequestID
                }.singleOrNull()
            }

            if (transaction != null) {
                val accountNumber = transaction[MpesaTransactions.accountNumber]
                val amount = transaction[MpesaTransactions.amount].toDouble()
                val phoneNumber = transaction[MpesaTransactions.phoneNumber]

                if (accountNumber.startsWith("WALLET_")) {
                    val walletId = accountNumber.removePrefix("WALLET_")
                    processWalletDeposit(checkoutRequestID, walletId, amount, phoneNumber)

                    TransactionStatusResult(
                        success = true,
                        transactionId = transaction[MpesaTransactions.id].toString(),
                        status = "COMPLETED",
                        amount = amount,
                        phoneNumber = phoneNumber,
                        walletId = walletId,
                        createdAt = transaction[MpesaTransactions.createdAt].toString()
                    )
                } else {
                    TransactionStatusResult(
                        success = false,
                        transactionId = checkoutRequestID,
                        error = "Transaction is not a wallet deposit"
                    )
                }
            } else {
                TransactionStatusResult(
                    success = false,
                    transactionId = checkoutRequestID,
                    error = "Transaction not found"
                )
            }
        } catch (e: Exception) {
            println("❌ Error manually completing transaction: ${e.message}")
            TransactionStatusResult(
                success = false,
                transactionId = checkoutRequestID,
                error = "Failed to complete transaction: ${e.message}"
            )
        }
    }

    /**
     * Calculate risk score based on transaction amount
     */
    private fun calculateRiskScore(amount: Double): Double {
        return when {
            amount > 100000 -> 80.0
            amount > 50000 -> 60.0
            amount > 10000 -> 40.0
            amount > 1000 -> 20.0
            else -> 10.0
        }
    }

    /**
     * Determine risk level based on amount
     */
    private fun determineRiskLevel(amount: Double): String {
        return when {
            amount > 100000 -> "HIGH"
            amount > 10000 -> "MEDIUM"
            else -> "LOW"
        }
    }

    /**
     * Private helper methods
     */
    private fun validateDepositRequest(request: MpesaDepositRequest) {
        if (request.amount <= 0) {
            throw IllegalArgumentException("Amount must be greater than zero")
        }
        if (request.amount < 1) {
            throw IllegalArgumentException("Minimum amount is KES 1")
        }
        if (request.amount > 150000) {
            throw IllegalArgumentException("Maximum amount is KES 150,000")
        }
        if (request.phoneNumber.isBlank()) {
            throw IllegalArgumentException("Phone number is required")
        }
        if (request.accountNumber.isBlank()) {
            throw IllegalArgumentException("Account number is required")
        }
    }

    private suspend fun storePendingTransaction(
        request: MpesaDepositRequest,
        transactionId: String,
        merchantRequestID: String,
        checkoutRequestID: String
    ) {
        try {
            newSuspendedTransaction(db = database) {
                MpesaTransactions.insert {
                    it[phoneNumber] = request.phoneNumber
                    it[accountNumber] = request.accountNumber
                    it[amount] = BigDecimal.valueOf(request.amount)
                    it[description] = request.description
                    it[status] = "PENDING"
                    it[merchantRequestId] = merchantRequestID
                    it[checkoutRequestId] = checkoutRequestID
                }
            }
        } catch (e: Exception) {
            println("Failed to store pending transaction: ${e.message}")
        }
    }

    private suspend fun updateTransactionStatus(checkoutRequestID: String, status: String, description: String) {
        try {
            newSuspendedTransaction(db = database) {
                MpesaTransactions.update({ MpesaTransactions.checkoutRequestId eq checkoutRequestID }) {
                    it[MpesaTransactions.status] = status
                }
            }
        } catch (e: Exception) {
            println("Failed to update transaction status: ${e.message}")
        }
    }

    private suspend fun handleSuccessfulCallback(stkCallback: StkCallback) {
        val checkoutRequestID = stkCallback.checkoutRequestID

        // Update transaction as completed
        try {
            newSuspendedTransaction(db = database) {
                MpesaTransactions.update({ MpesaTransactions.checkoutRequestId eq checkoutRequestID }) {
                    it[status] = "COMPLETED"
                }
            }
        } catch (e: Exception) {
            println("Failed to update successful callback: ${e.message}")
        }

        // Process wallet deposit if this is a wallet transaction
        try {
            val transaction = newSuspendedTransaction(db = database) {
                MpesaTransactions.select { MpesaTransactions.checkoutRequestId eq checkoutRequestID }.singleOrNull()
            }

            transaction?.let { row ->
                val accountNumber = row[MpesaTransactions.accountNumber]
                if (accountNumber.startsWith("WALLET_")) {
                    val walletId = accountNumber.removePrefix("WALLET_")
                    val amount = row[MpesaTransactions.amount].toDouble()
                    val phoneNumber = row[MpesaTransactions.phoneNumber]
                    processWalletDeposit(checkoutRequestID, walletId, amount, phoneNumber)
                }
            }
        } catch (e: Exception) {
            println("Failed to process wallet deposit after callback: ${e.message}")
        }
    }

    private suspend fun processWalletDeposit(
        transactionId: String,
        walletId: String,
        amount: Double,
        phoneNumber: String
    ) {
        try {
            println("💰 Processing wallet deposit: TxnID=$transactionId, WalletID=$walletId, Amount=$amount")

            // Find the target wallet or use CUSTOMER_FLOAT as fallback
            val targetWallet = newSuspendedTransaction(db = database) {
                MasterWallets.select {
                    MasterWallets.id eq UUID.fromString(walletId)
                }.firstOrNull()
            } ?: newSuspendedTransaction(db = database) {
                MasterWallets.select {
                    MasterWallets.walletType eq MasterWalletType.CUSTOMER_FLOAT
                }.firstOrNull()
            }

            if (targetWallet != null) {
                val actualWalletId = targetWallet[MasterWallets.id].value
                val currentBalance = targetWallet[MasterWallets.balance]
                val newBalance = currentBalance + java.math.BigDecimal.valueOf(amount)

                newSuspendedTransaction(db = database) {
                    // Create transaction record
                    MasterWalletTransactions.insert {
                        it[MasterWalletTransactions.walletId] = actualWalletId
                        it[MasterWalletTransactions.transactionType] = MasterWalletTransactionType.CUSTOMER_PAYOUT
                        it[MasterWalletTransactions.amount] = java.math.BigDecimal.valueOf(amount)
                        it[MasterWalletTransactions.balanceBefore] = currentBalance
                        it[MasterWalletTransactions.balanceAfter] = newBalance
                        it[MasterWalletTransactions.description] = "M-Pesa wallet deposit from $phoneNumber"
                        it[MasterWalletTransactions.reference] = transactionId
                        it[MasterWalletTransactions.externalAccountId] = "WALLET_$walletId"
                        it[MasterWalletTransactions.processedBy] =
                            UUID.fromString("00000000-0000-0000-0000-000000000000") // System user
                        it[MasterWalletTransactions.status] = TransactionStatus.COMPLETED
                        it[MasterWalletTransactions.riskScore] =
                            java.math.BigDecimal.valueOf(calculateRiskScore(amount))
                        it[MasterWalletTransactions.riskLevel] = determineRiskLevel(amount)
                    }

                    // Update wallet balance
                    MasterWallets.update({ MasterWallets.id eq actualWalletId }) {
                        it[MasterWallets.balance] = newBalance
                        it[MasterWallets.availableBalance] = newBalance
                    }
                }

                // Update M-Pesa transaction status to completed
                updateTransactionStatus(transactionId, "COMPLETED", "Wallet deposit processed successfully")

                println("✅ Wallet deposit processed successfully: $transactionId for wallet $actualWalletId, amount: $amount")
                println("💳 New wallet balance: $newBalance")
            } else {
                println("⚠️ No target wallet found for wallet ID: $walletId")
            }
        } catch (e: Exception) {
            println("❌ Failed to process wallet deposit: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Initiate M-Pesa deposit into a customer account
     */
    suspend fun initiateCustomerAccountDeposit(
        accountNumber: String,
        phoneNumber: String,
        amount: Double,
        description: String
    ): MpesaResponse {
        return try {
            println("🏦 Initiating customer account deposit: Account=$accountNumber, Phone=$phoneNumber, Amount=$amount")

            // Verify account exists
            val account = newSuspendedTransaction(db = database) {
                Accounts.select { Accounts.accountNumber eq accountNumber }.singleOrNull()
            }

            if (account == null) {
                return MpesaResponse(
                    success = false,
                    message = "Account not found: $accountNumber",
                    error = "ACCOUNT_NOT_FOUND"
                )
            }

            // Check if account is active
            val accountStatus = account[Accounts.status]
            if (accountStatus != AccountStatus.ACTIVE) {
                return MpesaResponse(
                    success = false,
                    message = "Account is not active. Status: $accountStatus",
                    error = "ACCOUNT_INACTIVE"
                )
            }

            // Create deposit request
            val depositRequest = MpesaDepositRequest(
                phoneNumber = phoneNumber,
                accountNumber = accountNumber,
                amount = amount,
                description = description
            )

            // Initiate STK Push
            val stkResponse = initiateStkPush(depositRequest)

            if (stkResponse.success) {
                // Schedule automatic status checking and deposit processing
                GlobalScope.launch {
                    delay(2000) // Wait 2 seconds then check status (reduced from 5)
                    checkAndProcessCustomerDeposit(
                        stkResponse.checkoutRequestID ?: "",
                        accountNumber,
                        amount,
                        phoneNumber
                    )
                }

                MpesaResponse(
                    success = true,
                    message = "M-Pesa STK Push sent successfully. Please check your phone and enter PIN.",
                    transactionId = stkResponse.transactionId,
                    checkoutRequestID = stkResponse.checkoutRequestID,
                    merchantRequestID = stkResponse.merchantRequestID,
                    customerMessage = "Please enter your M-Pesa PIN to complete the deposit of KES ${amount.toInt()} to account $accountNumber"
                )
            } else {
                MpesaResponse(
                    success = false,
                    message = stkResponse.message,
                    error = "ACCOUNT_DEPOSIT_ERROR"
                )
            }

        } catch (e: Exception) {
            println("❌ Error initiating customer account deposit: ${e.message}")
            e.printStackTrace()
            MpesaResponse(
                success = false,
                message = "Failed to initiate account deposit: ${e.message}",
                error = "ACCOUNT_DEPOSIT_ERROR"
            )
        }
    }

    /**
     * Check transaction status and process customer account deposit if completed
     */
    private suspend fun checkAndProcessCustomerDeposit(
        checkoutRequestID: String,
        accountNumber: String,
        amount: Double,
        phoneNumber: String
    ) {
        try {
            println("🔍 Checking customer deposit transaction status for: $checkoutRequestID")

            // Reduced wait time - check after 3 seconds instead of 10
            delay(3000)

            // For sandbox testing, we'll simulate completion after a delay
            val isSimulatedCompletion = true // Change to false in production

            if (isSimulatedCompletion) {
                println("🎯 Simulating successful M-Pesa completion for customer account deposit...")
                processCustomerAccountDeposit(checkoutRequestID, accountNumber, amount, phoneNumber)
                return
            }

            // Query transaction status from Safaricom
            try {
                val statusResponse = queryStkPushStatus(checkoutRequestID)

                if (statusResponse.success && statusResponse.responseCode == "0") {
                    // Transaction completed successfully, process account deposit
                    processCustomerAccountDeposit(checkoutRequestID, accountNumber, amount, phoneNumber)
                } else if (statusResponse.responseCode == "1032") {
                    // User cancelled
                    updateTransactionStatus(checkoutRequestID, "CANCELLED", "User cancelled transaction")
                } else if (statusResponse.responseCode == "1037") {
                    // Timeout
                    updateTransactionStatus(checkoutRequestID, "TIMEOUT", "User did not enter PIN in time")
                } else {
                    // Still pending or failed, try one more time after delay
                    delay(5000) // Reduced from 15 seconds
                    val secondCheck = queryStkPushStatus(checkoutRequestID)

                    if (secondCheck.success && secondCheck.responseCode == "0") {
                        processCustomerAccountDeposit(checkoutRequestID, accountNumber, amount, phoneNumber)
                    } else {
                        println("💔 Transaction not completed after second check: ${secondCheck.message}")
                        updateTransactionStatus(
                            checkoutRequestID,
                            "FAILED",
                            "Transaction was not completed within time limit"
                        )
                    }
                }
            } catch (e: Exception) {
                println("⚠️ Error checking transaction status: ${e.message}")
                // Fallback: For testing, process the deposit anyway after delay
                delay(2000) // Reduced from 5 seconds
                println("🔄 Processing deposit despite status check failure (testing mode)")
                processCustomerAccountDeposit(checkoutRequestID, accountNumber, amount, phoneNumber)
            }
        } catch (e: Exception) {
            println("❌ Error in checkAndProcessCustomerDeposit: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Process the actual customer account deposit - update balance and create transaction
     */
    private suspend fun processCustomerAccountDeposit(
        transactionId: String,
        accountNumber: String,
        amount: Double,
        phoneNumber: String
    ) {
        try {
            println("💰 Processing customer account deposit: TxnID=$transactionId, Account=$accountNumber, Amount=$amount")

            // Find the customer account
            val account = newSuspendedTransaction(db = database) {
                Accounts.select { Accounts.accountNumber eq accountNumber }.singleOrNull()
            }

            if (account == null) {
                println("⚠️ Account not found: $accountNumber")
                updateTransactionStatus(transactionId, "FAILED", "Account not found")
                return
            }

            val accountId = account[Accounts.id].value
            val currentBalance = account[Accounts.balance]
            val newBalance = currentBalance + BigDecimal.valueOf(amount)

            // Update account balance and create transaction record
            newSuspendedTransaction(db = database) {
                // Update account balance
                Accounts.update({ Accounts.accountNumber eq accountNumber }) {
                    it[Accounts.balance] = newBalance
                    it[Accounts.availableBalance] = newBalance
                    it[Accounts.lastTransactionDate] = Instant.now()
                }

                // Create transaction record
                Transactions.insert {
                    it[Transactions.accountId] = accountId
                    it[Transactions.type] = TransactionType.DEPOSIT
                    it[Transactions.amount] = BigDecimal.valueOf(amount)
                    it[Transactions.status] = TransactionStatus.COMPLETED
                    it[Transactions.description] = "M-Pesa deposit from $phoneNumber (ID: $transactionId)"
                    it[Transactions.balanceAfter] = newBalance
                    it[Transactions.reference] = transactionId // Use M-Pesa transaction ID as reference
                    it[Transactions.merchantName] = "M-Pesa"
                    it[Transactions.category] = "MOBILE_MONEY"
                }
            }

            // Update M-Pesa transaction status
            updateTransactionStatus(transactionId, "COMPLETED", "Account deposit processed successfully")

            // Send notification to customer
            try {
                val customerId = account[Accounts.customerId]
                val accountIdUUID = account[Accounts.id].value

                if (amount >= 1000) {
                    notificationService.notifyLargeDeposit(
                        customerId = customerId,
                        accountId = accountIdUUID,
                        amount = amount.toString()
                    )
                } else {
                    // Create a transaction ID for the notification
                    val txnId = UUID.fromString(transactionId)
                    notificationService.notifyTransactionCompleted(
                        customerId = customerId,
                        transactionId = txnId,
                        amount = amount.toString(),
                        type = "M-Pesa Deposit"
                    )
                }
                println("📬 Notification sent to customer $customerId")
            } catch (e: Exception) {
                println("⚠️ Failed to send notification: ${e.message}")
                e.printStackTrace()
            }

            println("✅ Customer account deposit processed successfully!")
            println("   Account: $accountNumber")
            println("   Amount: KES $amount")
            println("   New Balance: KES $newBalance")
            println("   Transaction ID: $transactionId")

        } catch (e: Exception) {
            println("❌ Failed to process customer account deposit: ${e.message}")
            e.printStackTrace()
            updateTransactionStatus(transactionId, "FAILED", "Deposit processing error: ${e.message}")
        }
    }

    /**
     * Process M-Pesa reversal - handles Safaricom reversing a completed transaction
     */
    suspend fun processWalletReversal(
        checkoutRequestID: String,
        mpesaReceiptNumber: String? = null,
        reason: String = "Safaricom reversed the transaction"
    ): TransactionStatusResult {
        return try {
            println("🔄 Processing M-Pesa reversal for: $checkoutRequestID")

            val originalTransaction = newSuspendedTransaction(db = database) {
                if (mpesaReceiptNumber != null) {
                    MpesaTransactions.select {
                        MpesaTransactions.mpesaReceiptNumber eq mpesaReceiptNumber
                    }.singleOrNull()
                } else {
                    MpesaTransactions.select {
                        MpesaTransactions.checkoutRequestId eq checkoutRequestID
                    }.singleOrNull()
                }
            }

            if (originalTransaction == null) {
                return TransactionStatusResult(
                    success = false,
                    transactionId = checkoutRequestID,
                    error = "Original transaction not found"
                )
            }

            val transactionStatus = originalTransaction[MpesaTransactions.status]
            if (transactionStatus == "REVERSED") {
                return TransactionStatusResult(
                    success = false,
                    transactionId = checkoutRequestID,
                    error = "Transaction already reversed"
                )
            }

            if (transactionStatus != "COMPLETED") {
                return TransactionStatusResult(
                    success = false,
                    transactionId = checkoutRequestID,
                    error = "Cannot reverse transaction with status: $transactionStatus"
                )
            }

            val accountNumber = originalTransaction[MpesaTransactions.accountNumber]
            val amount = originalTransaction[MpesaTransactions.amount].toDouble()
            val phoneNumber = originalTransaction[MpesaTransactions.phoneNumber]

            if (accountNumber.startsWith("WALLET_")) {
                val walletId = accountNumber.removePrefix("WALLET_")
                processWalletReversalDeduction(checkoutRequestID, walletId, amount, reason)
            } else {
                processCustomerAccountReversalDeduction(checkoutRequestID, accountNumber, amount, reason)
            }

            newSuspendedTransaction(db = database) {
                MpesaTransactions.update({
                    (MpesaTransactions.checkoutRequestId eq checkoutRequestID) or
                    (MpesaTransactions.mpesaReceiptNumber eq (mpesaReceiptNumber ?: ""))
                }) {
                    it[MpesaTransactions.status] = "REVERSED"
                }
            }

            println("✅ M-Pesa reversal processed successfully")
            TransactionStatusResult(
                success = true,
                transactionId = originalTransaction[MpesaTransactions.id].toString(),
                status = "REVERSED",
                amount = amount,
                phoneNumber = phoneNumber,
                walletId = if (accountNumber.startsWith("WALLET_")) accountNumber.removePrefix("WALLET_") else null,
                createdAt = originalTransaction[MpesaTransactions.createdAt].toString()
            )

        } catch (e: Exception) {
            println("❌ Error processing M-Pesa reversal: ${e.message}")
            e.printStackTrace()
            TransactionStatusResult(
                success = false,
                transactionId = checkoutRequestID,
                error = "Failed to process reversal: ${e.message}"
            )
        }
    }

    private suspend fun processWalletReversalDeduction(
        transactionId: String,
        walletId: String,
        amount: Double,
        reason: String
    ) {
        try {
            val targetWallet = newSuspendedTransaction(db = database) {
                MasterWallets.select {
                    MasterWallets.id eq UUID.fromString(walletId)
                }.firstOrNull()
            } ?: newSuspendedTransaction(db = database) {
                MasterWallets.select {
                    MasterWallets.walletType eq MasterWalletType.CUSTOMER_FLOAT
                }.firstOrNull()
            }

            if (targetWallet != null) {
                val actualWalletId = targetWallet[MasterWallets.id].value
                val currentBalance = targetWallet[MasterWallets.balance]
                val newBalance = currentBalance - java.math.BigDecimal.valueOf(amount)

                newSuspendedTransaction(db = database) {
                    MasterWalletTransactions.insert {
                        it[MasterWalletTransactions.walletId] = actualWalletId
                        it[MasterWalletTransactions.transactionType] = MasterWalletTransactionType.REVERSAL
                        it[MasterWalletTransactions.amount] = java.math.BigDecimal.valueOf(amount)
                        it[MasterWalletTransactions.balanceBefore] = currentBalance
                        it[MasterWalletTransactions.balanceAfter] = newBalance
                        it[MasterWalletTransactions.description] = "M-Pesa Reversal: $reason"
                        it[MasterWalletTransactions.reference] = transactionId
                        it[MasterWalletTransactions.externalAccountId] = "WALLET_$walletId"
                        it[MasterWalletTransactions.processedBy] = UUID.fromString("00000000-0000-0000-0000-000000000000")
                        it[MasterWalletTransactions.status] = TransactionStatus.COMPLETED
                        it[MasterWalletTransactions.riskScore] = java.math.BigDecimal.valueOf(calculateRiskScore(amount))
                        it[MasterWalletTransactions.riskLevel] = determineRiskLevel(amount)
                    }

                    MasterWallets.update({ MasterWallets.id eq actualWalletId }) {
                        it[MasterWallets.balance] = newBalance
                        it[MasterWallets.availableBalance] = newBalance
                    }
                }

                println("✅ Wallet reversal processed, New balance: $newBalance")
            }
        } catch (e: Exception) {
            println("❌ Failed to process wallet reversal: ${e.message}")
            throw e
        }
    }

    private suspend fun processCustomerAccountReversalDeduction(
        transactionId: String,
        accountNumber: String,
        amount: Double,
        reason: String
    ) {
        try {
            val account = newSuspendedTransaction(db = database) {
                Accounts.select { Accounts.accountNumber eq accountNumber }.singleOrNull()
            }

            if (account == null) {
                println("⚠️ Account not found for reversal deduction: $accountNumber")
                return
            }

            val accountId = account[Accounts.id].value
            val currentBalance = account[Accounts.balance]
            
            // Find the original transaction to determine if we should credit or debit
            // We search for transactions linked to this account with matching amount and description containing this checkoutRequestId
            val originalTxn = newSuspendedTransaction(db = database) {
                Transactions.select {
                    (Transactions.accountId eq accountId) and 
                    (Transactions.amount eq BigDecimal.valueOf(amount)) and
                    (Transactions.description like "%$transactionId%")
                }.orderBy(Transactions.timestamp, SortOrder.DESC).firstOrNull()
            }

            // Determine if we should credit or debit back
            // If original was DEPOSIT, we deduct (reverse credit)
            // If original was WITHDRAWAL/PAYMENT, we add back (reverse debit)
            val isOutgoing = originalTxn != null && (
                originalTxn[Transactions.type] == TransactionType.WITHDRAWAL ||
                originalTxn[Transactions.type] == TransactionType.PAYMENT ||
                originalTxn[Transactions.type] == TransactionType.MPESA_WITHDRAWAL ||
                originalTxn[Transactions.type] == TransactionType.MPESA_B2C_PAYMENT ||
                originalTxn[Transactions.type] == TransactionType.TRANSFER ||
                originalTxn[Transactions.type] == TransactionType.LOAN_PAYMENT ||
                originalTxn[Transactions.type] == TransactionType.MOBILE_MONEY_WITHDRAWAL
            )

            val newBalance = if (isOutgoing) {
                currentBalance + BigDecimal.valueOf(amount)
            } else {
                currentBalance - BigDecimal.valueOf(amount)
            }

            newSuspendedTransaction(db = database) {
                // Update account balance
                Accounts.update({ Accounts.id eq accountId }) {
                    it[Accounts.balance] = newBalance
                    it[Accounts.availableBalance] = newBalance
                    it[Accounts.lastTransactionDate] = Instant.now()
                }

                // Create reversal transaction record
                Transactions.insert {
                    it[Transactions.accountId] = accountId
                    it[Transactions.type] = TransactionType.REVERSAL
                    it[Transactions.amount] = BigDecimal.valueOf(amount)
                    it[Transactions.status] = TransactionStatus.COMPLETED
                    it[Transactions.description] = "M-Pesa Reversal (${if (isOutgoing) "Credit Back" else "Deduction"}) for: ${originalTxn?.get(Transactions.description) ?: transactionId}"
                    it[Transactions.balanceAfter] = newBalance
                    it[Transactions.reference] = "REV-${transactionId.take(20)}"
                    it[Transactions.merchantName] = "M-Pesa"
                    it[Transactions.category] = "MOBILE_MONEY"
                }

                // Mark original transaction as REVERSED if found
                if (originalTxn != null) {
                    Transactions.update({ Transactions.id eq originalTxn[Transactions.id] }) {
                        it[status] = TransactionStatus.REVERSED
                    }
                }
            }

            println("✅ Account reversal processed (${if (isOutgoing) "Credit" else "Debit"}), New balance: $newBalance")
        } catch (e: Exception) {
            println("❌ Failed to process account reversal: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Automatically detect and process reversed transactions
     * This should be called periodically (e.g., every 5 minutes) to check for reversals
     */
    suspend fun detectAndProcessReversals() {
        try {
            println("🔍 Scanning for reversed M-Pesa transactions...")

            // Find all COMPLETED transactions
            val completedTransactions = newSuspendedTransaction(db = database) {
                MpesaTransactions.select {
                    MpesaTransactions.status eq "COMPLETED"
                }.limit(100)
                    .map { row ->
                        Triple(
                            row[MpesaTransactions.checkoutRequestId],
                            row[MpesaTransactions.amount].toDouble(),
                            row[MpesaTransactions.accountNumber]
                        )
                    }
            }

            println("📊 Found ${completedTransactions.size} completed transactions to verify")

            var reversalsProcessed = 0

            // Check each transaction with Safaricom API
            completedTransactions.forEach { (checkoutRequestId, amount, accountNumber) ->
                try {
                    // Query Safaricom API for current status
                    val statusResult = queryStkPushStatus(checkoutRequestId)

                    // Check if transaction was reversed (result code 1032 or similar)
                    // Note: You may need to adjust this based on actual Safaricom response codes
                    val isReversed = statusResult.responseCode == "1032" ||
                                   statusResult.responseDescription?.contains("reversed", ignoreCase = true) ?: false ||
                                   statusResult.responseDescription?.contains("cancelled", ignoreCase = true) ?: false

                    if (isReversed) {
                        println("⚠️ Detected reversal for transaction: $checkoutRequestId (Amount: $amount)")

                        // Process the reversal
                        val reversalResult = processWalletReversal(
                            checkoutRequestID = checkoutRequestId,
                            reason = "Automatic reversal detection: ${statusResult.responseDescription}"
                        )

                        if (reversalResult.success) {
                            reversalsProcessed++
                            println("✅ Reversal processed successfully for: $checkoutRequestId")
                        } else {
                            println("❌ Failed to process reversal for: $checkoutRequestId - ${reversalResult.error}")
                        }
                    }

                    // Add delay to avoid rate limiting
                    delay(2000)

                } catch (e: Exception) {
                    println("⚠️ Error checking transaction $checkoutRequestId: ${e.message}")
                }
            }

            println("✅ Reversal scan complete. Processed $reversalsProcessed reversals.")

        } catch (e: Exception) {
            println("❌ Error in reversal detection: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Process all pending reversals immediately - useful for fixing existing issues
     */
    suspend fun processAllPendingReversals() {
        try {
            println("🔄 Processing ALL pending reversals (one-time fix)...")

            // Find all COMPLETED transactions where balance hasn't been corrected
            val transactions = newSuspendedTransaction(db = database) {
                MpesaTransactions.select {
                    MpesaTransactions.status eq "COMPLETED"
                }.map { row ->
                    mapOf(
                        "checkoutRequestId" to row[MpesaTransactions.checkoutRequestId],
                        "amount" to row[MpesaTransactions.amount].toDouble(),
                        "accountNumber" to row[MpesaTransactions.accountNumber],
                        "phoneNumber" to row[MpesaTransactions.phoneNumber]
                    )
                }
            }

            println("📋 Found ${transactions.size} completed transactions")
            println("⚠️ Note: This will check ALL completed transactions. Use with caution!")

            var processedCount = 0

            transactions.forEach { txn ->
                val checkoutRequestId = txn["checkoutRequestId"] as String

                try {
                    println("🔍 Checking: $checkoutRequestId")

                    // Query actual status from Safaricom
                    val statusResult = queryStkPushStatus(checkoutRequestId)

                    // If transaction was actually reversed or failed, process reversal
                    val pattern = Regex("reversed|cancelled|failed", RegexOption.IGNORE_CASE)
                    val shouldReverse = statusResult.responseCode != "0" &&
                                      statusResult.responseCode != "" &&
                                      (statusResult.responseDescription?.contains(pattern) ?: false)

                    if (shouldReverse) {
                        println("🔄 Processing reversal for: $checkoutRequestId")
                        val result = processWalletReversal(
                            checkoutRequestID = checkoutRequestId,
                            reason = "Batch reversal fix: ${statusResult.responseDescription}"
                        )

                        if (result.success) {
                            processedCount++
                            println("✅ Reversal applied: $checkoutRequestId")
                        }
                    }

                    delay(2000) // Rate limiting
                } catch (e: Exception) {
                    println("⚠️ Error processing $checkoutRequestId: ${e.message}")
                }
            }

            println("✅ Batch reversal processing complete. Processed $processedCount reversals.")

        } catch (e: Exception) {
            println("❌ Error in batch reversal processing: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun getMpesaTransactions(
        phoneNumber: String? = null,
        accountNumber: String? = null,
        status: String? = null
    ): List<Map<String, Any?>> {
        return newSuspendedTransaction(db = database) {
            val query = MpesaTransactions.selectAll()

            if (phoneNumber != null) {
                query.andWhere { MpesaTransactions.phoneNumber eq phoneNumber }
            }
            if (accountNumber != null) {
                query.andWhere { MpesaTransactions.accountNumber eq accountNumber }
            }
            if (status != null) {
                query.andWhere { MpesaTransactions.status eq status }
            }

            query.orderBy(MpesaTransactions.createdAt to SortOrder.DESC)
                .limit(50)
                .map { row ->
                    mapOf(
                        "id" to row[MpesaTransactions.id].toString(),
                        "checkoutRequestId" to row[MpesaTransactions.checkoutRequestId],
                        "merchantRequestId" to row[MpesaTransactions.merchantRequestId],
                        "phoneNumber" to row[MpesaTransactions.phoneNumber],
                        "accountNumber" to row[MpesaTransactions.accountNumber],
                        "amount" to row[MpesaTransactions.amount].toDouble(),
                        "status" to row[MpesaTransactions.status],
                        "mpesaReceiptNumber" to row[MpesaTransactions.mpesaReceiptNumber],
                        "description" to row[MpesaTransactions.description],
                        "createdAt" to row[MpesaTransactions.createdAt].toString()
                    )
                }
        }
    }

    fun cleanup() {
        // Cleanup resources
        httpClient.close()
    }

    // ==================== DATA MODELS ====================
    @Serializable
    data class TransactionStatusResult(
        val success: Boolean,
        val transactionId: String,
        val status: String? = null,
        val amount: Double? = null,
        val phoneNumber: String? = null,
        val walletId: String? = null,
        val createdAt: String? = null,
        val error: String? = null
    )

    @Serializable
    data class MpesaConfiguration(
        val consumerKey: String,
        val consumerSecret: String,
        val passkey: String,
        val shortcode: String,
        val environment: String,
        val callbackUrl: String
    )

    @Serializable
    data class AccessTokenCache(
        val token: String,
        val expiresAt: String
    )

    @Serializable
    data class TokenResponse(
        val access_token: String,
        val expires_in: String
    )

    @Serializable
    data class MpesaDepositRequest(
        val phoneNumber: String,
        val accountNumber: String,
        val amount: Double,
        val description: String,
        val operatorId: String? = null
    )

    @Serializable
    data class MpesaDepositResponse(
        val success: Boolean,
        val message: String,
        val merchantRequestID: String? = null,
        val checkoutRequestID: String? = null,
        val responseCode: String? = null,
        val responseDescription: String? = null,
        val customerMessage: String? = null,
        val transactionId: String? = null
    )

    @Serializable
    data class MpesaCallbackData(
        val body: CallbackBody
    )

    @Serializable
    data class CallbackBody(
        val stkCallback: StkCallback
    )

    @Serializable
    data class StkCallback(
        val merchantRequestID: String,
        val checkoutRequestID: String,
        val resultCode: Int,
        val resultDesc: String,
        val callbackMetadata: CallbackMetadata? = null
    )

    @Serializable
    data class CallbackMetadata(
        val item: List<CallbackItem>
    )

    @Serializable
    data class CallbackItem(
        val name: String,
        val value: String? = null
    )

    @Serializable
    data class MpesaResponse(
        val success: Boolean,
        val message: String,
        val error: String? = null,
        val transactionId: String? = null,
        val checkoutRequestID: String? = null,
        val merchantRequestID: String? = null,
        val customerMessage: String? = null
    )

    @Serializable
    data class StkPushRequest(
        val BusinessShortCode: String,
        val Password: String,
        val Timestamp: String,
        val TransactionType: String,
        val Amount: Int,
        val PartyA: String,
        val PartyB: String,
        val PhoneNumber: String,
        val CallBackURL: String,
        val AccountReference: String,
        val TransactionDesc: String
    )

    @Serializable
    data class StkPushApiResponse(
        val MerchantRequestID: String,
        val CheckoutRequestID: String,
        val ResponseCode: String,
        val ResponseDescription: String,
        val CustomerMessage: String
    )

    @Serializable
    data class StkPushQueryRequest(
        val BusinessShortCode: String,
        val Password: String,
        val Timestamp: String,
        val CheckoutRequestID: String
    )

    @Serializable
    data class StkPushQueryResponse(
        val ResultCode: String,
        val ResultDesc: String,
        val CheckoutRequestID: String
    )

    // ==================== B2C (Business to Customer) API ====================

    /**
     * B2C Payment Request - Withdraw money from bank account to M-Pesa
     */
    suspend fun initiateB2CPayment(
        phoneNumber: String,
        amount: Double,
        accountNumber: String,
        customerId: String,
        remarks: String = "Withdrawal to M-Pesa"
    ): B2CResponse {
        return try {
            println("🔄 Initiating B2C payment: $amount to $phoneNumber")

            // Format phone number
            val formattedPhone = formatPhoneNumber(phoneNumber)

            // Get access token
            val accessToken = getAccessToken()

            // Generate security credential (encrypted initiator password)
            val securityCredential = generateSecurityCredential()

            // Create B2C request
            val b2cRequest = B2CPaymentRequest(
                InitiatorName = "testapi", // Your initiator username
                SecurityCredential = securityCredential,
                CommandID = "BusinessPayment", // Use "SalaryPayment", "BusinessPayment", or "PromotionPayment"
                Amount = amount.toInt(),
                PartyA = mpesaConfig.shortcode, // Your B2C shortcode
                PartyB = formattedPhone,
                Remarks = remarks.ifBlank { "Withdrawal to M-Pesa" },
                QueueTimeOutURL = "${mpesaConfig.callbackUrl}/timeout",
                ResultURL = "${mpesaConfig.callbackUrl}/result",
                Occassion = "Withdrawal"
            )

            println("📤 Sending B2C request to Safaricom...")

            val json = Json { ignoreUnknownKeys = true }
            val requestBody = json.encodeToString(B2CPaymentRequest.serializer(), b2cRequest)
            println("📋 B2C Request body: $requestBody")

            val response = httpClient.post("$baseUrl/mpesa/b2c/v1/paymentrequest") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status == HttpStatusCode.OK) {
                val responseText = response.bodyAsText()
                println("✅ B2C Response: $responseText")

                val json = Json { ignoreUnknownKeys = true }
                val b2cApiResponse = json.decodeFromString<B2CApiResponse>(responseText)

                // Record B2C transaction in database
                val transactionId = UUID.randomUUID()
                newSuspendedTransaction(db = database) {
                    // Record in mpesa_transactions table
                    MpesaTransactions.insert {
                        it[id] = transactionId
                        it[MpesaTransactions.phoneNumber] = formattedPhone
                        it[MpesaTransactions.accountNumber] = accountNumber
                        it[MpesaTransactions.amount] = BigDecimal(amount)
                        it[status] = "PENDING"
                        it[MpesaTransactions.description] = "B2C Withdrawal to M-Pesa (ID: ${b2cApiResponse.ConversationID})"
                        it[merchantRequestId] = b2cApiResponse.OriginatorConversationID
                        it[checkoutRequestId] = b2cApiResponse.ConversationID
                    }

                    // Deduct from customer account
                    val account = Accounts.select { Accounts.accountNumber eq accountNumber }.singleOrNull()
                    if (account != null) {
                        val accountId = account[Accounts.id]
                        val currentBalance = account[Accounts.balance]
                        val newBalance = currentBalance - BigDecimal(amount)

                        // Check sufficient balance
                        if (newBalance < BigDecimal.ZERO) {
                            throw Exception("Insufficient funds for withdrawal")
                        }

                        // Update account balance
                        Accounts.update({ Accounts.id eq accountId }) {
                            it[balance] = newBalance
                            it[availableBalance] = newBalance
                        }

                        // Create transaction record
                        Transactions.insert {
                            it[Transactions.id] = UUID.randomUUID()
                            it[Transactions.accountId] = accountId.value
                            it[Transactions.type] = TransactionType.WITHDRAWAL
                            it[Transactions.amount] = BigDecimal(amount)
                            it[Transactions.description] = "M-Pesa Withdrawal (ID: ${b2cApiResponse.ConversationID})"
                            it[Transactions.balanceAfter] = newBalance
                            it[Transactions.reference] = b2cApiResponse.ConversationID
                            it[Transactions.merchantName] = "M-Pesa"
                            it[Transactions.category] = "WITHDRAWAL"
                            it[Transactions.status] = TransactionStatus.PENDING
                        }
                    }
                }

                B2CResponse(
                    success = true,
                    message = "Withdrawal initiated successfully. Money will be sent to M-Pesa shortly.",
                    conversationId = b2cApiResponse.ConversationID,
                    originatorConversationId = b2cApiResponse.OriginatorConversationID,
                    responseCode = b2cApiResponse.ResponseCode,
                    responseDescription = b2cApiResponse.ResponseDescription
                )
            } else {
                val errorText = response.bodyAsText()
                println("❌ B2C request failed: ${response.status} - $errorText")
                B2CResponse(
                    success = false,
                    message = "Failed to initiate withdrawal: $errorText",
                    error = "B2C_ERROR"
                )
            }
        } catch (e: Exception) {
            println("❌ B2C Error: ${e.message}")
            e.printStackTrace()
            B2CResponse(
                success = false,
                message = "Failed to initiate withdrawal: ${e.message}",
                error = "B2C_EXCEPTION"
            )
        }
    }

    /**
     * Generate security credential for B2C (encrypted initiator password)
     */
    private fun generateSecurityCredential(): String {
        // For sandbox, use the test security credential
        // In production, you need to encrypt your initiator password with Safaricom's public cert
        return if (mpesaConfig.environment == "sandbox") {
            "Safaricom999!*!"
        } else {
            // TODO: Implement actual password encryption with public certificate
            // See: https://developer.safaricom.co.ke/APIs/MpesaExpressQuery
            throw Exception("Production security credential not implemented. Please encrypt initiator password with public cert.")
        }
    }

    // B2C Data Models
    @Serializable
    data class B2CPaymentRequest(
        val InitiatorName: String,
        val SecurityCredential: String,
        val CommandID: String,
        val Amount: Int,
        val PartyA: String,
        val PartyB: String,
        val Remarks: String,
        val QueueTimeOutURL: String,
        val ResultURL: String,
        val Occassion: String
    )

    @Serializable
    data class B2CApiResponse(
        val ConversationID: String,
        val OriginatorConversationID: String,
        val ResponseCode: String,
        val ResponseDescription: String
    )

    @Serializable
    data class B2CResponse(
        val success: Boolean,
        val message: String,
        val conversationId: String? = null,
        val originatorConversationId: String? = null,
        val responseCode: String? = null,
        val responseDescription: String? = null,
        val error: String? = null
    )

    // ==================== Business Buy Goods API ====================

    /**
     * Business Buy Goods - Pay for goods/services from business account to till number
     */
    @Serializable
    data class BusinessBuyGoodsRequest(
        val Initiator: String,
        val SecurityCredential: String,
        val CommandID: String = "BusinessBuyGoods",
        val SenderIdentifierType: String = "4",
        val RecieverIdentifierType: String = "4",
        val Amount: String,
        val PartyA: String,
        val PartyB: String,
        val AccountReference: String,
        val Requester: String? = null,
        val Remarks: String,
        val QueueTimeOutURL: String,
        val ResultURL: String,
        val Occassion: String? = null
    )

    @Serializable
    data class BusinessBuyGoodsApiResponse(
        val OriginatorConversationID: String,
        val ConversationID: String,
        val ResponseCode: String,
        val ResponseDescription: String
    )

    @Serializable
    data class BusinessBuyGoodsResponse(
        val success: Boolean,
        val message: String,
        val conversationId: String? = null,
        val originatorConversationId: String? = null,
        val responseCode: String? = null,
        val responseDescription: String? = null,
        val transactionId: String? = null,
        val error: String? = null
    )

    /**
     * Initiate Business Buy Goods transaction
     * Pays for goods/services from business account to till number
     */
    suspend fun initiateBusinessBuyGoods(
        tillNumber: String,
        amount: Double,
        accountReference: String,
        remarks: String,
        requesterPhone: String? = null
    ): BusinessBuyGoodsResponse {
        return try {
            println("🛒 Initiating Business Buy Goods - Till: $tillNumber, Amount: $amount")

            // Validate inputs
            if (amount <= 0) {
                throw IllegalArgumentException("Amount must be greater than zero")
            }
            if (amount > 150000) {
                throw IllegalArgumentException("Maximum amount is KES 150,000")
            }
            if (tillNumber.isBlank()) {
                throw IllegalArgumentException("Till number is required")
            }

            // Get access token
            val accessToken = getAccessToken()

            // Generate security credential
            val securityCredential = generateSecurityCredential()

            // Prepare Buy Goods request
            val buyGoodsRequest = BusinessBuyGoodsRequest(
                Initiator = "testapi",
                SecurityCredential = securityCredential,
                CommandID = "BusinessBuyGoods",
                SenderIdentifierType = "4",
                RecieverIdentifierType = "4",
                Amount = amount.toInt().toString(),
                PartyA = mpesaConfig.shortcode,
                PartyB = tillNumber,
                AccountReference = accountReference,
                Requester = requesterPhone?.let { formatPhoneNumber(it) },
                Remarks = remarks,
                QueueTimeOutURL = "${mpesaConfig.callbackUrl}/businessbuygoods/queue",
                ResultURL = "${mpesaConfig.callbackUrl}/businessbuygoods/result",
                Occassion = "Payment for goods/services"
            )

            println("📤 Sending Business Buy Goods request to Safaricom...")
            val json = Json { ignoreUnknownKeys = true }
            val requestBody = json.encodeToString(BusinessBuyGoodsRequest.serializer(), buyGoodsRequest)

            val response = httpClient.post("$baseUrl/mpesa/b2b/v1/paymentrequest") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(requestBody)
            }

            val responseText = response.bodyAsText()
            println("📥 Business Buy Goods response: ${response.status} - $responseText")

            if (response.status == HttpStatusCode.OK) {
                val apiResponse = json.decodeFromString<BusinessBuyGoodsApiResponse>(responseText)

                if (apiResponse.ResponseCode == "0") {
                    // Store transaction record
                    val transactionId = storeBusinessBuyGoodsTransaction(
                        tillNumber = tillNumber,
                        amount = amount,
                        accountReference = accountReference,
                        remarks = remarks,
                        conversationId = apiResponse.ConversationID,
                        originatorConversationId = apiResponse.OriginatorConversationID
                    )

                    println("✅ Business Buy Goods initiated successfully")
                    BusinessBuyGoodsResponse(
                        success = true,
                        message = "Payment request accepted. Transaction is being processed.",
                        conversationId = apiResponse.ConversationID,
                        originatorConversationId = apiResponse.OriginatorConversationID,
                        responseCode = apiResponse.ResponseCode,
                        responseDescription = apiResponse.ResponseDescription,
                        transactionId = transactionId
                    )
                } else {
                    println("❌ Business Buy Goods failed: ${apiResponse.ResponseDescription}")
                    BusinessBuyGoodsResponse(
                        success = false,
                        message = "Payment request failed: ${apiResponse.ResponseDescription}",
                        responseCode = apiResponse.ResponseCode,
                        responseDescription = apiResponse.ResponseDescription
                    )
                }
            } else {
                println("❌ Business Buy Goods API failed: HTTP ${response.status.value}")
                BusinessBuyGoodsResponse(
                    success = false,
                    message = "API call failed: HTTP ${response.status.value}",
                    error = responseText
                )
            }
        } catch (e: Exception) {
            println("❌ Business Buy Goods exception: ${e.message}")
            e.printStackTrace()
            BusinessBuyGoodsResponse(
                success = false,
                message = "Failed to initiate payment: ${e.message}",
                error = e.message
            )
        }
    }

    /**
     * Store Business Buy Goods transaction in database
     */
    private suspend fun storeBusinessBuyGoodsTransaction(
        tillNumber: String,
        amount: Double,
        accountReference: String,
        remarks: String,
        conversationId: String,
        originatorConversationId: String
    ): String {
        val transactionId = "BBG_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}"

        try {
            newSuspendedTransaction(db = database) {
                MpesaTransactions.insert {
                    it[phoneNumber] = tillNumber
                    it[accountNumber] = accountReference
                    it[MpesaTransactions.amount] = BigDecimal.valueOf(amount)
                    it[description] = remarks
                    it[status] = "PENDING"
                    it[merchantRequestId] = originatorConversationId
                    it[checkoutRequestId] = conversationId
                    it[mpesaReceiptNumber] = transactionId
                }
            }
            println("💾 Business Buy Goods transaction stored: $transactionId")
        } catch (e: Exception) {
            println("⚠️ Failed to store transaction: ${e.message}")
        }

        return transactionId
    }

    /**
     * Handle Business Buy Goods callback
     */
    suspend fun handleBusinessBuyGoodsCallback(resultData: Map<String, Any>): Boolean {
        return try {
            println("📨 Processing Business Buy Goods/B2C callback")

            val result = resultData["Result"] as? Map<String, Any> ?: return false
            val resultCode = result["ResultCode"]?.toString() ?: return false
            val conversationId = result["ConversationID"]?.toString() ?: ""
            val transactionId = result["TransactionID"]?.toString()

            // Check if it's a B2C transaction (not Business Buy Goods)
            val isB2C = newSuspendedTransaction(db = database) {
                MpesaTransactions.select { MpesaTransactions.checkoutRequestId eq conversationId }
                    .singleOrNull()?.let { row ->
                        row[MpesaTransactions.description].contains("B2C", ignoreCase = true)
                    } ?: false
            }

            if (isB2C) {
                return handleB2CResult(result, resultCode, conversationId, transactionId)
            }

            when (resultCode) {
                "0" -> {
                    println("✅ Business Buy Goods completed successfully")
                    updateBusinessBuyGoodsStatus(conversationId, "COMPLETED", transactionId)
                    true
                }
                else -> {
                    val resultDesc = result["ResultDesc"]?.toString() ?: "Unknown error"
                    println("❌ Business Buy Goods failed: $resultDesc")
                    updateBusinessBuyGoodsStatus(conversationId, "FAILED", null)
                    true
                }
            }
        } catch (e: Exception) {
            println("❌ Error processing callback: ${e.message}")
            false
        }
    }

    /**
     * Handle B2C Withdrawal result
     */
    private suspend fun handleB2CResult(
        result: Map<String, Any>,
        resultCode: String,
        conversationId: String,
        mpesaReceipt: String?
    ): Boolean {
        return try {
            val resultDesc = result["ResultDesc"]?.toString() ?: ""
            
            if (resultCode == "0") {
                println("✅ B2C Withdrawal successful: $conversationId")
                
                newSuspendedTransaction(db = database) {
                    // Update M-Pesa transaction
                    MpesaTransactions.update({ MpesaTransactions.checkoutRequestId eq conversationId }) {
                        it[status] = "COMPLETED"
                        it[mpesaReceiptNumber] = mpesaReceipt
                        it[updatedAt] = Instant.now()
                    }
                    
                    // Update main transaction record
                    Transactions.update({ Transactions.reference eq conversationId }) {
                        it[status] = TransactionStatus.COMPLETED
                    }
                }
            } else {
                println("❌ B2C Withdrawal failed ($resultCode): $resultDesc")
                
                newSuspendedTransaction(db = database) {
                    val mpesaTxn = MpesaTransactions.select { MpesaTransactions.checkoutRequestId eq conversationId }.singleOrNull()
                    
                    if (mpesaTxn != null) {
                        val accountNumber = mpesaTxn[MpesaTransactions.accountNumber]
                        val amount = mpesaTxn[MpesaTransactions.amount]
                        
                        // Refund the customer since the withdrawal failed
                        val account = Accounts.select { Accounts.accountNumber eq accountNumber }.singleOrNull()
                        if (account != null) {
                            val currentBalance = account[Accounts.balance]
                            val newBalance = currentBalance + amount
                            
                            Accounts.update({ Accounts.id eq account[Accounts.id] }) {
                                it[balance] = newBalance
                                it[availableBalance] = newBalance
                            }
                            
                            // Create refund transaction
                            Transactions.insert {
                                it[Transactions.accountId] = account[Accounts.id].value
                                it[Transactions.type] = TransactionType.DEPOSIT
                                it[Transactions.amount] = amount
                                it[Transactions.status] = TransactionStatus.COMPLETED
                                it[Transactions.description] = "Refund: Failed Withdrawal ($resultDesc)"
                                it[Transactions.balanceAfter] = newBalance
                                it[Transactions.reference] = "REF-$conversationId"
                            }
                        }
                        
                        // Update M-Pesa transaction status
                        MpesaTransactions.update({ MpesaTransactions.checkoutRequestId eq conversationId }) {
                            it[status] = "FAILED"
                            it[MpesaTransactions.resultCode] = resultCode.toIntOrNull()
                            it[MpesaTransactions.resultDescription] = resultDesc
                            it[updatedAt] = Instant.now()
                        }
                        
                        // Update main transaction status
                        Transactions.update({ Transactions.reference eq conversationId }) {
                            it[status] = TransactionStatus.FAILED
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            println("❌ Error handling B2C result: ${e.message}")
            false
        }
    }

    /**
     * Update Business Buy Goods transaction status
     */
    private suspend fun updateBusinessBuyGoodsStatus(
        conversationId: String,
        status: String,
        transactionId: String?
    ) {
        try {
            newSuspendedTransaction(db = database) {
                MpesaTransactions.update({ MpesaTransactions.checkoutRequestId eq conversationId }) {
                    it[MpesaTransactions.status] = status
                    if (transactionId != null) {
                        it[MpesaTransactions.mpesaReceiptNumber] = transactionId
                    }
                }
            }
        } catch (e: Exception) {
            println("⚠️ Failed to update status: ${e.message}")
        }
    }
}