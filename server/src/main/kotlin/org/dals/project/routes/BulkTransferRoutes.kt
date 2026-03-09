package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.service.BulkTransferRequest
import org.dals.project.service.BulkTransferService

fun Route.bulkTransferRoutes() {
    val bulkTransferService = BulkTransferService()

    route("/bulk-transfer") {
        // Create a new bulk transfer
        post("/create") {
            try {
                val request = call.receive<BulkTransferRequest>()

                println("📦 Creating bulk transfer: ${request.batchName} with ${request.recipients.size} recipients")

                val result = bulkTransferService.createBulkTransfer(request)

                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrThrow())
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to create bulk transfer"))
                    )
                }
            } catch (e: Exception) {
                println("❌ Error in bulk transfer create endpoint: ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Internal server error: ${e.message}")
                )
            }
        }

        // Process a bulk transfer
        post("/process/{bulkTransferId}") {
            try {
                val bulkTransferId = call.parameters["bulkTransferId"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing bulkTransferId"))

                println("⚙️ Processing bulk transfer: $bulkTransferId")

                val result = bulkTransferService.processBulkTransfer(bulkTransferId)

                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrThrow())
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to process bulk transfer"))
                    )
                }
            } catch (e: Exception) {
                println("❌ Error in bulk transfer process endpoint: ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Internal server error: ${e.message}")
                )
            }
        }

        // Get bulk transfer status
        get("/status/{bulkTransferId}") {
            try {
                val bulkTransferId = call.parameters["bulkTransferId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing bulkTransferId"))

                val result = bulkTransferService.getBulkTransferStatus(bulkTransferId)

                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrThrow())
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Bulk transfer not found"))
                    )
                }
            } catch (e: Exception) {
                println("❌ Error in bulk transfer status endpoint: ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Internal server error: ${e.message}")
                )
            }
        }

        // Get all bulk transfers for a customer
        get("/customer/{customerId}") {
            try {
                val customerId = call.parameters["customerId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing customerId"))

                val result = bulkTransferService.getCustomerBulkTransfers(customerId)

                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrThrow())
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to fetch bulk transfers"))
                    )
                }
            } catch (e: Exception) {
                println("❌ Error in bulk transfer customer endpoint: ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Internal server error: ${e.message}")
                )
            }
        }
    }
}
