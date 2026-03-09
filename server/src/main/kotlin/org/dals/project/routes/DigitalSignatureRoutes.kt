package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.service.CreateSignatureRequest
import org.dals.project.service.DigitalSignatureService

fun Route.digitalSignatureRoutes() {
    val signatureService = DigitalSignatureService()

    route("/digital-signature") {
        // Create a new signature
        post("/create") {
            try {
                val request = call.receive<CreateSignatureRequest>()

                println("📝 Creating digital signature for document: ${request.documentName}")

                val result = signatureService.createSignature(request)

                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrThrow())
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to create signature"))
                    )
                }
            } catch (e: Exception) {
                println("❌ Error in signature create endpoint: ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Internal server error: ${e.message}")
                )
            }
        }

        // Get signature by ID
        get("/{signatureId}") {
            try {
                val signatureId = call.parameters["signatureId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing signatureId"))

                val result = signatureService.getSignatureById(signatureId)

                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrThrow())
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Signature not found"))
                    )
                }
            } catch (e: Exception) {
                println("❌ Error in signature get endpoint: ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Internal server error: ${e.message}")
                )
            }
        }

        // Get all signatures for a customer
        get("/customer/{customerId}") {
            try {
                val customerId = call.parameters["customerId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing customerId"))

                val result = signatureService.getCustomerSignatures(customerId)

                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrThrow())
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to fetch signatures"))
                    )
                }
            } catch (e: Exception) {
                println("❌ Error in customer signatures endpoint: ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Internal server error: ${e.message}")
                )
            }
        }

        // Verify a signature
        get("/verify/{signatureId}") {
            try {
                val signatureId = call.parameters["signatureId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing signatureId"))

                val result = signatureService.verifySignature(signatureId)

                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, mapOf("isValid" to result.getOrThrow()))
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to verify signature"))
                    )
                }
            } catch (e: Exception) {
                println("❌ Error in signature verify endpoint: ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Internal server error: ${e.message}")
                )
            }
        }

        // Invalidate a signature
        post("/invalidate/{signatureId}") {
            try {
                val signatureId = call.parameters["signatureId"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing signatureId"))

                val result = signatureService.invalidateSignature(signatureId)

                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, mapOf("success" to true))
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to invalidate signature"))
                    )
                }
            } catch (e: Exception) {
                println("❌ Error in signature invalidate endpoint: ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Internal server error: ${e.message}")
                )
            }
        }

        // Get signatures by document ID
        get("/document/{documentId}") {
            try {
                val documentId = call.parameters["documentId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing documentId"))

                val result = signatureService.getSignaturesByDocument(documentId)

                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrThrow())
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to fetch signatures"))
                    )
                }
            } catch (e: Exception) {
                println("❌ Error in document signatures endpoint: ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Internal server error: ${e.message}")
                )
            }
        }
    }
}
