package org.dals.project.services

import org.dals.project.database.*
import org.dals.project.utils.IdGenerator
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.Instant
import java.util.*
import kotlin.random.Random

// ==================== WALLET DOMAIN MODEL FOR CUSTOM ACCESS ====================

data class MasterWallet(
    val id: UUID,
    val name: String,
    val type: MasterWalletType,
    val currentBalance: BigDecimal,
    val maxBalance: BigDecimal?,
    val minBalance: BigDecimal?,
    val securityLevel: WalletSecurityLevel,
    val status: WalletStatus,
    val createdAt: String,
    val updatedAt: String
)

class MasterWalletService {

    /**
     * Get wallet by ID for M-Pesa deposits
     */
    suspend fun getWalletByIdForDeposit(walletId: String): MasterWallet? {
        return try {
            org.dals.project.database.DatabaseFactory.dbQuery {
                MasterWallets.select { MasterWallets.id eq UUID.fromString(walletId) }
                    .map { row ->
                        MasterWallet(
                            id = row[MasterWallets.id].value,
                            name = row[MasterWallets.walletName],
                            type = row[MasterWallets.walletType],
                            currentBalance = row[MasterWallets.balance],
                            maxBalance = row[MasterWallets.monthlyTransactionLimit],
                            minBalance = row[MasterWallets.dailyTransactionLimit],
                            securityLevel = row[MasterWallets.securityLevel],
                            status = row[MasterWallets.status],
                            createdAt = row[MasterWallets.createdAt].toString(),
                            updatedAt = row[MasterWallets.updatedAt].toString()
                        )
                    }
                    .singleOrNull()
            }
        } catch (e: Exception) {
            println("Error fetching wallet by ID: ${e.message}")
            null
        }
    }

    // ==================== WALLET OPERATIONS ====================

    fun getAllWallets(page: Int = 1, pageSize: Int = 10): ApiResponse<List<WalletDto>> = transaction {
        try {
            val wallets = MasterWallets.selectAll()
                .limit(pageSize, ((page - 1) * pageSize).toLong())
                .map { row ->
                    WalletDto(
                        id = row[MasterWallets.id].value.toString(),
                        walletName = row[MasterWallets.walletName],
                        walletType = row[MasterWallets.walletType].name,
                        currency = row[MasterWallets.currency],
                        balance = row[MasterWallets.balance].toDouble(),
                        availableBalance = row[MasterWallets.availableBalance].toDouble(),
                        reserveBalance = row[MasterWallets.reserveBalance].toDouble(),
                        securityLevel = row[MasterWallets.securityLevel].name,
                        status = row[MasterWallets.status].name,
                        maxSingleTransaction = row[MasterWallets.monthlyTransactionLimit].toDouble(),
                        dailyTransactionLimit = row[MasterWallets.dailyTransactionLimit].toDouble(),
                        monthlyTransactionLimit = row[MasterWallets.monthlyTransactionLimit].toDouble(),
                        createdBy = row[MasterWallets.createdBy].toString(),
                        managedBy = row[MasterWallets.managedBy].toString(),
                        branchId = null,
                        createdAt = row[MasterWallets.createdAt].toString(),
                        updatedAt = row[MasterWallets.updatedAt].toString()
                    )
                }

            ApiResponse(
                success = true,
                message = "Wallets retrieved successfully",
                data = wallets
            )
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Failed to retrieve wallets",
                error = e.message
            )
        }
    }

    fun getWalletById(walletId: UUID): ApiResponse<WalletDto> = transaction {
        try {
            val wallet = MasterWallets.select { MasterWallets.id eq walletId }
                .singleOrNull()?.let { row ->
                    WalletDto(
                        id = row[MasterWallets.id].value.toString(),
                        walletName = row[MasterWallets.walletName],
                        walletType = row[MasterWallets.walletType].name,
                        currency = row[MasterWallets.currency],
                        balance = row[MasterWallets.balance].toDouble(),
                        availableBalance = row[MasterWallets.availableBalance].toDouble(),
                        reserveBalance = row[MasterWallets.reserveBalance].toDouble(),
                        securityLevel = row[MasterWallets.securityLevel].name,
                        status = row[MasterWallets.status].name,
                        maxSingleTransaction = row[MasterWallets.monthlyTransactionLimit].toDouble(),
                        dailyTransactionLimit = row[MasterWallets.dailyTransactionLimit].toDouble(),
                        monthlyTransactionLimit = row[MasterWallets.monthlyTransactionLimit].toDouble(),
                        createdBy = row[MasterWallets.createdBy].toString(),
                        managedBy = row[MasterWallets.managedBy].toString(),
                        branchId = null,
                        createdAt = row[MasterWallets.createdAt].toString(),
                        updatedAt = row[MasterWallets.updatedAt].toString()
                    )
                }

            if (wallet != null) {
                ApiResponse(
                    success = true,
                    message = "Wallet retrieved successfully",
                    data = wallet
                )
            } else {
                ApiResponse(
                    success = false,
                    message = "Wallet not found"
                )
            }
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Failed to retrieve wallet",
                error = e.message
            )
        }
    }

    fun getWalletById(walletId: String): WalletDto? = transaction {
        MasterWallets.select { MasterWallets.id eq UUID.fromString(walletId) }
            .singleOrNull()?.let { row ->
                WalletDto(
                    id = row[MasterWallets.id].value.toString(),
                    walletName = row[MasterWallets.walletName],
                    walletType = row[MasterWallets.walletType].name,
                    currency = row[MasterWallets.currency],
                    balance = row[MasterWallets.balance].toDouble(),
                    availableBalance = row[MasterWallets.availableBalance].toDouble(),
                    reserveBalance = row[MasterWallets.reserveBalance].toDouble(),
                    securityLevel = row[MasterWallets.securityLevel].name,
                    status = row[MasterWallets.status].name,
                    maxSingleTransaction = row[MasterWallets.monthlyTransactionLimit].toDouble(),
                    dailyTransactionLimit = row[MasterWallets.dailyTransactionLimit].toDouble(),
                    monthlyTransactionLimit = row[MasterWallets.monthlyTransactionLimit].toDouble(),
                    createdBy = row[MasterWallets.createdBy].toString(),
                    managedBy = row[MasterWallets.managedBy].toString(),
                    branchId = null,
                    createdAt = row[MasterWallets.createdAt].toString(),
                    updatedAt = row[MasterWallets.updatedAt].toString()
                )
            }
    }

    fun createWallet(request: CreateWalletRequest): ApiResponse<WalletDto> = transaction {
        try {
            val createdBy = UUID.fromString(request.createdBy)

            val walletId = MasterWallets.insertAndGetId {
                it[MasterWallets.walletName] = request.walletName
                it[MasterWallets.walletType] = MasterWalletType.valueOf(request.walletType)
                it[MasterWallets.balance] = BigDecimal(request.initialBalance)
                it[MasterWallets.availableBalance] = BigDecimal(request.initialBalance)
                it[MasterWallets.reserveBalance] = BigDecimal.ZERO
                it[MasterWallets.securityLevel] = WalletSecurityLevel.valueOf(request.securityLevel)
                it[MasterWallets.dailyTransactionLimit] = BigDecimal(request.dailyLimit)
                it[MasterWallets.monthlyTransactionLimit] = BigDecimal(request.monthlyLimit)
                it[MasterWallets.createdBy] = createdBy
                it[MasterWallets.managedBy] = createdBy
                it[MasterWallets.authorizedUsers] = "[\"$createdBy\"]" // JSON array
                it[MasterWallets.encryptionKey] = generateEncryptionKey()
            }

            // Create audit entry
            createAuditEntry(
                walletId = walletId.value,
                action = "WALLET_CREATED",
                performedBy = createdBy,
                details = "Wallet created: ${request.walletName}"
            )

            val createdWallet = getWalletById(walletId.value.toString())!!

            ApiResponse(
                success = true,
                message = "Wallet created successfully",
                data = createdWallet
            )
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Failed to create wallet",
                error = e.message
            )
        }
    }

    fun createWallet(request: CreateMasterWalletRequest, createdBy: UUID): WalletDto = transaction {
        val walletId = MasterWallets.insertAndGetId {
            it[MasterWallets.walletName] = request.walletName
            it[MasterWallets.walletType] = MasterWalletType.valueOf(request.walletType)
            it[MasterWallets.balance] = BigDecimal(request.initialBalance)
            it[MasterWallets.availableBalance] = BigDecimal(request.initialBalance)
            it[MasterWallets.reserveBalance] = BigDecimal.ZERO
            it[MasterWallets.securityLevel] = WalletSecurityLevel.valueOf(request.securityLevel)
            it[MasterWallets.dailyTransactionLimit] = BigDecimal(request.dailyLimit)
            it[MasterWallets.monthlyTransactionLimit] = BigDecimal(request.monthlyLimit)
            it[MasterWallets.createdBy] = createdBy
            it[MasterWallets.managedBy] = createdBy
            it[MasterWallets.authorizedUsers] = "[\"$createdBy\"]" // JSON array
            it[MasterWallets.encryptionKey] = generateEncryptionKey()
        }

        // Create audit entry  
        createAuditEntry(
            walletId = walletId.value,
            action = "WALLET_CREATED",
            performedBy = createdBy,
            details = "Wallet created: ${request.walletName}"
        )

        getWalletById(walletId.value.toString())!!
    }

    fun updateWallet(walletId: UUID, request: UpdateWalletRequest): ApiResponse<WalletDto> = transaction {
        try {
            val updated = MasterWallets.update({ MasterWallets.id eq walletId }) {
                it[MasterWallets.walletName] = request.walletName
                it[MasterWallets.dailyTransactionLimit] = BigDecimal(request.dailyLimit)
                it[MasterWallets.monthlyTransactionLimit] = BigDecimal(request.monthlyLimit)
            }

            if (updated > 0) {
                val updatedWallet = getWalletById(walletId.toString())!!
                ApiResponse(
                    success = true,
                    message = "Wallet updated successfully",
                    data = updatedWallet
                )
            } else {
                ApiResponse(
                    success = false,
                    message = "Wallet not found"
                )
            }
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Failed to update wallet",
                error = e.message
            )
        }
    }

    fun deleteWallet(walletId: UUID): ApiResponse<String> = transaction {
        try {
            val deleted = MasterWallets.update({ MasterWallets.id eq walletId }) {
                it[MasterWallets.status] = WalletStatus.CLOSED
            }

            if (deleted > 0) {
                ApiResponse(
                    success = true,
                    message = "Wallet deactivated successfully"
                )
            } else {
                ApiResponse(
                    success = false,
                    message = "Wallet not found"
                )
            }
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Failed to deactivate wallet",
                error = e.message
            )
        }
    }

    // ==================== TRANSACTION OPERATIONS ====================

    fun getAllWalletTransactions(page: Int = 1, pageSize: Int = 10): ApiResponse<List<WalletTransactionDto>> =
        transaction {
            try {
                val transactions = MasterWalletTransactions.selectAll()
                    .orderBy(MasterWalletTransactions.processedAt, SortOrder.DESC)
                    .limit(pageSize, ((page - 1) * pageSize).toLong())
                    .map { row ->
                        WalletTransactionDto(
                            id = row[MasterWalletTransactions.id].value.toString(),
                            walletId = row[MasterWalletTransactions.walletId].toString(),
                            transactionType = row[MasterWalletTransactions.transactionType].name,
                            amount = row[MasterWalletTransactions.amount].toDouble(),
                            currency = row[MasterWalletTransactions.currency],
                            balanceBefore = row[MasterWalletTransactions.balanceBefore].toDouble(),
                            balanceAfter = row[MasterWalletTransactions.balanceAfter].toDouble(),
                            description = row[MasterWalletTransactions.description],
                            reference = row[MasterWalletTransactions.reference],
                            counterpartyWalletId = row[MasterWalletTransactions.counterpartyWalletId]?.toString(),
                            status = row[MasterWalletTransactions.status].name,
                            processedBy = row[MasterWalletTransactions.processedBy].toString(),
                            processedAt = row[MasterWalletTransactions.processedAt].toString(),
                            createdAt = row[MasterWalletTransactions.createdAt].toString()
                        )
                    }

                ApiResponse(
                    success = true,
                    message = "Transactions retrieved successfully",
                    data = transactions
                )
            } catch (e: Exception) {
                ApiResponse(
                    success = false,
                    message = "Failed to retrieve transactions",
                    error = e.message
                )
            }
        }

    fun getWalletTransactionById(transactionId: UUID): ApiResponse<WalletTransactionDto> = transaction {
        try {
            val transaction = MasterWalletTransactions.select { MasterWalletTransactions.id eq transactionId }
                .singleOrNull()?.let { row ->
                    WalletTransactionDto(
                        id = row[MasterWalletTransactions.id].value.toString(),
                        walletId = row[MasterWalletTransactions.walletId].toString(),
                        transactionType = row[MasterWalletTransactions.transactionType].name,
                        amount = row[MasterWalletTransactions.amount].toDouble(),
                        currency = row[MasterWalletTransactions.currency],
                        balanceBefore = row[MasterWalletTransactions.balanceBefore].toDouble(),
                        balanceAfter = row[MasterWalletTransactions.balanceAfter].toDouble(),
                        description = row[MasterWalletTransactions.description],
                        reference = row[MasterWalletTransactions.reference],
                        counterpartyWalletId = row[MasterWalletTransactions.counterpartyWalletId]?.toString(),
                        status = row[MasterWalletTransactions.status].name,
                        processedBy = row[MasterWalletTransactions.processedBy].toString(),
                        processedAt = row[MasterWalletTransactions.processedAt].toString(),
                        createdAt = row[MasterWalletTransactions.createdAt].toString()
                    )
                }

            if (transaction != null) {
                ApiResponse(
                    success = true,
                    message = "Transaction retrieved successfully",
                    data = transaction
                )
            } else {
                ApiResponse(
                    success = false,
                    message = "Transaction not found"
                )
            }
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Failed to retrieve transaction",
                error = e.message
            )
        }
    }

    fun createWalletTransaction(request: CreateWalletTransactionRequest): ApiResponse<WalletTransactionDto> =
        transaction {
            try {
                val processedBy = UUID.fromString(request.processedBy)

                val wallet = MasterWallets.select { MasterWallets.id eq UUID.fromString(request.walletId) }
                    .single()

                val currentBalance = wallet[MasterWallets.balance]
                val amount = BigDecimal(request.amount)
                val newBalance = when (MasterWalletTransactionType.valueOf(request.transactionType)) {
                    MasterWalletTransactionType.FUND_ALLOCATION,
                    MasterWalletTransactionType.FUND_TRANSFER -> currentBalance + amount.abs()

                    else -> currentBalance - amount.abs()
                }

                // Insert transaction
                val transactionId = MasterWalletTransactions.insertAndGetId {
                    it[MasterWalletTransactions.walletId] = UUID.fromString(request.walletId)
                    it[MasterWalletTransactions.transactionType] =
                        MasterWalletTransactionType.valueOf(request.transactionType)
                    it[MasterWalletTransactions.amount] = amount
                    it[MasterWalletTransactions.balanceBefore] = currentBalance
                    it[MasterWalletTransactions.balanceAfter] = newBalance
                    it[MasterWalletTransactions.description] = request.description
                    it[MasterWalletTransactions.processedBy] = processedBy
                    it[MasterWalletTransactions.status] = TransactionStatus.COMPLETED
                    it[MasterWalletTransactions.reference] = IdGenerator.generateTransactionId()
                    it[MasterWalletTransactions.riskScore] = BigDecimal(calculateRiskScore(amount.toDouble()))
                    it[MasterWalletTransactions.riskLevel] = determineRiskLevel(amount.toDouble())
                    it[MasterWalletTransactions.currency] = wallet[MasterWallets.currency]
                }

                // Update wallet balance
                MasterWallets.update({ MasterWallets.id eq UUID.fromString(request.walletId) }) {
                    it[MasterWallets.balance] = newBalance
                    it[MasterWallets.availableBalance] = newBalance
                }

                // Create audit entry
                createAuditEntry(
                    walletId = UUID.fromString(request.walletId),
                    action = "TRANSACTION_PROCESSED",
                    performedBy = processedBy,
                    details = "Transaction processed: ${request.description}"
                )

                val createdTransaction = WalletTransactionDto(
                    id = transactionId.value.toString(),
                    walletId = request.walletId,
                    transactionType = request.transactionType,
                    amount = request.amount,
                    currency = wallet[MasterWallets.currency],
                    balanceBefore = currentBalance.toDouble(),
                    balanceAfter = newBalance.toDouble(),
                    description = request.description,
                    reference = IdGenerator.generateTransactionId(),
                    counterpartyWalletId = null,
                    status = "COMPLETED",
                    processedBy = processedBy.toString(),
                    processedAt = LocalDateTime.now().toString(),
                    createdAt = LocalDateTime.now().toString()
                )

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

    fun processTransaction(request: WalletTransactionRequest, processedBy: UUID): WalletTransactionDto = transaction {
        val wallet = MasterWallets.select { MasterWallets.id eq UUID.fromString(request.walletId) }
            .single()

        val currentBalance = wallet[MasterWallets.balance]
        val amount = BigDecimal(request.amount)
        val newBalance = when (MasterWalletTransactionType.valueOf(request.transactionType)) {
            MasterWalletTransactionType.FUND_ALLOCATION,
            MasterWalletTransactionType.FUND_TRANSFER -> currentBalance + amount.abs()
            else -> currentBalance - amount.abs()
        }

        // Insert transaction
        val transactionId = MasterWalletTransactions.insertAndGetId {
            it[MasterWalletTransactions.walletId] = UUID.fromString(request.walletId)
            it[MasterWalletTransactions.transactionType] = MasterWalletTransactionType.valueOf(request.transactionType)
            it[MasterWalletTransactions.amount] = amount
            it[MasterWalletTransactions.balanceBefore] = currentBalance
            it[MasterWalletTransactions.balanceAfter] = newBalance
            it[MasterWalletTransactions.description] = request.description
            it[MasterWalletTransactions.processedBy] = processedBy
            it[MasterWalletTransactions.status] = TransactionStatus.COMPLETED
            it[MasterWalletTransactions.reference] = IdGenerator.generateTransactionId()
            it[MasterWalletTransactions.riskScore] = BigDecimal(calculateRiskScore(amount.toDouble()))
            it[MasterWalletTransactions.riskLevel] = determineRiskLevel(amount.toDouble())
            it[MasterWalletTransactions.currency] = wallet[MasterWallets.currency]
        }

        // Update wallet balance
        MasterWallets.update({ MasterWallets.id eq UUID.fromString(request.walletId) }) {
            it[MasterWallets.balance] = newBalance
            it[MasterWallets.availableBalance] = newBalance
        }

        // Create audit entry
        createAuditEntry(
            walletId = UUID.fromString(request.walletId),
            action = "TRANSACTION_PROCESSED",
            performedBy = processedBy,
            details = "Transaction processed: ${request.description}"
        )

        WalletTransactionDto(
            id = transactionId.value.toString(),
            walletId = request.walletId,
            transactionType = request.transactionType,
            amount = request.amount,
            currency = wallet[MasterWallets.currency],
            balanceBefore = currentBalance.toDouble(),
            balanceAfter = newBalance.toDouble(),
            description = request.description,
            reference = "REF${Random.nextInt(100000, 999999)}",
            counterpartyWalletId = null,
            status = "COMPLETED",
            processedBy = processedBy.toString(),
            processedAt = LocalDateTime.now().toString(),
            createdAt = LocalDateTime.now().toString()
        )
    }

    /**
     * Process wallet-to-wallet transfer - ensures BOTH sender and recipient wallets are updated
     */
    fun processWalletTransfer(
        fromWalletId: String,
        toWalletId: String,
        amount: Double,
        description: String,
        processedBy: UUID
    ): ApiResponse<WalletTransferResult> = transaction {
        try {
            println("💸 Processing wallet transfer: From=$fromWalletId, To=$toWalletId, Amount=$amount")

            // Validate amount
            if (amount <= 0) {
                return@transaction ApiResponse(
                    success = false,
                    message = "Transfer amount must be greater than zero",
                    error = "INVALID_AMOUNT"
                )
            }

            // Get sender wallet
            val senderWallet = MasterWallets.select {
                MasterWallets.id eq UUID.fromString(fromWalletId)
            }.singleOrNull()

            if (senderWallet == null) {
                return@transaction ApiResponse(
                    success = false,
                    message = "Sender wallet not found",
                    error = "SENDER_NOT_FOUND"
                )
            }

            // Get recipient wallet
            val recipientWallet = MasterWallets.select {
                MasterWallets.id eq UUID.fromString(toWalletId)
            }.singleOrNull()

            if (recipientWallet == null) {
                return@transaction ApiResponse(
                    success = false,
                    message = "Recipient wallet not found",
                    error = "RECIPIENT_NOT_FOUND"
                )
            }

            val senderBalance = senderWallet[MasterWallets.balance]
            val senderAvailable = senderWallet[MasterWallets.availableBalance]
            val transferAmount = BigDecimal(amount)

            // Check sufficient balance
            if (senderAvailable < transferAmount) {
                return@transaction ApiResponse(
                    success = false,
                    message = "Insufficient balance in sender wallet",
                    error = "INSUFFICIENT_BALANCE",
                    data = WalletTransferResult(
                        success = false,
                        transactionReference = null,
                        senderBalanceBefore = senderBalance.toDouble(),
                        senderBalanceAfter = senderBalance.toDouble(),
                        recipientBalanceBefore = recipientWallet[MasterWallets.balance].toDouble(),
                        recipientBalanceAfter = recipientWallet[MasterWallets.balance].toDouble()
                    )
                )
            }

            val recipientBalance = recipientWallet[MasterWallets.balance]
            val newSenderBalance = senderBalance - transferAmount
            val newRecipientBalance = recipientBalance + transferAmount

            val transactionReference = "TXN${System.currentTimeMillis()}${Random.nextInt(1000, 9999)}"

            // Deduct from sender wallet
            MasterWalletTransactions.insert {
                it[MasterWalletTransactions.walletId] = UUID.fromString(fromWalletId)
                it[MasterWalletTransactions.transactionType] = MasterWalletTransactionType.FUND_TRANSFER
                it[MasterWalletTransactions.amount] = transferAmount
                it[MasterWalletTransactions.balanceBefore] = senderBalance
                it[MasterWalletTransactions.balanceAfter] = newSenderBalance
                it[MasterWalletTransactions.description] = "Transfer to ${recipientWallet[MasterWallets.walletName]}: $description"
                it[MasterWalletTransactions.reference] = transactionReference
                it[MasterWalletTransactions.counterpartyWalletId] = UUID.fromString(toWalletId)
                it[MasterWalletTransactions.processedBy] = processedBy
                it[MasterWalletTransactions.status] = TransactionStatus.COMPLETED
                it[MasterWalletTransactions.riskScore] = BigDecimal(calculateRiskScore(amount))
                it[MasterWalletTransactions.riskLevel] = determineRiskLevel(amount)
                it[MasterWalletTransactions.currency] = senderWallet[MasterWallets.currency]
            }

            MasterWallets.update({ MasterWallets.id eq UUID.fromString(fromWalletId) }) {
                it[MasterWallets.balance] = newSenderBalance
                it[MasterWallets.availableBalance] = newSenderBalance
            }

            // Add to recipient wallet
            MasterWalletTransactions.insert {
                it[MasterWalletTransactions.walletId] = UUID.fromString(toWalletId)
                it[MasterWalletTransactions.transactionType] = MasterWalletTransactionType.FUND_TRANSFER
                it[MasterWalletTransactions.amount] = transferAmount
                it[MasterWalletTransactions.balanceBefore] = recipientBalance
                it[MasterWalletTransactions.balanceAfter] = newRecipientBalance
                it[MasterWalletTransactions.description] = "Transfer from ${senderWallet[MasterWallets.walletName]}: $description"
                it[MasterWalletTransactions.reference] = transactionReference
                it[MasterWalletTransactions.counterpartyWalletId] = UUID.fromString(fromWalletId)
                it[MasterWalletTransactions.processedBy] = processedBy
                it[MasterWalletTransactions.status] = TransactionStatus.COMPLETED
                it[MasterWalletTransactions.riskScore] = BigDecimal(calculateRiskScore(amount))
                it[MasterWalletTransactions.riskLevel] = determineRiskLevel(amount)
                it[MasterWalletTransactions.currency] = recipientWallet[MasterWallets.currency]
            }

            MasterWallets.update({ MasterWallets.id eq UUID.fromString(toWalletId) }) {
                it[MasterWallets.balance] = newRecipientBalance
                it[MasterWallets.availableBalance] = newRecipientBalance
            }

            // Create audit entries
            createAuditEntry(
                walletId = UUID.fromString(fromWalletId),
                action = "WALLET_TRANSFER_OUT",
                performedBy = processedBy,
                details = "Transferred $amount to $toWalletId: $description"
            )

            createAuditEntry(
                walletId = UUID.fromString(toWalletId),
                action = "WALLET_TRANSFER_IN",
                performedBy = processedBy,
                details = "Received $amount from $fromWalletId: $description"
            )

            println("✅ Wallet transfer completed successfully")
            println("   Sender: $fromWalletId, New balance: $newSenderBalance")
            println("   Recipient: $toWalletId, New balance: $newRecipientBalance")

            ApiResponse(
                success = true,
                message = "Transfer completed successfully",
                data = WalletTransferResult(
                    success = true,
                    transactionReference = transactionReference,
                    senderBalanceBefore = senderBalance.toDouble(),
                    senderBalanceAfter = newSenderBalance.toDouble(),
                    recipientBalanceBefore = recipientBalance.toDouble(),
                    recipientBalanceAfter = newRecipientBalance.toDouble()
                )
            )

        } catch (e: Exception) {
            println("❌ Failed to process wallet transfer: ${e.message}")
            e.printStackTrace()
            ApiResponse(
                success = false,
                message = "Failed to process transfer",
                error = e.message
            )
        }
    }

    // ==================== ADDITIONAL API METHODS ====================

    fun getAllWallets(): List<WalletDto> = transaction {
        MasterWallets.selectAll().map { row ->
            WalletDto(
                id = row[MasterWallets.id].value.toString(),
                walletName = row[MasterWallets.walletName],
                walletType = row[MasterWallets.walletType].name,
                currency = row[MasterWallets.currency],
                balance = row[MasterWallets.balance].toDouble(),
                availableBalance = row[MasterWallets.availableBalance].toDouble(),
                reserveBalance = row[MasterWallets.reserveBalance].toDouble(),
                securityLevel = row[MasterWallets.securityLevel].name,
                status = row[MasterWallets.status].name,
                maxSingleTransaction = row[MasterWallets.monthlyTransactionLimit].toDouble(),
                dailyTransactionLimit = row[MasterWallets.dailyTransactionLimit].toDouble(),
                monthlyTransactionLimit = row[MasterWallets.monthlyTransactionLimit].toDouble(),
                createdBy = row[MasterWallets.createdBy].toString(),
                managedBy = row[MasterWallets.managedBy].toString(),
                branchId = null,
                createdAt = row[MasterWallets.createdAt].toString(),
                updatedAt = row[MasterWallets.updatedAt].toString()
            )
        }
    }

    fun getAllTransactions(walletId: String? = null): List<WalletTransactionDto> = transaction {
        val query = if (walletId != null) {
            MasterWalletTransactions.select { MasterWalletTransactions.walletId eq UUID.fromString(walletId) }
        } else {
            MasterWalletTransactions.selectAll()
        }

        query.orderBy(MasterWalletTransactions.processedAt, SortOrder.DESC)
            .limit(100)
            .map { row ->
                WalletTransactionDto(
                    id = row[MasterWalletTransactions.id].value.toString(),
                    walletId = row[MasterWalletTransactions.walletId].toString(),
                    transactionType = row[MasterWalletTransactions.transactionType].name,
                    amount = row[MasterWalletTransactions.amount].toDouble(),
                    currency = row[MasterWalletTransactions.currency],
                    balanceBefore = row[MasterWalletTransactions.balanceBefore].toDouble(),
                    balanceAfter = row[MasterWalletTransactions.balanceAfter].toDouble(),
                    description = row[MasterWalletTransactions.description],
                    reference = row[MasterWalletTransactions.reference],
                    counterpartyWalletId = row[MasterWalletTransactions.counterpartyWalletId]?.toString(),
                    status = row[MasterWalletTransactions.status].name,
                    processedBy = row[MasterWalletTransactions.processedBy].toString(),
                    processedAt = row[MasterWalletTransactions.processedAt].toString(),
                    createdAt = row[MasterWalletTransactions.createdAt].toString()
                )
            }
    }

    // ==================== ALLOCATION API METHODS ====================

    fun getAllAllocations(page: Int = 1, pageSize: Int = 10): ApiResponse<List<WalletAllocation>> = transaction {
        try {
            val allocations = FloatAllocations.selectAll()
                .orderBy(FloatAllocations.createdAt, SortOrder.DESC)
                .limit(pageSize, ((page - 1) * pageSize).toLong())
                .map { row ->
                    WalletAllocation(
                        id = row[FloatAllocations.id].value.toString(),
                        sourceWalletId = row[FloatAllocations.sourceWalletId].toString(),
                        targetWalletId = row[FloatAllocations.targetWalletId].toString(),
                        amount = row[FloatAllocations.amount].toDouble(),
                        purpose = row[FloatAllocations.purpose],
                        status = row[FloatAllocations.status].name,
                        requestedBy = row[FloatAllocations.requestedBy].toString(),
                        allocatedBy = row[FloatAllocations.allocatedBy].toString(),
                        expiryDate = row[FloatAllocations.expiryDate]?.toString(),
                        createdAt = row[FloatAllocations.createdAt].toString(),
                        updatedAt = row[FloatAllocations.updatedAt].toString()
                    )
                }

            ApiResponse(
                success = true,
                message = "Allocations retrieved successfully",
                data = allocations
            )
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Failed to retrieve allocations",
                error = e.message
            )
        }
    }

    fun createAllocation(request: CreateWalletAllocationRequest): ApiResponse<WalletAllocation> = transaction {
        try {
            val allocatedBy = UUID.fromString(request.allocatedBy)

            val allocationId = FloatAllocations.insertAndGetId {
                it[FloatAllocations.sourceWalletId] = UUID.fromString(request.walletId)
                it[FloatAllocations.targetWalletId] = UUID.fromString(request.targetWalletId)
                it[FloatAllocations.allocationType] = AllocationStatus.ACTIVE
                it[FloatAllocations.amount] = BigDecimal(request.amount)
                it[FloatAllocations.purpose] = request.purpose
                it[FloatAllocations.allocatedBy] = allocatedBy
                it[FloatAllocations.requestedBy] = allocatedBy
                it[FloatAllocations.remainingAmount] = BigDecimal(request.amount)
                it[FloatAllocations.status] = AllocationStatus.ACTIVE
            }

            // Update wallet reserved amount
            val currentWallet = MasterWallets.select { MasterWallets.id eq UUID.fromString(request.walletId) }.single()
            val currentAvailable = currentWallet[MasterWallets.availableBalance]
            val currentReserved = currentWallet[MasterWallets.reserveBalance]

            MasterWallets.update({ MasterWallets.id eq UUID.fromString(request.walletId) }) {
                it[MasterWallets.availableBalance] = currentAvailable.minus(BigDecimal(request.amount))
                it[MasterWallets.reserveBalance] = currentReserved.plus(BigDecimal(request.amount))
            }

            val createdAllocation = WalletAllocation(
                id = allocationId.value.toString(),
                sourceWalletId = request.walletId,
                targetWalletId = request.targetWalletId,
                amount = request.amount,
                purpose = request.purpose,
                status = "ACTIVE",
                requestedBy = allocatedBy.toString(),
                allocatedBy = allocatedBy.toString(),
                expiryDate = null,
                createdAt = LocalDateTime.now().toString(),
                updatedAt = LocalDateTime.now().toString()
            )

            ApiResponse(
                success = true,
                message = "Allocation created successfully",
                data = createdAllocation
            )
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Failed to create allocation",
                error = e.message
            )
        }
    }

    // ==================== RECONCILIATION API METHODS ====================

    fun getAllReconciliations(page: Int = 1, pageSize: Int = 10): ApiResponse<List<ReconciliationRecord>> =
        transaction {
            try {
                val reconciliations = ReconciliationRecords.selectAll()
                    .orderBy(ReconciliationRecords.createdAt, SortOrder.DESC)
                    .limit(pageSize, ((page - 1) * pageSize).toLong())
                    .map { row ->
                        ReconciliationRecord(
                            id = row[ReconciliationRecords.id].value.toString(),
                            walletId = row[ReconciliationRecords.walletId].toString(),
                            reconciliationType = row[ReconciliationRecords.reconciliationType] ?: "DAILY",
                            expectedBalance = row[ReconciliationRecords.expectedBalance].toDouble(),
                            actualBalance = row[ReconciliationRecords.actualBalance].toDouble(),
                            difference = row[ReconciliationRecords.difference].toDouble(),
                            status = row[ReconciliationRecords.status],
                            performedBy = row[ReconciliationRecords.performedBy].toString(),
                            createdAt = row[ReconciliationRecords.createdAt].toString(),
                            updatedAt = row[ReconciliationRecords.updatedAt].toString()
                        )
                    }

                ApiResponse(
                    success = true,
                    message = "Reconciliations retrieved successfully",
                    data = reconciliations
                )
            } catch (e: Exception) {
                ApiResponse(
                    success = false,
                    message = "Failed to retrieve reconciliations",
                    error = e.message
                )
            }
        }

    fun createReconciliation(request: CreateWalletReconciliationRequest): ApiResponse<ReconciliationRecord> =
        transaction {
            try {
                val performedBy = UUID.fromString(request.performedBy)

                val reconciliation = performReconciliation(request.walletId, performedBy)

                ApiResponse(
                    success = true,
                    message = "Reconciliation created successfully",
                    data = reconciliation
                )
            } catch (e: Exception) {
                ApiResponse(
                    success = false,
                    message = "Failed to create reconciliation",
                    error = e.message
                )
            }
        }

    fun performReconciliation(walletId: String, performedBy: UUID): ReconciliationRecord = transaction {
        val wallet = MasterWallets.select { MasterWallets.id eq UUID.fromString(walletId) }
            .single()

        val currentBalance = wallet[MasterWallets.balance]
        val expectedBalance = currentBalance

        val reconciliationId = ReconciliationRecords.insertAndGetId {
            it[ReconciliationRecords.walletId] = UUID.fromString(walletId)
            it[ReconciliationRecords.performedBy] = performedBy
            it[ReconciliationRecords.expectedBalance] = expectedBalance
            it[ReconciliationRecords.actualBalance] = currentBalance
            it[ReconciliationRecords.difference] = BigDecimal.ZERO
            it[ReconciliationRecords.status] = "SUCCESSFUL"
            it[ReconciliationRecords.periodStart] = Instant.now().minusSeconds(86400) // 24 hours ago
            it[ReconciliationRecords.periodEnd] = Instant.now()
            it[ReconciliationRecords.transactionCount] = 0
            it[ReconciliationRecords.totalDebits] = BigDecimal.ZERO
            it[ReconciliationRecords.totalCredits] = BigDecimal.ZERO
        }

        ReconciliationRecord(
            id = reconciliationId.value.toString(),
            walletId = walletId,
            reconciliationType = "DAILY",
            expectedBalance = expectedBalance.toDouble(),
            actualBalance = currentBalance.toDouble(),
            difference = 0.0,
            status = "SUCCESSFUL",
            performedBy = performedBy.toString(),
            createdAt = LocalDateTime.now().toString(),
            updatedAt = LocalDateTime.now().toString()
        )
    }

    // ==================== SECURITY ALERT API METHODS ====================

    fun getAllSecurityAlerts(page: Int = 1, pageSize: Int = 10): ApiResponse<List<String>> =
        transaction {
            try {
                val alerts = WalletSecurityAlerts.selectAll()
                    .orderBy(WalletSecurityAlerts.createdAt, SortOrder.DESC)
                    .limit(pageSize, ((page - 1) * pageSize).toLong())
                    .map { row ->
                        // Convert WalletSecurityAlertDto to simple string message
                        val severity = row[WalletSecurityAlerts.severity].name
                        val title = row[WalletSecurityAlerts.title]
                        val status = if (row[WalletSecurityAlerts.isResolved]) "RESOLVED" else "ACTIVE"
                        "[$severity] $title - Status: $status"
                    }

                ApiResponse(
                    success = true,
                    message = "Security alerts retrieved successfully",
                    data = alerts
                )
            } catch (e: Exception) {
                ApiResponse(
                    success = false,
                    message = "Failed to retrieve security alerts",
                    error = e.message
                )
            }
        }

    fun resolveSecurityAlert(alertId: String, resolution: String): ApiResponse<String> = transaction {
        try {
            val updated = WalletSecurityAlerts.update({ WalletSecurityAlerts.id eq UUID.fromString(alertId) }) {
                it[WalletSecurityAlerts.isResolved] = true
                it[WalletSecurityAlerts.resolutionNotes] = resolution
            }

            if (updated > 0) {
                ApiResponse(
                    success = true,
                    message = "Security alert resolved successfully"
                )
            } else {
                ApiResponse(
                    success = false,
                    message = "Security alert not found"
                )
            }
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Failed to resolve security alert",
                error = e.message
            )
        }
    }

    // ==================== MPESA INTEGRATION METHODS ====================

    /**
     * Process incoming funds from M-Pesa transactions
     */
    suspend fun processIncomingFunds(
        amount: Double,
        source: String,
        reference: String,
        accountNumber: String,
        description: String
    ) {
        try {
            // Extract wallet ID from account number (format: WALLET_<uuid>)
            val targetWalletId = if (accountNumber.startsWith("WALLET_")) {
                try {
                    UUID.fromString(accountNumber.removePrefix("WALLET_"))
                } catch (e: Exception) {
                    println("⚠️ Invalid wallet ID in account number: $accountNumber, using CUSTOMER_FLOAT")
                    null
                }
            } else {
                println("⚠️ Account number doesn't contain wallet ID: $accountNumber, using CUSTOMER_FLOAT")
                null
            }

            // Find the target wallet or fall back to customer float wallet
            val targetWallet = if (targetWalletId != null) {
                transaction {
                    MasterWallets.select {
                        MasterWallets.id eq targetWalletId
                    }.firstOrNull()
                }
            } else {
                transaction {
                    MasterWallets.select {
                        MasterWallets.walletType eq MasterWalletType.CUSTOMER_FLOAT
                    }.firstOrNull()
                }
            }

            if (targetWallet != null) {
                val walletId = targetWallet[MasterWallets.id].value
                val currentBalance = targetWallet[MasterWallets.balance]
                val newBalance = currentBalance + java.math.BigDecimal.valueOf(amount)

                // Create transaction record
                transaction {
                    MasterWalletTransactions.insert {
                        it[MasterWalletTransactions.walletId] = walletId
                        it[MasterWalletTransactions.transactionType] = MasterWalletTransactionType.CUSTOMER_PAYOUT
                        it[MasterWalletTransactions.amount] = java.math.BigDecimal.valueOf(amount)
                        it[MasterWalletTransactions.balanceBefore] = currentBalance
                        it[MasterWalletTransactions.balanceAfter] = newBalance
                        it[MasterWalletTransactions.description] = "M-Pesa Settlement: $description"
                        it[MasterWalletTransactions.reference] = reference
                        it[MasterWalletTransactions.externalAccountId] = accountNumber
                        it[MasterWalletTransactions.processedBy] =
                            UUID.fromString("00000000-0000-0000-0000-000000000000") // System user
                        it[MasterWalletTransactions.status] = TransactionStatus.COMPLETED
                        it[MasterWalletTransactions.currency] = targetWallet[MasterWallets.currency]
                        it[MasterWalletTransactions.riskScore] =
                            java.math.BigDecimal.valueOf(calculateRiskScore(amount))
                        it[MasterWalletTransactions.riskLevel] = determineRiskLevel(amount)
                    }

                    // Update wallet balance
                    MasterWallets.update({ MasterWallets.id eq walletId }) {
                        it[MasterWallets.balance] = newBalance
                        it[MasterWallets.availableBalance] = newBalance
                    }
                }

                println("✅ Processed M-Pesa incoming funds: KES $amount to wallet $walletId (target: ${targetWalletId ?: "CUSTOMER_FLOAT"})")
            } else {
                println("⚠️ No target wallet found for account: $accountNumber")
            }
        } catch (e: Exception) {
            println("❌ Failed to process incoming M-Pesa funds: ${e.message}")
            e.printStackTrace()
        }
    }

    // ==================== INITIALIZATION ====================

    fun initializeMasterWallets(adminUserId: UUID) = transaction {
        // Check if wallets already exist
        val existingWallets = MasterWallets.selectAll().count()
        if (existingWallets > 0) {
            println("✅ Master wallets already exist: $existingWallets wallets found")
            return@transaction
        }

        println("🔐 Creating initial master wallets...")

        // Create Main Vault
        val mainVaultId = MasterWallets.insertAndGetId {
            it[MasterWallets.walletName] = "Main Bank Vault"
            it[MasterWallets.walletType] = MasterWalletType.MAIN_VAULT
            it[MasterWallets.balance] = BigDecimal("50000000.00")
            it[MasterWallets.availableBalance] = BigDecimal("35000000.00")
            it[MasterWallets.reserveBalance] = BigDecimal("10000000.00")
            it[MasterWallets.securityLevel] = WalletSecurityLevel.MAXIMUM
            it[MasterWallets.dailyTransactionLimit] = BigDecimal("10000000.00")
            it[MasterWallets.monthlyTransactionLimit] = BigDecimal("100000000.00")
            it[MasterWallets.createdBy] = adminUserId
            it[MasterWallets.managedBy] = adminUserId
            it[MasterWallets.authorizedUsers] = "[\"$adminUserId\"]"
            it[MasterWallets.encryptionKey] = generateEncryptionKey()
            it[MasterWallets.currency] = "USD"
        }

        // Create Branch Allocation Wallet
        val branchWalletId = MasterWallets.insertAndGetId {
            it[MasterWallets.walletName] = "Branch Float Allocation"
            it[MasterWallets.walletType] = MasterWalletType.BRANCH_ALLOCATION
            it[MasterWallets.balance] = BigDecimal("15000000.00")
            it[MasterWallets.availableBalance] = BigDecimal("12000000.00")
            it[MasterWallets.reserveBalance] = BigDecimal("2000000.00")
            it[MasterWallets.securityLevel] = WalletSecurityLevel.HIGH
            it[MasterWallets.dailyTransactionLimit] = BigDecimal("5000000.00")
            it[MasterWallets.monthlyTransactionLimit] = BigDecimal("50000000.00")
            it[MasterWallets.createdBy] = adminUserId
            it[MasterWallets.managedBy] = adminUserId
            it[MasterWallets.authorizedUsers] = "[\"$adminUserId\"]"
            it[MasterWallets.encryptionKey] = generateEncryptionKey()
            it[MasterWallets.currency] = "USD"
        }

        // Create Customer Float Wallet
        val customerWalletId = MasterWallets.insertAndGetId {
            it[MasterWallets.walletName] = "Customer Operations Float"
            it[MasterWallets.walletType] = MasterWalletType.CUSTOMER_FLOAT
            it[MasterWallets.balance] = BigDecimal("25000000.00")
            it[MasterWallets.availableBalance] = BigDecimal("20000000.00")
            it[MasterWallets.reserveBalance] = BigDecimal("3000000.00")
            it[MasterWallets.securityLevel] = WalletSecurityLevel.HIGH
            it[MasterWallets.dailyTransactionLimit] = BigDecimal("8000000.00")
            it[MasterWallets.monthlyTransactionLimit] = BigDecimal("80000000.00")
            it[MasterWallets.createdBy] = adminUserId
            it[MasterWallets.managedBy] = adminUserId
            it[MasterWallets.authorizedUsers] = "[\"$adminUserId\"]"
            it[MasterWallets.encryptionKey] = generateEncryptionKey()
            it[MasterWallets.currency] = "USD"
        }

        // Create Loan Disbursement Wallet
        val loanWalletId = MasterWallets.insertAndGetId {
            it[MasterWallets.walletName] = "Loan Disbursement Vault"
            it[MasterWallets.walletType] = MasterWalletType.LOAN_DISBURSEMENT
            it[MasterWallets.balance] = BigDecimal("20000000.00")
            it[MasterWallets.availableBalance] = BigDecimal("18000000.00")
            it[MasterWallets.reserveBalance] = BigDecimal("1500000.00")
            it[MasterWallets.securityLevel] = WalletSecurityLevel.HIGH
            it[MasterWallets.dailyTransactionLimit] = BigDecimal("3000000.00")
            it[MasterWallets.monthlyTransactionLimit] = BigDecimal("40000000.00")
            it[MasterWallets.createdBy] = adminUserId
            it[MasterWallets.managedBy] = adminUserId
            it[MasterWallets.authorizedUsers] = "[\"$adminUserId\"]"
            it[MasterWallets.encryptionKey] = generateEncryptionKey()
            it[MasterWallets.currency] = "USD"
        }

        // Create sample transactions
        createSampleTransactions(
            listOf(
                mainVaultId.value,
                branchWalletId.value,
                customerWalletId.value,
                loanWalletId.value
            ), adminUserId
        )

        // Create sample allocations
        createSampleAllocations(branchWalletId.value, adminUserId)

        // Create security alert for system initialization
        WalletSecurityAlerts.insert {
            it[WalletSecurityAlerts.alertType] = SecurityAlertType.SYSTEM_BREACH
            it[WalletSecurityAlerts.severity] = AlertSeverity.LOW
            it[WalletSecurityAlerts.title] = "Master wallet system initialized"
            it[WalletSecurityAlerts.description] = "Master wallet system initialized and secured"
            it[WalletSecurityAlerts.isResolved] = true
            it[WalletSecurityAlerts.resolvedBy] = adminUserId
        }

        println("✅ Master wallet system initialized with ${MasterWallets.selectAll().count()} wallets")
    }

    private fun createSampleTransactions(walletIds: List<UUID>, userId: UUID) {
        // Customer deposits
        MasterWalletTransactions.insert {
            it[MasterWalletTransactions.walletId] = walletIds[2] // Customer float wallet
            it[MasterWalletTransactions.transactionType] = MasterWalletTransactionType.CUSTOMER_PAYOUT
            it[MasterWalletTransactions.amount] = BigDecimal("250000.00")
            it[MasterWalletTransactions.balanceBefore] = BigDecimal("25000000.00")
            it[MasterWalletTransactions.balanceAfter] = BigDecimal("25250000.00")
            it[MasterWalletTransactions.description] = "Daily customer deposits batch"
            it[MasterWalletTransactions.processedBy] = userId
            it[MasterWalletTransactions.status] = TransactionStatus.COMPLETED
            it[MasterWalletTransactions.reference] = IdGenerator.generateTransactionId()
            it[MasterWalletTransactions.riskScore] = BigDecimal("20.0")
            it[MasterWalletTransactions.riskLevel] = "LOW"
            it[MasterWalletTransactions.currency] = "USD"
        }

        // Branch allocation
        MasterWalletTransactions.insert {
            it[MasterWalletTransactions.walletId] = walletIds[1] // Branch allocation wallet
            it[MasterWalletTransactions.transactionType] = MasterWalletTransactionType.BRANCH_DISBURSEMENT
            it[MasterWalletTransactions.amount] = BigDecimal("500000.00")
            it[MasterWalletTransactions.balanceBefore] = BigDecimal("15000000.00")
            it[MasterWalletTransactions.balanceAfter] = BigDecimal("14500000.00")
            it[MasterWalletTransactions.description] = "Weekly branch float allocation - Downtown Branch"
            it[MasterWalletTransactions.processedBy] = userId
            it[MasterWalletTransactions.status] = TransactionStatus.COMPLETED
            it[MasterWalletTransactions.reference] = IdGenerator.generateTransactionId()
            it[MasterWalletTransactions.riskScore] = BigDecimal("40.0")
            it[MasterWalletTransactions.riskLevel] = "MEDIUM"
            it[MasterWalletTransactions.currency] = "USD"
        }
    }

    private fun createSampleAllocations(branchWalletId: UUID, userId: UUID) {
        // Get a sample branch ID
        val sampleBranchId = Branches.selectAll().limit(1).firstOrNull()?.get(Branches.id)?.value
            ?: UUID.randomUUID()

        FloatAllocations.insert {
            it[FloatAllocations.sourceWalletId] = branchWalletId
            it[FloatAllocations.targetWalletId] = sampleBranchId
            it[FloatAllocations.allocationType] = AllocationStatus.ACTIVE
            it[FloatAllocations.amount] = BigDecimal("2000000.00")
            it[FloatAllocations.actualUsage] = BigDecimal("1500000.00")
            it[FloatAllocations.remainingAmount] = BigDecimal("500000.00")
            it[FloatAllocations.purpose] = "Daily operations and customer transactions"
            it[FloatAllocations.status] = AllocationStatus.ACTIVE
            it[FloatAllocations.allocatedBy] = userId
            it[FloatAllocations.requestedBy] = userId
        }
    }

    // ==================== HELPER METHODS ====================

    private fun createAuditEntry(
        walletId: UUID,
        action: String,
        performedBy: UUID,
        details: String
    ) {
        WalletAuditTrail.insert {
            it[WalletAuditTrail.walletId] = walletId
            it[WalletAuditTrail.userId] = performedBy
            it[WalletAuditTrail.action] = action
            it[WalletAuditTrail.entityType] = "WALLET"
            it[WalletAuditTrail.entityId] = walletId.toString()
            it[WalletAuditTrail.changeDescription] = details
            it[WalletAuditTrail.ipAddress] = "127.0.0.1"
            it[WalletAuditTrail.userAgent] = "AxionBank Server"
            it[WalletAuditTrail.riskLevel] = "LOW"
        }
    }

    private fun calculateRiskScore(amount: Double): Double {
        return when {
            kotlin.math.abs(amount) > 1_000_000 -> 80.0
            kotlin.math.abs(amount) > 500_000 -> 60.0
            kotlin.math.abs(amount) > 100_000 -> 40.0
            else -> 20.0
        }
    }

    private fun determineRiskLevel(amount: Double): String {
        return when {
            kotlin.math.abs(amount) > 5_000_000 -> "CRITICAL"
            kotlin.math.abs(amount) > 1_000_000 -> "HIGH"
            kotlin.math.abs(amount) > 100_000 -> "MEDIUM"
            else -> "LOW"
        }
    }

    private fun determineAuthorizationLevel(amount: Double): String {
        return when {
            kotlin.math.abs(amount) > 10_000_000 -> "BOARD_APPROVAL"
            kotlin.math.abs(amount) > 1_000_000 -> "DUAL_APPROVAL"
            kotlin.math.abs(amount) > 100_000 -> "MANAGER_REQUIRED"
            else -> "AUTOMATIC"
        }
    }

    private fun generateEncryptionKey(): String {
        return "AES256_${UUID.randomUUID().toString().replace("-", "").uppercase()}"
    }

    private fun calculateUtilization(allocated: BigDecimal, used: BigDecimal): Double {
        return if (allocated > BigDecimal.ZERO) {
            (used.toDouble() / allocated.toDouble()) * 100.0
        } else 0.0
    }
}

// ==================== DTO MODELS ====================

@Serializable
data class WalletDto(
    val id: String,
    val walletName: String,
    val walletType: String,
    val currency: String,
    val balance: Double,
    val availableBalance: Double,
    val reserveBalance: Double,
    val securityLevel: String,
    val status: String,
    val maxSingleTransaction: Double,
    val dailyTransactionLimit: Double,
    val monthlyTransactionLimit: Double,
    val createdBy: String,
    val managedBy: String,
    val branchId: String?,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class MasterWalletDto(
    val id: String,
    val walletName: String,
    val walletType: String,
    val currency: String,
    val balance: Double,
    val availableBalance: Double,
    val reserveBalance: Double,
    val securityLevel: String,
    val status: String,
    val maxSingleTransaction: Double,
    val dailyTransactionLimit: Double,
    val monthlyTransactionLimit: Double,
    val createdBy: String,
    val managedBy: String,
    val branchId: String?,
    val createdAt: String,
    val updatedAt: String,
    val totalFloatBalance: Double = balance,
    val availableFloat: Double = availableBalance,
    val reservedFloat: Double = reserveBalance,
    val pendingTransactions: Double = 0.0,
    val lastReconciliationDate: String = "Never",
    val lastUpdatedAt: String = updatedAt,
    val isActive: Boolean = status == "ACTIVE",
    val currentDailyTotal: Double = 0.0,
    val currentMonthlyTotal: Double = 0.0,
    val riskScore: Double = 5.0,
    val complianceStatus: String = "COMPLIANT"
)

@Serializable
data class WalletTransactionDto(
    val id: String,
    val walletId: String,
    val transactionType: String,
    val amount: Double,
    val currency: String,
    val balanceBefore: Double,
    val balanceAfter: Double,
    val description: String,
    val reference: String?,
    val counterpartyWalletId: String?,
    val status: String,
    val processedBy: String,
    val processedAt: String,
    val createdAt: String
)

@Serializable
data class WalletAllocation(
    val id: String,
    val sourceWalletId: String,
    val targetWalletId: String,
    val amount: Double,
    val purpose: String,
    val status: String,
    val requestedBy: String,
    val allocatedBy: String,
    val expiryDate: String?,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ReconciliationRecord(
    val id: String,
    val walletId: String,
    val reconciliationType: String,
    val expectedBalance: Double,
    val actualBalance: Double,
    val difference: Double,
    val status: String,
    val performedBy: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class WalletSecurityAlertDto(
    val id: String,
    val walletId: String?,
    val alertType: String,
    val severity: String,
    val message: String,
    val detectedAt: String,
    val resolved: Boolean,
    val resolvedAt: String?,
    val resolvedBy: String?
)

// ==================== REQUEST MODELS ====================

@Serializable
data class CreateMasterWalletRequest(
    val walletName: String,
    val walletType: String,
    val initialBalance: Double,
    val securityLevel: String,
    val dailyLimit: Double,
    val monthlyLimit: Double
)

@Serializable
data class CreateWalletRequest(
    val walletName: String,
    val walletType: String,
    val initialBalance: Double,
    val securityLevel: String,
    val dailyLimit: Double,
    val monthlyLimit: Double,
    val createdBy: String
)

@Serializable
data class UpdateWalletRequest(
    val walletName: String,
    val dailyLimit: Double,
    val monthlyLimit: Double
)

@Serializable
data class CreateWalletTransactionRequest(
    val walletId: String,
    val transactionType: String,
    val amount: Double,
    val description: String,
    val processedBy: String
)

@Serializable
data class WalletTransactionRequest(
    val walletId: String,
    val transactionType: String,
    val amount: Double,
    val description: String,
    val sourceAccountId: String? = null,
    val destinationAccountId: String? = null,
    val customerAccountNumber: String? = null
)

@Serializable
data class FloatAllocationRequest(
    val walletId: String,
    val branchId: String,
    val branchName: String,
    val amount: Double,
    val purpose: String
)

@Serializable
data class CreateWalletAllocationRequest(
    val walletId: String,
    val targetWalletId: String,
    val branchName: String,
    val amount: Double,
    val purpose: String,
    val allocatedBy: String
)

@Serializable
data class CreateWalletReconciliationRequest(
    val walletId: String,
    val performedBy: String
)

@Serializable
data class WalletTransferResult(
    val success: Boolean,
    val transactionReference: String?,
    val senderBalanceBefore: Double,
    val senderBalanceAfter: Double,
    val recipientBalanceBefore: Double,
    val recipientBalanceAfter: Double
)

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val error: String? = null
)