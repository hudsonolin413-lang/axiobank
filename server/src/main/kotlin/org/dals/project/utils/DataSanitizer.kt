package org.dals.project.utils

import org.dals.project.models.*
import kotlinx.serialization.Serializable

/**
 * Data Sanitizer - Removes sensitive information from DTOs before sending to clients.
 *
 * Sensitive fields that should NEVER be exposed:
 * - Password hashes and salts
 * - SSN / National ID numbers
 * - Full account numbers (only show last 4 digits)
 * - Internal database IDs (UUID)
 * - File paths on server
 * - Salary information
 * - Credit scores (except to authorized users)
 * - Document numbers (IDs, passports, etc.)
 */

object DataSanitizer {

    /**
     * Sanitize customer data - remove SSN and other sensitive personal info
     */
    fun sanitizeCustomer(customer: CustomerDto, isEmployee: Boolean = false): CustomerDto {
        return customer.copy(
            ssn = if (isEmployee) customer.ssn?.let { maskSensitiveData(it) } else null,
            // Keep other fields as is for now, but consider masking more for public access
        )
    }

    /**
     * Sanitize account data - mask account numbers except last 4 digits
     */
    fun sanitizeAccount(account: AccountDto, isOwner: Boolean = false): AccountDto {
        return account.copy(
            accountNumber = if (isOwner) account.accountNumber else maskAccountNumber(account.accountNumber),
            // Hide balance from non-owners
            balance = if (isOwner) account.balance else "***.**",
            availableBalance = if (isOwner) account.availableBalance else "***.**",
            creditLimit = if (isOwner) account.creditLimit else null
        )
    }

    /**
     * Sanitize transaction data - hide sensitive transaction details
     */
    fun sanitizeTransaction(transaction: TransactionDto, isOwner: Boolean = false): TransactionDto {
        return if (isOwner) {
            transaction
        } else {
            transaction.copy(
                amount = "***.**",
                balanceAfter = "***.**",
                description = "[REDACTED]"
            )
        }
    }

    /**
     * Sanitize KYC document details - remove file paths and sensitive document numbers
     */
    fun sanitizeKycDocument(doc: KycDocumentDetailsDto, isEmployee: Boolean = false): KycDocumentDetailsDto {
        return doc.copy(
            filePath = null, // NEVER expose server file paths
            documentNumber = if (isEmployee) doc.documentNumber?.let { maskSensitiveData(it) } else null,
            // Keep other fields for employees only
        )
    }

    /**
     * Sanitize employee data - remove salary and sensitive personal info
     */
    fun sanitizeEmployee(employee: EmployeeDto, isHR: Boolean = false): EmployeeDto {
        return employee.copy(
            salary = if (isHR) employee.salary else null,
            emergencyContactName = if (isHR) employee.emergencyContactName else null,
            emergencyContactPhone = if (isHR) employee.emergencyContactPhone else null,
            ssn = null, // NEVER expose SSN in API responses
        )
    }

    /**
     * Sanitize loan data - hide sensitive financial details from non-authorized users
     */
    fun sanitizeLoan(loan: LoanDto, isOwner: Boolean = false): LoanDto {
        return if (isOwner) {
            loan
        } else {
            loan.copy(
                originalAmount = "***.**",
                currentBalance = "***.**",
                monthlyPayment = "***.**"
            )
        }
    }

    /**
     * Sanitize credit assessment - hide detailed financial information
     */
    fun sanitizeCreditAssessment(assessment: CreditAssessmentDto, isAuthorized: Boolean = false): CreditAssessmentDto? {
        // Credit assessments should NEVER be exposed to non-authorized users
        return if (isAuthorized) {
            assessment.copy(
                // Could add additional masking here if needed
            )
        } else {
            null
        }
    }

    /**
     * Mask account number - show only last 4 digits
     * Example: "1234567890" -> "****7890"
     */
    private fun maskAccountNumber(accountNumber: String): String {
        if (accountNumber.length <= 4) return "****"
        val last4 = accountNumber.takeLast(4)
        val masked = "*".repeat((accountNumber.length - 4).coerceAtMost(10))
        return "$masked$last4"
    }

    /**
     * Mask sensitive data - show only first 2 and last 2 characters
     * Example: "123-45-6789" -> "12*****89"
     */
    private fun maskSensitiveData(data: String): String {
        if (data.length <= 4) return "****"
        val first2 = data.take(2)
        val last2 = data.takeLast(2)
        val masked = "*".repeat((data.length - 4).coerceAtMost(10))
        return "$first2$masked$last2"
    }

    /**
     * Sanitize a list of customers
     */
    fun sanitizeCustomers(customers: List<CustomerDto>, isEmployee: Boolean = false): List<CustomerDto> {
        return customers.map { sanitizeCustomer(it, isEmployee) }
    }

    /**
     * Sanitize a list of accounts
     */
    fun sanitizeAccounts(accounts: List<AccountDto>, isOwner: Boolean = false): List<AccountDto> {
        return accounts.map { sanitizeAccount(it, isOwner) }
    }

    /**
     * Sanitize a list of transactions
     */
    fun sanitizeTransactions(transactions: List<TransactionDto>, isOwner: Boolean = false): List<TransactionDto> {
        return transactions.map { sanitizeTransaction(it, isOwner) }
    }

    /**
     * Sanitize a list of loans
     */
    fun sanitizeLoans(loans: List<LoanDto>, isOwner: Boolean = false): List<LoanDto> {
        return loans.map { sanitizeLoan(it, isOwner) }
    }

    /**
     * Remove sensitive fields from any map-based response
     * Use this for generic responses that might contain sensitive data
     */
    fun sanitizeMap(data: Map<String, Any?>): Map<String, Any?> {
        val sensitiveKeys = setOf(
            "password", "passwordHash", "passwordSalt", "salt",
            "ssn", "socialSecurityNumber", "nationalId",
            "pin", "secret", "apiKey", "token",
            "filePath", "serverPath", "internalId"
        )

        return data.filterKeys { key ->
            !sensitiveKeys.any { sensitive -> key.contains(sensitive, ignoreCase = true) }
        }
    }
}

/**
 * DTO for employee with salary field for HR access
 */
@Serializable
data class EmployeeDto(
    val id: String,
    val userId: String,
    val employeeNumber: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val department: String,
    val position: String,
    val employmentStatus: String,
    val hireDate: String,
    val salary: String? = null,
    val branchId: String,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val ssn: String? = null
)
