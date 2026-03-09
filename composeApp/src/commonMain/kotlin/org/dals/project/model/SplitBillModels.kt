package org.dals.project.model

import kotlinx.serialization.Serializable

@Serializable
data class SplitBill(
    val id: String,
    val creatorId: String,
    val creatorName: String,
    val description: String,
    val totalAmount: Double,
    val currency: String = "USD",
    val splitEqually: Boolean = true,
    val status: SplitBillStatus,
    val dueDate: String? = null,
    val participants: List<SplitBillParticipant> = emptyList(),
    val paidAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class SplitBillParticipant(
    val id: String,
    val splitBillId: String,
    val customerId: String? = null,
    val name: String,
    val email: String? = null,
    val phoneNumber: String? = null,
    val amount: Double,
    val isPaid: Boolean = false,
    val paidAt: String? = null,
    val reminderSent: Boolean = false,
    val accountNumber: String? = null
)

@Serializable
enum class SplitBillStatus {
    PENDING,
    PARTIAL,
    COMPLETED,
    CANCELLED
}

@Serializable
data class CreateSplitBillRequest(
    val creatorId: String,
    val description: String,
    val totalAmount: Double,
    val currency: String = "USD",
    val splitEqually: Boolean = true,
    val participants: List<CreateParticipantRequest>,
    val dueDate: String? = null
)

@Serializable
data class CreateParticipantRequest(
    val customerId: String? = null,
    val name: String,
    val email: String? = null,
    val phoneNumber: String? = null,
    val amount: Double? = null // Only needed if not splitting equally
)

@Serializable
data class PaySplitBillRequest(
    val participantId: String,
    val splitBillId: String,
    val fromAccountId: String,
    val amount: Double
)

@Serializable
data class SplitBillListResponse(
    val success: Boolean,
    val message: String,
    val splitBills: List<SplitBill>
)

@Serializable
data class SplitBillResponse(
    val success: Boolean,
    val message: String,
    val splitBill: SplitBill? = null
)

@Serializable
data class SplitBillSummary(
    val totalSplits: Int,
    val activeSplits: Int,
    val completedSplits: Int,
    val totalOwed: Double,
    val totalOwing: Double,
    val netBalance: Double
)
