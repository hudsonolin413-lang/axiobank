package org.dals.project.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class EnrollOverdraftProtectionRequest(
    val customerId: String,
    val accountId: String,
    val protectionType: String,
    val linkedAccountId: String? = null,
    val creditLimit: Double? = null,
    val transferFee: Double = 0.0,
    val interestRate: Double? = null,
    val autoTransfer: Boolean = true,
    val dailyLimit: Double? = null
)

@Serializable
data class OverdraftProtectionDto(
    val id: String,
    val customerId: String,
    val accountId: String,
    val protectionType: String,
    val linkedAccountId: String?,
    val creditLimit: Double?,
    val usedAmount: Double,
    val availableAmount: Double?,
    val transferFee: Double,
    val interestRate: Double?,
    val status: String,
    val autoTransfer: Boolean,
    val dailyLimit: Double?,
    val lastUsedDate: String?,
    val enrolledAt: String,
    val cancelledAt: String?
)

@Serializable
data class OverdraftTransactionDto(
    val id: String,
    val overdraftProtectionId: String,
    val accountId: String,
    val linkedAccountId: String?,
    val transactionId: String,
    val amount: Double,
    val fee: Double,
    val totalAmount: Double,
    val balanceBefore: Double,
    val balanceAfter: Double,
    val protectionType: String,
    val status: String,
    val failureReason: String?,
    val createdAt: String
)

@Serializable
data class OverdraftUsageStatsDto(
    val totalProtections: Int,
    val activeProtections: Int,
    val totalUsedAmount: Double,
    val totalAvailableAmount: Double,
    val recentTransactions: List<OverdraftTransactionDto>
)

class OverdraftProtectionRepository(private val client: HttpClient, private val baseUrl: String) {

    suspend fun enrollOverdraftProtection(request: EnrollOverdraftProtectionRequest): Result<OverdraftProtectionDto> {
        return try {
            val response = client.post("$baseUrl/api/v1/overdraft-protection/enroll") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (response.status.isSuccess()) {
                Result.success(response.body<OverdraftProtectionDto>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to enroll in overdraft protection"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOverdraftProtection(accountId: String): Result<OverdraftProtectionDto?> {
        return try {
            val response = client.get("$baseUrl/api/v1/overdraft-protection/account/$accountId")

            if (response.status.isSuccess()) {
                Result.success(response.body<OverdraftProtectionDto>())
            } else if (response.status == HttpStatusCode.NotFound) {
                Result.success(null)
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to get overdraft protection"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllOverdraftProtections(customerId: String): Result<List<OverdraftProtectionDto>> {
        return try {
            val response = client.get("$baseUrl/api/v1/overdraft-protection/customer/$customerId")

            if (response.status.isSuccess()) {
                Result.success(response.body<List<OverdraftProtectionDto>>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to get overdraft protections"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOverdraftTransactions(accountId: String, limit: Int = 50): Result<List<OverdraftTransactionDto>> {
        return try {
            val response = client.get("$baseUrl/api/v1/overdraft-protection/transactions/$accountId") {
                parameter("limit", limit)
            }

            if (response.status.isSuccess()) {
                Result.success(response.body<List<OverdraftTransactionDto>>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to get transactions"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOverdraftUsageStats(customerId: String): Result<OverdraftUsageStatsDto> {
        return try {
            val response = client.get("$baseUrl/api/v1/overdraft-protection/stats/$customerId")

            if (response.status.isSuccess()) {
                Result.success(response.body<OverdraftUsageStatsDto>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to get usage stats"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelOverdraftProtection(protectionId: String): Result<OverdraftProtectionDto> {
        return try {
            val response = client.post("$baseUrl/api/v1/overdraft-protection/cancel/$protectionId") {
                contentType(ContentType.Application.Json)
            }

            if (response.status.isSuccess()) {
                Result.success(response.body<OverdraftProtectionDto>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to cancel overdraft protection"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
