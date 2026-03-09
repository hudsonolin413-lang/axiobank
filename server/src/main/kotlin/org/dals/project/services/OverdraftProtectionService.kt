package org.dals.project.services

import org.dals.project.database.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.*

data class EnrollOverdraftProtectionRequest(
    val customerId: String,
    val accountId: String,
    val protectionType: String, // LINKED_ACCOUNT, LINE_OF_CREDIT, STANDARD_OVERDRAFT
    val linkedAccountId: String? = null,
    val creditLimit: Double? = null,
    val transferFee: Double = 0.0,
    val interestRate: Double? = null,
    val autoTransfer: Boolean = true,
    val dailyLimit: Double? = null
)

data class OverdraftProtectionDto(
    val id: String,
    val customerId: String,
    val accountId: String,
    val protectionType: String,
    val linkedAccountId: String?,
    val creditLimit: Double?,
    val usedAmount: Double,
    val availableAmount: Double?,
    val transferFee: Double,
    val interestRate: Double?,
    val status: String,
    val autoTransfer: Boolean,
    val dailyLimit: Double?,
    val lastUsedDate: String?,
    val enrolledAt: String,
    val cancelledAt: String?
)

data class OverdraftTransactionDto(
    val id: String,
    val overdraftProtectionId: String,
    val accountId: String,
    val linkedAccountId: String?,
    val transactionId: String,
    val amount: Double,
    val fee: Double,
    val totalAmount: Double,
    val balanceBefore: Double,
    val balanceAfter: Double,
    val protectionType: String,
    val status: String,
    val failureReason: String?,
    val createdAt: String
)

data class OverdraftUsageStatsDto(
    val totalProtections: Int,
    val activeProtections: Int,
    val totalUsedAmount: Double,
    val totalAvailableAmount: Double,
    val recentTransactions: List<OverdraftTransactionDto>
)

object OverdraftProtectionService {

    fun enrollOverdraftProtection(request: EnrollOverdraftProtectionRequest): Result<OverdraftProtectionDto> {
        return try {
            transaction {
                // Validate customer and account
                val customer = Customers.select { Customers.id eq UUID.fromString(request.customerId) }
                    .singleOrNull()
                    ?: return@transaction Result.failure(Exception("Customer not found"))

                val account = Accounts.select {
                    (Accounts.id eq UUID.fromString(request.accountId)) and
                    (Accounts.customerId eq UUID.fromString(request.customerId))
                }.singleOrNull()
                    ?: return@transaction Result.failure(Exception("Account not found"))

                // Check if protection already exists
                val existing = OverdraftProtection.select {
                    (OverdraftProtection.accountId eq UUID.fromString(request.accountId)) and
                    (OverdraftProtection.status eq "ACTIVE")
                }.singleOrNull()

                if (existing != null) {
                    return@transaction Result.failure(Exception("Overdraft protection already enrolled for this account"))
                }

                // Validate linked account if provided
                if (request.linkedAccountId != null) {
                    val linkedAccount = Accounts.select {
                        (Accounts.id eq UUID.fromString(request.linkedAccountId)) and
                        (Accounts.customerId eq UUID.fromString(request.customerId)) and
                        (Accounts.status eq AccountStatus.ACTIVE)
                    }.singleOrNull()
                        ?: return@transaction Result.failure(Exception("Linked account not found or not active"))
                }

                // Calculate available amount
                val creditLimit = request.creditLimit?.let { BigDecimal(it) }
                val availableAmount = creditLimit

                // Create overdraft protection
                val protectionId = OverdraftProtection.insert {
                    it[customerId] = UUID.fromString(request.customerId)
                    it[accountId] = UUID.fromString(request.accountId)
                    it[protectionType] = request.protectionType
                    it[linkedAccountId] = request.linkedAccountId?.let { id -> UUID.fromString(id) }
                    it[OverdraftProtection.creditLimit] = creditLimit
                    it[usedAmount] = BigDecimal.ZERO
                    it[OverdraftProtection.availableAmount] = availableAmount
                    it[transferFee] = BigDecimal(request.transferFee)
                    it[interestRate] = request.interestRate?.let { rate -> BigDecimal(rate) }
                    it[status] = "ACTIVE"
                    it[autoTransfer] = request.autoTransfer
                    it[OverdraftProtection.dailyLimit] = request.dailyLimit?.let { limit -> BigDecimal(limit) }
                }[OverdraftProtection.id].value

                val protection = OverdraftProtection.select { OverdraftProtection.id eq protectionId }
                    .single()

                Result.success(mapToOverdraftProtectionDto(protection))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getOverdraftProtection(accountId: String): Result<OverdraftProtectionDto?> {
        return try {
            transaction {
                val protection = OverdraftProtection.select {
                    (OverdraftProtection.accountId eq UUID.fromString(accountId)) and
                    (OverdraftProtection.status eq "ACTIVE")
                }.singleOrNull()

                Result.success(protection?.let { mapToOverdraftProtectionDto(it) })
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllOverdraftProtections(customerId: String): Result<List<OverdraftProtectionDto>> {
        return try {
            transaction {
                val protections = OverdraftProtection.select {
                    OverdraftProtection.customerId eq UUID.fromString(customerId)
                }.map { mapToOverdraftProtectionDto(it) }

                Result.success(protections)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun processOverdraftTransfer(
        accountId: String,
        amount: Double,
        originalTransactionId: String
    ): Result<OverdraftTransactionDto> {
        return try {
            transaction {
                // Get active overdraft protection
                val protection = OverdraftProtection.select {
                    (OverdraftProtection.accountId eq UUID.fromString(accountId)) and
                    (OverdraftProtection.status eq "ACTIVE") and
                    (OverdraftProtection.autoTransfer eq true)
                }.singleOrNull()
                    ?: return@transaction Result.failure(Exception("No active overdraft protection with auto-transfer"))

                val protectionId = protection[OverdraftProtection.id].value
                val protectionType = protection[OverdraftProtection.protectionType]
                val linkedAccountId = protection[OverdraftProtection.linkedAccountId]
                val creditLimit = protection[OverdraftProtection.creditLimit]?.toDouble()
                val usedAmount = protection[OverdraftProtection.usedAmount].toDouble()
                val availableAmount = protection[OverdraftProtection.availableAmount]?.toDouble()
                val transferFee = protection[OverdraftProtection.transferFee].toDouble()
                val dailyLimit = protection[OverdraftProtection.dailyLimit]?.toDouble()

                // Check daily limit
                if (dailyLimit != null) {
                    val today = LocalDate.now()
                    val todayTransactions = OverdraftTransactions
                        .select {
                            (OverdraftTransactions.overdraftProtectionId eq protectionId) and
                            (OverdraftTransactions.createdAt.date() eq today)
                        }
                        .sumOf { it[OverdraftTransactions.amount].toDouble() }

                    if (todayTransactions + amount > dailyLimit) {
                        return@transaction Result.failure(Exception("Daily overdraft limit exceeded"))
                    }
                }

                val totalAmount = amount + transferFee

                // Get current balance
                val account = Accounts.select { Accounts.id eq UUID.fromString(accountId) }.single()
                val balanceBefore = account[Accounts.balance].toDouble()

                // Process based on protection type
                when (protectionType) {
                    "LINKED_ACCOUNT" -> {
                        if (linkedAccountId == null) {
                            return@transaction Result.failure(Exception("Linked account not configured"))
                        }

                        // Check linked account balance
                        val linkedAccount = Accounts.select { Accounts.id eq linkedAccountId }.single()
                        val linkedBalance = linkedAccount[Accounts.balance].toDouble()

                        if (linkedBalance < totalAmount) {
                            return@transaction Result.failure(Exception("Insufficient funds in linked account"))
                        }

                        // Transfer from linked account
                        Accounts.update({ Accounts.id eq linkedAccountId }) {
                            it[balance] = BigDecimal.valueOf(linkedBalance - totalAmount)
                            it[updatedAt] = Instant.now()
                        }

                        // Add to main account
                        val newBalance = balanceBefore + amount
                        Accounts.update({ Accounts.id eq UUID.fromString(accountId) }) {
                            it[balance] = BigDecimal.valueOf(newBalance)
                            it[updatedAt] = Instant.now()
                        }

                        // Create transaction
                        val transactionId = Transactions.insert {
                            it[Transactions.accountId] = UUID.fromString(accountId)
                            it[Transactions.type] = TransactionType.TRANSFER
                            it[Transactions.amount] = BigDecimal.valueOf(amount)
                            it[Transactions.description] = "Overdraft Protection Transfer from Linked Account"
                            it[Transactions.status] = TransactionStatus.COMPLETED
                            it[Transactions.category] = "OVERDRAFT"
                            it[Transactions.reference] = "LINK-${System.currentTimeMillis().toString().takeLast(6)}"
                            it[Transactions.balanceAfter] = BigDecimal.valueOf(newBalance)
                        }[Transactions.id].value

                        // Record overdraft transaction
                        val overdraftTransId = OverdraftTransactions.insert {
                            it[overdraftProtectionId] = protectionId
                            it[OverdraftTransactions.accountId] = UUID.fromString(accountId)
                            it[OverdraftTransactions.linkedAccountId] = linkedAccountId
                            it[OverdraftTransactions.transactionId] = transactionId
                            it[OverdraftTransactions.amount] = BigDecimal.valueOf(amount)
                            it[fee] = BigDecimal.valueOf(transferFee)
                            it[OverdraftTransactions.totalAmount] = BigDecimal.valueOf(totalAmount)
                            it[OverdraftTransactions.balanceBefore] = BigDecimal.valueOf(balanceBefore)
                            it[OverdraftTransactions.balanceAfter] = BigDecimal.valueOf(newBalance)
                            it[OverdraftTransactions.protectionType] = protectionType
                            it[OverdraftTransactions.status] = "COMPLETED"
                        }[OverdraftTransactions.id].value

                        // Update protection last used date
                        OverdraftProtection.update({ OverdraftProtection.id eq protectionId }) {
                            it[lastUsedDate] = Instant.now()
                            it[updatedAt] = Instant.now()
                        }

                        val overdraftTrans = OverdraftTransactions.select { OverdraftTransactions.id eq overdraftTransId }
                            .single()

                        Result.success(mapToOverdraftTransactionDto(overdraftTrans))
                    }

                    "LINE_OF_CREDIT" -> {
                        if (creditLimit == null || availableAmount == null) {
                            return@transaction Result.failure(Exception("Credit limit not configured"))
                        }

                        if (availableAmount < amount) {
                            return@transaction Result.failure(Exception("Insufficient overdraft credit available"))
                        }

                        // Add to main account from credit line
                        val newBalance = balanceBefore + amount
                        Accounts.update({ Accounts.id eq UUID.fromString(accountId) }) {
                            it[balance] = BigDecimal.valueOf(newBalance)
                            it[updatedAt] = Instant.now()
                        }

                        // Update protection usage
                        val newUsedAmount = usedAmount + amount
                        val newAvailableAmount = creditLimit - newUsedAmount
                        OverdraftProtection.update({ OverdraftProtection.id eq protectionId }) {
                            it[OverdraftProtection.usedAmount] = BigDecimal.valueOf(newUsedAmount)
                            it[OverdraftProtection.availableAmount] = BigDecimal.valueOf(newAvailableAmount)
                            it[lastUsedDate] = Instant.now()
                            it[updatedAt] = Instant.now()
                        }

                        // Create transaction
                        val transactionId = Transactions.insert {
                            it[Transactions.accountId] = UUID.fromString(accountId)
                            it[Transactions.type] = TransactionType.TRANSFER
                            it[Transactions.amount] = BigDecimal.valueOf(amount)
                            it[Transactions.description] = "Overdraft Protection - Line of Credit"
                            it[Transactions.status] = TransactionStatus.COMPLETED
                            it[Transactions.category] = "OVERDRAFT"
                            it[Transactions.balanceAfter] = BigDecimal.valueOf(newBalance)
                        }[Transactions.id].value

                        // Record overdraft transaction
                        val overdraftTransId = OverdraftTransactions.insert {
                            it[overdraftProtectionId] = protectionId
                            it[OverdraftTransactions.accountId] = UUID.fromString(accountId)
                            it[OverdraftTransactions.transactionId] = transactionId
                            it[OverdraftTransactions.amount] = BigDecimal.valueOf(amount)
                            it[fee] = BigDecimal.valueOf(transferFee)
                            it[OverdraftTransactions.totalAmount] = BigDecimal.valueOf(amount)
                            it[OverdraftTransactions.balanceBefore] = BigDecimal.valueOf(balanceBefore)
                            it[OverdraftTransactions.balanceAfter] = BigDecimal.valueOf(newBalance)
                            it[OverdraftTransactions.protectionType] = protectionType
                            it[OverdraftTransactions.status] = "COMPLETED"
                        }[OverdraftTransactions.id].value

                        val overdraftTrans = OverdraftTransactions.select { OverdraftTransactions.id eq overdraftTransId }
                            .single()

                        Result.success(mapToOverdraftTransactionDto(overdraftTrans))
                    }

                    else -> Result.failure(Exception("Unsupported protection type: $protectionType"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getOverdraftTransactions(accountId: String, limit: Int = 50): Result<List<OverdraftTransactionDto>> {
        return try {
            transaction {
                val transactions = OverdraftTransactions
                    .select { OverdraftTransactions.accountId eq UUID.fromString(accountId) }
                    .orderBy(OverdraftTransactions.createdAt to SortOrder.DESC)
                    .limit(limit)
                    .map { mapToOverdraftTransactionDto(it) }

                Result.success(transactions)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getOverdraftUsageStats(customerId: String): Result<OverdraftUsageStatsDto> {
        return try {
            transaction {
                val protections = OverdraftProtection.select {
                    OverdraftProtection.customerId eq UUID.fromString(customerId)
                }.toList()

                val totalProtections = protections.size
                val activeProtections = protections.count { it[OverdraftProtection.status] == "ACTIVE" }
                val totalUsedAmount = protections.sumOf { it[OverdraftProtection.usedAmount].toDouble() }
                val totalAvailableAmount = protections
                    .filter { it[OverdraftProtection.availableAmount] != null }
                    .sumOf { it[OverdraftProtection.availableAmount]!!.toDouble() }

                // Get recent transactions
                val accountIds = protections.map { it[OverdraftProtection.accountId] }
                val recentTransactions = OverdraftTransactions
                    .select { OverdraftTransactions.accountId inList accountIds }
                    .orderBy(OverdraftTransactions.createdAt to SortOrder.DESC)
                    .limit(10)
                    .map { mapToOverdraftTransactionDto(it) }

                Result.success(
                    OverdraftUsageStatsDto(
                        totalProtections = totalProtections,
                        activeProtections = activeProtections,
                        totalUsedAmount = totalUsedAmount,
                        totalAvailableAmount = totalAvailableAmount,
                        recentTransactions = recentTransactions
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun cancelOverdraftProtection(protectionId: String): Result<OverdraftProtectionDto> {
        return try {
            transaction {
                val protection = OverdraftProtection.select { OverdraftProtection.id eq UUID.fromString(protectionId) }
                    .singleOrNull()
                    ?: return@transaction Result.failure(Exception("Overdraft protection not found"))

                val usedAmount = protection[OverdraftProtection.usedAmount].toDouble()
                if (usedAmount > 0) {
                    return@transaction Result.failure(Exception("Cannot cancel protection with outstanding balance"))
                }

                OverdraftProtection.update({ OverdraftProtection.id eq UUID.fromString(protectionId) }) {
                    it[OverdraftProtection.status] = "CANCELLED"
                    it[cancelledAt] = Instant.now()
                    it[updatedAt] = Instant.now()
                }

                val updated = OverdraftProtection.select { OverdraftProtection.id eq UUID.fromString(protectionId) }
                    .single()

                Result.success(mapToOverdraftProtectionDto(updated))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapToOverdraftProtectionDto(row: ResultRow): OverdraftProtectionDto {
        return OverdraftProtectionDto(
            id = row[OverdraftProtection.id].value.toString(),
            customerId = row[OverdraftProtection.customerId].toString(),
            accountId = row[OverdraftProtection.accountId].toString(),
            protectionType = row[OverdraftProtection.protectionType],
            linkedAccountId = row[OverdraftProtection.linkedAccountId]?.toString(),
            creditLimit = row[OverdraftProtection.creditLimit]?.toDouble(),
            usedAmount = row[OverdraftProtection.usedAmount].toDouble(),
            availableAmount = row[OverdraftProtection.availableAmount]?.toDouble(),
            transferFee = row[OverdraftProtection.transferFee].toDouble(),
            interestRate = row[OverdraftProtection.interestRate]?.toDouble(),
            status = row[OverdraftProtection.status],
            autoTransfer = row[OverdraftProtection.autoTransfer],
            dailyLimit = row[OverdraftProtection.dailyLimit]?.toDouble(),
            lastUsedDate = row[OverdraftProtection.lastUsedDate]?.toString(),
            enrolledAt = row[OverdraftProtection.enrolledAt].toString(),
            cancelledAt = row[OverdraftProtection.cancelledAt]?.toString()
        )
    }

    private fun mapToOverdraftTransactionDto(row: ResultRow): OverdraftTransactionDto {
        return OverdraftTransactionDto(
            id = row[OverdraftTransactions.id].value.toString(),
            overdraftProtectionId = row[OverdraftTransactions.overdraftProtectionId].toString(),
            accountId = row[OverdraftTransactions.accountId].toString(),
            linkedAccountId = row[OverdraftTransactions.linkedAccountId]?.toString(),
            transactionId = row[OverdraftTransactions.transactionId].toString(),
            amount = row[OverdraftTransactions.amount].toDouble(),
            fee = row[OverdraftTransactions.fee].toDouble(),
            totalAmount = row[OverdraftTransactions.totalAmount].toDouble(),
            balanceBefore = row[OverdraftTransactions.balanceBefore].toDouble(),
            balanceAfter = row[OverdraftTransactions.balanceAfter].toDouble(),
            protectionType = row[OverdraftTransactions.protectionType],
            status = row[OverdraftTransactions.status],
            failureReason = row[OverdraftTransactions.failureReason],
            createdAt = row[OverdraftTransactions.createdAt].toString()
        )
    }
}
