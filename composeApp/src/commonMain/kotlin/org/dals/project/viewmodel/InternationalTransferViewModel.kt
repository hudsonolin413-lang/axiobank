package org.dals.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dals.project.repository.*

data class InternationalTransferUiState(
    val isLoading: Boolean = false,
    val quote: InternationalTransferQuote? = null,
    val transfers: List<InternationalTransferDto> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class InternationalTransferViewModel(
    private val repository: InternationalTransferRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InternationalTransferUiState())
    val uiState: StateFlow<InternationalTransferUiState> = _uiState.asStateFlow()

    fun getQuote(amount: Double, fromCurrency: String, toCurrency: String, country: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = repository.getQuote(amount, fromCurrency, toCurrency, country)
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    quote = result.getOrNull()
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to get quote"
                )
            }
        }
    }

    fun createTransfer(request: CreateInternationalTransferRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.createTransfer(request)
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Transfer initiated successfully"
                )
                loadTransfers(request.customerId)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Transfer failed"
                )
            }
        }
    }

    fun loadTransfers(customerId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.getCustomerTransfers(customerId)
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    transfers = result.getOrDefault(emptyList())
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to load transfers"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
