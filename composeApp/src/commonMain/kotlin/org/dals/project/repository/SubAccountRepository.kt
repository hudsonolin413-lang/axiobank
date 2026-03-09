package org.dals.project.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class SubAccountRecord(
    val id: String,
    val customerId: String,
    val parentAccountId: String,
    val name: String,
    val description: String?,
    val targetAmount: String?,
    val currentBalance: String,
    val iconName: String,
    val colorHex: String,
    val targetDate: String?,
    val isLocked: Boolean,
    val autoTransferAmount: String?,
    val autoTransferFrequency: String?,
    val isActive: Boolean,
    val progressPercentage: Double,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateSubAccountRequest(
    val customerId: String,
    val parentAccountId: String,
    val name: String,
    val description: String? = null,
    val targetAmount: String? = null,
    val iconName: String = "Savings",
    val colorHex: String = "0xFF2196F3",
    val targetDate: String? = null,
    val autoTransferAmount: String? = null,
    val autoTransferFrequency: String? = null
)

@Serializable
data class UpdateSubAccountRequest(
    val name: String? = null,
    val description: String? = null,
    val targetAmount: String? = null,
    val iconName: String? = null,
    val colorHex: String? = null,
    val targetDate: String? = null,
    val isLocked: Boolean? = null,
    val autoTransferAmount: String? = null,
    val autoTransferFrequency: String? = null,
    val isActive: Boolean? = null
)

@Serializable
data class TransferToSubAccountRequest(
    val subAccountId: String,
    val amount: String,
    val description: String? = null,
    val isDirectDeposit: Boolean = false
)

@Serializable
data class TransferResponse(
    val success: Boolean,
    val message: String,
    val subAccount: SubAccountRecord,
    val newParentBalance: String
)

class SubAccountRepository(private val httpClient: HttpClient, private val baseUrl: String) {

    suspend fun getAllSubAccounts(customerId: String): Result<List<SubAccountRecord>> = try {
        val response = httpClient.get("$baseUrl/$customerId")
        if (response.status == HttpStatusCode.OK) {
            Result.success(response.body<List<SubAccountRecord>>())
        } else {
            Result.failure(Exception("Failed to fetch sub-accounts: ${response.status}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getActiveSubAccounts(customerId: String): Result<List<SubAccountRecord>> = try {
        val response = httpClient.get("$baseUrl/$customerId/active")
        if (response.status == HttpStatusCode.OK) {
            Result.success(response.body<List<SubAccountRecord>>())
        } else {
            Result.failure(Exception("Failed to fetch active sub-accounts: ${response.status}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getSubAccountById(subAccountId: String): Result<SubAccountRecord> = try {
        val response = httpClient.get("$baseUrl/detail/$subAccountId")
        if (response.status == HttpStatusCode.OK) {
            Result.success(response.body<SubAccountRecord>())
        } else {
            Result.failure(Exception("Sub-account not found: ${response.status}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun createSubAccount(request: CreateSubAccountRequest): Result<SubAccountRecord> = try {
        val response = httpClient.post(baseUrl) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status == HttpStatusCode.Created) {
            Result.success(response.body<SubAccountRecord>())
        } else {
            Result.failure(Exception("Failed to create sub-account: ${response.status}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateSubAccount(subAccountId: String, request: UpdateSubAccountRequest): Result<SubAccountRecord> = try {
        val response = httpClient.put("$baseUrl/$subAccountId") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status == HttpStatusCode.OK) {
            Result.success(response.body<SubAccountRecord>())
        } else {
            Result.failure(Exception("Failed to update sub-account: ${response.status}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteSubAccount(subAccountId: String): Result<Boolean> = try {
        val response = httpClient.delete("$baseUrl/$subAccountId")
        if (response.status == HttpStatusCode.OK) {
            Result.success(true)
        } else {
            Result.failure(Exception("Failed to delete sub-account: ${response.status}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun transferToSubAccount(request: TransferToSubAccountRequest): Result<TransferResponse> = try {
        val response = httpClient.post("$baseUrl/transfer") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status == HttpStatusCode.OK) {
            Result.success(response.body<TransferResponse>())
        } else {
            Result.failure(Exception("Transfer failed: ${response.status}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun withdrawFromSubAccount(request: TransferToSubAccountRequest): Result<TransferResponse> = try {
        val response = httpClient.post("$baseUrl/withdraw") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status == HttpStatusCode.OK) {
            Result.success(response.body<TransferResponse>())
        } else {
            Result.failure(Exception("Withdrawal failed: ${response.status}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun toggleLock(subAccountId: String): Result<SubAccountRecord> = try {
        val response = httpClient.post("$baseUrl/$subAccountId/toggle-lock")
        if (response.status == HttpStatusCode.OK) {
            Result.success(response.body<SubAccountRecord>())
        } else {
            Result.failure(Exception("Failed to toggle lock: ${response.status}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
