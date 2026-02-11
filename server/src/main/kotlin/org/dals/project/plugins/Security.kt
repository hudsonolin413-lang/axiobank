package org.dals.project.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*
import org.dals.project.database.DatabaseFactory
import org.dals.project.database.UserSessions
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Security middleware for protecting sensitive API endpoints.
 * Validates session tokens and enforces domain-based access control.
 */

// Plugin key for the security feature
val SecurityPlugin = createApplicationPlugin(name = "SecurityPlugin") {
    // No special configuration needed
}

/**
 * Middleware to require authentication for protected routes.
 * Validates the session token from Authorization header.
 */
suspend fun ApplicationCall.requireAuth(): String? {
    val sessionId = request.headers["Authorization"]?.removePrefix("Bearer ")

    if (sessionId == null) {
        respond(
            HttpStatusCode.Unauthorized,
            mapOf("success" to false, "message" to "Authentication required. No session token provided.")
        )
        return null
    }

    // Validate session exists and is active (no expiration check as table doesn't have that field)
    val isValid = transaction {
        UserSessions.select { (UserSessions.id eq java.util.UUID.fromString(sessionId)) }
            .singleOrNull()
            ?.let { row ->
                row[UserSessions.isActive]
            } ?: false
    }

    if (!isValid) {
        respond(
            HttpStatusCode.Unauthorized,
            mapOf("success" to false, "message" to "Invalid or expired session token.")
        )
        return null
    }

    return sessionId
}

/**
 * Domain-based access control middleware.
 * Restricts certain endpoints to specific domains (e.g., employee portal vs public website).
 */
suspend fun ApplicationCall.requireDomain(vararg allowedDomains: String): Boolean {
    val origin = request.headers["Origin"] ?: request.headers["Referer"] ?: ""
    val host = request.headers["Host"] ?: ""

    // Extract domain from origin/referer
    val requestDomain = when {
        origin.isNotEmpty() -> {
            val domain = origin.removePrefix("http://").removePrefix("https://")
                .split("/")[0].split(":")[0]
            domain
        }
        host.isNotEmpty() -> host.split(":")[0]
        else -> ""
    }

    // Check if the request is from an allowed domain
    val isAllowed = allowedDomains.any { allowed ->
        requestDomain.equals(allowed, ignoreCase = true) ||
        requestDomain.endsWith(".$allowed", ignoreCase = true) ||
        allowed == "*" // Allow all if wildcard specified
    }

    if (!isAllowed) {
        respond(
            HttpStatusCode.Forbidden,
            mapOf(
                "success" to false,
                "message" to "Access denied. This resource is not accessible from your domain.",
                "allowedDomains" to allowedDomains.toList()
            )
        )
        return false
    }

    return true
}

/**
 * Combined middleware for employee/admin routes.
 * Requires both authentication and domain restriction.
 */
suspend fun ApplicationCall.requireEmployeeAccess(vararg allowedDomains: String): String? {
    // First check domain
    if (!requireDomain(*allowedDomains)) {
        return null
    }

    // Then check authentication
    return requireAuth()
}

/**
 * Rate limiting data class
 */
data class RateLimitConfig(
    val maxRequests: Int = 100,
    val windowMs: Long = 60_000 // 1 minute
)

/**
 * Simple in-memory rate limiter
 */
object RateLimiter {
    private val requestCounts = mutableMapOf<String, MutableList<Long>>()

    fun checkLimit(identifier: String, config: RateLimitConfig): Boolean {
        val now = System.currentTimeMillis()
        val requests = requestCounts.getOrPut(identifier) { mutableListOf() }

        // Remove old requests outside the window
        requests.removeIf { it < now - config.windowMs }

        // Check if limit exceeded
        if (requests.size >= config.maxRequests) {
            return false
        }

        // Add current request
        requests.add(now)
        return true
    }

    fun cleanup() {
        // Periodically cleanup old entries
        val now = System.currentTimeMillis()
        requestCounts.entries.removeIf { (_, timestamps) ->
            timestamps.all { it < now - 300_000 } // 5 minutes
        }
    }
}

/**
 * Rate limiting middleware
 */
suspend fun ApplicationCall.checkRateLimit(identifier: String? = null, config: RateLimitConfig = RateLimitConfig()): Boolean {
    val clientId = identifier ?: request.local.remoteHost
    if (!RateLimiter.checkLimit(clientId, config)) {
        respond(
            HttpStatusCode.TooManyRequests,
            mapOf("success" to false, "message" to "Rate limit exceeded. Please try again later.")
        )
        return false
    }
    return true
}
