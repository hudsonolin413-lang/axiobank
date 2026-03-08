package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.services.QRPaymentRequest
import org.dals.project.services.QRPaymentService
import java.util.*

fun Route.qrPaymentRoutes() {
    route("/qr-payment") {

        /**
         * GET /api/v1/qr-payment/generate/{customerId}
         * Generate QR code data for a customer's account
         */
        get("/generate/{customerId}") {
            try {
                val customerIdStr = call.parameters["customerId"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Customer ID is required"))
                    return@get
                }

                val customerId = try {
                    UUID.fromString(customerIdStr)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid customer ID format"))
                    return@get
                }

                val qrCodeData = QRPaymentService.generateQRCodeData(customerId)

                if (qrCodeData != null) {
                    call.respond(HttpStatusCode.OK, qrCodeData)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Customer or account not found")
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to generate QR code: ${e.message}")
                )
            }
        }

        /**
         * POST /api/qr-payment/validate
         * Validate QR code data before processing payment
         */
        post("/validate") {
            try {
                val request = call.receive<Map<String, String>>()
                val qrData = request["qrData"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "QR data is required"))
                    return@post
                }

                val response = QRPaymentService.validateQRCode(qrData)
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to validate QR code: ${e.message}")
                )
            }
        }

        /**
         * POST /api/qr-payment/process
         * Process a QR code payment transaction
         */
        post("/process") {
            try {
                val request = call.receive<QRPaymentRequest>()

                // Validate request
                if (request.fromCustomerId.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Customer ID is required"))
                    return@post
                }

                if (request.qrData.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "QR data is required"))
                    return@post
                }

                if (request.amount.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Amount is required"))
                    return@post
                }

                // Process payment
                val response = QRPaymentService.processQRPayment(request)

                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.BadRequest, response)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "success" to false,
                        "message" to "Failed to process payment: ${e.message}"
                    )
                )
            }
        }
    }
}
