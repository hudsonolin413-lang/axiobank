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
import org.dals.project.API_BASE_URL
import org.dals.project.repository.*

data class SubAccountUiState(
    val isLoading: Boolean = false,
    val subAccounts: List<SubAccountRecord> = emptyList(),
    val activeSubAccounts: List<SubAccountRecord> = emptyList(),
    val selectedSubAccount: SubAccountRecord? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val showAddDialog: Boolean = false,
    val showTransferDialog: Boolean = false,
    val showWithdrawDialog: Boolean = false,
    val editingSubAccount: SubAccountRecord? = null,
    val totalSaved: Double = 0.0,
    val totalTarget: Double = 0.0,
    val overallProgress: Double = 0.0
)

class SubAccountViewModel : ViewModel() {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
            })
        }
    }

    private val repository = SubAccountRepository(httpClient, "$API_BASE_URL/sub-accounts")

    private val _uiState = MutableStateFlow(SubAccountUiState())
    val uiState: StateFlow<SubAccountUiState> = _uiState.asStateFlow()

    fun loadSubAccounts(customerId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = repository.getAllSubAccounts(customerId)
            if (result.isSuccess) {
                val subAccounts = result.getOrNull() ?: emptyList()
                val activeSubAccounts = subAccounts.filter { it.isActive }

                val totalSaved = activeSubAccounts.sumOf { it.currentBalance.toDoubleOrNull() ?: 0.0 }
                val totalTarget = activeSubAccounts.mapNotNull { it.targetAmount?.toDoubleOrNull() }.sum()
                val overallProgress = if (totalTarget > 0) (totalSaved / totalTarget * 100).coerceAtMost(100.0) else 0.0

                _uiState.update {
                    it.copy(
                        subAccounts = subAccounts,
                        activeSubAccounts = activeSubAccounts,
                        totalSaved = totalSaved,
                        totalTarget = totalTarget,
                        overallProgress = overallProgress,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        error = result.exceptionOrNull()?.message ?: "Failed to load sub-accounts",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun createSubAccount(customerId: String, request: CreateSubAccountRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = repository.createSubAccount(request)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Sub-account created successfully",
                        showAddDialog = false
                    )
                }
                loadSubAccounts(customerId)
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        error = result.exceptionOrNull()?.message ?: "Failed to create sub-account",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun updateSubAccount(customerId: String, subAccountId: String, request: UpdateSubAccountRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = repository.updateSubAccount(subAccountId, request)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Sub-account updated successfully",
                        editingSubAccount = null
                    )
                }
                loadSubAccounts(customerId)
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        error = result.exceptionOrNull()?.message ?: "Failed to update sub-account",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun deleteSubAccount(customerId: String, subAccountId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = repository.deleteSubAccount(subAccountId)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Sub-account deleted successfully"
                    )
                }
                loadSubAccounts(customerId)
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        error = result.exceptionOrNull()?.message ?: "Failed to delete sub-account",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun transferToSubAccount(customerId: String, request: TransferToSubAccountRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = repository.transferToSubAccount(request)
            if (result.isSuccess) {
                val response = result.getOrNull()!!
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = response.message,
                        showTransferDialog = false
                    )
                }
                loadSubAccounts(customerId)
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        error = result.exceptionOrNull()?.message ?: "Transfer failed",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun withdrawFromSubAccount(customerId: String, request: TransferToSubAccountRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = repository.withdrawFromSubAccount(request)
            if (result.isSuccess) {
                val response = result.getOrNull()!!
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = response.message,
                        showWithdrawDialog = false
                    )
                }
                loadSubAccounts(customerId)
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        error = result.exceptionOrNull()?.message ?: "Withdrawal failed",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun toggleLock(customerId: String, subAccountId: String) {
        viewModelScope.launch {
            val result = repository.toggleLock(subAccountId)
            if (result.isSuccess) {
                loadSubAccounts(customerId)
            } else {
                _uiState.update {
                    it.copy(error = "Failed to toggle lock status")
                }
            }
        }
    }

    fun selectSubAccount(subAccount: SubAccountRecord) {
        _uiState.update { it.copy(selectedSubAccount = subAccount) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedSubAccount = null) }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true, editingSubAccount = null) }
    }

    fun hideAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun showTransferDialog(subAccount: SubAccountRecord) {
        _uiState.update { it.copy(showTransferDialog = true, selectedSubAccount = subAccount) }
    }

    fun hideTransferDialog() {
        _uiState.update { it.copy(showTransferDialog = false, selectedSubAccount = null) }
    }

    fun showWithdrawDialog(subAccount: SubAccountRecord) {
        _uiState.update { it.copy(showWithdrawDialog = true, selectedSubAccount = subAccount) }
    }

    fun hideWithdrawDialog() {
        _uiState.update { it.copy(showWithdrawDialog = false, selectedSubAccount = null) }
    }

    fun startEdit(subAccount: SubAccountRecord) {
        _uiState.update { it.copy(editingSubAccount = subAccount) }
    }

    fun cancelEdit() {
        _uiState.update { it.copy(editingSubAccount = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
