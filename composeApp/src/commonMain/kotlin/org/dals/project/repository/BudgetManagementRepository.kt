package org.dals.project.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class BudgetDto(
    val id: String,
    val customerId: String,
    val category: String,
    val amount: Double,
    val spentAmount: Double,
    val period: String,
    val startDate: String,
    val endDate: String,
    val status: String,
    val createdAt: String
)

@Serializable
data class CreateBudgetRequest(
    val customerId: String,
    val category: String,
    val amount: Double,
    val period: String,
    val startDate: String,
    val endDate: String
)

class BudgetManagementRepository(private val client: HttpClient, private val baseUrl: String) {

    suspend fun getBudgets(customerId: String): Result<List<BudgetDto>> {
        return try {
            val response = client.get("$baseUrl/api/v1/budget/customer/$customerId")
            if (response.status.isSuccess()) {
                Result.success(response.body<List<BudgetDto>>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to get budgets"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createBudget(request: CreateBudgetRequest): Result<BudgetDto> {
        return try {
            val response = client.post("$baseUrl/api/v1/budget/create") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<BudgetDto>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to create budget"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBudget(budgetId: String): Result<Unit> {
        return try {
            val response = client.delete("$baseUrl/api/v1/budget/$budgetId")
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to delete budget"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
