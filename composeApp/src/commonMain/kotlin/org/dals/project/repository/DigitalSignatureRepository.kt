package org.dals.project.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import org.dals.project.API_BASE_URL

@Serializable
data class CreateSignatureRequestDto(
    val customerId: String,
    val documentId: String,
    val documentType: String,
    val documentName: String,
    val signatureData: String,
    val ipAddress: String? = null,
    val deviceInfo: String? = null
)

@Serializable
data class DigitalSignatureDto(
    val id: String,
    val customerId: String,
    val documentId: String,
    val documentType: String,
    val documentName: String,
    val signatureData: String,
    val signedAt: String,
    val ipAddress: String?,
    val deviceInfo: String?,
    val isValid: Boolean,
    val verificationHash: String
)

class DigitalSignatureRepository(private val httpClient: HttpClient) {
    suspend fun createSignature(request: CreateSignatureRequestDto): Result<DigitalSignatureDto> {
        return try {
            val response = httpClient.post("$API_BASE_URL/digital-signature/create") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (response.status.isSuccess()) {
                val data = response.body<DigitalSignatureDto>()
                println("✅ Signature created: ${data.id}")
                Result.success(data)
            } else {
                val error = response.body<String>()
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            println("❌ Error creating signature: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getCustomerSignatures(customerId: String): Result<List<DigitalSignatureDto>> {
        return try {
            val response = httpClient.get("$API_BASE_URL/digital-signature/customer/$customerId")

            if (response.status.isSuccess()) {
                val data = response.body<List<DigitalSignatureDto>>()
                Result.success(data)
            } else {
                val error = response.body<String>()
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            println("❌ Error fetching signatures: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun verifySignature(signatureId: String): Result<Boolean> {
        return try {
            val response = httpClient.get("$API_BASE_URL/digital-signature/verify/$signatureId")

            if (response.status.isSuccess()) {
                val data = response.body<Map<String, Boolean>>()
                Result.success(data["isValid"] ?: false)
            } else {
                Result.failure(Exception("Failed to verify signature"))
            }
        } catch (e: Exception) {
            println("❌ Error verifying signature: ${e.message}")
            Result.failure(e)
        }
    }
}
