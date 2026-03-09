package org.dals.project.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.dals.project.model.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

class CashFlowRepository(
    private val transactionRepository: TransactionRepository
) {
    private val _cashFlowSummary = MutableStateFlow<CashFlowSummary?>(null)
    val cashFlowSummary: StateFlow<CashFlowSummary?> = _cashFlowSummary.asStateFlow()

    private val _monthlyTrends = MutableStateFlow<List<MonthlyTrend>>(emptyList())
    val monthlyTrends: StateFlow<List<MonthlyTrend>> = _monthlyTrends.asStateFlow()

    private val _insights = MutableStateFlow<List<CashFlowInsight>>(emptyList())
    val insights: StateFlow<List<CashFlowInsight>> = _insights.asStateFlow()

    private val _periodComparison = MutableStateFlow<PeriodComparison?>(null)
    val periodComparison: StateFlow<PeriodComparison?> = _periodComparison.asStateFlow()

    /**
     * Analyze cash flow from transaction data
     */
    fun analyzeCashFlow(period: AnalysisPeriod = AnalysisPeriod.MONTH_30) {
        try {
            val transactions = transactionRepository.transactions.value
            if (transactions.isEmpty()) {
                clearData()
                return
            }

            // Filter transactions by period
            val filteredTransactions = filterTransactionsByPeriod(transactions, period)

            // Calculate income and expenses
            val income = filteredTransactions
                .filter { it.type == TransactionType.RECEIVE || it.type == TransactionType.DEPOSIT }
                .sumOf { it.amount }

            val expenses = filteredTransactions
                .filter {
                    it.type == TransactionType.SEND ||
                    it.type == TransactionType.WITHDRAWAL ||
                    it.type == TransactionType.BILL_PAYMENT ||
                    it.type == TransactionType.RENT_PAYMENT ||
                    it.type == TransactionType.LOAN_PAYMENT
                }
                .sumOf { it.amount + it.fee }

            val netCashFlow = income - expenses

            // Analyze by category
            val categories = analyzeByCategory(filteredTransactions)

            _cashFlowSummary.value = CashFlowSummary(
                totalIncome = income,
                totalExpenses = expenses,
                netCashFlow = netCashFlow,
                period = period.displayName,
                categories = categories
            )

            // Generate monthly trends (last 6 months)
            _monthlyTrends.value = generateMonthlyTrends(transactions)

            // Generate insights
            _insights.value = generateInsights(income, expenses, netCashFlow, categories, transactions)

            // Compare periods
            _periodComparison.value = comparePeriods(transactions, period)

        } catch (e: Exception) {
            println("❌ Error analyzing cash flow: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun filterTransactionsByPeriod(
        transactions: List<Transaction>,
        period: AnalysisPeriod
    ): List<Transaction> {
        val now = getCurrentDateTime()
        val cutoffDate = when (period) {
            AnalysisPeriod.WEEK_7 -> now.minusDays(7)
            AnalysisPeriod.MONTH_30 -> now.minusDays(30)
            AnalysisPeriod.MONTH_90 -> now.minusDays(90)
            AnalysisPeriod.YEAR -> now.minusDays(365)
            AnalysisPeriod.ALL_TIME -> return transactions
        }

        return transactions.filter {
            try {
                val txnDate = parseTransactionDate(it.timestamp)
                txnDate.isAfter(cutoffDate)
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun analyzeByCategory(transactions: List<Transaction>): List<CategoryFlow> {
        val categoryMap = transactions.groupBy { it.category }

        return categoryMap.map { (category, txns) ->
            val inflow = txns
                .filter { it.type == TransactionType.RECEIVE || it.type == TransactionType.DEPOSIT }
                .sumOf { it.amount }

            val outflow = txns
                .filter {
                    it.type == TransactionType.SEND ||
                    it.type == TransactionType.WITHDRAWAL ||
                    it.type == TransactionType.BILL_PAYMENT ||
                    it.type == TransactionType.RENT_PAYMENT ||
                    it.type == TransactionType.LOAN_PAYMENT
                }
                .sumOf { it.amount + it.fee }

            CategoryFlow(
                category = category,
                inflow = inflow,
                outflow = outflow,
                netFlow = inflow - outflow,
                transactionCount = txns.size
            )
        }.sortedByDescending { abs(it.netFlow) }
    }

    private fun generateMonthlyTrends(transactions: List<Transaction>): List<MonthlyTrend> {
        val now = getCurrentDateTime()
        val trends = mutableListOf<MonthlyTrend>()

        for (i in 0..5) {
            val monthStart = now.minusMonths(i.toLong()).withDayOfMonth(1)
            val monthEnd = monthStart.plusMonths(1).minusDays(1)

            val monthTransactions = transactions.filter {
                try {
                    val txnDate = parseTransactionDate(it.timestamp)
                    txnDate.isAfter(monthStart.minusDays(1)) && txnDate.isBefore(monthEnd.plusDays(1))
                } catch (e: Exception) {
                    false
                }
            }

            val income = monthTransactions
                .filter { it.type == TransactionType.RECEIVE || it.type == TransactionType.DEPOSIT }
                .sumOf { it.amount }

            val expenses = monthTransactions
                .filter {
                    it.type == TransactionType.SEND ||
                    it.type == TransactionType.WITHDRAWAL ||
                    it.type == TransactionType.BILL_PAYMENT ||
                    it.type == TransactionType.RENT_PAYMENT ||
                    it.type == TransactionType.LOAN_PAYMENT
                }
                .sumOf { it.amount + it.fee }

            trends.add(
                MonthlyTrend(
                    month = monthStart.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                    income = income,
                    expenses = expenses,
                    netFlow = income - expenses
                )
            )
        }

        return trends.reversed()
    }

    private fun generateInsights(
        income: Double,
        expenses: Double,
        netCashFlow: Double,
        categories: List<CategoryFlow>,
        allTransactions: List<Transaction>
    ): List<CashFlowInsight> {
        val insights = mutableListOf<CashFlowInsight>()

        // Net cash flow insight
        when {
            netCashFlow > 1000 -> insights.add(
                CashFlowInsight(
                    type = InsightType.POSITIVE_TREND,
                    title = "Great Financial Health!",
                    message = "You saved $${String.format("%.2f", netCashFlow)} this period. Keep it up!",
                    severity = InsightSeverity.POSITIVE
                )
            )
            netCashFlow < 0 -> insights.add(
                CashFlowInsight(
                    type = InsightType.BUDGET_WARNING,
                    title = "Negative Cash Flow",
                    message = "You spent $${String.format("%.2f", abs(netCashFlow))} more than you earned this period.",
                    severity = InsightSeverity.WARNING
                )
            )
            netCashFlow >= 0 && netCashFlow <= 100 -> insights.add(
                CashFlowInsight(
                    type = InsightType.BUDGET_WARNING,
                    title = "Breaking Even",
                    message = "Your income and expenses are nearly equal. Consider reducing expenses to save more.",
                    severity = InsightSeverity.INFO
                )
            )
        }

        // Expense ratio insight
        if (income > 0) {
            val expenseRatio = (expenses / income) * 100
            when {
                expenseRatio > 90 -> insights.add(
                    CashFlowInsight(
                        type = InsightType.BUDGET_WARNING,
                        title = "High Expense Ratio",
                        message = "You're spending ${String.format("%.1f", expenseRatio)}% of your income. Aim for under 70%.",
                        severity = InsightSeverity.CRITICAL
                    )
                )
                expenseRatio < 50 -> insights.add(
                    CashFlowInsight(
                        type = InsightType.SAVING_OPPORTUNITY,
                        title = "Excellent Savings Rate",
                        message = "You're only spending ${String.format("%.1f", expenseRatio)}% of your income. Great job!",
                        severity = InsightSeverity.POSITIVE
                    )
                )
            }
        }

        // Category-specific insights
        val topExpenseCategory = categories.maxByOrNull { it.outflow }
        if (topExpenseCategory != null && topExpenseCategory.outflow > 0) {
            insights.add(
                CashFlowInsight(
                    type = InsightType.SPENDING_PATTERN,
                    title = "Top Expense Category",
                    message = "${topExpenseCategory.category.name} is your largest expense at $${String.format("%.2f", topExpenseCategory.outflow)}.",
                    severity = InsightSeverity.INFO
                )
            )
        }

        // Transaction frequency insight
        val transactionCount = allTransactions.size
        if (transactionCount > 50) {
            insights.add(
                CashFlowInsight(
                    type = InsightType.SPENDING_PATTERN,
                    title = "High Transaction Volume",
                    message = "You made $transactionCount transactions this period. Consider consolidating purchases.",
                    severity = InsightSeverity.INFO
                )
            )
        }

        return insights
    }

    private fun comparePeriods(
        transactions: List<Transaction>,
        period: AnalysisPeriod
    ): PeriodComparison {
        val now = getCurrentDateTime()
        val periodDays = when (period) {
            AnalysisPeriod.WEEK_7 -> 7L
            AnalysisPeriod.MONTH_30 -> 30L
            AnalysisPeriod.MONTH_90 -> 90L
            AnalysisPeriod.YEAR -> 365L
            AnalysisPeriod.ALL_TIME -> return PeriodComparison(
                currentPeriod = PeriodData("", "", 0.0, 0.0, 0.0, 0),
                previousPeriod = PeriodData("", "", 0.0, 0.0, 0.0, 0),
                percentageChange = 0.0,
                trend = TrendDirection.STABLE
            )
        }

        // Current period
        val currentStart = now.minusDays(periodDays)
        val currentTransactions = transactions.filter {
            try {
                val txnDate = parseTransactionDate(it.timestamp)
                txnDate.isAfter(currentStart)
            } catch (e: Exception) {
                false
            }
        }

        // Previous period
        val previousStart = currentStart.minusDays(periodDays)
        val previousEnd = currentStart
        val previousTransactions = transactions.filter {
            try {
                val txnDate = parseTransactionDate(it.timestamp)
                txnDate.isAfter(previousStart) && txnDate.isBefore(previousEnd)
            } catch (e: Exception) {
                false
            }
        }

        val currentIncome = currentTransactions
            .filter { it.type == TransactionType.RECEIVE || it.type == TransactionType.DEPOSIT }
            .sumOf { it.amount }
        val currentExpenses = currentTransactions
            .filter {
                it.type == TransactionType.SEND ||
                it.type == TransactionType.WITHDRAWAL ||
                it.type == TransactionType.BILL_PAYMENT ||
                it.type == TransactionType.RENT_PAYMENT ||
                it.type == TransactionType.LOAN_PAYMENT
            }
            .sumOf { it.amount + it.fee }

        val previousIncome = previousTransactions
            .filter { it.type == TransactionType.RECEIVE || it.type == TransactionType.DEPOSIT }
            .sumOf { it.amount }
        val previousExpenses = previousTransactions
            .filter {
                it.type == TransactionType.SEND ||
                it.type == TransactionType.WITHDRAWAL ||
                it.type == TransactionType.BILL_PAYMENT ||
                it.type == TransactionType.RENT_PAYMENT ||
                it.type == TransactionType.LOAN_PAYMENT
            }
            .sumOf { it.amount + it.fee }

        val currentNet = currentIncome - currentExpenses
        val previousNet = previousIncome - previousExpenses

        val percentageChange = if (previousNet != 0.0) {
            ((currentNet - previousNet) / abs(previousNet)) * 100
        } else {
            0.0
        }

        val trend = when {
            percentageChange > 5 -> TrendDirection.UP
            percentageChange < -5 -> TrendDirection.DOWN
            else -> TrendDirection.STABLE
        }

        return PeriodComparison(
            currentPeriod = PeriodData(
                startDate = currentStart.format(DateTimeFormatter.ISO_LOCAL_DATE),
                endDate = now.format(DateTimeFormatter.ISO_LOCAL_DATE),
                income = currentIncome,
                expenses = currentExpenses,
                netFlow = currentNet,
                transactionCount = currentTransactions.size
            ),
            previousPeriod = PeriodData(
                startDate = previousStart.format(DateTimeFormatter.ISO_LOCAL_DATE),
                endDate = previousEnd.format(DateTimeFormatter.ISO_LOCAL_DATE),
                income = previousIncome,
                expenses = previousExpenses,
                netFlow = previousNet,
                transactionCount = previousTransactions.size
            ),
            percentageChange = percentageChange,
            trend = trend
        )
    }

    private fun getCurrentDateTime(): LocalDateTime {
        return try {
            LocalDateTime.now()
        } catch (e: Exception) {
            // Fallback for platforms without LocalDateTime
            LocalDateTime.of(2026, 3, 9, 0, 0)
        }
    }

    private fun parseTransactionDate(timestamp: String): LocalDateTime {
        return try {
            LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } catch (e: Exception) {
            try {
                LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            } catch (e2: Exception) {
                LocalDateTime.now()
            }
        }
    }

    private fun clearData() {
        _cashFlowSummary.value = null
        _monthlyTrends.value = emptyList()
        _insights.value = emptyList()
        _periodComparison.value = null
    }

    enum class AnalysisPeriod(val displayName: String) {
        WEEK_7("Last 7 Days"),
        MONTH_30("Last 30 Days"),
        MONTH_90("Last 90 Days"),
        YEAR("Last Year"),
        ALL_TIME("All Time")
    }
}
