package org.dals.project.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.dals.project.model.*
import org.dals.project.utils.DateTimeUtils
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TransactionsResponse(
    val success: Boolean,
    val message: String,
    val data: List<ServerTransactionData>? = null,
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 10
)

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val error: String? = null
)

@Serializable
data class ServerTransactionData(
    val id: String,
    val accountId: String,
    val type: String,
    val amount: Double,
    val status: String,
    val description: String,
    val category: String? = null,
    val createdAt: String,
    val processedAt: String? = null,
    val toAccountId: String? = null,
    val reference: String? = null // Changed from referenceNumber to match server response
)

@Serializable
data class CreateTransactionRequest(
    val accountId: String,
    val type: String,
    val amount: String,
    val description: String,
    val fromAccountId: String? = null,
    val toAccountId: String? = null,
    val reference: String? = null,
    val processedBy: String? = null,
    val branchId: String? = null,
    val checkNumber: String? = null,
    val merchantName: String? = null,
    val category: String? = null
)

@Serializable
data class GenerateStatementRequest(
    val customerId: String,
    val accountId: String,
    val startDate: String,
    val endDate: String,
    val sendEmail: Boolean = true,
    val email: String? = null
)

class TransactionRepository(
    private val authRepository: AuthRepository
) {
    private val baseUrl = "http://localhost:8081/api/v1"

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _walletBalance = MutableStateFlow<WalletBalance?>(null)
    val walletBalance: StateFlow<WalletBalance?> = _walletBalance.asStateFlow()

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _billPayments = MutableStateFlow<List<BillPayment>>(emptyList())
    val billPayments: StateFlow<List<BillPayment>> = _billPayments.asStateFlow()

    private val _investments = MutableStateFlow<List<Investment>>(emptyList())
    val investments: StateFlow<List<Investment>> = _investments.asStateFlow()

    private val _savingsAccounts = MutableStateFlow<List<SavingsAccount>>(emptyList())
    val savingsAccounts: StateFlow<List<SavingsAccount>> = _savingsAccounts.asStateFlow()

    init {
        // Listen for user changes and fetch real data when user logs in
        CoroutineScope(Dispatchers.Default).launch {
            authRepository.currentUser.collect { user ->
                if (user != null) {
                    println("📊 User logged in, fetching real data for customer: ${user.id}")
                    fetchAccountBalanceFromServer(user.id)
                    fetchTransactionsFromServer(user.id)
                } else {
                    println("📊 User logged out, clearing data")
                    clearData()
                }
            }
        }
    }

    private suspend fun fetchAccountBalanceFromServer(customerId: String) {
        try {
//            println("🌐 Fetching account balance for customer: $customerId")

            val response = httpClient.get("$baseUrl/customer-care/accounts/customer/$customerId") {
                contentType(ContentType.Application.Json)
                headers {
                    authRepository.getAuthToken()?.let { token ->
                        append("Authorization", "Bearer $token")
                    }
                }
            }

            if (response.status == HttpStatusCode.OK) {
                val accountResponse = response.body<AccountBalanceResponse>()

                if (accountResponse.success && !accountResponse.data.isNullOrEmpty()) {
                    val serverAccounts = accountResponse.data

                    // Calculate total balance from all accounts
                    val totalBalance = serverAccounts.sumOf { it.balance }
                    val availableBalance = serverAccounts.sumOf { it.availableBalance }

                    _walletBalance.value = WalletBalance(
                        userId = customerId,
                        totalBalance = totalBalance,
                        availableBalance = availableBalance,
                        pendingAmount = totalBalance - availableBalance,
                        currency = "USD",
                        lastUpdated = getCurrentDateString()
                    )

                    // Convert server accounts to app Account model
                    _accounts.value = serverAccounts.map { serverAccount ->
                        Account(
                            id = serverAccount.id,
                            name = formatAccountName(serverAccount.type),
                            type = serverAccount.type,
                            accountNumber = serverAccount.accountNumber,
                            balance = serverAccount.balance,
                            currency = "USD"
                        )
                    }

    //                println("✅ Account balance fetched: Total = $$totalBalance, Available = $$availableBalance")
//                    println("✅ Fetched ${_accounts.value.size} accounts")
                } else {
//                    println("⚠️ No accounts found for customer")
                    // Set zero balance
                    _walletBalance.value = WalletBalance(
                        userId = customerId,
                        totalBalance = 0.0,
                        availableBalance = 0.0,
                        pendingAmount = 0.0,
                        currency = "USD",
                        lastUpdated = getCurrentDateString()
                    )
                }
            } else {
                println("❌ Failed to fetch account balance: ${response.status}")
            }
        } catch (e: Exception) {
            println("❌ Error fetching account balance: ${e.message}")
            e.printStackTrace()
            // Fallback to zero balance on error
            _walletBalance.value = WalletBalance(
                userId = customerId,
                totalBalance = 0.0,
                availableBalance = 0.0,
                pendingAmount = 0.0,
                currency = "USD",
                lastUpdated = getCurrentDateString()
            )
        }
    }

    private suspend fun fetchTransactionsFromServer(customerId: String) {
        try {
            println("🌐 Fetching transactions for customer: $customerId")

            val response = httpClient.get("$baseUrl/transactions/customer/$customerId") {
                contentType(ContentType.Application.Json)
                headers {
                    authRepository.getAuthToken()?.let { token ->
                        append("Authorization", "Bearer $token")
                    }
                }
            }

            if (response.status == HttpStatusCode.OK) {
                val transactionsResponse = response.body<TransactionsResponse>()

                if (transactionsResponse.success && !transactionsResponse.data.isNullOrEmpty()) {
                    val serverTransactions = transactionsResponse.data

                    // Convert server transactions to app model
                    val appTransactions = serverTransactions.map { serverTxn ->
                        Transaction(
                            id = serverTxn.id,
                            userId = customerId,
                            type = parseTransactionType(serverTxn.type),
                            category = parseTransactionCategory(serverTxn.category),
                            amount = serverTxn.amount,
                            description = serverTxn.description,
                            status = parseTransactionStatus(serverTxn.status),
                            timestamp = serverTxn.createdAt,
                            reference = serverTxn.reference // Now correctly maps from server
                        )
                    }

                    _transactions.value = appTransactions
                    println("✅ Fetched ${appTransactions.size} transactions")
                } else {
//                    println("ℹ️ No transactions found for customer")
                    _transactions.value = emptyList()
                }
            } else {
                println("❌ Failed to fetch transactions: ${response.status}")
                _transactions.value = emptyList()
            }
        } catch (e: Exception) {
            println("❌ Error fetching transactions: ${e.message}")
            e.printStackTrace()
            _transactions.value = emptyList()
        }
    }

    private fun clearData() {
        _walletBalance.value = null
        _transactions.value = emptyList()
        _accounts.value = emptyList()
        _billPayments.value = emptyList()
        _investments.value = emptyList()
    }

    private fun formatAccountName(type: String): String {
        return when (type.uppercase()) {
            "CHECKING" -> "Main Wallet"
            "SAVINGS" -> "Savings Account"
            "MONEY_MARKET" -> "Money Market"
            "BUSINESS" -> "Business Account"
            else -> type
        }
    }

    private fun parseTransactionType(type: String): TransactionType {
        return when (type.uppercase()) {
            "DEPOSIT" -> TransactionType.RECEIVE
            "WITHDRAWAL" -> TransactionType.WITHDRAWAL
            "TRANSFER" -> TransactionType.SEND
            "PAYMENT" -> TransactionType.BILL_PAYMENT
            else -> TransactionType.SEND
        }
    }

    private fun parseTransactionCategory(category: String?): TransactionCategory {
        return when (category?.uppercase()) {
            "TRANSFER" -> TransactionCategory.TRANSFER
            "UTILITIES" -> TransactionCategory.UTILITIES
            "RENT" -> TransactionCategory.RENT
            "BILLS" -> TransactionCategory.BILLS
            "INVESTMENT" -> TransactionCategory.INVESTMENT
            else -> TransactionCategory.TRANSFER
        }
    }

    private fun parseTransactionStatus(status: String): TransactionStatus {
        return when (status.uppercase()) {
            "COMPLETED", "SUCCESS" -> TransactionStatus.COMPLETED
            "PENDING", "MPESA_PENDING" -> TransactionStatus.PENDING
            "FAILED", "MPESA_FAILED", "CANCELLED", "TIMEOUT" -> TransactionStatus.FAILED
            "REVERSED" -> TransactionStatus.REVERSED
            else -> TransactionStatus.PENDING
        }
    }

    private fun getCurrentDateString(): String {
        return DateTimeUtils.getCurrentTimestamp()
    }

    suspend fun sendMoney(request: SendMoneyRequest): Result<String> {
        return try {
            val currentUser = authRepository.currentUser.value
                ?: return Result.failure(Exception("User not authenticated"))

            // Get primary account
            val accountsResponse = httpClient.get("$baseUrl/customer-care/accounts/customer/${currentUser.id}") {
                contentType(ContentType.Application.Json)
                headers {
                    authRepository.getAuthToken()?.let { token ->
                        append("Authorization", "Bearer $token")
                    }
                }
            }.body<AccountBalanceResponse>()

            val primaryAccount = accountsResponse.data?.firstOrNull { it.status == "ACTIVE" }
                ?: return Result.failure(Exception("No active account found"))

            // Check if recipient is a phone number (for mobile money transfer)
            // Phone numbers: 10-12 digits, may start with +, 254, or 07
            // Bank accounts: typically start with letters (ACC, SAV) or are UUIDs with dashes
            val cleanedRecipient = request.recipientAccount.replace(Regex("[^0-9]"), "")
            val hasLetters = request.recipientAccount.any { it.isLetter() }
            val isPhoneNumber = !hasLetters &&
                                cleanedRecipient.length in 9..15 &&
                                (request.paymentMethod == PaymentMethod.MPESA || request.paymentMethod == PaymentMethod.AIRTEL)

            if (isPhoneNumber) {
                // Send to mobile money via B2C
                return sendMoneyToMobileViaB2C(
                    phoneNumber = request.recipientAccount,
                    amount = request.amount,
                    accountNumber = primaryAccount.accountNumber,
                    customerId = currentUser.id,
                    remarks = request.description
                )
            }

            // Otherwise, send as regular account transfer
            // Need to look up the recipient account ID from account number
            val recipientAccountResponse = try {
                httpClient.get("$baseUrl/teller/account/lookup?accountNumber=${request.recipientAccount}") {
                    contentType(ContentType.Application.Json)
                    headers {
                        authRepository.getAuthToken()?.let { token ->
                            append("Authorization", "Bearer $token")
                        }
                    }
                }
            } catch (e: Exception) {
                return Result.failure(Exception("Recipient account not found: ${request.recipientAccount}"))
            }

            if (recipientAccountResponse.status != HttpStatusCode.OK) {
                return Result.failure(Exception("Recipient account not found: ${request.recipientAccount}"))
            }

            @Serializable
            data class AccountResponse(
                val success: Boolean,
                val message: String,
                val data: ServerAccountData? = null
            )

            val recipientAccount = recipientAccountResponse.body<AccountResponse>().data
                ?: return Result.failure(Exception("Recipient account not found"))

            val createTransactionRequest = CreateTransactionRequest(
                accountId = primaryAccount.id,
                type = "TRANSFER",
                amount = request.amount.toString(),
                description = request.description,
                fromAccountId = primaryAccount.id,
                toAccountId = recipientAccount.id,
                reference = null, // Let server generate the reference in new format
                merchantName = request.recipientName,
                category = "TRANSFER"
            )

            val response = httpClient.post("$baseUrl/transactions") {
                header("Authorization", "Bearer ${authRepository.getAuthToken()}")
                contentType(ContentType.Application.Json)
                setBody(createTransactionRequest)
            }

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                // Refresh balance and transactions
                fetchAccountBalanceFromServer(currentUser.id)
                fetchTransactionsFromServer(currentUser.id)

                Result.success("Transaction completed successfully")
            } else {
                // Try to parse error response
                val errorMessage = try {
                    val errorResponse = response.body<ApiResponse<String>>()
                    errorResponse.error ?: errorResponse.message
                } catch (e: Exception) {
                    "Transaction failed: ${response.status}"
                }
                println("❌ Transaction failed: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            println("❌ Send money error: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun sendMoneyToMobileViaB2C(
        phoneNumber: String,
        amount: Double,
        accountNumber: String,
        customerId: String,
        remarks: String
    ): Result<String> {
        return try {
            @Serializable
            data class MpesaWithdrawalRequest(
                val phoneNumber: String,
                val amount: Double,
                val accountNumber: String,
                val customerId: String,
                val remarks: String? = null
            )

            val withdrawalRequest = MpesaWithdrawalRequest(
                phoneNumber = phoneNumber,
                amount = amount,
                accountNumber = accountNumber,
                customerId = customerId,
                remarks = remarks.ifBlank { "Transfer to mobile" }
            )

            val response = httpClient.post("$baseUrl/mpesa/withdraw") {
                header("Authorization", "Bearer ${authRepository.getAuthToken()}")
                contentType(ContentType.Application.Json)
                setBody(withdrawalRequest)
            }

            if (response.status == HttpStatusCode.OK) {
                // Refresh balance and transactions
                fetchAccountBalanceFromServer(customerId)
                fetchTransactionsFromServer(customerId)
                Result.success("Money sent to $phoneNumber successfully. It will be delivered shortly.")
            } else {
                Result.failure(Exception("Mobile transfer failed: ${response.status}"))
            }
        } catch (e: Exception) {
            println("❌ Mobile transfer error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun receiveMoney(request: ReceiveMoneyRequest): Result<String> {
        return try {
            val currentUser = authRepository.currentUser.value
                ?: return Result.failure(Exception("User not authenticated"))

            // Get primary account
            val accountsResponse = httpClient.get("$baseUrl/customer-care/accounts/customer/${currentUser.id}") {
                contentType(ContentType.Application.Json)
                headers {
                    authRepository.getAuthToken()?.let { token ->
                        append("Authorization", "Bearer $token")
                    }
                }
            }.body<AccountBalanceResponse>()

            val primaryAccount = accountsResponse.data?.firstOrNull { it.status == "ACTIVE" }
                ?: return Result.failure(Exception("No active account found"))

            // Create deposit transaction via API
            val createTransactionRequest = CreateTransactionRequest(
                accountId = primaryAccount.id,
                type = "DEPOSIT",
                amount = request.amount.toString(),
                description = request.description,
                reference = null, // Let server generate the reference in new format
                category = "TRANSFER"
            )

            val response = httpClient.post("$baseUrl/transactions") {
                header("Authorization", "Bearer ${authRepository.getAuthToken()}")
                contentType(ContentType.Application.Json)
                setBody(createTransactionRequest)
            }

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                // Refresh balance and transactions
                fetchAccountBalanceFromServer(currentUser.id)
                fetchTransactionsFromServer(currentUser.id)

                Result.success("Money received successfully")
            } else {
                Result.failure(Exception("Transaction failed: ${response.status}"))
            }
        } catch (e: Exception) {
            println("❌ Receive money error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun payBill(billId: String): Result<String> {
        return try {
            delay(2000)

            val currentUser = authRepository.currentUser.value
                ?: return Result.failure(Exception("User not authenticated"))

            val bills = _billPayments.value.toMutableList()
            val billIndex = bills.indexOfFirst { it.id == billId }

            if (billIndex == -1) {
                return Result.failure(Exception("Bill not found"))
            }

            val bill = bills[billIndex]
            val updatedBill = bill.copy(status = PaymentStatus.PAID)
            bills[billIndex] = updatedBill
            _billPayments.value = bills

            // Create transaction record
            val transaction = Transaction(
                id = "txn-${kotlin.random.Random.nextLong()}",
                userId = currentUser.id,
                type = TransactionType.BILL_PAYMENT,
                category = when (bill.billType) {
                    BillType.RENT -> TransactionCategory.RENT
                    BillType.ELECTRICITY, BillType.WATER, BillType.GAS -> TransactionCategory.UTILITIES
                    else -> TransactionCategory.BILLS
                },
                amount = bill.amount,
                description = "${bill.billType.name} payment to ${bill.providerName}",
                recipientName = bill.providerName,
                status = TransactionStatus.COMPLETED,
                timestamp = getCurrentDateString()
            )

            val currentTransactions = _transactions.value.toMutableList()
            currentTransactions.add(0, transaction)
            _transactions.value = currentTransactions

            // Update wallet balance
            updateWalletBalance(-bill.amount)

            Result.success(transaction.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun payManualBill(
        paybillNumber: String,
        amount: Double,
        billType: BillType,
        description: String
    ): Result<String> {
        return try {
            delay(2000)

            val currentUser = authRepository.currentUser.value
                ?: return Result.failure(Exception("User not authenticated"))

            // Create transaction record for manual bill payment
            val transaction = Transaction(
                id = "txn-${kotlin.random.Random.nextLong()}",
                userId = currentUser.id,
                type = TransactionType.BILL_PAYMENT,
                category = when (billType) {
                    BillType.RENT -> TransactionCategory.RENT
                    BillType.ELECTRICITY, BillType.WATER, BillType.GAS -> TransactionCategory.UTILITIES
                    else -> TransactionCategory.BILLS
                },
                amount = amount,
                description = description,
                recipientName = paybillNumber,
                recipientAccount = paybillNumber,
                status = TransactionStatus.COMPLETED,
                timestamp = getCurrentDateString(),
                fee = calculateTransactionFee(amount)
            )

            val currentTransactions = _transactions.value.toMutableList()
            currentTransactions.add(0, transaction)
            _transactions.value = currentTransactions

            // Update wallet balance
            updateWalletBalance(-(amount + transaction.fee))

            Result.success(transaction.id)
        } catch (e: Exception) {
            println("❌ Pay bill error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun buyGoods(
        tillNumber: String,
        amount: Double,
        accountReference: String,
        remarks: String
    ): Result<String> {
        return try {
            delay(2000)

            val currentUser = authRepository.currentUser.value
                ?: return Result.failure(Exception("User not authenticated"))

            // Create transaction record for Buy Goods payment
            val transaction = Transaction(
                id = "txn-${kotlin.random.Random.nextLong()}",
                userId = currentUser.id,
                type = TransactionType.BILL_PAYMENT,
                category = TransactionCategory.SHOPPING,
                amount = amount,
                description = remarks,
                recipientName = "Till $tillNumber",
                recipientAccount = tillNumber,
                status = TransactionStatus.COMPLETED,
                timestamp = getCurrentDateString(),
                fee = calculateTransactionFee(amount)
            )

            val currentTransactions = _transactions.value.toMutableList()
            currentTransactions.add(0, transaction)
            _transactions.value = currentTransactions

            // Update wallet balance
            updateWalletBalance(-(amount + transaction.fee))

            Result.success(transaction.id)
        } catch (e: Exception) {
            println("❌ Buy goods error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun makeInvestment(type: InvestmentType, name: String, amount: Double): Result<String> {
        return try {
            delay(2000)

            val currentUser = authRepository.currentUser.value
                ?: return Result.failure(Exception("User not authenticated"))

            val investment = Investment(
                id = "inv-${kotlin.random.Random.nextLong()}",
                userId = currentUser.id,
                type = type,
                name = name,
                symbol = generateSymbol(name),
                amount = amount,
                currentValue = amount, // Initially same as invested amount
                purchaseDate = getCurrentDateString(),
                currentPrice = amount,
                quantity = 1.0,
                totalReturn = 0.0,
                returnPercentage = 0.0
            )

            val currentInvestments = _investments.value.toMutableList()
            currentInvestments.add(0, investment)
            _investments.value = currentInvestments

            // Create transaction record
            val transaction = Transaction(
                id = "txn-${kotlin.random.Random.nextLong()}",
                userId = currentUser.id,
                type = TransactionType.INVESTMENT,
                category = TransactionCategory.INVESTMENT,
                amount = amount,
                description = "Investment in $name",
                status = TransactionStatus.COMPLETED,
                timestamp = getCurrentDateString()
            )

            val currentTransactions = _transactions.value.toMutableList()
            currentTransactions.add(0, transaction)
            _transactions.value = currentTransactions

            // Update wallet balance
            updateWalletBalance(-amount)

            Result.success(investment.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun withdraw(amount: Double, description: String, paymentMethod: PaymentMethod, phoneNumber: String? = null): Result<String> {
        return try {
            val currentUser = authRepository.currentUser.value
                ?: return Result.failure(Exception("User not authenticated"))

            // If M-Pesa payment method, use the M-Pesa B2C withdrawal API
            if (paymentMethod == PaymentMethod.MPESA) {
                if (phoneNumber.isNullOrBlank()) {
                    return Result.failure(Exception("Phone number is required for M-Pesa withdrawal"))
                }
                return withdrawViaMpesa(amount, phoneNumber, description)
            }

            // For other payment methods, create a simple withdrawal transaction
            delay(1500)

            val transaction = Transaction(
                id = "txn-${kotlin.random.Random.nextLong()}",
                userId = currentUser.id,
                type = TransactionType.WITHDRAWAL,
                category = TransactionCategory.TRANSFER,
                amount = amount,
                description = description,
                status = TransactionStatus.COMPLETED,
                timestamp = getCurrentDateString(),
                fee = calculateTransactionFee(amount),
                reference = "Withdrawn via ${paymentMethod.name}"
            )

            val currentTransactions = _transactions.value.toMutableList()
            currentTransactions.add(0, transaction)
            _transactions.value = currentTransactions

            // Update wallet balance
            updateWalletBalance(-(amount + transaction.fee))

            Result.success(transaction.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun withdrawViaMpesa(amount: Double, phoneNumber: String, remarks: String): Result<String> {
        return try {
            val currentUser = authRepository.currentUser.value
                ?: return Result.failure(Exception("User not authenticated"))

            // Get primary account
            val accountsResponse = httpClient.get("$baseUrl/customer-care/accounts/customer/${currentUser.id}") {
                contentType(ContentType.Application.Json)
                headers {
                    authRepository.getAuthToken()?.let { token ->
                        append("Authorization", "Bearer $token")
                    }
                }
            }.body<AccountBalanceResponse>()

            val primaryAccount = accountsResponse.data?.firstOrNull { it.status == "ACTIVE" }
                ?: return Result.failure(Exception("No active account found"))

            // Call M-Pesa B2C withdrawal API
            @Serializable
            data class MpesaWithdrawalRequest(
                val phoneNumber: String,
                val amount: Double,
                val accountNumber: String,
                val customerId: String,
                val remarks: String? = null
            )

            val withdrawalRequest = MpesaWithdrawalRequest(
                phoneNumber = phoneNumber,
                amount = amount,
                accountNumber = primaryAccount.accountNumber,
                customerId = currentUser.id,
                remarks = remarks.ifBlank { "M-Pesa Withdrawal" }
            )

            val response = httpClient.post("$baseUrl/mpesa/withdraw") {
                header("Authorization", "Bearer ${authRepository.getAuthToken()}")
                contentType(ContentType.Application.Json)
                setBody(withdrawalRequest)
            }

            if (response.status == HttpStatusCode.OK) {
                // Refresh balance and transactions
                fetchAccountBalanceFromServer(currentUser.id)
                fetchTransactionsFromServer(currentUser.id)
                Result.success("M-Pesa withdrawal initiated successfully. Money will be sent to your phone shortly.")
            } else {
                Result.failure(Exception("M-Pesa withdrawal failed: ${response.status}"))
            }
        } catch (e: Exception) {
            println("❌ M-Pesa withdrawal error: ${e.message}")
            Result.failure(e)
        }
    }

    private fun updateWalletBalance(amount: Double) {
        val currentBalance = _walletBalance.value
        if (currentBalance != null) {
            val newBalance = currentBalance.copy(
                totalBalance = currentBalance.totalBalance + amount,
                availableBalance = currentBalance.availableBalance + amount,
                lastUpdated = getCurrentDateString()
            )
            _walletBalance.value = newBalance
        }
    }

    private fun calculateTransactionFee(amount: Double): Double {
        return when {
            amount <= 100 -> 1.0
            amount <= 1000 -> 2.5
            amount <= 5000 -> 5.0
            else -> 10.0
        }
    }

    private fun generateSymbol(name: String): String {
        return name.take(4).uppercase()
    }

    fun getTransactionById(id: String): Transaction? {
        return _transactions.value.find { it.id == id }
    }

    fun getBillById(id: String): BillPayment? {
        return _billPayments.value.find { it.id == id }
    }

    fun getInvestmentById(id: String): Investment? {
        return _investments.value.find { it.id == id }
    }
    /**
     * Record M-Pesa deposit transaction on server
     */
    suspend fun recordMpesaDeposit(
        amount: Double,
        phoneNumber: String,
        mpesaReference: String,
        accountNumber: String
    ): Result<String> {
        return try {
            val currentUser = authRepository.currentUser.value
                ?: return Result.failure(Exception("User not authenticated"))

            val accountsResponse = httpClient.get("$baseUrl/customer-care/accounts/customer/${currentUser.id}") {
                contentType(ContentType.Application.Json)
                headers {
                    authRepository.getAuthToken()?.let { token ->
                        append("Authorization", "Bearer $token")
                    }
                }
            }.body<AccountBalanceResponse>()

            // Find primary account (prefer CHECKING, then SAVINGS, then any active account)
            val primaryAccount = accountsResponse.data?.let { accounts ->
                accounts.firstOrNull { it.type == "CHECKING" && it.status == "ACTIVE" }
                    ?: accounts.firstOrNull { it.type == "SAVINGS" && it.status == "ACTIVE" }
                    ?: accounts.firstOrNull { it.status == "ACTIVE" }
                    ?: accounts.firstOrNull()
            }

            val accountId = primaryAccount?.id
                ?: return Result.failure(Exception("No active account found"))

            val createTransactionRequest = mapOf(
                "accountId" to accountId,
                "type" to "DEPOSIT",
                "amount" to amount.toString(),
                "description" to "M-Pesa Deposit from $phoneNumber",
                "reference" to mpesaReference,
                "merchantName" to "M-Pesa",
                "category" to "DEPOSIT"
            )

            val response = httpClient.post("$baseUrl/transactions") {
                header("Authorization", "Bearer ${authRepository.getAuthToken()}")
                contentType(ContentType.Application.Json)
                setBody(createTransactionRequest)
            }

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                // Refresh both transactions and balance after successful deposit
                fetchAccountBalanceFromServer(currentUser.id)
                fetchTransactionsFromServer(currentUser.id)
                Result.success("Deposit recorded successfully")
            } else {
                Result.failure(Exception("Failed to record deposit: ${response.status}"))
            }
        } catch (e: Exception) {
            println("Error recording M-Pesa deposit: ${e.message}")
            Result.failure(e)
        }
    }

    // Savings Account Methods
    suspend fun getSavingsAccounts(): Result<List<SavingsAccount>> {
        return try {
            val currentUser = authRepository.currentUser.value
                ?: return Result.failure(Exception("User not logged in"))

            println("🌐 Fetching savings accounts for user: ${currentUser.id}")

            val response = httpClient.get("$baseUrl/savings?userId=${currentUser.id}") {
                contentType(ContentType.Application.Json)
                headers {
                    authRepository.getAuthToken()?.let { token ->
                        append("Authorization", "Bearer $token")
                    }
                }
            }

            if (response.status == HttpStatusCode.OK) {
                @Serializable
                data class SavingsResponse(
                    val success: Boolean,
                    val message: String,
                    val savings: List<SavingsAccount>? = null
                )

                val savingsResponse: SavingsResponse = response.body()
                _savingsAccounts.value = savingsResponse.savings ?: emptyList()
                println("✅ Fetched ${savingsResponse.savings?.size ?: 0} savings accounts")
                Result.success(savingsResponse.savings ?: emptyList())
            } else {
                println("❌ Failed to fetch savings: ${response.status}")
                Result.failure(Exception("Failed to fetch savings: ${response.status}"))
            }
        } catch (e: Exception) {
            println("❌ Error fetching savings: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun createSavingsAccount(
        accountName: String,
        amount: Double,
        lockPeriod: LockPeriod
    ): Result<SavingsAccount> {
        return try {
            val currentUser = authRepository.currentUser.value
                ?: return Result.failure(Exception("User not logged in"))

            @Serializable
            data class CreateSavingsRequest(
                val accountName: String,
                val amount: Double,
                val lockPeriod: String
            )

            @Serializable
            data class SavingsResponse(
                val success: Boolean,
                val message: String,
                val data: SavingsAccount? = null
            )

            println("🌐 Creating savings account: $accountName, amount: $amount, period: ${lockPeriod.name}")

            val response = httpClient.post("$baseUrl/savings?userId=${currentUser.id}") {
                contentType(ContentType.Application.Json)
                headers {
                    authRepository.getAuthToken()?.let { token ->
                        append("Authorization", "Bearer $token")
                    }
                }
                setBody(CreateSavingsRequest(accountName, amount, lockPeriod.name))
            }

            if (response.status == HttpStatusCode.Created) {
                val savingsResponse: SavingsResponse = response.body()
                println("✅ Savings account created successfully")
                // Refresh balance
                fetchAccountBalanceFromServer(currentUser.id)
                Result.success(savingsResponse.data!!)
            } else {
                val errorResponse: SavingsResponse = response.body()
                println("❌ Failed to create savings: ${errorResponse.message}")
                Result.failure(Exception(errorResponse.message))
            }
        } catch (e: Exception) {
            println("❌ Error creating savings: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun withdrawSavings(savingsId: String): Result<Double> {
        return try {
            val currentUser = authRepository.currentUser.value
                ?: return Result.failure(Exception("User not logged in"))

            @Serializable
            data class WithdrawResponse(
                val success: Boolean,
                val message: String,
                val totalAmount: Double,
                val isMatured: Boolean
            )

            println("🌐 Withdrawing savings: $savingsId")

            val response = httpClient.post("$baseUrl/savings/$savingsId/withdraw?userId=${currentUser.id}") {
                contentType(ContentType.Application.Json)
                headers {
                    authRepository.getAuthToken()?.let { token ->
                        append("Authorization", "Bearer $token")
                    }
                }
            }

            if (response.status == HttpStatusCode.OK) {
                val withdrawResponse: WithdrawResponse = response.body()
                println("✅ Savings withdrawn successfully: ${withdrawResponse.totalAmount}")
                // Refresh balance
                fetchAccountBalanceFromServer(currentUser.id)
                Result.success(withdrawResponse.totalAmount)
            } else {
                val errorResponse: WithdrawResponse = response.body()
                println("❌ Failed to withdraw savings: ${errorResponse.message}")
                Result.failure(Exception(errorResponse.message))
            }
        } catch (e: Exception) {
            println("❌ Error withdrawing savings: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Refresh all transaction data from server
     */
    suspend fun refreshData() {
        val currentUser = authRepository.currentUser.value
        if (currentUser != null) {
            println("🔄 Refreshing transaction data for customer: ${currentUser.id}")
            fetchAccountBalanceFromServer(currentUser.id)
            fetchTransactionsFromServer(currentUser.id)
        }
    }

    /**
     * Download statement and send to user's email as encrypted PDF
     */
    suspend fun downloadAndEmailStatement(
        customerId: String,
        accountId: String,
        startDate: kotlinx.datetime.LocalDateTime,
        endDate: kotlinx.datetime.LocalDateTime,
        email: String? = null
    ): Result<String> {
        return try {
            val token = authRepository.getAuthToken()
            if (token == null) {
                return Result.failure(Exception("Not authenticated"))
            }

            // Format dates as strings
            val startDateStr = "${startDate.date.year}-${startDate.date.monthNumber.toString().padStart(2, '0')}-${startDate.date.dayOfMonth.toString().padStart(2, '0')}"
            val endDateStr = "${endDate.date.year}-${endDate.date.monthNumber.toString().padStart(2, '0')}-${endDate.date.dayOfMonth.toString().padStart(2, '0')}"

            val requestBody = GenerateStatementRequest(
                customerId = customerId,
                accountId = accountId,
                startDate = startDateStr,
                endDate = endDateStr,
                sendEmail = true,
                email = email
            )

            val response = httpClient.post("http://localhost:8081/api/statements/generate") {
                contentType(ContentType.Application.Json)
                bearerAuth(token)
                setBody(requestBody)
            }.body<ApiResponse<String>>()

            if (response.success) {
                println("✅ ${response.message}")
                Result.success(response.message)
            } else {
                println("❌ Failed to send statement: ${response.message}")
                Result.failure(Exception(response.message))
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            println("❌ Error sending statement: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
