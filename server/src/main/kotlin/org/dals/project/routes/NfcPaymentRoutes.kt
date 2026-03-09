package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.services.InitiateNfcPaymentRequest
import org.dals.project.services.NfcPaymentService

fun Route.nfcPaymentRoutes() {
    route("/nfc-payment") {

        // Initiate NFC payment
        post("/initiate") {
            try {
                val request = call.receive<InitiateNfcPaymentRequest>()
                val result = NfcPaymentService.initiateNfcPayment(request)

                if (result.isSuccess) {
                    call.respond(HttpStatusCode.Created, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to initiate NFC payment"))
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Internal server error"))
                )
            }
        }

        // Process NFC payment
        post("/process/{id}") {
            try {
                val nfcPaymentId = call.parameters["id"]
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "NFC payment ID is required")
                    )

                val result = NfcPaymentService.processNfcPayment(nfcPaymentId)

                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to process NFC payment"))
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Internal server error"))
                )
            }
        }

        // Get NFC payment by ID
        get("/{id}") {
            try {
                val nfcPaymentId = call.parameters["id"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "NFC payment ID is required")
                    )

                val result = NfcPaymentService.getNfcPaymentById(nfcPaymentId)

                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "NFC payment not found"))
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Internal server error"))
                )
            }
        }

        // Get NFC payment history for customer
        get("/history/{customerId}") {
            try {
                val customerId = call.parameters["customerId"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Customer ID is required")
                    )

                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

                val result = NfcPaymentService.getNfcPaymentHistory(customerId, limit, offset)

                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to get payment history"))
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Internal server error"))
                )
            }
        }

        // Cancel NFC payment
        post("/cancel/{id}") {
            try {
                val nfcPaymentId = call.parameters["id"]
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "NFC payment ID is required")
                    )

                val result = NfcPaymentService.cancelNfcPayment(nfcPaymentId)

                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to cancel NFC payment"))
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Internal server error"))
                )
            }
        }

        // Get active NFC payments
        get("/active/{customerId}") {
            try {
                val customerId = call.parameters["customerId"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Customer ID is required")
                    )

                val result = NfcPaymentService.getActiveNfcPayments(customerId)

                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to get active payments"))
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Internal server error"))
                )
            }
        }
    }
}
