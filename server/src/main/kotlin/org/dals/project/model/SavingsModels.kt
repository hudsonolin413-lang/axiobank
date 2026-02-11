package org.dals.project.model

import kotlinx.serialization.Serializable

@Serializable
data class SavingsAccount(
    val id: String,
    val userId: String,
    val accountName: String,
    val lockPeriod: LockPeriod,
    val interestRate: Double,
    val amount: Double,
    val startDate: String,
    val maturityDate: String,
    val status: SavingsStatus,
    val accruedInterest: Double = 0.0,
    val projectedEarnings: Double = 0.0,
    val earlyWithdrawalPenalty: Double = 0.0
)

@Serializable
enum class LockPeriod(val days: Int, val displayName: String) {
    ONE_MONTH(30, "1 Month"),
    THREE_MONTHS(90, "3 Months"),
    SIX_MONTHS(180, "6 Months"),
    ONE_YEAR(365, "1 Year"),
    TWO_YEARS(730, "2 Years"),
    FIVE_YEARS(1825, "5 Years")
}

@Serializable
enum class SavingsStatus {
    ACTIVE, MATURED, WITHDRAWN, CANCELLED
}
