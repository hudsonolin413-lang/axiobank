package org.dals.project.service

import org.dals.project.database.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class BulkTransferRecipient(
    val name: String,
    val phoneNumber: String,
    val accountNumber: String? = null,
    val amount: Double,
    val description: String? = null
)

data class BulkTransferRequest(
    val customerId: String,
    val fromAccountId: String,
    val batchName: String,
    val recipients: List<BulkTransferRecipient>,
    val description: String? = null
)

data class BulkTransferResponse(
    val bulkTransferId: String,
    val batchName: String,
    val totalRecipients: Int,
    val totalAmount: Double,
    val status: String,
    val message: String
)

data class BulkTransferStatus(
    val bulkTransferId: String,
    val batchName: String,
    val totalRecipients: Int,
    val totalAmount: Double,
    val completedTransfers: Int,
    val failedTransfers: Int,
    val status: String,
    val initiatedAt: String,
    val completedAt: String?,
    val recipients: List<RecipientStatus>
)

data class RecipientStatus(
    val recipientName: String,
    val recipientPhone: String,
    val amount: Double,
    val status: String,
    val failureReason: String? = null
)

class BulkTransferService {

    /**
     * Create a bulk transfer batch
     */
    fun createBulkTransfer(request: BulkTransferRequest): Result<BulkTransferResponse> {
        return try {
            val customerId = UUID.fromString(request.customerId)
            val fromAccountId = UUID.fromString(request.fromAccountId)

            // Validate account exists and has sufficient balance
            val account = transaction {
                Accounts.select { Accounts.id eq fromAccountId }.firstOrNull()
            } ?: return Result.failure(Exception("Account not found"))

            val totalAmount = request.recipients.sumOf { it.amount }
            val currentBalance = transaction {
                account[Accounts.balance].toDouble()
            }

            if (currentBalance < totalAmount) {
                return Result.failure(Exception("Insufficient balance. Available: $currentBalance, Required: $totalAmount"))
            }

            // Create bulk transfer record
            val bulkTransferId = transaction {
                BulkTransfers.insert {
                    it[this.customerId] = customerId
                    it[this.fromAccountId] = fromAccountId
                    it[batchName] = request.batchName
                    it[totalRecipients] = request.recipients.size
                    it[this.totalAmount] = BigDecimal.valueOf(totalAmount)
                    it[status] = "PENDING"
                    it[description] = request.description
                } get BulkTransfers.id
            }

            // Create recipient records
            transaction {
                request.recipients.forEach { recipient ->
                    BulkTransferRecipients.insert {
                        it[BulkTransferRecipients.bulkTransferId] = bulkTransferId.value
                        it[recipientName] = recipient.name
                        it[recipientPhone] = recipient.phoneNumber
                        it[recipientAccountNumber] = recipient.accountNumber
                        it[amount] = BigDecimal.valueOf(recipient.amount)
                        it[this.description] = recipient.description
                        it[status] = "PENDING"
                    }
                }
            }

            println("✅ Bulk transfer created: $bulkTransferId with ${request.recipients.size} recipients")

            Result.success(
                BulkTransferResponse(
                    bulkTransferId = bulkTransferId.value.toString(),
                    batchName = request.batchName,
                    totalRecipients = request.recipients.size,
                    totalAmount = totalAmount,
                    status = "PENDING",
                    message = "Bulk transfer created successfully"
                )
            )
        } catch (e: Exception) {
            println("❌ Error creating bulk transfer: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Process a bulk transfer batch
     */
    fun processBulkTransfer(bulkTransferId: String): Result<BulkTransferStatus> {
        return try {
            val bulkId = UUID.fromString(bulkTransferId)

            // Get bulk transfer details
            val bulkTransfer = transaction {
                BulkTransfers.select { BulkTransfers.id eq bulkId }.firstOrNull()
            } ?: return Result.failure(Exception("Bulk transfer not found"))

            val customerId = transaction { bulkTransfer[BulkTransfers.customerId] }
            val fromAccountId = transaction { bulkTransfer[BulkTransfers.fromAccountId] }

            // Update status to PROCESSING
            transaction {
                BulkTransfers.update({ BulkTransfers.id eq bulkId }) {
                    it[status] = "PROCESSING"
                }
            }

            // Get all recipients
            val recipients = transaction {
                BulkTransferRecipients.select { BulkTransferRecipients.bulkTransferId eq bulkId }
                    .map { row ->
                        row to row[BulkTransferRecipients.id]
                    }
            }

            var completedCount = 0
            var failedCount = 0
            val recipientStatuses = mutableListOf<RecipientStatus>()

            // Process each recipient
            recipients.forEach { (recipientRow, recipientId) ->
                try {
                    val amount = recipientRow[BulkTransferRecipients.amount].toDouble()
                    val recipientName = recipientRow[BulkTransferRecipients.recipientName]
                    val recipientPhone = recipientRow[BulkTransferRecipients.recipientPhone]

                    // Create individual transaction
                    val transactionId = transaction {
                        val currentBalance = Accounts.select { Accounts.id eq fromAccountId }
                            .first()[Accounts.balance]

                        val newBalance = currentBalance - BigDecimal.valueOf(amount)

                        // Update account balance
                        Accounts.update({ Accounts.id eq fromAccountId }) {
                            it[balance] = newBalance
                        }

                        // Create transaction record
                        Transactions.insert {
                            it[accountId] = fromAccountId
                            it[type] = TransactionType.BULK_TRANSFER
                            it[this.amount] = BigDecimal.valueOf(amount)
                            it[this.status] = TransactionStatus.COMPLETED
                            it[description] = "Bulk transfer to $recipientName - $recipientPhone"
                            it[balanceAfter] = newBalance
                            it[this.fromAccountId] = fromAccountId
                        } get Transactions.id
                    }

                    // Update recipient status
                    transaction {
                        BulkTransferRecipients.update({ BulkTransferRecipients.id eq recipientId }) {
                            it[status] = "COMPLETED"
                            it[this.transactionId] = transactionId.value
                            it[processedAt] = Instant.now()
                        }
                    }

                    completedCount++
                    recipientStatuses.add(
                        RecipientStatus(
                            recipientName = recipientName,
                            recipientPhone = recipientPhone,
                            amount = amount,
                            status = "COMPLETED"
                        )
                    )

                } catch (e: Exception) {
                    println("❌ Failed to process recipient ${recipientRow[BulkTransferRecipients.recipientName]}: ${e.message}")

                    // Update recipient status to FAILED
                    transaction {
                        BulkTransferRecipients.update({ BulkTransferRecipients.id eq recipientId }) {
                            it[status] = "FAILED"
                            it[failureReason] = e.message
                        }
                    }

                    failedCount++
                    recipientStatuses.add(
                        RecipientStatus(
                            recipientName = recipientRow[BulkTransferRecipients.recipientName],
                            recipientPhone = recipientRow[BulkTransferRecipients.recipientPhone],
                            amount = recipientRow[BulkTransferRecipients.amount].toDouble(),
                            status = "FAILED",
                            failureReason = e.message
                        )
                    )
                }
            }

            // Update bulk transfer final status
            val finalStatus = when {
                failedCount == 0 -> "COMPLETED"
                completedCount == 0 -> "FAILED"
                else -> "PARTIALLY_COMPLETED"
            }

            transaction {
                BulkTransfers.update({ BulkTransfers.id eq bulkId }) {
                    it[completedTransfers] = completedCount
                    it[failedTransfers] = failedCount
                    it[status] = finalStatus
                    it[completedAt] = Instant.now()
                }
            }

            println("✅ Bulk transfer processed: $completedCount completed, $failedCount failed")

            val status = getBulkTransferStatus(bulkTransferId).getOrThrow()
            Result.success(status)

        } catch (e: Exception) {
            println("❌ Error processing bulk transfer: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Get bulk transfer status
     */
    fun getBulkTransferStatus(bulkTransferId: String): Result<BulkTransferStatus> {
        return try {
            val bulkId = UUID.fromString(bulkTransferId)

            val bulkTransfer = transaction {
                BulkTransfers.select { BulkTransfers.id eq bulkId }.firstOrNull()
            } ?: return Result.failure(Exception("Bulk transfer not found"))

            val recipients = transaction {
                BulkTransferRecipients.select { BulkTransferRecipients.bulkTransferId eq bulkId }
                    .map { row ->
                        RecipientStatus(
                            recipientName = row[BulkTransferRecipients.recipientName],
                            recipientPhone = row[BulkTransferRecipients.recipientPhone],
                            amount = row[BulkTransferRecipients.amount].toDouble(),
                            status = row[BulkTransferRecipients.status],
                            failureReason = row[BulkTransferRecipients.failureReason]
                        )
                    }
            }

            Result.success(
                BulkTransferStatus(
                    bulkTransferId = bulkTransferId,
                    batchName = transaction { bulkTransfer[BulkTransfers.batchName] },
                    totalRecipients = transaction { bulkTransfer[BulkTransfers.totalRecipients] },
                    totalAmount = transaction { bulkTransfer[BulkTransfers.totalAmount].toDouble() },
                    completedTransfers = transaction { bulkTransfer[BulkTransfers.completedTransfers] },
                    failedTransfers = transaction { bulkTransfer[BulkTransfers.failedTransfers] },
                    status = transaction { bulkTransfer[BulkTransfers.status] },
                    initiatedAt = transaction { bulkTransfer[BulkTransfers.initiatedAt].toString() },
                    completedAt = transaction { bulkTransfer[BulkTransfers.completedAt]?.toString() },
                    recipients = recipients
                )
            )
        } catch (e: Exception) {
            println("❌ Error getting bulk transfer status: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Get all bulk transfers for a customer
     */
    fun getCustomerBulkTransfers(customerId: String): Result<List<BulkTransferStatus>> {
        return try {
            val custId = UUID.fromString(customerId)

            val bulkTransfers = transaction {
                BulkTransfers.select { BulkTransfers.customerId eq custId }
                    .orderBy(BulkTransfers.createdAt, SortOrder.DESC)
                    .map { it[BulkTransfers.id].value.toString() }
            }

            val statuses = bulkTransfers.mapNotNull { id ->
                getBulkTransferStatus(id).getOrNull()
            }

            Result.success(statuses)
        } catch (e: Exception) {
            println("❌ Error getting customer bulk transfers: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
