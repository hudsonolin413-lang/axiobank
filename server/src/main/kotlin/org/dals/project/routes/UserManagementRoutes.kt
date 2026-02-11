package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.services.AdminUserManagementService
import org.dals.project.services.CreateUserManagementRequest
import org.dals.project.services.UpdateUserManagementRequest

/**
 * User management routes for admin
 */
fun Route.userManagementRoutes() {
    val userManagementService = AdminUserManagementService()

    route("/admin/users") {
        /**
         * GET /admin/users
         * Get all users with pagination and filtering
         * Query params: page, pageSize, role
         */
        get {
            try {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 50
                val role = call.request.queryParameters["role"]

                val response = userManagementService.getAllUsers(page, pageSize, role)
                call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Error retrieving users: ${e.message}"
                ))
            }
        }

        /**
         * GET /admin/users/statistics
         * Get user statistics
         */
        get("/statistics") {
            try {
                val response = userManagementService.getUserStatistics()
                call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Error retrieving user statistics: ${e.message}"
                ))
            }
        }

        /**
         * GET /admin/users/{userId}
         * Get user by ID
         */
        get("/{userId}") {
            try {
                val userId = call.parameters["userId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("success" to false, "message" to "User ID is required")
                )

                val response = userManagementService.getUserById(userId)
                call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.NotFound, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Error retrieving user: ${e.message}"
                ))
            }
        }

        /**
         * POST /admin/users
         * Create new user
         */
        post {
            try {
                val request = call.receive<CreateUserManagementRequest>()
                val response = userManagementService.createUser(request)
                call.respond(if (response.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf(
                    "success" to false,
                    "message" to "Error creating user: ${e.message}"
                ))
            }
        }

        /**
         * PUT /admin/users/{userId}
         * Update user
         */
        put("/{userId}") {
            try {
                val userId = call.parameters["userId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("success" to false, "message" to "User ID is required")
                )

                val request = call.receive<UpdateUserManagementRequest>()
                val response = userManagementService.updateUser(userId, request)
                call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf(
                    "success" to false,
                    "message" to "Error updating user: ${e.message}"
                ))
            }
        }

        /**
         * DELETE /admin/users/{userId}
         * Delete (deactivate) user
         */
        delete("/{userId}") {
            try {
                val userId = call.parameters["userId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("success" to false, "message" to "User ID is required")
                )

                val response = userManagementService.deleteUser(userId)
                call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.NotFound, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Error deleting user: ${e.message}"
                ))
            }
        }
    }
}
