package org.dals.project.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.dals.project.model.Investment
import org.dals.project.model.InvestmentType

@Serializable
data class CreateInvestmentRequest(
    val userId: String,
    val type: InvestmentType,
    val name: String,
    val symbol: String?,
    val amount: Double
)

@Serializable
data class WithdrawInvestmentRequest(
    val investmentId: String,
    val userId: String,
    val amount: Double,
    val paymentMethod: String
)

@Serializable
data class InvestmentApiResponse(
    val id: String,
    val userId: String,
    val type: InvestmentType,
    val name: String,
    val symbol: String?,
    val amount: Double,
    val currentValue: Double,
    val totalReturn: Double,
    val returnPercentage: Double,
    val purchaseDate: String,
    val status: String
)

class InvestmentApiClient(private val baseUrl: String = "https://axiobank-production.up.railway.app") {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }

    suspend fun getInvestments(userId: String): Result<List<Investment>> {
        return try {
            val response: List<InvestmentApiResponse> = client.get("$baseUrl/api/investments/$userId").body()
            Result.success(response.map { it.toInvestment() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInvestmentById(userId: String, investmentId: String): Result<Investment> {
        return try {
            val response: InvestmentApiResponse = client.get("$baseUrl/api/investments/$userId/$investmentId").body()
            Result.success(response.toInvestment())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createInvestment(
        userId: String,
        type: InvestmentType,
        name: String,
        symbol: String?,
        amount: Double
    ): Result<Investment> {
        return try {
            val request = CreateInvestmentRequest(userId, type, name, symbol, amount)
            val response: InvestmentApiResponse = client.post("$baseUrl/api/investments/create") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            Result.success(response.toInvestment())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun withdrawFromInvestment(
        investmentId: String,
        userId: String,
        amount: Double,
        paymentMethod: String
    ): Result<Boolean> {
        return try {
            val request = WithdrawInvestmentRequest(investmentId, userId, amount, paymentMethod)
            client.post("$baseUrl/api/investments/withdraw") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPortfolioSummary(userId: String): Result<Map<String, Any>> {
        return try {
            val response: Map<String, Any> = client.get("$baseUrl/api/investments/$userId/portfolio").body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun InvestmentApiResponse.toInvestment(): Investment {
        val quantity = if (amount > 0 && currentValue > 0) amount / (currentValue / (1 + returnPercentage / 100)) else 1.0
        val currentPrice = if (quantity > 0) currentValue / quantity else currentValue

        return Investment(
            id = id,
            userId = userId,
            type = type,
            name = name,
            symbol = symbol,
            amount = amount,
            currentValue = currentValue,
            purchaseDate = purchaseDate,
            currentPrice = currentPrice,
            quantity = quantity,
            totalReturn = totalReturn,
            returnPercentage = returnPercentage
        )
    }
}
