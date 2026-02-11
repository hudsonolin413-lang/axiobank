package org.dals.project.model

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val userId: String,
    val language: String = "en",
    val currency: String = "USD",
    val isDarkMode: Boolean = false,
    val notificationSettings: NotificationSettings? = null,
    val transactionLimits: TransactionLimits? = null,
    val autoPaySettings: AutoPaySettings? = null,
    val locationSettings: LocationSettings? = null,
    val privacySettings: PrivacySettings? = null
)

@Serializable
data class TransactionLimits(
    val dailyLimit: Double = 10000.0,
    val monthlyLimit: Double = 100000.0,
    val singleTransactionLimit: Double = 5000.0,
    val isEnabled: Boolean = true
)

@Serializable
data class AutoPaySettings(
    val isEnabled: Boolean = false,
    val recurringPayments: List<RecurringPayment> = emptyList(),
    val autoLoanPayment: Boolean = false,
    val minimumBalance: Double = 1000.0
)

@Serializable
data class RecurringPayment(
    val id: String,
    val name: String,
    val amount: Double,
    val frequency: PaymentFrequency,
    val nextPaymentDate: String,
    val isActive: Boolean = true,
    val paymentMethod: String
)

@Serializable
enum class PaymentFrequency {
    WEEKLY, MONTHLY, QUARTERLY, YEARLY
}

@Serializable
data class LocationSettings(
    val enableLocationServices: Boolean = false,
    val shareLocationForTransactions: Boolean = false,
    val nearbyATMNotifications: Boolean = false
)

@Serializable
data class PrivacySettings(
    val enableBiometrics: Boolean = true,
    val enablePinLock: Boolean = true,
    val autoLockTimeout: Int = 5, // minutes
    val hideBalanceInNotifications: Boolean = false,
    val enableTransactionMasking: Boolean = false
)

@Serializable
data class KYCDocument(
    val id: String,
    val type: DocumentType,
    val fileName: String,
    val filePath: String,
    val uploadDate: String,
    val status: DocumentStatus = DocumentStatus.PENDING,
    val notes: String = ""
)

@Serializable
enum class DocumentType {
    NATIONAL_ID,
    PASSPORT,
    DRIVING_LICENSE,
    UTILITY_BILL,
    BANK_STATEMENT,
    SELFIE
}

@Serializable
enum class DocumentStatus {
    PENDING,
    APPROVED,
    REJECTED,
    EXPIRED
}

@Serializable
data class Currency(
    val code: String,
    val name: String,
    val symbol: String,
    val flag: String
)

@Serializable
data class DateTimeSettings(
    val timeFormat: TimeFormat = TimeFormat.HOUR_24,
    val dateFormat: DateFormat = DateFormat.DD_MM_YYYY,
    val timezone: String = "UTC"
)

@Serializable
enum class TimeFormat {
    HOUR_12, HOUR_24
}

@Serializable
enum class DateFormat {
    DD_MM_YYYY,
    MM_DD_YYYY,
    YYYY_MM_DD
}