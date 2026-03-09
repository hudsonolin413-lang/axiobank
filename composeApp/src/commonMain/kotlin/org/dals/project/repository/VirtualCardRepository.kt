package org.dals.project.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class VirtualCardDto(
    val id: String,
    val customerId: String,
    val accountId: String,
    val cardNumber: String,
    val cardHolderName: String,
    val expiryDate: String,
    val cvv: String,
    val cardType: String,
    val status: String,
    val spendingLimit: Double,
    val usedAmount: Double,
    val createdAt: String
)

@Serializable
data class CreateVirtualCardRequest(
    val customerId: String,
    val accountId: String,
    val cardType: String,
    val spendingLimit: Double
)

class VirtualCardRepository(private val client: HttpClient, private val baseUrl: String) {

    suspend fun getVirtualCards(customerId: String): Result<List<VirtualCardDto>> {
        return try {
            val response = client.get("$baseUrl/api/v1/virtual-card/customer/$customerId")
            if (response.status.isSuccess()) {
                Result.success(response.body<List<VirtualCardDto>>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to get virtual cards"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createVirtualCard(request: CreateVirtualCardRequest): Result<VirtualCardDto> {
        return try {
            val response = client.post("$baseUrl/api/v1/virtual-card/create") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<VirtualCardDto>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to create virtual card"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStatus(cardId: String, status: String): Result<VirtualCardDto> {
        return try {
            val response = client.post("$baseUrl/api/v1/virtual-card/status/$cardId") {
                parameter("status", status)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<VirtualCardDto>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to update status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
