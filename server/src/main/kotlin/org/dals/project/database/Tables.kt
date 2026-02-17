package org.dals.project.database

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

// Branches Table
object Branches : UUIDTable("branches") {
    val branchCode = varchar("branch_code", 20).uniqueIndex()
    val name = varchar("name", 255)
    val street = varchar("street", 255)
    val city = varchar("city", 100)
    val state = varchar("state", 50)
    val zipCode = varchar("zip_code", 20)
    val country = varchar("country", 50).default("USA")
    val phoneNumber = varchar("phone_number", 20)
    val managerUserId = uuid("manager_user_id").nullable()
    val operatingHours = text("operating_hours").nullable()
    val status = varchar("status", 50).default("ACTIVE")
    val totalCustomers = integer("total_customers").default(0)
    val totalAccounts = integer("total_accounts").default(0)
    val totalDeposits = decimal("total_deposits", 15, 2).default(java.math.BigDecimal.ZERO)
    val totalLoans = decimal("total_loans", 15, 2).default(java.math.BigDecimal.ZERO)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Users Table
object Users : UUIDTable("users") {
    val username = varchar("username", 50).uniqueIndex()
    val email = varchar("email", 255).uniqueIndex()
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100)
    val phoneNumber = varchar("phone_number", 20)
    val role = enumerationByName<UserRole>("role", 50)
    val status = enumerationByName<UserStatus>("status", 50).default(UserStatus.PENDING_APPROVAL)
    val branchId = uuid("branch_id").nullable()
    val employeeId = varchar("employee_id", 50).nullable()
    val passwordHash = varchar("password_hash", 255)
    val salt = varchar("salt", 255)
    val permissions = text("permissions").default("[]") // JSON array of permission strings
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val lastLoginAt = timestamp("last_login_at").nullable()
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// User Sessions Table
object UserSessions : UUIDTable("user_sessions") {
    val userId = uuid("user_id")
    val role = enumerationByName<UserRole>("role", 50)
    val branchId = uuid("branch_id").nullable()
    val loginTime = timestamp("login_time").defaultExpression(CurrentTimestamp())
    val lastActivity = timestamp("last_activity").defaultExpression(CurrentTimestamp())
    val ipAddress = varchar("ip_address", 45).nullable()
    val userAgent = text("user_agent").nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

// Employees Table
object Employees : UUIDTable("employees") {
    val userId = uuid("user_id").uniqueIndex()
    val employeeNumber = varchar("employee_number", 50).uniqueIndex()
    val department = enumerationByName<Department>("department", 50)
    val position = varchar("position", 100)
    val employmentStatus = enumerationByName<EmploymentStatus>("employment_status", 50).default(EmploymentStatus.ACTIVE)
    val hireDate = date("hire_date")
    val terminationDate = date("termination_date").nullable()
    val salary = decimal("salary", 15, 2).nullable()
    val managerId = uuid("manager_id").nullable()
    val branchId = uuid("branch_id")

    val emergencyContactName = varchar("emergency_contact_name", 255).nullable()
    val emergencyContactPhone = varchar("emergency_contact_phone", 20).nullable()
    val emergencyContactRelation = varchar("emergency_contact_relation", 50).nullable()

    val shiftType = varchar("shift_type", 50).nullable()
    val performanceRating = decimal("performance_rating", 3, 2).nullable()
    val lastReviewDate = date("last_review_date").nullable()
    val nextReviewDate = date("next_review_date").nullable()
    val accessLevel = integer("access_level").default(1)

    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Employee Attendance Table
object EmployeeAttendance : UUIDTable("employee_attendance") {
    val employeeId = uuid("employee_id")
    val date = date("date")
    val checkInTime = timestamp("check_in_time").nullable()
    val checkOutTime = timestamp("check_out_time").nullable()
    val status = varchar("status", 50).default("PRESENT")
    val hoursWorked = decimal("hours_worked", 5, 2).nullable()
    val overtimeHours = decimal("overtime_hours", 5, 2).default(java.math.BigDecimal.ZERO)
    val notes = text("notes").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

// Employee Leaves Table
object EmployeeLeaves : UUIDTable("employee_leaves") {
    val employeeId = uuid("employee_id")
    val leaveType = varchar("leave_type", 50)
    val startDate = date("start_date")
    val endDate = date("end_date")
    val daysCount = integer("days_count")
    val reason = text("reason").nullable()
    val status = varchar("status", 50).default("PENDING")
    val approvedBy = uuid("approved_by").nullable()
    val approvalDate = date("approval_date").nullable()
    val rejectionReason = text("rejection_reason").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Employee Performance Reviews Table
object EmployeePerformanceReviews : UUIDTable("employee_performance_reviews") {
    val employeeId = uuid("employee_id")
    val reviewDate = date("review_date")
    val reviewerId = uuid("reviewer_id")
    val periodStart = date("period_start")
    val periodEnd = date("period_end")
    val overallRating = decimal("overall_rating", 3, 2)
    val strengths = text("strengths").nullable()
    val areasForImprovement = text("areas_for_improvement").nullable()
    val goals = text("goals").nullable()
    val comments = text("comments").nullable()
    val employeeAcknowledgement = bool("employee_acknowledgement").default(false)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

// Customers Table
object Customers : UUIDTable("customers") {
    val customerNumber = varchar("customer_number", 50).uniqueIndex()
    val username = varchar("username", 50).nullable().uniqueIndex() // For customer app login
    val passwordHash = varchar("password_hash", 255).nullable() // For customer app login
    val type = enumerationByName<CustomerType>("type", 50)
    val status = enumerationByName<CustomerStatus>("status", 50).default(CustomerStatus.PROSPECT)
    val firstName = varchar("first_name", 100).nullable()
    val lastName = varchar("last_name", 100).nullable()
    val middleName = varchar("middle_name", 100).nullable()
    val dateOfBirth = date("date_of_birth").nullable()
    val ssn = varchar("ssn", 20).nullable()
    val email = varchar("email", 255).nullable()
    val phoneNumber = varchar("phone_number", 20).nullable()
    val alternatePhone = varchar("alternate_phone", 20).nullable()

    // Primary Address
    val primaryStreet = varchar("primary_street", 255).nullable()
    val primaryCity = varchar("primary_city", 100).nullable()
    val primaryState = varchar("primary_state", 50).nullable()
    val primaryZipCode = varchar("primary_zip_code", 20).nullable()
    val primaryCountry = varchar("primary_country", 50).default("USA")

    // Mailing Address
    val mailingStreet = varchar("mailing_street", 255).nullable()
    val mailingCity = varchar("mailing_city", 100).nullable()
    val mailingState = varchar("mailing_state", 50).nullable()
    val mailingZipCode = varchar("mailing_zip_code", 20).nullable()
    val mailingCountry = varchar("mailing_country", 50).nullable()

    val occupation = varchar("occupation", 100).nullable()
    val employer = varchar("employer", 255).nullable()
    val annualIncome = decimal("annual_income", 15, 2).nullable()
    val creditScore = integer("credit_score").nullable()

    // val debtToIncomeRatio = decimal("debt_to_income_ratio", 5, 4).nullable()
    val branchId = uuid("branch_id")
    val accountManagerId = uuid("account_manager_id").nullable()
    val onboardedDate = date("onboarded_date").clientDefault { LocalDate.now() }
    val lastContactDate = date("last_contact_date").nullable()
    val lastLoginAt = timestamp("last_login_at").nullable() // For customer app login
    val riskLevel = varchar("risk_level", 20).default("LOW")
    val kycStatus = varchar("kyc_status", 50).default("PENDING")

    // Business Customer Fields
    val businessName = varchar("business_name", 255).nullable()
    val businessType = varchar("business_type", 100).nullable()
    val taxId = varchar("tax_id", 50).nullable()
    val businessLicenseNumber = varchar("business_license_number", 100).nullable()

    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Account Types Table - for customer care account opening
object AccountTypes : UUIDTable("account_types") {
    val typeName = varchar("type_name", 50).uniqueIndex() // SAVINGS, CHECKING, etc.
    val displayName = varchar("display_name", 100) // "Savings Account", "Checking Account"
    val description = text("description").nullable()
    val minimumDeposit = decimal("minimum_deposit", 15, 2).default(java.math.BigDecimal.ZERO)
    val minimumBalance = decimal("minimum_balance", 15, 2).default(java.math.BigDecimal.ZERO)
    val interestRate = decimal("interest_rate", 5, 4).default(java.math.BigDecimal.ZERO) // Annual rate as decimal
    val monthlyMaintenanceFee = decimal("monthly_maintenance_fee", 15, 2).default(java.math.BigDecimal.ZERO)
    val overdraftLimit = decimal("overdraft_limit", 15, 2).nullable()
    val features = text("features") // JSON array of features
    val isActive = bool("is_active").default(true)
    val category = varchar("category", 50).default("PERSONAL") // PERSONAL, BUSINESS
    val maxTransactionsPerMonth = integer("max_transactions_per_month").nullable()
    val atmWithdrawalLimit = decimal("atm_withdrawal_limit", 15, 2).nullable()
    val onlineBankingEnabled = bool("online_banking_enabled").default(true)
    val mobileBankingEnabled = bool("mobile_banking_enabled").default(true)
    val checkBookAvailable = bool("check_book_available").default(true)
    val debitCardAvailable = bool("debit_card_available").default(true)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Accounts Table
object Accounts : UUIDTable("accounts") {
    val accountNumber = varchar("account_number", 50).uniqueIndex()
    val customerId = uuid("customer_id")
    val type = enumerationByName<AccountType>("type", 50)
    val status = enumerationByName<AccountStatus>("status", 50).default(AccountStatus.PENDING_APPROVAL)
    val balance = decimal("balance", 15, 2).default(java.math.BigDecimal.ZERO)
    val availableBalance = decimal("available_balance", 15, 2).default(java.math.BigDecimal.ZERO)
    val minimumBalance = decimal("minimum_balance", 15, 2).default(java.math.BigDecimal.ZERO)
    val interestRate = decimal("interest_rate", 5, 4).default(java.math.BigDecimal.ZERO)
    val creditLimit = decimal("credit_limit", 15, 2).nullable()
    val branchId = uuid("branch_id")
    val openedDate = date("opened_date").clientDefault { LocalDate.now() }
    val closedDate = date("closed_date").nullable()
    val lastTransactionDate = timestamp("last_transaction_date").nullable()
    val accountManagerId = uuid("account_manager_id").nullable()
    val isJointAccount = bool("is_joint_account").default(false)
    val nickname = varchar("nickname", 100).nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Transactions Table
object Transactions : UUIDTable("transactions") {
    val accountId = uuid("account_id")
    val type = enumerationByName<TransactionType>("type", 50)
    val amount = decimal("amount", 15, 2)
    val status = enumerationByName<TransactionStatus>("status", 50).default(TransactionStatus.PENDING)
    val description = text("description")
    val timestamp = timestamp("timestamp").defaultExpression(CurrentTimestamp())
    val balanceAfter = decimal("balance_after", 15, 2)
    val fromAccountId = uuid("from_account_id").nullable()
    val toAccountId = uuid("to_account_id").nullable()
    val reference = varchar("reference", 100).nullable()
    val processedBy = uuid("processed_by").nullable()
    val branchId = uuid("branch_id").nullable()
    val checkNumber = varchar("check_number", 50).nullable()
    val merchantName = varchar("merchant_name", 255).nullable()
    val category = varchar("category", 100).nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

// Cards Table
object Cards : UUIDTable("cards") {
    val customerId = uuid("customer_id")
    val linkedAccountId = uuid("linked_account_id").nullable() // Link to customer's bank account
    val cardHolderName = varchar("card_holder_name", 255)
    val cardType = enumerationByName<CardType>("card_type", 50)
    val cardBrand = enumerationByName<CardBrand>("card_brand", 50)
    val cardNumberHash = varchar("card_number_hash", 255) // Store hashed card number for security
    val lastFourDigits = varchar("last_four_digits", 4)
    val expiryMonth = integer("expiry_month")
    val expiryYear = integer("expiry_year")
    val cvvHash = varchar("cvv_hash", 255).nullable() // Store hashed CVV (optional, usually not stored)
    val isDefault = bool("is_default").default(false)
    val isActive = bool("is_active").default(true)
    val status = enumerationByName<CardStatus>("status", 50).default(CardStatus.PENDING_VERIFICATION)
    val nickname = varchar("nickname", 100).nullable()
    val addedDate = timestamp("added_date").defaultExpression(CurrentTimestamp())
    val verifiedDate = timestamp("verified_date").nullable()
    val lastUsedDate = timestamp("last_used_date").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Transaction Reversals Table
object TransactionReversals : UUIDTable("transaction_reversals") {
    val transactionId = uuid("transaction_id")
    val customerId = uuid("customer_id")
    val accountId = uuid("account_id")
    val amount = decimal("amount", 15, 2)
    val reason = text("reason")
    val requestedBy = uuid("requested_by")
    val status = varchar("status", 50).default("PENDING") // PENDING, APPROVED, REJECTED, COMPLETED
    val reviewedBy = uuid("reviewed_by").nullable()
    val reviewNotes = text("review_notes").nullable()
    val rejectionReason = text("rejection_reason").nullable()
    val requestedAt = timestamp("requested_at").defaultExpression(CurrentTimestamp())
    val reviewedAt = timestamp("reviewed_at").nullable()
    val completedAt = timestamp("completed_at").nullable()
    val estimatedCompletionDate = timestamp("estimated_completion_date").nullable()
    val onHoldUntil = timestamp("on_hold_until").nullable()
    val reversalType = varchar("reversal_type", 50).default("REFUND") // REFUND, SEND_TO_RECEIVER
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Loan Applications Table
object LoanApplications : UUIDTable("loan_applications") {
    val customerId = uuid("customer_id")
    val loanType = enumerationByName<LoanType>("loan_type", 50)
    val requestedAmount = decimal("requested_amount", 15, 2)
    val purpose = text("purpose")
    val status = enumerationByName<LoanStatus>("status", 50).default(LoanStatus.APPLIED)
    val applicationDate = date("application_date").clientDefault { LocalDate.now() }
    val reviewedBy = uuid("reviewed_by").nullable()
    val reviewedDate = date("reviewed_date").nullable()
    val approvedAmount = decimal("approved_amount", 15, 2).nullable()
    val interestRate = decimal("interest_rate", 5, 4).nullable()
    val termMonths = integer("term_months").nullable()
    val collateralDescription = text("collateral_description").nullable()
    val collateralValue = decimal("collateral_value", 15, 2).nullable()
    val creditScore = integer("credit_score").nullable()

    // val debtToIncomeRatio = decimal("debt_to_income_ratio", 5, 4).nullable()
    val annualIncome = decimal("annual_income", 15, 2).nullable()
    val employmentHistory = text("employment_history").nullable()
    val comments = text("comments").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Loans Table
object Loans : UUIDTable("loans") {
    val accountId = uuid("account_id").nullable()
    val customerId = uuid("customer_id")
    val applicationId = uuid("application_id")
    val loanType = enumerationByName<LoanType>("loan_type", 50)
    val originalAmount = decimal("original_amount", 15, 2)
    val currentBalance = decimal("current_balance", 15, 2)
    val interestRate = decimal("interest_rate", 5, 4)
    val termMonths = integer("term_months")
    val monthlyPayment = decimal("monthly_payment", 15, 2)
    val paymentFrequency =
        enumerationByName<PaymentFrequency>("payment_frequency", 50).default(PaymentFrequency.MONTHLY)
    val status = enumerationByName<LoanStatus>("status", 50).default(LoanStatus.ACTIVE)
    val originationDate = date("origination_date").clientDefault { LocalDate.now() }
    val maturityDate = date("maturity_date")
    val nextPaymentDate = date("next_payment_date")
    val lastPaymentDate = date("last_payment_date").nullable()
    val paymentsRemaining = integer("payments_remaining").nullable()
    val totalInterestPaid = decimal("total_interest_paid", 15, 2).default(java.math.BigDecimal.ZERO)
    val totalPrincipalPaid = decimal("total_principal_paid", 15, 2).default(java.math.BigDecimal.ZERO)
    val latePaymentFees = decimal("late_payment_fees", 15, 2).default(java.math.BigDecimal.ZERO)
    val collateralDescription = text("collateral_description").nullable()
    val collateralValue = decimal("collateral_value", 15, 2).nullable()
    val loanOfficerId = uuid("loan_officer_id")
    val branchId = uuid("branch_id")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Loan Payments Table
object LoanPayments : UUIDTable("loan_payments") {
    val loanId = uuid("loan_id")
    val paymentDate = date("payment_date").clientDefault { LocalDate.now() }
    val amount = decimal("amount", 15, 2)
    val principalAmount = decimal("principal_amount", 15, 2)
    val interestAmount = decimal("interest_amount", 15, 2)
    val feeAmount = decimal("fee_amount", 15, 2).default(java.math.BigDecimal.ZERO)
    val balanceAfter = decimal("balance_after", 15, 2)
    val paymentMethod = varchar("payment_method", 50)
    val reference = varchar("reference", 100).nullable()
    val processedBy = uuid("processed_by")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

// Credit Assessments Table
object CreditAssessments : UUIDTable("credit_assessments") {
    val customerId = uuid("customer_id")
    val assessmentDate = date("assessment_date").clientDefault { LocalDate.now() }
    val creditScore = integer("credit_score")
    val debtToIncomeRatio = decimal("debt_to_income_ratio", 5, 4)
    val annualIncome = decimal("annual_income", 15, 2)
    val existingDebt = decimal("existing_debt", 15, 2)
    val paymentHistory = varchar("payment_history", 20)
    val riskLevel = varchar("risk_level", 20)
    val recommendedCreditLimit = decimal("recommended_credit_limit", 15, 2)
    val assessedBy = uuid("assessed_by")
    val comments = text("comments").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

// KYC Profiles Table
object KycProfiles : UUIDTable("kyc_profiles") {
    val customerId = uuid("customer_id").uniqueIndex()
    val customerName = varchar("customer_name", 255)
    val customerType = enumerationByName<CustomerType>("customer_type", 50)
    val overallKycStatus =
        enumerationByName<KycDocumentStatus>("overall_kyc_status", 50).default(KycDocumentStatus.PENDING_UPLOAD)
    val complianceLevel = enumerationByName<ComplianceLevel>("compliance_level", 50).default(ComplianceLevel.BASIC)
    val lastReviewDate = date("last_review_date").nullable()
    val nextReviewDate = date("next_review_date").nullable()
    val riskRating = varchar("risk_rating", 20).default("LOW")
    val completionPercentage = decimal("completion_percentage", 5, 2).default(java.math.BigDecimal.ZERO)
    val flags = text("flags").nullable()
    val notes = text("notes").nullable()
    val assignedOfficer = uuid("assigned_officer").nullable()
    val createdDate = date("created_date").clientDefault { LocalDate.now() }
    val lastUpdatedDate = date("last_updated_date").clientDefault { LocalDate.now() }
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// KYC Documents Table
object KycDocuments : UUIDTable("kyc_documents") {
    val customerId = uuid("customer_id")
    val customerName = varchar("customer_name", 255)
    val documentType = enumerationByName<KycDocumentType>("document_type", 50)
    val fileName = varchar("file_name", 255)
    val originalFileName = varchar("original_file_name", 255).nullable()
    val filePath = varchar("file_path", 500).nullable()
    val fileSize = long("file_size").default(0)
    val mimeType = varchar("mime_type", 100).nullable()
    val uploadDate = date("upload_date").clientDefault { LocalDate.now() }
    val lastModifiedDate = date("last_modified_date").nullable()
    val expiryDate = date("expiry_date").nullable()
    val issueDate = date("issue_date").nullable()
    val issuingAuthority = varchar("issuing_authority", 255).nullable()
    val documentNumber = varchar("document_number", 100).nullable()
    val status = enumerationByName<KycDocumentStatus>("status", 50).default(KycDocumentStatus.PENDING_UPLOAD)
    val priority = enumerationByName<DocumentPriority>("priority", 50).default(DocumentPriority.MEDIUM)
    val complianceLevel = enumerationByName<ComplianceLevel>("compliance_level", 50).default(ComplianceLevel.BASIC)
    val uploadedBy = uuid("uploaded_by")
    val verifiedBy = uuid("verified_by").nullable()
    val verificationDate = date("verification_date").nullable()
    val rejectedBy = uuid("rejected_by").nullable()
    val rejectionDate = date("rejection_date").nullable()
    val rejectionReason = text("rejection_reason").nullable()
    val notes = text("notes").nullable()
    val internalNotes = text("internal_notes").nullable()
    val isRequired = bool("is_required").default(true)
    val isConfidential = bool("is_confidential").default(true)
    val requiresManualReview = bool("requires_manual_review").default(false)
    val riskScore = decimal("risk_score", 5, 2).nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Document Audit Entries Table
object DocumentAuditEntries : UUIDTable("document_audit_entries") {
    val documentId = uuid("document_id")
    val action = varchar("action", 100)
    val performedBy = uuid("performed_by")
    val timestamp = timestamp("timestamp").defaultExpression(CurrentTimestamp())
    val details = text("details").nullable()
    val previousValue = text("previous_value").nullable()
    val newValue = text("new_value").nullable()
    val ipAddress = varchar("ip_address", 45).nullable()
    val userAgent = text("user_agent").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

// System Alert related tables
object SystemAlerts : UUIDTable("system_alerts") {
    val alertType = varchar("alert_type", 50) // SYSTEM_ERROR, FAILED_LOGIN_ATTEMPTS, HIGH_VALUE_TRANSACTION, etc.
    val severity = varchar("severity", 20) // LOW, MEDIUM, HIGH, CRITICAL
    val title = varchar("title", 255)
    val message = text("message")
    val details = text("details").nullable()
    val isResolved = bool("is_resolved").default(false)
    val actionRequired = bool("action_required").default(false)
    val resolvedBy = uuid("resolved_by").nullable()
    val resolution = text("resolution").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val resolvedAt = timestamp("resolved_at").nullable()
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Compliance Alert related tables
object ComplianceAlerts : UUIDTable("compliance_alerts") {
    val alertType = varchar("alert_type", 50) // SUSPICIOUS_TRANSACTION, LARGE_CASH_TRANSACTION, AML_VIOLATION, etc.
    val customerId = uuid("customer_id").nullable()
    val accountId = uuid("account_id").nullable()
    val transactionId = uuid("transaction_id").nullable()
    val amount = decimal("amount", 15, 2).nullable()
    val description = text("description")
    val riskScore = decimal("risk_score", 3, 1) // e.g., 8.5
    val priority = varchar("priority", 20) // LOW, MEDIUM, HIGH, CRITICAL
    val reviewStatus = varchar("review_status", 30) // PENDING, IN_REVIEW, CLEARED, ESCALATED
    val reviewedBy = uuid("reviewed_by").nullable()
    val reviewComments = text("review_comments").nullable()
    val flaggedBy = varchar("flagged_by", 50).default("SYSTEM")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val reviewedAt = timestamp("reviewed_at").nullable()
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Workflow Approval related tables
object WorkflowApprovals : UUIDTable("workflow_approvals") {
    val workflowType =
        varchar("workflow_type", 50) // LOAN_APPLICATION, CUSTOMER_ACCOUNT_OPENING, LARGE_TRANSACTION, etc.
    val entityType = varchar("entity_type", 50) // LOAN_APPLICATION, ACCOUNT, TRANSACTION, etc.
    val entityId = varchar("entity_id", 50) // ID of the entity being approved
    val requesterId = uuid("requester_id") // User who initiated the request
    val currentApproverRole = varchar("current_approver_role", 50) // Role required for current approval step
    val status = varchar("status", 30) // PENDING, IN_PROGRESS, APPROVED, REJECTED
    val priority = varchar("priority", 20) // LOW, NORMAL, HIGH, CRITICAL
    val requestData = text("request_data") // JSON data about the request
    val currentStepIndex = integer("current_step_index").default(0)
    val totalSteps = integer("total_steps").default(1)
    val approvedBy = uuid("approved_by").nullable()
    val rejectedBy = uuid("rejected_by").nullable()
    val approvalComments = text("approval_comments").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val deadline = timestamp("deadline").nullable()
    val processedAt = timestamp("processed_at").nullable()
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Compliance related tables
object ComplianceChecks : UUIDTable("compliance_checks") {
    val customerId = uuid("customer_id")
    val checkType = varchar("check_type", 30) // FATCA, PEP, SANCTIONS, AML
    val status = varchar("status", 30) // CLEAR, PENDING_REVIEW, FLAGGED, etc.
    val riskScore = integer("risk_score")
    val riskLevel = varchar("risk_level", 20) // LOW, MEDIUM, HIGH, CRITICAL
    val checkedBy = uuid("checked_by")
    val findings = text("findings").nullable()
    val recommendations = text("recommendations").nullable()
    val nextReviewDate = timestamp("next_review_date").nullable()
    val notes = text("notes").nullable()
    val checkedAt = timestamp("checked_at").defaultExpression(CurrentTimestamp())
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Service Request related tables
object ServiceRequests : UUIDTable("service_requests") {
    val customerId = uuid("customer_id")
    val requestType = varchar("request_type", 50) // ATM_CARD_REQUEST, PIN_RESET, ACCOUNT_OPENING, etc.
    val title = varchar("title", 255)
    val description = text("description")
    val status = varchar("status", 30) // PENDING, IN_PROGRESS, COMPLETED, REJECTED, CANCELLED
    val priority = varchar("priority", 20) // LOW, MEDIUM, HIGH, URGENT
    val createdBy = uuid("created_by")
    val assignedTo = uuid("assigned_to").nullable()
    val completedBy = uuid("completed_by").nullable()
    val estimatedCompletionDate = timestamp("estimated_completion_date").nullable()
    val actualCompletionDate = timestamp("actual_completion_date").nullable()
    val rejectionReason = text("rejection_reason").nullable()
    val approvalRequired = bool("approval_required").default(false)
    val approvedBy = uuid("approved_by").nullable()
    val approvedAt = timestamp("approved_at").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
    val completedAt = timestamp("completed_at").nullable()
}

// Complaints Table
object Complaints : UUIDTable("complaints") {
    val customerId = uuid("customer_id")
    val complaintType = varchar("complaint_type", 100) // SERVICE_ISSUE, BILLING_DISPUTE, FRAUD_REPORT, etc.
    val title = varchar("title", 255)
    val description = text("description")
    val status = varchar("status", 30).default("OPEN") // OPEN, IN_PROGRESS, RESOLVED, CLOSED, ESCALATED
    val priority = varchar("priority", 20).default("MEDIUM") // LOW, MEDIUM, HIGH, CRITICAL
    val category = varchar("category", 100).nullable() // Transaction, Account, Service, etc.
    val reportedBy = uuid("reported_by") // Customer Care Officer who filed the complaint
    val assignedTo = uuid("assigned_to").nullable()
    val resolvedBy = uuid("resolved_by").nullable()
    val escalatedTo = uuid("escalated_to").nullable()
    val escalationLevel = integer("escalation_level").default(0)
    val resolutionNotes = text("resolution_notes").nullable()
    val customerSatisfaction = integer("customer_satisfaction").nullable() // 1-5 rating
    val followUpRequired = bool("follow_up_required").default(false)
    val followUpDate = timestamp("follow_up_date").nullable()
    val reportedAt = timestamp("reported_at").defaultExpression(CurrentTimestamp())
    val resolvedAt = timestamp("resolved_at").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Customer Care Access Requests Table
object CustomerCareAccessRequests : UUIDTable("customer_care_access_requests") {
    val customerId = uuid("customer_id").references(Customers.id, onDelete = ReferenceOption.CASCADE)
    val customerName = varchar("customer_name", 255)
    val customerEmail = varchar("customer_email", 255)
    val customerPhone = varchar("customer_phone", 20)
    val requestType = varchar("request_type", 50) // FULL_ACCESS, LIMITED_ACCESS, VIEW_ONLY
    val reason = text("reason")
    val requestedPermissions = text("requested_permissions") // JSON array of permissions
    val status = varchar("status", 30).default("PENDING") // PENDING, APPROVED, REJECTED, REVOKED
    val priority = varchar("priority", 20).default("MEDIUM") // LOW, MEDIUM, HIGH, URGENT
    val requestedBy = uuid("requested_by") // Customer who made the request
    val reviewedBy = uuid("reviewed_by").nullable() // Admin who reviewed
    val approvedBy = uuid("approved_by").nullable() // Admin who approved
    val rejectedBy = uuid("rejected_by").nullable() // Admin who rejected
    val revokedBy = uuid("revoked_by").nullable() // Admin who revoked
    val reviewNotes = text("review_notes").nullable()
    val rejectionReason = text("rejection_reason").nullable()
    val revocationReason = text("revocation_reason").nullable()
    val approvedAt = timestamp("approved_at").nullable()
    val rejectedAt = timestamp("rejected_at").nullable()
    val revokedAt = timestamp("revoked_at").nullable()
    val expiresAt = timestamp("expires_at").nullable() // Access expiration
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Notifications Table
object Notifications : UUIDTable("notifications") {
    val userId = uuid("user_id") // Can reference Customers or Users
    val userType = varchar("user_type", 20) // CUSTOMER, EMPLOYEE
    val title = varchar("title", 255)
    val message = text("message")
    val type = varchar("type", 50) // TRANSACTION_COMPLETED, LOAN_APPROVED, etc.
    val category = varchar("category", 50).default("GENERAL") // TRANSACTION, LOAN, PAYMENT, SECURITY, SYSTEM
    val priority = varchar("priority", 20).default("NORMAL") // LOW, NORMAL, HIGH, URGENT
    val isRead = bool("is_read").default(false)
    val actionUrl = varchar("action_url", 500).nullable()
    val relatedEntityType = varchar("related_entity_type", 50).nullable() // TRANSACTION, LOAN, ACCOUNT
    val relatedEntityId = uuid("related_entity_id").nullable()
    val metadata = text("metadata").nullable() // JSON for additional data
    val expiresAt = timestamp("expires_at").nullable()
    val readAt = timestamp("read_at").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

// Account Freeze related tables
object AccountFreezeRequests : UUIDTable("account_freeze_requests") {
    val accountId = uuid("account_id")
    val customerId = uuid("customer_id")
    val freezeType = varchar("freeze_type", 30) // FRAUD, LEGAL, DEATH, SUSPICIOUS_ACTIVITY
    val reason = text("reason")
    val requestedBy = uuid("requested_by")
    val approvedBy = uuid("approved_by").nullable()
    val unfrozenBy = uuid("unfrozen_by").nullable()
    val status = varchar("status", 20) // PENDING, ACTIVE, LIFTED
    val notes = text("notes").nullable()
    val legalDocumentPath = varchar("legal_document_path", 500).nullable()
    val requestedAt = timestamp("requested_at").defaultExpression(CurrentTimestamp())
    val approvedAt = timestamp("approved_at").nullable()
    val effectiveAt = timestamp("effective_at").nullable()
    val unfrozenAt = timestamp("unfrozen_at").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Audit Log related tables
object AuditLogs : UUIDTable("audit_logs") {
    val userId = uuid("user_id") // User who performed the action
    val sessionId = uuid("session_id").nullable()
    val action = varchar("action", 50) // LOGIN, CREATE, UPDATE, DELETE, VIEW, etc.
    val entityType = varchar("entity_type", 50) // USER, CUSTOMER, ACCOUNT, TRANSACTION, etc.
    val entityId = varchar("entity_id", 50) // ID of the affected entity
    val oldValue = text("old_value").nullable() // JSON of old values
    val newValue = text("new_value").nullable() // JSON of new values
    val description = text("description")
    val ipAddress = varchar("ip_address", 45)
    val userAgent = text("user_agent").nullable()
    val branchId = uuid("branch_id").nullable()
    val timestamp = timestamp("timestamp").defaultExpression(CurrentTimestamp())
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

// Master Wallets Table
object MasterWallets : UUIDTable("master_wallets") {
    val walletName = varchar("wallet_name", 255)
    val walletType = enumerationByName<MasterWalletType>("wallet_type", 50)
    val currency = varchar("currency", 10).default("USD")
    val balance = decimal("balance", 18, 2).default(java.math.BigDecimal.ZERO)
    val availableBalance = decimal("available_balance", 18, 2).default(java.math.BigDecimal.ZERO)
    val reserveBalance = decimal("reserve_balance", 18, 2).default(java.math.BigDecimal.ZERO)
    val securityLevel = enumerationByName<WalletSecurityLevel>("security_level", 50)
    val status = enumerationByName<WalletStatus>("status", 50).default(WalletStatus.ACTIVE)
    val maxSingleTransaction = decimal("max_single_transaction", 18, 2).default(java.math.BigDecimal("1000000"))
    val dailyTransactionLimit = decimal("daily_transaction_limit", 18, 2).default(java.math.BigDecimal("10000000"))
    val monthlyTransactionLimit = decimal("monthly_transaction_limit", 18, 2).default(java.math.BigDecimal("100000000"))
    val authorizedUsers = text("authorized_users") // JSON array of user IDs
    val encryptionKey = varchar("encryption_key", 512) // AES-256 encryption key
    val lastReconciliationDate = timestamp("last_reconciliation_date").nullable()
    val reconciliationStatus = varchar("reconciliation_status", 50).default("PENDING")
    val createdBy = uuid("created_by")
    val managedBy = uuid("managed_by")
    val branchId = uuid("branch_id").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Master Wallet Transactions Table
object MasterWalletTransactions : UUIDTable("master_wallet_transactions") {
    val walletId = uuid("wallet_id")
    val transactionType = enumerationByName<MasterWalletTransactionType>("transaction_type", 50)
    val amount = decimal("amount", 18, 2)
    val currency = varchar("currency", 10).default("USD")
    val balanceBefore = decimal("balance_before", 18, 2)
    val balanceAfter = decimal("balance_after", 18, 2)
    val description = text("description")
    val reference = varchar("reference", 255).nullable()
    val counterpartyWalletId = uuid("counterparty_wallet_id").nullable()
    val externalAccountId = varchar("external_account_id", 255).nullable()
    val status = enumerationByName<TransactionStatus>("status", 50).default(TransactionStatus.PENDING)
    val riskScore = decimal("risk_score", 5, 2).nullable()
    val riskLevel = varchar("risk_level", 20).default("LOW")
    val approvalRequired = bool("approval_required").default(false)
    val approvalLevel = varchar("approval_level", 50).nullable()
    val approvedBy = uuid("approved_by").nullable()
    val approvedAt = timestamp("approved_at").nullable()
    val processedBy = uuid("processed_by")
    val processedAt = timestamp("processed_at").defaultExpression(CurrentTimestamp())
    val reversalId = uuid("reversal_id").nullable()
    val tags = text("tags").nullable() // JSON array for categorization
    val metadata = text("metadata").nullable() // JSON for additional data
    val branchId = uuid("branch_id").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

// Float Allocations Table
object FloatAllocations : UUIDTable("float_allocations") {
    val sourceWalletId = uuid("source_wallet_id")
    val targetWalletId = uuid("target_wallet_id")
    val allocationType = enumerationByName<AllocationStatus>("allocation_type", 50)
    val amount = decimal("amount", 18, 2)
    val currency = varchar("currency", 10).default("USD")
    val purpose = text("purpose")
    val status = enumerationByName<AllocationStatus>("status", 50).default(AllocationStatus.ACTIVE)
    val expiryDate = timestamp("expiry_date").nullable()
    val actualUsage = decimal("actual_usage", 18, 2).default(java.math.BigDecimal.ZERO)
    val remainingAmount = decimal("remaining_amount", 18, 2)
    val approvalRequired = bool("approval_required").default(true)
    val approvedBy = uuid("approved_by").nullable()
    val approvedAt = timestamp("approved_at").nullable()
    val requestedBy = uuid("requested_by")
    val allocatedBy = uuid("allocated_by")
    val branchId = uuid("branch_id").nullable()
    val notes = text("notes").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Wallet Security Alerts Table
object WalletSecurityAlerts : UUIDTable("wallet_security_alerts") {
    val walletId = uuid("wallet_id").nullable()
    val alertType = enumerationByName<SecurityAlertType>("alert_type", 50)
    val severity = enumerationByName<AlertSeverity>("severity", 20)
    val title = varchar("title", 255)
    val description = text("description")
    val details = text("details").nullable() // JSON with technical details
    val affectedEntityType = varchar("affected_entity_type", 50).nullable()
    val affectedEntityId = varchar("affected_entity_id", 255).nullable()
    val isResolved = bool("is_resolved").default(false)
    val actionRequired = bool("action_required").default(true)
    val assignedTo = uuid("assigned_to").nullable()
    val resolvedBy = uuid("resolved_by").nullable()
    val resolutionNotes = text("resolution_notes").nullable()
    val escalationLevel = integer("escalation_level").default(1)
    val notificationsSent = text("notifications_sent").nullable() // JSON array
    val riskScore = decimal("risk_score", 5, 2).nullable()
    val triggeredBy = varchar("triggered_by", 255).default("SYSTEM")
    val relatedTransactionId = uuid("related_transaction_id").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val resolvedAt = timestamp("resolved_at").nullable()
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Wallet Audit Trail Table
object WalletAuditTrail : UUIDTable("wallet_audit_trail") {
    val walletId = uuid("wallet_id").nullable()
    val userId = uuid("user_id")
    val action = varchar("action", 100)
    val entityType = varchar("entity_type", 50)
    val entityId = varchar("entity_id", 255)
    val oldValue = text("old_value").nullable() // JSON
    val newValue = text("new_value").nullable() // JSON
    val changeDescription = text("change_description")
    val ipAddress = varchar("ip_address", 45)
    val userAgent = text("user_agent").nullable()
    val sessionId = uuid("session_id").nullable()
    val branchId = uuid("branch_id").nullable()
    val riskLevel = varchar("risk_level", 20).default("LOW")
    val complianceFlags = text("compliance_flags").nullable() // JSON array
    val timestamp = timestamp("timestamp").defaultExpression(CurrentTimestamp())
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

// Reconciliation Records Table
object ReconciliationRecords : UUIDTable("reconciliation_records") {
    val walletId = uuid("wallet_id")
    val reconciliationType = varchar("reconciliation_type", 50).default("DAILY")
    val expectedBalance = decimal("expected_balance", 18, 2)
    val actualBalance = decimal("actual_balance", 18, 2)
    val difference = decimal("difference", 18, 2)
    val status = varchar("status", 50).default("PENDING")
    val discrepancies = text("discrepancies").nullable() // JSON array
    val resolutionNotes = text("resolution_notes").nullable()
    val performedBy = uuid("performed_by")
    val reviewedBy = uuid("reviewed_by").nullable()
    val automatedCheck = bool("automated_check").default(true)
    val manualOverride = bool("manual_override").default(false)
    val overrideReason = text("override_reason").nullable()
    val periodStart = timestamp("period_start")
    val periodEnd = timestamp("period_end")
    val transactionCount = integer("transaction_count")
    val totalDebits = decimal("total_debits", 18, 2)
    val totalCredits = decimal("total_credits", 18, 2)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Drawers Master Table - Physical drawer inventory
object Drawers : UUIDTable("drawers") {
    val drawerNumber = varchar("drawer_number", 20).uniqueIndex() // Physical drawer identifier
    val branchId = uuid("branch_id").references(Branches.id, onDelete = ReferenceOption.CASCADE)
    val drawerName = varchar("drawer_name", 100) // Display name like "Drawer 1", "Main Counter Drawer"
    val location = varchar("location", 255).nullable() // Physical location description
    val maxFloatAmount = decimal("max_float_amount", 15, 2).default(java.math.BigDecimal("50000")) // Max cash capacity
    val minFloatAmount = decimal("min_float_amount", 15, 2).default(java.math.BigDecimal("5000")) // Minimum required float
    val status = varchar("status", 20).default("AVAILABLE") // AVAILABLE, ASSIGNED, MAINTENANCE, RETIRED
    val isActive = bool("is_active").default(true)
    val createdBy = uuid("created_by").references(Users.id).nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Drawer Assignments Table - Links drawers to tellers/customer care
object DrawerAssignments : UUIDTable("drawer_assignments") {
    val drawerId = uuid("drawer_id").references(Drawers.id, onDelete = ReferenceOption.CASCADE)
    val userId = uuid("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE) // Teller or Customer Care Officer
    val branchId = uuid("branch_id").references(Branches.id, onDelete = ReferenceOption.CASCADE)
    val assignedBy = uuid("assigned_by").references(Users.id) // Admin who assigned
    val assignedDate = timestamp("assigned_date").defaultExpression(CurrentTimestamp())
    val revokedDate = timestamp("revoked_date").nullable()
    val revokedBy = uuid("revoked_by").references(Users.id).nullable()
    val status = varchar("status", 20).default("ACTIVE") // ACTIVE, REVOKED, EXPIRED
    val accessLevel = varchar("access_level", 30).default("FULL") // FULL, READ_ONLY, LIMITED
    val notes = text("notes").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())

    init {
        uniqueIndex(drawerId, userId, assignedDate)
    }
}

// Teller Cash Drawers Table
object TellerCashDrawers : UUIDTable("teller_cash_drawers") {
    val tellerId = uuid("teller_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val drawerId = uuid("drawer_id").references(Drawers.id, onDelete = ReferenceOption.CASCADE).nullable() // Link to physical drawer
    val branchId = uuid("branch_id").references(Branches.id, onDelete = ReferenceOption.CASCADE)
    val drawerNumber = varchar("drawer_number", 20) // Physical drawer identifier
    val openingBalance = decimal("opening_balance", 15, 2).default(java.math.BigDecimal.ZERO)
    val currentBalance = decimal("current_balance", 15, 2).default(java.math.BigDecimal.ZERO)
    val floatAmount = decimal("float_amount", 15, 2).default(java.math.BigDecimal.ZERO) // Starting cash float
    val totalDeposits = decimal("total_deposits", 15, 2).default(java.math.BigDecimal.ZERO)
    val totalWithdrawals = decimal("total_withdrawals", 15, 2).default(java.math.BigDecimal.ZERO)
    val totalCashIn = decimal("total_cash_in", 15, 2).default(java.math.BigDecimal.ZERO)
    val totalCashOut = decimal("total_cash_out", 15, 2).default(java.math.BigDecimal.ZERO)
    val status = varchar("status", 20).default("ACTIVE") // ACTIVE, CLOSED, SUSPENDED
    val lastReconciliationDate = timestamp("last_reconciliation_date").nullable()
    val reconciliationStatus = varchar("reconciliation_status", 20).default("BALANCED") // BALANCED, OVER, SHORT
    val varianceAmount = decimal("variance_amount", 15, 2).default(java.math.BigDecimal.ZERO)
    val sessionStartTime = timestamp("session_start_time").defaultExpression(CurrentTimestamp())
    val sessionEndTime = timestamp("session_end_time").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())

    init {
        uniqueIndex(tellerId, drawerNumber, sessionStartTime)
    }
}

// Teller Cash Drawer Transactions Table
object TellerCashDrawerTransactions : UUIDTable("teller_cash_drawer_transactions") {
    val drawerId = uuid("drawer_id").references(TellerCashDrawers.id, onDelete = ReferenceOption.CASCADE)
    val tellerId = uuid("teller_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val transactionType = varchar("transaction_type", 30) // CASH_IN, CASH_OUT, DEPOSIT, WITHDRAWAL, RECONCILIATION
    val amount = decimal("amount", 15, 2)
    val balanceBefore = decimal("balance_before", 15, 2)
    val balanceAfter = decimal("balance_after", 15, 2)
    val customerAccountNumber = varchar("customer_account_number", 50).nullable()
    val customerId = uuid("customer_id").nullable()
    val description = text("description")
    val reference = varchar("reference", 100).nullable()
    val receiptNumber = varchar("receipt_number", 50).nullable()
    val chequeNumber = varchar("cheque_number", 50).nullable()
    val currency = varchar("currency", 10).default("USD")
    val exchangeRate = decimal("exchange_rate", 10, 6).default(java.math.BigDecimal.ONE)
    val transactionDate = timestamp("transaction_date").defaultExpression(CurrentTimestamp())
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

// Teller Float Allocations Table  
object TellerFloatAllocations : UUIDTable("teller_float_allocations") {
    val tellerId = uuid("teller_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val branchId = uuid("branch_id").references(Branches.id, onDelete = ReferenceOption.CASCADE)
    val sourceWalletId = uuid("source_wallet_id").references(MasterWallets.id).nullable()
    val allocatedAmount = decimal("allocated_amount", 15, 2)
    val utilizationAmount = decimal("utilization_amount", 15, 2).default(java.math.BigDecimal.ZERO)
    val remainingAmount = decimal("remaining_amount", 15, 2)
    val allocationDate = timestamp("allocation_date").defaultExpression(CurrentTimestamp())
    val expiryDate = timestamp("expiry_date").nullable()
    val status = varchar("status", 20).default("ACTIVE") // ACTIVE, EXPIRED, RECALLED
    val allocatedBy = uuid("allocated_by").references(Users.id).nullable()
    val purpose = text("purpose").nullable()
    val notes = text("notes").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Teller End of Day Reconciliation Table
object TellerReconciliation : UUIDTable("teller_reconciliation") {
    val tellerId = uuid("teller_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val drawerId = uuid("drawer_id").references(TellerCashDrawers.id, onDelete = ReferenceOption.CASCADE)
    val reconciliationDate = date("reconciliation_date").clientDefault { LocalDate.now() }
    val expectedBalance = decimal("expected_balance", 15, 2)
    val actualBalance = decimal("actual_balance", 15, 2)
    val variance = decimal("variance", 15, 2)
    val varianceType = varchar("variance_type", 10) // OVER, SHORT, BALANCED
    val totalTransactions = integer("total_transactions").default(0)
    val totalCashIn = decimal("total_cash_in", 15, 2).default(java.math.BigDecimal.ZERO)
    val totalCashOut = decimal("total_cash_out", 15, 2).default(java.math.BigDecimal.ZERO)
    val denominationBreakdown = text("denomination_breakdown").nullable() // JSON with bill/coin counts
    val notes = text("notes").nullable()
    val supervisorId = uuid("supervisor_id").references(Users.id).nullable()
    val supervisorApproval = bool("supervisor_approval").default(false)
    val status = varchar("status", 20).default("PENDING") // PENDING, APPROVED, REQUIRES_REVIEW
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Transaction Fee Structure Table - Stores fee configuration for different transaction types
object TransactionFeeStructure : UUIDTable("transaction_fee_structure") {
    val transactionType = varchar("transaction_type", 100) // BANK_TO_MPESA, BANK_TO_AIRTEL, EQUITEL_TO_MPESA, etc.
    val minAmount = decimal("min_amount", 15, 2) // Minimum transaction amount for this fee tier
    val maxAmount = decimal("max_amount", 15, 2) // Maximum transaction amount for this fee tier
    val feeAmount = decimal("fee_amount", 15, 2) // Fixed fee amount
    val feePercentage = decimal("fee_percentage", 5, 4).nullable() // Optional percentage-based fee
    val currency = varchar("currency", 10).default("KES")
    val isActive = bool("is_active").default(true)
    val description = text("description").nullable()
    val createdBy = uuid("created_by").references(Users.id)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())

    init {
        uniqueIndex(transactionType, minAmount, maxAmount)
    }
}

// Transaction Fee Records - Tracks all fees collected
object TransactionFeeRecords : UUIDTable("transaction_fee_records") {
    val transactionId = uuid("transaction_id").references(Transactions.id).nullable()
    val customerId = uuid("customer_id").references(Customers.id)
    val accountId = uuid("account_id").references(Accounts.id)
    val transactionType = varchar("transaction_type", 100)
    val transactionAmount = decimal("transaction_amount", 15, 2)
    val feeAmount = decimal("fee_amount", 15, 2)
    val feeStructureId = uuid("fee_structure_id").references(TransactionFeeStructure.id).nullable()
    val profitWalletId = uuid("profit_wallet_id").references(MasterWallets.id) // Links to company profit wallet
    val currency = varchar("currency", 10).default("KES")
    val collectedAt = timestamp("collected_at").defaultExpression(CurrentTimestamp())
    val processedBy = uuid("processed_by").references(Users.id).nullable()
    val branchId = uuid("branch_id").references(Branches.id).nullable()
    val status = varchar("status", 20).default("COLLECTED") // COLLECTED, REVERSED, REFUNDED
    val description = text("description").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

// Enums
enum class UserRole {
    CUSTOMER, TELLER, CASHIER, CUSTOMER_SERVICE_OFFICER,
    ACCOUNT_MANAGER, LOAN_OFFICER, CREDIT_ANALYST,
    BRANCH_MANAGER, SYSTEM_ADMIN
}

enum class UserStatus {
    ACTIVE, INACTIVE, SUSPENDED, PENDING_APPROVAL
}

enum class CustomerType {
    INDIVIDUAL, BUSINESS, CORPORATE, NON_PROFIT
}

enum class CustomerStatus {
    ACTIVE, INACTIVE, SUSPENDED, CLOSED, PROSPECT
}

enum class AccountType {
    CHECKING, SAVINGS, CREDIT, LOAN, INVESTMENT,
    BUSINESS_CHECKING, BUSINESS_SAVINGS
}

enum class AccountStatus {
    ACTIVE, INACTIVE, FROZEN, CLOSED, PENDING_APPROVAL
}

enum class TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER,
    PAYMENT,
    INTEREST_CREDIT,
    FEE_DEBIT,
    CHECK_DEPOSIT,
    ATM_WITHDRAWAL,
    WIRE_TRANSFER,
    DIRECT_DEPOSIT,
    LOAN_PAYMENT,
    LOAN_DISBURSEMENT,
    MPESA_DEPOSIT,
    MPESA_WITHDRAWAL,
    MPESA_B2C_PAYMENT,
    MOBILE_MONEY_DEPOSIT,
    MOBILE_MONEY_WITHDRAWAL,
    REVERSAL
}

enum class TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED,
    CANCELLED,
    REVERSED,

    // M-Pesa specific statuses
    MPESA_PENDING,
    MPESA_SUCCESS,
    MPESA_FAILED,
    MPESA_TIMEOUT,
    MPESA_CANCELLED
}

enum class CardType {
    CREDIT,
    DEBIT
}

enum class CardBrand {
    VISA,
    MASTERCARD,
    AMERICAN_EXPRESS,
    DISCOVER,
    UNKNOWN
}

enum class CardStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    BLOCKED,
    EXPIRED,
    INVALID
}

enum class LoanType {
    PERSONAL_LOAN, HOME_LOAN, AUTO_LOAN, BUSINESS_LOAN,
    STUDENT_LOAN, CREDIT_CARD, LINE_OF_CREDIT,
    EQUIPMENT_LOAN, CONSTRUCTION_LOAN
}

enum class LoanStatus {
    APPLIED, UNDER_REVIEW, APPROVED, REJECTED,
    ACTIVE, PAID_OFF, DEFAULTED, CHARGED_OFF
}

enum class PaymentFrequency {
    WEEKLY, BI_WEEKLY, MONTHLY, QUARTERLY, ANNUALLY
}

enum class KycDocumentType {
    DRIVERS_LICENSE, PASSPORT, SSN_CARD, UTILITY_BILL,
    BANK_STATEMENT, PAY_STUB, TAX_RETURN, BUSINESS_LICENSE,
    ARTICLES_OF_INCORPORATION, EIN_LETTER, PROOF_OF_ADDRESS,
    INCOME_VERIFICATION, EMPLOYMENT_LETTER, INSURANCE_CARD,
    BIRTH_CERTIFICATE, MARRIAGE_CERTIFICATE, DIVORCE_DECREE,
    VISA, GREEN_CARD, MILITARY_ID, STUDENT_ID, TRIBAL_ID,
    OTHER_GOVERNMENT_ID
}

enum class KycDocumentStatus {
    PENDING_UPLOAD, PENDING_REVIEW, UNDER_REVIEW, VERIFIED,
    REJECTED, EXPIRED, ADDITIONAL_INFO_REQUIRED,
    RESUBMISSION_REQUIRED, SUSPENDED, ARCHIVED
}

enum class DocumentPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class ComplianceLevel {
    BASIC, ENHANCED, PREMIUM, INSTITUTIONAL
}

// ===== MASTER WALLET ENUMS =====

enum class MasterWalletType {
    MAIN_VAULT, BRANCH_ALLOCATION, CUSTOMER_FLOAT,
    LOAN_DISBURSEMENT, RESERVE_FUND, OPERATIONAL_FUND,
    REGULATORY_RESERVE, EMERGENCY_FUND, TRANSACTION_FEES_PROFIT
}

enum class WalletSecurityLevel {
    BASIC, MEDIUM, HIGH, MAXIMUM
}

enum class WalletStatus {
    ACTIVE, INACTIVE, SUSPENDED, FROZEN, CLOSED
}

enum class MasterWalletTransactionType {
    CREATE_WALLET, FUND_ALLOCATION, FUND_TRANSFER,
    BRANCH_DISBURSEMENT, CUSTOMER_PAYOUT, LOAN_FUNDING,
    RESERVE_ADJUSTMENT, RECONCILIATION_ADJUSTMENT,
    REVERSAL, EMERGENCY_WITHDRAWAL, REGULATORY_PAYMENT,
    CASH_WITHDRAWAL
}

enum class AllocationStatus {
    ACTIVE, EXPIRED, RECALLED, SUSPENDED
}

enum class SecurityAlertType {
    UNAUTHORIZED_ACCESS, SUSPICIOUS_TRANSACTION, LIMIT_EXCEEDED,
    MULTIPLE_FAILED_ATTEMPTS, UNUSUAL_PATTERN, SYSTEM_BREACH,
    DATA_INTEGRITY_ISSUE, COMPLIANCE_VIOLATION, FRAUD_DETECTION
}

enum class AlertSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class Department {
    EXECUTIVE, OPERATIONS, CUSTOMER_SERVICE, LOANS,
    ACCOUNTS, IT, HR, COMPLIANCE, MARKETING, FINANCE
}

enum class EmploymentStatus {
    ACTIVE, ON_LEAVE, SUSPENDED, TERMINATED, RETIRED
}

enum class OperationType {
    GENERAL, CASH_MANAGEMENT, COMPLIANCE, MAINTENANCE,
    CUSTOMER_SERVICE, SECURITY, AUDIT, TRAINING
}

enum class OperationStatus {
    PENDING, IN_PROGRESS, COMPLETED, CANCELLED, ON_HOLD
}

enum class Priority {
    LOW, MEDIUM, HIGH, CRITICAL
}

// ==================== BRANCH OPERATIONS TABLES ====================

// Branch Operations Table
object BranchOperations : UUIDTable("branch_operations") {
    val branchId = uuid("branch_id")
    val type = enumerationByName<OperationType>("type", 50).default(OperationType.GENERAL)
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val status = enumerationByName<OperationStatus>("status", 50).default(OperationStatus.PENDING)
    val priority = enumerationByName<Priority>("priority", 50).default(Priority.MEDIUM)
    val assignedTo = uuid("assigned_to").nullable()
    val createdBy = uuid("created_by")
    val dueDate = date("due_date").nullable()
    val completedAt = timestamp("completed_at").nullable()
    val notes = text("notes").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Daily Operations Summary Table
object DailyOperationsSummary : UUIDTable("daily_operations_summary") {
    val branchId = uuid("branch_id")
    val date = date("date")
    val accountsOpened = integer("accounts_opened").default(0)
    val loansProcessed = integer("loans_processed").default(0)
    val transactionsCompleted = integer("transactions_completed").default(0)
    val customerInquiries = integer("customer_inquiries").default(0)
    val cashInHand = decimal("cash_in_hand", 15, 2).default(java.math.BigDecimal.ZERO)
    val totalDeposits = decimal("total_deposits", 15, 2).default(java.math.BigDecimal.ZERO)
    val totalWithdrawals = decimal("total_withdrawals", 15, 2).default(java.math.BigDecimal.ZERO)
    val avgWaitTime = decimal("avg_wait_time", 5, 2).nullable()
    val customerSatisfaction = decimal("customer_satisfaction", 3, 2).nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Performance Metrics Table
object PerformanceMetrics : UUIDTable("performance_metrics") {
    val branchId = uuid("branch_id")
    val date = date("date")
    val category = varchar("category", 100)
    val metricName = varchar("metric_name", 255)
    val currentValue = varchar("current_value", 50)
    val targetValue = varchar("target_value", 50)
    val percentage = integer("percentage")
    val trend = varchar("trend", 10).default("STABLE")
    val unit = varchar("unit", 50).nullable()
    val description = text("description").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// Staff Productivity Table
object StaffProductivity : UUIDTable("staff_productivity") {
    val employeeId = uuid("employee_id")
    val branchId = uuid("branch_id")
    val date = date("date")
    val tasksCompleted = integer("tasks_completed").default(0)
    val customersServed = integer("customers_served").default(0)
    val avgResponseTime = decimal("avg_response_time", 5, 2).nullable()
    val rating = decimal("rating", 2, 1).nullable()
    val transactionsProcessed = integer("transactions_processed").default(0)
    val hoursWorked = decimal("hours_worked", 5, 2).nullable()
    val overtimeHours = decimal("overtime_hours", 5, 2).default(java.math.BigDecimal.ZERO)
    val notes = text("notes").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

// ==================== M-PESA INTEGRATION TABLES ====================

object MpesaTransactions : Table("mpesa_transactions") {
    val id = uuid("id").autoGenerate()
    val merchantRequestId = varchar("merchant_request_id", 100)
    val checkoutRequestId = varchar("checkout_request_id", 100).uniqueIndex()
    val phoneNumber = varchar("phone_number", 20)
    val accountNumber = varchar("account_number", 50)
    val amount = decimal("amount", 15, 2)
    val status = varchar("status", 50).default("PENDING")
    val description = text("description")
    val mpesaReceiptNumber = varchar("mpesa_receipt_number", 100).nullable()
    val transactionDate = timestamp("transaction_date").nullable()
    val responseCode = varchar("response_code", 10).nullable()
    val responseDescription = text("response_description").nullable()
    val resultCode = integer("result_code").nullable()
    val resultDescription = text("result_description").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())

    override val primaryKey = PrimaryKey(id)
}

object MpesaPhoneAccountLinks : Table("mpesa_phone_account_links") {
    val id = uuid("id").autoGenerate()
    val phoneNumber = varchar("phone_number", 20)
    val accountId = uuid("account_id").references(Accounts.id, onDelete = ReferenceOption.CASCADE)
    val accountNumber = varchar("account_number", 50)
    val isDefault = bool("is_default").default(false)
    val isVerified = bool("is_verified").default(false)
    val verificationCode = varchar("verification_code", 10).nullable()
    val verificationExpiresAt = timestamp("verification_expires_at").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(phoneNumber, accountId)
    }
}

object MpesaConfiguration : Table("mpesa_configuration") {
    val id = uuid("id").autoGenerate()
    val environment = varchar("environment", 20).default("sandbox")
    val consumerKey = text("consumer_key")
    val consumerSecret = text("consumer_secret")
    val businessShortCode = varchar("business_short_code", 20)
    val passkey = text("passkey")
    val callbackUrl = text("callback_url")
    val timeoutUrl = text("timeout_url").nullable()
    val resultUrl = text("result_url").nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())

    override val primaryKey = PrimaryKey(id)
}

object MpesaAuditTrail : Table("mpesa_audit_trail") {
    val id = uuid("id").autoGenerate()
    val transactionId = uuid("transaction_id").references(MpesaTransactions.id).nullable()
    val action = varchar("action", 100)
    val entityType = varchar("entity_type", 50)
    val entityId = varchar("entity_id", 100)
    val oldValue = text("old_value").nullable()
    val newValue = text("new_value").nullable()
    val performedBy = uuid("performed_by").references(Users.id).nullable()
    val ipAddress = varchar("ip_address", 45).nullable()
    val userAgent = text("user_agent").nullable()
    val timestamp = timestamp("timestamp").defaultExpression(CurrentTimestamp())
    val description = text("description").nullable()
    val amount = decimal("amount", 15, 2).nullable()
    val accountNumber = varchar("account_number", 50).nullable()
    val mpesaReceipt = varchar("mpesa_receipt", 100).nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())

    override val primaryKey = PrimaryKey(id)
}

// Advertisements Table
object Advertisements : UUIDTable("advertisements") {
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val imageUrl = varchar("image_url", 500)
    val linkUrl = varchar("link_url", 500).nullable()
    val displayOrder = integer("display_order").default(0)
    val isActive = bool("is_active").default(true)
    val startDate = timestamp("start_date").defaultExpression(CurrentTimestamp())
    val endDate = timestamp("end_date").nullable()
    val createdBy = uuid("created_by").references(Users.id)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}