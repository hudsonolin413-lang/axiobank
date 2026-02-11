package org.dals.project.model

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val id: String,
    val name: String,
    val type: String,
    val accountNumber: String,
    val balance: Double,
    val currency: String = "USD"
)

@Serializable
data class CashFlow(
    val monthYear: String,
    val moneyIn: Double,
    val moneyOut: Double,
    val currency: String = "USD"
)