package org.dals.project.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.dals.project.API_BASE_URL

@Serializable
data class QRCodeData(
    val accountNumber: String,
    val accountName: String,
    val bankName: String = "Axio Bank",
    val customerId: String,
    val timestamp: String
)

@Serializable
data class QRPaymentRequest(
    val fromCustomerId: String,
    val qrData: String,
    val amount: String,
    val description: String = "QR Code Payment"
)

@Serializable
data class QRPaymentResponse(
    val success: Boolean,
    val message: String,
    val transactionId: String? = null,
    val recipientName: String? = null,
    val amount: String? = null
)

class QRPaymentRepository(private val httpClient: HttpClient) {
    /**
     * Generate QR code data for a customer
     */
    suspend fun generateQRCode(customerId: String): Result<QRCodeData> {
        return try {
            val url = "$API_BASE_URL/qr-payment/generate/$customerId"
            println("🔹 QRPaymentRepository: Calling API: $url")

            val response: HttpResponse = httpClient.get(url)

            println("🔹 QRPaymentRepository: Response status: ${response.status}")

            if (response.status == HttpStatusCode.OK) {
                val qrData = response.body<QRCodeData>()
                println("✅ QRPaymentRepository: QR data received: ${qrData.accountNumber}")
                Result.success(qrData)
            } else {
                val errorBody = response.bodyAsText()
                println("❌ QRPaymentRepository: API error (${response.status}): $errorBody")
                Result.failure(Exception("Failed to generate QR code (${response.status}): $errorBody"))
            }
        } catch (e: Exception) {
            println("❌ QRPaymentRepository: Exception: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Validate QR code data
     */
    suspend fun validateQRCode(qrData: String): Result<QRPaymentResponse> {
        return try {
            val response: HttpResponse = httpClient.post("$API_BASE_URL/qr-payment/validate") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("qrData" to qrData))
            }

            if (response.status == HttpStatusCode.OK) {
                val validationResponse = response.body<QRPaymentResponse>()
                Result.success(validationResponse)
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(Exception("Validation failed: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Process QR payment transaction
     */
    suspend fun processQRPayment(request: QRPaymentRequest): Result<QRPaymentResponse> {
        return try {
            val response: HttpResponse = httpClient.post("$API_BASE_URL/qr-payment/process") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val paymentResponse = response.body<QRPaymentResponse>()

            if (paymentResponse.success) {
                Result.success(paymentResponse)
            } else {
                Result.failure(Exception(paymentResponse.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Convert QR code data to JSON string for QR code generation
     */
    fun qrDataToJsonString(qrData: QRCodeData): String {
        return Json.encodeToString(QRCodeData.serializer(), qrData)
    }

    /**
     * Parse JSON string back to QR code data
     */
    fun jsonStringToQRData(jsonString: String): QRCodeData {
        return Json.decodeFromString(QRCodeData.serializer(), jsonString)
    }
}
