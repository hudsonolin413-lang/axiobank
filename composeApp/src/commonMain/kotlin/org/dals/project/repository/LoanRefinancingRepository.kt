package org.dals.project.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class AnalyzeRefinancingRequest(
    val loanId: String,
    val proposedRate: Double,
    val proposedTerm: Int,
    val closingCosts: Double = 0.0
)

@Serializable
data class RefinancingAnalysisDto(
    val currentLoanId: String,
    val currentBalance: Double,
    val currentMonthlyPayment: Double,
    val currentRemainingTerm: Int,
    val currentAPR: Double,
    val proposedMonthlyPayment: Double,
    val monthlySavings: Double,
    val totalInterestSavings: Double,
    val breakEvenMonths: Int,
    val recommended: Boolean,
    val insights: List<String>
)

@Serializable
data class CreateRefinancingApplicationRequest(
    val customerId: String,
    val loanId: String,
    val proposedRate: Double,
    val proposedTerm: Int,
    val closingCosts: Double,
    val monthlySavings: Double,
    val totalInterestSavings: Double
)

@Serializable
data class RefinancingApplicationDto(
    val id: String,
    val customerId: String,
    val loanId: String,
    val proposedRate: Double,
    val proposedTerm: Int,
    val status: String,
    val monthlySavings: Double,
    val totalInterestSavings: Double,
    val createdAt: String,
    val approvedAt: String? = null,
    val approvedBy: String? = null
)

class LoanRefinancingRepository(private val client: HttpClient, private val baseUrl: String) {

    suspend fun analyzeRefinancing(
        loanId: String,
        proposedRate: Double,
        proposedTerm: Int,
        closingCosts: Double = 0.0
    ): Result<RefinancingAnalysisDto> {
        return try {
            val response = client.post("$baseUrl/api/v1/loan-refinancing/analyze") {
                parameter("loanId", loanId)
                parameter("proposedRate", proposedRate)
                parameter("proposedTerm", proposedTerm)
                parameter("closingCosts", closingCosts)
            }

            if (response.status.isSuccess()) {
                Result.success(response.body<RefinancingAnalysisDto>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to analyze refinancing"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun applyForRefinancing(request: CreateRefinancingApplicationRequest): Result<RefinancingApplicationDto> {
        return try {
            val response = client.post("$baseUrl/api/v1/loan-refinancing/apply") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (response.status.isSuccess()) {
                Result.success(response.body<RefinancingApplicationDto>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to apply for refinancing"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCustomerApplications(customerId: String): Result<List<RefinancingApplicationDto>> {
        return try {
            val response = client.get("$baseUrl/api/v1/loan-refinancing/customer/$customerId")

            if (response.status.isSuccess()) {
                Result.success(response.body<List<RefinancingApplicationDto>>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to get applications"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
