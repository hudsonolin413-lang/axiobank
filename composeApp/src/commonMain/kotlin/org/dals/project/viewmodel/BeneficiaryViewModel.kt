package org.dals.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.dals.project.repository.*
import org.dals.project.API_BASE_URL

data class BeneficiaryUiState(
    val isLoading: Boolean = false,
    val beneficiaries: List<BeneficiaryRecord> = emptyList(),
    val favorites: List<BeneficiaryRecord> = emptyList(),
    val searchResults: List<BeneficiaryRecord> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: String = "ALL", // ALL, BANK, MOBILE, INTERNAL
    val error: String? = null,
    val successMessage: String? = null,
    val showAddDialog: Boolean = false,
    val editingBeneficiary: BeneficiaryRecord? = null
)

class BeneficiaryViewModel : ViewModel() {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
            })
        }
    }

    private val repository = BeneficiaryRepository(httpClient, API_BASE_URL)

    private val _uiState = MutableStateFlow(BeneficiaryUiState())
    val uiState: StateFlow<BeneficiaryUiState> = _uiState.asStateFlow()

    fun loadBeneficiaries(customerId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = repository.getAllBeneficiaries(customerId)
            if (result.isSuccess) {
                val beneficiaries = result.getOrNull() ?: emptyList()
                _uiState.update {
                    it.copy(
                        beneficiaries = beneficiaries,
                        isLoading = false
                    )
                }
                loadFavorites(customerId)
            } else {
                _uiState.update {
                    it.copy(
                        error = result.exceptionOrNull()?.message ?: "Failed to load beneficiaries",
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun loadFavorites(customerId: String) {
        viewModelScope.launch {
            val result = repository.getFavoriteBeneficiaries(customerId)
            if (result.isSuccess) {
                _uiState.update { it.copy(favorites = result.getOrNull() ?: emptyList()) }
            }
        }
    }

    fun searchBeneficiaries(customerId: String, query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(searchQuery = query, isLoading = true) }

            if (query.isBlank()) {
                _uiState.update { it.copy(searchResults = emptyList(), isLoading = false) }
                return@launch
            }

            val result = repository.searchBeneficiaries(customerId, query)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        searchResults = result.getOrNull() ?: emptyList(),
                        isLoading = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        error = "Search failed",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun filterByType(type: String) {
        _uiState.update { it.copy(selectedFilter = type) }
    }

    fun getFilteredBeneficiaries(): List<BeneficiaryRecord> {
        val current = _uiState.value
        val list = if (current.searchQuery.isNotBlank()) current.searchResults else current.beneficiaries

        return if (current.selectedFilter == "ALL") {
            list
        } else {
            list.filter { it.type == current.selectedFilter }
        }
    }

    fun createBeneficiary(customerId: String, request: CreateBeneficiaryRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = repository.createBeneficiary(request)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Beneficiary added successfully",
                        showAddDialog = false
                    )
                }
                loadBeneficiaries(customerId)
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        error = result.exceptionOrNull()?.message ?: "Failed to add beneficiary",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun updateBeneficiary(customerId: String, beneficiaryId: String, request: UpdateBeneficiaryRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = repository.updateBeneficiary(beneficiaryId, request)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Beneficiary updated successfully",
                        editingBeneficiary = null
                    )
                }
                loadBeneficiaries(customerId)
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        error = result.exceptionOrNull()?.message ?: "Failed to update beneficiary",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun toggleFavorite(customerId: String, beneficiaryId: String) {
        viewModelScope.launch {
            val result = repository.toggleFavorite(beneficiaryId)
            if (result.isSuccess) {
                loadBeneficiaries(customerId)
            } else {
                _uiState.update {
                    it.copy(error = "Failed to update favorite status")
                }
            }
        }
    }

    fun deleteBeneficiary(customerId: String, beneficiaryId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = repository.deleteBeneficiary(beneficiaryId)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Beneficiary deleted successfully"
                    )
                }
                loadBeneficiaries(customerId)
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        error = result.exceptionOrNull()?.message ?: "Failed to delete beneficiary",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true, editingBeneficiary = null) }
    }

    fun hideAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun startEdit(beneficiary: BeneficiaryRecord) {
        _uiState.update { it.copy(editingBeneficiary = beneficiary) }
    }

    fun cancelEdit() {
        _uiState.update { it.copy(editingBeneficiary = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun clearSearchQuery() {
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList()) }
    }
}
