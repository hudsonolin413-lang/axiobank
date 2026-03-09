package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.services.*
import java.util.UUID

fun Route.subAccountRoutes() {
    val subAccountService = SubAccountService()

    route("/sub-accounts") {
        // Get all sub-accounts for a customer
        get("/{customerId}") {
            try {
                val customerId = UUID.fromString(call.parameters["customerId"])
                val subAccounts = subAccountService.getAllSubAccounts(customerId)
                call.respond(HttpStatusCode.OK, subAccounts)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid customer ID")))
            }
        }

        // Get active sub-accounts only
        get("/{customerId}/active") {
            try {
                val customerId = UUID.fromString(call.parameters["customerId"])
                val subAccounts = subAccountService.getActiveSubAccounts(customerId)
                call.respond(HttpStatusCode.OK, subAccounts)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid customer ID")))
            }
        }

        // Get sub-account by ID
        get("/detail/{subAccountId}") {
            try {
                val subAccountId = UUID.fromString(call.parameters["subAccountId"])
                val subAccount = subAccountService.getSubAccountById(subAccountId)
                if (subAccount != null) {
                    call.respond(HttpStatusCode.OK, subAccount)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Sub-account not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid sub-account ID")))
            }
        }

        // Create new sub-account
        post {
            try {
                val request = call.receive<CreateSubAccountRequest>()
                val subAccount = subAccountService.createSubAccount(request)
                call.respond(HttpStatusCode.Created, subAccount)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Failed to create sub-account")))
            }
        }

        // Update sub-account
        put("/{subAccountId}") {
            try {
                val subAccountId = UUID.fromString(call.parameters["subAccountId"])
                val request = call.receive<UpdateSubAccountRequest>()
                val updated = subAccountService.updateSubAccount(subAccountId, request)
                if (updated != null) {
                    call.respond(HttpStatusCode.OK, updated)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Sub-account not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Failed to update sub-account")))
            }
        }

        // Delete sub-account
        delete("/{subAccountId}") {
            try {
                val subAccountId = UUID.fromString(call.parameters["subAccountId"])
                val deleted = subAccountService.deleteSubAccount(subAccountId)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Sub-account deleted successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Sub-account not found"))
                }
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Failed to delete sub-account")))
            }
        }

        // Transfer money to sub-account
        post("/transfer") {
            try {
                val request = call.receive<TransferToSubAccountRequest>()
                val result = subAccountService.transferToSubAccount(request)
                call.respond(HttpStatusCode.OK, result)
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to e.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Transfer failed")))
            }
        }

        // Withdraw money from sub-account
        post("/withdraw") {
            try {
                val request = call.receive<TransferToSubAccountRequest>()
                val result = subAccountService.withdrawFromSubAccount(request)
                call.respond(HttpStatusCode.OK, result)
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to e.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Withdrawal failed")))
            }
        }

        // Toggle lock status
        post("/{subAccountId}/toggle-lock") {
            try {
                val subAccountId = UUID.fromString(call.parameters["subAccountId"])
                val updated = subAccountService.toggleLock(subAccountId)
                if (updated != null) {
                    call.respond(HttpStatusCode.OK, updated)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Sub-account not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Failed to toggle lock")))
            }
        }
    }
}
