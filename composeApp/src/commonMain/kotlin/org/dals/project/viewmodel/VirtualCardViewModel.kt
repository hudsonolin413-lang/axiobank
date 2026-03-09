package org.dals.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dals.project.repository.*

data class VirtualCardUiState(
    val isLoading: Boolean = false,
    val cards: List<VirtualCardDto> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class VirtualCardViewModel(
    private val repository: VirtualCardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VirtualCardUiState())
    val uiState: StateFlow<VirtualCardUiState> = _uiState.asStateFlow()

    fun loadCards(customerId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.getVirtualCards(customerId)
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    cards = result.getOrDefault(emptyList())
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to load cards"
                )
            }
        }
    }

    fun createCard(request: CreateVirtualCardRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.createVirtualCard(request)
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Virtual card created successfully"
                )
                loadCards(request.customerId)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to create card"
                )
            }
        }
    }

    fun toggleStatus(cardId: String, currentStatus: String, customerId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val newStatus = if (currentStatus == "ACTIVE") "FROZEN" else "ACTIVE"
            val result = repository.updateStatus(cardId, newStatus)
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Card status updated to $newStatus"
                )
                loadCards(customerId)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to update card status"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
