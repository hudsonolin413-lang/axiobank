package org.dals.project.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dals.project.repository.CreateSignatureRequestDto
import org.dals.project.repository.DigitalSignatureDto
import org.dals.project.repository.DigitalSignatureRepository

data class DigitalSignatureUiState(
    val isLoading: Boolean = false,
    val signatures: List<DigitalSignatureDto> = emptyList(),
    val currentSignature: ImageBitmap? = null,
    val documentName: String = "",
    val documentType: String = "General",
    val signatureCreated: DigitalSignatureDto? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showSignaturePad: Boolean = false
)

class DigitalSignatureViewModel : ViewModel() {
    private val httpClient = HttpClient()
    private val repository = DigitalSignatureRepository(httpClient)

    private val _uiState = MutableStateFlow(DigitalSignatureUiState())
    val uiState: StateFlow<DigitalSignatureUiState> = _uiState.asStateFlow()

    fun loadSignatures(customerId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val result = repository.getCustomerSignatures(customerId)

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        signatures = result.getOrThrow(),
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to load signatures"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error: ${e.message}"
                )
            }
        }
    }

    fun updateDocumentName(name: String) {
        _uiState.value = _uiState.value.copy(documentName = name)
    }

    fun updateDocumentType(type: String) {
        _uiState.value = _uiState.value.copy(documentType = type)
    }

    fun showSignaturePad() {
        _uiState.value = _uiState.value.copy(showSignaturePad = true)
    }

    fun hideSignaturePad() {
        _uiState.value = _uiState.value.copy(showSignaturePad = false, currentSignature = null)
    }

    fun setSignature(signature: ImageBitmap) {
        _uiState.value = _uiState.value.copy(currentSignature = signature)
    }

    fun createSignature(customerId: String, signatureBase64: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val documentId = "DOC-${System.currentTimeMillis()}"

                val request = CreateSignatureRequestDto(
                    customerId = customerId,
                    documentId = documentId,
                    documentType = _uiState.value.documentType,
                    documentName = _uiState.value.documentName,
                    signatureData = signatureBase64,
                    ipAddress = null,
                    deviceInfo = "Axio Bank Desktop App"
                )

                val result = repository.createSignature(request)

                if (result.isSuccess) {
                    val signature = result.getOrThrow()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        signatureCreated = signature,
                        successMessage = "Signature created successfully",
                        showSignaturePad = false,
                        documentName = "",
                        currentSignature = null
                    )

                    // Reload signatures
                    loadSignatures(customerId)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to create signature"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error: ${e.message}"
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

    override fun onCleared() {
        super.onCleared()
        httpClient.close()
    }
}
