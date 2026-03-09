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
import org.dals.project.API_BASE_URL
import org.dals.project.model.*
import org.dals.project.repository.SplitBillRepository

data class SplitBillUiState(
    val isLoading: Boolean = false,
    val splitBills: List<SplitBill> = emptyList(),
    val summary: SplitBillSummary? = null,
    val selectedSplitBill: SplitBill? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val showCreateDialog: Boolean = false,
    val showPayDialog: Boolean = false,
    val selectedParticipant: SplitBillParticipant? = null
)

class SplitBillViewModel : ViewModel() {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
            })
        }
    }

    private val repository = SplitBillRepository(httpClient, API_BASE_URL)

    private val _uiState = MutableStateFlow(SplitBillUiState())
    val uiState: StateFlow<SplitBillUiState> = _uiState.asStateFlow()

    init {
        // Collect repository state
        viewModelScope.launch {
            repository.splitBills.collect { bills ->
                _uiState.value = _uiState.value.copy(splitBills = bills)
            }
        }

        viewModelScope.launch {
            repository.summary.collect { summary ->
                _uiState.value = _uiState.value.copy(summary = summary)
            }
        }
    }

    fun loadSplitBills(customerId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = repository.getSplitBills(customerId)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    splitBills = result.getOrNull() ?: emptyList(),
                    isLoading = false
                )
                // Load summary
                loadSummary(customerId)
            } else {
                _uiState.value = _uiState.value.copy(
                    error = result.exceptionOrNull()?.message,
                    isLoading = false
                )
            }
        }
    }

    fun loadSummary(customerId: String) {
        viewModelScope.launch {
            val result = repository.getSummary(customerId)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(summary = result.getOrNull())
            }
        }
    }

    fun loadSplitBillDetails(splitBillId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = repository.getSplitBillById(splitBillId)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    selectedSplitBill = result.getOrNull(),
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    error = result.exceptionOrNull()?.message,
                    isLoading = false
                )
            }
        }
    }

    fun createSplitBill(request: CreateSplitBillRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = repository.createSplitBill(request)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Split bill created successfully!",
                    showCreateDialog = false,
                    isLoading = false
                )
                // Reload the list
                loadSplitBills(request.creatorId)
            } else {
                _uiState.value = _uiState.value.copy(
                    error = result.exceptionOrNull()?.message,
                    isLoading = false
                )
            }
        }
    }

    fun paySplitBill(request: PaySplitBillRequest, customerId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = repository.paySplitBill(request)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Payment successful!",
                    showPayDialog = false,
                    selectedParticipant = null,
                    isLoading = false
                )
                // Reload the split bill details
                loadSplitBillDetails(request.splitBillId)
                // Reload summary
                loadSummary(customerId)
            } else {
                _uiState.value = _uiState.value.copy(
                    error = result.exceptionOrNull()?.message,
                    isLoading = false
                )
            }
        }
    }

    fun cancelSplitBill(splitBillId: String, customerId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = repository.cancelSplitBill(splitBillId)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Split bill cancelled",
                    isLoading = false
                )
                // Reload the list
                loadSplitBills(customerId)
            } else {
                _uiState.value = _uiState.value.copy(
                    error = result.exceptionOrNull()?.message,
                    isLoading = false
                )
            }
        }
    }

    fun sendReminder(participantId: String) {
        viewModelScope.launch {
            val result = repository.sendReminder(participantId)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Reminder sent successfully"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }

    fun hideCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    fun showPayDialog(participant: SplitBillParticipant) {
        _uiState.value = _uiState.value.copy(
            showPayDialog = true,
            selectedParticipant = participant
        )
    }

    fun hidePayDialog() {
        _uiState.value = _uiState.value.copy(
            showPayDialog = false,
            selectedParticipant = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}
