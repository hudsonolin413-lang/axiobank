package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.services.EnrollOverdraftProtectionRequest
import org.dals.project.services.OverdraftProtectionService

fun Route.overdraftProtectionRoutes() {
    route("/overdraft-protection") {

        // Enroll in overdraft protection
        post("/enroll") {
            try {
                val request = call.receive<EnrollOverdraftProtectionRequest>()
                val result = OverdraftProtectionService.enrollOverdraftProtection(request)

                if (result.isSuccess) {
                    call.respond(HttpStatusCode.Created, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to enroll in overdraft protection"))
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Internal server error"))
                )
            }
        }

        // Get overdraft protection for account
        get("/account/{accountId}") {
            try {
                val accountId = call.parameters["accountId"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Account ID is required")
                    )

                val result = OverdraftProtectionService.getOverdraftProtection(accountId)

                if (result.isSuccess) {
                    val protection = result.getOrNull()
                    if (protection != null) {
                        call.respond(HttpStatusCode.OK, protection)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            mapOf("error" to "No overdraft protection found for this account")
                        )
                    }
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to get overdraft protection"))
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Internal server error"))
                )
            }
        }

        // Get all overdraft protections for customer
        get("/customer/{customerId}") {
            try {
                val customerId = call.parameters["customerId"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Customer ID is required")
                    )

                val result = OverdraftProtectionService.getAllOverdraftProtections(customerId)

                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to get overdraft protections"))
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Internal server error"))
                )
            }
        }

        // Get overdraft transactions
        get("/transactions/{accountId}") {
            try {
                val accountId = call.parameters["accountId"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Account ID is required")
                    )

                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50

                val result = OverdraftProtectionService.getOverdraftTransactions(accountId, limit)

                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to get transactions"))
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Internal server error"))
                )
            }
        }

        // Get overdraft usage stats
        get("/stats/{customerId}") {
            try {
                val customerId = call.parameters["customerId"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Customer ID is required")
                    )

                val result = OverdraftProtectionService.getOverdraftUsageStats(customerId)

                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to get usage stats"))
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Internal server error"))
                )
            }
        }

        // Cancel overdraft protection
        post("/cancel/{protectionId}") {
            try {
                val protectionId = call.parameters["protectionId"]
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Protection ID is required")
                    )

                val result = OverdraftProtectionService.cancelOverdraftProtection(protectionId)

                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to cancel overdraft protection"))
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
