package org.dals.project.services

import org.dals.project.database.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import kotlin.math.pow

data class CreateRefinancingApplicationRequest(
    val customerId: String,
    val originalLoanId: String,
    val newInterestRate: Double,
    val newTermMonths: Int,
    val closingCosts: Double = 0.0
)

data class LoanRefinancingDto(
    val id: String,
    val customerId: String,
    val originalLoanId: String,
    val originalLoanAmount: Double,
    val originalInterestRate: Double,
    val originalMonthlyPayment: Double,
    val originalRemainingBalance: Double,
    val originalRemainingTermMonths: Int,
    val newLoanAmount: Double,
    val newInterestRate: Double,
    val newMonthlyPayment: Double,
    val newTermMonths: Int,
    val monthlySavings: Double,
    val totalInterestSavings: Double,
    val breakEvenMonths: Int?,
    val closingCosts: Double,
    val status: String,
    val reason: String?,
    val reviewedBy: String?,
    val reviewedAt: String?,
    val completedAt: String?,
    val createdAt: String
)

data class RefinancingAnalysisDto(
    val recommendRefinancing: Boolean,
    val estimatedMonthlySavings: Double,
    val estimatedTotalInterestSavings: Double,
    val breakEvenMonths: Int,
    val currentMonthlyPayment: Double,
    val newMonthlyPayment: Double,
    val currentRemainingBalance: Double,
    val currentInterestRate: Double,
    val proposedInterestRate: Double,
    val closingCosts: Double,
    val insights: List<String>
)

object LoanRefinancingService {

    fun analyzeRefinancingOpportunity(
        loanId: String,
        proposedInterestRate: Double,
        proposedTermMonths: Int,
        closingCosts: Double = 0.0
    ): Result<RefinancingAnalysisDto> {
        return try {
            transaction {
                val loan = Loans.select { Loans.id eq UUID.fromString(loanId) }
                    .singleOrNull()
                    ?: return@transaction Result.failure(Exception("Loan not found"))

                val currentAmount = loan[Loans.currentBalance].toDouble()
                val interestRate = loan[Loans.interestRate].toDouble()
                val termMonths = loan[Loans.termMonths]
                val monthlyPayment = loan[Loans.monthlyPayment].toDouble()

                // Calculate remaining months
                val paymentsMade = Transactions.select {
                    (Transactions.accountId eq loan[Loans.accountId]!!) and
                    (Transactions.type eq TransactionType.LOAN_PAYMENT)
                }.count().toInt()

                val remainingMonths = (termMonths - paymentsMade).coerceAtLeast(1)

                // Calculate current total interest
                val currentTotalPayment = monthlyPayment * remainingMonths
                val currentTotalInterest = currentTotalPayment - currentAmount

                // Calculate new monthly payment
                val newMonthlyPayment = calculateMonthlyPayment(
                    currentAmount,
                    proposedInterestRate,
                    proposedTermMonths
                )

                // Calculate new total interest
                val newTotalPayment = newMonthlyPayment * proposedTermMonths
                val newTotalInterest = newTotalPayment - currentAmount

                // Calculate savings
                val monthlySavings = monthlyPayment - newMonthlyPayment
                val totalInterestSavings = currentTotalInterest - newTotalInterest - closingCosts

                // Calculate break-even point
                val breakEvenMonths = if (monthlySavings > 0) {
                    (closingCosts / monthlySavings).toInt() + 1
                } else {
                    Int.MAX_VALUE
                }

                // Generate insights
                val insights = mutableListOf<String>()

                val recommendRefinancing = totalInterestSavings > 0 && breakEvenMonths < remainingMonths

                if (recommendRefinancing) {
                    insights.add("Refinancing could save you $${String.format("%.2f", totalInterestSavings)} in total interest")
                    insights.add("Your monthly payment would decrease by $${String.format("%.2f", monthlySavings)}")
                    insights.add("You'll break even after $breakEvenMonths months")
                } else {
                    if (totalInterestSavings <= 0) {
                        insights.add("Refinancing would cost you more in interest over the life of the loan")
                    }
                    if (breakEvenMonths >= remainingMonths) {
                        insights.add("It would take $breakEvenMonths months to recover closing costs, which exceeds your remaining loan term")
                    }
                }

                if (proposedInterestRate < interestRate - 0.5) {
                    insights.add("Great rate reduction of ${String.format("%.2f", interestRate - proposedInterestRate)}%!")
                }

                if (proposedTermMonths > remainingMonths) {
                    val extraMonths = proposedTermMonths - remainingMonths
                    insights.add("Note: New term extends your loan by $extraMonths months")
                }

                Result.success(
                    RefinancingAnalysisDto(
                        recommendRefinancing = recommendRefinancing,
                        estimatedMonthlySavings = monthlySavings,
                        estimatedTotalInterestSavings = totalInterestSavings,
                        breakEvenMonths = breakEvenMonths,
                        currentMonthlyPayment = monthlyPayment,
                        newMonthlyPayment = newMonthlyPayment,
                        currentRemainingBalance = currentAmount,
                        currentInterestRate = interestRate,
                        proposedInterestRate = proposedInterestRate,
                        closingCosts = closingCosts,
                        insights = insights
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun createRefinancingApplication(request: CreateRefinancingApplicationRequest): Result<LoanRefinancingDto> {
        return try {
            transaction {
                // Get loan details
                val loan = Loans.select {
                    (Loans.id eq UUID.fromString(request.originalLoanId)) and
                    (Loans.customerId eq UUID.fromString(request.customerId))
                }.singleOrNull()
                    ?: return@transaction Result.failure(Exception("Loan not found"))

                val originalAmount = loan[Loans.originalAmount].toDouble()
                val currentAmount = loan[Loans.currentBalance].toDouble()
                val interestRate = loan[Loans.interestRate].toDouble()
                val termMonths = loan[Loans.termMonths]
                val monthlyPayment = loan[Loans.monthlyPayment].toDouble()

                // Calculate remaining months
                val paymentsMade = Transactions.select {
                    (Transactions.accountId eq loan[Loans.accountId]!!) and
                    (Transactions.type eq TransactionType.LOAN_PAYMENT)
                }.count().toInt()

                val remainingMonths = (termMonths - paymentsMade).coerceAtLeast(1)

                // Calculate new loan details
                val newMonthlyPayment = calculateMonthlyPayment(
                    currentAmount,
                    request.newInterestRate,
                    request.newTermMonths
                )

                // Calculate savings
                val currentTotalInterest = (monthlyPayment * remainingMonths) - currentAmount
                val newTotalInterest = (newMonthlyPayment * request.newTermMonths) - currentAmount
                val monthlySavings = monthlyPayment - newMonthlyPayment
                val totalInterestSavings = currentTotalInterest - newTotalInterest - request.closingCosts

                // Calculate break-even
                val breakEvenMonths = if (monthlySavings > 0 && request.closingCosts > 0) {
                    (request.closingCosts / monthlySavings).toInt() + 1
                } else null

                // Create refinancing application
                val refinancingId = LoanRefinancing.insert {
                    it[customerId] = UUID.fromString(request.customerId)
                    it[originalLoanId] = UUID.fromString(request.originalLoanId)
                    it[originalLoanAmount] = BigDecimal(originalAmount)
                    it[originalInterestRate] = BigDecimal(interestRate)
                    it[originalMonthlyPayment] = BigDecimal(monthlyPayment)
                    it[originalRemainingBalance] = BigDecimal(currentAmount)
                    it[originalRemainingTermMonths] = remainingMonths
                    it[newLoanAmount] = BigDecimal(currentAmount)
                    it[newInterestRate] = BigDecimal(request.newInterestRate)
                    it[LoanRefinancing.newMonthlyPayment] = BigDecimal(newMonthlyPayment)
                    it[newTermMonths] = request.newTermMonths
                    it[LoanRefinancing.monthlySavings] = BigDecimal(monthlySavings)
                    it[LoanRefinancing.totalInterestSavings] = BigDecimal(totalInterestSavings)
                    it[LoanRefinancing.breakEvenMonths] = breakEvenMonths
                    it[LoanRefinancing.closingCosts] = BigDecimal(request.closingCosts)
                    it[status] = "PENDING"
                }[LoanRefinancing.id].value

                val refinancing = LoanRefinancing.select { LoanRefinancing.id eq refinancingId }
                    .single()

                Result.success(mapToRefinancingDto(refinancing))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getRefinancingApplication(refinancingId: String): Result<LoanRefinancingDto> {
        return try {
            transaction {
                val refinancing = LoanRefinancing.select { LoanRefinancing.id eq UUID.fromString(refinancingId) }
                    .singleOrNull()
                    ?: return@transaction Result.failure(Exception("Refinancing application not found"))

                Result.success(mapToRefinancingDto(refinancing))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCustomerRefinancingApplications(customerId: String): Result<List<LoanRefinancingDto>> {
        return try {
            transaction {
                val applications = LoanRefinancing.select {
                    LoanRefinancing.customerId eq UUID.fromString(customerId)
                }.orderBy(LoanRefinancing.createdAt to SortOrder.DESC)
                    .map { mapToRefinancingDto(it) }

                Result.success(applications)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun approveRefinancing(refinancingId: String, reviewerId: String): Result<LoanRefinancingDto> {
        return try {
            transaction {
                LoanRefinancing.update({ LoanRefinancing.id eq UUID.fromString(refinancingId) }) {
                    it[status] = "APPROVED"
                    it[reviewedBy] = UUID.fromString(reviewerId)
                    it[reviewedAt] = Instant.now()
                    it[updatedAt] = Instant.now()
                }

                val refinancing = LoanRefinancing.select { LoanRefinancing.id eq UUID.fromString(refinancingId) }
                    .single()

                Result.success(mapToRefinancingDto(refinancing))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun rejectRefinancing(refinancingId: String, reviewerId: String, reason: String): Result<LoanRefinancingDto> {
        return try {
            transaction {
                LoanRefinancing.update({ LoanRefinancing.id eq UUID.fromString(refinancingId) }) {
                    it[status] = "REJECTED"
                    it[LoanRefinancing.reason] = reason
                    it[reviewedBy] = UUID.fromString(reviewerId)
                    it[reviewedAt] = Instant.now()
                    it[updatedAt] = Instant.now()
                }

                val refinancing = LoanRefinancing.select { LoanRefinancing.id eq UUID.fromString(refinancingId) }
                    .single()

                Result.success(mapToRefinancingDto(refinancing))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateMonthlyPayment(principal: Double, annualRate: Double, termMonths: Int): Double {
        if (annualRate == 0.0) return principal / termMonths

        val monthlyRate = annualRate / 100.0 / 12.0
        val payment = principal * (monthlyRate * (1 + monthlyRate).pow(termMonths)) /
                ((1 + monthlyRate).pow(termMonths) - 1)

        return BigDecimal(payment).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    private fun mapToRefinancingDto(row: ResultRow): LoanRefinancingDto {
        return LoanRefinancingDto(
            id = row[LoanRefinancing.id].value.toString(),
            customerId = row[LoanRefinancing.customerId].toString(),
            originalLoanId = row[LoanRefinancing.originalLoanId].toString(),
            originalLoanAmount = row[LoanRefinancing.originalLoanAmount].toDouble(),
            originalInterestRate = row[LoanRefinancing.originalInterestRate].toDouble(),
            originalMonthlyPayment = row[LoanRefinancing.originalMonthlyPayment].toDouble(),
            originalRemainingBalance = row[LoanRefinancing.originalRemainingBalance].toDouble(),
            originalRemainingTermMonths = row[LoanRefinancing.originalRemainingTermMonths],
            newLoanAmount = row[LoanRefinancing.newLoanAmount].toDouble(),
            newInterestRate = row[LoanRefinancing.newInterestRate].toDouble(),
            newMonthlyPayment = row[LoanRefinancing.newMonthlyPayment].toDouble(),
            newTermMonths = row[LoanRefinancing.newTermMonths],
            monthlySavings = row[LoanRefinancing.monthlySavings].toDouble(),
            totalInterestSavings = row[LoanRefinancing.totalInterestSavings].toDouble(),
            breakEvenMonths = row[LoanRefinancing.breakEvenMonths],
            closingCosts = row[LoanRefinancing.closingCosts].toDouble(),
            status = row[LoanRefinancing.status],
            reason = row[LoanRefinancing.reason],
            reviewedBy = row[LoanRefinancing.reviewedBy]?.toString(),
            reviewedAt = row[LoanRefinancing.reviewedAt]?.toString(),
            completedAt = row[LoanRefinancing.completedAt]?.toString(),
            createdAt = row[LoanRefinancing.createdAt].toString()
        )
    }
}
