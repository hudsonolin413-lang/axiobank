package org.dals.project.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.dals.project.database.InvestmentRepository
import org.dals.project.model.InvestmentType
import java.util.*

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
data class InvestmentResponse(
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

fun Route.investmentRoutes(investmentRepository: InvestmentRepository) {

    route("/api/investments") {

        // Get all investments for a user
        get("/{userId}") {
            val userId = call.parameters["userId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "User ID is required")
            )

            try {
                val investments = investmentRepository.getInvestmentsByUserId(userId)
                call.respond(HttpStatusCode.OK, investments)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to fetch investments: ${e.message}")
                )
            }
        }

        // Get a specific investment
        get("/{userId}/{investmentId}") {
            val userId = call.parameters["userId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "User ID is required")
            )
            val investmentId = call.parameters["investmentId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Investment ID is required")
            )

            try {
                val investment = investmentRepository.getInvestmentById(investmentId, userId)
                if (investment != null) {
                    call.respond(HttpStatusCode.OK, investment)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Investment not found")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to fetch investment: ${e.message}")
                )
            }
        }

        // Create a new investment
        post("/create") {
            try {
                val request = call.receive<CreateInvestmentRequest>()

                // Validate request
                if (request.amount <= 0) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Investment amount must be greater than 0")
                    )
                }

                val investment = investmentRepository.createInvestment(
                    userId = request.userId,
                    type = request.type,
                    name = request.name,
                    symbol = request.symbol,
                    amount = request.amount
                )

                call.respond(HttpStatusCode.Created, investment)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to create investment: ${e.message}")
                )
            }
        }

        // Withdraw from an investment
        post("/withdraw") {
            try {
                val request = call.receive<WithdrawInvestmentRequest>()

                // Validate request
                if (request.amount <= 0) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Withdrawal amount must be greater than 0")
                    )
                }

                val result = investmentRepository.withdrawFromInvestment(
                    investmentId = request.investmentId,
                    userId = request.userId,
                    amount = request.amount
                )

                if (result) {
                    call.respond(
                        HttpStatusCode.OK,
                        mapOf("message" to "Withdrawal successful", "amount" to request.amount)
                    )
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Insufficient funds or invalid investment")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to process withdrawal: ${e.message}")
                )
            }
        }

        // Get investment portfolio summary
        get("/{userId}/portfolio") {
            val userId = call.parameters["userId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "User ID is required")
            )

            try {
                val investments = investmentRepository.getInvestmentsByUserId(userId)
                val totalValue = investments.sumOf { it.currentValue }
                val totalInvested = investments.sumOf { it.amount }
                val totalReturn = totalValue - totalInvested
                val returnPercentage = if (totalInvested > 0) (totalReturn / totalInvested) * 100 else 0.0

                val portfolioSummary = mapOf(
                    "totalValue" to totalValue,
                    "totalInvested" to totalInvested,
                    "totalReturn" to totalReturn,
                    "returnPercentage" to returnPercentage,
                    "investmentCount" to investments.size,
                    "investments" to investments
                )

                call.respond(HttpStatusCode.OK, portfolioSummary)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to fetch portfolio summary: ${e.message}")
                )
            }
        }

        // Update investment value (simulated market changes)
        put("/{investmentId}/update-value") {
            val investmentId = call.parameters["investmentId"] ?: return@put call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Investment ID is required")
            )

            try {
                val body = call.receive<Map<String, Double>>()
                val newValue = body["currentValue"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Current value is required")
                )

                val updated = investmentRepository.updateInvestmentValue(investmentId, newValue)
                if (updated) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Investment value updated"))
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Investment not found")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to update investment value: ${e.message}")
                )
            }
        }

        // Delete an investment
        delete("/{userId}/{investmentId}") {
            val userId = call.parameters["userId"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "User ID is required")
            )
            val investmentId = call.parameters["investmentId"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Investment ID is required")
            )

            try {
                val deleted = investmentRepository.deleteInvestment(investmentId, userId)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Investment deleted"))
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Investment not found")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to delete investment: ${e.message}")
                )
            }
        }
    }
}
