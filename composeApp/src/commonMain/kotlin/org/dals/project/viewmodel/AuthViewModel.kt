package org.dals.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import org.dals.project.model.*
import org.dals.project.repository.AuthRepository
import org.dals.project.storage.PreferencesStorage
import org.dals.project.utils.SnackbarManager

data class AuthUiState(
    val isLoading: Boolean = false,
    val authState: AuthState = AuthState.LOGGED_OUT,
    val currentUser: User? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val savedCredentials: Pair<String?, String?> = Pair(null, null),
    val lastUsername: String? = null,
    val isRememberMeEnabled: Boolean = false
)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var inactivityManager: org.dals.project.utils.InactivityManager? = null
    private var userDataRefreshJob: Job? = null

    init {
        observeAuthState()
        loadSavedCredentials()
        startUserDataRefreshPolling()
    }

    /**
     * Start periodic polling to refresh user data (e.g., KYC status updates)
     */
    private fun startUserDataRefreshPolling() {
        userDataRefreshJob?.cancel()
        userDataRefreshJob = viewModelScope.launch {
            // Track previous KYC status to only show notification on actual status change
            var previousKycStatus: KycStatus? = null
            
            while (true) {
                delay(10_000L) // Poll every 10 seconds

                // Only refresh if user is logged in
                if (authRepository.isLoggedIn()) {
                    try {
                        val success = authRepository.refreshUserData()
                        if (success) {
                            // Check if KYC status changed to VERIFIED (not just already verified)
                            val currentUser = _uiState.value.currentUser
                            if (currentUser != null) {
                                // Only show notification if status actually changed from non-verified to verified
                                if (previousKycStatus != null && 
                                    previousKycStatus != KycStatus.VERIFIED && 
                                    currentUser.kycStatus == KycStatus.VERIFIED && 
                                    currentUser.accountNumber != null) {
                                    SnackbarManager.showSuccess("Your account has been verified! Account #: ${currentUser.accountNumber}")
                                }
                                previousKycStatus = currentUser.kycStatus
                            }
                        }
                    } catch (e: Exception) {
                        // Silently fail - don't interrupt user experience
                        println("Background refresh failed: ${e.message}")
                    }
                }
            }
        }
    }

    fun setInactivityManager(manager: org.dals.project.utils.InactivityManager) {
        this.inactivityManager = manager
    }

    private fun resetInactivityTimer() {
        inactivityManager?.resetTimer()
    }

    fun setStorage(storage: PreferencesStorage) {
        authRepository.setStorage(storage)
        loadSavedCredentials()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.authState.collect { authState ->
                _uiState.value = _uiState.value.copy(authState = authState)
            }
        }

        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(currentUser = user)
            }
        }
    }

    private fun loadSavedCredentials() {
        viewModelScope.launch {
            try {
                val credentials = authRepository.getSavedCredentials()
                val rememberMe = authRepository.isRememberMeEnabled()
                val lastUsername = authRepository.getLastUsername()

                _uiState.value = _uiState.value.copy(
                    savedCredentials = credentials,
                    isRememberMeEnabled = rememberMe,
                    lastUsername = lastUsername
                )
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    fun tryAutoLogin() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val success = authRepository.tryAutoLogin()

            if (!success) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun login(username: String, password: String, rememberMe: Boolean = false) {
        resetInactivityTimer()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            val response = authRepository.login(username, password, rememberMe)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = if (response.success) null else response.message,
                successMessage = if (response.success) response.message else null
            )

            // Show immediate feedback
            if (response.success) {
                SnackbarManager.showSuccess(response.message)
                loadSavedCredentials()
            } else {
                SnackbarManager.showError(response.message)
            }
        }
    }

    fun register(
        username: String,
        password: String,
        confirmPassword: String,
        fullName: String,
        email: String,
        phoneNumber: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            val response = authRepository.register(
                username = username,
                password = password,
                confirmPassword = confirmPassword,
                fullName = fullName,
                email = email,
                phoneNumber = phoneNumber
            )

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = if (response.success) null else response.message,
                successMessage = if (response.success) response.message else null
            )

            // Show immediate feedback
            if (response.success) {
                SnackbarManager.showSuccess(response.message)
                loadSavedCredentials()
            } else {
                SnackbarManager.showError(response.message)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            // Reload credentials state after logout (should be empty now)
            loadSavedCredentials()
        }
    }

    fun logoutWithoutClearingCredentials() {
        viewModelScope.launch {
            authRepository.logoutWithoutClearingCredentials()
            // Reload credentials to update lastUsername in state
            loadSavedCredentials()
        }
    }

    fun lock() {
        viewModelScope.launch {
            authRepository.lock()
        }
    }

    fun clearSavedCredentials() {
        viewModelScope.launch {
            authRepository.clearSavedCredentials()
            loadSavedCredentials()
        }
    }

    fun setAutoLogin(enabled: Boolean) {
        viewModelScope.launch {
            authRepository.setAutoLogin(enabled)
        }
    }

    fun loginWithBiometrics() {
        viewModelScope.launch {
            val credentials = authRepository.getSavedCredentials()
            val username = credentials.first
            val password = credentials.second

            if (username != null && password != null) {
                login(username, password, true)
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "No saved credentials found. Please login with password once."
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    fun sendPasswordResetCode(email: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            val response = authRepository.sendPasswordResetCode(email)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = if (response.success) null else response.message,
                successMessage = if (response.success) response.message else null
            )

            // Show immediate feedback
            if (response.success) {
                SnackbarManager.showSuccess(response.message)
            } else {
                SnackbarManager.showError(response.message)
            }

            onComplete(response.success)
        }
    }

    fun verifyPasswordResetCode(email: String, code: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            val response = authRepository.verifyPasswordResetCode(email, code)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = if (response.success) null else response.message,
                successMessage = if (response.success) response.message else null
            )

            // Show immediate feedback
            if (response.success) {
                SnackbarManager.showSuccess(response.message)
            } else {
                SnackbarManager.showError(response.message)
            }

            onComplete(response.success)
        }
    }

    fun resetPassword(email: String, code: String, newPassword: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            val response = authRepository.resetPassword(email, code, newPassword)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = if (response.success) null else response.message,
                successMessage = if (response.success) response.message else null
            )

            // Show immediate feedback
            if (response.success) {
                SnackbarManager.showSuccess(response.message)
            } else {
                SnackbarManager.showError(response.message)
            }

            onComplete(response.success)
        }
    }

    fun isLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }

    fun uploadKYCDocument(
        customerId: String,
        documentType: String,
        fileName: String,
        fileData: ByteArray,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val response = authRepository.uploadKYCDocument(
                customerId = customerId,
                documentType = documentType,
                fileName = fileName,
                fileData = fileData
            )

            _uiState.value = _uiState.value.copy(isLoading = false)

            if (response.success) {
                _uiState.value = _uiState.value.copy(successMessage = response.message)
                SnackbarManager.showSuccess(response.message)
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = response.message)
                SnackbarManager.showError(response.message)
                onError(response.message)
            }
        }
    }

    fun getKYCDocuments(
        customerId: String,
        onSuccess: (List<Map<String, String>>) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val documents = authRepository.getKYCDocuments(customerId)
                onSuccess(documents)
            } catch (e: Exception) {
                onError(e.message ?: "Failed to fetch KYC documents")
            }
        }
    }

    /**
     * Manually refresh user data (can be called by pull-to-refresh)
     */
    fun refreshUserData() {
        viewModelScope.launch {
            try {
                authRepository.refreshUserData()
            } catch (e: Exception) {
                println("Failed to refresh user data: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        userDataRefreshJob?.cancel()
    }
}