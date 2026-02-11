package org.dals.project.services

import org.dals.project.database.*
import org.dals.project.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AdminReportsService {

    /**
     * Generate a report based on type and date range
     */
    fun generateReport(reportType: String, startDate: String?, endDate: String?): ApiResponse<ReportGenerationDto> {
        return try {
            val reportId = "REPORT_${reportType}_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}"
            val start = startDate ?: LocalDate.now().minusDays(30).toString()
            val end = endDate ?: LocalDate.now().toString()

            val reportData = when (reportType) {
                "USER_ACTIVITY" -> generateUserActivityReport(start, end)
                "EMPLOYEE_PERFORMANCE" -> generateEmployeePerformanceReport(start, end)
                "TRANSACTION_SUMMARY" -> generateTransactionSummaryReport(start, end)
                "AUDIT_LOG" -> generateAuditLogReport(start, end)
                "SYSTEM_HEALTH" -> generateSystemHealthReport()
                "SECURITY_REPORT" -> generateSecurityReport(start, end)
                else -> "Unknown report type"
            }

            val report = ReportGenerationDto(
                reportId = reportId,
                reportType = reportType,
                generatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                startDate = start,
                endDate = end,
                status = "COMPLETED",
                format = "PDF",
                downloadUrl = "/api/v1/admin/reports/download/$reportId",
                summary = reportData
            )

            ApiResponse(success = true, message = "Report generated successfully", data = report)
        } catch (e: Exception) {
            println("Error generating report: ${e.message}")
            e.printStackTrace()
            ApiResponse(success = false, message = "Error generating report: ${e.message}")
        }
    }

    private fun generateUserActivityReport(startDate: String, endDate: String): String {
        return transaction {
            val start = LocalDate.parse(startDate)
            val end = LocalDate.parse(endDate)
            val startInstant = start.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endInstant = end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            val activeSessions = UserSessions.select {
                (UserSessions.loginTime greater startInstant) and (UserSessions.loginTime less endInstant)
            }.count()

            val activeUsers = UserSessions.select {
                (UserSessions.loginTime greater startInstant) and (UserSessions.loginTime less endInstant)
            }.map { it[UserSessions.userId] }.distinct().count()

            "User Activity Report ($startDate to $endDate): $activeUsers active users with $activeSessions sessions"
        }
    }

    private fun generateEmployeePerformanceReport(startDate: String, endDate: String): String {
        return transaction {
            val employeeCount = Employees.select { Employees.employmentStatus eq EmploymentStatus.ACTIVE }.count()
            "Employee Performance Report ($startDate to $endDate): $employeeCount active employees analyzed"
        }
    }

    private fun generateTransactionSummaryReport(startDate: String, endDate: String): String {
        return transaction {
            val start = LocalDate.parse(startDate)
            val end = LocalDate.parse(endDate)
            val startInstant = start.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endInstant = end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            val transactionCount = Transactions.select {
                (Transactions.timestamp greater startInstant) and (Transactions.timestamp less endInstant)
            }.count()

            val totalVolume = Transactions.select {
                (Transactions.timestamp greater startInstant) and
                (Transactions.timestamp less endInstant) and
                (Transactions.status eq TransactionStatus.COMPLETED)
            }.sumOf { it[Transactions.amount] }

            "Transaction Summary ($startDate to $endDate): $transactionCount transactions with total volume $${totalVolume}"
        }
    }

    private fun generateAuditLogReport(startDate: String, endDate: String): String {
        return transaction {
            val start = LocalDate.parse(startDate)
            val end = LocalDate.parse(endDate)
            val startInstant = start.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endInstant = end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            val auditCount = AuditLogs.select {
                (AuditLogs.timestamp greater startInstant) and (AuditLogs.timestamp less endInstant)
            }.count()

            "Audit Log Report ($startDate to $endDate): $auditCount audit events recorded"
        }
    }

    private fun generateSystemHealthReport(): String {
        return transaction {
            val totalUsers = Users.selectAll().count()
            val activeAccounts = Accounts.select { Accounts.status eq AccountStatus.ACTIVE }.count()
            val pendingTransactions = Transactions.select { Transactions.status eq TransactionStatus.PENDING }.count()

            "System Health Report: $totalUsers users, $activeAccounts active accounts, $pendingTransactions pending transactions"
        }
    }

    private fun generateSecurityReport(startDate: String, endDate: String): String {
        return transaction {
            val start = LocalDate.parse(startDate)
            val end = LocalDate.parse(endDate)
            val startInstant = start.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endInstant = end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            val securityAlerts = WalletSecurityAlerts.select {
                (WalletSecurityAlerts.createdAt greater startInstant) and (WalletSecurityAlerts.createdAt less endInstant)
            }.count()

            val unresolvedAlerts = WalletSecurityAlerts.select {
                (WalletSecurityAlerts.createdAt greater startInstant) and
                (WalletSecurityAlerts.createdAt less endInstant) and
                (WalletSecurityAlerts.isResolved eq false)
            }.count()

            "Security Report ($startDate to $endDate): $securityAlerts total alerts, $unresolvedAlerts unresolved"
        }
    }

    /**
     * Get financial summary report
     */
    fun getFinancialSummary(startDate: String?, endDate: String?): ApiResponse<FinancialSummaryDto> {
        return try {
            transaction {
                val start = startDate?.let { LocalDate.parse(it) } ?: LocalDate.now().minusDays(30)
                val end = endDate?.let { LocalDate.parse(it) } ?: LocalDate.now()

                val startInstant = start.atStartOfDay(ZoneId.systemDefault()).toInstant()
                val endInstant = end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

                // Total deposits
                val totalDeposits = Transactions.select {
                    (Transactions.type eq TransactionType.DEPOSIT) and
                            (Transactions.status eq TransactionStatus.COMPLETED) and
                            (Transactions.timestamp greater startInstant) and
                            (Transactions.timestamp less endInstant)
                }.sumOf { it[Transactions.amount] }

                // Total withdrawals
                val totalWithdrawals = Transactions.select {
                    (Transactions.type eq TransactionType.WITHDRAWAL) and
                            (Transactions.status eq TransactionStatus.COMPLETED) and
                            (Transactions.timestamp greater startInstant) and
                            (Transactions.timestamp less endInstant)
                }.sumOf { it[Transactions.amount] }

                // Total transfers
                val totalTransfers = Transactions.select {
                    (Transactions.type eq TransactionType.TRANSFER) and
                            (Transactions.status eq TransactionStatus.COMPLETED) and
                            (Transactions.timestamp greater startInstant) and
                            (Transactions.timestamp less endInstant)
                }.sumOf { it[Transactions.amount] }

                // Total accounts balance
                val totalBalance = Accounts.select {
                    Accounts.status eq AccountStatus.ACTIVE
                }.sumOf { it[Accounts.balance] }

                // Total loans disbursed
                val totalLoansDisbursed = Loans.select {
                    (Loans.status eq LoanStatus.ACTIVE) and
                            (Loans.originationDate greaterEq start) and
                            (Loans.originationDate lessEq end)
                }.sumOf { it[Loans.originalAmount] }

                // Total loan repayments
                val totalLoanRepayments = LoanPayments.select {
                    (LoanPayments.paymentDate greaterEq start) and
                            (LoanPayments.paymentDate lessEq end)
                }.sumOf { it[LoanPayments.amount] }

                val summary = FinancialSummaryDto(
                    totalDeposits = totalDeposits.toDouble(),
                    totalWithdrawals = totalWithdrawals.toDouble(),
                    totalTransfers = totalTransfers.toDouble(),
                    totalBalance = totalBalance.toDouble(),
                    totalLoansDisbursed = totalLoansDisbursed.toDouble(),
                    totalLoanRepayments = totalLoanRepayments.toDouble(),
                    netCashFlow = (totalDeposits - totalWithdrawals).toDouble(),
                    period = "$start to $end"
                )

                ApiResponse(success = true, message = "Financial summary retrieved", data = summary)
            }
        } catch (e: Exception) {
            println("Error retrieving financial summary: ${e.message}")
            e.printStackTrace()
            ApiResponse(success = false, message = "Error retrieving financial summary: ${e.message}")
        }
    }

    /**
     * Get transaction analytics
     */
    fun getTransactionAnalytics(days: Int = 30): ApiResponse<TransactionAnalyticsDto> {
        return try {
            transaction {
                val startDate = LocalDate.now().minusDays(days.toLong())
                val startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()

                // Total transactions
                val totalTransactions = Transactions.select {
                    Transactions.timestamp greater startInstant
                }.count().toInt()

                // Successful transactions
                val successfulTransactions = Transactions.select {
                    (Transactions.timestamp greater startInstant) and
                            (Transactions.status eq TransactionStatus.COMPLETED)
                }.count().toInt()

                // Failed transactions
                val failedTransactions = Transactions.select {
                    (Transactions.timestamp greater startInstant) and
                            (Transactions.status eq TransactionStatus.FAILED)
                }.count().toInt()

                // Average transaction value
                val avgTransactionValue = Transactions.select {
                    (Transactions.timestamp greater startInstant) and
                            (Transactions.status eq TransactionStatus.COMPLETED)
                }.map { it[Transactions.amount].toDouble() }.average()

                // Transaction by type
                val transactionsByType = TransactionType.values().associate { type ->
                    type.name to Transactions.select {
                        (Transactions.type eq type) and
                                (Transactions.timestamp greater startInstant)
                    }.count().toInt()
                }

                // Daily transaction trend (last 7 days)
                val dailyTrend = (0..6).map { daysAgo ->
                    val date = LocalDate.now().minusDays(daysAgo.toLong())
                    val dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
                    val dayEnd = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

                    val count = Transactions.select {
                        (Transactions.timestamp greater dayStart) and
                                (Transactions.timestamp less dayEnd)
                    }.count().toInt()

                    DailyTransactionDto(
                        date = date.toString(),
                        count = count
                    )
                }.reversed()

                val analytics = TransactionAnalyticsDto(
                    totalTransactions = totalTransactions,
                    successfulTransactions = successfulTransactions,
                    failedTransactions = failedTransactions,
                    successRate = if (totalTransactions > 0)
                        (successfulTransactions.toDouble() / totalTransactions * 100) else 0.0,
                    averageTransactionValue = avgTransactionValue,
                    transactionsByType = transactionsByType,
                    dailyTrend = dailyTrend,
                    period = "$days days"
                )

                ApiResponse(success = true, message = "Transaction analytics retrieved", data = analytics)
            }
        } catch (e: Exception) {
            println("Error retrieving transaction analytics: ${e.message}")
            e.printStackTrace()
            ApiResponse(success = false, message = "Error retrieving transaction analytics: ${e.message}")
        }
    }

    /**
     * Get customer analytics
     */
    fun getCustomerAnalytics(): ApiResponse<CustomerAnalyticsDto> {
        return try {
            transaction {
                // Total customers
                val totalCustomers = Customers.selectAll().count().toInt()

                // Active customers
                val activeCustomers = Customers.select {
                    Customers.status eq CustomerStatus.ACTIVE
                }.count().toInt()

                // New customers (last 30 days)
                val thirtyDaysAgo = LocalDate.now().minusDays(30)
                val newCustomers = Customers.select {
                    Customers.onboardedDate greater thirtyDaysAgo
                }.count().toInt()

                // Customers by type
                val customersByType = CustomerType.values().associate { type ->
                    type.name to Customers.select {
                        Customers.type eq type
                    }.count().toInt()
                }

                // Customers by KYC status
                val customersByKycStatus = listOf("PENDING", "VERIFIED", "REJECTED", "EXPIRED").associate { status ->
                    status to Customers.select {
                        Customers.kycStatus eq status
                    }.count().toInt()
                }

                // Customers by risk level
                val customersByRiskLevel = listOf("LOW", "MEDIUM", "HIGH", "CRITICAL").associate { risk ->
                    risk to Customers.select {
                        Customers.riskLevel eq risk
                    }.count().toInt()
                }

                // Top 10 branches by customer count
                val topBranches = Branches
                    .selectAll()
                    .orderBy(Branches.totalCustomers to SortOrder.DESC)
                    .limit(10)
                    .map { row ->
                        BranchCustomerDto(
                            branchId = row[Branches.id].toString(),
                            branchName = row[Branches.name],
                            customerCount = row[Branches.totalCustomers]
                        )
                    }

                val analytics = CustomerAnalyticsDto(
                    totalCustomers = totalCustomers,
                    activeCustomers = activeCustomers,
                    inactiveCustomers = totalCustomers - activeCustomers,
                    newCustomers = newCustomers,
                    customersByType = customersByType,
                    customersByKycStatus = customersByKycStatus,
                    customersByRiskLevel = customersByRiskLevel,
                    topBranches = topBranches
                )

                ApiResponse(success = true, message = "Customer analytics retrieved", data = analytics)
            }
        } catch (e: Exception) {
            println("Error retrieving customer analytics: ${e.message}")
            e.printStackTrace()
            ApiResponse(success = false, message = "Error retrieving customer analytics: ${e.message}")
        }
    }

    /**
     * Get loan portfolio report
     */
    fun getLoanPortfolioReport(): ApiResponse<LoanPortfolioDto> {
        return try {
            transaction {
                // Total loans
                val totalLoans = Loans.selectAll().count().toInt()

                // Active loans
                val activeLoans = Loans.select {
                    Loans.status eq LoanStatus.ACTIVE
                }.count().toInt()

                // Total loan amount outstanding
                val totalOutstanding = Loans.select {
                    Loans.status eq LoanStatus.ACTIVE
                }.sumOf { it[Loans.currentBalance] }

                // Total loan amount disbursed
                val totalDisbursed = Loans.selectAll().sumOf { it[Loans.originalAmount] }

                // Loans by type
                val loansByType = LoanType.values().associate { type ->
                    type.name to Loans.select {
                        Loans.loanType eq type
                    }.count().toInt()
                }

                // Loans by status
                val loansByStatus = LoanStatus.values().associate { status ->
                    status.name to Loans.select {
                        Loans.status eq status
                    }.count().toInt()
                }

                // Overdue loans (payment date in past)
                val overdueLoans = Loans.select {
                    (Loans.status eq LoanStatus.ACTIVE) and
                            (Loans.nextPaymentDate less LocalDate.now())
                }.count().toInt()

                // Average loan amount
                val avgLoanAmount = if (totalLoans > 0) {
                    totalDisbursed.toDouble() / totalLoans
                } else 0.0

                val report = LoanPortfolioDto(
                    totalLoans = totalLoans,
                    activeLoans = activeLoans,
                    totalOutstanding = totalOutstanding.toDouble(),
                    totalDisbursed = totalDisbursed.toDouble(),
                    averageLoanAmount = avgLoanAmount,
                    overdueLoans = overdueLoans,
                    loansByType = loansByType,
                    loansByStatus = loansByStatus,
                    portfolioHealthScore = if (totalLoans > 0)
                        ((activeLoans.toDouble() / totalLoans) * 100) else 100.0
                )

                ApiResponse(success = true, message = "Loan portfolio report retrieved", data = report)
            }
        } catch (e: Exception) {
            println("Error retrieving loan portfolio: ${e.message}")
            e.printStackTrace()
            ApiResponse(success = false, message = "Error retrieving loan portfolio: ${e.message}")
        }
    }

    /**
     * Get branch performance report
     */
    fun getBranchPerformanceReport(): ListResponse<BranchPerformanceDto> {
        return try {
            transaction {
                val branches = Branches.selectAll().map { row ->
                    val branchId = row[Branches.id]

                    // Count transactions for this branch
                    val transactionCount = Transactions.select {
                        Transactions.branchId eq branchId.value
                    }.count().toInt()

                    // Count loans for this branch
                    val loanCount = Loans.select {
                        Loans.branchId eq branchId.value
                    }.count().toInt()

                    BranchPerformanceDto(
                        branchId = branchId.toString(),
                        branchName = row[Branches.name],
                        branchCode = row[Branches.branchCode],
                        totalCustomers = row[Branches.totalCustomers],
                        totalAccounts = row[Branches.totalAccounts],
                        totalDeposits = row[Branches.totalDeposits].toDouble(),
                        totalLoans = row[Branches.totalLoans].toDouble(),
                        transactionCount = transactionCount,
                        loanCount = loanCount,
                        status = row[Branches.status]
                    )
                }

                ListResponse(
                    success = true,
                    message = "Branch performance report retrieved",
                    data = branches,
                    total = branches.size
                )
            }
        } catch (e: Exception) {
            println("Error retrieving branch performance: ${e.message}")
            e.printStackTrace()
            ListResponse(success = false, message = "Error retrieving branch performance: ${e.message}")
        }
    }
}

// DTOs
@kotlinx.serialization.Serializable
data class FinancialSummaryDto(
    val totalDeposits: Double,
    val totalWithdrawals: Double,
    val totalTransfers: Double,
    val totalBalance: Double,
    val totalLoansDisbursed: Double,
    val totalLoanRepayments: Double,
    val netCashFlow: Double,
    val period: String
)

@kotlinx.serialization.Serializable
data class TransactionAnalyticsDto(
    val totalTransactions: Int,
    val successfulTransactions: Int,
    val failedTransactions: Int,
    val successRate: Double,
    val averageTransactionValue: Double,
    val transactionsByType: Map<String, Int>,
    val dailyTrend: List<DailyTransactionDto>,
    val period: String
)

@kotlinx.serialization.Serializable
data class DailyTransactionDto(
    val date: String,
    val count: Int
)

@kotlinx.serialization.Serializable
data class CustomerAnalyticsDto(
    val totalCustomers: Int,
    val activeCustomers: Int,
    val inactiveCustomers: Int,
    val newCustomers: Int,
    val customersByType: Map<String, Int>,
    val customersByKycStatus: Map<String, Int>,
    val customersByRiskLevel: Map<String, Int>,
    val topBranches: List<BranchCustomerDto>
)

@kotlinx.serialization.Serializable
data class BranchCustomerDto(
    val branchId: String,
    val branchName: String,
    val customerCount: Int
)

@kotlinx.serialization.Serializable
data class LoanPortfolioDto(
    val totalLoans: Int,
    val activeLoans: Int,
    val totalOutstanding: Double,
    val totalDisbursed: Double,
    val averageLoanAmount: Double,
    val overdueLoans: Int,
    val loansByType: Map<String, Int>,
    val loansByStatus: Map<String, Int>,
    val portfolioHealthScore: Double
)

@kotlinx.serialization.Serializable
data class BranchPerformanceDto(
    val branchId: String,
    val branchName: String,
    val branchCode: String,
    val totalCustomers: Int,
    val totalAccounts: Int,
    val totalDeposits: Double,
    val totalLoans: Double,
    val transactionCount: Int,
    val loanCount: Int,
    val status: String
)

@kotlinx.serialization.Serializable
data class ReportGenerationDto(
    val reportId: String,
    val reportType: String,
    val generatedAt: String,
    val startDate: String,
    val endDate: String,
    val status: String,
    val format: String,
    val downloadUrl: String,
    val summary: String
)
