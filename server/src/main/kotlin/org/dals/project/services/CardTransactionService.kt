package org.dals.project.services

import org.dals.project.database.*
import org.dals.project.models.*
import org.dals.project.utils.IdGenerator
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Service for handling card-based transactions including:
 * - Online shopping payments
 * - POS (Point of Sale) transactions
 * - Bill payments
 * - Money transfers
 * - ATM withdrawals
 */
class CardTransactionService {
    private val notificationService = NotificationService()
    private val feeService = TransactionFeeService()

    /**
     * Process online shopping payment
     */
    fun processOnlinePayment(request: OnlinePaymentRequest): CardTransactionResponse {
        return transaction {
            // Validate card
            val card = validateCard(request.cardId, request.cvv)

            // Get linked account
            val account = getLinkedAccount(card[Cards.customerId])

            // Check balance
            if (account[Accounts.availableBalance] < BigDecimal(request.amount)) {
                throw IllegalArgumentException("Insufficient funds")
            }

            // Calculate fees (if any)
            val fee = feeService.calculateOnlinePaymentFee(BigDecimal(request.amount))
            val totalAmount = BigDecimal(request.amount) + fee

            // Deduct from account
            val newBalance = account[Accounts.balance] - totalAmount
            val newAvailableBalance = account[Accounts.availableBalance] - totalAmount

            Accounts.update({ Accounts.id eq account[Accounts.id] }) {
                it[balance] = newBalance
                it[availableBalance] = newAvailableBalance
                it[lastTransactionDate] = Instant.now()
                it[updatedAt] = Instant.now()
            }

            // Create transaction record
            val transactionId = UUID.randomUUID()
            val reference = "ONL-${IdGenerator.generateTransactionId()}"

            Transactions.insert {
                it[id] = transactionId
                it[accountId] = account[Accounts.id].value
                it[type] = TransactionType.PAYMENT
                it[amount] = BigDecimal(request.amount)
                it[status] = TransactionStatus.COMPLETED
                it[description] = "Online payment to ${request.merchantName}"
                it[balanceAfter] = newBalance
                it[Transactions.reference] = reference
                it[merchantName] = request.merchantName
                it[category] = request.category ?: "ONLINE_SHOPPING"
                it[Transactions.timestamp] = Instant.now()
                it[createdAt] = Instant.now()
            }

            // Update card last used
            Cards.update({ Cards.id eq UUID.fromString(request.cardId) }) {
                it[lastUsedDate] = Instant.now()
                it[updatedAt] = Instant.now()
            }

            // Record fee if applicable
            if (fee > BigDecimal.ZERO) {
                recordTransactionFee(transactionId, account[Accounts.customerId], account[Accounts.id].value,
                    "ONLINE_PAYMENT", BigDecimal(request.amount), fee)
            }

            // Send notification
            notificationService.notifyTransactionCompleted(
                customerId = account[Accounts.customerId],
                transactionId = transactionId,
                amount = request.amount.toString(),
                type = "ONLINE_PAYMENT"
            )

            CardTransactionResponse(
                success = true,
                message = "Payment processed successfully",
                transactionId = transactionId.toString(),
                reference = reference,
                amount = request.amount,
                fee = fee.toDouble(),
                newBalance = newBalance.toDouble(),
                timestamp = Instant.now().toString()
            )
        }
    }

    /**
     * Process POS (Point of Sale) transaction
     */
    fun processPOSTransaction(request: POSTransactionRequest): CardTransactionResponse {
        return transaction {
            // Validate card
            val card = validateCard(request.cardId, request.pin ?: "")

            // Get linked account
            val account = getLinkedAccount(card[Cards.customerId])

            // Check balance
            if (account[Accounts.availableBalance] < BigDecimal(request.amount)) {
                throw IllegalArgumentException("Insufficient funds")
            }

            // Calculate fees
            val fee = feeService.calculatePOSTransactionFee(BigDecimal(request.amount))
            val totalAmount = BigDecimal(request.amount) + fee

            // Deduct from account
            val newBalance = account[Accounts.balance] - totalAmount
            val newAvailableBalance = account[Accounts.availableBalance] - totalAmount

            Accounts.update({ Accounts.id eq account[Accounts.id] }) {
                it[balance] = newBalance
                it[availableBalance] = newAvailableBalance
                it[lastTransactionDate] = Instant.now()
                it[updatedAt] = Instant.now()
            }

            // Create transaction record
            val transactionId = UUID.randomUUID()
            val reference = "POS-${IdGenerator.generateTransactionId()}"

            Transactions.insert {
                it[id] = transactionId
                it[accountId] = account[Accounts.id].value
                it[type] = TransactionType.PAYMENT
                it[amount] = BigDecimal(request.amount)
                it[status] = TransactionStatus.COMPLETED
                it[description] = "POS transaction at ${request.merchantName}"
                it[balanceAfter] = newBalance
                it[Transactions.reference] = reference
                it[merchantName] = request.merchantName
                it[category] = "POS_PURCHASE"
                it[Transactions.timestamp] = Instant.now()
                it[createdAt] = Instant.now()
            }

            // Update card last used
            Cards.update({ Cards.id eq UUID.fromString(request.cardId) }) {
                it[lastUsedDate] = Instant.now()
                it[updatedAt] = Instant.now()
            }

            // Record fee
            if (fee > BigDecimal.ZERO) {
                recordTransactionFee(transactionId, account[Accounts.customerId], account[Accounts.id].value,
                    "POS_TRANSACTION", BigDecimal(request.amount), fee)
            }

            // Send notification
            notificationService.notifyTransactionCompleted(
                customerId = account[Accounts.customerId],
                transactionId = transactionId,
                amount = request.amount.toString(),
                type = "POS_TRANSACTION"
            )

            CardTransactionResponse(
                success = true,
                message = "POS transaction completed successfully",
                transactionId = transactionId.toString(),
                reference = reference,
                amount = request.amount,
                fee = fee.toDouble(),
                newBalance = newBalance.toDouble(),
                timestamp = Instant.now().toString()
            )
        }
    }

    /**
     * Process bill payment
     */
    fun processBillPayment(request: BillPaymentRequest): CardTransactionResponse {
        return transaction {
            // Validate card
            val card = validateCard(request.cardId, request.cvv)

            // Get linked account
            val account = getLinkedAccount(card[Cards.customerId])

            // Check balance
            if (account[Accounts.availableBalance] < BigDecimal(request.amount)) {
                throw IllegalArgumentException("Insufficient funds")
            }

            // Calculate fees
            val fee = feeService.calculateBillPaymentFee(BigDecimal(request.amount))
            val totalAmount = BigDecimal(request.amount) + fee

            // Deduct from account
            val newBalance = account[Accounts.balance] - totalAmount
            val newAvailableBalance = account[Accounts.availableBalance] - totalAmount

            Accounts.update({ Accounts.id eq account[Accounts.id] }) {
                it[balance] = newBalance
                it[availableBalance] = newAvailableBalance
                it[lastTransactionDate] = Instant.now()
                it[updatedAt] = Instant.now()
            }

            // Create transaction record
            val transactionId = UUID.randomUUID()
            val reference = "BILL-${IdGenerator.generateTransactionId()}"

            Transactions.insert {
                it[id] = transactionId
                it[accountId] = account[Accounts.id].value
                it[type] = TransactionType.PAYMENT
                it[amount] = BigDecimal(request.amount)
                it[status] = TransactionStatus.COMPLETED
                it[description] = "Bill payment: ${request.billType} - ${request.billerName} (${request.accountNumber})"
                it[balanceAfter] = newBalance
                it[Transactions.reference] = reference
                it[merchantName] = request.billerName
                it[category] = "BILL_PAYMENT"
                it[Transactions.timestamp] = Instant.now()
                it[createdAt] = Instant.now()
            }

            // Update card last used
            Cards.update({ Cards.id eq UUID.fromString(request.cardId) }) {
                it[lastUsedDate] = Instant.now()
                it[updatedAt] = Instant.now()
            }

            // Record fee
            if (fee > BigDecimal.ZERO) {
                recordTransactionFee(transactionId, account[Accounts.customerId], account[Accounts.id].value,
                    "BILL_PAYMENT", BigDecimal(request.amount), fee)
            }

            // Send notification
            notificationService.notifyTransactionCompleted(
                customerId = account[Accounts.customerId],
                transactionId = transactionId,
                amount = request.amount.toString(),
                type = "BILL_PAYMENT"
            )

            CardTransactionResponse(
                success = true,
                message = "Bill payment completed successfully",
                transactionId = transactionId.toString(),
                reference = reference,
                amount = request.amount,
                fee = fee.toDouble(),
                newBalance = newBalance.toDouble(),
                timestamp = Instant.now().toString()
            )
        }
    }

    /**
     * Process card-to-card or card-to-account transfer
     */
    fun processCardTransfer(request: CardTransferRequest): CardTransactionResponse {
        return transaction {
            // Validate card
            val card = validateCard(request.cardId, request.cvv)

            // Get source account
            val sourceAccount = getLinkedAccount(card[Cards.customerId])

            // Check balance
            if (sourceAccount[Accounts.availableBalance] < BigDecimal(request.amount)) {
                throw IllegalArgumentException("Insufficient funds")
            }

            // Get destination account
            val destAccount = Accounts.select {
                Accounts.accountNumber eq request.destinationAccountNumber
            }.singleOrNull() ?: throw IllegalArgumentException("Destination account not found")

            // Calculate fees
            val fee = feeService.calculateTransferFee(BigDecimal(request.amount))
            val totalAmount = BigDecimal(request.amount) + fee

            // Deduct from source
            val newSourceBalance = sourceAccount[Accounts.balance] - totalAmount
            val newSourceAvailableBalance = sourceAccount[Accounts.availableBalance] - totalAmount

            Accounts.update({ Accounts.id eq sourceAccount[Accounts.id] }) {
                it[balance] = newSourceBalance
                it[availableBalance] = newSourceAvailableBalance
                it[lastTransactionDate] = Instant.now()
                it[updatedAt] = Instant.now()
            }

            // Add to destination
            val newDestBalance = destAccount[Accounts.balance] + BigDecimal(request.amount)
            val newDestAvailableBalance = destAccount[Accounts.availableBalance] + BigDecimal(request.amount)

            Accounts.update({ Accounts.id eq destAccount[Accounts.id] }) {
                it[balance] = newDestBalance
                it[availableBalance] = newDestAvailableBalance
                it[lastTransactionDate] = Instant.now()
                it[updatedAt] = Instant.now()
            }

            // Create transaction records
            val transactionId = UUID.randomUUID()
            val reference = "TRF-${IdGenerator.generateTransactionId()}"

            // Debit transaction
            Transactions.insert {
                it[id] = transactionId
                it[accountId] = sourceAccount[Accounts.id].value
                it[type] = TransactionType.TRANSFER
                it[amount] = BigDecimal(request.amount)
                it[status] = TransactionStatus.COMPLETED
                it[description] = "Transfer to ${request.destinationAccountNumber}: ${request.description}"
                it[balanceAfter] = newSourceBalance
                it[fromAccountId] = sourceAccount[Accounts.id].value
                it[toAccountId] = destAccount[Accounts.id].value
                it[Transactions.reference] = reference
                it[category] = "CARD_TRANSFER"
                it[Transactions.timestamp] = Instant.now()
                it[createdAt] = Instant.now()
            }

            // Credit transaction
            Transactions.insert {
                it[id] = UUID.randomUUID()
                it[accountId] = destAccount[Accounts.id].value
                it[type] = TransactionType.TRANSFER
                it[amount] = BigDecimal(request.amount)
                it[status] = TransactionStatus.COMPLETED
                it[description] = "Transfer from ${sourceAccount[Accounts.accountNumber]}: ${request.description}"
                it[balanceAfter] = newDestBalance
                it[fromAccountId] = sourceAccount[Accounts.id].value
                it[toAccountId] = destAccount[Accounts.id].value
                it[Transactions.reference] = reference
                it[category] = "CARD_TRANSFER"
                it[Transactions.timestamp] = Instant.now()
                it[createdAt] = Instant.now()
            }

            // Update card last used
            Cards.update({ Cards.id eq UUID.fromString(request.cardId) }) {
                it[lastUsedDate] = Instant.now()
                it[updatedAt] = Instant.now()
            }

            // Record fee
            if (fee > BigDecimal.ZERO) {
                recordTransactionFee(transactionId, sourceAccount[Accounts.customerId], sourceAccount[Accounts.id].value,
                    "CARD_TRANSFER", BigDecimal(request.amount), fee)
            }

            // Send notifications
            notificationService.notifyTransactionCompleted(
                customerId = sourceAccount[Accounts.customerId],
                transactionId = transactionId,
                amount = request.amount.toString(),
                type = "CARD_TRANSFER"
            )

            notificationService.notifyTransactionCompleted(
                customerId = destAccount[Accounts.customerId],
                transactionId = transactionId,
                amount = request.amount.toString(),
                type = "TRANSFER_RECEIVED"
            )

            CardTransactionResponse(
                success = true,
                message = "Transfer completed successfully",
                transactionId = transactionId.toString(),
                reference = reference,
                amount = request.amount,
                fee = fee.toDouble(),
                newBalance = newSourceBalance.toDouble(),
                timestamp = Instant.now().toString()
            )
        }
    }

    /**
     * Process ATM withdrawal
     */
    fun processATMWithdrawal(request: ATMWithdrawalRequest): CardTransactionResponse {
        return transaction {
            // Validate card and PIN
            val card = validateCard(request.cardId, request.pin, requirePin = true)

            // Get linked account
            val account = getLinkedAccount(card[Cards.customerId])

            // Check balance
            if (account[Accounts.availableBalance] < BigDecimal(request.amount)) {
                throw IllegalArgumentException("Insufficient funds")
            }

            // Check daily ATM limit (if configured)
            val accountType = account[Accounts.type]
            // TODO: Implement daily limit checking

            // Calculate fees
            val fee = feeService.calculateATMWithdrawalFee(BigDecimal(request.amount))
            val totalAmount = BigDecimal(request.amount) + fee

            // Deduct from account
            val newBalance = account[Accounts.balance] - totalAmount
            val newAvailableBalance = account[Accounts.availableBalance] - totalAmount

            Accounts.update({ Accounts.id eq account[Accounts.id] }) {
                it[balance] = newBalance
                it[availableBalance] = newAvailableBalance
                it[lastTransactionDate] = Instant.now()
                it[updatedAt] = Instant.now()
            }

            // Create transaction record
            val transactionId = UUID.randomUUID()
            val reference = "ATM-${IdGenerator.generateTransactionId()}"

            Transactions.insert {
                it[id] = transactionId
                it[accountId] = account[Accounts.id].value
                it[type] = TransactionType.WITHDRAWAL
                it[amount] = BigDecimal(request.amount)
                it[status] = TransactionStatus.COMPLETED
                it[description] = "ATM withdrawal at ${request.atmLocation}"
                it[balanceAfter] = newBalance
                it[Transactions.reference] = reference
                it[merchantName] = request.atmLocation
                it[category] = "ATM_WITHDRAWAL"
                it[Transactions.timestamp] = Instant.now()
                it[createdAt] = Instant.now()
            }

            // Update card last used
            Cards.update({ Cards.id eq UUID.fromString(request.cardId) }) {
                it[lastUsedDate] = Instant.now()
                it[updatedAt] = Instant.now()
            }

            // Record fee
            if (fee > BigDecimal.ZERO) {
                recordTransactionFee(transactionId, account[Accounts.customerId], account[Accounts.id].value,
                    "ATM_WITHDRAWAL", BigDecimal(request.amount), fee)
            }

            // Send notification
            notificationService.notifyTransactionCompleted(
                customerId = account[Accounts.customerId],
                transactionId = transactionId,
                amount = request.amount.toString(),
                type = "ATM_WITHDRAWAL"
            )

            CardTransactionResponse(
                success = true,
                message = "Withdrawal completed successfully",
                transactionId = transactionId.toString(),
                reference = reference,
                amount = request.amount,
                fee = fee.toDouble(),
                newBalance = newBalance.toDouble(),
                timestamp = Instant.now().toString()
            )
        }
    }

    // Helper methods

    private fun validateCard(cardId: String, cvvOrPin: String, requirePin: Boolean = false): ResultRow {
        val card = Cards.select { Cards.id eq UUID.fromString(cardId) }
            .singleOrNull()
            ?: throw IllegalArgumentException("Card not found")

        if (!card[Cards.isActive]) {
            throw IllegalArgumentException("Card is not active")
        }

        if (card[Cards.status] != CardStatus.ACTIVE) {
            throw IllegalArgumentException("Card is not verified or is blocked")
        }

        // Validate expiry date
        val now = java.time.LocalDateTime.now()
        val currentYear = now.year
        val currentMonth = now.monthValue

        if (card[Cards.expiryYear] < currentYear ||
            (card[Cards.expiryYear] == currentYear && card[Cards.expiryMonth] < currentMonth)) {
            throw IllegalArgumentException("Card has expired")
        }

        // Validate CVV/PIN if provided
        if (cvvOrPin.isNotEmpty()) {
            val cvvHash = card[Cards.cvvHash]
            if (cvvHash != null && !BCrypt.checkpw(cvvOrPin, cvvHash)) {
                throw IllegalArgumentException("Invalid CVV/PIN")
            }
        }

        return card
    }

    private fun getLinkedAccount(customerId: UUID): ResultRow {
        // Get customer's primary checking or savings account
        return Accounts.select {
            (Accounts.customerId eq customerId) and
            (Accounts.status eq AccountStatus.ACTIVE) and
            ((Accounts.type eq AccountType.CHECKING) or (Accounts.type eq AccountType.SAVINGS))
        }
            .orderBy(Accounts.type to SortOrder.ASC) // Prefer CHECKING over SAVINGS
            .firstOrNull()
            ?: throw IllegalArgumentException("No active account found for card")
    }

    private fun recordTransactionFee(
        transactionId: UUID,
        customerId: UUID,
        accountId: UUID,
        transactionType: String,
        transactionAmount: BigDecimal,
        feeAmount: BigDecimal
    ) {
        // Get profit wallet
        val profitWallet = MasterWallets.select {
            MasterWallets.walletType eq MasterWalletType.TRANSACTION_FEES_PROFIT
        }.firstOrNull() ?: return

        // Record fee
        TransactionFeeRecords.insert {
            it[TransactionFeeRecords.transactionId] = transactionId
            it[TransactionFeeRecords.customerId] = customerId
            it[TransactionFeeRecords.accountId] = accountId
            it[TransactionFeeRecords.transactionType] = transactionType
            it[TransactionFeeRecords.transactionAmount] = transactionAmount
            it[TransactionFeeRecords.feeAmount] = feeAmount
            it[profitWalletId] = profitWallet[MasterWallets.id].value
            it[status] = "COLLECTED"
            it[createdAt] = Instant.now()
        }

        // Update profit wallet balance
        val newBalance = profitWallet[MasterWallets.balance] + feeAmount
        MasterWallets.update({ MasterWallets.id eq profitWallet[MasterWallets.id] }) {
            it[balance] = newBalance
            it[updatedAt] = Instant.now()
        }
    }
}
