package org.dals.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dals.project.repository.BulkTransferRecipientDto
import org.dals.project.repository.BulkTransferRepository
import org.dals.project.repository.BulkTransferRequestDto
import org.dals.project.repository.BulkTransferStatusDto
import org.dals.project.utils.Contact
import org.dals.project.utils.PlatformContactsManager

data class SelectedRecipient(
    val contact: Contact,
    val amount: String = "",
    val description: String = ""
)

data class BulkTransferUiState(
    val isLoading: Boolean = false,
    val contacts: List<Contact> = emptyList(),
    val selectedRecipients: List<SelectedRecipient> = emptyList(),
    val batchName: String = "",
    val fromAccountId: String = "",
    val searchQuery: String = "",
    val currentBulkTransfer: BulkTransferStatusDto? = null,
    val bulkTransferHistory: List<BulkTransferStatusDto> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showContactPicker: Boolean = false,
    val showSummary: Boolean = false,
    val permissionGranted: Boolean = false
)

class BulkTransferViewModel : ViewModel() {
    private val httpClient = HttpClient()
    private val repository = BulkTransferRepository(httpClient)
    private val contactsManager = PlatformContactsManager()

    private val _uiState = MutableStateFlow(BulkTransferUiState())
    val uiState: StateFlow<BulkTransferUiState> = _uiState.asStateFlow()

    fun requestContactsPermission() {
        viewModelScope.launch {
            try {
                val granted = contactsManager.requestPermission()
                _uiState.value = _uiState.value.copy(
                    permissionGranted = granted,
                    errorMessage = if (!granted) "Contacts permission denied" else null
                )

                if (granted) {
                    fetchContacts()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to request permission: ${e.message}"
                )
            }
        }
    }

    fun fetchContacts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val contacts = contactsManager.fetchContacts()
                _uiState.value = _uiState.value.copy(
                    contacts = contacts,
                    isLoading = false,
                    permissionGranted = true
                )
                println("✅ Fetched ${contacts.size} contacts")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to fetch contacts: ${e.message}"
                )
                println("❌ Error fetching contacts: ${e.message}")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun updateBatchName(name: String) {
        _uiState.value = _uiState.value.copy(batchName = name)
    }

    fun setFromAccountId(accountId: String) {
        _uiState.value = _uiState.value.copy(fromAccountId = accountId)
    }

    fun addRecipient(contact: Contact) {
        val currentRecipients = _uiState.value.selectedRecipients
        if (currentRecipients.none { it.contact.id == contact.id }) {
            _uiState.value = _uiState.value.copy(
                selectedRecipients = currentRecipients + SelectedRecipient(contact)
            )
        }
    }

    fun removeRecipient(contactId: String) {
        _uiState.value = _uiState.value.copy(
            selectedRecipients = _uiState.value.selectedRecipients.filter { it.contact.id != contactId }
        )
    }

    fun updateRecipientAmount(contactId: String, amount: String) {
        _uiState.value = _uiState.value.copy(
            selectedRecipients = _uiState.value.selectedRecipients.map { recipient ->
                if (recipient.contact.id == contactId) {
                    recipient.copy(amount = amount)
                } else {
                    recipient
                }
            }
        )
    }

    fun updateRecipientDescription(contactId: String, description: String) {
        _uiState.value = _uiState.value.copy(
            selectedRecipients = _uiState.value.selectedRecipients.map { recipient ->
                if (recipient.contact.id == contactId) {
                    recipient.copy(description = description)
                } else {
                    recipient
                }
            }
        )
    }

    fun showSummary() {
        _uiState.value = _uiState.value.copy(showSummary = true)
    }

    fun hideSummary() {
        _uiState.value = _uiState.value.copy(showSummary = false)
    }

    fun createBulkTransfer(customerId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val recipients = _uiState.value.selectedRecipients.map { selected ->
                    BulkTransferRecipientDto(
                        name = selected.contact.name,
                        phoneNumber = selected.contact.phoneNumber,
                        accountNumber = null,
                        amount = selected.amount.toDoubleOrNull() ?: 0.0,
                        description = selected.description.ifBlank { null }
                    )
                }

                val request = BulkTransferRequestDto(
                    customerId = customerId,
                    fromAccountId = _uiState.value.fromAccountId,
                    batchName = _uiState.value.batchName,
                    recipients = recipients,
                    description = null
                )

                val result = repository.createBulkTransfer(request)

                if (result.isSuccess) {
                    val response = result.getOrThrow()

                    // Automatically process the bulk transfer
                    processBulkTransfer(response.bulkTransferId)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to create bulk transfer"
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

    private fun processBulkTransfer(bulkTransferId: String) {
        viewModelScope.launch {
            try {
                val result = repository.processBulkTransfer(bulkTransferId)

                if (result.isSuccess) {
                    val status = result.getOrThrow()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentBulkTransfer = status,
                        successMessage = "Bulk transfer completed: ${status.completedTransfers} successful, ${status.failedTransfers} failed",
                        selectedRecipients = emptyList(),
                        batchName = "",
                        showSummary = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to process bulk transfer"
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

    fun loadBulkTransferHistory(customerId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val result = repository.getCustomerBulkTransfers(customerId)

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        bulkTransferHistory = result.getOrThrow(),
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to load history"
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
