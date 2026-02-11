package org.dals.project.models

import kotlinx.serialization.Serializable

@Serializable
data class MpesaDepositRequest(
    val phoneNumber: String,
    val accountNumber: String,
    val amount: Double,
    val description: String,
    val operatorId: String
)

@Serializable
data class MpesaWalletDepositRequest(
    val phoneNumber: String,
    val amount: Double,
    val description: String? = null
)

@Serializable
data class MpesaReversalRequest(
    val checkoutRequestID: String,
    val mpesaReceiptNumber: String? = null,
    val reason: String? = null
)

@Serializable
data class MpesaWithdrawalRequest(
    val phoneNumber: String,
    val amount: Double,
    val accountNumber: String,
    val customerId: String,
    val remarks: String? = null
)

@Serializable
data class MpesaResponse(
    val success: Boolean,
    val message: String,
    val transactionId: String? = null,
    val checkoutRequestID: String? = null,
    val merchantRequestID: String? = null,
    val customerMessage: String? = null,
    val error: String? = null
)

@Serializable
data class MpesaReversalResponse(
    val success: Boolean,
    val message: String,
    val transactionId: String? = null,
    val status: String? = null,
    val amount: Double? = null,
    val walletId: String? = null,
    val error: String? = null
)

// ... existing code ...