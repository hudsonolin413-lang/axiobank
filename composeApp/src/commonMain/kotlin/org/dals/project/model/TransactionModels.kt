package org.dals.project.model

import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    val id: String,
    val userId: String,
    val type: TransactionType,
    val category: TransactionCategory,
    val amount: Double,
    val currency: String = "USD",
    val description: String,
    val recipientName: String? = null,
    val recipientAccount: String? = null,
    val status: TransactionStatus,
    val timestamp: String,
    val fee: Double = 0.0,
    val reference: String? = null
)

@Serializable
data class SendMoneyRequest(
    val recipientName: String,
    val recipientAccount: String,
    val amount: Double,
    val description: String,
    val currency: String = "USD",
    val paymentMethod: PaymentMethod = PaymentMethod.MPESA
)

@Serializable
data class ReceiveMoneyRequest(
    val amount: Double,
    val description: String,
    val currency: String = "USD"
)

@Serializable
data class BillPayment(
    val id: String,
    val userId: String,
    val billType: BillType,
    val providerName: String,
    val accountNumber: String,
    val amount: Double,
    val dueDate: String,
    val status: PaymentStatus,
    val scheduledDate: String? = null,
    val isRecurring: Boolean = false
)

@Serializable
data class Investment(
    val id: String,
    val userId: String,
    val type: InvestmentType,
    val name: String,
    val symbol: String? = null,
    val amount: Double,
    val currentValue: Double,
    val purchaseDate: String,
    val currentPrice: Double,
    val quantity: Double,
    val totalReturn: Double,
    val returnPercentage: Double
)

@Serializable
data class WalletBalance(
    val userId: String,
    val totalBalance: Double,
    val availableBalance: Double,
    val pendingAmount: Double,
    val currency: String = "USD",
    val lastUpdated: String
)

@Serializable
enum class TransactionType {
    SEND, RECEIVE, BILL_PAYMENT, RENT_PAYMENT, LOAN_PAYMENT, INVESTMENT, WITHDRAWAL, DEPOSIT
}

@Serializable
enum class TransactionCategory {
    TRANSFER, BILLS, RENT, LOANS, INVESTMENT, SHOPPING, FOOD, TRANSPORT, UTILITIES, OTHER
}

@Serializable
enum class TransactionStatus {
    PENDING, COMPLETED, FAILED, CANCELLED, REVERSED
}

@Serializable
enum class BillType {
    ELECTRICITY, WATER, GAS, INTERNET, PHONE, INSURANCE, CREDIT_CARD, RENT, LOAN, OTHER
}

@Serializable
enum class PaymentStatus {
    SCHEDULED, PAID, OVERDUE, CANCELLED
}

@Serializable
enum class InvestmentType {
    STOCKS, CRYPTO, BONDS, MUTUAL_FUNDS, ETF, REAL_ESTATE, COMMODITIES
}

@Serializable
enum class PaymentMethod {
    MPESA, AIRTEL, CREDIT_CARD, DEBIT_CARD
}

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