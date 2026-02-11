package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.dals.project.database.SavingsRepository
import org.dals.project.database.Accounts
import org.dals.project.model.LockPeriod
import org.dals.project.model.SavingsAccount
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.sql.SQLException
import java.util.UUID

@Serializable
data class CreateSavingsRequest(
    val accountName: String,
    val amount: Double,
    val lockPeriod: String
)

@Serializable
data class WithdrawSavingsResponse(
    val success: Boolean,
    val message: String,
    val totalAmount: Double,
    val isMatured: Boolean
)

@Serializable
data class SavingsResponse(
    val success: Boolean,
    val message: String,
    val data: SavingsAccount? = null,
    val savings: List<SavingsAccount>? = null
)

fun Route.savingsRoutes() {
    val savingsRepository = SavingsRepository()

    route("/savings") {

        // GET /api/savings - Get all savings accounts for user
        get {
            try {
                val userId = call.request.queryParameters["userId"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        SavingsResponse(false, "User ID is required")
                    )

                val savings = savingsRepository.getSavingsAccountsByUserId(userId)

                call.respond(
                    HttpStatusCode.OK,
                    SavingsResponse(
                        success = true,
                        message = "Savings accounts retrieved successfully",
                        savings = savings
                    )
                )
            } catch (e: SQLException) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    SavingsResponse(false, "Database error: ${e.message}")
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    SavingsResponse(false, "Error: ${e.message}")
                )
            }
        }

        // GET /api/savings/{id} - Get specific savings account
        get("/{id}") {
            try {
                val savingsId = call.parameters["id"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        SavingsResponse(false, "Savings ID is required")
                    )

                val userId = call.request.queryParameters["userId"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        SavingsResponse(false, "User ID is required")
                    )

                val savings = savingsRepository.getSavingsAccountById(savingsId, userId)
                    ?: return@get call.respond(
                        HttpStatusCode.NotFound,
                        SavingsResponse(false, "Savings account not found")
                    )

                call.respond(
                    HttpStatusCode.OK,
                    SavingsResponse(
                        success = true,
                        message = "Savings account retrieved successfully",
                        data = savings
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    SavingsResponse(false, "Error: ${e.message}")
                )
            }
        }

        // POST /api/savings - Create new savings account
        post {
            try {
                val request = call.receive<CreateSavingsRequest>()
                val userId = call.request.queryParameters["userId"]
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        SavingsResponse(false, "User ID is required")
                    )

                // Validate request
                if (request.accountName.isBlank()) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        SavingsResponse(false, "Account name is required")
                    )
                }

                if (request.amount <= 0) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        SavingsResponse(false, "Amount must be greater than 0")
                    )
                }

                // Parse lock period
                val lockPeriod = try {
                    LockPeriod.valueOf(request.lockPeriod)
                } catch (e: IllegalArgumentException) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        SavingsResponse(false, "Invalid lock period")
                    )
                }

                // Check user balance and deduct amount
                val (hasBalance, newBalance) = transaction {
                    val userUUID = try {
                        UUID.fromString(userId)
                    } catch (e: IllegalArgumentException) {
                        return@transaction Pair(false, 0.0)
                    }

                    val account = Accounts.select { Accounts.customerId eq userUUID }.firstOrNull()
                        ?: return@transaction Pair(false, 0.0)

                    val currentBalance = account[Accounts.balance].toDouble()
                    if (currentBalance < request.amount) {
                        return@transaction Pair(false, currentBalance)
                    }

                    val updatedBalance = currentBalance - request.amount
                    Accounts.update({ Accounts.customerId eq userUUID }) {
                        it[balance] = java.math.BigDecimal(updatedBalance)
                        it[availableBalance] = java.math.BigDecimal(updatedBalance)
                    }

                    Pair(true, updatedBalance)
                }

                if (!hasBalance) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        SavingsResponse(false, "Insufficient balance")
                    )
                }

                // Create savings account
                val savings = savingsRepository.createSavingsAccount(
                    userId = userId,
                    accountName = request.accountName,
                    lockPeriod = lockPeriod,
                    amount = request.amount
                )

                call.respond(
                    HttpStatusCode.Created,
                    SavingsResponse(
                        success = true,
                        message = "Savings account created successfully",
                        data = savings
                    )
                )
            } catch (e: SQLException) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    SavingsResponse(false, "Database error: ${e.message}")
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    SavingsResponse(false, "Error: ${e.message}")
                )
            }
        }

        // POST /api/savings/{id}/withdraw - Withdraw from savings account
        post("/{id}/withdraw") {
            try {
                val savingsId = call.parameters["id"]
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        WithdrawSavingsResponse(false, "Savings ID is required", 0.0, false)
                    )

                val userId = call.request.queryParameters["userId"]
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        WithdrawSavingsResponse(false, "User ID is required", 0.0, false)
                    )

                // Withdraw from savings
                val (totalAmount, isMatured) = savingsRepository.withdrawSavings(savingsId, userId)

                // Add amount back to user balance
                transaction {
                    val userUUID = try {
                        UUID.fromString(userId)
                    } catch (e: IllegalArgumentException) {
                        throw IllegalArgumentException("Invalid user ID format")
                    }

                    val account = Accounts.select { Accounts.customerId eq userUUID }.firstOrNull()
                        ?: throw IllegalArgumentException("Account not found")

                    val currentBalance = account[Accounts.balance].toDouble()
                    val newBalance = currentBalance + totalAmount

                    Accounts.update({ Accounts.customerId eq userUUID }) {
                        it[balance] = java.math.BigDecimal(newBalance)
                        it[availableBalance] = java.math.BigDecimal(newBalance)
                    }
                }

                val message = if (isMatured) {
                    "Savings matured and withdrawn successfully. Amount: $${"%.2f".format(totalAmount)}"
                } else {
                    "Savings withdrawn early with penalty. Amount: $${"%.2f".format(totalAmount)}"
                }

                call.respond(
                    HttpStatusCode.OK,
                    WithdrawSavingsResponse(
                        success = true,
                        message = message,
                        totalAmount = totalAmount,
                        isMatured = isMatured
                    )
                )
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.NotFound,
                    WithdrawSavingsResponse(false, e.message ?: "Savings account not found", 0.0, false)
                )
            } catch (e: IllegalStateException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    WithdrawSavingsResponse(false, e.message ?: "Invalid savings state", 0.0, false)
                )
            } catch (e: SQLException) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    WithdrawSavingsResponse(false, "Database error: ${e.message}", 0.0, false)
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    WithdrawSavingsResponse(false, "Error: ${e.message}", 0.0, false)
                )
            }
        }
    }
}
