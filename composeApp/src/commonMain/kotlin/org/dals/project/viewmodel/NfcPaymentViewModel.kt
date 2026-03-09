package org.dals.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dals.project.repository.NfcPaymentRepository
import org.dals.project.repository.InitiateNfcPaymentRequest
import org.dals.project.repository.NfcPaymentResponse

data class NfcPaymentUiState(
    val isLoading: Boolean = false,
    val isScanning: Boolean = false,
    val currentPayment: NfcPaymentResponse? = null,
    val paymentHistory: List<NfcPaymentResponse> = emptyList(),
    val activePayments: List<NfcPaymentResponse> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val nfcAvailable: Boolean = false,
    val nfcEnabled: Boolean = false
)

class NfcPaymentViewModel(
    private val repository: NfcPaymentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NfcPaymentUiState())
    val uiState: StateFlow<NfcPaymentUiState> = _uiState.asStateFlow()

    fun setNfcAvailability(available: Boolean, enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            nfcAvailable = available,
            nfcEnabled = enabled
        )
    }

    fun startNfcScan() {
        _uiState.value = _uiState.value.copy(
            isScanning = true,
            errorMessage = null,
            successMessage = null
        )
    }

    fun stopNfcScan() {
        _uiState.value = _uiState.value.copy(isScanning = false)
    }

    fun initiatePayment(
        customerId: String,
        fromAccountId: String,
        merchantName: String,
        merchantId: String?,
        amount: Double,
        currency: String = "USD",
        deviceId: String?,
        nfcTagId: String?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            val request = InitiateNfcPaymentRequest(
                customerId = customerId,
                fromAccountId = fromAccountId,
                merchantName = merchantName,
                merchantId = merchantId,
                amount = amount,
                currency = currency,
                deviceId = deviceId,
                nfcTagId = nfcTagId
            )

            val result = repository.initiateNfcPayment(request)

            if (result.isSuccess) {
                val payment = result.getOrNull()!!
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentPayment = payment,
                    successMessage = "Payment initiated successfully"
                )

                // Automatically process the payment
                processPayment(payment.id)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to initiate payment"
                )
            }
        }
    }

    fun processPayment(nfcPaymentId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            val result = repository.processNfcPayment(nfcPaymentId)

            if (result.isSuccess) {
                val payment = result.getOrNull()!!
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isScanning = false,
                    currentPayment = payment,
                    successMessage = if (payment.status == "COMPLETED") {
                        "Payment completed successfully! Auth Code: ${payment.authorizationCode}"
                    } else {
                        "Payment ${payment.status.lowercase()}"
                    }
                )

                // Refresh payment history
                loadPaymentHistory(payment.customerId)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isScanning = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to process payment"
                )
            }
        }
    }

    fun loadPaymentHistory(customerId: String, limit: Int = 50, offset: Int = 0) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = repository.getNfcPaymentHistory(customerId, limit, offset)

            if (result.isSuccess) {
                val history = result.getOrNull()!!
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    paymentHistory = history.payments
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to load payment history"
                )
            }
        }
    }

    fun loadActivePayments(customerId: String) {
        viewModelScope.launch {
            val result = repository.getActiveNfcPayments(customerId)

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    activePayments = result.getOrNull() ?: emptyList()
                )
            }
        }
    }

    fun cancelPayment(nfcPaymentId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            val result = repository.cancelNfcPayment(nfcPaymentId)

            if (result.isSuccess) {
                val payment = result.getOrNull()!!
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentPayment = payment,
                    successMessage = "Payment cancelled successfully"
                )

                // Refresh payment history
                loadPaymentHistory(payment.customerId)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to cancel payment"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    fun clearCurrentPayment() {
        _uiState.value = _uiState.value.copy(currentPayment = null)
    }
}
