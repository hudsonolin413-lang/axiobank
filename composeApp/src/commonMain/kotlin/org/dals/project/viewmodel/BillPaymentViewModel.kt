package org.dals.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.dals.project.repository.*
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

data class BillPaymentUiState(
    val isLoading: Boolean = false,
    val vendors: List<BillVendor> = emptyList(),
    val categories: List<String> = emptyList(),
    val savedBillers: List<SavedBiller> = emptyList(),
    val paymentHistory: List<BillPaymentRecord> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val error: String? = null,
    val successMessage: String? = null,
    val lastPayment: BillPaymentRecord? = null,
    val newBalance: Double? = null
)

class BillPaymentViewModel : ViewModel() {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
            })
        }
    }

    private val repository = BillPaymentRepository(httpClient)

    private val _uiState = MutableStateFlow(BillPaymentUiState())
    val uiState: StateFlow<BillPaymentUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    /**
     * Load initial data (vendors and categories)
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Load categories first
                val categoriesResult = repository.getVendorCategories()
                if (categoriesResult.isSuccess) {
                    _uiState.update { it.copy(categories = categoriesResult.getOrNull() ?: emptyList()) }
                }

                // Load all vendors
                val vendorsResult = repository.getAllVendors()
                if (vendorsResult.isSuccess) {
                    _uiState.update { it.copy(vendors = vendorsResult.getOrNull() ?: emptyList()) }
                } else {
                    _uiState.update { it.copy(error = vendorsResult.exceptionOrNull()?.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load data: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Load saved billers for a user
     */
    fun loadSavedBillers(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = repository.getSavedBillers(userId)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        savedBillers = result.getOrNull() ?: emptyList(),
                        isLoading = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        error = result.exceptionOrNull()?.message,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Load payment history for a user
     */
    fun loadPaymentHistory(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = repository.getPaymentHistory(userId)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        paymentHistory = result.getOrNull() ?: emptyList(),
                        isLoading = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        error = result.exceptionOrNull()?.message,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Filter vendors by category
     */
    fun filterByCategory(category: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedCategory = category, isLoading = true, error = null) }

            val result = repository.getAllVendors(category)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        vendors = result.getOrNull() ?: emptyList(),
                        isLoading = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        error = result.exceptionOrNull()?.message,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * Get filtered vendors based on search query
     */
    fun getFilteredVendors(): List<BillVendor> {
        val query = _uiState.value.searchQuery.lowercase()
        if (query.isBlank()) return _uiState.value.vendors

        return _uiState.value.vendors.filter {
            it.vendorName.lowercase().contains(query) ||
            it.category.lowercase().contains(query) ||
            it.description?.lowercase()?.contains(query) == true
        }
    }

    /**
     * Pay a bill
     */
    fun payBill(request: PayBillRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }

            val result = repository.payBill(request)
            if (result.isSuccess) {
                val response = result.getOrNull()!!
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = response.message,
                        lastPayment = response.payment,
                        newBalance = response.newBalance
                    )
                }
                // Refresh payment history
                if (request.userId.isNotBlank()) {
                    loadPaymentHistory(request.userId)
                }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        error = result.exceptionOrNull()?.message ?: "Payment failed",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Calculate total amount including fees
     */
    fun calculateTotalAmount(vendor: BillVendor, amount: Double): Double {
        val fee = when (vendor.processingFeeType) {
            "PERCENTAGE" -> amount * vendor.processingFeeValue / 100
            "FLAT" -> vendor.processingFeeValue
            else -> 0.0
        }
        return amount + fee
    }

    /**
     * Calculate processing fee
     */
    fun calculateProcessingFee(vendor: BillVendor, amount: Double): Double {
        return when (vendor.processingFeeType) {
            "PERCENTAGE" -> amount * vendor.processingFeeValue / 100
            "FLAT" -> vendor.processingFeeValue
            else -> 0.0
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Clear success message
     */
    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    /**
     * Refresh all data
     */
    fun refresh(userId: String? = null) {
        loadInitialData()
        if (userId != null) {
            loadSavedBillers(userId)
            loadPaymentHistory(userId)
        }
    }
}
