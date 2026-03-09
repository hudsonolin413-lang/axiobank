package org.dals.project.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class BillVendor(
    val id: String,
    val vendorCode: String,
    val vendorName: String,
    val category: String,
    val description: String?,
    val logoUrl: String?,
    val requiresAccountNumber: Boolean,
    val accountNumberLabel: String,
    val minAmount: Double,
    val maxAmount: Double,
    val processingFeeType: String,
    val processingFeeValue: Double
)

@Serializable
data class SavedBiller(
    val id: String,
    val vendorId: String,
    val vendorName: String,
    val nickname: String?,
    val accountNumber: String,
    val category: String,
    val isFavorite: Boolean
)

@Serializable
data class BillPaymentRecord(
    val id: String,
    val vendorId: String,
    val vendorName: String,
    val accountNumber: String,
    val amount: Double,
    val processingFee: Double,
    val totalAmount: Double,
    val paymentReference: String,
    val vendorReference: String?,
    val status: String,
    val description: String?,
    val createdAt: String,
    val processedAt: String?
)

@Serializable
data class PayBillRequest(
    val userId: String,
    val vendorId: String,
    val accountNumber: String,
    val amount: Double,
    val description: String? = null,
    val saveBiller: Boolean = false,
    val billerNickname: String? = null
)

@Serializable
data class PayBillResponse(
    val success: Boolean,
    val message: String,
    val payment: BillPaymentRecord? = null,
    val newBalance: Double? = null
)

@Serializable
data class VendorsResponse(
    val success: Boolean,
    val message: String,
    val vendors: List<BillVendor> = emptyList()
)

@Serializable
data class CategoriesResponse(
    val success: Boolean,
    val message: String,
    val categories: List<String> = emptyList()
)

@Serializable
data class SavedBillersResponse(
    val success: Boolean,
    val message: String,
    val billers: List<SavedBiller> = emptyList()
)

@Serializable
data class PaymentHistoryResponse(
    val success: Boolean,
    val message: String,
    val payments: List<BillPaymentRecord> = emptyList()
)

class BillPaymentRepository(private val client: HttpClient) {
    private val baseUrl = "https://axionbank.up.railway.app/api"

    /**
     * Get all bill payment vendors
     */
    suspend fun getAllVendors(category: String? = null): Result<List<BillVendor>> {
        return try {
            val url = if (category != null) {
                "$baseUrl/bill-payment/vendors?category=$category"
            } else {
                "$baseUrl/bill-payment/vendors"
            }

            val response: VendorsResponse = client.get(url).body()
            if (response.success) {
                Result.success(response.vendors)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all vendor categories
     */
    suspend fun getVendorCategories(): Result<List<String>> {
        return try {
            val response: CategoriesResponse = client.get("$baseUrl/bill-payment/categories").body()
            if (response.success) {
                Result.success(response.categories)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user's saved billers
     */
    suspend fun getSavedBillers(userId: String): Result<List<SavedBiller>> {
        return try {
            val response: SavedBillersResponse = client.get("$baseUrl/bill-payment/saved-billers?userId=$userId").body()
            if (response.success) {
                Result.success(response.billers)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pay a bill
     */
    suspend fun payBill(request: PayBillRequest): Result<PayBillResponse> {
        return try {
            val response: PayBillResponse = client.post("$baseUrl/bill-payment/pay") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()

            if (response.success) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get bill payment history
     */
    suspend fun getPaymentHistory(userId: String, limit: Int = 50): Result<List<BillPaymentRecord>> {
        return try {
            val response: PaymentHistoryResponse = client.get("$baseUrl/bill-payment/history?userId=$userId&limit=$limit").body()
            if (response.success) {
                Result.success(response.payments)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
