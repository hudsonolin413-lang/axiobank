package org.dals.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.dals.project.model.*
import org.dals.project.repository.AuthRepository
import org.dals.project.repository.TransactionRepository
import org.dals.project.utils.getPaymentMethodName
import org.dals.project.utils.AccountStatementUtils
import org.dals.project.utils.DateTimeUtils
import org.dals.project.repository.NotificationRepository
import org.dals.project.utils.DateRangePeriod
import org.dals.project.utils.StatementFormat
import org.dals.project.utils.StatementSummary
import org.dals.project.utils.NetworkConnectivityManager
import org.dals.project.utils.SnackbarManager

data class TransactionUiState(
    val isLoading: Boolean = false,
    val isConfirming: Boolean = false,
    val confirmationAmount: Double = 0.0,
    val confirmationRecipient: String = "",
    val confirmationError: String? = null,
    val transactions: List<Transaction> = emptyList(),
    val walletBalance: WalletBalance? = null,
    val accounts: List<Account> = emptyList(),
    val billPayments: List<BillPayment> = emptyList(),
    val investments: List<Investment> = emptyList(),
    val savingsAccounts: List<SavingsAccount> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val generatedStatement: String? = null,
    val statementFileName: String? = null
)

class TransactionViewModel(
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository? = null
) : ViewModel() {

    private val repository = TransactionRepository(authRepository)
    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    private var inactivityManager: org.dals.project.utils.InactivityManager? = null
    private val networkManager = NetworkConnectivityManager.getInstance()

    init {
        loadInitialData()
        setupNetworkMonitoring()
    }

    /**
     * Setup network connectivity monitoring and auto-refresh
     */
    private fun setupNetworkMonitoring() {
        // Register data refresh callback for when network is restored
        networkManager.registerDataRefreshCallback {
            refreshAllData()
        }

        // Start monitoring network connectivity
        networkManager.startMonitoring()
    }

    fun setInactivityManager(manager: org.dals.project.utils.InactivityManager) {
        this.inactivityManager = manager
    }

    private fun resetInactivityTimer() {
        inactivityManager?.resetTimer()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Collect data from repository
                launch {
                    repository.transactions.collect { transactions ->
                        _uiState.value = _uiState.value.copy(transactions = transactions)
                    }
                }

                launch {
                    repository.walletBalance.collect { balance ->
                        _uiState.value = _uiState.value.copy(walletBalance = balance)
                    }
                }

                launch {
                    repository.accounts.collect { accounts ->
                        _uiState.value = _uiState.value.copy(accounts = accounts)
                    }
                }

                launch {
                    repository.billPayments.collect { bills ->
                        _uiState.value = _uiState.value.copy(billPayments = bills)
                    }
                }

                launch {
                    repository.investments.collect { investments ->
                        _uiState.value = _uiState.value.copy(investments = investments)
                    }
                }

            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun sendMoney(
        recipientName: String,
        recipientAccount: String,
        amount: Double,
        description: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val request = SendMoneyRequest(
                recipientName = recipientName,
                recipientAccount = recipientAccount,
                amount = amount,
                description = description
            )

            repository.sendMoney(request)
                .onSuccess { transactionId ->
                    val message = "Money sent successfully! Transaction ID: $transactionId"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = message
                    )
                    SnackbarManager.showSuccess(message)
                    // Refresh notifications after successful transaction
                    viewModelScope.launch {
                        notificationRepository?.refreshNotifications()
                    }
                }
                .onFailure { error ->
                    val message = "Failed to send money: ${error.message}"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = message
                    )
                    SnackbarManager.showError(message)
                }
        }
    }

    fun sendMoneyWithPaymentMethod(
        recipientName: String,
        recipientAccount: String,
        amount: Double,
        description: String,
        paymentMethod: PaymentMethod
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val request = SendMoneyRequest(
                recipientName = recipientName,
                recipientAccount = recipientAccount,
                amount = amount,
                description = description,
                paymentMethod = paymentMethod
            )

            repository.sendMoney(request)
                .onSuccess { transactionId ->
                    val paymentMethodName = getPaymentMethodName(paymentMethod)
                    val message = "Money sent successfully via $paymentMethodName! Transaction ID: $transactionId"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = message
                    )
                    SnackbarManager.showSuccess(message)
                    // Refresh notifications after successful transaction
                    viewModelScope.launch {
                        notificationRepository?.refreshNotifications()
                    }
                }
                .onFailure { error ->
                    val message = "Failed to send money: ${error.message}"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = message
                    )
                    SnackbarManager.showError(message)
                }
        }
    }

    fun sendMoneyToAccount(
        recipientName: String,
        recipientAccount: String,
        amount: Double,
        description: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val request = SendMoneyRequest(
                recipientName = recipientName,
                recipientAccount = recipientAccount,
                amount = amount,
                description = description
            )

            repository.sendMoney(request)
                .onSuccess { transactionId ->
                    val message = "Money sent successfully to account $recipientAccount! Transaction ID: $transactionId"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = message
                    )
                    SnackbarManager.showSuccess(message)
                    // Refresh notifications after successful transaction
                    viewModelScope.launch {
                        notificationRepository?.refreshNotifications()
                    }
                }
                .onFailure { error ->
                    val message = "Failed to send money: ${error.message}"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = message
                    )
                    SnackbarManager.showError(message)
                }
        }
    }

    fun sendMoneyToMobile(
        recipientName: String,
        mobileNumber: String,
        amount: Double,
        description: String,
        paymentMethod: PaymentMethod,
        country: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val request = SendMoneyRequest(
                recipientName = recipientName,
                recipientAccount = mobileNumber, // Store mobile number in recipientAccount field
                amount = amount,
                description = description,
                paymentMethod = paymentMethod
            )

            repository.sendMoney(request)
                .onSuccess { transactionId ->
                    val paymentMethodName = getPaymentMethodName(paymentMethod)
                    val message = "Money sent successfully to $mobileNumber via $paymentMethodName! Transaction ID: $transactionId"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = message
                    )
                    SnackbarManager.showSuccess(message)
                }
                .onFailure { error ->
                    val message = "Failed to send money: ${error.message}"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = message
                    )
                    SnackbarManager.showError(message)
                }
        }
    }

    fun receiveMoney(amount: Double, description: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val request = ReceiveMoneyRequest(
                amount = amount,
                description = description
            )

            repository.receiveMoney(request)
                .onSuccess { transactionId ->
                    val message = "Money received successfully! Transaction ID: $transactionId"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = message
                    )
                    SnackbarManager.showSuccess(message)
                    // Refresh notifications after successful transaction
                    viewModelScope.launch {
                        notificationRepository?.refreshNotifications()
                    }
                }
                .onFailure { error ->
                    val message = "Failed to receive money: ${error.message}"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = message
                    )
                    SnackbarManager.showError(message)
                }
        }
    }

    fun payBill(billId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            repository.payBill(billId)
                .onSuccess { transactionId ->
                    val message = "Bill paid successfully! Transaction ID: $transactionId"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = message
                    )
                    SnackbarManager.showSuccess(message)
                    // Refresh notifications after successful transaction
                    viewModelScope.launch {
                        notificationRepository?.refreshNotifications()
                    }
                }
                .onFailure { error ->
                    val message = "Failed to pay bill: ${error.message}"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = message
                    )
                    SnackbarManager.showError(message)
                }
        }
    }

    fun payManualBill(
        paybillNumber: String,
        amount: Double,
        billType: BillType,
        description: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            repository.payManualBill(paybillNumber, amount, billType, description)
                .onSuccess { transactionId ->
                    val message = "Bill payment successful! Transaction ID: $transactionId"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = message
                    )
                    SnackbarManager.showSuccess(message)
                    // Refresh notifications after successful transaction
                    viewModelScope.launch {
                        notificationRepository?.refreshNotifications()
                    }
                }
                .onFailure { error ->
                    val message = "Failed to pay bill: ${error.message}"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = message
                    )
                    SnackbarManager.showError(message)
                }
        }
    }

    fun buyGoods(
        tillNumber: String,
        amount: Double,
        accountReference: String,
        remarks: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            repository.buyGoods(tillNumber, amount, accountReference, remarks)
                .onSuccess { transactionId ->
                    val message = "Payment to till $tillNumber successful! Transaction ID: $transactionId"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = message
                    )
                    SnackbarManager.showSuccess(message)
                    // Refresh notifications after successful transaction
                    viewModelScope.launch {
                        notificationRepository?.refreshNotifications()
                    }
                }
                .onFailure { error ->
                    val message = "Failed to pay: ${error.message}"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = message
                    )
                    SnackbarManager.showError(message)
                }
        }
    }

    fun makeInvestment(type: InvestmentType, name: String, amount: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            repository.makeInvestment(type, name, amount)
                .onSuccess { investmentId ->
                    val message = "Investment made successfully! Investment ID: $investmentId"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = message
                    )
                    SnackbarManager.showSuccess(message)
                    // Reload investments to show the new one
                    loadInvestments()
                }
                .onFailure { error ->
                    val message = "Failed to make investment: ${error.message}"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = message
                    )
                    SnackbarManager.showError(message)
                }
        }
    }

    fun loadInvestments() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                // Investments are already loaded via repository flow in loadInitialData
                // This method can be used to force a refresh if needed
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Investments loaded successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load investments: ${e.message}"
                )
            }
        }
    }

    fun withdrawFromInvestment(investment: Investment, amount: Double, paymentMethod: PaymentMethod) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                // For now, use the existing withdraw method
                // In a full implementation, this would call a specific investment withdrawal endpoint
                withdraw(amount, "Withdrawal from ${investment.name}", paymentMethod)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Successfully withdrew $${amount} from ${investment.name}"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to withdraw from investment: ${e.message}"
                )
            }
        }
    }

    fun withdraw(amount: Double, description: String, paymentMethod: PaymentMethod, phoneNumber: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            repository.withdraw(amount, description, paymentMethod, phoneNumber)
                .onSuccess { message ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = message
                    )
                    SnackbarManager.showSuccess(message)
                    // Refresh notifications after successful transaction
                    viewModelScope.launch {
                        notificationRepository?.refreshNotifications()
                    }
                }
                .onFailure { error ->
                    val message = "Failed to withdraw: ${error.message}"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = message
                    )
                    SnackbarManager.showError(message)
                }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null,
            isConfirming = false,
            confirmationError = null
        )
    }

    private var pendingTransaction: (suspend () -> Unit)? = null

    fun initiateTransaction(
        amount: Double,
        recipient: String,
        action: suspend () -> Unit
    ) {
        resetInactivityTimer()
        pendingTransaction = action
        _uiState.value = _uiState.value.copy(
            isConfirming = true,
            confirmationAmount = amount,
            confirmationRecipient = recipient,
            confirmationError = null
        )
    }

    fun confirmTransaction(password: String) {
        resetInactivityTimer()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, confirmationError = null)
            
            val isPasswordValid = authRepository.verifyPassword(password)
            
            if (isPasswordValid) {
                pendingTransaction?.invoke()
                _uiState.value = _uiState.value.copy(
                    isConfirming = false,
                    isLoading = false
                )
                pendingTransaction = null
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    confirmationError = "Invalid password. Please try again."
                )
            }
        }
    }

    fun cancelTransaction() {
        pendingTransaction = null
        _uiState.value = _uiState.value.copy(
            isConfirming = false,
            confirmationAmount = 0.0,
            confirmationRecipient = "",
            confirmationError = null
        )
    }

    fun getTransactionById(id: String): Transaction? {
        return repository.getTransactionById(id)
    }

    fun getBillById(id: String): BillPayment? {
        return repository.getBillById(id)
    }

    fun getInvestmentById(id: String): Investment? {
        return repository.getInvestmentById(id)
    }

    fun generateStatement(
        periodStart: LocalDateTime,
        periodEnd: LocalDateTime,
        format: StatementFormat = StatementFormat.TXT
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val currentUser = authRepository.currentUser.value
                if (currentUser == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "User not authenticated"
                    )
                    return@launch
                }

                val transactions = _uiState.value.transactions
                val walletBalance = _uiState.value.walletBalance

                if (walletBalance == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Wallet balance not available"
                    )
                    return@launch
                }

                // Filter transactions for the specified period
                val filteredTransactions = transactions.filter { transaction ->
                    val transactionDate = DateTimeUtils.parseDateTime(transaction.timestamp)
                    transactionDate != null &&
                            transactionDate >= periodStart &&
                            transactionDate <= periodEnd
                }

                val statement = if (format == StatementFormat.CSV) {
                    AccountStatementUtils.generateCSVStatement(filteredTransactions, walletBalance)
                } else {
                    AccountStatementUtils.generateStatement(
                        userFullName = currentUser.fullName,
                        accountNumber = currentUser.id, // Using user ID as account number
                        transactions = filteredTransactions,
                        walletBalance = walletBalance,
                        periodStart = periodStart,
                        periodEnd = periodEnd
                    )
                }

                val fileName = AccountStatementUtils.generateStatementFileName(
                    accountNumber = currentUser.id,
                    periodStart = periodStart,
                    periodEnd = periodEnd,
                    format = format
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    generatedStatement = statement,
                    statementFileName = fileName,
                    successMessage = "Statement generated successfully"
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to generate statement: ${e.message}"
                )
            }
        }
    }

    fun generateStatementForPeriod(
        period: DateRangePeriod,
        format: StatementFormat = StatementFormat.TXT
    ) {
        viewModelScope.launch {
            try {
                // Use simplified date ranges for demo purposes
                val (startDate, endDate) = when (period) {
                    DateRangePeriod.TODAY -> {
                        val start = LocalDateTime(2024, 12, 15, 0, 0, 0)
                        val end = LocalDateTime(2024, 12, 15, 23, 59, 59)
                        Pair(start, end)
                    }
                    DateRangePeriod.THIS_WEEK -> {
                        val start = LocalDateTime(2024, 12, 9, 0, 0, 0)
                        val end = LocalDateTime(2024, 12, 15, 23, 59, 59)
                        Pair(start, end)
                    }
                    DateRangePeriod.THIS_MONTH -> {
                        val start = LocalDateTime(2024, 12, 1, 0, 0, 0)
                        val end = LocalDateTime(2024, 12, 31, 23, 59, 59)
                        Pair(start, end)
                    }
                    DateRangePeriod.LAST_30_DAYS -> {
                        val start = LocalDateTime(2024, 11, 15, 0, 0, 0)
                        val end = LocalDateTime(2024, 12, 15, 23, 59, 59)
                        Pair(start, end)
                    }
                    DateRangePeriod.THIS_YEAR -> {
                        val start = LocalDateTime(2024, 1, 1, 0, 0, 0)
                        val end = LocalDateTime(2024, 12, 31, 23, 59, 59)
                        Pair(start, end)
                    }
                }

                generateStatement(startDate, endDate, format)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to generate statement: ${e.message}"
                )
            }
        }
    }

    fun getStatementSummary(period: DateRangePeriod): StatementSummary? {
        val transactions = _uiState.value.transactions
        val walletBalance = _uiState.value.walletBalance ?: return null

        return AccountStatementUtils.generateSummaryStatement(
            transactions = transactions,
            walletBalance = walletBalance,
            period = period
        )
    }

    fun clearStatement() {
        _uiState.value = _uiState.value.copy(
            generatedStatement = null,
            statementFileName = null
        )
    }
    suspend fun recordMpesaDeposit(
        amount: Double,
        phoneNumber: String,
        mpesaReference: String,
        accountNumber: String
    ): Result<String> {
        return repository.recordMpesaDeposit(amount, phoneNumber, mpesaReference, accountNumber)
    }

    fun refreshData() {
        loadInitialData()
    }

    // Savings Account Methods
    fun loadSavingsAccounts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val result = repository.getSavingsAccounts()
                result.onSuccess { savings ->
                    _uiState.value = _uiState.value.copy(
                        savingsAccounts = savings,
                        isLoading = false
                    )
                }.onFailure { error ->
                    val message = "Failed to load savings accounts: ${error.message}"
                    _uiState.value = _uiState.value.copy(
                        errorMessage = message,
                        isLoading = false
                    )
                    SnackbarManager.showError(message)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load savings accounts: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun createSavingsAccount(accountName: String, amount: Double, lockPeriod: LockPeriod) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val result = repository.createSavingsAccount(accountName, amount, lockPeriod)

                result.onSuccess {
                    val message = "Savings account created successfully!"
                    _uiState.value = _uiState.value.copy(
                        successMessage = message
                    )
                    SnackbarManager.showSuccess(message)
                    loadSavingsAccounts()
                }.onFailure { error ->
                    val message = "Failed to create savings: ${error.message}"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = message
                    )
                    SnackbarManager.showError(message)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to create savings: ${e.message}"
                )
            }
        }
    }

    fun withdrawSavings(savingsId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val result = repository.withdrawSavings(savingsId)

                result.onSuccess { totalAmount ->
                    val message = "Withdrawn $${"%.2f".format(totalAmount)} successfully!"
                    _uiState.value = _uiState.value.copy(
                        successMessage = message
                    )
                    SnackbarManager.showSuccess(message)
                    loadSavingsAccounts()
                }.onFailure { error ->
                    val message = "Failed to withdraw: ${error.message}"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = message
                    )
                    SnackbarManager.showError(message)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to withdraw: ${e.message}"
                )
            }
        }
    }

    fun getSavingsById(id: String): SavingsAccount? {
        return _uiState.value.savingsAccounts.firstOrNull { it.id == id }
    }

    /**
     * Refresh all transaction and account data from server
     */
    fun refreshAllData() {
        viewModelScope.launch {
            try {
        //        println("🔄 Refreshing all transaction data...")
                repository.refreshData()
                // Also refresh notifications
                notificationRepository?.refreshNotifications()
//                println("✅ Data refresh complete")
            } catch (e: Exception) {
                println("❌ Error refreshing data: ${e.message}")
            }
        }
    }

    /**
     * Manual refresh for pull-to-refresh functionality
     */
    fun manualRefresh() {
        refreshAllData()
    }

    /**
     * Download statement and send to user's email as encrypted PDF with custom date range
     */
    suspend fun sendStatementEmail(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        email: String? = null
    ): Result<String> {
        return try {
            val currentUser = authRepository.currentUser.value
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }

            // Call server API to generate and email statement
            repository.downloadAndEmailStatement(
                customerId = currentUser.id,
                accountId = currentUser.accountNumber ?: currentUser.id,
                startDate = startDate,
                endDate = endDate,
                email = email
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Download statement and send to user's email as encrypted PDF
     */
    fun downloadAndEmailStatement(period: DateRangePeriod, email: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val currentUser = authRepository.currentUser.value
                if (currentUser == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "User not authenticated"
                    )
                    return@launch
                }

                // Get date range based on period
                val (startDate, endDate) = when (period) {
                    DateRangePeriod.TODAY -> {
                        val start = LocalDateTime(2024, 12, 15, 0, 0, 0)
                        val end = LocalDateTime(2024, 12, 15, 23, 59, 59)
                        Pair(start, end)
                    }
                    DateRangePeriod.THIS_WEEK -> {
                        val start = LocalDateTime(2024, 12, 9, 0, 0, 0)
                        val end = LocalDateTime(2024, 12, 15, 23, 59, 59)
                        Pair(start, end)
                    }
                    DateRangePeriod.THIS_MONTH -> {
                        val start = LocalDateTime(2024, 12, 1, 0, 0, 0)
                        val end = LocalDateTime(2024, 12, 31, 23, 59, 59)
                        Pair(start, end)
                    }
                    DateRangePeriod.LAST_30_DAYS -> {
                        val start = LocalDateTime(2024, 11, 15, 0, 0, 0)
                        val end = LocalDateTime(2024, 12, 15, 23, 59, 59)
                        Pair(start, end)
                    }
                    DateRangePeriod.THIS_YEAR -> {
                        val start = LocalDateTime(2024, 1, 1, 0, 0, 0)
                        val end = LocalDateTime(2024, 12, 31, 23, 59, 59)
                        Pair(start, end)
                    }
                }

                // Call server API to generate and email statement
                // The server will generate encrypted PDF and send to user's email
                val result = repository.downloadAndEmailStatement(
                    customerId = currentUser.id,
                    accountId = currentUser.accountNumber ?: currentUser.id,
                    startDate = startDate,
                    endDate = endDate,
                    email = email
                )

                result.onSuccess { message ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Statement has been sent successfully! The PDF is encrypted with the last 4 digits of your account number."
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to send statement: ${error.message}"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to send statement: ${e.message}"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up network monitoring when view model is destroyed
        val networkManager = NetworkConnectivityManager.getInstance()
        networkManager.registerDataRefreshCallback {
            // Unregister callback
        }
    }
}
