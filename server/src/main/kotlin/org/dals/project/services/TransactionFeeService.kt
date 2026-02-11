package org.dals.project.services

import org.dals.project.database.*
import org.dals.project.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.util.*

class TransactionFeeService {

    /**
     * Calculate fee for online payment
     */
    fun calculateOnlinePaymentFee(amount: BigDecimal): BigDecimal {
        return calculateTransactionFee("ONLINE_PAYMENT", amount)
    }

    /**
     * Calculate fee for POS transaction
     */
    fun calculatePOSTransactionFee(amount: BigDecimal): BigDecimal {
        return calculateTransactionFee("POS_TRANSACTION", amount)
    }

    /**
     * Calculate fee for bill payment
     */
    fun calculateBillPaymentFee(amount: BigDecimal): BigDecimal {
        return calculateTransactionFee("BILL_PAYMENT", amount)
    }

    /**
     * Calculate fee for card transfer
     */
    fun calculateTransferFee(amount: BigDecimal): BigDecimal {
        return calculateTransactionFee("CARD_TRANSFER", amount)
    }

    /**
     * Calculate fee for ATM withdrawal
     */
    fun calculateATMWithdrawalFee(amount: BigDecimal): BigDecimal {
        return calculateTransactionFee("ATM_WITHDRAWAL", amount)
    }

    /**
     * Calculate fee based on transaction type and amount using the fee structure
     */
    fun calculateTransactionFee(transactionType: String, amount: BigDecimal): BigDecimal {
        return transaction {
            val feeStructure = TransactionFeeStructure.select {
                (TransactionFeeStructure.transactionType eq transactionType) and
                (TransactionFeeStructure.minAmount lessEq amount) and
                (TransactionFeeStructure.maxAmount greaterEq amount) and
                (TransactionFeeStructure.isActive eq true)
            }.singleOrNull()

            if (feeStructure != null) {
                val fixedFee = feeStructure[TransactionFeeStructure.feeAmount]
                val percentageFee = feeStructure[TransactionFeeStructure.feePercentage]

                // If percentage fee is defined, use it; otherwise use fixed fee
                if (percentageFee != null && percentageFee > BigDecimal.ZERO) {
                    amount * percentageFee
                } else {
                    fixedFee
                }
            } else {
                BigDecimal.ZERO // No fee if no structure found
            }
        }
    }

    /**
     * Record a transaction fee and credit it to the profit wallet
     */
    fun recordTransactionFee(
        transactionId: UUID?,
        customerId: UUID,
        accountId: UUID,
        transactionType: String,
        transactionAmount: BigDecimal,
        feeAmount: BigDecimal,
        processedBy: UUID?,
        branchId: UUID?
    ): ApiResponse<String> {
        return try {
            transaction {
                // Get or create the company profit wallet
                val profitWallet = MasterWallets.select {
                    MasterWallets.walletType eq MasterWalletType.TRANSACTION_FEES_PROFIT
                }.singleOrNull()

                if (profitWallet == null) {
                    return@transaction ApiResponse<String>(
                        success = false,
                        message = "Company profit wallet not found. Please create it first.",
                        error = "PROFIT_WALLET_NOT_FOUND"
                    )
                }

                val profitWalletId = profitWallet[MasterWallets.id]

                // Find the fee structure that was used
                val feeStructure = TransactionFeeStructure.select {
                    (TransactionFeeStructure.transactionType eq transactionType) and
                    (TransactionFeeStructure.minAmount lessEq transactionAmount) and
                    (TransactionFeeStructure.maxAmount greaterEq transactionAmount) and
                    (TransactionFeeStructure.isActive eq true)
                }.singleOrNull()

                // Record the fee collection
                val feeRecordId = TransactionFeeRecords.insert {
                    it[TransactionFeeRecords.transactionId] = transactionId
                    it[TransactionFeeRecords.customerId] = customerId
                    it[TransactionFeeRecords.accountId] = accountId
                    it[TransactionFeeRecords.transactionType] = transactionType
                    it[TransactionFeeRecords.transactionAmount] = transactionAmount
                    it[TransactionFeeRecords.feeAmount] = feeAmount
                    it[TransactionFeeRecords.feeStructureId] = feeStructure?.get(TransactionFeeStructure.id)?.value
                    it[TransactionFeeRecords.profitWalletId] = profitWalletId.value
                    it[TransactionFeeRecords.processedBy] = processedBy
                    it[TransactionFeeRecords.branchId] = branchId
                    it[TransactionFeeRecords.status] = "COLLECTED"
                    it[TransactionFeeRecords.description] = "Transaction fee collected for $transactionType"
                }[TransactionFeeRecords.id]

                // Credit the profit wallet
                val currentBalance = profitWallet[MasterWallets.balance]
                val newBalance = currentBalance + feeAmount

                MasterWallets.update({ MasterWallets.id eq profitWalletId }) {
                    it[MasterWallets.balance] = newBalance
                    it[MasterWallets.availableBalance] = newBalance
                }

                // Record the wallet transaction
                MasterWalletTransactions.insert {
                    it[MasterWalletTransactions.walletId] = profitWalletId.value
                    it[MasterWalletTransactions.transactionType] = MasterWalletTransactionType.FUND_ALLOCATION // Best fit for profit
                    it[MasterWalletTransactions.amount] = feeAmount
                    it[MasterWalletTransactions.balanceBefore] = currentBalance
                    it[MasterWalletTransactions.balanceAfter] = newBalance
                    it[MasterWalletTransactions.description] = "Transaction fee collected: $transactionType"
                    it[MasterWalletTransactions.reference] = feeRecordId.toString()
                    it[MasterWalletTransactions.status] = TransactionStatus.COMPLETED
                    it[MasterWalletTransactions.riskLevel] = "LOW"
                }

                ApiResponse(
                    success = true,
                    message = "Transaction fee recorded and credited to profit wallet",
                    data = feeRecordId.toString()
                )
            }
        } catch (e: Exception) {
            println("Error recording transaction fee: ${e.message}")
            e.printStackTrace()
            ApiResponse(
                success = false,
                message = "Failed to record transaction fee: ${e.message}",
                error = e.message
            )
        }
    }

    /**
     * Get all fee structures
     */
    fun getAllFeeStructures(): ListResponse<TransactionFeeStructureDto> {
        return try {
            transaction {
                val structures = TransactionFeeStructure.selectAll()
                    .orderBy(TransactionFeeStructure.transactionType to SortOrder.ASC)
                    .map { row ->
                        TransactionFeeStructureDto(
                            id = row[TransactionFeeStructure.id].toString(),
                            transactionType = row[TransactionFeeStructure.transactionType],
                            minAmount = row[TransactionFeeStructure.minAmount].toDouble(),
                            maxAmount = row[TransactionFeeStructure.maxAmount].toDouble(),
                            feeAmount = row[TransactionFeeStructure.feeAmount].toDouble(),
                            feePercentage = row[TransactionFeeStructure.feePercentage]?.toDouble(),
                            currency = row[TransactionFeeStructure.currency],
                            isActive = row[TransactionFeeStructure.isActive],
                            description = row[TransactionFeeStructure.description],
                            createdAt = row[TransactionFeeStructure.createdAt].toString()
                        )
                    }

                ListResponse(
                    success = true,
                    message = "Fee structures retrieved successfully",
                    data = structures,
                    total = structures.size,
                    page = 1,
                    pageSize = 100
                )
            }
        } catch (e: Exception) {
            println("Error retrieving fee structures: ${e.message}")
            e.printStackTrace()
            ListResponse(
                success = false,
                message = "Failed to retrieve fee structures: ${e.message}",
                data = emptyList(),
                total = 0,
                page = 1,
                pageSize = 100
            )
        }
    }

    /**
     * Get total fees collected
     */
    fun getTotalFeesCollected(
        startDate: String? = null,
        endDate: String? = null
    ): ApiResponse<ProfitSummaryDto> {
        return try {
            transaction {
                val query = TransactionFeeRecords.selectAll()

                // TODO: Add date filtering if needed

                val records = query.toList()
                val totalFees = records.sumOf { it[TransactionFeeRecords.feeAmount] }
                val totalTransactions = records.size

                // Group by transaction type
                val feesByType = records.groupBy { it[TransactionFeeRecords.transactionType] }
                    .mapValues { (_, rows) -> rows.sumOf { it[TransactionFeeRecords.feeAmount] } }

                ApiResponse(
                    success = true,
                    message = "Profit summary retrieved successfully",
                    data = ProfitSummaryDto(
                        totalFeesCollected = totalFees.toDouble(),
                        totalTransactions = totalTransactions,
                        feesByType = feesByType.mapValues { it.value.toDouble() }
                    )
                )
            }
        } catch (e: Exception) {
            println("Error retrieving profit summary: ${e.message}")
            e.printStackTrace()
            ApiResponse(
                success = false,
                message = "Failed to retrieve profit summary: ${e.message}"
            )
        }
    }

    /**
     * Create a new fee structure
     */
    fun createFeeStructure(request: org.dals.project.routes.CreateFeeStructureRequest): ApiResponse<String> {
        return try {
            transaction {
                val id = TransactionFeeStructure.insert {
                    it[transactionType] = request.transactionType
                    it[minAmount] = BigDecimal(request.minAmount)
                    it[maxAmount] = BigDecimal(request.maxAmount)
                    it[feeAmount] = BigDecimal(request.feeAmount)
                    it[feePercentage] = request.feePercentage?.let { pct -> BigDecimal(pct) }
                    it[currency] = request.currency
                    it[description] = request.description
                    it[createdBy] = UUID.fromString(request.createdBy)
                }[TransactionFeeStructure.id]

                ApiResponse(
                    success = true,
                    message = "Fee structure created successfully",
                    data = id.toString()
                )
            }
        } catch (e: Exception) {
            println("Error creating fee structure: ${e.message}")
            e.printStackTrace()
            ApiResponse(
                success = false,
                message = "Failed to create fee structure: ${e.message}"
            )
        }
    }

    /**
     * Update an existing fee structure
     */
    fun updateFeeStructure(id: UUID, request: org.dals.project.routes.UpdateFeeStructureRequest): ApiResponse<String> {
        return try {
            transaction {
                val updated = TransactionFeeStructure.update({ TransactionFeeStructure.id eq id }) {
                    request.transactionType?.let { type -> it[transactionType] = type }
                    request.minAmount?.let { min -> it[minAmount] = BigDecimal(min) }
                    request.maxAmount?.let { max -> it[maxAmount] = BigDecimal(max) }
                    request.feeAmount?.let { fee -> it[feeAmount] = BigDecimal(fee) }
                    request.feePercentage?.let { pct -> it[feePercentage] = BigDecimal(pct) }
                    request.isActive?.let { active -> it[isActive] = active }
                    request.description?.let { desc -> it[description] = desc }
                }

                if (updated > 0) {
                    ApiResponse(
                        success = true,
                        message = "Fee structure updated successfully",
                        data = id.toString()
                    )
                } else {
                    ApiResponse(
                        success = false,
                        message = "Fee structure not found"
                    )
                }
            }
        } catch (e: Exception) {
            println("Error updating fee structure: ${e.message}")
            e.printStackTrace()
            ApiResponse(
                success = false,
                message = "Failed to update fee structure: ${e.message}"
            )
        }
    }

    /**
     * Delete (deactivate) a fee structure
     */
    fun deleteFeeStructure(id: UUID): ApiResponse<String> {
        return try {
            transaction {
                val updated = TransactionFeeStructure.update({ TransactionFeeStructure.id eq id }) {
                    it[isActive] = false
                }

                if (updated > 0) {
                    ApiResponse(
                        success = true,
                        message = "Fee structure deactivated successfully",
                        data = id.toString()
                    )
                } else {
                    ApiResponse(
                        success = false,
                        message = "Fee structure not found"
                    )
                }
            }
        } catch (e: Exception) {
            println("Error deleting fee structure: ${e.message}")
            e.printStackTrace()
            ApiResponse(
                success = false,
                message = "Failed to delete fee structure: ${e.message}"
            )
        }
    }

    /**
     * Create the company profit wallet
     */
    fun createCompanyProfitWallet(request: org.dals.project.routes.CreateProfitWalletRequest): ApiResponse<String> {
        return try {
            transaction {
                // Check if profit wallet already exists
                val existing = MasterWallets.select {
                    MasterWallets.walletType eq MasterWalletType.TRANSACTION_FEES_PROFIT
                }.singleOrNull()

                if (existing != null) {
                    return@transaction ApiResponse<String>(
                        success = false,
                        message = "Company profit wallet already exists",
                        data = existing[MasterWallets.id].toString()
                    )
                }

                // Create the profit wallet
                val walletId = MasterWallets.insert {
                    it[walletName] = request.walletName
                    it[walletType] = MasterWalletType.TRANSACTION_FEES_PROFIT
                    it[currency] = request.currency
                    it[balance] = BigDecimal.ZERO
                    it[availableBalance] = BigDecimal.ZERO
                    it[reserveBalance] = BigDecimal.ZERO
                    it[securityLevel] = WalletSecurityLevel.MAXIMUM
                    it[status] = WalletStatus.ACTIVE
                    it[authorizedUsers] = "[]" // JSON array
                    it[encryptionKey] = UUID.randomUUID().toString() // Generate encryption key
                    it[createdBy] = UUID.fromString(request.createdBy)
                    it[managedBy] = UUID.fromString(request.managedBy)
                    it[reconciliationStatus] = "PENDING"
                }[MasterWallets.id]

                ApiResponse(
                    success = true,
                    message = "Company profit wallet created successfully",
                    data = walletId.toString()
                )
            }
        } catch (e: Exception) {
            println("Error creating profit wallet: ${e.message}")
            e.printStackTrace()
            ApiResponse(
                success = false,
                message = "Failed to create profit wallet: ${e.message}"
            )
        }
    }

    /**
     * Create initial fee structures based on the provided fee schedule
     */
    fun initializeFeeStructures(createdBy: UUID): ApiResponse<String> {
        return try {
            transaction {
                val feeSchedules = listOf(
                    // BANK TO MPESA
                    FeeSchedule("BANK_TO_MPESA", 1.0, 100.0, 0.0),
                    FeeSchedule("BANK_TO_MPESA", 101.0, 500.0, 10.0),
                    FeeSchedule("BANK_TO_MPESA", 501.0, 1000.0, 12.0),
                    FeeSchedule("BANK_TO_MPESA", 1001.0, 1500.0, 14.0),
                    FeeSchedule("BANK_TO_MPESA", 1501.0, 2500.0, 23.0),
                    FeeSchedule("BANK_TO_MPESA", 2501.0, 3500.0, 33.0),
                    FeeSchedule("BANK_TO_MPESA", 3501.0, 5000.0, 43.0),
                    FeeSchedule("BANK_TO_MPESA", 5001.0, 7500.0, 55.0),
                    FeeSchedule("BANK_TO_MPESA", 7501.0, 20000.0, 65.0),
                    FeeSchedule("BANK_TO_MPESA", 20001.0, 150000.0, 67.0),

                    // BANK TO AIRTEL MONEY
                    FeeSchedule("BANK_TO_AIRTEL", 1.0, 100.0, 0.0),
                    FeeSchedule("BANK_TO_AIRTEL", 101.0, 500.0, 11.0),
                    FeeSchedule("BANK_TO_AIRTEL", 501.0, 1000.0, 13.0),
                    FeeSchedule("BANK_TO_AIRTEL", 1001.0, 1500.0, 20.0),
                    FeeSchedule("BANK_TO_AIRTEL", 1501.0, 2500.0, 25.0),
                    FeeSchedule("BANK_TO_AIRTEL", 2501.0, 3500.0, 35.0),
                    FeeSchedule("BANK_TO_AIRTEL", 3501.0, 5000.0, 45.0),
                    FeeSchedule("BANK_TO_AIRTEL", 5001.0, 7500.0, 55.0),
                    FeeSchedule("BANK_TO_AIRTEL", 7501.0, 20000.0, 65.0),
                    FeeSchedule("BANK_TO_AIRTEL", 20001.0, 150000.0, 65.0),

                    // EQUITEL TO MPESA
                    FeeSchedule("EQUITEL_TO_MPESA", 1.0, 100.0, 0.0),
                    FeeSchedule("EQUITEL_TO_MPESA", 101.0, 500.0, 10.0),
                    FeeSchedule("EQUITEL_TO_MPESA", 501.0, 1000.0, 12.0),
                    FeeSchedule("EQUITEL_TO_MPESA", 1001.0, 1500.0, 14.0),
                    FeeSchedule("EQUITEL_TO_MPESA", 1501.0, 2500.0, 23.0),
                    FeeSchedule("EQUITEL_TO_MPESA", 2501.0, 3500.0, 33.0),
                    FeeSchedule("EQUITEL_TO_MPESA", 3501.0, 5000.0, 43.0),
                    FeeSchedule("EQUITEL_TO_MPESA", 5001.0, 7500.0, 55.0),
                    FeeSchedule("EQUITEL_TO_MPESA", 7501.0, 20000.0, 65.0),
                    FeeSchedule("EQUITEL_TO_MPESA", 20001.0, 150000.0, 67.0),

                    // EQUITEL TO AIRTEL
                    FeeSchedule("EQUITEL_TO_AIRTEL", 1.0, 100.0, 0.0),
                    FeeSchedule("EQUITEL_TO_AIRTEL", 101.0, 500.0, 11.0),
                    FeeSchedule("EQUITEL_TO_AIRTEL", 501.0, 1000.0, 13.0),
                    FeeSchedule("EQUITEL_TO_AIRTEL", 1001.0, 1500.0, 20.0),
                    FeeSchedule("EQUITEL_TO_AIRTEL", 1501.0, 2500.0, 25.0),
                    FeeSchedule("EQUITEL_TO_AIRTEL", 2501.0, 3500.0, 35.0),
                    FeeSchedule("EQUITEL_TO_AIRTEL", 3501.0, 5000.0, 45.0),
                    FeeSchedule("EQUITEL_TO_AIRTEL", 5001.0, 7500.0, 55.0),
                    FeeSchedule("EQUITEL_TO_AIRTEL", 7501.0, 20000.0, 65.0),
                    FeeSchedule("EQUITEL_TO_AIRTEL", 20001.0, 150000.0, 65.0),

                    // BANK TO BANK (PESALINK)
                    FeeSchedule("BANK_TO_BANK_PESALINK", 0.0, 1000.0, 0.0),
                    FeeSchedule("BANK_TO_BANK_PESALINK", 1000.0, 100000.0, 50.0),
                    FeeSchedule("BANK_TO_BANK_PESALINK", 100001.0, 999000.0, 100.0),

                    // BANK TO BANK (RTGS)
                    FeeSchedule("BANK_TO_BANK_RTGS", 0.0, 999999999.0, 500.0),

                    // BANK TO BANK (SWIFT - Cross Border)
                    FeeSchedule("BANK_TO_BANK_SWIFT", 0.0, 999999999.0, 1000.0),

                    // EQUITY TO EQUITY (FREE)
                    FeeSchedule("EQUITY_TO_EQUITY", 0.0, 999999999.0, 0.0)
                )

                feeSchedules.forEach { schedule ->
                    // Check if already exists to avoid duplicate key error
                    val exists = TransactionFeeStructure.select {
                        (TransactionFeeStructure.transactionType eq schedule.type) and
                        (TransactionFeeStructure.minAmount eq BigDecimal(schedule.minAmount)) and
                        (TransactionFeeStructure.maxAmount eq BigDecimal(schedule.maxAmount))
                    }.count() > 0

                    if (!exists) {
                        TransactionFeeStructure.insert {
                            it[transactionType] = schedule.type
                            it[minAmount] = BigDecimal(schedule.minAmount)
                            it[maxAmount] = BigDecimal(schedule.maxAmount)
                            it[feeAmount] = BigDecimal(schedule.feeAmount)
                            it[TransactionFeeStructure.createdBy] = createdBy
                            it[description] = "Fee for ${schedule.type} transactions from KES ${schedule.minAmount} to ${schedule.maxAmount}"
                        }
                    }
                }

                ApiResponse(
                    success = true,
                    message = "Fee structures initialized successfully",
                    data = "${feeSchedules.size} fee structures created"
                )
            }
        } catch (e: Exception) {
            println("Error initializing fee structures: ${e.message}")
            e.printStackTrace()
            ApiResponse(
                success = false,
                message = "Failed to initialize fee structures: ${e.message}"
            )
        }
    }
}

// Helper data classes
data class FeeSchedule(
    val type: String,
    val minAmount: Double,
    val maxAmount: Double,
    val feeAmount: Double
)

@kotlinx.serialization.Serializable
data class TransactionFeeStructureDto(
    val id: String,
    val transactionType: String,
    val minAmount: Double,
    val maxAmount: Double,
    val feeAmount: Double,
    val feePercentage: Double? = null,
    val currency: String,
    val isActive: Boolean,
    val description: String? = null,
    val createdAt: String
)

@kotlinx.serialization.Serializable
data class ProfitSummaryDto(
    val totalFeesCollected: Double,
    val totalTransactions: Int,
    val feesByType: Map<String, Double>
)
