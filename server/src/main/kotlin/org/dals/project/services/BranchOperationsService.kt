package org.dals.project.services

import org.dals.project.database.*
import org.dals.project.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.util.UUID

class BranchOperationsService {

    // Get all operations for a branch
    fun getBranchOperations(branchId: UUID, status: String? = null): ApiResponse<List<Map<String, Any>>> {
        return transaction {
            try {
                var query = (BranchOperations innerJoin Users)
                    .select { BranchOperations.branchId eq branchId }

                // Filter by status if provided
                status?.let {
                    val operationStatus = OperationStatus.valueOf(it)
                    query = query.andWhere { BranchOperations.status eq operationStatus }
                }

                val operations = query
                    .orderBy(BranchOperations.createdAt to SortOrder.DESC)
                    .map { row ->
                        mapOf(
                            "id" to row[BranchOperations.id].toString(),
                            "type" to row[BranchOperations.type].toString(),
                            "title" to row[BranchOperations.title],
                            "description" to (row[BranchOperations.description] ?: ""),
                            "status" to row[BranchOperations.status].toString(),
                            "priority" to row[BranchOperations.priority].toString(),
                            "assignedTo" to (row[BranchOperations.assignedTo]?.toString() ?: ""),
                            "createdAt" to row[BranchOperations.createdAt].toString(),
                            "dueDate" to (row[BranchOperations.dueDate]?.toString() ?: ""),
                            "completedAt" to (row[BranchOperations.completedAt]?.toString() ?: "")
                        )
                    }

                ApiResponse(
                    success = true,
                    message = "Operations retrieved successfully",
                    data = operations
                )
            } catch (e: Exception) {
                ApiResponse(
                    success = false,
                    message = "Failed to retrieve operations: ${e.message}"
                )
            }
        }
    }

    // Create a new branch operation
    fun createBranchOperation(
        branchId: UUID,
        createdBy: UUID,
        type: String,
        title: String,
        description: String?,
        priority: String,
        assignedTo: UUID?,
        dueDate: LocalDate?
    ): ApiResponse<Map<String, Any>> {
        return transaction {
            try {
                val operationId = BranchOperations.insert {
                    it[BranchOperations.branchId] = branchId
                    it[BranchOperations.createdBy] = createdBy
                    it[BranchOperations.type] = OperationType.valueOf(type)
                    it[BranchOperations.title] = title
                    it[BranchOperations.description] = description
                    it[BranchOperations.priority] = Priority.valueOf(priority)
                    it[BranchOperations.assignedTo] = assignedTo
                    it[BranchOperations.dueDate] = dueDate
                } get BranchOperations.id

                val operation = BranchOperations.select { BranchOperations.id eq operationId }
                    .first()
                    .let { row ->
                        mapOf(
                            "id" to row[BranchOperations.id].toString(),
                            "type" to row[BranchOperations.type].toString(),
                            "title" to row[BranchOperations.title],
                            "description" to (row[BranchOperations.description] ?: ""),
                            "status" to row[BranchOperations.status].toString(),
                            "priority" to row[BranchOperations.priority].toString(),
                            "assignedTo" to (row[BranchOperations.assignedTo]?.toString() ?: ""),
                            "createdAt" to row[BranchOperations.createdAt].toString(),
                            "dueDate" to (row[BranchOperations.dueDate]?.toString() ?: "")
                        )
                    }

                ApiResponse(
                    success = true,
                    message = "Operation created successfully",
                    data = operation
                )
            } catch (e: Exception) {
                ApiResponse(
                    success = false,
                    message = "Failed to create operation: ${e.message}"
                )
            }
        }
    }

    // Get or create today's daily summary
    fun getDailySummary(branchId: UUID, date: LocalDate = LocalDate.now()): ApiResponse<Map<String, Any>> {
        return transaction {
            try {
                val existing = DailyOperationsSummary.select {
                    (DailyOperationsSummary.branchId eq branchId) and
                    (DailyOperationsSummary.date eq date)
                }.firstOrNull()

                val summary = if (existing != null) {
                    mapOf(
                        "date" to existing[DailyOperationsSummary.date].toString(),
                        "accountsOpened" to existing[DailyOperationsSummary.accountsOpened],
                        "loansProcessed" to existing[DailyOperationsSummary.loansProcessed],
                        "transactionsCompleted" to existing[DailyOperationsSummary.transactionsCompleted],
                        "customerInquiries" to existing[DailyOperationsSummary.customerInquiries],
                        "cashInHand" to existing[DailyOperationsSummary.cashInHand].toDouble(),
                        "totalDeposits" to existing[DailyOperationsSummary.totalDeposits].toDouble(),
                        "totalWithdrawals" to existing[DailyOperationsSummary.totalWithdrawals].toDouble()
                    )
                } else {
                    // Calculate from actual data
                    val today = date
                    val startOfDay = today.atStartOfDay()
                    val startInstant = startOfDay.atZone(java.time.ZoneId.systemDefault()).toInstant()

                    // Count accounts opened today
                    val accountsOpened = Accounts.select {
                        (Accounts.branchId eq branchId) and
                        (Accounts.createdAt greaterEq startInstant)
                    }.count().toInt()

                    // Count loans processed today
                    val loansProcessed = Loans.select {
                        (Loans.branchId eq branchId) and
                        (Loans.createdAt greaterEq startInstant)
                    }.count().toInt()

                    // Count transactions completed today
                    val transactionsCompleted = Transactions.select {
                        (Transactions.branchId eq branchId) and
                        (Transactions.timestamp greaterEq startInstant) and
                        (Transactions.status eq TransactionStatus.COMPLETED)
                    }.count().toInt()

                    // Calculate total deposits and withdrawals
                    val deposits = Transactions
                        .select {
                            (Transactions.branchId eq branchId) and
                            (Transactions.type eq TransactionType.DEPOSIT) and
                            (Transactions.status eq TransactionStatus.COMPLETED) and
                            (Transactions.timestamp greaterEq startInstant)
                        }
                        .sumOf { it[Transactions.amount].toDouble() }

                    val withdrawals = Transactions
                        .select {
                            (Transactions.branchId eq branchId) and
                            (Transactions.type eq TransactionType.WITHDRAWAL) and
                            (Transactions.status eq TransactionStatus.COMPLETED) and
                            (Transactions.timestamp greaterEq startInstant)
                        }
                        .sumOf { it[Transactions.amount].toDouble() }

                    // Get total branch deposits (cash in hand approximation)
                    val cashInHand = Accounts
                        .slice(Accounts.balance.sum())
                        .select {
                            (Accounts.branchId eq branchId) and
                            (Accounts.status eq AccountStatus.ACTIVE)
                        }
                        .firstOrNull()
                        ?.get(Accounts.balance.sum())
                        ?.toDouble() ?: 0.0

                    // Create the summary
                    DailyOperationsSummary.insert {
                        it[DailyOperationsSummary.branchId] = branchId
                        it[DailyOperationsSummary.date] = date
                        it[DailyOperationsSummary.accountsOpened] = accountsOpened
                        it[DailyOperationsSummary.loansProcessed] = loansProcessed
                        it[DailyOperationsSummary.transactionsCompleted] = transactionsCompleted
                        it[DailyOperationsSummary.customerInquiries] = 0
                        it[DailyOperationsSummary.cashInHand] = java.math.BigDecimal.valueOf(cashInHand)
                        it[DailyOperationsSummary.totalDeposits] = java.math.BigDecimal.valueOf(deposits)
                        it[DailyOperationsSummary.totalWithdrawals] = java.math.BigDecimal.valueOf(withdrawals)
                    }

                    mapOf(
                        "date" to date.toString(),
                        "accountsOpened" to accountsOpened,
                        "loansProcessed" to loansProcessed,
                        "transactionsCompleted" to transactionsCompleted,
                        "customerInquiries" to 0,
                        "cashInHand" to cashInHand,
                        "totalDeposits" to deposits,
                        "totalWithdrawals" to withdrawals
                    )
                }

                ApiResponse(
                    success = true,
                    message = "Daily summary retrieved successfully",
                    data = summary
                )
            } catch (e: Exception) {
                ApiResponse(
                    success = false,
                    message = "Failed to retrieve daily summary: ${e.message}"
                )
            }
        }
    }

    // Get performance metrics
    fun getPerformanceMetrics(branchId: UUID, date: LocalDate = LocalDate.now()): ApiResponse<List<Map<String, Any>>> {
        return transaction {
            try {
                val metrics = PerformanceMetrics.select {
                    (PerformanceMetrics.branchId eq branchId) and
                    (PerformanceMetrics.date eq date)
                }.map { row ->
                    mapOf(
                        "category" to row[PerformanceMetrics.category],
                        "value" to row[PerformanceMetrics.currentValue],
                        "target" to row[PerformanceMetrics.targetValue],
                        "percentage" to row[PerformanceMetrics.percentage],
                        "trend" to row[PerformanceMetrics.trend]
                    )
                }

                // If no metrics exist for today, create default ones
                if (metrics.isEmpty()) {
                    val defaultMetrics = listOf(
                        mapOf("category" to "Customer Satisfaction", "value" to "4.5/5.0", "target" to "4.5/5.0", "percentage" to 100, "trend" to "STABLE"),
                        mapOf("category" to "Transaction Speed", "value" to "3.0 min", "target" to "3.0 min", "percentage" to 100, "trend" to "STABLE"),
                        mapOf("category" to "Service Uptime", "value" to "99.5%", "target" to "99.5%", "percentage" to 100, "trend" to "UP")
                    )

                    defaultMetrics.forEach { metric ->
                        PerformanceMetrics.insert {
                            it[PerformanceMetrics.branchId] = branchId
                            it[PerformanceMetrics.date] = date
                            it[PerformanceMetrics.category] = metric["category"] as String
                            it[PerformanceMetrics.metricName] = metric["category"] as String
                            it[PerformanceMetrics.currentValue] = metric["value"] as String
                            it[PerformanceMetrics.targetValue] = metric["target"] as String
                            it[PerformanceMetrics.percentage] = metric["percentage"] as Int
                            it[PerformanceMetrics.trend] = metric["trend"] as String
                        }
                    }

                    return@transaction ApiResponse(
                        success = true,
                        message = "Performance metrics retrieved successfully",
                        data = defaultMetrics
                    )
                }

                ApiResponse(
                    success = true,
                    message = "Performance metrics retrieved successfully",
                    data = metrics
                )
            } catch (e: Exception) {
                ApiResponse(
                    success = false,
                    message = "Failed to retrieve performance metrics: ${e.message}"
                )
            }
        }
    }

    // Get staff productivity
    fun getStaffProductivity(branchId: UUID, date: LocalDate = LocalDate.now()): ApiResponse<List<Map<String, Any>>> {
        return transaction {
            try {
                val productivity = (StaffProductivity innerJoin Employees innerJoin Users)
                    .select {
                        (StaffProductivity.branchId eq branchId) and
                        (StaffProductivity.date eq date)
                    }
                    .map { row ->
                        mapOf(
                            "employeeId" to row[StaffProductivity.employeeId].toString(),
                            "employeeName" to "${row[Users.firstName]} ${row[Users.lastName]}",
                            "department" to row[Employees.department].toString(),
                            "tasksCompleted" to row[StaffProductivity.tasksCompleted],
                            "customersSolved" to row[StaffProductivity.customersServed],
                            "avgResponseTime" to "${row[StaffProductivity.avgResponseTime] ?: "0.0"} min",
                            "rating" to (row[StaffProductivity.rating]?.toDouble() ?: 0.0)
                        )
                    }

                ApiResponse(
                    success = true,
                    message = "Staff productivity retrieved successfully",
                    data = productivity
                )
            } catch (e: Exception) {
                ApiResponse(
                    success = false,
                    message = "Failed to retrieve staff productivity: ${e.message}"
                )
            }
        }
    }
}
