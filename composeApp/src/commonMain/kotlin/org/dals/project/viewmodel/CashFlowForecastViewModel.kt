package org.dals.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dals.project.repository.*

data class CashFlowUiState(
    val isLoading: Boolean = false,
    val analysis: CashFlowAnalysisDto? = null,
    val forecasts: List<CashFlowForecastDto> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class CashFlowForecastViewModel(
    private val repository: CashFlowForecastRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CashFlowUiState())
    val uiState: StateFlow<CashFlowUiState> = _uiState.asStateFlow()

    fun loadAnalysis(accountId: String, customerId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.getCashFlowAnalysis(accountId, customerId)
            
            if (result.isSuccess) {
                val analysis = result.getOrNull()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    analysis = analysis,
                    forecasts = analysis?.forecasts ?: emptyList()
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to load analysis"
                )
            }
        }
    }

    fun generateForecast(customerId: String, accountId: String, days: Int = 90) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val request = GenerateForecastRequest(customerId, accountId, days)
            val result = repository.generateForecasts(request)
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Forecast generated successfully"
                )
                loadAnalysis(accountId, customerId)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to generate forecast"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
