package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.models.*
import org.dals.project.services.CustomerCareService
import org.dals.project.services.TransactionService

fun Route.transactionReversalRoutes() {
    val customerCareService = CustomerCareService()
    val transactionService = TransactionService()

    // Search transaction by reference
    route("/api/v1/transactions") {
        // Get all transactions with optional filters
        get {
            try {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 50
                val accountNumber = call.request.queryParameters["accountNumber"]
                val status = call.request.queryParameters["status"]
                val startDate = call.request.queryParameters["startDate"]
                val endDate = call.request.queryParameters["endDate"]

                val transactions = transactionService.getAllTransactions(
                    page = page,
                    pageSize = pageSize,
                    accountNumber = accountNumber,
                    status = status,
                    startDate = startDate,
                    endDate = endDate
                )

                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to true,
                    "message" to "Transactions retrieved successfully",
                    "data" to transactions.data,
                    "total" to transactions.total,
                    "page" to page,
                    "pageSize" to pageSize
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Failed to retrieve transactions: ${e.message}"
                ))
            }
        }

        get("/reference/{reference}") {
            try {
                val reference = call.parameters["reference"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "message" to "Transaction reference is required"
                    ))

                println("🔍 Searching for transaction with reference: $reference")
                val transaction = transactionService.getTransactionByReference(reference)
                println("📄 Transaction data result: ${if (transaction != null) "FOUND" else "NOT FOUND"}")

                if (transaction != null) {
                    // Enhance transaction with customer and balance information
                    println("🔄 Enhancing transaction data...")
                    val enhancedData = transactionService.getEnhancedTransactionDetails(transaction)
                    println("✅ Enhanced data keys: ${enhancedData.keys}")

                    println("📤 Sending transaction response: ${mapOf(
                        "success" to true,
                        "message" to "Transaction found",
                        "data" to enhancedData
                    )}")

                    call.respond(HttpStatusCode.OK, mapOf(
                        "success" to true,
                        "message" to "Transaction found",
                        "data" to enhancedData
                    ))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf(
                        "success" to false,
                        "message" to "Transaction not found with reference: $reference"
                    ))
                }
            } catch (e: Exception) {
                println("❌ Error searching transaction: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Failed to search transaction: ${e.message}"
                ))
            }
        }
    }

    route("/api/customer-care/transaction-reversals") {

        // Create a new transaction reversal request
        post {
            try {
                val request = call.receive<CreateTransactionReversalRequest>()
                val response = customerCareService.createTransactionReversal(request)

                if (response.success) {
                    call.respond(HttpStatusCode.Created, response)
                } else {
                    call.respond(HttpStatusCode.BadRequest, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    CustomerCareResponse<TransactionReversalDto>(
                        success = false,
                        message = "Failed to create transaction reversal request",
                        error = e.message,
                        timestamp = java.time.LocalDateTime.now().toString()
                    )
                )
            }
        }

        // Get all reversal requests (for customer care agents)
        get {
            try {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 50
                val status = call.request.queryParameters["status"]

                val response = if (status != null) {
                    customerCareService.getReversalRequestsByStatus(status, page, pageSize)
                } else {
                    customerCareService.getAllReversalRequests(page, pageSize)
                }

                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    CustomerCareListResponse<TransactionReversalDto>(
                        success = false,
                        message = "Failed to retrieve reversal requests",
                        data = emptyList(),
                        total = 0,
                        page = 1,
                        pageSize = 50,
                        timestamp = java.time.LocalDateTime.now().toString()
                    )
                )
            }
        }

        // Get reversal requests by customer ID
        get("/customer/{customerId}") {
            try {
                val customerId = call.parameters["customerId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Customer ID is required")

                val response = customerCareService.getReversalRequestsByCustomerId(customerId)
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    CustomerCareResponse<List<TransactionReversalDto>>(
                        success = false,
                        message = "Failed to retrieve reversal requests",
                        error = e.message,
                        timestamp = java.time.LocalDateTime.now().toString()
                    )
                )
            }
        }

        // Review a reversal request (approve/reject) - Customer Care Agent only
        post("/review") {
            try {
                val review = call.receive<ReviewTransactionReversalRequest>()
                // TODO: Extract reviewer ID from authenticated session
                val reviewerId = call.request.headers["X-User-Id"]
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, "User ID not found in headers")

                val response = customerCareService.reviewReversalRequest(review, reviewerId)

                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.BadRequest, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    CustomerCareResponse<TransactionReversalDto>(
                        success = false,
                        message = "Failed to review reversal request",
                        error = e.message,
                        timestamp = java.time.LocalDateTime.now().toString()
                    )
                )
            }
        }

        // Complete a reversal request - Customer Care Agent only
        post("/complete") {
            try {
                val complete = call.receive<CompleteTransactionReversalRequest>()
                // TODO: Extract completer ID from authenticated session
                val completerId = call.request.headers["X-User-Id"]
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, "User ID not found in headers")

                val response = customerCareService.completeReversalRequest(complete, completerId)

                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.BadRequest, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    CustomerCareResponse<TransactionReversalDto>(
                        success = false,
                        message = "Failed to complete reversal request",
                        error = e.message,
                        timestamp = java.time.LocalDateTime.now().toString()
                    )
                )
            }
        }
    }
}
