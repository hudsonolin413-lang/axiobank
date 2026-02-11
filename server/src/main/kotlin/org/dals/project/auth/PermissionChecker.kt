package org.dals.project.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import org.dals.project.database.Users
import org.dals.project.models.ApiResponse
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

/**
 * Permission definitions for the system
 */
object Permissions {
    const val VIEW_CUSTOMERS = "VIEW_CUSTOMERS"
    const val EDIT_CUSTOMERS = "EDIT_CUSTOMERS"
    const val CREATE_ACCOUNTS = "CREATE_ACCOUNTS"
    const val PROCESS_TRANSACTIONS = "PROCESS_TRANSACTIONS"
    const val APPROVE_LOANS = "APPROVE_LOANS"
    const val VIEW_REPORTS = "VIEW_REPORTS"
    const val MANAGE_CASH_DRAWER = "MANAGE_CASH_DRAWER"
    const val KYC_VERIFICATION = "KYC_VERIFICATION"
    const val MANAGE_EMPLOYEES = "MANAGE_EMPLOYEES"
    const val SYSTEM_CONFIG = "SYSTEM_CONFIG"
}

/**
 * Permission checker utility
 */
object PermissionChecker {

    /**
     * Check if a user has the required permission
     */
    fun hasPermission(userId: UUID, requiredPermission: String): Boolean {
        return transaction {
            try {
                val user = Users.select { Users.id eq userId }.singleOrNull()

                if (user == null) {
                    println("⚠️ User not found: $userId")
                    return@transaction false
                }

                // SYSTEM_ADMIN has all permissions
                val role = user[Users.role].name
                if (role == "SYSTEM_ADMIN") {
                    println("✅ SYSTEM_ADMIN has all permissions")
                    return@transaction true
                }

                // Parse permissions from JSON
                val permissionsJson = user[Users.permissions]
                val permissions: List<String> = try {
                    Json.decodeFromString(permissionsJson)
                } catch (e: Exception) {
                    emptyList()
                }

                val hasPermission = permissions.contains(requiredPermission)

                if (hasPermission) {
                    println("✅ User ${user[Users.username]} has permission: $requiredPermission")
                } else {
                    println("❌ User ${user[Users.username]} lacks permission: $requiredPermission (has: $permissions)")
                }

                hasPermission
            } catch (e: Exception) {
                println("❌ Error checking permission: ${e.message}")
                false
            }
        }
    }

    /**
     * Check if a user has ANY of the required permissions
     */
    fun hasAnyPermission(userId: UUID, requiredPermissions: List<String>): Boolean {
        return requiredPermissions.any { hasPermission(userId, it) }
    }

    /**
     * Check if a user has ALL of the required permissions
     */
    fun hasAllPermissions(userId: UUID, requiredPermissions: List<String>): Boolean {
        return requiredPermissions.all { hasPermission(userId, it) }
    }
}

/**
 * Ktor extension to check permissions in routes
 */
suspend fun ApplicationCall.requirePermission(userId: String, permission: String): Boolean {
    try {
        val uuid = UUID.fromString(userId)
        val hasPermission = PermissionChecker.hasPermission(uuid, permission)

        if (!hasPermission) {
            respond(
                HttpStatusCode.Forbidden,
                ApiResponse<String>(
                    success = false,
                    message = "Access denied. Required permission: $permission"
                )
            )
        }

        return hasPermission
    } catch (e: Exception) {
        respond(
            HttpStatusCode.BadRequest,
            ApiResponse<String>(
                success = false,
                message = "Invalid user ID"
            )
        )
        return false
    }
}

/**
 * Ktor extension to check multiple permissions (requires ANY)
 */
suspend fun ApplicationCall.requireAnyPermission(userId: String, permissions: List<String>): Boolean {
    try {
        val uuid = UUID.fromString(userId)
        val hasPermission = PermissionChecker.hasAnyPermission(uuid, permissions)

        if (!hasPermission) {
            respond(
                HttpStatusCode.Forbidden,
                ApiResponse<String>(
                    success = false,
                    message = "Access denied. Required one of: ${permissions.joinToString(", ")}"
                )
            )
        }

        return hasPermission
    } catch (e: Exception) {
        respond(
            HttpStatusCode.BadRequest,
            ApiResponse<String>(
                success = false,
                message = "Invalid user ID"
            )
        )
        return false
    }
}
