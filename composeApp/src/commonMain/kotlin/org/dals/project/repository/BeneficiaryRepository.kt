package org.dals.project.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class BeneficiaryRecord(
    val id: String,
    val customerId: String,
    val name: String,
    val nickname: String?,
    val accountNumber: String?,
    val bankName: String?,
    val bankCode: String?,
    val phoneNumber: String?,
    val email: String?,
    val type: String,
    val isFavorite: Boolean,
    val lastUsed: String?,
    val transferCount: Int,
    val isVerified: Boolean,
    val isActive: Boolean,
    val createdAt: String
)

@Serializable
data class CreateBeneficiaryRequest(
    val customerId: String,
    val name: String,
    val nickname: String? = null,
    val accountNumber: String? = null,
    val bankName: String? = null,
    val bankCode: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val type: String = "BANK"
)

@Serializable
data class UpdateBeneficiaryRequest(
    val name: String? = null,
    val nickname: String? = null,
    val accountNumber: String? = null,
    val bankName: String? = null,
    val bankCode: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val isFavorite: Boolean? = null,
    val isActive: Boolean? = null
)

class BeneficiaryRepository(private val httpClient: HttpClient, private val apiBaseUrl: String) {
    private val baseUrl = "$apiBaseUrl/beneficiaries"

    suspend fun getAllBeneficiaries(customerId: String): Result<List<BeneficiaryRecord>> = try {
        val url = "$baseUrl/$customerId"
        println("🌐 Fetching beneficiaries from: $url")
        val response = httpClient.get(url)
        println("📡 Response status: ${response.status}")
        if (response.status == HttpStatusCode.OK) {
            Result.success(response.body<List<BeneficiaryRecord>>())
        } else {
            val errorMsg = "Failed to fetch beneficiaries from $url - Status: ${response.status}"
            println("❌ $errorMsg")
            Result.failure(Exception(errorMsg))
        }
    } catch (e: Exception) {
        println("❌ Exception fetching beneficiaries: ${e.message}")
        e.printStackTrace()
        Result.failure(Exception("Error fetching beneficiaries: ${e.message}", e))
    }

    suspend fun getFavoriteBeneficiaries(customerId: String): Result<List<BeneficiaryRecord>> = try {
        val response = httpClient.get("$baseUrl/$customerId/favorites")
        if (response.status == HttpStatusCode.OK) {
            Result.success(response.body<List<BeneficiaryRecord>>())
        } else {
            Result.failure(Exception("Failed to fetch favorites: ${response.status}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun searchBeneficiaries(customerId: String, query: String): Result<List<BeneficiaryRecord>> = try {
        val response = httpClient.get("$baseUrl/$customerId/search") {
            parameter("q", query)
        }
        if (response.status == HttpStatusCode.OK) {
            Result.success(response.body<List<BeneficiaryRecord>>())
        } else {
            Result.failure(Exception("Search failed: ${response.status}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getBeneficiaryById(beneficiaryId: String): Result<BeneficiaryRecord> = try {
        val response = httpClient.get("$baseUrl/detail/$beneficiaryId")
        if (response.status == HttpStatusCode.OK) {
            Result.success(response.body<BeneficiaryRecord>())
        } else {
            Result.failure(Exception("Beneficiary not found: ${response.status}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun createBeneficiary(request: CreateBeneficiaryRequest): Result<BeneficiaryRecord> = try {
        val response = httpClient.post(baseUrl) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status == HttpStatusCode.Created) {
            Result.success(response.body<BeneficiaryRecord>())
        } else {
            Result.failure(Exception("Failed to create beneficiary: ${response.status}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateBeneficiary(beneficiaryId: String, request: UpdateBeneficiaryRequest): Result<BeneficiaryRecord> = try {
        val response = httpClient.put("$baseUrl/$beneficiaryId") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status == HttpStatusCode.OK) {
            Result.success(response.body<BeneficiaryRecord>())
        } else {
            Result.failure(Exception("Failed to update beneficiary: ${response.status}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun toggleFavorite(beneficiaryId: String): Result<BeneficiaryRecord> = try {
        val response = httpClient.post("$baseUrl/$beneficiaryId/toggle-favorite")
        if (response.status == HttpStatusCode.OK) {
            Result.success(response.body<BeneficiaryRecord>())
        } else {
            Result.failure(Exception("Failed to toggle favorite: ${response.status}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteBeneficiary(beneficiaryId: String): Result<Boolean> = try {
        val response = httpClient.delete("$baseUrl/$beneficiaryId")
        if (response.status == HttpStatusCode.OK) {
            Result.success(true)
        } else {
            Result.failure(Exception("Failed to delete beneficiary: ${response.status}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun recordUsage(beneficiaryId: String): Result<Boolean> = try {
        val response = httpClient.post("$baseUrl/$beneficiaryId/record-usage")
        if (response.status == HttpStatusCode.OK) {
            Result.success(true)
        } else {
            Result.failure(Exception("Failed to record usage: ${response.status}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
