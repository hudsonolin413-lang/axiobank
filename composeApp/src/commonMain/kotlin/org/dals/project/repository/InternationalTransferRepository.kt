package org.dals.project.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class InternationalTransferQuote(
    val fromAmount: Double,
    val fromCurrency: String,
    val toAmount: Double,
    val toCurrency: String,
    val exchangeRate: Double,
    val transferFee: Double,
    val intermediaryFee: Double,
    val totalCost: Double,
    val estimatedDeliveryDays: Int
)

@Serializable
data class CreateInternationalTransferRequest(
    val customerId: String,
    val accountId: String,
    val recipientName: String,
    val recipientBank: String,
    val recipientSwift: String? = null,
    val recipientIban: String? = null,
    val recipientCountry: String,
    val amount: Double,
    val currency: String,
    val purpose: String
)

@Serializable
data class InternationalTransferDto(
    val id: String,
    val referenceNumber: String,
    val amount: Double,
    val currency: String,
    val status: String,
    val recipientName: String,
    val createdAt: String,
    val estimatedDelivery: String
)

class InternationalTransferRepository(private val client: HttpClient, private val baseUrl: String) {

    suspend fun getQuote(
        amount: Double,
        fromCurrency: String,
        toCurrency: String,
        country: String
    ): Result<InternationalTransferQuote> {
        return try {
            val response = client.get("$baseUrl/api/v1/international-transfer/quote") {
                parameter("amount", amount)
                parameter("fromCurrency", fromCurrency)
                parameter("toCurrency", toCurrency)
                parameter("country", country)
            }

            if (response.status.isSuccess()) {
                Result.success(response.body<InternationalTransferQuote>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to get quote"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTransfer(request: CreateInternationalTransferRequest): Result<InternationalTransferDto> {
        return try {
            val response = client.post("$baseUrl/api/v1/international-transfer/create") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (response.status.isSuccess()) {
                Result.success(response.body<InternationalTransferDto>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to create transfer"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCustomerTransfers(customerId: String): Result<List<InternationalTransferDto>> {
        return try {
            val response = client.get("$baseUrl/api/v1/international-transfer/customer/$customerId")

            if (response.status.isSuccess()) {
                Result.success(response.body<List<InternationalTransferDto>>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to get transfers"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
