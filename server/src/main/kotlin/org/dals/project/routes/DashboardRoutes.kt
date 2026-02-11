package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.services.AdminDashboardService

/**
 * Dashboard routes for admin metrics and system health
 */
fun Route.dashboardRoutes() {
    val dashboardService = AdminDashboardService()

    route("/admin/dashboard") {
        /**
         * GET /admin/dashboard/metrics
         * Get comprehensive dashboard metrics from real-time database
         */
        get("/metrics") {
            try {
                val response = dashboardService.getDashboardMetrics()
                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, response)
                }
            } catch (e: Exception) {
                println("Error in /admin/dashboard/metrics: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Error retrieving dashboard metrics: ${e.message}"
                ))
            }
        }

        /**
         * GET /admin/dashboard/health
         * Get system health status with real-time metrics
         */
        get("/health") {
            try {
                val response = dashboardService.getSystemHealth()
                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, response)
                }
            } catch (e: Exception) {
                println("Error in /admin/dashboard/health: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Error retrieving system health: ${e.message}"
                ))
            }
        }

        /**
         * GET /admin/dashboard/branches
         * Get all branches with real-time data
         */
        get("/branches") {
            try {
                val response = dashboardService.getAllBranches()
                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, response)
                }
            } catch (e: Exception) {
                println("Error in /admin/dashboard/branches: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Error retrieving branches: ${e.message}"
                ))
            }
        }
    }
}
