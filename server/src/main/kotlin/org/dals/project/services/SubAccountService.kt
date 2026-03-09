package org.dals.project.services

import org.dals.project.database.*
import org.dals.project.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import java.util.UUID
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class CreateSubAccountRequest(
    val customerId: String,
    val parentAccountId: String,
    val name: String,
    val description: String? = null,
    val targetAmount: String? = null,
    val iconName: String = "Savings",
    val colorHex: String = "0xFF2196F3",
    val targetDate: String? = null,
    val autoTransferAmount: String? = null,
    val autoTransferFrequency: String? = null // DAILY, WEEKLY, MONTHLY
)

@Serializable
data class UpdateSubAccountRequest(
    val name: String? = null,
    val description: String? = null,
    val targetAmount: String? = null,
    val iconName: String? = null,
    val colorHex: String? = null,
    val targetDate: String? = null,
    val isLocked: Boolean? = null,
    val autoTransferAmount: String? = null,
    val autoTransferFrequency: String? = null,
    val isActive: Boolean? = null
)

@Serializable
data class TransferToSubAccountRequest(
    val subAccountId: String,
    val amount: String,
    val description: String? = null,
    val isDirectDeposit: Boolean = false // True for M-Pesa/external deposits, false for internal transfers
)

@Serializable
data class SubAccountResponse(
    val id: String,
    val customerId: String,
    val parentAccountId: String,
    val name: String,
    val description: String?,
    val targetAmount: String?,
    val currentBalance: String,
    val iconName: String,
    val colorHex: String,
    val targetDate: String?,
    val isLocked: Boolean,
    val autoTransferAmount: String?,
    val autoTransferFrequency: String?,
    val isActive: Boolean,
    val progressPercentage: Double,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class TransferResponse(
    val success: Boolean,
    val message: String,
    val subAccount: SubAccountResponse,
    val newParentBalance: String
)

class SubAccountService {

    suspend fun getAllSubAccounts(customerId: UUID): List<SubAccountResponse> = DatabaseFactory.dbQuery {
        SubAccounts.select { SubAccounts.customerId eq customerId }
            .orderBy(SubAccounts.createdAt to SortOrder.DESC)
            .map { row ->
                val currentBalance = row[SubAccounts.currentBalance]
                val targetAmount = row[SubAccounts.targetAmount]
                val progressPercentage = if (targetAmount != null && targetAmount > java.math.BigDecimal.ZERO) {
                    (currentBalance.toDouble() / targetAmount.toDouble() * 100).coerceAtMost(100.0)
                } else {
                    0.0
                }

                SubAccountResponse(
                    id = row[SubAccounts.id].toString(),
                    customerId = row[SubAccounts.customerId].toString(),
                    parentAccountId = row[SubAccounts.parentAccountId].toString(),
                    name = row[SubAccounts.name],
                    description = row[SubAccounts.description],
                    targetAmount = row[SubAccounts.targetAmount]?.toString(),
                    currentBalance = currentBalance.toString(),
                    iconName = row[SubAccounts.iconName],
                    colorHex = row[SubAccounts.colorHex],
                    targetDate = row[SubAccounts.targetDate]?.toString(),
                    isLocked = row[SubAccounts.isLocked],
                    autoTransferAmount = row[SubAccounts.autoTransferAmount]?.toString(),
                    autoTransferFrequency = row[SubAccounts.autoTransferFrequency],
                    isActive = row[SubAccounts.isActive],
                    progressPercentage = progressPercentage,
                    createdAt = row[SubAccounts.createdAt].toString(),
                    updatedAt = row[SubAccounts.updatedAt].toString()
                )
            }
    }

    suspend fun getSubAccountById(subAccountId: UUID): SubAccountResponse? = DatabaseFactory.dbQuery {
        SubAccounts.select { SubAccounts.id eq subAccountId }
            .mapNotNull { row ->
                val currentBalance = row[SubAccounts.currentBalance]
                val targetAmount = row[SubAccounts.targetAmount]
                val progressPercentage = if (targetAmount != null && targetAmount > java.math.BigDecimal.ZERO) {
                    (currentBalance.toDouble() / targetAmount.toDouble() * 100).coerceAtMost(100.0)
                } else {
                    0.0
                }

                SubAccountResponse(
                    id = row[SubAccounts.id].toString(),
                    customerId = row[SubAccounts.customerId].toString(),
                    parentAccountId = row[SubAccounts.parentAccountId].toString(),
                    name = row[SubAccounts.name],
                    description = row[SubAccounts.description],
                    targetAmount = targetAmount?.toString(),
                    currentBalance = currentBalance.toString(),
                    iconName = row[SubAccounts.iconName],
                    colorHex = row[SubAccounts.colorHex],
                    targetDate = row[SubAccounts.targetDate]?.toString(),
                    isLocked = row[SubAccounts.isLocked],
                    autoTransferAmount = row[SubAccounts.autoTransferAmount]?.toString(),
                    autoTransferFrequency = row[SubAccounts.autoTransferFrequency],
                    isActive = row[SubAccounts.isActive],
                    progressPercentage = progressPercentage,
                    createdAt = row[SubAccounts.createdAt].toString(),
                    updatedAt = row[SubAccounts.updatedAt].toString()
                )
            }.singleOrNull()
    }

    suspend fun getActiveSubAccounts(customerId: UUID): List<SubAccountResponse> = DatabaseFactory.dbQuery {
        SubAccounts.select {
            (SubAccounts.customerId eq customerId) and (SubAccounts.isActive eq true)
        }
            .orderBy(SubAccounts.createdAt to SortOrder.DESC)
            .map { row ->
                val currentBalance = row[SubAccounts.currentBalance]
                val targetAmount = row[SubAccounts.targetAmount]
                val progressPercentage = if (targetAmount != null && targetAmount > java.math.BigDecimal.ZERO) {
                    (currentBalance.toDouble() / targetAmount.toDouble() * 100).coerceAtMost(100.0)
                } else {
                    0.0
                }

                SubAccountResponse(
                    id = row[SubAccounts.id].toString(),
                    customerId = row[SubAccounts.customerId].toString(),
                    parentAccountId = row[SubAccounts.parentAccountId].toString(),
                    name = row[SubAccounts.name],
                    description = row[SubAccounts.description],
                    targetAmount = targetAmount?.toString(),
                    currentBalance = currentBalance.toString(),
                    iconName = row[SubAccounts.iconName],
                    colorHex = row[SubAccounts.colorHex],
                    targetDate = row[SubAccounts.targetDate]?.toString(),
                    isLocked = row[SubAccounts.isLocked],
                    autoTransferAmount = row[SubAccounts.autoTransferAmount]?.toString(),
                    autoTransferFrequency = row[SubAccounts.autoTransferFrequency],
                    isActive = row[SubAccounts.isActive],
                    progressPercentage = progressPercentage,
                    createdAt = row[SubAccounts.createdAt].toString(),
                    updatedAt = row[SubAccounts.updatedAt].toString()
                )
            }
    }

    suspend fun createSubAccount(request: CreateSubAccountRequest): SubAccountResponse = DatabaseFactory.dbQuery {
        // Get the customer's primary account if parentAccountId is actually a customerId
        val actualParentAccountId: UUID = try {
            val parentId = UUID.fromString(request.parentAccountId)
            // Check if this ID exists in accounts table
            val accountExists = Accounts.select { Accounts.id eq parentId }.count() > 0
            if (accountExists) {
                parentId
            } else {
                // It's likely a customer ID, so get their primary account
                val accountEntityId = Accounts.select { Accounts.customerId eq parentId }
                    .orderBy(Accounts.createdAt to SortOrder.ASC)
                    .firstOrNull()
                    ?.get(Accounts.id)
                    ?: throw Exception("No account found for customer")
                accountEntityId.value
            }
        } catch (e: Exception) {
            throw Exception("Invalid parent account ID: ${e.message}")
        }

        val subAccountId = SubAccounts.insertAndGetId {
            it[customerId] = UUID.fromString(request.customerId)
            it[parentAccountId] = actualParentAccountId
            it[name] = request.name
            it[description] = request.description
            it[targetAmount] = request.targetAmount?.toBigDecimalOrNull()
            it[iconName] = request.iconName
            it[colorHex] = request.colorHex
            it[targetDate] = request.targetDate?.let { date -> LocalDate.parse(date) }
            it[autoTransferAmount] = request.autoTransferAmount?.toBigDecimalOrNull()
            it[autoTransferFrequency] = request.autoTransferFrequency
        }

        // Return the newly created sub-account
        val row = SubAccounts.select { SubAccounts.id eq subAccountId }.single()
        val targetAmountBD = row[SubAccounts.targetAmount]
        val currentBalanceBD = row[SubAccounts.currentBalance]
        val progressPercentage = if (targetAmountBD != null && targetAmountBD > java.math.BigDecimal.ZERO) {
            (currentBalanceBD.toDouble() / targetAmountBD.toDouble() * 100).coerceAtMost(100.0)
        } else {
            0.0
        }

        SubAccountResponse(
            id = subAccountId.toString(),
            customerId = request.customerId,
            parentAccountId = actualParentAccountId.toString(),
            name = request.name,
            description = request.description,
            targetAmount = targetAmountBD?.toString(),
            currentBalance = currentBalanceBD.toString(),
            iconName = request.iconName,
            colorHex = request.colorHex,
            targetDate = row[SubAccounts.targetDate]?.toString(),
            isLocked = row[SubAccounts.isLocked],
            autoTransferAmount = row[SubAccounts.autoTransferAmount]?.toString(),
            autoTransferFrequency = row[SubAccounts.autoTransferFrequency],
            isActive = row[SubAccounts.isActive],
            progressPercentage = progressPercentage,
            createdAt = row[SubAccounts.createdAt].toString(),
            updatedAt = row[SubAccounts.updatedAt].toString()
        )
    }

    suspend fun updateSubAccount(subAccountId: UUID, request: UpdateSubAccountRequest): SubAccountResponse? = DatabaseFactory.dbQuery {
        val updated = SubAccounts.update({ SubAccounts.id eq subAccountId }) { stmt ->
            request.name?.let { stmt[name] = it }
            request.description?.let { stmt[description] = it }
            request.targetAmount?.let { stmt[targetAmount] = it.toBigDecimalOrNull() }
            request.iconName?.let { stmt[iconName] = it }
            request.colorHex?.let { stmt[colorHex] = it }
            request.targetDate?.let { stmt[targetDate] = LocalDate.parse(it) }
            request.isLocked?.let { stmt[isLocked] = it }
            request.autoTransferAmount?.let { stmt[autoTransferAmount] = it.toBigDecimalOrNull() }
            request.autoTransferFrequency?.let { stmt[autoTransferFrequency] = it }
            request.isActive?.let { stmt[isActive] = it }
            stmt[updatedAt] = Instant.now()
        }

        if (updated > 0) getSubAccountById(subAccountId) else null
    }

    suspend fun deleteSubAccount(subAccountId: UUID): Boolean = DatabaseFactory.dbQuery {
        // Check if sub-account has balance
        val subAccount = SubAccounts.select { SubAccounts.id eq subAccountId }.firstOrNull()
        if (subAccount != null) {
            val balance = subAccount[SubAccounts.currentBalance]
            if (balance > java.math.BigDecimal.ZERO) {
                throw IllegalStateException("Cannot delete sub-account with non-zero balance. Please transfer funds first.")
            }
        }

        val deleted = SubAccounts.deleteWhere {
            Op.build { SubAccounts.id eq subAccountId }
        }
        deleted > 0
    }

    suspend fun transferToSubAccount(request: TransferToSubAccountRequest): TransferResponse = DatabaseFactory.dbQuery {
        val subAccountId = UUID.fromString(request.subAccountId)
        val amount = java.math.BigDecimal(request.amount)

        // Get sub-account
        val subAccount = SubAccounts.select { SubAccounts.id eq subAccountId }.firstOrNull()
            ?: throw IllegalArgumentException("Sub-account not found")

        if (!subAccount[SubAccounts.isActive]) {
            throw IllegalStateException("Sub-account is not active")
        }

        if (subAccount[SubAccounts.isLocked]) {
            throw IllegalStateException("Sub-account is locked")
        }

        val parentAccountId = subAccount[SubAccounts.parentAccountId]
        val currentSubBalance = subAccount[SubAccounts.currentBalance]
        val newSubBalance = currentSubBalance + amount

        var newParentBalance = java.math.BigDecimal.ZERO

        // If it's a direct deposit (e.g., M-Pesa), skip parent account balance check
        if (!request.isDirectDeposit) {
            // Get parent account
            val parentAccount = Accounts.select { Accounts.id eq parentAccountId }.firstOrNull()
                ?: throw IllegalArgumentException("Parent account not found")

            val parentBalance = parentAccount[Accounts.balance]

            // Check if parent has sufficient balance
            if (parentBalance < amount) {
                throw IllegalStateException("Insufficient balance in parent account")
            }

            // Calculate new parent balance
            newParentBalance = parentBalance - amount

            // Deduct from parent account
            Accounts.update({ Accounts.id eq parentAccountId }) {
                it[balance] = newParentBalance
                it[availableBalance] = newParentBalance
            }

            // Create transaction record for internal transfer
            val transactionService = TransactionService()
            transactionService.createTransaction(
                CreateTransactionRequest(
                    accountId = parentAccountId.toString(),
                    type = "SUB_ACCOUNT_TRANSFER",
                    amount = amount.toString(),
                    description = request.description ?: "Transfer to ${subAccount[SubAccounts.name]}",
                    fromAccountId = parentAccountId.toString(),
                    toAccountId = null
                )
            )
        } else {
            // For direct deposits, get current parent balance for response
            val parentAccount = Accounts.select { Accounts.id eq parentAccountId }.firstOrNull()
            newParentBalance = parentAccount?.get(Accounts.balance) ?: java.math.BigDecimal.ZERO
        }

        // Add to sub-account (for both direct deposits and internal transfers)
        SubAccounts.update({ SubAccounts.id eq subAccountId }) {
            it[SubAccounts.currentBalance] = newSubBalance
            it[SubAccounts.updatedAt] = CurrentTimestamp()
        }

        val updatedSubAccount = getSubAccountById(subAccountId)!!

        TransferResponse(
            success = true,
            message = if (request.isDirectDeposit) "Funds added successfully" else "Transfer successful",
            subAccount = updatedSubAccount,
            newParentBalance = newParentBalance.toString()
        )
    }

    suspend fun withdrawFromSubAccount(request: TransferToSubAccountRequest): TransferResponse = DatabaseFactory.dbQuery {
        val subAccountId = UUID.fromString(request.subAccountId)
        val amount = java.math.BigDecimal(request.amount)

        // Get sub-account
        val subAccount = SubAccounts.select { SubAccounts.id eq subAccountId }.firstOrNull()
            ?: throw IllegalArgumentException("Sub-account not found")

        if (!subAccount[SubAccounts.isActive]) {
            throw IllegalStateException("Sub-account is not active")
        }

        if (subAccount[SubAccounts.isLocked]) {
            throw IllegalStateException("Sub-account is locked")
        }

        val currentBalance = subAccount[SubAccounts.currentBalance]
        if (currentBalance < amount) {
            throw IllegalStateException("Insufficient balance in sub-account")
        }

        val parentAccountId = subAccount[SubAccounts.parentAccountId]

        // Calculate new balances
        val newSubBalance = currentBalance - amount

        // Get parent account balance
        val parentAccount = Accounts.select { Accounts.id eq parentAccountId }.firstOrNull()
            ?: throw IllegalArgumentException("Parent account not found")
        val parentBalance = parentAccount[Accounts.balance]
        val newParentBalance = parentBalance + amount

        // Deduct from sub-account
        SubAccounts.update({ SubAccounts.id eq subAccountId }) {
            it[SubAccounts.currentBalance] = newSubBalance
            it[SubAccounts.updatedAt] = CurrentTimestamp()
        }

        // Add to parent account
        Accounts.update({ Accounts.id eq parentAccountId }) {
            it[balance] = newParentBalance
            it[availableBalance] = newParentBalance
        }

        // Create transaction record
        val transactionService = TransactionService()
        transactionService.createTransaction(
            CreateTransactionRequest(
                accountId = parentAccountId.toString(),
                type = "SUB_ACCOUNT_WITHDRAWAL",
                amount = amount.toString(),
                description = request.description ?: "Withdrawal from ${subAccount[SubAccounts.name]}",
                fromAccountId = null,
                toAccountId = parentAccountId.toString()
            )
        )

        val updatedSubAccount = getSubAccountById(subAccountId)!!

        TransferResponse(
            success = true,
            message = "Withdrawal successful",
            subAccount = updatedSubAccount,
            newParentBalance = newParentBalance.toString()
        )
    }

    suspend fun toggleLock(subAccountId: UUID): SubAccountResponse? = DatabaseFactory.dbQuery {
        val current = getSubAccountById(subAccountId) ?: return@dbQuery null

        SubAccounts.update({ SubAccounts.id eq subAccountId }) {
            it[SubAccounts.isLocked] =!current.isLocked
            it[SubAccounts.updatedAt] = CurrentTimestamp()
        }

        getSubAccountById(subAccountId)
    }
}
