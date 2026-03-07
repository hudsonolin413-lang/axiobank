package org.dals.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dals.project.repository.AuthRepository
import org.dals.project.repository.ReferralData
import org.dals.project.repository.ReferralRepository
import org.dals.project.utils.SnackbarManager

data class ReferralUiState(
    val isLoading: Boolean = false,
    val referrals: List<ReferralData> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ReferralViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val repository = ReferralRepository(authRepository)
    private val _uiState = MutableStateFlow(ReferralUiState())
    val uiState: StateFlow<ReferralUiState> = _uiState.asStateFlow()

    init {
        loadReferrals()
    }

    fun loadReferrals() {
        val currentUser = authRepository.currentUser.value ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.fetchReferrals(currentUser.id)
                .onSuccess { data ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        referrals = data,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
        }
    }

    fun inviteFriend(email: String, referralCode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.inviteFriend(email, referralCode)
                .onSuccess { message ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = message,
                        errorMessage = null
                    )
                    SnackbarManager.showSuccess(message)
                    loadReferrals() // Refresh list
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                    SnackbarManager.showError(error.message ?: "Failed to send invitation")
                }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}
