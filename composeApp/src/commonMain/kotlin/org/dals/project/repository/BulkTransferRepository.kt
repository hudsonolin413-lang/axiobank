package org.dals.project.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import org.dals.project.API_BASE_URL

@Serializable
data class BulkTransferRecipientDto(
    val name: String,
    val phoneNumber: String,
    val accountNumber: String? = null,
    val amount: Double,
    val description: String? = null
)

@Serializable
data class BulkTransferRequestDto(
    val customerId: String,
    val fromAccountId: String,
    val batchName: String,
    val recipients: List<BulkTransferRecipientDto>,
    val description: String? = null
)

@Serializable
data class BulkTransferResponseDto(
    val bulkTransferId: String,
    val batchName: String,
    val totalRecipients: Int,
    val totalAmount: Double,
    val status: String,
    val message: String
)

@Serializable
data class RecipientStatusDto(
    val recipientName: String,
    val recipientPhone: String,
    val amount: Double,
    val status: String,
    val failureReason: String? = null
)

@Serializable
data class BulkTransferStatusDto(
    val bulkTransferId: String,
    val batchName: String,
    val totalRecipients: Int,
    val totalAmount: Double,
    val completedTransfers: Int,
    val failedTransfers: Int,
    val status: String,
    val initiatedAt: String,
    val completedAt: String?,
    val recipients: List<RecipientStatusDto>
)

class BulkTransferRepository(private val httpClient: HttpClient) {

    suspend fun createBulkTransfer(request: BulkTransferRequestDto): Result<BulkTransferResponseDto> {
        return try {
            val response = httpClient.post("$API_BASE_URL/bulk-transfer/create") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (response.status.isSuccess()) {
                val data = response.body<BulkTransferResponseDto>()
                println("✅ Bulk transfer created: ${data.bulkTransferId}")
                Result.success(data)
            } else {
                val error = response.body<String>()
                println("❌ Failed to create bulk transfer: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            println("❌ Error creating bulk transfer: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun processBulkTransfer(bulkTransferId: String): Result<BulkTransferStatusDto> {
        return try {
            val response = httpClient.post("$API_BASE_URL/bulk-transfer/process/$bulkTransferId")

            if (response.status.isSuccess()) {
                val data = response.body<BulkTransferStatusDto>()
                println("✅ Bulk transfer processed: ${data.completedTransfers} completed, ${data.failedTransfers} failed")
                Result.success(data)
            } else {
                val error = response.body<String>()
                println("❌ Failed to process bulk transfer: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            println("❌ Error processing bulk transfer: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getBulkTransferStatus(bulkTransferId: String): Result<BulkTransferStatusDto> {
        return try {
            val response = httpClient.get("$API_BASE_URL/bulk-transfer/status/$bulkTransferId")

            if (response.status.isSuccess()) {
                val data = response.body<BulkTransferStatusDto>()
                Result.success(data)
            } else {
                val error = response.body<String>()
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            println("❌ Error getting bulk transfer status: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getCustomerBulkTransfers(customerId: String): Result<List<BulkTransferStatusDto>> {
        return try {
            val response = httpClient.get("$API_BASE_URL/bulk-transfer/customer/$customerId")

            if (response.status.isSuccess()) {
                val data = response.body<List<BulkTransferStatusDto>>()
                println("✅ Fetched ${data.size} bulk transfers")
                Result.success(data)
            } else {
                val error = response.body<String>()
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            println("❌ Error getting customer bulk transfers: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
