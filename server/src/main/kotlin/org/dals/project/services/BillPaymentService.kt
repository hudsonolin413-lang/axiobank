package org.dals.project.services

import kotlinx.serialization.Serializable
import org.dals.project.database.*
import org.dals.project.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.SQLException
import java.time.Instant
import java.util.UUID

@Serializable
data class BillVendor(
    val id: String,
    val vendorCode: String,
    val vendorName: String,
    val category: String,
    val description: String?,
    val logoUrl: String?,
    val requiresAccountNumber: Boolean,
    val accountNumberLabel: String,
    val minAmount: Double,
    val maxAmount: Double,
    val processingFeeType: String,
    val processingFeeValue: Double
)

@Serializable
data class SavedBiller(
    val id: String,
    val vendorId: String,
    val vendorName: String,
    val nickname: String?,
    val accountNumber: String,
    val category: String,
    val isFavorite: Boolean
)

@Serializable
data class BillPayment(
    val id: String,
    val vendorId: String,
    val vendorName: String,
    val accountNumber: String,
    val amount: Double,
    val processingFee: Double,
    val totalAmount: Double,
    val paymentReference: String,
    val vendorReference: String?,
    val status: String,
    val description: String?,
    val createdAt: String,
    val processedAt: String?
)

@Serializable
data class PayBillRequest(
    val userId: String,
    val vendorId: String,
    val accountNumber: String,
    val amount: Double,
    val description: String? = null,
    val saveBiller: Boolean = false,
    val billerNickname: String? = null
)

@Serializable
data class PayBillResponse(
    val success: Boolean,
    val message: String,
    val payment: BillPayment? = null,
    val newBalance: Double? = null
)

object BillVendorsTable : Table("bill_vendors") {
    val id = uuid("id").autoGenerate()
    val vendorCode = varchar("vendor_code", 50).uniqueIndex()
    val vendorName = varchar("vendor_name", 200)
    val category = varchar("category", 50)
    val description = text("description").nullable()
    val logoUrl = text("logo_url").nullable()
    val isActive = bool("is_active").default(true)
    val requiresAccountNumber = bool("requires_account_number").default(true)
    val accountNumberLabel = varchar("account_number_label", 100).default("Account Number")
    val minAmount = decimal("min_amount", 15, 2).default(BigDecimal("1.00"))
    val maxAmount = decimal("max_amount", 15, 2).default(BigDecimal("1000000.00"))
    val processingFeeType = varchar("processing_fee_type", 20).default("PERCENTAGE")
    val processingFeeValue = decimal("processing_fee_value", 10, 4).default(BigDecimal("0.0"))
    override val primaryKey = PrimaryKey(id)
}

object UserSavedBillersTable : Table("user_saved_billers") {
    val id = uuid("id").autoGenerate()
    val userId = uuid("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val vendorId = uuid("vendor_id").references(BillVendorsTable.id, onDelete = ReferenceOption.CASCADE)
    val nickname = varchar("nickname", 100).nullable()
    val accountNumber = varchar("account_number", 100)
    val isFavorite = bool("is_favorite").default(false)
    val createdAt = varchar("created_at", 50).default(Instant.now().toString())
    override val primaryKey = PrimaryKey(id)
}

object BillPaymentsTable : Table("bill_payments") {
    val id = uuid("id").autoGenerate()
    val userId = uuid("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val vendorId = uuid("vendor_id").references(BillVendorsTable.id)
    val accountNumber = varchar("account_number", 100)
    val amount = decimal("amount", 15, 2)
    val processingFee = decimal("processing_fee", 15, 2).default(BigDecimal.ZERO)
    val totalAmount = decimal("total_amount", 15, 2)
    val paymentMethod = varchar("payment_method", 50).default("WALLET")
    val paymentReference = varchar("payment_reference", 100).uniqueIndex()
    val vendorReference = varchar("vendor_reference", 100).nullable()
    val status = varchar("status", 50).default("PENDING")
    val description = text("description").nullable()
    val transactionId = uuid("transaction_id").references(Transactions.id).nullable()
    val createdAt = varchar("created_at", 50).default(Instant.now().toString())
    val processedAt = varchar("processed_at", 50).nullable()
    override val primaryKey = PrimaryKey(id)
}

class BillPaymentService(private val transactionService: TransactionService) {

    /**
     * Get all available bill vendors, optionally filtered by category
     */
    suspend fun getAllVendors(category: String? = null): List<BillVendor> = DatabaseFactory.dbQuery {
        val query = BillVendorsTable.select { BillVendorsTable.isActive eq true }
            .let { if (category != null) it.andWhere { BillVendorsTable.category eq category } else it }

        query.map { row ->
            BillVendor(
                id = row[BillVendorsTable.id].toString(),
                vendorCode = row[BillVendorsTable.vendorCode],
                vendorName = row[BillVendorsTable.vendorName],
                category = row[BillVendorsTable.category],
                description = row[BillVendorsTable.description],
                logoUrl = row[BillVendorsTable.logoUrl],
                requiresAccountNumber = row[BillVendorsTable.requiresAccountNumber],
                accountNumberLabel = row[BillVendorsTable.accountNumberLabel],
                minAmount = row[BillVendorsTable.minAmount].toDouble(),
                maxAmount = row[BillVendorsTable.maxAmount].toDouble(),
                processingFeeType = row[BillVendorsTable.processingFeeType],
                processingFeeValue = row[BillVendorsTable.processingFeeValue].toDouble()
            )
        }
    }

    /**
     * Get vendor categories
     */
    suspend fun getVendorCategories(): List<String> = DatabaseFactory.dbQuery {
        BillVendorsTable
            .slice(BillVendorsTable.category)
            .select { BillVendorsTable.isActive eq true }
            .withDistinct()
            .map { it[BillVendorsTable.category] }
            .sorted()
    }

    /**
     * Get user's saved billers
     */
    suspend fun getSavedBillers(userId: UUID): List<SavedBiller> = DatabaseFactory.dbQuery {
        (UserSavedBillersTable innerJoin BillVendorsTable)
            .select { UserSavedBillersTable.userId eq userId }
            .orderBy(UserSavedBillersTable.isFavorite to SortOrder.DESC, UserSavedBillersTable.createdAt to SortOrder.DESC)
            .map { row ->
                SavedBiller(
                    id = row[UserSavedBillersTable.id].toString(),
                    vendorId = row[UserSavedBillersTable.vendorId].toString(),
                    vendorName = row[BillVendorsTable.vendorName],
                    nickname = row[UserSavedBillersTable.nickname],
                    accountNumber = row[UserSavedBillersTable.accountNumber],
                    category = row[BillVendorsTable.category],
                    isFavorite = row[UserSavedBillersTable.isFavorite]
                )
            }
    }

    /**
     * Save a biller for quick access
     */
    suspend fun saveBiller(userId: UUID, vendorId: UUID, accountNumber: String, nickname: String?): Result<SavedBiller> = DatabaseFactory.dbQuery {
        try {
            val billerId = UserSavedBillersTable.insert {
                it[this.userId] = userId
                it[this.vendorId] = vendorId
                it[this.accountNumber] = accountNumber
                it[this.nickname] = nickname
                it[isFavorite] = false
            } get UserSavedBillersTable.id

            // Fetch vendor details
            val vendor = BillVendorsTable.select { BillVendorsTable.id eq vendorId }.firstOrNull()
                ?: return@dbQuery Result.failure(Exception("Vendor not found"))

            Result.success(SavedBiller(
                id = billerId.toString(),
                vendorId = vendorId.toString(),
                vendorName = vendor[BillVendorsTable.vendorName],
                nickname = nickname,
                accountNumber = accountNumber,
                category = vendor[BillVendorsTable.category],
                isFavorite = false
            ))
        } catch (e: SQLException) {
            Result.failure(e)
        }
    }

    /**
     * Calculate processing fee
     */
    private fun calculateFee(vendor: ResultRow, amount: BigDecimal): BigDecimal {
        val feeType = vendor[BillVendorsTable.processingFeeType]
        val feeValue = vendor[BillVendorsTable.processingFeeValue]

        return when (feeType) {
            "PERCENTAGE" -> (amount * feeValue / BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)
            "FLAT" -> feeValue
            else -> BigDecimal.ZERO
        }
    }

    /**
     * Pay a bill
     */
    suspend fun payBill(request: PayBillRequest): PayBillResponse = DatabaseFactory.dbQuery {
        try {
            val userId = UUID.fromString(request.userId)
            val vendorId = UUID.fromString(request.vendorId)
            val amount = BigDecimal(request.amount)

            // Validate vendor
            val vendor = BillVendorsTable.select {
                (BillVendorsTable.id eq vendorId) and (BillVendorsTable.isActive eq true)
            }.firstOrNull()
                ?: return@dbQuery PayBillResponse(false, "Vendor not found or inactive")

            // Validate amount
            val minAmount = vendor[BillVendorsTable.minAmount]
            val maxAmount = vendor[BillVendorsTable.maxAmount]
            if (amount < minAmount || amount > maxAmount) {
                return@dbQuery PayBillResponse(
                    false,
                    "Amount must be between ${minAmount.toDouble()} and ${maxAmount.toDouble()}"
                )
            }

            // Calculate fee
            val processingFee = calculateFee(vendor, amount)
            val totalAmount = amount + processingFee

            // Get customer and account for this user
            val user = Users.select { Users.id eq userId }.firstOrNull()
                ?: return@dbQuery PayBillResponse(false, "User not found")

            // Find customer by username or email
            val customer = Customers.select {
                (Customers.username eq user[Users.username]) or (Customers.email eq user[Users.email])
            }.firstOrNull()
                ?: return@dbQuery PayBillResponse(false, "Customer account not found")

            val customerId = customer[Customers.id]

            // Get the user's account
            val account = Accounts.select { Accounts.customerId eq customerId.value }.firstOrNull()
                ?: return@dbQuery PayBillResponse(false, "Bank account not found")

            val currentBalance = account[Accounts.balance]
            if (currentBalance < totalAmount) {
                return@dbQuery PayBillResponse(false, "Insufficient balance")
            }

            // Generate payment reference
            val paymentReference = "BILL${System.currentTimeMillis()}${(1000..9999).random()}"

            // Create bill payment record
            val paymentId = BillPaymentsTable.insert {
                it[this.userId] = userId
                it[this.vendorId] = vendorId
                it[accountNumber] = request.accountNumber
                it[this.amount] = amount
                it[this.processingFee] = processingFee
                it[this.totalAmount] = totalAmount
                it[this.paymentReference] = paymentReference
                it[status] = "PROCESSING"
                it[description] = request.description
                it[createdAt] = Instant.now().toString()
            } get BillPaymentsTable.id

            // Create transaction
            val accountId = account[Accounts.id]
            val transactionRequest = CreateTransactionRequest(
                accountId = accountId.toString(),
                type = "BILL_PAYMENT",
                amount = totalAmount.toString(),
                description = "Bill Payment: ${vendor[BillVendorsTable.vendorName]} - ${request.accountNumber}",
                fromAccountId = accountId.toString(),
                toAccountId = null
            )

            val transactionResult = transactionService.createTransaction(transactionRequest)

            if (!transactionResult.success) {
                // Rollback bill payment
                BillPaymentsTable.update({ BillPaymentsTable.id eq paymentId }) {
                    it[status] = "FAILED"
                }
                return@dbQuery PayBillResponse(false, transactionResult.message)
            }

            // Update bill payment with transaction ID and status
            val vendorRef = "VND${System.currentTimeMillis()}"
            val transId = UUID.fromString(transactionResult.data!!.id.toString())
            BillPaymentsTable.update({ BillPaymentsTable.id eq paymentId }) {
                it[transactionId] = transId
                it[vendorReference] = vendorRef
                it[status] = "COMPLETED"
                it[processedAt] = Instant.now().toString()
            }

            // Save biller if requested
            if (request.saveBiller) {
                try {
                    saveBiller(userId, vendorId, request.accountNumber, request.billerNickname)
                } catch (e: Exception) {
                    // Ignore save error, payment already succeeded
                }
            }

            // Get updated balance
            val newBalance = Accounts.select { Accounts.id eq accountId }
                .first()[Accounts.balance].toDouble()

            PayBillResponse(
                success = true,
                message = "Bill payment successful",
                payment = BillPayment(
                    id = paymentId.toString(),
                    vendorId = vendorId.toString(),
                    vendorName = vendor[BillVendorsTable.vendorName],
                    accountNumber = request.accountNumber,
                    amount = amount.toDouble(),
                    processingFee = processingFee.toDouble(),
                    totalAmount = totalAmount.toDouble(),
                    paymentReference = paymentReference,
                    vendorReference = vendorRef,
                    status = "COMPLETED",
                    description = request.description,
                    createdAt = Instant.now().toString(),
                    processedAt = Instant.now().toString()
                ),
                newBalance = newBalance
            )
        } catch (e: Exception) {
            PayBillResponse(false, "Error processing bill payment: ${e.message}")
        }
    }

    /**
     * Get user's bill payment history
     */
    suspend fun getPaymentHistory(userId: UUID, limit: Int = 50): List<BillPayment> = DatabaseFactory.dbQuery {
        (BillPaymentsTable innerJoin BillVendorsTable)
            .select { BillPaymentsTable.userId eq userId }
            .orderBy(BillPaymentsTable.createdAt to SortOrder.DESC)
            .limit(limit)
            .map { row ->
                BillPayment(
                    id = row[BillPaymentsTable.id].toString(),
                    vendorId = row[BillPaymentsTable.vendorId].toString(),
                    vendorName = row[BillVendorsTable.vendorName],
                    accountNumber = row[BillPaymentsTable.accountNumber],
                    amount = row[BillPaymentsTable.amount].toDouble(),
                    processingFee = row[BillPaymentsTable.processingFee].toDouble(),
                    totalAmount = row[BillPaymentsTable.totalAmount].toDouble(),
                    paymentReference = row[BillPaymentsTable.paymentReference],
                    vendorReference = row[BillPaymentsTable.vendorReference],
                    status = row[BillPaymentsTable.status],
                    description = row[BillPaymentsTable.description],
                    createdAt = row[BillPaymentsTable.createdAt],
                    processedAt = row[BillPaymentsTable.processedAt]
                )
            }
    }
}
