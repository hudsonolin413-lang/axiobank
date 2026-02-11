package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.models.ApiResponse
import org.dals.project.services.CardService
import org.dals.project.models.CardDto
import org.dals.project.models.UserCardsResponse
import org.dals.project.models.CardPaymentResponse
import org.dals.project.models.AddCardRequest
import org.dals.project.models.CardVerificationRequest
import org.dals.project.models.CardPaymentRequest

fun Route.cardRoutes(cardService: CardService) {
    route("/cards") {
        // Get all cards for a user
        get("/user/{userId}") {
            try {
                val userId = call.parameters["userId"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse(
                            success = false,
                            message = "User ID is required",
                            data = null
                        )
                    )

                println("🌐 CardRoutes: Fetching cards for user: $userId")
                val cards = try {
                    cardService.getCardsByUserId(userId)
                } catch (e: Exception) {
                    println("❌ CardRoutes: Service error fetching cards for user $userId: ${e.message}")
                    e.printStackTrace()
                    throw e
                }
                println("✅ CardRoutes: Found ${cards.size} cards for user $userId")

                call.respond(
                    HttpStatusCode.OK,
                    UserCardsResponse(
                        success = true,
                        message = "Cards fetched successfully",
                        cards = cards
                    )
                )
            } catch (e: Exception) {
                println("❌ CardRoutes: Error fetching cards: ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse(
                        success = false,
                        message = "Failed to fetch cards",
                        data = null,
                        error = e.message
                    )
                )
            }
        }

        // Get a specific card by ID
        get("/{cardId}") {
            try {
                val cardId = call.parameters["cardId"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse(
                            success = false,
                            message = "Card ID is required",
                            data = null
                        )
                    )

                val card = cardService.getCardById(cardId)
                if (card != null) {
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            message = "Card fetched successfully",
                            data = card
                        )
                    )
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse(
                            success = false,
                            message = "Card not found",
                            data = null
                        )
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse(
                        success = false,
                        message = "Failed to fetch card",
                        data = null,
                        error = e.message
                    )
                )
            }
        }

        // Add a new card
        post {
            try {
                val request = call.receive<AddCardRequest>()
                val card = cardService.addCard(request)

                call.respond(
                    HttpStatusCode.Created,
                    ApiResponse(
                        success = true,
                        message = "Card added successfully",
                        data = card
                    )
                )
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse(
                        success = false,
                        message = "Invalid card details",
                        data = null,
                        error = e.message
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse(
                        success = false,
                        message = "Failed to add card",
                        data = null,
                        error = e.message
                    )
                )
            }
        }

        // Verify a card
        post("/verify") {
            try {
                val request = call.receive<CardVerificationRequest>()
                cardService.verifyCard(request.cardId, request.verificationCode)

                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        message = "Card verified successfully",
                        data = null
                    )
                )
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse(
                        success = false,
                        message = "Verification failed",
                        data = null,
                        error = e.message
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse(
                        success = false,
                        message = "Failed to verify card",
                        data = null,
                        error = e.message
                    )
                )
            }
        }

        // Make a card payment
        post("/payment") {
            try {
                val request = call.receive<CardPaymentRequest>()
                val transactionId = cardService.processCardPayment(request)

                call.respond(
                    HttpStatusCode.OK,
                    CardPaymentResponse(
                        success = true,
                        message = "Payment processed successfully",
                        transactionId = transactionId
                    )
                )
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse(
                        success = false,
                        message = "Payment failed",
                        data = null,
                        error = e.message
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse(
                        success = false,
                        message = "Failed to process payment",
                        data = null,
                        error = e.message
                    )
                )
            }
        }

        // Set card as default
        put("/{cardId}/default") {
            try {
                val cardId = call.parameters["cardId"]
                    ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse(
                            success = false,
                            message = "Card ID is required",
                            data = null
                        )
                    )

                cardService.setDefaultCard(cardId)

                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        message = "Default card set successfully",
                        data = null
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse(
                        success = false,
                        message = "Failed to set default card",
                        data = null,
                        error = e.message
                    )
                )
            }
        }

        // Delete a card
        delete("/{cardId}") {
            try {
                val cardId = call.parameters["cardId"]
                    ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse(
                            success = false,
                            message = "Card ID is required",
                            data = null
                        )
                    )

                cardService.deleteCard(cardId)

                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        message = "Card deleted successfully",
                        data = null
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse(
                        success = false,
                        message = "Failed to delete card",
                        data = null,
                        error = e.message
                    )
                )
            }
        }
    }
}
