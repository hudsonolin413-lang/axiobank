package org.dals.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dals.project.repository.*

data class LoanRefinancingUiState(
    val isLoading: Boolean = false,
    val analysis: RefinancingAnalysisDto? = null,
    val applications: List<RefinancingApplicationDto> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class LoanRefinancingViewModel(
    private val repository: LoanRefinancingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoanRefinancingUiState())
    val uiState: StateFlow<LoanRefinancingUiState> = _uiState.asStateFlow()

    fun analyze(loanId: String, rate: Double, term: Int, costs: Double = 0.0) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = repository.analyzeRefinancing(loanId, rate, term, costs)
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    analysis = result.getOrNull()
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Analysis failed"
                )
            }
        }
    }

    fun apply(request: CreateRefinancingApplicationRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.applyForRefinancing(request)
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Refinancing application submitted successfully"
                )
                loadApplications(request.customerId)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Application failed"
                )
            }
        }
    }

    fun loadApplications(customerId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.getCustomerApplications(customerId)
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    applications = result.getOrDefault(emptyList())
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to load applications"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
