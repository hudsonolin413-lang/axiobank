package org.dals.project.database

import org.dals.project.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

class TransactionReversalRepository {

    fun createReversalRequest(request: CreateTransactionReversalRequest): TransactionReversalDto {
        return transaction {
            val reversalId = UUID.randomUUID()
            val now = Instant.now()

            // Fetch the transaction details to get account ID and amount
            val transactionId = UUID.fromString(request.transactionId)
            val transactionRow = Transactions.select { Transactions.id eq transactionId }.firstOrNull()
                ?: throw IllegalArgumentException("Transaction not found with ID: ${request.transactionId}")
            
            val accountId = transactionRow[Transactions.accountId]
            val amount = transactionRow[Transactions.amount]
            
            // Verify the account has sufficient balance if needed
            val account = Accounts.select { Accounts.id eq accountId }.firstOrNull()
                ?: throw IllegalArgumentException("Account not found")
            
            val currentBalance = account[Accounts.balance]
            
            // For REFUND type, check if account can receive the refund (no balance check needed for receiving)
            // For SEND_TO_RECEIVER type, the receiver account balance check will happen during completion

            // Calculate estimated completion date (1-3 business days from now)
            val estimatedCompletion = now.plusSeconds(86400 * 3) // 3 days
            val onHold = now.plusSeconds(86400) // Hold for 1 day minimum

            TransactionReversals.insert {
                it[TransactionReversals.id] = reversalId
                it[TransactionReversals.transactionId] = transactionId
                it[TransactionReversals.customerId] = UUID.fromString(request.customerId)
                it[TransactionReversals.accountId] = accountId
                it[TransactionReversals.amount] = amount
                it[TransactionReversals.reason] = request.reason
                it[TransactionReversals.requestedBy] = UUID.fromString(request.createdBy)
                it[TransactionReversals.status] = "PENDING"
                it[TransactionReversals.reversalType] = request.reversalType
                it[TransactionReversals.estimatedCompletionDate] = estimatedCompletion
                it[TransactionReversals.onHoldUntil] = onHold
                it[TransactionReversals.requestedAt] = now
                it[TransactionReversals.createdAt] = now
                it[TransactionReversals.updatedAt] = now
            }

            mapToDto(TransactionReversals.select { TransactionReversals.id eq reversalId }.first())
        }
    }

    fun getAllReversalRequests(page: Int = 1, pageSize: Int = 50): Pair<List<TransactionReversalDto>, Int> {
        return transaction {
            val offset = (page - 1) * pageSize

            val reversals = TransactionReversals
                .selectAll()
                .orderBy(TransactionReversals.requestedAt, SortOrder.DESC)
                .limit(pageSize, offset.toLong())
                .map { mapToDto(it) }

            val total = TransactionReversals.selectAll().count().toInt()

            Pair(reversals, total)
        }
    }

    fun getReversalRequestsByStatus(status: String, page: Int = 1, pageSize: Int = 50): Pair<List<TransactionReversalDto>, Int> {
        return transaction {
            val offset = (page - 1) * pageSize

            val reversals = TransactionReversals
                .select { TransactionReversals.status eq status }
                .orderBy(TransactionReversals.requestedAt, SortOrder.DESC)
                .limit(pageSize, offset.toLong())
                .map { mapToDto(it) }

            val total = TransactionReversals.select { TransactionReversals.status eq status }.count().toInt()

            Pair(reversals, total)
        }
    }

    fun getReversalRequestsByCustomerId(customerId: String): List<TransactionReversalDto> {
        return transaction {
            TransactionReversals
                .select { TransactionReversals.customerId eq UUID.fromString(customerId) }
                .orderBy(TransactionReversals.requestedAt, SortOrder.DESC)
                .map { mapToDto(it) }
        }
    }

    fun getReversalRequestById(reversalId: String): TransactionReversalDto? {
        return transaction {
            TransactionReversals
                .select { TransactionReversals.id eq UUID.fromString(reversalId) }
                .firstOrNull()
                ?.let { mapToDto(it) }
        }
    }

    fun approveReversalRequest(
        reversalId: String,
        approvedBy: String,
        reviewNotes: String? = null,
        reversalType: String? = null
    ): TransactionReversalDto {
        return transaction {
            val now = Instant.now()

            TransactionReversals.update({ TransactionReversals.id eq UUID.fromString(reversalId) }) {
                it[status] = "APPROVED"
                it[reviewedBy] = UUID.fromString(approvedBy)
                it[TransactionReversals.reviewNotes] = reviewNotes
                it[reviewedAt] = now
                it[updatedAt] = now
                if (reversalType != null) {
                    it[TransactionReversals.reversalType] = reversalType
                }
            }

            getReversalRequestById(reversalId)
                ?: throw IllegalStateException("Failed to retrieve updated reversal request")
        }
    }

    fun rejectReversalRequest(
        reversalId: String,
        rejectedBy: String,
        rejectionReason: String,
        reviewNotes: String? = null
    ): TransactionReversalDto {
        return transaction {
            val now = Instant.now()

            TransactionReversals.update({ TransactionReversals.id eq UUID.fromString(reversalId) }) {
                it[status] = "REJECTED"
                it[reviewedBy] = UUID.fromString(rejectedBy)
                it[TransactionReversals.rejectionReason] = rejectionReason
                it[TransactionReversals.reviewNotes] = reviewNotes
                it[reviewedAt] = now
                it[updatedAt] = now
            }

            getReversalRequestById(reversalId)
                ?: throw IllegalStateException("Failed to retrieve updated reversal request")
        }
    }

    fun completeReversalRequest(
        reversalId: String,
        completionNotes: String? = null
    ): TransactionReversalDto {
        return transaction {
            val now = Instant.now()

            TransactionReversals.update({ TransactionReversals.id eq UUID.fromString(reversalId) }) {
                it[status] = "COMPLETED"
                it[completedAt] = now
                it[updatedAt] = now
                if (completionNotes != null) {
                    it[reviewNotes] = completionNotes
                }
            }

            getReversalRequestById(reversalId)
                ?: throw IllegalStateException("Failed to retrieve updated reversal request")
        }
    }

    fun getPendingReversalsBeyondHoldPeriod(): List<TransactionReversalDto> {
        return transaction {
            val now = Instant.now()

            TransactionReversals
                .select {
                    (TransactionReversals.status eq "APPROVED") and
                    (TransactionReversals.onHoldUntil.isNotNull()) and
                    (TransactionReversals.onHoldUntil less now)
                }
                .map { mapToDto(it) }
        }
    }

    private fun mapToDto(row: ResultRow): TransactionReversalDto {
        return TransactionReversalDto(
            id = row[TransactionReversals.id].toString(),
            transactionId = row[TransactionReversals.transactionId].toString(),
            customerId = row[TransactionReversals.customerId].toString(),
            accountId = row[TransactionReversals.accountId].toString(),
            amount = row[TransactionReversals.amount].toString(),
            reason = row[TransactionReversals.reason],
            requestedBy = row[TransactionReversals.requestedBy].toString(),
            status = row[TransactionReversals.status],
            reviewedBy = row[TransactionReversals.reviewedBy]?.toString(),
            reviewNotes = row[TransactionReversals.reviewNotes],
            rejectionReason = row[TransactionReversals.rejectionReason],
            requestedAt = row[TransactionReversals.requestedAt].toString(),
            reviewedAt = row[TransactionReversals.reviewedAt]?.toString(),
            completedAt = row[TransactionReversals.completedAt]?.toString(),
            estimatedCompletionDate = row[TransactionReversals.estimatedCompletionDate]?.toString(),
            onHoldUntil = row[TransactionReversals.onHoldUntil]?.toString(),
            reversalType = row[TransactionReversals.reversalType],
            createdAt = row[TransactionReversals.createdAt].toString(),
            updatedAt = row[TransactionReversals.updatedAt].toString()
        )
    }
}
