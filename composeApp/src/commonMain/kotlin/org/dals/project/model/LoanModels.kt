package org.dals.project.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val username: String,
    val walletAddress: String,
    val fullName: String,
    val email: String,
    val phoneNumber: String,
    val customerNumber: String = "", // Customer #: CUST1764097149666
    val accountNumber: String? = null, // Account number (shown after KYC verified)
    val creditScore: Int = 0,
    val kycStatus: KycStatus = KycStatus.PENDING,
    val totalBorrowed: Double = 0.0,
    val totalRepaid: Double = 0.0,
    val activeLoans: Int = 0,
    val defaultedLoans: Int = 0,
    val joinedDate: String
)

@Serializable
data class LoanApplication(
    val id: String,
    val borrowerId: String,
    val amount: Double,
    val purpose: LoanPurpose,
    val termInMonths: Int,
    val interestRate: Double,
    val collateralType: CollateralType,
    val collateralValue: Double,
    val status: LoanStatus,
    val applicationDate: String,
    val description: String,
    val employmentInfo: EmploymentInfo? = null,
    val monthlyIncome: Double,
    val existingDebts: Double
)

@Serializable
data class Loan(
    val id: String,
    val applicationId: String,
    val borrowerId: String,
    val lenderId: String? = null,
    val amount: Double,
    val interestRate: Double,
    val termInMonths: Int,
    val monthlyPayment: Double,
    val remainingBalance: Double,
    val status: LoanStatus,
    val createdDate: String,
    val dueDate: String,
    val lastPaymentDate: String? = null,
    val collateral: Collateral,
    val smartContractAddress: String? = null
)

@Serializable
data class LoanPayment(
    val id: String,
    val loanId: String,
    val amount: Double,
    val paymentDate: String,
    val paymentType: PaymentType,
    val transactionHash: String? = null
)

@Serializable
data class EmploymentInfo(
    val employerName: String,
    val jobTitle: String,
    val employmentType: EmploymentType,
    val yearsOfExperience: Int
)

@Serializable
data class Collateral(
    val type: CollateralType,
    val description: String,
    val value: Double,
    val documentUrl: String? = null
)

@Serializable
enum class KycStatus {
    PENDING, VERIFIED, REJECTED
}

@Serializable
enum class LoanStatus {
    DRAFT, SUBMITTED, UNDER_REVIEW, APPROVED, FUNDED, ACTIVE, COMPLETED, DEFAULTED, REJECTED, PENDING, PAID_OFF
}

@Serializable
enum class LoanPurpose {
    BUSINESS, EDUCATION, HOME_IMPROVEMENT, MEDICAL, PERSONAL, DEBT_CONSOLIDATION, VEHICLE, HOME, AUTO
}

@Serializable
enum class CollateralType {
    CRYPTOCURRENCY, REAL_ESTATE, VEHICLE, JEWELRY, STOCKS, NONE
}

@Serializable
enum class EmploymentType {
    FULL_TIME, PART_TIME, CONTRACT, FREELANCE, SELF_EMPLOYED, UNEMPLOYED
}

@Serializable
enum class PaymentType {
    MONTHLY_PAYMENT, EARLY_PAYMENT, LATE_FEE, PENALTY
}