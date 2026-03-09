package org.dals.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dals.project.repository.*

data class OverdraftUiState(
    val isLoading: Boolean = false,
    val protection: OverdraftProtectionDto? = null,
    val transactions: List<OverdraftTransactionDto> = emptyList(),
    val stats: OverdraftUsageStatsDto? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class OverdraftProtectionViewModel(
    private val repository: OverdraftProtectionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverdraftUiState())
    val uiState: StateFlow<OverdraftUiState> = _uiState.asStateFlow()

    fun loadData(customerId: String, accountId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val protectionResult = repository.getOverdraftProtection(accountId)
            val statsResult = repository.getOverdraftUsageStats(customerId)
            val transactionsResult = repository.getOverdraftTransactions(accountId)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                protection = protectionResult.getOrNull(),
                stats = statsResult.getOrNull(),
                transactions = transactionsResult.getOrDefault(emptyList()),
                errorMessage = protectionResult.exceptionOrNull()?.message ?: statsResult.exceptionOrNull()?.message
            )
        }
    }

    fun enroll(request: EnrollOverdraftProtectionRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.enrollOverdraftProtection(request)
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    protection = result.getOrNull(),
                    successMessage = "Successfully enrolled in overdraft protection"
                )
                loadData(request.customerId, request.accountId)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to enroll"
                )
            }
        }
    }

    fun cancel(protectionId: String, customerId: String, accountId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.cancelOverdraftProtection(protectionId)
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    protection = null,
                    successMessage = "Overdraft protection cancelled"
                )
                loadData(customerId, accountId)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to cancel"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
