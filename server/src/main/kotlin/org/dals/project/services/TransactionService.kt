package org.dals.project.services

import org.dals.project.database.*
import org.dals.project.models.*
import org.dals.project.utils.IdGenerator
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import java.math.BigDecimal
import java.util.*

class TransactionService {
    private val notificationService = NotificationService()
    private val feeService = TransactionFeeService()

    suspend fun getAllTransactions(
        page: Int = 1,
        pageSize: Int = 10,
        accountNumber: String? = null,
        status: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): ListResponse<TransactionDto> {
        return DatabaseFactory.dbQuery {
            val offset = (page - 1) * pageSize

            // Build query with filters
            var query = Transactions.selectAll()

            // Apply filters if provided
            if (accountNumber != null) {
                val account = Accounts.select { Accounts.accountNumber eq accountNumber }.singleOrNull()
                if (account != null) {
                    val accountId = account[Accounts.id].value
                    query = query.andWhere {
                        (Transactions.accountId eq accountId) or
                        (Transactions.fromAccountId eq accountId) or
                        (Transactions.toAccountId eq accountId)
                    }
                }
            }

            if (status != null) {
                query = query.andWhere { Transactions.status eq TransactionStatus.valueOf(status.uppercase()) }
            }

            val transactions = query
                .orderBy(Transactions.timestamp, SortOrder.DESC)
                .limit(pageSize, offset.toLong())
                .map { toTransactionDto(it) }

            val total = query.count().toInt()

            ListResponse(
                success = true,
                message = "Transactions retrieved successfully",
                data = transactions,
                total = total,
                page = page,
                pageSize = pageSize
            )
        }
    }

    suspend fun getTransactionById(id: UUID): ApiResponse<TransactionDto> {
        return DatabaseFactory.dbQuery {
            val transaction = Transactions.select { Transactions.id eq id }
                .singleOrNull()
                ?.let { toTransactionDto(it) }

            if (transaction != null) {
                ApiResponse(
                    success = true,
                    message = "Transaction found",
                    data = transaction
                )
            } else {
                ApiResponse(
                    success = false,
                    message = "Transaction not found",
                    error = "No transaction found with ID: $id"
                )
            }
        }
    }

    suspend fun getTransactionByReference(reference: String): TransactionDto? {
        return DatabaseFactory.dbQuery {
            println("🔍 Searching for transaction with reference: '$reference'")

            // First try exact match
            var result = Transactions.select { Transactions.reference eq reference }
                .orderBy(Transactions.timestamp, SortOrder.DESC)
                .firstOrNull()
                ?.let { toTransactionDto(it) }

            // If not found, try case-insensitive match
            if (result == null) {
                println("⚠️ Exact match not found, trying case-insensitive search...")
                result = Transactions.select {
                    Transactions.reference.upperCase() eq reference.uppercase()
                }
                .orderBy(Transactions.timestamp, SortOrder.DESC)
                .firstOrNull()
                ?.let { toTransactionDto(it) }
            }

            // If still not found, show some sample references for debugging
            if (result == null) {
                println("❌ Transaction not found. Showing last 5 transaction references:")
                Transactions.selectAll()
                    .orderBy(Transactions.timestamp, SortOrder.DESC)
                    .limit(5)
                    .forEach { row ->
                        println("   - Reference: '${row[Transactions.reference]}' (ID: ${row[Transactions.id]})")
                    }
            } else {
                println("✅ Transaction found: ${result.reference}")
            }

            result
        }
    }

    suspend fun getTransactionsByAccountId(
        accountId: UUID,
        page: Int = 1,
        pageSize: Int = 10
    ): ListResponse<TransactionDto> {
        return DatabaseFactory.dbQuery {
            val offset = (page - 1) * pageSize
            val transactions = Transactions.select {
                (Transactions.accountId eq accountId) or
                        (Transactions.fromAccountId eq accountId) or
                        (Transactions.toAccountId eq accountId)
            }
                .orderBy(Transactions.timestamp, SortOrder.DESC)
                .limit(pageSize, offset.toLong())
                .map { toTransactionDto(it) }

            val total = Transactions.select {
                (Transactions.accountId eq accountId) or
                        (Transactions.fromAccountId eq accountId) or
                        (Transactions.toAccountId eq accountId)
            }.count().toInt()

            ListResponse(
                success = true,
                message = "Account transactions retrieved successfully",
                data = transactions,
                total = total,
                page = page,
                pageSize = pageSize
            )
        }
    }

    suspend fun createTransaction(request: CreateTransactionRequest): ApiResponse<TransactionDto> {
        return DatabaseFactory.dbQuery {
            try {
                val transactionId = UUID.randomUUID()
                val accountId = UUID.fromString(request.accountId)

                // Get current account balance
                val currentAccount = Accounts.select { Accounts.id eq accountId }.singleOrNull()
                    ?: throw Exception("Account not found")

                val currentBalance = currentAccount[Accounts.balance]
                val transactionAmount = BigDecimal(request.amount)

                // Calculate transaction fee if applicable
                val transactionFee = if (request.category != null && request.category.contains("MOBILE", ignoreCase = true) ||
                                         request.category != null && request.category.contains("BANK_TRANSFER", ignoreCase = true)) {
                    // Map transaction category to fee type
                    val feeType = when {
                        request.category.contains("MPESA", ignoreCase = true) -> "BANK_TO_MPESA"
                        request.category.contains("AIRTEL", ignoreCase = true) -> "BANK_TO_AIRTEL"
                        request.category.contains("EQUITEL_MPESA", ignoreCase = true) -> "EQUITEL_TO_MPESA"
                        request.category.contains("EQUITEL_AIRTEL", ignoreCase = true) -> "EQUITEL_TO_AIRTEL"
                        request.category.contains("PESALINK", ignoreCase = true) -> "BANK_TO_BANK_PESALINK"
                        request.category.contains("RTGS", ignoreCase = true) -> "BANK_TO_BANK_RTGS"
                        request.category.contains("SWIFT", ignoreCase = true) -> "BANK_TO_BANK_SWIFT"
                        request.category.contains("EQUITY", ignoreCase = true) -> "EQUITY_TO_EQUITY"
                        else -> null
                    }
                    feeType?.let { feeService.calculateTransactionFee(it, transactionAmount) } ?: BigDecimal.ZERO
                } else {
                    BigDecimal.ZERO
                }

                val totalAmountToDeduct = transactionAmount + transactionFee

                // Calculate new balance based on transaction type
                val newBalance = when (TransactionType.valueOf(request.type.uppercase())) {
                    TransactionType.DEPOSIT, TransactionType.DIRECT_DEPOSIT, TransactionType.INTEREST_CREDIT -> {
                        currentBalance + transactionAmount
                    }

                    TransactionType.WITHDRAWAL, TransactionType.PAYMENT, TransactionType.FEE_DEBIT,
                    TransactionType.ATM_WITHDRAWAL, TransactionType.LOAN_PAYMENT -> {
                        val calculatedBalance = currentBalance - totalAmountToDeduct

                        // Check for sufficient funds
                        if (calculatedBalance < BigDecimal.ZERO) {
                            throw Exception("Insufficient funds. Current balance: $currentBalance, Requested: $transactionAmount, Fee: $transactionFee, Total: $totalAmountToDeduct")
                        }

                        calculatedBalance
                    }

                    TransactionType.TRANSFER -> {
                        if (request.fromAccountId == request.accountId) {
                            val calculatedBalance = currentBalance - totalAmountToDeduct

                            // Check for sufficient funds
                            if (calculatedBalance < BigDecimal.ZERO) {
                                throw Exception("Insufficient funds. Current balance: $currentBalance, Requested: $transactionAmount, Fee: $transactionFee, Total: $totalAmountToDeduct")
                            }

                            calculatedBalance
                        } else {
                            currentBalance + transactionAmount
                        }
                    }

                    else -> currentBalance
                }

                // Insert transaction
                // Generate transaction reference if not provided
                val transactionReference = request.reference?.takeIf { it.isNotBlank() }
                    ?: IdGenerator.generateTransactionId()

                val insertedId = Transactions.insert {
                    it[id] = transactionId
                    it[Transactions.accountId] = accountId
                    it[type] = TransactionType.valueOf(request.type.uppercase())
                    it[amount] = transactionAmount
                    it[description] = request.description
                    it[balanceAfter] = newBalance
                    it[fromAccountId] = request.fromAccountId?.let { UUID.fromString(it) }
                    it[toAccountId] = request.toAccountId?.let { UUID.fromString(it) }
                    it[reference] = transactionReference
                    it[processedBy] = request.processedBy?.let { UUID.fromString(it) }
                    it[branchId] = request.branchId?.let { UUID.fromString(it) }
                    it[checkNumber] = request.checkNumber
                    it[merchantName] = request.merchantName
                    it[category] = request.category
                    it[status] = TransactionStatus.COMPLETED
                    it[timestamp] = CurrentTimestamp()
                }[Transactions.id]

                // Update account balance
                Accounts.update({ Accounts.id eq accountId }) {
                    it[balance] = newBalance
                    it[availableBalance] = newBalance
                    it[lastTransactionDate] = CurrentTimestamp()
                }

                // If it's a transfer, update the other account as well
                if (request.type.uppercase() == "TRANSFER" && request.toAccountId != null && request.toAccountId != request.accountId) {
                    // Try to parse as UUID first, if it fails, look up by account number
                    val toAccount = try {
                        val toAccountId = UUID.fromString(request.toAccountId)
                        Accounts.select { Accounts.id eq toAccountId }.singleOrNull()
                    } catch (e: IllegalArgumentException) {
                        // Not a UUID, try looking up by account number
                        Accounts.select { Accounts.accountNumber eq request.toAccountId }.singleOrNull()
                    }

                    if (toAccount != null) {
                        val toAccountId = toAccount[Accounts.id].value
                        val toAccountBalance = toAccount[Accounts.balance]
                        val newToAccountBalance = toAccountBalance + transactionAmount

                        // Create corresponding transaction for the receiving account
                        Transactions.insert {
                            it[id] = UUID.randomUUID()
                            it[Transactions.accountId] = toAccountId
                            it[type] = TransactionType.TRANSFER
                            it[amount] = transactionAmount
                            it[description] = "Transfer from ${request.accountId}"
                            it[balanceAfter] = newToAccountBalance
                            it[fromAccountId] = accountId
                            it[Transactions.toAccountId] = toAccountId
                            it[reference] = transactionReference // Use same reference as original transaction
                            it[processedBy] = request.processedBy?.let { UUID.fromString(it) }
                            it[branchId] = request.branchId?.let { UUID.fromString(it) }
                            it[category] = request.category
                            it[status] = TransactionStatus.COMPLETED
                            it[timestamp] = CurrentTimestamp()
                        }

                        // Update receiving account balance
                        Accounts.update({ Accounts.id eq toAccountId }) {
                            it[balance] = newToAccountBalance
                            it[availableBalance] = newToAccountBalance
                            it[lastTransactionDate] = CurrentTimestamp()
                        }
                    } else {
                        throw Exception("Recipient account not found: ${request.toAccountId}")
                    }
                }

                val createdTransaction = Transactions.select { Transactions.id eq transactionId }
                    .single()
                    .let { toTransactionDto(it) }

                // Record transaction fee if applicable
                if (transactionFee > BigDecimal.ZERO && request.category != null) {
                    try {
                        val customerId = currentAccount[Accounts.customerId]
                        val feeType = when {
                            request.category.contains("MPESA", ignoreCase = true) -> "BANK_TO_MPESA"
                            request.category.contains("AIRTEL", ignoreCase = true) -> "BANK_TO_AIRTEL"
                            request.category.contains("EQUITEL_MPESA", ignoreCase = true) -> "EQUITEL_TO_MPESA"
                            request.category.contains("EQUITEL_AIRTEL", ignoreCase = true) -> "EQUITEL_TO_AIRTEL"
                            request.category.contains("PESALINK", ignoreCase = true) -> "BANK_TO_BANK_PESALINK"
                            request.category.contains("RTGS", ignoreCase = true) -> "BANK_TO_BANK_RTGS"
                            request.category.contains("SWIFT", ignoreCase = true) -> "BANK_TO_BANK_SWIFT"
                            request.category.contains("EQUITY", ignoreCase = true) -> "EQUITY_TO_EQUITY"
                            else -> "GENERAL_TRANSFER"
                        }

                        feeService.recordTransactionFee(
                            transactionId = transactionId,
                            customerId = customerId,
                            accountId = accountId,
                            transactionType = feeType,
                            transactionAmount = transactionAmount,
                            feeAmount = transactionFee,
                            processedBy = request.processedBy?.let { UUID.fromString(it) },
                            branchId = request.branchId?.let { UUID.fromString(it) }
                        )
                        println("✅ Transaction fee recorded: $transactionFee for $feeType")
                    } catch (e: Exception) {
                        println("⚠️ Failed to record transaction fee: ${e.message}")
                        // Don't fail the transaction if fee recording fails
                    }
                }

                // Send notification for successful transaction
                try {
                    val customerId = currentAccount[Accounts.customerId]
                    val transactionType = TransactionType.valueOf(request.type.uppercase())

                    // Determine notification message based on transaction type
                    val notificationType = when (transactionType) {
                        TransactionType.DEPOSIT, TransactionType.MPESA_DEPOSIT,
                        TransactionType.MOBILE_MONEY_DEPOSIT, TransactionType.CHECK_DEPOSIT,
                        TransactionType.DIRECT_DEPOSIT -> {
                            // Check if it's a large deposit (> $1000)
                            if (transactionAmount >= BigDecimal(1000)) {
                                notificationService.notifyLargeDeposit(
                                    customerId = customerId,
                                    accountId = accountId,
                                    amount = transactionAmount.toString()
                                )
                            } else {
                                notificationService.notifyTransactionCompleted(
                                    customerId = customerId,
                                    transactionId = transactionId,
                                    amount = transactionAmount.toString(),
                                    type = "deposit"
                                )
                            }
                            "deposit"
                        }
                        TransactionType.WITHDRAWAL, TransactionType.ATM_WITHDRAWAL -> {
                            notificationService.notifyTransactionCompleted(
                                customerId = customerId,
                                transactionId = transactionId,
                                amount = transactionAmount.toString(),
                                type = "withdrawal"
                            )
                            "withdrawal"
                        }
                        TransactionType.TRANSFER -> {
                            notificationService.notifyTransactionCompleted(
                                customerId = customerId,
                                transactionId = transactionId,
                                amount = transactionAmount.toString(),
                                type = "transfer"
                            )
                            "transfer"
                        }
                        else -> {
                            notificationService.notifyTransactionCompleted(
                                customerId = customerId,
                                transactionId = transactionId,
                                amount = transactionAmount.toString(),
                                type = transactionType.name.lowercase().replace("_", " ")
                            )
                            "transaction"
                        }
                    }

                    // Check for low balance warning
                    if (newBalance < BigDecimal(100)) {
                        notificationService.notifyLowBalance(
                            customerId = customerId,
                            accountId = accountId,
                            balance = newBalance.toString()
                        )
                    }

                    println("📬 Transaction notification sent for $notificationType")
                } catch (e: Exception) {
                    println("⚠️ Failed to send transaction notification: ${e.message}")
                }

                ApiResponse(
                    success = true,
                    message = "Transaction created successfully",
                    data = createdTransaction
                )
            } catch (e: Exception) {
                ApiResponse(
                    success = false,
                    message = "Failed to create transaction",
                    error = e.message
                )
            }
        }
    }

    suspend fun updateTransactionStatus(id: UUID, status: String): ApiResponse<TransactionDto> {
        return DatabaseFactory.dbQuery {
            try {
                val updated = Transactions.update({ Transactions.id eq id }) {
                    it[Transactions.status] = TransactionStatus.valueOf(status.uppercase())
                }

                if (updated > 0) {
                    val updatedTransaction = Transactions.select { Transactions.id eq id }
                        .single()
                        .let { toTransactionDto(it) }

                    ApiResponse(
                        success = true,
                        message = "Transaction status updated successfully",
                        data = updatedTransaction
                    )
                } else {
                    ApiResponse(
                        success = false,
                        message = "Transaction not found",
                        error = "No transaction found with ID: $id"
                    )
                }
            } catch (e: Exception) {
                ApiResponse(
                    success = false,
                    message = "Failed to update transaction status",
                    error = e.message
                )
            }
        }
    }

    suspend fun getAccountBalance(accountId: UUID): ApiResponse<String> {
        return DatabaseFactory.dbQuery {
            try {
                val account = Accounts.select { Accounts.id eq accountId }
                    .singleOrNull()

                if (account != null) {
                    ApiResponse(
                        success = true,
                        message = "Account balance retrieved successfully",
                        data = account[Accounts.balance].toString()
                    )
                } else {
                    ApiResponse(
                        success = false,
                        message = "Account not found",
                        error = "No account found with ID: $accountId"
                    )
                }
            } catch (e: Exception) {
                ApiResponse(
                    success = false,
                    message = "Failed to retrieve account balance",
                    error = e.message
                )
            }
        }
    }

    suspend fun getEnhancedTransactionDetails(transaction: TransactionDto): Map<String, Any?> {
        return DatabaseFactory.dbQuery {
            val enhancedData = mutableMapOf<String, Any?>(
                "id" to transaction.id,
                "reference" to transaction.reference,
                "accountId" to transaction.accountId,
                "amount" to transaction.amount,
                "status" to transaction.status,
                "type" to transaction.type,
                "description" to transaction.description,
                "timestamp" to transaction.timestamp,
                "balanceAfter" to transaction.balanceAfter,
                "category" to transaction.category,
                "fromAccountId" to transaction.fromAccountId,
                "toAccountId" to transaction.toAccountId
            )

            println("🔍 Enhancing transaction: ${transaction.reference}")
            println("  - fromAccountId: ${transaction.fromAccountId}")
            println("  - toAccountId: ${transaction.toAccountId}")

            // Get sender account and customer info
            transaction.fromAccountId?.let { fromAcctId ->
                println("  📤 Looking up sender account: $fromAcctId")
                val fromAccount = Accounts.select { Accounts.id eq UUID.fromString(fromAcctId) }.singleOrNull()
                fromAccount?.let { acct ->
                    val customerId = acct[Accounts.customerId]
                    val customer = Customers.select { Customers.id eq customerId }.singleOrNull()

                    enhancedData["fromAccountId"] = fromAcctId
                    enhancedData["fromAccountNumber"] = acct[Accounts.accountNumber]
                    enhancedData["fromAccountBalance"] = acct[Accounts.balance].toString()
                    enhancedData["fromCustomerId"] = customerId.toString()
                    customer?.let {
                        enhancedData["fromCustomerName"] = "${it[Customers.firstName]} ${it[Customers.lastName]}"
                        enhancedData["fromCustomerEmail"] = it[Customers.email]
                    }
                    println("  ✅ Sender info added: ${enhancedData["fromCustomerName"]} (${enhancedData["fromAccountNumber"]})")
                }
            }

            // Get receiver account and customer info
            transaction.toAccountId?.let { toAcctId ->
                println("  📥 Looking up receiver account: $toAcctId")
                val toAccount = Accounts.select { Accounts.id eq UUID.fromString(toAcctId) }.singleOrNull()
                toAccount?.let { acct ->
                    val customerId = acct[Accounts.customerId]
                    val customer = Customers.select { Customers.id eq customerId }.singleOrNull()

                    enhancedData["toAccountId"] = toAcctId
                    enhancedData["toAccountNumber"] = acct[Accounts.accountNumber]
                    enhancedData["toAccountBalance"] = acct[Accounts.balance].toString()
                    enhancedData["toCustomerId"] = customerId.toString()
                    customer?.let {
                        enhancedData["toCustomerName"] = "${it[Customers.firstName]} ${it[Customers.lastName]}"
                        enhancedData["toCustomerEmail"] = it[Customers.email]
                    }
                    println("  ✅ Receiver info added: ${enhancedData["toCustomerName"]} (${enhancedData["toAccountNumber"]})")
                }
            }

            // Get primary account info
            val primaryAccount = Accounts.select { Accounts.id eq UUID.fromString(transaction.accountId) }.singleOrNull()
            primaryAccount?.let { acct ->
                val customerId = acct[Accounts.customerId]
                val customer = Customers.select { Customers.id eq customerId }.singleOrNull()

                enhancedData["accountNumber"] = acct[Accounts.accountNumber]
                enhancedData["accountBalance"] = acct[Accounts.balance].toString()
                enhancedData["customerId"] = customerId.toString()
                customer?.let {
                    enhancedData["customerName"] = "${it[Customers.firstName]} ${it[Customers.lastName]}"
                    enhancedData["customerEmail"] = it[Customers.email]
                }
                println("  ℹ️ Primary account info added: ${enhancedData["customerName"]} (${enhancedData["accountNumber"]})")
            }

            println("  🎯 Total enhanced fields: ${enhancedData.keys.size}")
            enhancedData
        }
    }

    private fun toTransactionDto(row: ResultRow): TransactionDto {
        return TransactionDto(
            id = row[Transactions.id].toString(),
            accountId = row[Transactions.accountId].toString(),
            type = row[Transactions.type].name,
            amount = row[Transactions.amount].toString(),
            status = row[Transactions.status].name,
            description = row[Transactions.description],
            timestamp = row[Transactions.timestamp].toString(),
            balanceAfter = row[Transactions.balanceAfter].toString(),
            fromAccountId = row[Transactions.fromAccountId]?.toString(),
            toAccountId = row[Transactions.toAccountId]?.toString(),
            reference = row[Transactions.reference],
            processedBy = row[Transactions.processedBy]?.toString(),
            branchId = row[Transactions.branchId]?.toString(),
            checkNumber = row[Transactions.checkNumber],
            merchantName = row[Transactions.merchantName],
            category = row[Transactions.category],
            createdAt = row[Transactions.createdAt].toString()
        )
    }
}