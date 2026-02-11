package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.services.AdminAlertsService

/**
 * Alerts routes for system and compliance alerts
 */
fun Route.alertsRoutes() {
    val alertsService = AdminAlertsService()

    route("/admin/alerts") {
        /**
         * GET /admin/alerts/system
         * Get all system alerts from database
         */
        get("/system") {
            try {
                val response = alertsService.getSystemAlerts()
                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, response)
                }
            } catch (e: Exception) {
                println("Error in /admin/alerts/system: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Error retrieving system alerts: ${e.message}"
                ))
            }
        }

        /**
         * GET /admin/alerts/compliance
         * Get compliance alerts from multiple sources
         */
        get("/compliance") {
            try {
                val response = alertsService.getComplianceAlerts()
                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, response)
                }
            } catch (e: Exception) {
                println("Error in /admin/alerts/compliance: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Error retrieving compliance alerts: ${e.message}"
                ))
            }
        }

        /**
         * POST /admin/alerts/system/{alertId}/resolve
         * Resolve a system alert
         */
        post("/system/{alertId}/resolve") {
            try {
                val alertId = call.parameters["alertId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("success" to false, "message" to "Alert ID is required")
                )

                val requestBody = call.receive<Map<String, String>>()
                val resolution = requestBody["resolution"] ?: ""

                val response = alertsService.resolveSystemAlert(alertId, resolution)
                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, response)
                }
            } catch (e: Exception) {
                println("Error in /admin/alerts/system/resolve: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Error resolving system alert: ${e.message}"
                ))
            }
        }

        /**
         * POST /admin/alerts/compliance/{alertId}/review
         * Review a compliance alert
         */
        post("/compliance/{alertId}/review") {
            try {
                val alertId = call.parameters["alertId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("success" to false, "message" to "Alert ID is required")
                )

                val requestBody = call.receive<Map<String, String>>()
                val status = requestBody["status"] ?: "PENDING"
                val comments = requestBody["comments"] ?: ""

                val response = alertsService.reviewComplianceAlert(alertId, status, comments)
                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, response)
                }
            } catch (e: Exception) {
                println("Error in /admin/alerts/compliance/review: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Error reviewing compliance alert: ${e.message}"
                ))
            }
        }
    }
}
