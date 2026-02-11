package org.dals.project.model

import kotlinx.serialization.Serializable

@Serializable
data class Card(
    val id: String,
    val userId: String,
    val linkedAccountId: String? = null,
    val linkedAccountNumber: String? = null,
    val linkedAccountBalance: Double? = null,
    val cardHolderName: String,
    val cardType: CardType,
    val cardBrand: CardBrand,
    val lastFourDigits: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val isDefault: Boolean = false,
    val isActive: Boolean = true,
    val addedDate: String,
    val nickname: String? = null,
    val billingAddress: BillingAddress? = null
)

@Serializable
data class CardRequest(
    val cardNumber: String,
    val cardHolderName: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val cvv: String,
    val cardType: CardType,
    val nickname: String? = null,
    val billingAddress: BillingAddress? = null
)

@Serializable
data class BillingAddress(
    val streetAddress: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String
)

@Serializable
data class CardVerificationRequest(
    val cardId: String,
    val verificationCode: String
)

@Serializable
data class CardPaymentRequest(
    val cardId: String,
    val amount: Double,
    val currency: String = "USD",
    val description: String,
    val cvv: String
)

@Serializable
enum class CardType {
    CREDIT, DEBIT
}

@Serializable
enum class CardBrand {
    VISA, MASTERCARD, AMERICAN_EXPRESS, DISCOVER, UNKNOWN
}

@Serializable
enum class CardStatus {
    PENDING_VERIFICATION, ACTIVE, BLOCKED, EXPIRED, INVALID
}
