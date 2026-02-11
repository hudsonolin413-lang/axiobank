package org.dals.project.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateLoanApplicationStatusRequest(
    val status: String,
    val reviewedBy: String,
    val approvedAmount: String? = null,
    val interestRate: String? = null,
    val termMonths: Int? = null,
    val comments: String? = null
)
