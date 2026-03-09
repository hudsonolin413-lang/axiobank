package org.dals.project.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class InitiateNfcPaymentRequest(
    val customerId: String,
    val fromAccountId: String,
    val merchantName: String,
    val merchantId: String?,
    val amount: Double,
    val currency: String = "USD",
    val deviceId: String?,
    val nfcTagId: String?
)

@Serializable
data class NfcPaymentResponse(
    val id: String,
    val customerId: String,
    val fromAccountId: String,
    val merchantName: String,
    val merchantId: String?,
    val amount: Double,
    val currency: String,
    val transactionId: String?,
    val deviceId: String?,
    val nfcTagId: String?,
    val status: String,
    val paymentMethod: String,
    val authorizationCode: String?,
    val failureReason: String?,
    val initiatedAt: String,
    val completedAt: String?,
    val createdAt: String
)

@Serializable
data class NfcPaymentHistoryDto(
    val payments: List<NfcPaymentResponse>,
    val totalCount: Int,
    val totalAmount: Double
)

class NfcPaymentRepository(private val client: HttpClient, private val baseUrl: String) {

    suspend fun initiateNfcPayment(request: InitiateNfcPaymentRequest): Result<NfcPaymentResponse> {
        return try {
            val url = "$baseUrl/nfc-payment/initiate"
            println("🌐 Initiating NFC payment at: $url")
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            println("📡 Response status: ${response.status}")

            if (response.status.isSuccess()) {
                Result.success(response.body<NfcPaymentResponse>())
            } else {
                val errorMsg = "Failed to initiate NFC payment from $url - Status: ${response.status}"
                println("❌ $errorMsg")
                try {
                    val errorBody = response.body<Map<String, String>>()
                    Result.failure(Exception(errorBody["error"] ?: errorMsg))
                } catch (e: Exception) {
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception initiating NFC payment: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Error initiating NFC payment: ${e.message}", e))
        }
    }

    suspend fun processNfcPayment(nfcPaymentId: String): Result<NfcPaymentResponse> {
        return try {
            val response = client.post("$baseUrl/nfc-payment/process/$nfcPaymentId") {
                contentType(ContentType.Application.Json)
            }

            if (response.status.isSuccess()) {
                Result.success(response.body<NfcPaymentResponse>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to process NFC payment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNfcPaymentById(nfcPaymentId: String): Result<NfcPaymentResponse> {
        return try {
            val response = client.get("$baseUrl/nfc-payment/$nfcPaymentId")

            if (response.status.isSuccess()) {
                Result.success(response.body<NfcPaymentResponse>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "NFC payment not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNfcPaymentHistory(
        customerId: String,
        limit: Int = 50,
        offset: Int = 0
    ): Result<NfcPaymentHistoryDto> {
        return try {
            val response = client.get("$baseUrl/nfc-payment/history/$customerId") {
                parameter("limit", limit)
                parameter("offset", offset)
            }

            if (response.status.isSuccess()) {
                Result.success(response.body<NfcPaymentHistoryDto>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to get payment history"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelNfcPayment(nfcPaymentId: String): Result<NfcPaymentResponse> {
        return try {
            val response = client.post("$baseUrl/nfc-payment/cancel/$nfcPaymentId") {
                contentType(ContentType.Application.Json)
            }

            if (response.status.isSuccess()) {
                Result.success(response.body<NfcPaymentResponse>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to cancel NFC payment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveNfcPayments(customerId: String): Result<List<NfcPaymentResponse>> {
        return try {
            val response = client.get("$baseUrl/nfc-payment/active/$customerId")

            if (response.status.isSuccess()) {
                Result.success(response.body<List<NfcPaymentResponse>>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to get active payments"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
