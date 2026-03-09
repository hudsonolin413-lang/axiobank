package org.dals.project.services

import org.dals.project.database.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.math.abs

data class CashFlowForecastDto(
    val id: String,
    val customerId: String,
    val accountId: String,
    val forecastDate: String,
    val predictedIncome: Double,
    val predictedExpenses: Double,
    val predictedBalance: Double,
    val actualIncome: Double?,
    val actualExpenses: Double?,
    val actualBalance: Double?,
    val confidence: Double,
    val forecastMethod: String,
    val notes: String?,
    val createdAt: String
)

data class CashFlowAnalysisDto(
    val currentBalance: Double,
    val forecasts: List<CashFlowForecastDto>,
    val averageMonthlyIncome: Double,
    val averageMonthlyExpenses: Double,
    val projectedBalanceIn30Days: Double,
    val projectedBalanceIn60Days: Double,
    val projectedBalanceIn90Days: Double,
    val riskLevel: String, // LOW, MEDIUM, HIGH
    val insights: List<String>
)

data class GenerateForecastRequest(
    val customerId: String,
    val accountId: String,
    val days: Int = 90,
    val forecastMethod: String = "PATTERN_BASED"
)

object CashFlowForecastService {

    fun generateForecasts(request: GenerateForecastRequest): Result<List<CashFlowForecastDto>> {
        return try {
            transaction {
                // Validate customer and account
                val account = Accounts.select {
                    (Accounts.id eq UUID.fromString(request.accountId)) and
                    (Accounts.customerId eq UUID.fromString(request.customerId))
                }.singleOrNull()
                    ?: return@transaction Result.failure(Exception("Account not found"))

                val currentBalance = account[Accounts.balance].toDouble()

                // Get transaction history for pattern analysis
                val history = getTransactionHistory(request.accountId, 90) // Last 90 days

                // Analyze patterns
                val patterns = analyzeTransactionPatterns(history)

                // Generate forecasts
                val forecasts = mutableListOf<CashFlowForecastDto>()
                var projectedBalance = currentBalance

                for (day in 1..request.days) {
                    val forecastDate = LocalDate.now().plusDays(day.toLong())

                    // Predict income and expenses based on patterns
                    val (predictedIncome, predictedExpenses, confidence) = predictDailyFlow(
                        forecastDate,
                        patterns,
                        day
                    )

                    projectedBalance = projectedBalance + predictedIncome - predictedExpenses

                    val forecastId = CashFlowForecasts.insert {
                        it[customerId] = UUID.fromString(request.customerId)
                        it[accountId] = UUID.fromString(request.accountId)
                        it[CashFlowForecasts.forecastDate] = forecastDate
                        it[CashFlowForecasts.predictedIncome] = BigDecimal(predictedIncome)
                        it[CashFlowForecasts.predictedExpenses] = BigDecimal(predictedExpenses)
                        it[predictedBalance] = BigDecimal(projectedBalance)
                        it[CashFlowForecasts.confidence] = BigDecimal(confidence)
                        it[forecastMethod] = request.forecastMethod
                    }[CashFlowForecasts.id].value

                    val forecast = CashFlowForecasts.select { CashFlowForecasts.id eq forecastId }
                        .single()

                    forecasts.add(mapToForecastDto(forecast))
                }

                Result.success(forecasts)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getForecasts(
        accountId: String,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Result<List<CashFlowForecastDto>> {
        return try {
            transaction {
                val start = startDate ?: LocalDate.now()
                val end = endDate ?: LocalDate.now().plusDays(90)

                val forecasts = CashFlowForecasts.select {
                    (CashFlowForecasts.accountId eq UUID.fromString(accountId)) and
                    (CashFlowForecasts.forecastDate greaterEq start) and
                    (CashFlowForecasts.forecastDate lessEq end)
                }.orderBy(CashFlowForecasts.forecastDate)
                    .map { mapToForecastDto(it) }

                Result.success(forecasts)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCashFlowAnalysis(accountId: String, customerId: String): Result<CashFlowAnalysisDto> {
        return try {
            transaction {
                // Get current balance
                val account = Accounts.select {
                    (Accounts.id eq UUID.fromString(accountId)) and
                    (Accounts.customerId eq UUID.fromString(customerId))
                }.singleOrNull()
                    ?: return@transaction Result.failure(Exception("Account not found"))

                val currentBalance = account[Accounts.balance].toDouble()

                // Get forecasts
                val forecasts = CashFlowForecasts.select {
                    (CashFlowForecasts.accountId eq UUID.fromString(accountId)) and
                    (CashFlowForecasts.forecastDate greaterEq LocalDate.now())
                }.orderBy(CashFlowForecasts.forecastDate)
                    .limit(90)
                    .map { mapToForecastDto(it) }

                // Calculate averages from history
                val history = getTransactionHistory(accountId, 90)
                val income = history.filter { it.type == "DEPOSIT" || it.type == "CREDIT" }
                    .sumOf { it.amount }
                val expenses = history.filter { it.type == "WITHDRAWAL" || it.type == "DEBIT" || it.type == "PAYMENT" }
                    .sumOf { it.amount }

                val averageMonthlyIncome = income / 3.0 // Average over 3 months
                val averageMonthlyExpenses = expenses / 3.0

                // Get projected balances
                val day30 = forecasts.find { it.forecastDate == LocalDate.now().plusDays(30).toString() }
                val day60 = forecasts.find { it.forecastDate == LocalDate.now().plusDays(60).toString() }
                val day90 = forecasts.find { it.forecastDate == LocalDate.now().plusDays(90).toString() }

                val projectedBalanceIn30Days = day30?.predictedBalance ?: currentBalance
                val projectedBalanceIn60Days = day60?.predictedBalance ?: currentBalance
                val projectedBalanceIn90Days = day90?.predictedBalance ?: currentBalance

                // Determine risk level
                val riskLevel = when {
                    projectedBalanceIn30Days < 0 -> "HIGH"
                    projectedBalanceIn60Days < (averageMonthlyExpenses * 0.5) -> "MEDIUM"
                    else -> "LOW"
                }

                // Generate insights
                val insights = mutableListOf<String>()

                if (projectedBalanceIn30Days < 0) {
                    insights.add("Warning: Your account may go negative within 30 days")
                }

                if (averageMonthlyExpenses > averageMonthlyIncome) {
                    val deficit = averageMonthlyExpenses - averageMonthlyIncome
                    insights.add("You're spending $${String.format("%.2f", deficit)} more than you earn per month")
                }

                if (currentBalance < averageMonthlyExpenses) {
                    insights.add("Your current balance is less than one month of expenses")
                }

                val expenseGrowth = calculateGrowthRate(history.filter { it.type == "PAYMENT" || it.type == "WITHDRAWAL" })
                if (expenseGrowth > 10) {
                    insights.add("Your expenses are growing by ${String.format("%.1f", expenseGrowth)}% month-over-month")
                }

                if (projectedBalanceIn90Days > currentBalance * 1.2) {
                    insights.add("Great news! Your balance is projected to grow by 20% in 90 days")
                }

                if (insights.isEmpty()) {
                    insights.add("Your cash flow looks stable")
                }

                Result.success(
                    CashFlowAnalysisDto(
                        currentBalance = currentBalance,
                        forecasts = forecasts,
                        averageMonthlyIncome = averageMonthlyIncome,
                        averageMonthlyExpenses = averageMonthlyExpenses,
                        projectedBalanceIn30Days = projectedBalanceIn30Days,
                        projectedBalanceIn60Days = projectedBalanceIn60Days,
                        projectedBalanceIn90Days = projectedBalanceIn90Days,
                        riskLevel = riskLevel,
                        insights = insights
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private data class TransactionHistoryItem(
        val date: LocalDate,
        val type: String,
        val amount: Double
    )

    private fun getTransactionHistory(accountId: String, days: Int): List<TransactionHistoryItem> {
        val cutoffDate = LocalDate.now().minusDays(days.toLong())

        return Transactions.select {
            (Transactions.accountId eq UUID.fromString(accountId)) and
            (Transactions.createdAt.date() greaterEq cutoffDate)
        }.map {
            TransactionHistoryItem(
                date = it[Transactions.createdAt].atZone(ZoneId.systemDefault()).toLocalDate(),
                type = it[Transactions.type].name,
                amount = it[Transactions.amount].toDouble()
            )
        }
    }

    private data class TransactionPattern(
        val averageDailyIncome: Double,
        val averageDailyExpenses: Double,
        val incomeVariance: Double,
        val expenseVariance: Double,
        val incomeFrequency: Map<Int, Double>, // Day of month -> average income
        val expenseFrequency: Map<Int, Double> // Day of month -> average expenses
    )

    private fun analyzeTransactionPatterns(history: List<TransactionHistoryItem>): TransactionPattern {
        val income = history.filter { it.type == "DEPOSIT" || it.type == "INTEREST_CREDIT" || it.type == "CHECK_DEPOSIT" || it.type == "DIRECT_DEPOSIT" || it.type == "MPESA_DEPOSIT" || it.type == "MOBILE_MONEY_DEPOSIT" || it.type == "LOAN_DISBURSEMENT" }
        val expenses = history.filter { it.type == "WITHDRAWAL" || it.type == "PAYMENT" || it.type == "FEE_DEBIT" || it.type == "ATM_WITHDRAWAL" || it.type == "LOAN_PAYMENT" || it.type == "MPESA_WITHDRAWAL" || it.type == "MOBILE_MONEY_WITHDRAWAL" || it.type == "QR_PAYMENT" || it.type == "NFC_PAYMENT" || it.type == "INTERNATIONAL_TRANSFER" }

        val totalIncome = income.sumOf { it.amount }
        val totalExpenses = expenses.sumOf { it.amount }

        val days = if (history.isNotEmpty()) {
            (history.maxOf { it.date.toEpochDay() } - history.minOf { it.date.toEpochDay() } + 1).toDouble()
        } else 1.0

        val averageDailyIncome = totalIncome / days
        val averageDailyExpenses = totalExpenses / days

        // Calculate variance
        val incomeVariance = if (income.isNotEmpty()) {
            income.map { (it.amount - averageDailyIncome) * (it.amount - averageDailyIncome) }
                .average()
        } else 0.0

        val expenseVariance = if (expenses.isNotEmpty()) {
            expenses.map { (it.amount - averageDailyExpenses) * (it.amount - averageDailyExpenses) }
                .average()
        } else 0.0

        // Analyze by day of month
        val incomeByDay = income.groupBy { it.date.dayOfMonth }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } / transactions.size }

        val expenseByDay = expenses.groupBy { it.date.dayOfMonth }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } / transactions.size }

        return TransactionPattern(
            averageDailyIncome = averageDailyIncome,
            averageDailyExpenses = averageDailyExpenses,
            incomeVariance = incomeVariance,
            expenseVariance = expenseVariance,
            incomeFrequency = incomeByDay,
            expenseFrequency = expenseByDay
        )
    }

    private fun predictDailyFlow(
        forecastDate: LocalDate,
        patterns: TransactionPattern,
        dayOffset: Int
    ): Triple<Double, Double, Double> {
        val dayOfMonth = forecastDate.dayOfMonth

        // Check if there's a pattern for this day
        val predictedIncome = patterns.incomeFrequency[dayOfMonth] ?: patterns.averageDailyIncome
        val predictedExpenses = patterns.expenseFrequency[dayOfMonth] ?: patterns.averageDailyExpenses

        // Calculate confidence (decreases with distance)
        val baseConfidence = 85.0
        val decayRate = 0.5 // 0.5% per day
        val confidence = (baseConfidence - (dayOffset * decayRate)).coerceAtLeast(50.0)

        return Triple(predictedIncome, predictedExpenses, confidence)
    }

    private fun calculateGrowthRate(transactions: List<TransactionHistoryItem>): Double {
        if (transactions.isEmpty()) return 0.0

        val now = LocalDate.now()
        val lastMonth = transactions.filter {
            it.date.isAfter(now.minusDays(30)) && it.date.isBefore(now)
        }.sumOf { it.amount }

        val previousMonth = transactions.filter {
            it.date.isAfter(now.minusDays(60)) && it.date.isBefore(now.minusDays(30))
        }.sumOf { it.amount }

        return if (previousMonth > 0) {
            ((lastMonth - previousMonth) / previousMonth) * 100
        } else 0.0
    }

    fun updateActualValues(forecastId: String): Result<CashFlowForecastDto> {
        return try {
            transaction {
                val forecast = CashFlowForecasts.select { CashFlowForecasts.id eq UUID.fromString(forecastId) }
                    .singleOrNull()
                    ?: return@transaction Result.failure(Exception("Forecast not found"))

                val accountId = forecast[CashFlowForecasts.accountId].toString()
                val forecastDate = forecast[CashFlowForecasts.forecastDate]

                // Get actual transactions for that date
                val transactions = Transactions.select {
                    (Transactions.accountId eq UUID.fromString(accountId)) and
                    (Transactions.createdAt.date() eq forecastDate)
                }.toList()

                val actualIncome = transactions
                    .filter { 
                        val t = it[Transactions.type]
                        t == TransactionType.DEPOSIT || 
                        t == TransactionType.INTEREST_CREDIT || 
                        t == TransactionType.CHECK_DEPOSIT || 
                        t == TransactionType.DIRECT_DEPOSIT || 
                        t == TransactionType.MPESA_DEPOSIT || 
                        t == TransactionType.MOBILE_MONEY_DEPOSIT || 
                        t == TransactionType.LOAN_DISBURSEMENT
                    }
                    .sumOf { it[Transactions.amount].toDouble() }

                val actualExpenses = transactions
                    .filter { 
                        val t = it[Transactions.type]
                        t == TransactionType.WITHDRAWAL || 
                        t == TransactionType.PAYMENT || 
                        t == TransactionType.FEE_DEBIT || 
                        t == TransactionType.ATM_WITHDRAWAL || 
                        t == TransactionType.LOAN_PAYMENT || 
                        t == TransactionType.MPESA_WITHDRAWAL || 
                        t == TransactionType.MOBILE_MONEY_WITHDRAWAL || 
                        t == TransactionType.QR_PAYMENT || 
                        t == TransactionType.NFC_PAYMENT || 
                        t == TransactionType.INTERNATIONAL_TRANSFER
                    }
                    .sumOf { it[Transactions.amount].toDouble() }

                // Get actual balance at end of day
                val account = Accounts.select { Accounts.id eq UUID.fromString(accountId) }.single()
                val actualBalance = account[Accounts.balance].toDouble()

                // Update forecast
                CashFlowForecasts.update({ CashFlowForecasts.id eq UUID.fromString(forecastId) }) {
                    it[CashFlowForecasts.actualIncome] = BigDecimal.valueOf(actualIncome)
                    it[CashFlowForecasts.actualExpenses] = BigDecimal.valueOf(actualExpenses)
                    it[CashFlowForecasts.actualBalance] = BigDecimal.valueOf(actualBalance)
                    it[updatedAt] = java.time.Instant.now()
                }

                val updated = CashFlowForecasts.select { CashFlowForecasts.id eq UUID.fromString(forecastId) }
                    .single()

                Result.success(mapToForecastDto(updated))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapToForecastDto(row: ResultRow): CashFlowForecastDto {
        return CashFlowForecastDto(
            id = row[CashFlowForecasts.id].value.toString(),
            customerId = row[CashFlowForecasts.customerId].toString(),
            accountId = row[CashFlowForecasts.accountId].toString(),
            forecastDate = row[CashFlowForecasts.forecastDate].toString(),
            predictedIncome = row[CashFlowForecasts.predictedIncome].toDouble(),
            predictedExpenses = row[CashFlowForecasts.predictedExpenses].toDouble(),
            predictedBalance = row[CashFlowForecasts.predictedBalance].toDouble(),
            actualIncome = row[CashFlowForecasts.actualIncome]?.toDouble(),
            actualExpenses = row[CashFlowForecasts.actualExpenses]?.toDouble(),
            actualBalance = row[CashFlowForecasts.actualBalance]?.toDouble(),
            confidence = row[CashFlowForecasts.confidence].toDouble(),
            forecastMethod = row[CashFlowForecasts.forecastMethod],
            notes = row[CashFlowForecasts.notes],
            createdAt = row[CashFlowForecasts.createdAt].toString()
        )
    }
}
