package org.dals.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dals.project.repository.*

data class AtmUiState(
    val isLoading: Boolean = false,
    val atms: List<AtmLocationDto> = emptyList(),
    val errorMessage: String? = null
)

class AtmLocatorViewModel(
    private val repository: AtmLocatorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AtmUiState())
    val uiState: StateFlow<AtmUiState> = _uiState.asStateFlow()

    fun findNearby(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.findNearbyAtms(latitude, longitude)
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    atms = result.getOrDefault(emptyList())
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to find ATMs"
                )
            }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.searchAtms(query)
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    atms = result.getOrDefault(emptyList())
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Search failed"
                )
            }
        }
    }
}
