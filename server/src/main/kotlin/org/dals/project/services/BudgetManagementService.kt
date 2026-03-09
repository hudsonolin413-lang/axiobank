package org.dals.project.services

import org.dals.project.database.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class CreateBudgetRequest(
    val customerId: String,
    val accountId: String? = null,
    val name: String,
    val category: String,
    val budgetType: String = "MONTHLY",
    val amount: Double,
    val alertThreshold: Double = 80.0,
    val startDate: String,
    val endDate: String,
    val rollover: Boolean = false
)

data class BudgetDto(
    val id: String,
    val customerId: String,
    val accountId: String?,
    val name: String,
    val category: String,
    val budgetType: String,
    val amount: Double,
    val spent: Double,
    val remaining: Double,
    val percentage: Double,
    val alertThreshold: Double,
    val alertSent: Boolean,
    val status: String,
    val startDate: String,
    val endDate: String,
    val rollover: Boolean,
    val createdAt: String
)

data class BudgetSummaryDto(
    val totalBudgeted: Double,
    val totalSpent: Double,
    val totalRemaining: Double,
    val overBudgetCategories: Int,
    val budgets: List<BudgetDto>
)

object BudgetManagementService {

    fun createBudget(request: CreateBudgetRequest): Result<BudgetDto> {
        return try {
            transaction {
                val startDate = LocalDate.parse(request.startDate)
                val budgetId = Budgets.insert {
                    it[customerId] = UUID.fromString(request.customerId)
                    it[accountId] = request.accountId?.let { id -> UUID.fromString(id) }
                    it[name] = request.name
                    it[category] = request.category
                    it[budgetType] = request.budgetType
                    it[amount] = BigDecimal.valueOf(request.amount)
                    it[monthlyLimit] = BigDecimal.valueOf(request.amount)
                    it[spent] = BigDecimal.ZERO
                    it[Budgets.remaining] = BigDecimal.valueOf(request.amount)
                    it[percentage] = BigDecimal.ZERO
                    it[alertThreshold] = request.alertThreshold.toInt()
                    it[month] = startDate.monthValue
                    it[year] = startDate.year
                    it[Budgets.startDate] = startDate
                    it[endDate] = LocalDate.parse(request.endDate)
                    it[rollover] = request.rollover
                }[Budgets.id].value

                val budget = Budgets.select { Budgets.id eq budgetId }.single()
                Result.success(mapToBudgetDto(budget))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getBudgets(customerId: String): Result<List<BudgetDto>> {
        return try {
            transaction {
                val budgets = Budgets.select { Budgets.customerId eq UUID.fromString(customerId) }
                    .map { mapToBudgetDto(it) }
                Result.success(budgets)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getBudgetSummary(customerId: String): Result<BudgetSummaryDto> {
        return try {
            transaction {
                val budgets = Budgets.select { Budgets.customerId eq UUID.fromString(customerId) }
                    .map { mapToBudgetDto(it) }

                val totalBudgeted = budgets.sumOf { it.amount }
                val totalSpent = budgets.sumOf { it.spent }
                val totalRemaining = budgets.sumOf { it.remaining }
                val overBudget = budgets.count { it.status == "EXCEEDED" }

                Result.success(BudgetSummaryDto(totalBudgeted, totalSpent, totalRemaining, overBudget, budgets))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun updateBudgetSpending(budgetId: String, transactionId: String, amount: Double, category: String): Result<BudgetDto> {
        return try {
            transaction {
                val budget = Budgets.select { Budgets.id eq UUID.fromString(budgetId) }.singleOrNull()
                    ?: return@transaction Result.failure(Exception("Budget not found"))

                val currentSpent = budget[Budgets.spent].toDouble()
                val budgetAmount = (budget[Budgets.amount] ?: budget[Budgets.monthlyLimit]).toDouble()
                val newSpent = currentSpent + amount
                val newRemaining = budgetAmount - newSpent
                val newPercentage = (newSpent / budgetAmount) * 100

                val newStatus = when {
                    newPercentage >= 100 -> "EXCEEDED"
                    newPercentage >= budget[Budgets.alertThreshold].toDouble() -> "ACTIVE"
                    else -> "ACTIVE"
                }

                Budgets.update({ Budgets.id eq UUID.fromString(budgetId) }) {
                    it[spent] = BigDecimal.valueOf(newSpent)
                    it[remaining] = BigDecimal.valueOf(newRemaining)
                    it[percentage] = BigDecimal.valueOf(newPercentage)
                    it[status] = newStatus
                    it[updatedAt] = java.time.Instant.now()
                }

                BudgetTransactions.insert {
                    it[BudgetTransactions.budgetId] = UUID.fromString(budgetId)
                    it[BudgetTransactions.transactionId] = UUID.fromString(transactionId)
                    it[BudgetTransactions.amount] = BigDecimal(amount)
                    it[BudgetTransactions.category] = category
                }

                val updated = Budgets.select { Budgets.id eq UUID.fromString(budgetId) }.single()
                Result.success(mapToBudgetDto(updated))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteBudget(budgetId: String): Result<Unit> {
        return try {
            transaction {
                BudgetTransactions.deleteWhere { BudgetTransactions.budgetId eq UUID.fromString(budgetId) }
                Budgets.deleteWhere { Budgets.id eq UUID.fromString(budgetId) }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapToBudgetDto(row: ResultRow) = BudgetDto(
        id = row[Budgets.id].value.toString(),
        customerId = row[Budgets.customerId].toString(),
        accountId = row[Budgets.accountId]?.toString(),
        name = row[Budgets.name] ?: "Unnamed Budget",
        category = row[Budgets.category],
        budgetType = row[Budgets.budgetType],
        amount = (row[Budgets.amount] ?: row[Budgets.monthlyLimit]).toDouble(),
        spent = row[Budgets.spent].toDouble(),
        remaining = (row[Budgets.remaining] ?: BigDecimal.ZERO).toDouble(),
        percentage = row[Budgets.percentage].toDouble(),
        alertThreshold = row[Budgets.alertThreshold].toDouble(),
        alertSent = row[Budgets.alertSent],
        status = row[Budgets.status],
        startDate = row[Budgets.startDate].toString(),
        endDate = row[Budgets.endDate].toString(),
        rollover = row[Budgets.rollover],
        createdAt = row[Budgets.createdAt].toString()
    )
}
