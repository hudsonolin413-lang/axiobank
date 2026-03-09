package org.dals.project.model

import kotlinx.serialization.Serializable

@Serializable
data class CashFlowSummary(
    val totalIncome: Double,
    val totalExpenses: Double,
    val netCashFlow: Double,
    val period: String,
    val categories: List<CategoryFlow>
)

@Serializable
data class CategoryFlow(
    val category: TransactionCategory,
    val inflow: Double,
    val outflow: Double,
    val netFlow: Double,
    val transactionCount: Int
)

@Serializable
data class MonthlyTrend(
    val month: String,
    val income: Double,
    val expenses: Double,
    val netFlow: Double
)

@Serializable
data class CashFlowInsight(
    val type: InsightType,
    val title: String,
    val message: String,
    val severity: InsightSeverity
)

@Serializable
enum class InsightType {
    SPENDING_PATTERN,
    INCOME_TREND,
    SAVING_OPPORTUNITY,
    BUDGET_WARNING,
    POSITIVE_TREND
}

@Serializable
enum class InsightSeverity {
    INFO,
    WARNING,
    CRITICAL,
    POSITIVE
}

@Serializable
data class PeriodComparison(
    val currentPeriod: PeriodData,
    val previousPeriod: PeriodData,
    val percentageChange: Double,
    val trend: TrendDirection
)

@Serializable
data class PeriodData(
    val startDate: String,
    val endDate: String,
    val income: Double,
    val expenses: Double,
    val netFlow: Double,
    val transactionCount: Int
)

@Serializable
enum class TrendDirection {
    UP,
    DOWN,
    STABLE
}
