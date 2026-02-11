package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.models.ApiResponse
import org.dals.project.services.MastercardIssuanceService
import org.dals.project.services.CardService
import kotlinx.serialization.Serializable

@Serializable
data class IssueCardRequest(
    val userId: String,
    val cardHolderName: String,
    val cardType: String, // "DEBIT" or "CREDIT"
    val linkedAccountId: String? = null, // Required for debit cards
    val linkedAccountNumber: String? = null,
    val deliveryMethod: String = "PHYSICAL", // "PHYSICAL" or "VIRTUAL"
    val billingAddress: AddressDto,
    val shippingAddress: AddressDto? = null,
    // Credit card specific
    val creditCardTier: String? = "STANDARD", // "STANDARD", "GOLD", "PLATINUM", "WORLD"
    val creditLimit: Double? = 5000.00
)

@Serializable
data class AddressDto(
    val streetAddress: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String
)

@Serializable
data class ActivateCardRequest(
    val cardId: String,
    val pin: String
)

@Serializable
data class SuspendCardRequest(
    val cardId: String,
    val reason: String
)

@Serializable
data class CardIssuanceResponse(
    val success: Boolean,
    val message: String,
    val card: IssuedCardDto? = null,
    val error: String? = null
)

@Serializable
data class IssuedCardDto(
    val cardId: String,
    val maskedCardNumber: String,
    val cardHolderName: String,
    val cardType: String,
    val cardBrand: String = "MASTERCARD",
    val expiryMonth: Int,
    val expiryYear: Int,
    val deliveryMethod: String,
    val status: String,
    val trackingNumber: String? = null,
    val estimatedDelivery: String? = null,
    val linkedAccountId: String? = null
)

fun Route.cardIssuanceRoutes(
    mastercardService: MastercardIssuanceService,
    cardService: CardService
) {
    route("/cards/issue") {

        // Issue a new card (debit or credit)
        post {
            try {
                val request = call.receive<IssueCardRequest>()

                println("🎴 CardIssuanceRoutes: Issuing ${request.cardType} card for user: ${request.userId}")

                // Validate request
                if (request.cardType == "DEBIT" && request.linkedAccountId == null) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        CardIssuanceResponse(
                            success = false,
                            message = "Debit cards must be linked to an account",
                            error = "Missing linkedAccountId"
                        )
                    )
                }

                // Convert to Mastercard request format
                val mcRequest = org.dals.project.services.CardIssuanceRequest(
                    customerId = request.userId,
                    cardHolderName = request.cardHolderName,
                    linkedAccountNumber = request.linkedAccountNumber ?: "",
                    cardType = request.cardType,
                    billingAddress = org.dals.project.services.Address(
                        street = request.billingAddress.streetAddress,
                        city = request.billingAddress.city,
                        state = request.billingAddress.state,
                        zipCode = request.billingAddress.zipCode,
                        country = request.billingAddress.country
                    ),
                    shippingAddress = request.shippingAddress?.let {
                        org.dals.project.services.Address(
                            street = it.streetAddress,
                            city = it.city,
                            state = it.state,
                            zipCode = it.zipCode,
                            country = it.country
                        )
                    },
                    deliveryMethod = request.deliveryMethod,
                    creditCardTier = request.creditCardTier,
                    creditLimit = request.creditLimit
                )

                // Issue card via Mastercard API
                val mcResponse = if (request.cardType == "DEBIT") {
                    mastercardService.issueDebitCard(mcRequest)
                } else {
                    mastercardService.issueCreditCard(mcRequest)
                }

                println("✅ CardIssuanceRoutes: Card issued successfully - ID: ${mcResponse.cardId}")

                // Save to database
                val cardDto = org.dals.project.models.CardDto(
                    id = mcResponse.cardId ?: "",
                    userId = request.userId,
                    linkedAccountId = request.linkedAccountId,
                    linkedAccountNumber = request.linkedAccountNumber,
                    linkedAccountBalance = null,
                    cardHolderName = request.cardHolderName,
                    cardType = if (request.cardType == "DEBIT") "DEBIT" else "CREDIT",
                    cardBrand = "MASTERCARD",
                    lastFourDigits = mcResponse.lastFourDigits ?: "",
                    expiryMonth = mcResponse.expiryMonth ?: 0,
                    expiryYear = mcResponse.expiryYear ?: 0,
                    isDefault = false,
                    isActive = false, // Requires activation
                    addedDate = java.time.LocalDateTime.now().toString(),
                    nickname = "${request.cardType} Card"
                )

                // Store card in database
                cardService.saveIssuedCard(cardDto)

                call.respond(
                    HttpStatusCode.Created,
                    CardIssuanceResponse(
                        success = true,
                        message = "Card issued successfully",
                        card = IssuedCardDto(
                            cardId = mcResponse.cardId ?: "",
                            maskedCardNumber = mcResponse.maskedCardNumber ?: "",
                            cardHolderName = request.cardHolderName,
                            cardType = request.cardType,
                            cardBrand = "MASTERCARD",
                            expiryMonth = mcResponse.expiryMonth ?: 0,
                            expiryYear = mcResponse.expiryYear ?: 0,
                            deliveryMethod = request.deliveryMethod,
                            status = mcResponse.status,
                            trackingNumber = mcResponse.trackingNumber,
                            estimatedDelivery = mcResponse.estimatedDelivery,
                            linkedAccountId = request.linkedAccountId
                        )
                    )
                )
            } catch (e: IllegalArgumentException) {
                println("❌ CardIssuanceRoutes: Invalid request - ${e.message}")
                call.respond(
                    HttpStatusCode.BadRequest,
                    CardIssuanceResponse(
                        success = false,
                        message = "Invalid card issuance request",
                        error = e.message
                    )
                )
            } catch (e: Exception) {
                println("❌ CardIssuanceRoutes: Error issuing card - ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    CardIssuanceResponse(
                        success = false,
                        message = "Failed to issue card",
                        error = e.message
                    )
                )
            }
        }

        // Activate a card with PIN
        post("/activate") {
            try {
                val request = call.receive<ActivateCardRequest>()

                println("🔓 CardIssuanceRoutes: Activating card: ${request.cardId}")

                // Validate PIN (4-6 digits)
                if (!request.pin.matches(Regex("^\\d{4,6}$"))) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse(
                            success = false,
                            message = "PIN must be 4-6 digits",
                            data = null
                        )
                    )
                }

                // Activate card via Mastercard API
                val activationResponse = mastercardService.activateCard(request.cardId, request.pin)

                // Update card status in database
                cardService.activateCard(request.cardId)

                println("✅ CardIssuanceRoutes: Card activated successfully")

                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        message = "Card activated successfully",
                        data = mapOf(
                            "cardId" to request.cardId,
                            "status" to activationResponse.status,
                            "activatedAt" to activationResponse.activatedAt
                        )
                    )
                )
            } catch (e: IllegalArgumentException) {
                println("❌ CardIssuanceRoutes: Activation failed - ${e.message}")
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse(
                        success = false,
                        message = "Card activation failed",
                        data = null,
                        error = e.message
                    )
                )
            } catch (e: Exception) {
                println("❌ CardIssuanceRoutes: Error activating card - ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse(
                        success = false,
                        message = "Failed to activate card",
                        data = null,
                        error = e.message
                    )
                )
            }
        }

        // Get card details and status
        get("/{cardId}/details") {
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

                println("📋 CardIssuanceRoutes: Fetching details for card: $cardId")

                // Get details from Mastercard API
                val cardDetails = mastercardService.getCardDetails(cardId)

                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        message = "Card details fetched successfully",
                        data = cardDetails
                    )
                )
            } catch (e: Exception) {
                println("❌ CardIssuanceRoutes: Error fetching card details - ${e.message}")
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse(
                        success = false,
                        message = "Failed to fetch card details",
                        data = null,
                        error = e.message
                    )
                )
            }
        }

        // Suspend/block a card
        post("/suspend") {
            try {
                val request = call.receive<SuspendCardRequest>()

                println("🚫 CardIssuanceRoutes: Suspending card: ${request.cardId}")

                // Suspend card via Mastercard API
                val suspendResponse = mastercardService.suspendCard(request.cardId, request.reason)

                // Update card status in database
                cardService.suspendCard(request.cardId)

                println("✅ CardIssuanceRoutes: Card suspended successfully")

                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        message = "Card suspended successfully",
                        data = mapOf(
                            "cardId" to request.cardId,
                            "status" to suspendResponse.status,
                            "suspendedAt" to suspendResponse.suspendedAt,
                            "reason" to request.reason
                        )
                    )
                )
            } catch (e: Exception) {
                println("❌ CardIssuanceRoutes: Error suspending card - ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse(
                        success = false,
                        message = "Failed to suspend card",
                        data = null,
                        error = e.message
                    )
                )
            }
        }
    }
}
