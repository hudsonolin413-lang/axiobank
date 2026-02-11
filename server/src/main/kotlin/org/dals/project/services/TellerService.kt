package org.dals.project.services

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.dals.project.database.*
import org.dals.project.models.*
import org.dals.project.utils.IdGenerator
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
data class TellerCashDrawerResponse(
    val id: String,
    val tellerId: String,
    val tellerName: String,
    val branchId: String,
    val drawerNumber: String,
    val openingBalance: Double,
    val currentBalance: Double,
    val floatAmount: Double,
    val totalDeposits: Double,
    val totalWithdrawals: Double,
    val totalCashIn: Double,
    val totalCashOut: Double,
    val status: String,
    val reconciliationStatus: String,
    val varianceAmount: Double,
    val sessionStartTime: String,
    val sessionEndTime: String?,
    val lastReconciliationDate: String?
)

@Serializable
data class TellerCashDrawerTransactionResponse(
    val id: String,
    val drawerId: String,
    val tellerId: String,
    val transactionType: String,
    val amount: Double,
    val balanceBefore: Double,
    val balanceAfter: Double,
    val customerAccountNumber: String?,
    val customerId: String?,
    val description: String,
    val reference: String?,
    val receiptNumber: String?,
    val chequeNumber: String?,
    val currency: String,
    val transactionDate: String
)

@Serializable
data class CreateTellerCashDrawerRequest(
    val tellerId: String,
    val branchId: String,
    val drawerNumber: String,
    val floatAmount: Double,
    val openingBalance: Double
)

@Serializable
data class TellerTransactionRequest(
    val drawerId: String,
    val transactionType: String, // CASH_IN, CASH_OUT, DEPOSIT, WITHDRAWAL
    val amount: Double,
    val customerAccountNumber: String? = null,
    val customerId: String? = null,
    val description: String,
    val reference: String? = null,
    val receiptNumber: String? = null,
    val chequeNumber: String? = null
)

@Serializable
data class TellerReconciliationRequest(
    val drawerId: String,
    val actualBalance: Double,
    val denominationBreakdown: String? = null,
    val notes: String? = null
)

class TellerCashDrawerService {

    /**
     * Initialize or get existing teller cash drawer for the current session
     */
    fun initializeTellerCashDrawer(
        tellerId: String,
        branchId: String,
        drawerNumber: String,
        floatAmount: Double,
        openingBalance: Double = floatAmount
    ): ApiResponse<TellerCashDrawerResponse> {
        return try {
            transaction {
                // Check if teller already has an active drawer session
                val existingDrawer = TellerCashDrawers.select {
                    (TellerCashDrawers.tellerId eq UUID.fromString(tellerId)) and
                            (TellerCashDrawers.status eq "ACTIVE")
                }.singleOrNull()

                val drawer = if (existingDrawer != null) {
                    // Return existing active drawer
                    existingDrawer
                } else {
                    // Create new drawer session
                    val drawerId = TellerCashDrawers.insertAndGetId {
                        it[TellerCashDrawers.tellerId] = UUID.fromString(tellerId)
                        it[TellerCashDrawers.branchId] = UUID.fromString(branchId)
                        it[TellerCashDrawers.drawerNumber] = drawerNumber
                        it[TellerCashDrawers.floatAmount] = BigDecimal(floatAmount)
                        it[TellerCashDrawers.openingBalance] = BigDecimal(openingBalance)
                        it[TellerCashDrawers.currentBalance] = BigDecimal(openingBalance)
                        it[TellerCashDrawers.status] = "ACTIVE"
                        it[TellerCashDrawers.reconciliationStatus] = "BALANCED"
                    }

                    TellerCashDrawers.select { TellerCashDrawers.id eq drawerId }.single()
                }

                // Get teller name
                val teller = Users.select { Users.id eq UUID.fromString(tellerId) }.single()
                val tellerName = "${teller[Users.firstName]} ${teller[Users.lastName]}"

                ApiResponse(
                    success = true,
                    message = "Teller cash drawer initialized successfully",
                    data = TellerCashDrawerResponse(
                        id = drawer[TellerCashDrawers.id].value.toString(),
                        tellerId = tellerId,
                        tellerName = tellerName,
                        branchId = branchId,
                        drawerNumber = drawer[TellerCashDrawers.drawerNumber],
                        openingBalance = drawer[TellerCashDrawers.openingBalance].toDouble(),
                        currentBalance = drawer[TellerCashDrawers.currentBalance].toDouble(),
                        floatAmount = drawer[TellerCashDrawers.floatAmount].toDouble(),
                        totalDeposits = drawer[TellerCashDrawers.totalDeposits].toDouble(),
                        totalWithdrawals = drawer[TellerCashDrawers.totalWithdrawals].toDouble(),
                        totalCashIn = drawer[TellerCashDrawers.totalCashIn].toDouble(),
                        totalCashOut = drawer[TellerCashDrawers.totalCashOut].toDouble(),
                        status = drawer[TellerCashDrawers.status],
                        reconciliationStatus = drawer[TellerCashDrawers.reconciliationStatus],
                        varianceAmount = drawer[TellerCashDrawers.varianceAmount].toDouble(),
                        sessionStartTime = drawer[TellerCashDrawers.sessionStartTime].toString(),
                        sessionEndTime = drawer[TellerCashDrawers.sessionEndTime]?.toString(),
                        lastReconciliationDate = drawer[TellerCashDrawers.lastReconciliationDate]?.toString()
                    )
                )
            }
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Failed to initialize teller cash drawer: ${e.message}",
                error = e.message
            )
        }
    }

    /**
     * Get teller cash drawer by teller ID
     */
    fun getTellerCashDrawer(tellerId: String): ApiResponse<TellerCashDrawerResponse> {
        return try {
            transaction {
                val result = TellerCashDrawers.innerJoin(Users)
                    .select {
                        (TellerCashDrawers.tellerId eq UUID.fromString(tellerId)) and
                                (TellerCashDrawers.status eq "ACTIVE")
                    }.singleOrNull()

                if (result != null) {
                    val tellerName = "${result[Users.firstName]} ${result[Users.lastName]}"

                    ApiResponse(
                        success = true,
                        message = "Teller cash drawer retrieved successfully",
                        data = TellerCashDrawerResponse(
                            id = result[TellerCashDrawers.id].value.toString(),
                            tellerId = tellerId,
                            tellerName = tellerName,
                            branchId = result[TellerCashDrawers.branchId].toString(),
                            drawerNumber = result[TellerCashDrawers.drawerNumber],
                            openingBalance = result[TellerCashDrawers.openingBalance].toDouble(),
                            currentBalance = result[TellerCashDrawers.currentBalance].toDouble(),
                            floatAmount = result[TellerCashDrawers.floatAmount].toDouble(),
                            totalDeposits = result[TellerCashDrawers.totalDeposits].toDouble(),
                            totalWithdrawals = result[TellerCashDrawers.totalWithdrawals].toDouble(),
                            totalCashIn = result[TellerCashDrawers.totalCashIn].toDouble(),
                            totalCashOut = result[TellerCashDrawers.totalCashOut].toDouble(),
                            status = result[TellerCashDrawers.status],
                            reconciliationStatus = result[TellerCashDrawers.reconciliationStatus],
                            varianceAmount = result[TellerCashDrawers.varianceAmount].toDouble(),
                            sessionStartTime = result[TellerCashDrawers.sessionStartTime].toString(),
                            sessionEndTime = result[TellerCashDrawers.sessionEndTime]?.toString(),
                            lastReconciliationDate = result[TellerCashDrawers.lastReconciliationDate]?.toString()
                        )
                    )
                } else {
                    ApiResponse(
                        success = false,
                        message = "No active cash drawer found for teller",
                        data = null as TellerCashDrawerResponse?
                    )
                }
            }
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Failed to get teller cash drawer: ${e.message}",
                error = e.message
            )
        }
    }

    /**
     * Process a cash transaction and update drawer balance
     */
    fun processCashTransaction(request: TellerTransactionRequest): ApiResponse<TellerCashDrawerTransactionResponse> {
        return try {
            transaction {
                // Get current drawer
                val drawer = TellerCashDrawers.select {
                    TellerCashDrawers.id eq UUID.fromString(request.drawerId)
                }.singleOrNull()

                if (drawer == null) {
                    return@transaction ApiResponse(
                        success = false,
                        message = "Cash drawer not found",
                        data = null as TellerCashDrawerTransactionResponse?
                    )
                }

                val currentBalance = drawer[TellerCashDrawers.currentBalance]
                val transactionAmount = BigDecimal(request.amount)

                // Calculate new balance based on transaction type
                val newBalance = when (request.transactionType) {
                    "CASH_IN", "DEPOSIT" -> currentBalance + transactionAmount
                    "CASH_OUT", "WITHDRAWAL" -> currentBalance - transactionAmount
                    else -> currentBalance
                }

                // Check if withdrawal would cause negative balance
                if (newBalance < BigDecimal.ZERO) {
                    return@transaction ApiResponse(
                        success = false,
                        message = "Insufficient cash in drawer",
                        data = null as TellerCashDrawerTransactionResponse?
                    )
                }

                // Create transaction record
                val transactionId = TellerCashDrawerTransactions.insertAndGetId {
                    it[TellerCashDrawerTransactions.drawerId] = UUID.fromString(request.drawerId)
                    it[TellerCashDrawerTransactions.tellerId] = drawer[TellerCashDrawers.tellerId]
                    it[TellerCashDrawerTransactions.transactionType] = request.transactionType
                    it[TellerCashDrawerTransactions.amount] = transactionAmount
                    it[TellerCashDrawerTransactions.balanceBefore] = currentBalance
                    it[TellerCashDrawerTransactions.balanceAfter] = newBalance
                    it[TellerCashDrawerTransactions.customerAccountNumber] = request.customerAccountNumber
                    it[TellerCashDrawerTransactions.customerId] = request.customerId?.let { UUID.fromString(it) }
                    it[TellerCashDrawerTransactions.description] = request.description
                    it[TellerCashDrawerTransactions.reference] = request.reference
                    it[TellerCashDrawerTransactions.receiptNumber] = request.receiptNumber
                    it[TellerCashDrawerTransactions.chequeNumber] = request.chequeNumber
                }

                // Update drawer balances
                TellerCashDrawers.update({ TellerCashDrawers.id eq UUID.fromString(request.drawerId) }) {
                    it[TellerCashDrawers.currentBalance] = newBalance
                    when (request.transactionType) {
                        "CASH_IN" -> it[TellerCashDrawers.totalCashIn] =
                            drawer[TellerCashDrawers.totalCashIn] + transactionAmount

                        "CASH_OUT" -> it[TellerCashDrawers.totalCashOut] =
                            drawer[TellerCashDrawers.totalCashOut] + transactionAmount

                        "DEPOSIT" -> it[TellerCashDrawers.totalDeposits] =
                            drawer[TellerCashDrawers.totalDeposits] + transactionAmount

                        "WITHDRAWAL" -> it[TellerCashDrawers.totalWithdrawals] =
                            drawer[TellerCashDrawers.totalWithdrawals] + transactionAmount
                    }
                    it[TellerCashDrawers.updatedAt] = org.jetbrains.exposed.sql.javatime.CurrentTimestamp()
                }

                // Update customer account balance for DEPOSIT and WITHDRAWAL transactions
                if ((request.transactionType == "DEPOSIT" || request.transactionType == "WITHDRAWAL") && 
                    request.customerAccountNumber != null) {

                    val customerAccount = Accounts.select { 
                        Accounts.accountNumber eq request.customerAccountNumber 
                    }.singleOrNull()

                    if (customerAccount != null) {
                        val accountId = customerAccount[Accounts.id].value
                        val currentAccountBalance = customerAccount[Accounts.balance]

                        val newAccountBalance = when (request.transactionType) {
                            "DEPOSIT" -> currentAccountBalance + transactionAmount
                            "WITHDRAWAL" -> {
                                val calculatedBalance = currentAccountBalance - transactionAmount
                                if (calculatedBalance < BigDecimal.ZERO) {
                                    return@transaction ApiResponse(
                                        success = false,
                                        message = "Insufficient funds in customer account",
                                        data = null as TellerCashDrawerTransactionResponse?
                                    )
                                }
                                calculatedBalance
                            }
                            else -> currentAccountBalance
                        }

                        // Update account balance
                        Accounts.update({ Accounts.id eq accountId }) {
                            it[Accounts.balance] = newAccountBalance
                            it[Accounts.availableBalance] = newAccountBalance
                            it[Accounts.lastTransactionDate] = org.jetbrains.exposed.sql.javatime.CurrentTimestamp()
                        }

                        // Generate transaction reference if not provided
                        val transactionReference = request.reference?.takeIf { it.isNotBlank() }
                            ?: IdGenerator.generateTransactionId()

                        // Create a corresponding transaction record in the Transactions table
                        Transactions.insert {
                            it[Transactions.id] = UUID.randomUUID()
                            it[Transactions.accountId] = accountId
                            it[Transactions.type] = when (request.transactionType) {
                                "DEPOSIT" -> TransactionType.DEPOSIT
                                "WITHDRAWAL" -> TransactionType.WITHDRAWAL
                                else -> TransactionType.DEPOSIT
                            }
                            it[Transactions.amount] = transactionAmount
                            it[Transactions.description] = request.description
                            it[Transactions.balanceAfter] = newAccountBalance
                            it[Transactions.reference] = transactionReference
                            it[Transactions.processedBy] = drawer[TellerCashDrawers.tellerId]
                            it[Transactions.branchId] = drawer[TellerCashDrawers.branchId]
                            it[Transactions.checkNumber] = request.chequeNumber
                            it[Transactions.status] = TransactionStatus.COMPLETED
                        }
                    }
                }

                val transaction = TellerCashDrawerTransactions.select {
                    TellerCashDrawerTransactions.id eq transactionId
                }.single()

                ApiResponse(
                    success = true,
                    message = "Cash transaction processed successfully",
                    data = TellerCashDrawerTransactionResponse(
                        id = transaction[TellerCashDrawerTransactions.id].value.toString(),
                        drawerId = request.drawerId,
                        tellerId = transaction[TellerCashDrawerTransactions.tellerId].toString(),
                        transactionType = transaction[TellerCashDrawerTransactions.transactionType],
                        amount = transaction[TellerCashDrawerTransactions.amount].toDouble(),
                        balanceBefore = transaction[TellerCashDrawerTransactions.balanceBefore].toDouble(),
                        balanceAfter = transaction[TellerCashDrawerTransactions.balanceAfter].toDouble(),
                        customerAccountNumber = transaction[TellerCashDrawerTransactions.customerAccountNumber],
                        customerId = transaction[TellerCashDrawerTransactions.customerId]?.toString(),
                        description = transaction[TellerCashDrawerTransactions.description],
                        reference = transaction[TellerCashDrawerTransactions.reference],
                        receiptNumber = transaction[TellerCashDrawerTransactions.receiptNumber],
                        chequeNumber = transaction[TellerCashDrawerTransactions.chequeNumber],
                        currency = transaction[TellerCashDrawerTransactions.currency],
                        transactionDate = transaction[TellerCashDrawerTransactions.transactionDate].toString()
                    )
                )
            }
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Failed to process cash transaction: ${e.message}",
                error = e.message
            )
        }
    }

    /**
     * Get recent transactions for a teller's cash drawer
     */
    fun getTellerTransactions(
        tellerId: String,
        limit: Int = 20
    ): ApiResponse<List<TellerCashDrawerTransactionResponse>> {
        return try {
            transaction {
                val results = TellerCashDrawerTransactions.innerJoin(TellerCashDrawers)
                    .select {
                        TellerCashDrawers.tellerId eq UUID.fromString(tellerId)
                    }
                    .orderBy(TellerCashDrawerTransactions.transactionDate, SortOrder.DESC)
                    .limit(limit)
                    .map { row ->
                        TellerCashDrawerTransactionResponse(
                            id = row[TellerCashDrawerTransactions.id].value.toString(),
                            drawerId = row[TellerCashDrawerTransactions.drawerId].toString(),
                            tellerId = row[TellerCashDrawerTransactions.tellerId].toString(),
                            transactionType = row[TellerCashDrawerTransactions.transactionType],
                            amount = row[TellerCashDrawerTransactions.amount].toDouble(),
                            balanceBefore = row[TellerCashDrawerTransactions.balanceBefore].toDouble(),
                            balanceAfter = row[TellerCashDrawerTransactions.balanceAfter].toDouble(),
                            customerAccountNumber = row[TellerCashDrawerTransactions.customerAccountNumber],
                            customerId = row[TellerCashDrawerTransactions.customerId]?.toString(),
                            description = row[TellerCashDrawerTransactions.description],
                            reference = row[TellerCashDrawerTransactions.reference],
                            receiptNumber = row[TellerCashDrawerTransactions.receiptNumber],
                            chequeNumber = row[TellerCashDrawerTransactions.chequeNumber],
                            currency = row[TellerCashDrawerTransactions.currency],
                            transactionDate = row[TellerCashDrawerTransactions.transactionDate].toString()
                        )
                    }

                ApiResponse(
                    success = true,
                    message = "Teller transactions retrieved successfully",
                    data = results
                )
            }
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Failed to get teller transactions: ${e.message}",
                error = e.message
            )
        }
    }

    /**
     * Close teller cash drawer session
     */
    fun closeTellerCashDrawer(tellerId: String, finalBalance: Double, notes: String? = null): ApiResponse<String> {
        return try {
            transaction {
                val currentDrawerBalance = TellerCashDrawers.select {
                    (TellerCashDrawers.tellerId eq UUID.fromString(tellerId)) and
                            (TellerCashDrawers.status eq "ACTIVE")
                }.singleOrNull()?.let { it[TellerCashDrawers.currentBalance] } ?: BigDecimal.ZERO

                val updated = TellerCashDrawers.update({
                    (TellerCashDrawers.tellerId eq UUID.fromString(tellerId)) and
                            (TellerCashDrawers.status eq "ACTIVE")
                }) {
                    it[TellerCashDrawers.status] = "CLOSED"
                    it[TellerCashDrawers.sessionEndTime] = org.jetbrains.exposed.sql.javatime.CurrentTimestamp()
                    it[TellerCashDrawers.varianceAmount] = BigDecimal(finalBalance) - currentDrawerBalance
                    it[TellerCashDrawers.updatedAt] = org.jetbrains.exposed.sql.javatime.CurrentTimestamp()
                }

                if (updated > 0) {
                    ApiResponse(
                        success = true,
                        message = "Teller cash drawer closed successfully",
                        data = "Cash drawer closed"
                    )
                } else {
                    ApiResponse(
                        success = false,
                        message = "No active cash drawer found to close",
                        data = null as String?
                    )
                }
            }
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Failed to close teller cash drawer: ${e.message}",
                error = e.message
            )
        }
    }

    /**
     * Allocate float money to teller from master wallet
     */
    fun allocateFloatToTeller(
        tellerId: String,
        branchId: String,
        sourceWalletId: String,
        amount: Double,
        allocatedBy: String,
        purpose: String? = null
    ): ApiResponse<String> {
        return try {
            transaction {
                // Check if source wallet has sufficient balance
                val wallet = MasterWallets.select {
                    MasterWallets.id eq UUID.fromString(sourceWalletId)
                }.singleOrNull()

                if (wallet == null) {
                    return@transaction ApiResponse(
                        success = false,
                        message = "Source wallet not found",
                        data = null as String?
                    )
                }

                if (wallet[MasterWallets.availableBalance] < BigDecimal(amount)) {
                    return@transaction ApiResponse(
                        success = false,
                        message = "Insufficient balance in source wallet",
                        data = null as String?
                    )
                }

                // Create float allocation record
                TellerFloatAllocations.insert {
                    it[TellerFloatAllocations.tellerId] = UUID.fromString(tellerId)
                    it[TellerFloatAllocations.branchId] = UUID.fromString(branchId)
                    it[TellerFloatAllocations.sourceWalletId] = UUID.fromString(sourceWalletId)
                    it[TellerFloatAllocations.allocatedAmount] = BigDecimal(amount)
                    it[TellerFloatAllocations.remainingAmount] = BigDecimal(amount)
                    it[TellerFloatAllocations.allocatedBy] = UUID.fromString(allocatedBy)
                    it[TellerFloatAllocations.purpose] = purpose
                }

                // Get current wallet balance first
                val currentWallet = MasterWallets.select {
                    MasterWallets.id eq UUID.fromString(sourceWalletId)
                }.single()
                val currentAvailableBalance = currentWallet[MasterWallets.availableBalance]

                // Update master wallet balance
                MasterWallets.update({ MasterWallets.id eq UUID.fromString(sourceWalletId) }) {
                    it[MasterWallets.availableBalance] = currentAvailableBalance - BigDecimal(amount)
                }

                ApiResponse(
                    success = true,
                    message = "Float allocated to teller successfully",
                    data = "Float allocated"
                )
            }
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Failed to allocate float to teller: ${e.message}",
                error = e.message
            )
        }
    }
}