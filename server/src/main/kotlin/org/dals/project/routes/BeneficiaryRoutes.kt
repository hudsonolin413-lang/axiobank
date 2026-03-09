package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.services.*
import java.util.UUID

fun Route.beneficiaryRoutes() {
    val beneficiaryService = BeneficiaryService()

    route("/beneficiaries") {
        // Get all beneficiaries for a customer
        get("/{customerId}") {
            try {
                val customerId = UUID.fromString(call.parameters["customerId"])
                val beneficiaries = beneficiaryService.getAllBeneficiaries(customerId)
                call.respond(HttpStatusCode.OK, beneficiaries)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid customer ID")))
            }
        }

        // Get favorite beneficiaries
        get("/{customerId}/favorites") {
            try {
                val customerId = UUID.fromString(call.parameters["customerId"])
                val favorites = beneficiaryService.getFavoriteBeneficiaries(customerId)
                call.respond(HttpStatusCode.OK, favorites)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid customer ID")))
            }
        }

        // Search beneficiaries
        get("/{customerId}/search") {
            try {
                val customerId = UUID.fromString(call.parameters["customerId"])
                val query = call.request.queryParameters["q"] ?: ""
                val results = beneficiaryService.searchBeneficiaries(customerId, query)
                call.respond(HttpStatusCode.OK, results)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Search failed")))
            }
        }

        // Get beneficiary by ID
        get("/detail/{beneficiaryId}") {
            try {
                val beneficiaryId = UUID.fromString(call.parameters["beneficiaryId"])
                val beneficiary = beneficiaryService.getBeneficiaryById(beneficiaryId)
                if (beneficiary != null) {
                    call.respond(HttpStatusCode.OK, beneficiary)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Beneficiary not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid beneficiary ID")))
            }
        }

        // Create new beneficiary
        post {
            try {
                val request = call.receive<CreateBeneficiaryRequest>()
                val beneficiary = beneficiaryService.createBeneficiary(request)
                call.respond(HttpStatusCode.Created, beneficiary)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Failed to create beneficiary")))
            }
        }

        // Update beneficiary
        put("/{beneficiaryId}") {
            try {
                val beneficiaryId = UUID.fromString(call.parameters["beneficiaryId"])
                val request = call.receive<UpdateBeneficiaryRequest>()
                val updated = beneficiaryService.updateBeneficiary(beneficiaryId, request)
                if (updated != null) {
                    call.respond(HttpStatusCode.OK, updated)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Beneficiary not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Failed to update beneficiary")))
            }
        }

        // Toggle favorite status
        post("/{beneficiaryId}/toggle-favorite") {
            try {
                val beneficiaryId = UUID.fromString(call.parameters["beneficiaryId"])
                val updated = beneficiaryService.toggleFavorite(beneficiaryId)
                if (updated != null) {
                    call.respond(HttpStatusCode.OK, updated)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Beneficiary not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Failed to toggle favorite")))
            }
        }

        // Delete beneficiary
        delete("/{beneficiaryId}") {
            try {
                val beneficiaryId = UUID.fromString(call.parameters["beneficiaryId"])
                val deleted = beneficiaryService.deleteBeneficiary(beneficiaryId)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Beneficiary deleted successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Beneficiary not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Failed to delete beneficiary")))
            }
        }

        // Record beneficiary usage (called when a transfer is made)
        post("/{beneficiaryId}/record-usage") {
            try {
                val beneficiaryId = UUID.fromString(call.parameters["beneficiaryId"])
                beneficiaryService.recordBeneficiaryUsage(beneficiaryId)
                call.respond(HttpStatusCode.OK, mapOf("message" to "Usage recorded"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Failed to record usage")))
            }
        }
    }
}
