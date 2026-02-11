package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.services.AdminAuditService

/**
 * Audit routes for logs and system monitoring
 */
fun Route.auditRoutes() {
    val auditService = AdminAuditService()

    route("/admin/audit") {
        /**
         * GET /admin/audit/logs
         * Get audit logs with optional filters
         * Query parameters: startDate, endDate, userId, limit
         */
        get("/logs") {
            try {
                val startDate = call.request.queryParameters["startDate"]
                val endDate = call.request.queryParameters["endDate"]
                val userId = call.request.queryParameters["userId"]
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100

                val response = auditService.getAuditLogs(startDate, endDate, userId, limit)
                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, response)
                }
            } catch (e: Exception) {
                println("Error in /admin/audit/logs: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Error retrieving audit logs: ${e.message}"
                ))
            }
        }

        /**
         * GET /admin/audit/logs/export
         * Export audit logs to CSV or JSON
         * Query parameters: startDate, endDate, format
         */
        get("/logs/export") {
            try {
                val startDate = call.request.queryParameters["startDate"] ?: ""
                val endDate = call.request.queryParameters["endDate"] ?: ""
                val format = call.request.queryParameters["format"] ?: "CSV"

                val response = auditService.exportAuditLogs(startDate, endDate, format)
                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, response)
                }
            } catch (e: Exception) {
                println("Error in /admin/audit/logs/export: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Error exporting audit logs: ${e.message}"
                ))
            }
        }

        /**
         * GET /admin/audit/system-logs
         * Get system logs from audit trail and real-time metrics
         * Query parameters: level, limit
         */
        get("/system-logs") {
            try {
                val level = call.request.queryParameters["level"]
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50

                val response = auditService.getSystemLogs(level, limit)
                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, response)
                }
            } catch (e: Exception) {
                println("Error in /admin/audit/system-logs: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Error retrieving system logs: ${e.message}"
                ))
            }
        }
    }
}
