package org.dals.project.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class GenerateForecastRequest(
    val customerId: String,
    val accountId: String,
    val days: Int = 90,
    val forecastMethod: String = "PATTERN_BASED"
)

@Serializable
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

@Serializable
data class CashFlowAnalysisDto(
    val currentBalance: Double,
    val forecasts: List<CashFlowForecastDto>,
    val averageMonthlyIncome: Double,
    val averageMonthlyExpenses: Double,
    val projectedBalanceIn30Days: Double,
    val projectedBalanceIn60Days: Double,
    val projectedBalanceIn90Days: Double,
    val riskLevel: String,
    val insights: List<String>
)

class CashFlowForecastRepository(private val client: HttpClient, private val baseUrl: String) {

    suspend fun generateForecasts(request: GenerateForecastRequest): Result<List<CashFlowForecastDto>> {
        return try {
            val response = client.post("$baseUrl/api/v1/cash-flow-forecast/generate") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (response.status.isSuccess()) {
                Result.success(response.body<List<CashFlowForecastDto>>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to generate forecasts"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getForecasts(
        accountId: String,
        startDate: String? = null,
        endDate: String? = null
    ): Result<List<CashFlowForecastDto>> {
        return try {
            val response = client.get("$baseUrl/api/v1/cash-flow-forecast/account/$accountId") {
                startDate?.let { parameter("startDate", it) }
                endDate?.let { parameter("endDate", it) }
            }

            if (response.status.isSuccess()) {
                Result.success(response.body<List<CashFlowForecastDto>>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to get forecasts"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCashFlowAnalysis(accountId: String, customerId: String): Result<CashFlowAnalysisDto> {
        return try {
            val response = client.get("$baseUrl/api/v1/cash-flow-forecast/analysis/$accountId") {
                parameter("customerId", customerId)
            }

            if (response.status.isSuccess()) {
                Result.success(response.body<CashFlowAnalysisDto>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to get cash flow analysis"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateActualValues(forecastId: String): Result<CashFlowForecastDto> {
        return try {
            val response = client.post("$baseUrl/api/v1/cash-flow-forecast/update-actual/$forecastId") {
                contentType(ContentType.Application.Json)
            }

            if (response.status.isSuccess()) {
                Result.success(response.body<CashFlowForecastDto>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to update forecast"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
