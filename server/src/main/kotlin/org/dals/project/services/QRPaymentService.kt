package org.dals.project.services

import org.dals.project.database.DatabaseFactory
import org.dals.project.database.Accounts
import org.dals.project.database.Customers
import org.dals.project.database.Transactions
import org.dals.project.database.TransactionType
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import java.math.BigDecimal
import java.util.*
import kotlinx.serialization.Serializable

@Serializable
data class QRCodeData(
    val accountNumber: String,
    val accountName: String,
    val bankName: String = "Axio Bank",
    val customerId: String,
    val timestamp: String
)

@Serializable
data class QRPaymentRequest(
    val fromCustomerId: String,
    val qrData: String, // JSON string of QRCodeData
    val amount: String,
    val description: String = "QR Code Payment"
)

@Serializable
data class QRPaymentResponse(
    val success: Boolean,
    val message: String,
    val transactionId: String? = null,
    val recipientName: String? = null,
    val amount: String? = null
)

object QRPaymentService {

    /**
     * Generate QR code data for a customer's account
     */
    suspend fun generateQRCodeData(customerId: UUID): QRCodeData? = DatabaseFactory.dbQuery {
        // Get customer and their primary account
        val customer = Customers
            .select { Customers.id eq customerId }
            .singleOrNull() ?: return@dbQuery null

        val account = Accounts
            .select { Accounts.customerId eq customerId }
            .orderBy(Accounts.createdAt to SortOrder.ASC)
            .firstOrNull() ?: return@dbQuery null

        val accountNumber = account[Accounts.accountNumber]
        val accountName = "${customer[Customers.firstName]} ${customer[Customers.lastName]}"

        QRCodeData(
            accountNumber = accountNumber,
            accountName = accountName,
            bankName = "Axio Bank",
            customerId = customerId.toString(),
            timestamp = System.currentTimeMillis().toString()
        )
    }

    /**
     * Process a QR code payment
     */
    suspend fun processQRPayment(request: QRPaymentRequest): QRPaymentResponse = DatabaseFactory.dbQuery {
        try {
            val amount = BigDecimal(request.amount)

            if (amount <= BigDecimal.ZERO) {
                return@dbQuery QRPaymentResponse(
                    success = false,
                    message = "Invalid amount"
                )
            }

            // Parse QR code data (simplified - in production use proper JSON parsing)
            val recipientCustomerId = try {
                UUID.fromString(
                    request.qrData.substringAfter("\"customerId\":\"").substringBefore("\"")
                )
            } catch (e: Exception) {
                return@dbQuery QRPaymentResponse(
                    success = false,
                    message = "Invalid QR code data"
                )
            }

            val fromCustomerId = UUID.fromString(request.fromCustomerId)

            // Get sender's account
            val fromAccount = Accounts
                .select { Accounts.customerId eq fromCustomerId }
                .orderBy(Accounts.createdAt to SortOrder.ASC)
                .firstOrNull() ?: return@dbQuery QRPaymentResponse(
                    success = false,
                    message = "Sender account not found"
                )

            // Get recipient's account
            val toAccount = Accounts
                .select { Accounts.customerId eq recipientCustomerId }
                .orderBy(Accounts.createdAt to SortOrder.ASC)
                .firstOrNull() ?: return@dbQuery QRPaymentResponse(
                    success = false,
                    message = "Recipient account not found"
                )

            // Get recipient customer for name
            val recipientCustomer = Customers
                .select { Customers.id eq recipientCustomerId }
                .singleOrNull() ?: return@dbQuery QRPaymentResponse(
                    success = false,
                    message = "Recipient not found"
                )

            val recipientName = "${recipientCustomer[Customers.firstName]} ${recipientCustomer[Customers.lastName]}"

            // Check if sender and recipient are the same
            if (fromCustomerId == recipientCustomerId) {
                return@dbQuery QRPaymentResponse(
                    success = false,
                    message = "Cannot send money to yourself"
                )
            }

            // Check sender's balance
            val currentBalance = fromAccount[Accounts.balance]
            if (currentBalance < amount) {
                return@dbQuery QRPaymentResponse(
                    success = false,
                    message = "Insufficient funds. Available balance: $$currentBalance"
                )
            }

            // Calculate new balances
            val newSenderBalance = currentBalance - amount
            val recipientBalance = toAccount[Accounts.balance]
            val newRecipientBalance = recipientBalance + amount

            // Update sender's account
            Accounts.update({ Accounts.id eq fromAccount[Accounts.id] }) {
                it[balance] = newSenderBalance
                it[availableBalance] = newSenderBalance
                it[Accounts.updatedAt] = CurrentTimestamp()
            }

            // Update recipient's account
            Accounts.update({ Accounts.id eq toAccount[Accounts.id] }) {
                it[balance] = newRecipientBalance
                it[availableBalance] = newRecipientBalance
                it[Accounts.updatedAt] = CurrentTimestamp()
            }

            // Create debit transaction for sender
            val senderTransactionId = Transactions.insert {
                it[Transactions.accountId] = fromAccount[Accounts.id].value
                it[type] = TransactionType.QR_PAYMENT
                it[Transactions.amount] = amount
                it[Transactions.description] = request.description
                it[status] = org.dals.project.database.TransactionStatus.COMPLETED
                it[category] = "QR_PAYMENT"
                it[Transactions.toAccountId] = toAccount[Accounts.id].value
                it[merchantName] = recipientName
                it[balanceAfter] = newSenderBalance
                it[createdAt] = CurrentTimestamp()
            }[Transactions.id]

            // Create credit transaction for recipient
            Transactions.insert {
                it[Transactions.accountId] = toAccount[Accounts.id].value
                it[type] = TransactionType.QR_RECEIPT
                it[Transactions.amount] = amount
                it[Transactions.description] = "QR Payment received from ${fromAccount[Accounts.accountNumber]}"
                it[status] = org.dals.project.database.TransactionStatus.COMPLETED
                it[category] = "QR_PAYMENT"
                it[Transactions.fromAccountId] = fromAccount[Accounts.id].value
                it[balanceAfter] = newRecipientBalance
                it[createdAt] = CurrentTimestamp()
            }

            QRPaymentResponse(
                success = true,
                message = "Payment successful",
                transactionId = senderTransactionId.toString(),
                recipientName = recipientName,
                amount = amount.toString()
            )

        } catch (e: Exception) {
            e.printStackTrace()
            QRPaymentResponse(
                success = false,
                message = "Payment failed: ${e.message}"
            )
        }
    }

    /**
     * Validate QR code data
     */
    suspend fun validateQRCode(qrData: String): QRPaymentResponse = DatabaseFactory.dbQuery {
        try {
            // Parse QR code data
            val customerId = UUID.fromString(
                qrData.substringAfter("\"customerId\":\"").substringBefore("\"")
            )

            val accountNumber = qrData.substringAfter("\"accountNumber\":\"").substringBefore("\"")

            // Verify account exists
            val account = Accounts
                .select {
                    (Accounts.customerId eq customerId) and (Accounts.accountNumber eq accountNumber)
                }
                .singleOrNull() ?: return@dbQuery QRPaymentResponse(
                    success = false,
                    message = "Invalid QR code - Account not found"
                )

            // Get customer name
            val customer = Customers
                .select { Customers.id eq customerId }
                .singleOrNull() ?: return@dbQuery QRPaymentResponse(
                    success = false,
                    message = "Invalid QR code - Customer not found"
                )

            val accountName = "${customer[Customers.firstName]} ${customer[Customers.lastName]}"

            QRPaymentResponse(
                success = true,
                message = "Valid QR code",
                recipientName = accountName
            )

        } catch (e: Exception) {
            QRPaymentResponse(
                success = false,
                message = "Invalid QR code format"
            )
        }
    }
}
