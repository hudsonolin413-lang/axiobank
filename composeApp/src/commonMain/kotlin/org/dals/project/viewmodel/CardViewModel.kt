package org.dals.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dals.project.model.Card
import org.dals.project.model.CardRequest
import org.dals.project.model.CardPaymentRequest
import org.dals.project.repository.AuthRepository
import org.dals.project.repository.CardRepository
import org.dals.project.utils.SnackbarManager

data class CardUiState(
    val isLoading: Boolean = false,
    val cards: List<Card> = emptyList(),
    val defaultCard: Card? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isAddingCard: Boolean = false,
    val isProcessingPayment: Boolean = false
)

class CardViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val repository = CardRepository(authRepository)
    private val _uiState = MutableStateFlow(CardUiState())
    val uiState: StateFlow<CardUiState> = _uiState.asStateFlow()

    init {
        loadCards()
        observeCardChanges()
    }

    private fun observeCardChanges() {
        viewModelScope.launch {
            repository.cards.collect { cards ->
                _uiState.value = _uiState.value.copy(
                    cards = cards,
                    isLoading = false
                )
            }
        }

        viewModelScope.launch {
            repository.defaultCard.collect { defaultCard ->
                _uiState.value = _uiState.value.copy(
                    defaultCard = defaultCard
                )
            }
        }
    }

    fun loadCards() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true, 
                errorMessage = null,
                successMessage = null
            )
            
            repository.fetchCards()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load cards"
                    )
                }
        }
    }

    fun addCard(cardRequest: CardRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAddingCard = true,
                errorMessage = null,
                successMessage = null
            )

            repository.addCard(cardRequest)
                .onSuccess { card ->
                    val message = "Card added successfully"
                    _uiState.value = _uiState.value.copy(
                        isAddingCard = false,
                        successMessage = message,
                        errorMessage = null
                    )
                    SnackbarManager.showSuccess(message)
                }
                .onFailure { error ->
                    val message = error.message ?: "Failed to add card"
                    _uiState.value = _uiState.value.copy(
                        isAddingCard = false,
                        errorMessage = message,
                        successMessage = null
                    )
                    SnackbarManager.showError(message)
                }
        }
    }

    fun removeCard(cardId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            repository.removeCard(cardId)
                .onSuccess {
                    val message = "Card removed successfully"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = message,
                        errorMessage = null
                    )
                    SnackbarManager.showSuccess(message)
                }
                .onFailure { error ->
                    val message = error.message ?: "Failed to remove card"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = message,
                        successMessage = null
                    )
                    SnackbarManager.showError(message)
                }
        }
    }

    fun setDefaultCard(cardId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            repository.setDefaultCard(cardId)
                .onSuccess {
                    val message = "Default card updated"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = message,
                        errorMessage = null
                    )
                    SnackbarManager.showSuccess(message)
                }
                .onFailure { error ->
                    val message = error.message ?: "Failed to set default card"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = message,
                        successMessage = null
                    )
                    SnackbarManager.showError(message)
                }
        }
    }

    fun verifyCard(cardId: String, verificationCode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            repository.verifyCard(cardId, verificationCode)
                .onSuccess {
                    val message = "Card verified successfully"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = message,
                        errorMessage = null
                    )
                    SnackbarManager.showSuccess(message)
                }
                .onFailure { error ->
                    val message = error.message ?: "Failed to verify card"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = message,
                        successMessage = null
                    )
                    SnackbarManager.showError(message)
                }
        }
    }

    fun makeCardPayment(paymentRequest: CardPaymentRequest, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessingPayment = true,
                errorMessage = null
            )

            repository.makeCardPayment(paymentRequest)
                .onSuccess { transactionId ->
                    val message = "Payment successful"
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        successMessage = message,
                        errorMessage = null
                    )
                    SnackbarManager.showSuccess(message)
                    onSuccess(transactionId)
                }
                .onFailure { error ->
                    val message = error.message ?: "Payment failed"
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        errorMessage = message,
                        successMessage = null
                    )
                    SnackbarManager.showError(message)
                }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    fun getCardById(cardId: String): Card? {
        return repository.getCardById(cardId)
    }

    fun processOnlinePayment(
        cardId: String,
        amount: Double,
        merchantName: String,
        cvv: String,
        category: String? = null,
        onSuccess: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessingPayment = true,
                errorMessage = null
            )

            repository.processOnlinePayment(cardId, amount, merchantName, cvv, category)
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        successMessage = result.message
                    )
                    SnackbarManager.showSuccess(result.message)
                    onSuccess(result.transactionId ?: "")
                }
                .onFailure { error ->
                    val message = error.message ?: "Online payment failed"
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        errorMessage = message
                    )
                    SnackbarManager.showError(message)
                }
        }
    }

    fun processPOSTransaction(
        cardId: String,
        amount: Double,
        merchantName: String,
        pin: String,
        onSuccess: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessingPayment = true,
                errorMessage = null
            )

            repository.processPOSTransaction(cardId, amount, merchantName, pin)
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        successMessage = result.message
                    )
                    SnackbarManager.showSuccess(result.message)
                    onSuccess(result.transactionId ?: "")
                }
                .onFailure { error ->
                    val message = error.message ?: "POS transaction failed"
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        errorMessage = message
                    )
                    SnackbarManager.showError(message)
                }
        }
    }

    fun processBillPayment(
        cardId: String,
        amount: Double,
        billType: String,
        billerName: String,
        accountNumber: String,
        cvv: String,
        onSuccess: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessingPayment = true,
                errorMessage = null
            )

            repository.processBillPayment(cardId, amount, billType, billerName, accountNumber, cvv)
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        successMessage = result.message
                    )
                    SnackbarManager.showSuccess(result.message)
                    onSuccess(result.transactionId ?: "")
                }
                .onFailure { error ->
                    val message = error.message ?: "Bill payment failed"
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        errorMessage = message
                    )
                    SnackbarManager.showError(message)
                }
        }
    }

    fun processCardTransfer(
        cardId: String,
        amount: Double,
        destinationAccountNumber: String,
        description: String,
        cvv: String,
        onSuccess: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessingPayment = true,
                errorMessage = null
            )

            repository.processCardTransfer(cardId, amount, destinationAccountNumber, description, cvv)
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        successMessage = result.message
                    )
                    SnackbarManager.showSuccess(result.message)
                    onSuccess(result.transactionId ?: "")
                }
                .onFailure { error ->
                    val message = error.message ?: "Card transfer failed"
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        errorMessage = message
                    )
                    SnackbarManager.showError(message)
                }
        }
    }

    fun processATMWithdrawal(
        cardId: String,
        amount: Double,
        atmLocation: String,
        pin: String,
        onSuccess: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessingPayment = true,
                errorMessage = null
            )

            repository.processATMWithdrawal(cardId, amount, atmLocation, pin)
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        successMessage = result.message
                    )
                    SnackbarManager.showSuccess(result.message)
                    onSuccess(result.transactionId ?: "")
                }
                .onFailure { error ->
                    val message = error.message ?: "ATM withdrawal failed"
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        errorMessage = message
                    )
                    SnackbarManager.showError(message)
                }
        }
    }
}
