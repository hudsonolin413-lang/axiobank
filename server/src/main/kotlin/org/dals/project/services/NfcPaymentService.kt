package org.dals.project.services

import kotlinx.serialization.Serializable
import org.dals.project.database.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

@Serializable
data class InitiateNfcPaymentRequest(
    val customerId: String,
    val fromAccountId: String,
    val merchantName: String,
    val merchantId: String?,
    val amount: Double,
    val currency: String = "USD",
    val deviceId: String?,
    val nfcTagId: String?
)

@Serializable
data class NfcPaymentResponse(
    val id: String,
    val customerId: String,
    val fromAccountId: String,
    val merchantName: String,
    val merchantId: String?,
    val amount: Double,
    val currency: String,
    val transactionId: String?,
    val deviceId: String?,
    val nfcTagId: String?,
    val status: String,
    val paymentMethod: String,
    val authorizationCode: String?,
    val failureReason: String?,
    val initiatedAt: String,
    val completedAt: String?,
    val createdAt: String
)

@Serializable
data class NfcPaymentHistoryDto(
    val payments: List<NfcPaymentResponse>,
    val totalCount: Int,
    val totalAmount: Double
)

object NfcPaymentService {

    fun initiateNfcPayment(request: InitiateNfcPaymentRequest): Result<NfcPaymentResponse> {
        return try {
            transaction {
                // Validate customer
                val customer = Customers.select { Customers.id eq UUID.fromString(request.customerId) }
                    .singleOrNull()
                    ?: return@transaction Result.failure(Exception("Customer not found"))

                // Validate account
                val account = Accounts.select {
                    (Accounts.id eq UUID.fromString(request.fromAccountId)) and
                    (Accounts.customerId eq UUID.fromString(request.customerId))
                }.singleOrNull()
                    ?: return@transaction Result.failure(Exception("Account not found"))

                val currentBalance = account[Accounts.balance].toDouble()

                // Check sufficient balance
                if (currentBalance < request.amount) {
                    return@transaction Result.failure(Exception("Insufficient balance"))
                }

                // Check account status
                if (account[Accounts.status] != AccountStatus.ACTIVE) {
                    return@transaction Result.failure(Exception("Account is not active"))
                }

                // Create NFC payment record
                val nfcPaymentId = NfcPayments.insert {
                    it[customerId] = UUID.fromString(request.customerId)
                    it[fromAccountId] = UUID.fromString(request.fromAccountId)
                    it[merchantName] = request.merchantName
                    it[merchantId] = request.merchantId
                    it[amount] = BigDecimal.valueOf(request.amount)
                    it[currency] = request.currency
                    it[deviceId] = request.deviceId
                    it[nfcTagId] = request.nfcTagId
                    it[status] = "PENDING"
                    it[paymentMethod] = "NFC"
                }[NfcPayments.id].value

                val payment = NfcPayments.select { NfcPayments.id eq nfcPaymentId }
                    .single()

                Result.success(mapToNfcPaymentResponse(payment))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun processNfcPayment(nfcPaymentId: String): Result<NfcPaymentResponse> {
        return try {
            transaction {
                // Get NFC payment
                val payment = NfcPayments.select { NfcPayments.id eq UUID.fromString(nfcPaymentId) }
                    .singleOrNull()
                    ?: return@transaction Result.failure(Exception("NFC payment not found"))

                val status = payment[NfcPayments.status]
                if (status != "PENDING") {
                    return@transaction Result.failure(Exception("Payment is not in PENDING status"))
                }

                val customerId = payment[NfcPayments.customerId]
                val fromAccountId = payment[NfcPayments.fromAccountId]
                val amount = payment[NfcPayments.amount].toDouble()
                val merchantName = payment[NfcPayments.merchantName]

                // Get account current balance
                val account = Accounts.select { Accounts.id eq fromAccountId }.single()
                val currentBalance = account[Accounts.balance].toDouble()

                // Final balance check
                if (currentBalance < amount) {
                    // Mark as failed
                    NfcPayments.update({ NfcPayments.id eq UUID.fromString(nfcPaymentId) }) {
                        it[NfcPayments.status] = "FAILED"
                        it[failureReason] = "Insufficient balance"
                        it[completedAt] = Instant.now()
                    }
                    return@transaction Result.failure(Exception("Insufficient balance"))
                }

                // Generate authorization code
                val authCode = "NFC${System.currentTimeMillis().toString().takeLast(8)}"

                // Create transaction
                val transactionId = Transactions.insert {
                    it[Transactions.accountId] = fromAccountId
                    it[Transactions.type] = TransactionType.NFC_PAYMENT
                    it[Transactions.amount] = BigDecimal.valueOf(amount)
                    it[Transactions.description] = "NFC Payment to $merchantName"
                    it[Transactions.status] = TransactionStatus.COMPLETED
                    it[Transactions.category] = "PAYMENT"
                    it[Transactions.merchantName] = merchantName
                    it[Transactions.balanceAfter] = BigDecimal.valueOf(currentBalance - amount)
                    it[Transactions.reference] = authCode
                }[Transactions.id].value

                // Update account balance
                val newBalance = currentBalance - amount
                Accounts.update({ Accounts.id eq fromAccountId }) {
                    it[Accounts.balance] = BigDecimal.valueOf(newBalance)
                    it[Accounts.updatedAt] = Instant.now()
                }

                // Update NFC payment record
                NfcPayments.update({ NfcPayments.id eq UUID.fromString(nfcPaymentId) }) {
                    it[NfcPayments.transactionId] = transactionId
                    it[NfcPayments.status] = "COMPLETED"
                    it[NfcPayments.authorizationCode] = authCode
                    it[NfcPayments.completedAt] = Instant.now()
                }

                // Create receipt transaction
                Transactions.insert {
                    it[Transactions.accountId] = fromAccountId
                    it[Transactions.type] = TransactionType.NFC_RECEIPT
                    it[Transactions.amount] = BigDecimal.valueOf(amount)
                    it[Transactions.description] = "NFC Payment Receipt - $merchantName"
                    it[Transactions.status] = TransactionStatus.COMPLETED
                    it[Transactions.category] = "RECEIPT"
                    it[Transactions.merchantName] = merchantName
                    it[Transactions.reference] = authCode
                    it[Transactions.balanceAfter] = BigDecimal.valueOf(currentBalance - amount)
                }

                val updatedPayment = NfcPayments.select { NfcPayments.id eq UUID.fromString(nfcPaymentId) }
                    .single()

                Result.success(mapToNfcPaymentResponse(updatedPayment))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getNfcPaymentById(nfcPaymentId: String): Result<NfcPaymentResponse> {
        return try {
            transaction {
                val payment = NfcPayments.select { NfcPayments.id eq UUID.fromString(nfcPaymentId) }
                    .singleOrNull()
                    ?: return@transaction Result.failure(Exception("NFC payment not found"))

                Result.success(mapToNfcPaymentResponse(payment))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getNfcPaymentHistory(
        customerId: String,
        limit: Int = 50,
        offset: Int = 0
    ): Result<NfcPaymentHistoryDto> {
        return try {
            transaction {
                val payments = NfcPayments
                    .select { NfcPayments.customerId eq UUID.fromString(customerId) }
                    .orderBy(NfcPayments.createdAt to SortOrder.DESC)
                    .limit(limit, offset.toLong())
                    .map { mapToNfcPaymentResponse(it) }

                val totalCount = NfcPayments
                    .select { NfcPayments.customerId eq UUID.fromString(customerId) }
                    .count()
                    .toInt()

                val totalAmount = payments
                    .filter { it.status == "COMPLETED" }
                    .sumOf { it.amount }

                Result.success(
                    NfcPaymentHistoryDto(
                        payments = payments,
                        totalCount = totalCount,
                        totalAmount = totalAmount
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun cancelNfcPayment(nfcPaymentId: String): Result<NfcPaymentResponse> {
        return try {
            transaction {
                val payment = NfcPayments.select { NfcPayments.id eq UUID.fromString(nfcPaymentId) }
                    .singleOrNull()
                    ?: return@transaction Result.failure(Exception("NFC payment not found"))

                val status = payment[NfcPayments.status]
                if (status != "PENDING") {
                    return@transaction Result.failure(Exception("Only PENDING payments can be cancelled"))
                }

                NfcPayments.update({ NfcPayments.id eq UUID.fromString(nfcPaymentId) }) {
                    it[NfcPayments.status] = "CANCELLED"
                    it[failureReason] = "Cancelled by user"
                    it[completedAt] = Instant.now()
                }

                val updatedPayment = NfcPayments.select { NfcPayments.id eq UUID.fromString(nfcPaymentId) }
                    .single()

                Result.success(mapToNfcPaymentResponse(updatedPayment))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getActiveNfcPayments(customerId: String): Result<List<NfcPaymentResponse>> {
        return try {
            transaction {
                val payments = NfcPayments
                    .select {
                        (NfcPayments.customerId eq UUID.fromString(customerId)) and
                        (NfcPayments.status eq "PENDING")
                    }
                    .orderBy(NfcPayments.initiatedAt to SortOrder.DESC)
                    .map { mapToNfcPaymentResponse(it) }

                Result.success(payments)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapToNfcPaymentResponse(row: ResultRow): NfcPaymentResponse {
        return NfcPaymentResponse(
            id = row[NfcPayments.id].value.toString(),
            customerId = row[NfcPayments.customerId].toString(),
            fromAccountId = row[NfcPayments.fromAccountId].toString(),
            merchantName = row[NfcPayments.merchantName],
            merchantId = row[NfcPayments.merchantId],
            amount = row[NfcPayments.amount].toDouble(),
            currency = row[NfcPayments.currency],
            transactionId = row[NfcPayments.transactionId]?.toString(),
            deviceId = row[NfcPayments.deviceId],
            nfcTagId = row[NfcPayments.nfcTagId],
            status = row[NfcPayments.status],
            paymentMethod = row[NfcPayments.paymentMethod],
            authorizationCode = row[NfcPayments.authorizationCode],
            failureReason = row[NfcPayments.failureReason],
            initiatedAt = row[NfcPayments.initiatedAt].toString(),
            completedAt = row[NfcPayments.completedAt]?.toString(),
            createdAt = row[NfcPayments.createdAt].toString()
        )
    }
}
