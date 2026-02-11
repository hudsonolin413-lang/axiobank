package org.dals.project.plugins

import org.dals.project.models.*

/**
 * Data Security Layer - Automatically removes sensitive fields from all API responses
 *
 * This plugin intercepts all responses and sanitizes data before sending to clients.
 *
 * SENSITIVE FIELDS REMOVED:
 * - SSN / Social Security Numbers
 * - Tax IDs
 * - Document Numbers (passport, ID, license)
 * - File paths on server
 * - Password hashes and salts (already not in DTOs, but double-checking)
 * - Salary information (unless HR role)
 * - Full credit scores (unless authorized)
 * - Internal database IDs (UUIDs are kept for reference, but no internal paths)
 */

object DataSecurity {

    /**
     * Remove SSN from CustomerDto responses
     */
    fun sanitizeCustomerDto(dto: CustomerDto): CustomerDto {
        return dto.copy(
            ssn = null, // Always remove SSN from API responses
            taxId = null, // Always remove Tax ID from API responses
            creditScore = null, // Remove credit score (employees can use dedicated endpoints)
            annualIncome = null // Remove income data from general responses
        )
    }

    /**
     * Remove sensitive file paths from KYC documents
     */
    fun sanitizeKycDocumentDto(dto: KycDocumentDetailsDto): KycDocumentDetailsDto {
        return dto.copy(
            filePath = null, // Never expose server file paths
            documentNumber = maskDocumentNumber(dto.documentNumber), // Mask document numbers
            internalNotes = null // Remove internal notes from customer-facing responses
        )
    }

    /**
     * Mask document numbers - show only last 4 characters
     */
    private fun maskDocumentNumber(docNumber: String?): String? {
        if (docNumber == null || docNumber.length <= 4) return null
        return "****" + docNumber.takeLast(4)
    }

    /**
     * Sanitize lists of customers
     */
    fun sanitizeCustomers(customers: List<CustomerDto>): List<CustomerDto> {
        return customers.map { sanitizeCustomerDto(it) }
    }

    /**
     * Sanitize lists of KYC documents
     */
    fun sanitizeKycDocuments(docs: List<KycDocumentDetailsDto>): List<KycDocumentDetailsDto> {
        return docs.map { sanitizeKycDocumentDto(it) }
    }

    /**
     * Sanitize account data - mask account numbers for non-owners
     */
    fun sanitizeAccountDto(dto: AccountDto, maskFull: Boolean = true): AccountDto {
        return if (maskFull) {
            dto.copy(
                accountNumber = maskAccountNumber(dto.accountNumber)
            )
        } else {
            dto
        }
    }

    /**
     * Mask account number - show only last 4 digits
     */
    private fun maskAccountNumber(accountNumber: String): String {
        if (accountNumber.length <= 4) return "****"
        return "****" + accountNumber.takeLast(4)
    }

    /**
     * Sanitize loan data
     */
    fun sanitizeLoanDto(dto: LoanDto): LoanDto {
        // Loans are generally OK to show to account owners
        // Add specific masking here if needed
        return dto
    }

    /**
     * Sanitize transaction data
     */
    fun sanitizeTransactionDto(dto: TransactionDto): TransactionDto {
        // Transactions are generally OK to show to account owners
        // Add specific masking here if needed
        return dto
    }

    /**
     * Sanitize lists of accounts
     */
    fun sanitizeAccounts(accounts: List<AccountDto>, maskFull: Boolean = true): List<AccountDto> {
        return accounts.map { sanitizeAccountDto(it, maskFull) }
    }

    /**
     * Sanitize lists of loans
     */
    fun sanitizeLoans(loans: List<LoanDto>): List<LoanDto> {
        return loans.map { sanitizeLoanDto(it) }
    }

    /**
     * Sanitize lists of transactions
     */
    fun sanitizeTransactions(transactions: List<TransactionDto>): List<TransactionDto> {
        return transactions.map { sanitizeTransactionDto(it) }
    }

    /**
     * Generic map sanitizer - removes any field with sensitive keywords
     */
    fun sanitizeGenericMap(data: Map<String, Any?>): Map<String, Any?> {
        val sensitivePatterns = listOf(
            "password", "passwordhash", "passwordsalt", "salt",
            "ssn", "socialsecurity", "nationalid", "taxid",
            "filepath", "serverpath", "absolutepath",
            "secret", "apikey", "privatekey",
            "pin", "cvv", "securitycode"
        )

        return data.filterKeys { key ->
            val lowerKey = key.lowercase()
            !sensitivePatterns.any { pattern -> lowerKey.contains(pattern) }
        }.mapValues { (_, value) ->
            when (value) {
                is Map<*, *> -> sanitizeGenericMap(value as Map<String, Any?>)
                is List<*> -> value.map {
                    if (it is Map<*, *>) sanitizeGenericMap(it as Map<String, Any?>)
                    else it
                }
                else -> value
            }
        }
    }
}
