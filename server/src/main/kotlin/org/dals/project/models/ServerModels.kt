package org.dals.project.models

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Instant
import java.util.*

// Authentication Models
@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val confirmPassword: String,
    val fullName: String,
    val email: String,
    val phoneNumber: String
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val message: String,
    val user: UserDto? = null,
    val token: String? = null
)

// User Models
@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val role: String,
    val status: String,
    val branchId: String? = null,
    val employeeId: String? = null,
    val permissions: List<String> = emptyList(),
    val createdAt: String,
    val lastLoginAt: String? = null
)

// Customer Models
@Serializable
data class CustomerDto(
    val id: String,
    val customerNumber: String,
    val type: String,
    val status: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val middleName: String? = null,
    val dateOfBirth: String? = null,
    val ssn: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val alternatePhone: String? = null,
    val primaryStreet: String? = null,
    val primaryCity: String? = null,
    val primaryState: String? = null,
    val primaryZipCode: String? = null,
    val primaryCountry: String = "USA",
    val mailingStreet: String? = null,
    val mailingCity: String? = null,
    val mailingState: String? = null,
    val mailingZipCode: String? = null,
    val mailingCountry: String? = null,
    val occupation: String? = null,
    val employer: String? = null,
    val annualIncome: Double? = null,
    val creditScore: Int? = null,
    val branchId: String,
    val accountManagerId: String? = null,
    val onboardedDate: String,
    val lastContactDate: String? = null,
    val riskLevel: String = "LOW",
    val kycStatus: String = "PENDING",
    val businessName: String? = null,
    val businessType: String? = null,
    val taxId: String? = null,
    val businessLicenseNumber: String? = null,
    val createdAt: String,
    val updatedAt: String
)

// Card Models
@Serializable
data class CardDto(
    val id: String,
    val userId: String,
    val linkedAccountId: String? = null,
    val linkedAccountNumber: String? = null,
    val linkedAccountBalance: Double? = null,
    val cardHolderName: String,
    val cardType: String,
    val cardBrand: String,
    val lastFourDigits: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val isDefault: Boolean = false,
    val isActive: Boolean = true,
    val addedDate: String,
    val nickname: String? = null
)

@Serializable
data class UserCardsResponse(
    val success: Boolean,
    val message: String,
    val cards: List<CardDto>
)

@Serializable
data class CardPaymentResponse(
    val success: Boolean,
    val message: String,
    val transactionId: String? = null
)

@Serializable
data class AddCardRequest(
    val userId: String,
    val cardNumber: String,
    val cardHolderName: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val cvv: String,
    val cardType: String,
    val nickname: String? = null
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

// Card Transaction Models
@Serializable
data class OnlinePaymentRequest(
    val cardId: String,
    val amount: Double,
    val merchantName: String,
    val cvv: String,
    val currency: String = "USD",
    val category: String? = null
)

@Serializable
data class POSTransactionRequest(
    val cardId: String,
    val amount: Double,
    val merchantName: String,
    val pin: String? = null,
    val currency: String = "USD"
)

@Serializable
data class BillPaymentRequest(
    val cardId: String,
    val amount: Double,
    val billType: String, // e.g., "Electricity", "Water", "Internet"
    val billerName: String,
    val accountNumber: String,
    val cvv: String,
    val currency: String = "USD"
)

@Serializable
data class CardTransferRequest(
    val cardId: String,
    val amount: Double,
    val destinationAccountNumber: String,
    val description: String,
    val cvv: String,
    val currency: String = "USD"
)

@Serializable
data class ATMWithdrawalRequest(
    val cardId: String,
    val amount: Double,
    val atmLocation: String,
    val pin: String,
    val currency: String = "USD"
)

@Serializable
data class CardTransactionResponse(
    val success: Boolean,
    val message: String,
    val transactionId: String? = null,
    val reference: String? = null,
    val amount: Double? = null,
    val fee: Double? = null,
    val newBalance: Double? = null,
    val timestamp: String? = null
)

@Serializable
data class CreateCustomerRequest(
    val type: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val middleName: String? = null,
    val dateOfBirth: String? = null,
    val ssn: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val alternatePhone: String? = null,
    val primaryStreet: String? = null,
    val primaryCity: String? = null,
    val primaryState: String? = null,
    val primaryZipCode: String? = null,
    val primaryCountry: String = "USA",
    val mailingStreet: String? = null,
    val mailingCity: String? = null,
    val mailingState: String? = null,
    val mailingZipCode: String? = null,
    val mailingCountry: String? = null,
    val occupation: String? = null,
    val employer: String? = null,
    val annualIncome: Double? = null,
    val creditScore: Int? = null,
    val branchId: String,
    val businessName: String? = null,
    val businessType: String? = null,
    val taxId: String? = null,
    val businessLicenseNumber: String? = null
)

@Serializable
data class OpenAccountForNewCustomerRequest(
    val customer: CreateCustomerRequest,
    val accountTypeId: String,
    val initialDeposit: Double,
    val nickname: String? = null
)

// KYC Models
@Serializable
data class KycProfileDto(
    val id: String,
    val customerId: String,
    val customerName: String,
    val customerType: String,
    val overallKycStatus: String,
    val complianceLevel: String,
    val lastReviewDate: String?,
    val nextReviewDate: String?,
    val riskRating: String,
    val completionPercentage: Double,
    val flags: String?,
    val notes: String?,
    val assignedOfficer: String?,
    val createdDate: String,
    val lastUpdatedDate: String
)

@Serializable
data class KycDocumentDetailsDto(
    val id: String,
    val customerId: String,
    val customerName: String,
    val documentType: String,
    val fileName: String,
    val originalFileName: String?,
    val filePath: String?,
    val fileSize: Long,
    val mimeType: String?,
    val uploadDate: String,
    val lastModifiedDate: String?,
    val expiryDate: String?,
    val issueDate: String?,
    val issuingAuthority: String?,
    val documentNumber: String?,
    val status: String,
    val priority: String,
    val complianceLevel: String,
    val uploadedBy: String,
    val verifiedBy: String?,
    val verificationDate: String?,
    val rejectedBy: String?,
    val rejectionDate: String?,
    val rejectionReason: String?,
    val notes: String?,
    val internalNotes: String?,
    val isRequired: Boolean,
    val isConfidential: Boolean,
    val requiresManualReview: Boolean,
    val riskScore: Double?
)

@Serializable
data class UpdateKycDocumentStatusRequest(
    val documentId: String,
    val status: String,
    val notes: String? = null,
    val internalNotes: String? = null,
    val rejectionReason: String? = null
)

@Serializable
data class UpdateKycProfileRequest(
    val customerId: String,
    val overallKycStatus: String? = null,
    val complianceLevel: String? = null,
    val riskRating: String? = null,
    val flags: String? = null,
    val notes: String? = null,
    val assignedOfficer: String? = null
)

@Serializable
data class KycDashboardMetricsDto(
    val totalProfiles: Int,
    val pendingReview: Int,
    val underReview: Int,
    val verified: Int,
    val rejected: Int,
    val expired: Int,
    val requiresAction: Int,
    val highRiskCount: Int,
    val pendingDocumentsCount: Int,
    val expiringIn30Days: Int
)

// Account Models
@Serializable
data class AccountDto(
    val id: String,
    val accountNumber: String,
    val customerId: String,
    val type: String,
    val status: String,
    val balance: String,
    val availableBalance: String,
    val minimumBalance: String,
    val interestRate: String,
    val creditLimit: String? = null,
    val branchId: String,
    val openedDate: String,
    val closedDate: String? = null,
    val lastTransactionDate: String? = null,
    val accountManagerId: String? = null,
    val isJointAccount: Boolean = false,
    val nickname: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateAccountRequest(
    val customerId: String,
    val type: String,
    val minimumBalance: String = "0.00",
    val interestRate: String = "0.0000",
    val creditLimit: String? = null,
    val branchId: String,
    val accountManagerId: String? = null,
    val isJointAccount: Boolean = false,
    val nickname: String? = null
)

// Transaction Models
@Serializable
data class TransactionDto(
    val id: String,
    val accountId: String,
    val type: String,
    val amount: String,
    val status: String,
    val description: String,
    val timestamp: String,
    val balanceAfter: String,
    val fromAccountId: String? = null,
    val toAccountId: String? = null,
    val reference: String? = null,
    val processedBy: String? = null,
    val branchId: String? = null,
    val checkNumber: String? = null,
    val merchantName: String? = null,
    val category: String? = null,
    val createdAt: String
)

@Serializable
data class CreateTransactionRequest(
    val accountId: String,
    val type: String,
    val amount: String,
    val description: String,
    val fromAccountId: String? = null,
    val toAccountId: String? = null,
    val reference: String? = null,
    val processedBy: String? = null,
    val branchId: String? = null,
    val checkNumber: String? = null,
    val merchantName: String? = null,
    val category: String? = null
)

// Loan Models
@Serializable
data class LoanStatusUpdateData(
    val id: String,
    val status: String,
    val reviewedBy: String,
    val approvedAmount: String? = null,
    val interestRate: String? = null,
    val termMonths: String? = null,
    val comments: String? = null
)

@Serializable
data class LoanApplicationDto(
    val id: String,
    val customerId: String,
    val loanType: String,
    val requestedAmount: String,
    val purpose: String,
    val status: String,
    val applicationDate: String,
    val reviewedBy: String? = null,
    val reviewedDate: String? = null,
    val approvedAmount: String? = null,
    val interestRate: String? = null,
    val termMonths: Int? = null,
    val collateralDescription: String? = null,
    val collateralValue: String? = null,
    val creditScore: Int? = null,
    // val debtToIncomeRatio: String? = null,
    val annualIncome: String? = null,
    val employmentHistory: String? = null,
    val comments: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateLoanApplicationRequest(
    val customerId: String,
    val loanType: String,
    val requestedAmount: String,
    val purpose: String,
    val collateralDescription: String? = null,
    val collateralValue: String? = null,
    val creditScore: Int? = null,
    // val debtToIncomeRatio: String? = null,
    val annualIncome: String? = null,
    val employmentHistory: String? = null
)

@Serializable
data class LoanDto(
    val id: String,
    val accountId: String? = null,
    val customerId: String,
    val applicationId: String,
    val loanType: String,
    val originalAmount: String,
    val currentBalance: String,
    val interestRate: String,
    val termMonths: Int,
    val monthlyPayment: String,
    val paymentFrequency: String,
    val status: String,
    val originationDate: String,
    val maturityDate: String,
    val nextPaymentDate: String,
    val lastPaymentDate: String? = null,
    val paymentsRemaining: Int? = null,
    val totalInterestPaid: String,
    val totalPrincipalPaid: String,
    val latePaymentFees: String,
    val collateralDescription: String? = null,
    val collateralValue: String? = null,
    val loanOfficerId: String,
    val branchId: String,
    val createdAt: String,
    val updatedAt: String
)

// Credit Assessment Models
@Serializable
data class CreditAssessmentDto(
    val id: String,
    val customerId: String,
    val assessmentDate: String,
    val creditScore: Int,
    val annualIncome: String,
    val existingDebt: String,
    val paymentHistory: String,
    val riskLevel: String,
    val recommendedCreditLimit: String,
    val assessedBy: String,
    val comments: String? = null,
    val createdAt: String,
    val customerName: String? = null
)

@Serializable
data class CreateCreditAssessmentRequest(
    val customerId: String,
    val creditScore: Int,
    val annualIncome: Double,
    val existingDebt: Double,
    val paymentHistory: String,
    val riskLevel: String,
    val recommendedCreditLimit: Double,
    val assessedBy: String,
    val comments: String? = null
)

// Branch Models
@Serializable
data class BranchDto(
    val id: String,
    val branchCode: String,
    val name: String,
    val street: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String = "USA",
    val phoneNumber: String,
    val managerUserId: String? = null,
    val operatingHours: String? = null,
    val status: String = "ACTIVE",
    val totalCustomers: Int = 0,
    val totalAccounts: Int = 0,
    val totalDeposits: String = "0.00",
    val totalLoans: String = "0.00",
    val createdAt: String,
    val updatedAt: String
)

// Employee Management Models
@Serializable
data class CreateEmployeeRequest(
    val username: String,
    val password: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val role: String,
    val branchId: String,
    val department: String,
    val position: String,
    val hireDate: String,
    val salary: Double? = null,
    val managerId: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val emergencyContactRelation: String? = null
)

@Serializable
data class EmployeeDto(
    val id: String,
    val userId: String,
    val employeeNumber: String,
    val department: String,
    val position: String,
    val employmentStatus: String,
    val hireDate: String,
    val terminationDate: String? = null,
    val salary: Double? = null,
    val managerId: String? = null,
    val branchId: String,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val emergencyContactRelation: String? = null,
    val shiftType: String? = null,
    val performanceRating: Double? = null,
    val lastReviewDate: String? = null,
    val nextReviewDate: String? = null,
    val accessLevel: Int = 1,
    val user: UserDto? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class UpdateEmployeeRequest(
    val department: String? = null,
    val position: String? = null,
    val employmentStatus: String? = null,
    val salary: Double? = null,
    val managerId: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val emergencyContactRelation: String? = null,
    val shiftType: String? = null,
    val performanceRating: Double? = null
)

@Serializable
data class EmployeeAttendanceDto(
    val id: String,
    val employeeId: String,
    val date: String,
    val checkInTime: String? = null,
    val checkOutTime: String? = null,
    val status: String,
    val hoursWorked: Double? = null,
    val overtimeHours: Double = 0.0,
    val notes: String? = null,
    val createdAt: String
)

@Serializable
data class EmployeeLeaveDto(
    val id: String,
    val employeeId: String,
    val leaveType: String,
    val startDate: String,
    val endDate: String,
    val daysCount: Int,
    val reason: String? = null,
    val status: String,
    val approvedBy: String? = null,
    val approvalDate: String? = null,
    val rejectionReason: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateLeaveRequest(
    val leaveType: String,
    val startDate: String,
    val endDate: String,
    val reason: String? = null
)

@Serializable
data class UpdateEmployeeRoleRequest(
    val role: String
)

// Admin Role and Permission Models
@Serializable
data class RoleDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val permissions: List<PermissionDto> = emptyList(),
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class PermissionDto(
    val id: String,
    val name: String,
    val description: String? = null
)

@Serializable
data class CreateRoleRequest(
    val name: String,
    val description: String? = null,
    val permissionIds: List<String> = emptyList()
)

@Serializable
data class UpdateRoleRequest(
    val name: String? = null,
    val description: String? = null,
    val permissionIds: List<String>? = null
)

@Serializable
data class CreatePermissionRequest(
    val name: String,
    val description: String? = null
)

@Serializable
data class UpdatePermissionRequest(
    val name: String? = null,
    val description: String? = null
)

// System Configuration Models
@Serializable
data class SystemConfigurationDto(
    val id: String,
    val configKey: String,
    val configValue: String,
    val description: String,
    val category: String,
    val dataType: String,
    val isEditable: Boolean = true,
    val lastModifiedBy: String,
    val lastModifiedAt: String
)

// System Health Models
@Serializable
data class SystemHealthDto(
    val status: String,
    val uptime: String,
    val lastBackup: String? = null,
    val diskSpaceUsed: Double,
    val memoryUsage: Double,
    val cpuUsage: Double,
    val activeConnections: Int,
    val errorRate: Double,
    val lastUpdated: String
)

// System Logs Models
@Serializable
data class SystemLogDto(
    val id: String,
    val level: String,
    val message: String,
    val source: String,
    val timestamp: String
)

@Serializable
data class TransactionSearchResponse(
    val id: String,
    val reference: String,
    val accountId: String? = null,
    val amount: Double,
    val status: String,
    val type: String,
    val description: String?,
    val timestamp: String,
    val balanceAfter: Double? = null,
    val category: String? = null,
    val checkoutRequestId: String? = null,
    val phoneNumber: String? = null,
    val accountNumber: String? = null,
    val createdAt: String? = null
)

// Response wrappers
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val error: String? = null,
    val timestamp: String? = null
)

@Serializable
data class ListResponse<T>(
    val success: Boolean,
    val message: String,
    val data: List<T> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 10
) {
    companion object {
        fun <T> empty(message: String = ""): ListResponse<T> {
            return ListResponse(
                success = false,
                message = message,
                data = emptyList(),
                total = 0,
                page = 1,
                pageSize = 10
            )
        }
    }
}

// Drawer management DTOs
@Serializable
data class DrawerDto(
    val id: String,
    val drawerNumber: String,
    val branchId: String,
    val drawerName: String,
    val location: String? = null,
    val maxFloatAmount: Double,
    val minFloatAmount: Double,
    val status: String,
    val isActive: Boolean,
    val createdBy: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class AssignDrawerRequest(
    val drawerId: String,
    val userId: String,
    val branchId: String,
    val accessLevel: String = "FULL",
    val notes: String? = null
)

@Serializable
data class DrawerAssignmentDto(
    val id: String,
    val drawerId: String,
    val drawerNumber: String? = null, // Added drawer number from Drawers table
    val drawerName: String? = null, // Added drawer name from Drawers table
    val userId: String,
    val branchId: String,
    val assignedBy: String,
    val assignedDate: String,
    val revokedDate: String? = null,
    val revokedBy: String? = null,
    val status: String,
    val accessLevel: String,
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String
)

// Service Response class for backend operations
@Serializable
data class ServiceResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val error: String? = null
) {
    companion object {
        fun <T> success(message: String, data: T? = null): ServiceResponse<T> {
            return ServiceResponse(
                success = true,
                message = message,
                data = data
            )
        }

        fun <T> error(message: String, error: String? = null): ServiceResponse<T> {
            return ServiceResponse(
                success = false,
                message = message,
                error = error
            )
        }
    }
}

// Customer Care Models
@Serializable
data class CustomerDataDto(
    val id: String,
    val customerNumber: String,
    val type: String,
    val status: String,
    val firstName: String,
    val lastName: String,
    val middleName: String? = null,
    val dateOfBirth: String? = null,
    val ssn: String? = null,
    val email: String,
    val phoneNumber: String,
    val alternatePhone: String? = null,
    val occupation: String? = null,
    val employer: String? = null,
    val annualIncome: Double? = null,
    val creditScore: Int? = null,
    val branchId: String,
    val accountManagerId: String? = null,
    val onboardedDate: String,
    val lastContactDate: String? = null,
    val riskLevel: String,
    val kycStatus: String,
    val businessName: String? = null,
    val businessType: String? = null,
    val taxId: String? = null,
    val businessLicenseNumber: String? = null,
    val primaryAddress: AddressDataDto? = null,
    val mailingAddress: AddressDataDto? = null
)

@Serializable
data class AddressDataDto(
    val street: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String,
    val type: String
)

@Serializable
data class ServiceRequestDataDto(
    val id: String,
    val customerId: String,
    val requestType: String,
    val title: String,
    val description: String,
    val status: String,
    val priority: String,
    val createdBy: String,
    val assignedTo: String?,
    val completedBy: String?,
    val estimatedCompletionDate: String?,
    val actualCompletionDate: String?,
    val rejectionReason: String?,
    val approvalRequired: Boolean,
    val approvedBy: String?,
    val approvedAt: String?,
    val createdAt: String,
    val updatedAt: String,
    val completedAt: String?
)

@Serializable
data class ComplaintDataDto(
    val id: String,
    val customerId: String,
    val complaintType: String,
    val title: String,
    val description: String,
    val status: String,
    val priority: String,
    val createdBy: String,
    val assignedTo: String?,
    val resolvedBy: String?,
    val resolutionDate: String?,
    val resolutionNotes: String?,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CustomerCareListResponse<T>(
    val success: Boolean,
    val message: String,
    val data: List<T>,
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 50,
    val timestamp: String
)

@Serializable
data class CustomerServiceMetricsDto(
    val totalCustomers: Int,
    val activeCustomers: Int,
    val pendingKYCDocuments: Int,
    val activeServiceRequests: Int,
    val openComplaints: Int,
    val newCustomersToday: Int,
    val completedServiceRequestsToday: Int,
    val resolvedComplaintsToday: Int
)

@Serializable
data class CustomerCareResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val error: String? = null,
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 10,
    val timestamp: String
)

// Customer Care Access Request Models
@Serializable
data class CustomerCareAccessRequestDto(
    val id: String,
    val customerId: String,
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String,
    val requestType: String, // FULL_ACCESS, LIMITED_ACCESS, VIEW_ONLY
    val reason: String,
    val requestedPermissions: List<String>, // List of specific permissions requested
    val status: String, // PENDING, APPROVED, REJECTED, REVOKED
    val priority: String, // LOW, MEDIUM, HIGH, URGENT
    val requestedBy: String, // Customer ID who requested
    val reviewedBy: String?,
    val approvedBy: String?,
    val rejectedBy: String?,
    val revokedBy: String?,
    val reviewNotes: String?,
    val rejectionReason: String?,
    val revocationReason: String?,
    val approvedAt: String?,
    val rejectedAt: String?,
    val revokedAt: String?,
    val expiresAt: String?, // Access expiration date
    val createdAt: String,
    val updatedAt: String
)

// Transaction Reversal Models
@Serializable
data class TransactionReversalDto(
    val id: String,
    val transactionId: String,
    val customerId: String,
    val accountId: String,
    val amount: String,
    val reason: String,
    val requestedBy: String,
    val status: String, // PENDING, APPROVED, REJECTED, COMPLETED
    val reviewedBy: String?,
    val reviewNotes: String?,
    val rejectionReason: String?,
    val requestedAt: String,
    val reviewedAt: String?,
    val completedAt: String?,
    val estimatedCompletionDate: String?,
    val onHoldUntil: String?,
    val reversalType: String, // REFUND, SEND_TO_RECEIVER
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateTransactionReversalRequest(
    val transactionId: String,
    val customerId: String,
    val reason: String,
    val reversalType: String = "REFUND", // REFUND or SEND_TO_RECEIVER
    val createdBy: String
)

@Serializable
data class ReviewTransactionReversalRequest(
    val reversalId: String,
    val action: String, // APPROVE or REJECT
    val reviewNotes: String?,
    val rejectionReason: String? = null,
    val reversalType: String? = null // Override reversal type if needed
)

@Serializable
data class CompleteTransactionReversalRequest(
    val reversalId: String,
    val completionNotes: String?
)

@Serializable
data class CreateCustomerCareAccessRequest(
    val customerId: String,
    val requestType: String, // FULL_ACCESS, LIMITED_ACCESS, VIEW_ONLY
    val reason: String,
    val requestedPermissions: List<String> = emptyList(), // Specific permissions
    val priority: String = "MEDIUM"
)

@Serializable
data class ReviewCustomerCareAccessRequest(
    val requestId: String,
    val action: String, // APPROVE, REJECT
    val reviewNotes: String? = null,
    val rejectionReason: String? = null,
    val expiresAt: String? = null // For approved requests
)

@Serializable
data class RevokeCustomerCareAccessRequest(
    val requestId: String,
    val revocationReason: String
)

// Loan Payment Models
@Serializable
data class MakeLoanPaymentRequest(
    val loanId: String,
    val amount: String,
    val paymentMethod: String = "ACCOUNT_DEBIT", // ACCOUNT_DEBIT, CASH, CHECK, WIRE_TRANSFER
    val processedBy: String? = null
)

@Serializable
data class LoanPaymentDto(
    val id: String,
    val loanId: String,
    val paymentDate: String,
    val amount: String,
    val principalAmount: String,
    val interestAmount: String,
    val feeAmount: String,
    val balanceAfter: String,
    val paymentMethod: String,
    val reference: String?,
    val processedBy: String,
    val createdAt: String
)

// Notification Models
@Serializable
data class NotificationDto(
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: String,
    val category: String,
    val priority: String,
    val isRead: Boolean,
    val actionUrl: String? = null,
    val relatedEntityType: String? = null,
    val relatedEntityId: String? = null,
    val readAt: String? = null,
    val createdAt: String
)

@Serializable
data class NotificationsResponse(
    val success: Boolean,
    val message: String,
    val data: List<NotificationDto>? = null,
    val unreadCount: Int = 0,
    val timestamp: String
)

@Serializable
data class SendNotificationRequest(
    val recipientId: String? = null, // For single user notification
    val recipientIds: List<String>? = null, // For bulk notification
    val userType: String? = null, // "EMPLOYEE", "CUSTOMER", "ALL_EMPLOYEES", "ALL_CUSTOMERS", "ALL"
    val targetRole: String? = null, // Optional: "TELLER", "LOAN_OFFICER", etc.
    val title: String,
    val message: String,
    val type: String = "CUSTOM",
    val category: String = "GENERAL",
    val priority: String = "NORMAL", // "LOW", "NORMAL", "HIGH", "URGENT"
    val actionUrl: String? = null,
    val channels: List<String> = listOf("IN_APP"), // "IN_APP", "EMAIL", "SMS"
    val customEmail: String? = null, // Optional: Send to custom email instead
    val customPhoneNumber: String? = null, // Optional: Send to custom phone instead
    val attachments: List<String> = emptyList() // Optional: File paths for attachments
)

@Serializable
data class SendNotificationResponse(
    val success: Boolean,
    val message: String,
    val notificationId: String? = null,
    val recipientCount: Int = 0
)

@Serializable
data class SendOtpRequest(
    val email: String,
    val purpose: String = "verification", // "verification", "password_reset", "transaction_confirmation", etc.
    val expiryMinutes: Int = 10
)

@Serializable
data class SendOtpResponse(
    val success: Boolean,
    val message: String,
    val otpId: String? = null, // For tracking/verification
    val expiresAt: String? = null
)

@Serializable
data class UnreadCountResponse(
    val success: Boolean,
    val unreadCount: Int,
    val timestamp: String
)

@Serializable
data class MarkAllReadResponse(
    val success: Boolean,
    val message: String,
    val count: Int,
    val timestamp: String
)

@Serializable
data class PendingKycCustomerDto(
    val customerId: String,
    val customerName: String,
    val customerNumber: String,
    val email: String,
    val phoneNumber: String,
    val submittedDate: String,
    val documents: List<PendingKycDocumentDto>
)

@Serializable
data class PendingKycDocumentDto(
    val id: String,
    val documentType: String,
    val fileName: String,
    val filePath: String,
    val uploadDate: String
)

// Advertisement Models
@Serializable
data class AdvertisementDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val imageUrl: String,
    val linkUrl: String? = null,
    val displayOrder: Int = 0,
    val isActive: Boolean = true,
    val startDate: String,
    val endDate: String? = null,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateAdvertisementRequest(
    val title: String,
    val description: String? = null,
    val imageUrl: String,
    val linkUrl: String? = null,
    val displayOrder: Int = 0,
    val startDate: String? = null,
    val endDate: String? = null
)

@Serializable
data class UpdateAdvertisementRequest(
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val linkUrl: String? = null,
    val displayOrder: Int? = null,
    val isActive: Boolean? = null,
    val endDate: String? = null
)