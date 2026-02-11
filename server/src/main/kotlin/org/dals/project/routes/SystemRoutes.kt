package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.services.AdminSystemService

/**
 * System configuration and maintenance routes
 */
fun Route.systemRoutes() {
    val systemService = AdminSystemService()

    route("/admin/system") {
        /**
         * GET /admin/system/configurations
         * Get all system configurations
         */
        get("/configurations") {
            try {
                val response = systemService.getSystemConfigurations()
                call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Error retrieving configurations: ${e.message}"
                ))
            }
        }

        /**
         * PUT /admin/system/configurations/{configId}
         * Update system configuration
         */
        put("/configurations/{configId}") {
            try {
                val configId = call.parameters["configId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("success" to false, "message" to "Config ID is required")
                )

                val requestBody = call.receive<Map<String, String>>()
                val newValue = requestBody["value"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("success" to false, "message" to "Value is required")
                )

                val response = systemService.updateSystemConfiguration(configId, newValue)
                call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf(
                    "success" to false,
                    "message" to "Error updating configuration: ${e.message}"
                ))
            }
        }

        /**
         * GET /admin/system/info
         * Get system information
         */
        get("/info") {
            try {
                val response = systemService.getSystemInformation()
                call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Error retrieving system information: ${e.message}"
                ))
            }
        }

        /**
         * POST /admin/system/backup
         * Create system backup
         * Request body: { "backupType": "FULL" | "DATABASE_ONLY" | "INCREMENTAL" }
         */
        post("/backup") {
            try {
                val requestBody = call.receive<Map<String, String>>()
                val backupType = requestBody["backupType"] ?: "FULL"

                val response = systemService.createBackup(backupType)
                call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Error creating backup: ${e.message}"
                ))
            }
        }

        /**
         * GET /admin/system/backups
         * Get backup history
         */
        get("/backups") {
            try {
                val response = systemService.getBackupHistory()
                call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Error retrieving backup history: ${e.message}"
                ))
            }
        }
    }
}
