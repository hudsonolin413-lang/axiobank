package org.dals.project.services

import org.dals.project.database.*
import org.dals.project.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class AdminMasterWalletDashboardService {

    /**
     * Get master wallet dashboard overview
     */
    fun getMasterWalletDashboard(): ApiResponse<MasterWalletDashboardDto> {
        return try {
            transaction {
                // Get all master wallets with their balances
                val wallets = MasterWallets.selectAll().map { row ->
                    AdminWalletSummaryDto(
                        id = row[MasterWallets.id].toString(),
                        walletName = row[MasterWallets.walletName],
                        walletType = row[MasterWallets.walletType].name,
                        balance = row[MasterWallets.balance].toDouble(),
                        availableBalance = row[MasterWallets.availableBalance].toDouble(),
                        reserveBalance = row[MasterWallets.reserveBalance].toDouble(),
                        status = row[MasterWallets.status].name,
                        currency = row[MasterWallets.currency]
                    )
                }

                // Get company profit wallet specifically
                val profitWallet = MasterWallets.select {
                    MasterWallets.walletType eq MasterWalletType.TRANSACTION_FEES_PROFIT
                }.singleOrNull()

                val companyProfit = profitWallet?.let {
                    CompanyProfitDto(
                        profitWalletId = it[MasterWallets.id].toString(),
                        totalFeesCollected = it[MasterWallets.balance].toDouble(),
                        currency = it[MasterWallets.currency],
                        walletStatus = it[MasterWallets.status].name
                    )
                }

                // Calculate totals
                val totalBalance = wallets.sumOf { it.balance }
                val totalAvailable = wallets.sumOf { it.availableBalance }
                val totalReserve = wallets.sumOf { it.reserveBalance }

                // Recent transactions (last 24 hours)
                val last24Hours = java.time.Instant.now().minusSeconds(86400)
                val recentTransactions = MasterWalletTransactions.select {
                    MasterWalletTransactions.processedAt greater last24Hours
                }.count().toInt()

                // Pending reconciliations
                val pendingReconciliations = ReconciliationRecords.select {
                    ReconciliationRecords.status eq "PENDING"
                }.count().toInt()

                // Active security alerts
                val securityAlerts = WalletSecurityAlerts.select {
                    WalletSecurityAlerts.isResolved eq false
                }.count().toInt()

                // Active float allocations
                val activeAllocations = FloatAllocations.select {
                    FloatAllocations.status eq AllocationStatus.ACTIVE
                }.count().toInt()

                val dashboard = MasterWalletDashboardDto(
                    totalBalance = totalBalance,
                    totalAvailableBalance = totalAvailable,
                    totalReserveBalance = totalReserve,
                    walletsCount = wallets.size,
                    recentTransactionsCount = recentTransactions,
                    pendingReconciliations = pendingReconciliations,
                    securityAlertsCount = securityAlerts,
                    activeAllocations = activeAllocations,
                    wallets = wallets,
                    companyProfit = companyProfit
                )

                ApiResponse(success = true, message = "Master wallet dashboard retrieved", data = dashboard)
            }
        } catch (e: Exception) {
            println("Error retrieving master wallet dashboard: ${e.message}")
            e.printStackTrace()
            ApiResponse(success = false, message = "Error retrieving master wallet dashboard: ${e.message}")
        }
    }

    /**
     * Get all master wallets with details
     */
    fun getAllMasterWallets(): ListResponse<MasterWalletDetailDto> {
        return try {
            transaction {
                val wallets = MasterWallets.selectAll().map { row ->
                    val walletId = row[MasterWallets.id]

                    // Count transactions for this wallet
                    val transactionCount = MasterWalletTransactions.select {
                        MasterWalletTransactions.walletId eq walletId.value
                    }.count().toInt()

                    // Get last transaction time
                    val lastTransaction = MasterWalletTransactions.select {
                        MasterWalletTransactions.walletId eq walletId.value
                    }.orderBy(MasterWalletTransactions.processedAt to SortOrder.DESC)
                        .limit(1)
                        .singleOrNull()

                    MasterWalletDetailDto(
                        id = walletId.toString(),
                        walletName = row[MasterWallets.walletName],
                        walletType = row[MasterWallets.walletType].name,
                        currency = row[MasterWallets.currency],
                        balance = row[MasterWallets.balance].toDouble(),
                        availableBalance = row[MasterWallets.availableBalance].toDouble(),
                        reserveBalance = row[MasterWallets.reserveBalance].toDouble(),
                        securityLevel = row[MasterWallets.securityLevel].name,
                        status = row[MasterWallets.status].name,
                        maxSingleTransaction = row[MasterWallets.maxSingleTransaction].toDouble(),
                        dailyTransactionLimit = row[MasterWallets.dailyTransactionLimit].toDouble(),
                        monthlyTransactionLimit = row[MasterWallets.monthlyTransactionLimit].toDouble(),
                        transactionCount = transactionCount,
                        lastTransactionAt = lastTransaction?.get(MasterWalletTransactions.processedAt)?.toString(),
                        lastReconciliationDate = row[MasterWallets.lastReconciliationDate]?.toString(),
                        reconciliationStatus = row[MasterWallets.reconciliationStatus],
                        createdAt = row[MasterWallets.createdAt].toString()
                    )
                }

                ListResponse(
                    success = true,
                    message = "Master wallets retrieved successfully",
                    data = wallets,
                    total = wallets.size
                )
            }
        } catch (e: Exception) {
            println("Error retrieving master wallets: ${e.message}")
            e.printStackTrace()
            ListResponse(success = false, message = "Error retrieving master wallets: ${e.message}")
        }
    }

    /**
     * Get master wallet transactions
     */
    fun getMasterWalletTransactions(
        walletId: String? = null,
        limit: Int = 100
    ): ListResponse<AdminMasterWalletTransactionDto> {
        return try {
            transaction {
                var query = MasterWalletTransactions
                    .join(MasterWallets, JoinType.INNER, MasterWalletTransactions.walletId, MasterWallets.id)
                    .selectAll()

                // Filter by wallet if provided
                if (walletId != null) {
                    query = query.andWhere { MasterWalletTransactions.walletId eq UUID.fromString(walletId) }
                }

                val transactions = query
                    .orderBy(MasterWalletTransactions.processedAt to SortOrder.DESC)
                    .limit(limit)
                    .map { row ->
                        AdminMasterWalletTransactionDto(
                            id = row[MasterWalletTransactions.id].toString(),
                            walletId = row[MasterWalletTransactions.walletId].toString(),
                            walletName = row[MasterWallets.walletName],
                            transactionType = row[MasterWalletTransactions.transactionType].name,
                            amount = row[MasterWalletTransactions.amount].toDouble(),
                            currency = row[MasterWalletTransactions.currency],
                            balanceBefore = row[MasterWalletTransactions.balanceBefore].toDouble(),
                            balanceAfter = row[MasterWalletTransactions.balanceAfter].toDouble(),
                            description = row[MasterWalletTransactions.description],
                            reference = row[MasterWalletTransactions.reference],
                            status = row[MasterWalletTransactions.status].name,
                            riskScore = row[MasterWalletTransactions.riskScore]?.toDouble(),
                            riskLevel = row[MasterWalletTransactions.riskLevel],
                            processedAt = row[MasterWalletTransactions.processedAt].toString()
                        )
                    }

                ListResponse(
                    success = true,
                    message = "Master wallet transactions retrieved",
                    data = transactions,
                    total = transactions.size
                )
            }
        } catch (e: Exception) {
            println("Error retrieving master wallet transactions: ${e.message}")
            e.printStackTrace()
            ListResponse(success = false, message = "Error retrieving transactions: ${e.message}")
        }
    }

    /**
     * Get float allocations
     */
    fun getFloatAllocations(status: String? = null): ListResponse<AdminFloatAllocationDto> {
        return try {
            transaction {
                var query = FloatAllocations
                    .join(MasterWallets, JoinType.INNER, FloatAllocations.sourceWalletId, MasterWallets.id)
                    .selectAll()

                // Filter by status if provided
                if (status != null) {
                    query = query.andWhere { FloatAllocations.status eq AllocationStatus.valueOf(status) }
                }

                val allocations = query
                    .orderBy(FloatAllocations.createdAt to SortOrder.DESC)
                    .limit(100)
                    .map { row ->
                        AdminFloatAllocationDto(
                            id = row[FloatAllocations.id].toString(),
                            sourceWalletId = row[FloatAllocations.sourceWalletId].toString(),
                            sourceWalletName = row[MasterWallets.walletName],
                            targetWalletId = row[FloatAllocations.targetWalletId].toString(),
                            amount = row[FloatAllocations.amount].toDouble(),
                            actualUsage = row[FloatAllocations.actualUsage].toDouble(),
                            remainingAmount = row[FloatAllocations.remainingAmount].toDouble(),
                            purpose = row[FloatAllocations.purpose],
                            status = row[FloatAllocations.status].name,
                            expiryDate = row[FloatAllocations.expiryDate]?.toString(),
                            createdAt = row[FloatAllocations.createdAt].toString()
                        )
                    }

                ListResponse(
                    success = true,
                    message = "Float allocations retrieved",
                    data = allocations,
                    total = allocations.size
                )
            }
        } catch (e: Exception) {
            println("Error retrieving float allocations: ${e.message}")
            e.printStackTrace()
            ListResponse(success = false, message = "Error retrieving float allocations: ${e.message}")
        }
    }

    /**
     * Get wallet security alerts
     */
    fun getWalletSecurityAlerts(): ListResponse<AdminWalletSecurityAlertDto> {
        return try {
            transaction {
                val alerts = WalletSecurityAlerts
                    .leftJoin(MasterWallets, { WalletSecurityAlerts.walletId }, { MasterWallets.id })
                    .selectAll()
                    .orderBy(WalletSecurityAlerts.createdAt to SortOrder.DESC)
                    .limit(100)
                    .map { row ->
                        AdminWalletSecurityAlertDto(
                            id = row[WalletSecurityAlerts.id].toString(),
                            walletId = row[WalletSecurityAlerts.walletId]?.toString(),
                            walletName = row.getOrNull(MasterWallets.walletName),
                            alertType = row[WalletSecurityAlerts.alertType].name,
                            severity = row[WalletSecurityAlerts.severity].name,
                            title = row[WalletSecurityAlerts.title],
                            description = row[WalletSecurityAlerts.description],
                            isResolved = row[WalletSecurityAlerts.isResolved],
                            actionRequired = row[WalletSecurityAlerts.actionRequired],
                            riskScore = row[WalletSecurityAlerts.riskScore]?.toDouble(),
                            createdAt = row[WalletSecurityAlerts.createdAt].toString(),
                            resolvedAt = row[WalletSecurityAlerts.resolvedAt]?.toString()
                        )
                    }

                ListResponse(
                    success = true,
                    message = "Wallet security alerts retrieved",
                    data = alerts,
                    total = alerts.size
                )
            }
        } catch (e: Exception) {
            println("Error retrieving security alerts: ${e.message}")
            e.printStackTrace()
            ListResponse(success = false, message = "Error retrieving security alerts: ${e.message}")
        }
    }

    /**
     * Get reconciliation records
     */
    fun getReconciliationRecords(walletId: String? = null): ListResponse<AdminReconciliationRecordDto> {
        return try {
            transaction {
                var query = ReconciliationRecords
                    .join(MasterWallets, JoinType.INNER, ReconciliationRecords.walletId, MasterWallets.id)
                    .selectAll()

                if (walletId != null) {
                    query = query.andWhere { ReconciliationRecords.walletId eq UUID.fromString(walletId) }
                }

                val records = query
                    .orderBy(ReconciliationRecords.createdAt to SortOrder.DESC)
                    .limit(100)
                    .map { row ->
                        AdminReconciliationRecordDto(
                            id = row[ReconciliationRecords.id].toString(),
                            walletId = row[ReconciliationRecords.walletId].toString(),
                            walletName = row[MasterWallets.walletName],
                            reconciliationType = row[ReconciliationRecords.reconciliationType],
                            expectedBalance = row[ReconciliationRecords.expectedBalance].toDouble(),
                            actualBalance = row[ReconciliationRecords.actualBalance].toDouble(),
                            difference = row[ReconciliationRecords.difference].toDouble(),
                            status = row[ReconciliationRecords.status],
                            transactionCount = row[ReconciliationRecords.transactionCount],
                            totalDebits = row[ReconciliationRecords.totalDebits].toDouble(),
                            totalCredits = row[ReconciliationRecords.totalCredits].toDouble(),
                            periodStart = row[ReconciliationRecords.periodStart].toString(),
                            periodEnd = row[ReconciliationRecords.periodEnd].toString(),
                            createdAt = row[ReconciliationRecords.createdAt].toString()
                        )
                    }

                ListResponse(
                    success = true,
                    message = "Reconciliation records retrieved",
                    data = records,
                    total = records.size
                )
            }
        } catch (e: Exception) {
            println("Error retrieving reconciliation records: ${e.message}")
            e.printStackTrace()
            ListResponse(success = false, message = "Error retrieving reconciliation records: ${e.message}")
        }
    }
}

// DTOs - Only unique ones for Admin Dashboard
@kotlinx.serialization.Serializable
data class MasterWalletDashboardDto(
    val totalBalance: Double,
    val totalAvailableBalance: Double,
    val totalReserveBalance: Double,
    val walletsCount: Int,
    val recentTransactionsCount: Int,
    val pendingReconciliations: Int,
    val securityAlertsCount: Int,
    val activeAllocations: Int,
    val wallets: List<AdminWalletSummaryDto>,
    val companyProfit: CompanyProfitDto? = null
)

@kotlinx.serialization.Serializable
data class CompanyProfitDto(
    val profitWalletId: String,
    val totalFeesCollected: Double,
    val currency: String,
    val walletStatus: String
)

@kotlinx.serialization.Serializable
data class AdminWalletSummaryDto(
    val id: String,
    val walletName: String,
    val walletType: String,
    val balance: Double,
    val availableBalance: Double,
    val reserveBalance: Double,
    val status: String,
    val currency: String
)

@kotlinx.serialization.Serializable
data class MasterWalletDetailDto(
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
    val transactionCount: Int,
    val lastTransactionAt: String? = null,
    val lastReconciliationDate: String? = null,
    val reconciliationStatus: String,
    val createdAt: String
)

@kotlinx.serialization.Serializable
data class AdminMasterWalletTransactionDto(
    val id: String,
    val walletId: String,
    val walletName: String,
    val transactionType: String,
    val amount: Double,
    val currency: String,
    val balanceBefore: Double,
    val balanceAfter: Double,
    val description: String,
    val reference: String? = null,
    val status: String,
    val riskScore: Double? = null,
    val riskLevel: String,
    val processedAt: String
)

@kotlinx.serialization.Serializable
data class AdminFloatAllocationDto(
    val id: String,
    val sourceWalletId: String,
    val sourceWalletName: String,
    val targetWalletId: String,
    val amount: Double,
    val actualUsage: Double,
    val remainingAmount: Double,
    val purpose: String,
    val status: String,
    val expiryDate: String? = null,
    val createdAt: String
)

@kotlinx.serialization.Serializable
data class AdminWalletSecurityAlertDto(
    val id: String,
    val walletId: String? = null,
    val walletName: String? = null,
    val alertType: String,
    val severity: String,
    val title: String,
    val description: String,
    val isResolved: Boolean,
    val actionRequired: Boolean,
    val riskScore: Double? = null,
    val createdAt: String,
    val resolvedAt: String? = null
)

@kotlinx.serialization.Serializable
data class AdminReconciliationRecordDto(
    val id: String,
    val walletId: String,
    val walletName: String,
    val reconciliationType: String,
    val expectedBalance: Double,
    val actualBalance: Double,
    val difference: Double,
    val status: String,
    val transactionCount: Int,
    val totalDebits: Double,
    val totalCredits: Double,
    val periodStart: String,
    val periodEnd: String,
    val createdAt: String
)
