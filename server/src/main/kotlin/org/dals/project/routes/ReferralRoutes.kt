package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.models.ApiResponse
import org.dals.project.services.InviteFriendRequest
import org.dals.project.services.ReferralService
import java.util.*

fun Route.referralRoutes(referralService: ReferralService) {
    route("/referrals") {
        // Get referrals for a specific user
        get("/customer/{customerId}") {
            try {
                val customerId = call.parameters["customerId"]?.let { UUID.fromString(it) }
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid customer ID")
                
                val referrals = referralService.getReferralsByReferrer(customerId)
                call.respond(HttpStatusCode.OK, referrals)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to fetch referrals: ${e.message}")
            }
        }

        // Send an invitation
        post("/invite") {
            try {
                val request = call.receive<InviteFriendRequest>()
                val response = referralService.inviteFriend(request)
                
                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.BadRequest, response)
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<String>(false, "Failed to send invitation: ${e.message}"))
            }
        }
    }
}
