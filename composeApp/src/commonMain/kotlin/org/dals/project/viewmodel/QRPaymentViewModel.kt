package org.dals.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.dals.project.repository.QRCodeData
import org.dals.project.repository.QRPaymentRepository
import org.dals.project.repository.QRPaymentRequest

data class QRPaymentUiState(
    val isLoading: Boolean = false,
    val myQRCode: QRCodeData? = null,
    val scannedQRData: String? = null,
    val validatedRecipient: String? = null,
    val paymentAmount: String = "",
    val paymentDescription: String = "QR Code Payment",
    val paymentSuccess: Boolean = false,
    val transactionId: String? = null,
    val errorMessage: String? = null,
    val currentTab: QRPaymentTab = QRPaymentTab.MY_QR
)

enum class QRPaymentTab {
    MY_QR, SCAN_PAY
}

class QRPaymentViewModel : ViewModel() {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
            })
        }
    }

    private val repository = QRPaymentRepository(httpClient)

    private val _uiState = MutableStateFlow(QRPaymentUiState())
    val uiState: StateFlow<QRPaymentUiState> = _uiState.asStateFlow()

    /**
     * Generate QR code for the current user
     */
    fun generateMyQRCode(customerId: String) {
        viewModelScope.launch {
            println("🔹 QRPaymentViewModel: Generating QR code for customer: $customerId")
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            repository.generateQRCode(customerId).fold(
                onSuccess = { qrData ->
                    println("✅ QRPaymentViewModel: QR code generated successfully: ${qrData.accountNumber}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        myQRCode = qrData,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    println("❌ QRPaymentViewModel: Failed to generate QR code: ${error.message}")
                    error.printStackTrace()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to generate QR code: ${error.message}"
                    )
                }
            )
        }
    }

    /**
     * Validate scanned QR code
     */
    fun validateScannedQR(qrData: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            repository.validateQRCode(qrData).fold(
                onSuccess = { response ->
                    if (response.success) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            scannedQRData = qrData,
                            validatedRecipient = response.recipientName,
                            errorMessage = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = response.message
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "QR code validation failed: ${error.message}"
                    )
                }
            )
        }
    }

    /**
     * Process QR payment
     */
    fun processPayment(fromCustomerId: String) {
        val currentState = _uiState.value

        if (currentState.scannedQRData == null) {
            _uiState.value = currentState.copy(errorMessage = "No QR code scanned")
            return
        }

        if (currentState.paymentAmount.isBlank() || currentState.paymentAmount.toDoubleOrNull() == null) {
            _uiState.value = currentState.copy(errorMessage = "Please enter a valid amount")
            return
        }

        val amount = currentState.paymentAmount.toDouble()
        if (amount <= 0) {
            _uiState.value = currentState.copy(errorMessage = "Amount must be greater than zero")
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, errorMessage = null)

            val request = QRPaymentRequest(
                fromCustomerId = fromCustomerId,
                qrData = currentState.scannedQRData,
                amount = currentState.paymentAmount,
                description = currentState.paymentDescription
            )

            repository.processQRPayment(request).fold(
                onSuccess = { response ->
                    if (response.success) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            paymentSuccess = true,
                            transactionId = response.transactionId,
                            errorMessage = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = response.message
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Payment failed: ${error.message}"
                    )
                }
            )
        }
    }

    /**
     * Update payment amount
     */
    fun updateAmount(amount: String) {
        _uiState.value = _uiState.value.copy(paymentAmount = amount, errorMessage = null)
    }

    /**
     * Update payment description
     */
    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(paymentDescription = description)
    }

    /**
     * Switch tabs
     */
    fun switchTab(tab: QRPaymentTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab, errorMessage = null)
    }

    /**
     * Reset payment state
     */
    fun resetPayment() {
        _uiState.value = _uiState.value.copy(
            scannedQRData = null,
            validatedRecipient = null,
            paymentAmount = "",
            paymentDescription = "QR Code Payment",
            paymentSuccess = false,
            transactionId = null,
            errorMessage = null
        )
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Get JSON string from QR code data for QR code generation
     */
    fun getQRCodeJsonString(): String? {
        return _uiState.value.myQRCode?.let { qrData ->
            repository.qrDataToJsonString(qrData)
        }
    }
}
