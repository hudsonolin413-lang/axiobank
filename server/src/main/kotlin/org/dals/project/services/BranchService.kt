package org.dals.project.services

import org.dals.project.database.*
import org.dals.project.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@kotlinx.serialization.Serializable
data class BranchStatistics(
    val branchId: String,
    val branchName: String,
    val totalStaff: Int,
    val totalCustomers: Int,
    val totalAccounts: Int,
    val totalDeposits: String,
    val totalLoans: String,
    val monthlyGrowth: String,
    val todayTransactions: TransactionsSummary,
    val recentActivities: List<ActivityItem>
)

@kotlinx.serialization.Serializable
data class TransactionsSummary(
    val deposits: TransactionTypeStats,
    val withdrawals: TransactionTypeStats,
    val transfers: TransactionTypeStats,
    val netChange: String
)

@kotlinx.serialization.Serializable
data class TransactionTypeStats(
    val count: Int,
    val amount: String
)

@kotlinx.serialization.Serializable
data class ActivityItem(
    val description: String,
    val timestamp: String,
    val icon: String
)

@kotlinx.serialization.Serializable
data class StaffPerformance(
    val employeeName: String,
    val role: String,
    val performanceScore: String,
    val transactionsProcessed: Int
)

class BranchService {

    fun getBranchStatistics(branchId: UUID): ApiResponse<BranchStatistics> {
        return transaction {
            try {
                // Get branch info
                val branch = Branches.select { Branches.id eq branchId }.firstOrNull()
                    ?: return@transaction ApiResponse(
                        success = false,
                        message = "Branch not found"
                    )

                // Get staff count
                val staffCount = Employees.select {
                    (Employees.branchId eq branchId) and
                    (Employees.employmentStatus eq EmploymentStatus.ACTIVE)
                }.count().toInt()

                // Get customers count
                val customersCount = Customers.select {
                    (Customers.branchId eq branchId) and
                    (Customers.status eq CustomerStatus.ACTIVE)
                }.count().toInt()

                // Get accounts count
                val accountsCount = Accounts.select {
                    (Accounts.branchId eq branchId) and
                    (Accounts.status eq AccountStatus.ACTIVE)
                }.count().toInt()

                // Get total deposits (sum of all active account balances)
                val totalDeposits = Accounts
                    .slice(Accounts.balance.sum())
                    .select {
                        (Accounts.branchId eq branchId) and
                        (Accounts.status eq AccountStatus.ACTIVE)
                    }
                    .firstOrNull()
                    ?.get(Accounts.balance.sum())
                    ?.toString() ?: "0.00"

                // Get total loans
                val totalLoans = Loans
                    .slice(Loans.currentBalance.sum())
                    .select {
                        (Loans.branchId eq branchId) and
                        (Loans.status eq LoanStatus.ACTIVE)
                    }
                    .firstOrNull()
                    ?.get(Loans.currentBalance.sum())
                    ?.toString() ?: "0.00"

                // Calculate monthly growth (simplified - comparing current vs last month)
                val monthlyGrowth = calculateMonthlyGrowth(branchId)

                // Get today's transactions
                val todayTransactions = getTodayTransactionsSummary(branchId)

                // Get recent activities
                val recentActivities = getRecentActivities(branchId)

                val statistics = BranchStatistics(
                    branchId = branchId.toString(),
                    branchName = branch[Branches.name],
                    totalStaff = staffCount,
                    totalCustomers = customersCount,
                    totalAccounts = accountsCount,
                    totalDeposits = totalDeposits,
                    totalLoans = totalLoans,
                    monthlyGrowth = monthlyGrowth,
                    todayTransactions = todayTransactions,
                    recentActivities = recentActivities
                )

                ApiResponse(
                    success = true,
                    message = "Branch statistics retrieved successfully",
                    data = statistics
                )
            } catch (e: Exception) {
                ApiResponse(
                    success = false,
                    message = "Failed to retrieve branch statistics: ${e.message}"
                )
            }
        }
    }

    private fun getTodayTransactionsSummary(branchId: UUID): TransactionsSummary {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay()
        val startInstant = startOfDay.atZone(java.time.ZoneId.systemDefault()).toInstant()

        // Get deposits
        val deposits = Transactions
            .select {
                (Transactions.branchId eq branchId) and
                (Transactions.type eq TransactionType.DEPOSIT) and
                (Transactions.status eq TransactionStatus.COMPLETED) and
                (Transactions.timestamp greaterEq startInstant)
            }
            .let { query ->
                val count = query.count().toInt()
                val sum = query.sumOf { it[Transactions.amount].toDouble() }
                TransactionTypeStats(count, String.format("%.2f", sum))
            }

        // Get withdrawals
        val withdrawals = Transactions
            .select {
                (Transactions.branchId eq branchId) and
                (Transactions.type eq TransactionType.WITHDRAWAL) and
                (Transactions.status eq TransactionStatus.COMPLETED) and
                (Transactions.timestamp greaterEq startInstant)
            }
            .let { query ->
                val count = query.count().toInt()
                val sum = query.sumOf { it[Transactions.amount].toDouble() }
                TransactionTypeStats(count, String.format("%.2f", sum))
            }

        // Get transfers
        val transfers = Transactions
            .select {
                (Transactions.branchId eq branchId) and
                (Transactions.type eq TransactionType.TRANSFER) and
                (Transactions.status eq TransactionStatus.COMPLETED) and
                (Transactions.timestamp greaterEq startInstant)
            }
            .let { query ->
                val count = query.count().toInt()
                val sum = query.sumOf { it[Transactions.amount].toDouble() }
                TransactionTypeStats(count, String.format("%.2f", sum))
            }

        val netChange = deposits.amount.toDouble() - withdrawals.amount.toDouble()

        return TransactionsSummary(
            deposits = deposits,
            withdrawals = withdrawals,
            transfers = transfers,
            netChange = String.format("%.2f", netChange)
        )
    }

    private fun getRecentActivities(branchId: UUID): List<ActivityItem> {
        val activities = mutableListOf<ActivityItem>()

        // Get recent accounts opened
        Accounts
            .select { Accounts.branchId eq branchId }
            .orderBy(Accounts.createdAt to SortOrder.DESC)
            .limit(2)
            .forEach { account ->
                activities.add(
                    ActivityItem(
                        description = "New customer account opened (${account[Accounts.accountNumber]})",
                        timestamp = formatTimestamp(account[Accounts.createdAt]),
                        icon = "AccountCircle"
                    )
                )
            }

        // Get recent loan approvals
        Loans
            .select { (Loans.branchId eq branchId) and (Loans.status eq LoanStatus.ACTIVE) }
            .orderBy(Loans.createdAt to SortOrder.DESC)
            .limit(2)
            .forEach { loan ->
                activities.add(
                    ActivityItem(
                        description = "Loan approved ($${loan[Loans.originalAmount]})",
                        timestamp = formatTimestamp(loan[Loans.createdAt]),
                        icon = "CheckCircle"
                    )
                )
            }

        return activities.sortedByDescending { it.timestamp }.take(5)
    }

    private fun calculateMonthlyGrowth(branchId: UUID): String {
        // Simplified calculation - compare active accounts this month vs last month
        val thisMonth = LocalDate.now().withDayOfMonth(1)
        val lastMonth = thisMonth.minusMonths(1)

        val thisMonthCount = Accounts.select {
            (Accounts.branchId eq branchId) and
            (Accounts.openedDate greaterEq thisMonth)
        }.count()

        val lastMonthCount = Accounts.select {
            (Accounts.branchId eq branchId) and
            (Accounts.openedDate greaterEq lastMonth) and
            (Accounts.openedDate less thisMonth)
        }.count()

        if (lastMonthCount == 0L) return "+0%"

        val growth = ((thisMonthCount - lastMonthCount).toDouble() / lastMonthCount.toDouble()) * 100
        return String.format("%+.1f%%", growth)
    }

    private fun formatTimestamp(timestamp: java.time.Instant): String {
        val now = java.time.Instant.now()
        val diffMinutes = java.time.Duration.between(timestamp, now).toMinutes()

        return when {
            diffMinutes < 1 -> "Just now"
            diffMinutes < 60 -> "$diffMinutes minutes ago"
            diffMinutes < 120 -> "1 hour ago"
            diffMinutes < 1440 -> "${diffMinutes / 60} hours ago"
            else -> "${diffMinutes / 1440} days ago"
        }
    }

    fun getStaffPerformance(branchId: UUID): ApiResponse<List<StaffPerformance>> {
        return transaction {
            try {
                val staffPerformance = (Users innerJoin Employees)
                    .select {
                        (Employees.branchId eq branchId) and
                        (Employees.employmentStatus eq EmploymentStatus.ACTIVE)
                    }
                    .map { row ->
                        val employeeId = row[Employees.id]

                        // Count transactions processed by this employee (using employee record ID)
                        // Note: Skip this for now as processedBy may reference different entities
                        val transactionsProcessed = 0

                        val performanceScore = row[Employees.performanceRating]?.toDouble() ?: 0.85
                        val scorePercent = (performanceScore * 100).toInt()

                        StaffPerformance(
                            employeeName = "${row[Users.firstName]} ${row[Users.lastName]}",
                            role = row[Employees.position],
                            performanceScore = "$scorePercent%",
                            transactionsProcessed = transactionsProcessed
                        )
                    }
                    .sortedByDescending { it.performanceScore }
                    .take(10)

                ApiResponse(
                    success = true,
                    message = "Staff performance retrieved successfully",
                    data = staffPerformance
                )
            } catch (e: Exception) {
                ApiResponse(
                    success = false,
                    message = "Failed to retrieve staff performance: ${e.message}"
                )
            }
        }
    }
}
