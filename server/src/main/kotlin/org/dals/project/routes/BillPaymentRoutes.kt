package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.dals.project.services.BillPaymentService
import org.dals.project.services.TransactionService
import java.util.UUID

@Serializable
data class BillPaymentApiResponse(
    val success: Boolean,
    val message: String,
    val data: kotlinx.serialization.json.JsonElement? = null
)

fun Route.billPaymentRoutes() {
    val transactionService = TransactionService()
    val billPaymentService = BillPaymentService(transactionService)

    route("/bill-payment") {

        // GET /api/bill-payment/vendors - Get all vendors
        get("/vendors") {
            try {
                val category = call.request.queryParameters["category"]
                val vendors = billPaymentService.getAllVendors(category)

                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "success" to true,
                        "message" to "Vendors retrieved successfully",
                        "vendors" to vendors
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "success" to false,
                        "message" to "Error: ${e.message}"
                    )
                )
            }
        }

        // GET /api/bill-payment/categories - Get vendor categories
        get("/categories") {
            try {
                val categories = billPaymentService.getVendorCategories()

                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "success" to true,
                        "message" to "Categories retrieved successfully",
                        "categories" to categories
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "success" to false,
                        "message" to "Error: ${e.message}"
                    )
                )
            }
        }

        // GET /api/bill-payment/saved-billers - Get saved billers
        get("/saved-billers") {
            try {
                val userId = call.request.queryParameters["userId"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("success" to false, "message" to "User ID is required")
                    )

                val billers = billPaymentService.getSavedBillers(UUID.fromString(userId))

                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "success" to true,
                        "message" to "Saved billers retrieved successfully",
                        "billers" to billers
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "success" to false,
                        "message" to "Error: ${e.message}"
                    )
                )
            }
        }

        // POST /api/bill-payment/pay - Pay a bill
        post("/pay") {
            try {
                val request = call.receive<org.dals.project.services.PayBillRequest>()
                val response = billPaymentService.payBill(request)

                call.respond(
                    if (response.success) HttpStatusCode.OK else HttpStatusCode.BadRequest,
                    response
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    org.dals.project.services.PayBillResponse(
                        success = false,
                        message = "Error: ${e.message}"
                    )
                )
            }
        }

        // GET /api/bill-payment/history - Get payment history
        get("/history") {
            try {
                val userId = call.request.queryParameters["userId"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("success" to false, "message" to "User ID is required")
                    )

                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50

                val history = billPaymentService.getPaymentHistory(UUID.fromString(userId), limit)

                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "success" to true,
                        "message" to "Payment history retrieved successfully",
                        "payments" to history
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "success" to false,
                        "message" to "Error: ${e.message}"
                    )
                )
            }
        }
    }
}
