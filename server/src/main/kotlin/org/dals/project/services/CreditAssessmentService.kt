package org.dals.project.services

import org.dals.project.database.*
import org.dals.project.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

class CreditAssessmentService {

    suspend fun getAllCreditAssessments(page: Int = 1, pageSize: Int = 10): CustomerCareListResponse<CreditAssessmentDto> {
        return DatabaseFactory.dbQuery {
            val offset = (page - 1) * pageSize

            // Use JOIN to fetch customer names efficiently
            val assessments = CreditAssessments
                .leftJoin(Customers, { CreditAssessments.customerId }, { Customers.id })
                .selectAll()
                .orderBy(CreditAssessments.assessmentDate, SortOrder.DESC)
                .limit(pageSize, offset.toLong())
                .map { toCreditAssessmentDto(it) }

            val total = CreditAssessments.selectAll().count().toInt()

            CustomerCareListResponse(
                success = true,
                message = "Credit assessments retrieved successfully",
                data = assessments,
                total = total,
                page = page,
                pageSize = pageSize,
                timestamp = java.time.Instant.now().toString()
            )
        }
    }

    suspend fun getCreditAssessmentById(id: UUID): ApiResponse<CreditAssessmentDto> {
        return DatabaseFactory.dbQuery {
            val assessment = CreditAssessments
                .leftJoin(Customers, { CreditAssessments.customerId }, { Customers.id })
                .select { CreditAssessments.id eq id }
                .singleOrNull()
                ?.let { toCreditAssessmentDto(it) }

            if (assessment != null) {
                ApiResponse(
                    success = true,
                    message = "Credit assessment found",
                    data = assessment
                )
            } else {
                ApiResponse(
                    success = false,
                    message = "Credit assessment not found",
                    error = "No credit assessment found with ID: $id"
                )
            }
        }
    }

    suspend fun getCreditAssessmentsByCustomerId(customerId: UUID): CustomerCareListResponse<CreditAssessmentDto> {
        return DatabaseFactory.dbQuery {
            val assessments = CreditAssessments
                .leftJoin(Customers, { CreditAssessments.customerId }, { Customers.id })
                .select { CreditAssessments.customerId eq customerId }
                .orderBy(CreditAssessments.assessmentDate, SortOrder.DESC)
                .map { toCreditAssessmentDto(it) }

            CustomerCareListResponse(
                success = true,
                message = "Customer credit assessments retrieved successfully",
                data = assessments,
                total = assessments.size,
                page = 1,
                pageSize = assessments.size,
                timestamp = java.time.Instant.now().toString()
            )
        }
    }

    suspend fun createCreditAssessment(request: CreateCreditAssessmentRequest): ApiResponse<CreditAssessmentDto> {
        return DatabaseFactory.dbQuery {
            try {
                val assessmentId = UUID.randomUUID()

                val debtToIncomeRatio = BigDecimal(request.existingDebt).divide(
                    BigDecimal(request.annualIncome), 4, BigDecimal.ROUND_HALF_UP
                )

                val insertedId = CreditAssessments.insert {
                    it[id] = assessmentId
                    it[customerId] = UUID.fromString(request.customerId)
                    it[creditScore] = request.creditScore
                    it[CreditAssessments.debtToIncomeRatio] = debtToIncomeRatio
                    it[annualIncome] = BigDecimal(request.annualIncome)
                    it[existingDebt] = BigDecimal(request.existingDebt)
                    it[paymentHistory] = request.paymentHistory
                    it[riskLevel] = request.riskLevel
                    it[recommendedCreditLimit] = BigDecimal(request.recommendedCreditLimit)
                    it[assessedBy] = UUID.fromString(request.assessedBy)
                    it[comments] = request.comments
                }[CreditAssessments.id]

                // Update the customer's credit score
                Customers.update({ Customers.id eq UUID.fromString(request.customerId) }) {
                    it[creditScore] = request.creditScore
                }

                // Fetch the created assessment with customer name using JOIN
                val createdAssessment = CreditAssessments
                    .leftJoin(Customers, { CreditAssessments.customerId }, { Customers.id })
                    .select { CreditAssessments.id eq assessmentId }
                    .single()
                    .let { toCreditAssessmentDto(it) }

                ApiResponse(
                    success = true,
                    message = "Credit assessment created successfully",
                    data = createdAssessment
                )
            } catch (e: Exception) {
                ApiResponse(
                    success = false,
                    message = "Failed to create credit assessment",
                    error = e.message
                )
            }
        }
    }

    suspend fun syncCreditScoresToCustomers(): ApiResponse<Map<String, Any>> {
        return DatabaseFactory.dbQuery {
            try {
                // Get all customers with credit assessments
                val customersWithAssessments = CreditAssessments
                    .slice(CreditAssessments.customerId)
                    .selectAll()
                    .withDistinct()
                    .map { it[CreditAssessments.customerId] }

                var updatedCount = 0
                val updates = mutableListOf<String>()

                // For each customer, get their latest credit assessment and update their credit score
                customersWithAssessments.forEach { customerId ->
                    val latestAssessment = CreditAssessments
                        .select { CreditAssessments.customerId eq customerId }
                        .orderBy(CreditAssessments.assessmentDate to SortOrder.DESC)
                        .limit(1)
                        .singleOrNull()

                    if (latestAssessment != null) {
                        val creditScore = latestAssessment[CreditAssessments.creditScore]
                        if (creditScore != null) {
                            Customers.update({ Customers.id eq customerId }) {
                                it[Customers.creditScore] = creditScore
                            }
                            updatedCount++

                            // Get customer name for reporting
                            val customer = Customers.select { Customers.id eq customerId }.singleOrNull()
                            if (customer != null) {
                                val name = "${customer[Customers.firstName]} ${customer[Customers.lastName]}"
                                updates.add("$name: $creditScore")
                            }
                        }
                    }
                }

                ApiResponse(
                    success = true,
                    message = "Successfully synced credit scores for $updatedCount customers",
                    data = mapOf(
                        "updatedCount" to updatedCount,
                        "updates" to updates
                    )
                )
            } catch (e: Exception) {
                ApiResponse(
                    success = false,
                    message = "Failed to sync credit scores",
                    error = e.message
                )
            }
        }
    }

    private fun toCreditAssessmentDto(row: ResultRow): CreditAssessmentDto {
        // Get customer name from JOIN result
        val customerName = try {
            val firstName = row.getOrNull(Customers.firstName)
            val lastName = row.getOrNull(Customers.lastName)
            if (firstName != null && lastName != null) {
                "$firstName $lastName"
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }

        return CreditAssessmentDto(
            id = row[CreditAssessments.id].toString(),
            customerId = row[CreditAssessments.customerId].toString(),
            assessmentDate = row[CreditAssessments.assessmentDate].toString(),
            creditScore = row[CreditAssessments.creditScore],
            annualIncome = row[CreditAssessments.annualIncome].toString(),
            existingDebt = row[CreditAssessments.existingDebt].toString(),
            paymentHistory = row[CreditAssessments.paymentHistory],
            riskLevel = row[CreditAssessments.riskLevel],
            recommendedCreditLimit = row[CreditAssessments.recommendedCreditLimit].toString(),
            assessedBy = row[CreditAssessments.assessedBy].toString(),
            comments = row[CreditAssessments.comments],
            createdAt = row[CreditAssessments.createdAt].toString(),
            customerName = customerName
        )
    }
}
