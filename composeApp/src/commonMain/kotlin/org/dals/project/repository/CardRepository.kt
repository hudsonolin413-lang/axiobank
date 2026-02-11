package org.dals.project.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.dals.project.model.*

@Serializable
data class CardResponse(
    val success: Boolean,
    val message: String,
    val data: Card? = null,
    val cards: List<Card>? = null,
    val error: String? = null
)

@Serializable
data class CardTransactionResult(
    val success: Boolean,
    val message: String,
    val transactionId: String? = null,
    val reference: String? = null,
    val amount: Double? = null,
    val fee: Double? = null,
    val newBalance: Double? = null,
    val timestamp: String? = null
)

class CardRepository(
    private val authRepository: AuthRepository
) {
    private val baseUrl = "http://localhost:8081/api/v1"

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    private val _cards = MutableStateFlow<List<Card>>(emptyList())
    val cards: StateFlow<List<Card>> = _cards.asStateFlow()

    private val _defaultCard = MutableStateFlow<Card?>(null)
    val defaultCard: StateFlow<Card?> = _defaultCard.asStateFlow()

    /**
     * Fetch all cards for the current user
     */
    suspend fun fetchCards(): Result<List<Card>> {
        return try {
            val currentUser = authRepository.currentUser.value
                ?: return Result.failure(Exception("User not authenticated"))

            println("🌐 Fetching cards for user: ${currentUser.id}")

            val response = httpClient.get("$baseUrl/cards/user/${currentUser.id}") {
                contentType(ContentType.Application.Json)
                headers {
                    authRepository.getAuthToken()?.let { token ->
                        append("Authorization", "Bearer $token")
                    }
                }
            }

            if (response.status == HttpStatusCode.OK) {
                println("📥 Response received with status OK")
                val cardResponse = response.body<CardResponse>()
                println("📦 CardResponse: success=${cardResponse.success}, message=${cardResponse.message}")
                val userCards = cardResponse.cards ?: emptyList()
                println("📊 Cards in response: ${userCards.size}")

                userCards.forEachIndexed { index, card ->
                    println("   Card $index: ${card.lastFourDigits} - ${card.cardBrand} - Active: ${card.isActive}")
                }

                _cards.value = userCards
                _defaultCard.value = userCards.firstOrNull { it.isDefault }

                println("✅ Fetched ${userCards.size} cards")
                Result.success(userCards)
            } else {
                println("❌ Failed to fetch cards: ${response.status}")
                Result.failure(Exception("Failed to fetch cards: ${response.status}"))
            }
        } catch (e: Exception) {
            println("❌ Error fetching cards: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Add a new card
     */
    suspend fun addCard(cardRequest: CardRequest): Result<Card> {
        return try {
            val currentUser = authRepository.currentUser.value
                ?: return Result.failure(Exception("User not authenticated"))

            println("🌐 Adding new card for user: ${currentUser.id}")

            // Transform CardRequest to AddCardRequest with userId
            @Serializable
            data class AddCardRequest(
                val userId: String,
                val cardNumber: String,
                val cardHolderName: String,
                val expiryMonth: Int,
                val expiryYear: Int,
                val cvv: String,
                val cardType: String,
                val nickname: String? = null
            )

            val addCardRequest = AddCardRequest(
                userId = currentUser.id,
                cardNumber = cardRequest.cardNumber,
                cardHolderName = cardRequest.cardHolderName,
                expiryMonth = cardRequest.expiryMonth,
                expiryYear = cardRequest.expiryYear,
                cvv = cardRequest.cvv,
                cardType = cardRequest.cardType.name,
                nickname = cardRequest.nickname
            )

            val response = httpClient.post("$baseUrl/cards") {
                contentType(ContentType.Application.Json)
                headers {
                    authRepository.getAuthToken()?.let { token ->
                        append("Authorization", "Bearer $token")
                    }
                }
                setBody(addCardRequest)
            }

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                val cardResponse = response.body<CardResponse>()
                val newCard = cardResponse.data
                    ?: return Result.failure(Exception("Failed to add card"))

                // Refresh cards list
                fetchCards()

                println("✅ Card added successfully")
                Result.success(newCard)
            } else {
                val errorResponse = try {
                    response.body<CardResponse>()
                } catch (e: Exception) {
                    null
                }
                val errorMessage = errorResponse?.error ?: errorResponse?.message ?: "Failed to add card"
                println("❌ Failed to add card: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            println("❌ Error adding card: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Remove a card
     */
    suspend fun removeCard(cardId: String): Result<Unit> {
        return try {
            val currentUser = authRepository.currentUser.value
                ?: return Result.failure(Exception("User not authenticated"))

            println("🌐 Removing card: $cardId")

            val response = httpClient.delete("$baseUrl/cards/$cardId") {
                contentType(ContentType.Application.Json)
                headers {
                    authRepository.getAuthToken()?.let { token ->
                        append("Authorization", "Bearer $token")
                    }
                }
            }

            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.NoContent) {
                // Refresh cards list
                fetchCards()

                println("✅ Card removed successfully")
                Result.success(Unit)
            } else {
                println("❌ Failed to remove card: ${response.status}")
                Result.failure(Exception("Failed to remove card: ${response.status}"))
            }
        } catch (e: Exception) {
            println("❌ Error removing card: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Set a card as default
     */
    suspend fun setDefaultCard(cardId: String): Result<Unit> {
        return try {
            val currentUser = authRepository.currentUser.value
                ?: return Result.failure(Exception("User not authenticated"))

            println("🌐 Setting default card: $cardId")

            val response = httpClient.put("$baseUrl/cards/$cardId/default") {
                contentType(ContentType.Application.Json)
                headers {
                    authRepository.getAuthToken()?.let { token ->
                        append("Authorization", "Bearer $token")
                    }
                }
            }

            if (response.status == HttpStatusCode.OK) {
                // Refresh cards list
                fetchCards()

                println("✅ Default card set successfully")
                Result.success(Unit)
            } else {
                println("❌ Failed to set default card: ${response.status}")
                Result.failure(Exception("Failed to set default card: ${response.status}"))
            }
        } catch (e: Exception) {
            println("❌ Error setting default card: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Verify a card with verification code
     */
    suspend fun verifyCard(cardId: String, verificationCode: String): Result<Unit> {
        return try {
            val currentUser = authRepository.currentUser.value
                ?: return Result.failure(Exception("User not authenticated"))

            println("🌐 Verifying card: $cardId")

            val verificationRequest = CardVerificationRequest(
                cardId = cardId,
                verificationCode = verificationCode
            )

            val response = httpClient.post("$baseUrl/cards/verify") {
                contentType(ContentType.Application.Json)
                headers {
                    authRepository.getAuthToken()?.let { token ->
                        append("Authorization", "Bearer $token")
                    }
                }
                setBody(verificationRequest)
            }

            if (response.status == HttpStatusCode.OK) {
                // Refresh cards list
                fetchCards()

                println("✅ Card verified successfully")
                Result.success(Unit)
            } else {
                println("❌ Failed to verify card: ${response.status}")
                Result.failure(Exception("Failed to verify card: ${response.status}"))
            }
        } catch (e: Exception) {
            println("❌ Error verifying card: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Process online payment
     */
    suspend fun processOnlinePayment(
        cardId: String,
        amount: Double,
        merchantName: String,
        cvv: String,
        category: String? = null
    ): Result<CardTransactionResult> {
        return try {
            @Serializable
            data class OnlinePaymentRequest(
                val cardId: String,
                val amount: Double,
                val merchantName: String,
                val cvv: String,
                val currency: String = "USD",
                val category: String? = null
            )

            val response = httpClient.post("$baseUrl/card-transactions/online-payment") {
                contentType(ContentType.Application.Json)
                setBody(OnlinePaymentRequest(cardId, amount, merchantName, cvv, "USD", category))
            }

            if (response.status == HttpStatusCode.OK) {
                val result = response.body<CardTransactionResult>()
                Result.success(result)
            } else {
                Result.failure(Exception("Payment failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Process POS transaction
     */
    suspend fun processPOSTransaction(
        cardId: String,
        amount: Double,
        merchantName: String,
        pin: String
    ): Result<CardTransactionResult> {
        return try {
            @Serializable
            data class POSTransactionRequest(
                val cardId: String,
                val amount: Double,
                val merchantName: String,
                val pin: String,
                val currency: String = "USD"
            )

            val response = httpClient.post("$baseUrl/card-transactions/pos-transaction") {
                contentType(ContentType.Application.Json)
                setBody(POSTransactionRequest(cardId, amount, merchantName, pin))
            }

            if (response.status == HttpStatusCode.OK) {
                val result = response.body<CardTransactionResult>()
                Result.success(result)
            } else {
                Result.failure(Exception("Transaction failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Process bill payment
     */
    suspend fun processBillPayment(
        cardId: String,
        amount: Double,
        billType: String,
        billerName: String,
        accountNumber: String,
        cvv: String
    ): Result<CardTransactionResult> {
        return try {
            @Serializable
            data class BillPaymentRequest(
                val cardId: String,
                val amount: Double,
                val billType: String,
                val billerName: String,
                val accountNumber: String,
                val cvv: String,
                val currency: String = "USD"
            )

            val response = httpClient.post("$baseUrl/card-transactions/bill-payment") {
                contentType(ContentType.Application.Json)
                setBody(BillPaymentRequest(cardId, amount, billType, billerName, accountNumber, cvv))
            }

            if (response.status == HttpStatusCode.OK) {
                val result = response.body<CardTransactionResult>()
                Result.success(result)
            } else {
                Result.failure(Exception("Bill payment failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Process card transfer
     */
    suspend fun processCardTransfer(
        cardId: String,
        amount: Double,
        destinationAccountNumber: String,
        description: String,
        cvv: String
    ): Result<CardTransactionResult> {
        return try {
            @Serializable
            data class CardTransferRequest(
                val cardId: String,
                val amount: Double,
                val destinationAccountNumber: String,
                val description: String,
                val cvv: String,
                val currency: String = "USD"
            )

            val response = httpClient.post("$baseUrl/card-transactions/transfer") {
                contentType(ContentType.Application.Json)
                setBody(CardTransferRequest(cardId, amount, destinationAccountNumber, description, cvv))
            }

            if (response.status == HttpStatusCode.OK) {
                val result = response.body<CardTransactionResult>()
                Result.success(result)
            } else {
                Result.failure(Exception("Transfer failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Process ATM withdrawal
     */
    suspend fun processATMWithdrawal(
        cardId: String,
        amount: Double,
        atmLocation: String,
        pin: String
    ): Result<CardTransactionResult> {
        return try {
            @Serializable
            data class ATMWithdrawalRequest(
                val cardId: String,
                val amount: Double,
                val atmLocation: String,
                val pin: String,
                val currency: String = "USD"
            )

            val response = httpClient.post("$baseUrl/card-transactions/atm-withdrawal") {
                contentType(ContentType.Application.Json)
                setBody(ATMWithdrawalRequest(cardId, amount, atmLocation, pin))
            }

            if (response.status == HttpStatusCode.OK) {
                val result = response.body<CardTransactionResult>()
                Result.success(result)
            } else {
                Result.failure(Exception("Withdrawal failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Make a payment using a card
     */
    suspend fun makeCardPayment(paymentRequest: CardPaymentRequest): Result<String> {
        return try {
            val currentUser = authRepository.currentUser.value
                ?: return Result.failure(Exception("User not authenticated"))

            println("🌐 Processing card payment")

            val response = httpClient.post("$baseUrl/cards/payment") {
                contentType(ContentType.Application.Json)
                headers {
                    authRepository.getAuthToken()?.let { token ->
                        append("Authorization", "Bearer $token")
                    }
                }
                setBody(paymentRequest)
            }

            if (response.status == HttpStatusCode.OK) {
                @Serializable
                data class PaymentResponse(
                    val success: Boolean,
                    val message: String,
                    val transactionId: String? = null
                )

                val paymentResponse = response.body<PaymentResponse>()
                println("✅ Card payment successful")
                Result.success(paymentResponse.transactionId ?: "Payment successful")
            } else {
                val errorResponse = try {
                    response.body<CardResponse>()
                } catch (e: Exception) {
                    null
                }
                val errorMessage = errorResponse?.error ?: errorResponse?.message ?: "Payment failed"
                println("❌ Card payment failed: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            println("❌ Error processing card payment: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Clear all cards data (on logout)
     */
    fun clearCards() {
        _cards.value = emptyList()
        _defaultCard.value = null
    }

    /**
     * Get card by ID
     */
    fun getCardById(cardId: String): Card? {
        return _cards.value.find { it.id == cardId }
    }
}
