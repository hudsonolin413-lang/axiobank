package org.dals.project.utils

import java.util.*
import kotlin.random.Random

object IdGenerator {

    /**
     * Generates a transaction ID in format: TKG60ACDU9
     * - 3 uppercase letters
     * - 2 digits
     * - 5 uppercase letters/digits
     * Total: 10 characters
     */
    fun generateTransactionId(): String {
        val letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val digits = "0123456789"
        val alphanumeric = letters + digits

        val part1 = (1..3).map { letters.random() }.joinToString("")
        val part2 = (1..2).map { digits.random() }.joinToString("")
        val part3 = (1..5).map { alphanumeric.random() }.joinToString("")

        return part1 + part2 + part3
    }

    /**
     * Generates a 6-digit user/customer ID
     * Format: 123456
     */
    fun generateUserId(): String {
        return Random.nextInt(100000, 999999).toString()
    }

    /**
     * Generates a 7-digit customer number
     * Format: 1234567
     */
    fun generateCustomerNumber(): String {
        return Random.nextInt(1000000, 9999999).toString()
    }

    /**
     * Generates a 7-digit numeric account number
     * Format: 1234567 (7 digits, range 1000000-9999999)
     * Matches database constraint: CHECK (account_number ~ '^[0-9]{7}$')
     */
    fun generateAccountNumber(): String {
        return Random.nextInt(1000000, 9999999).toString()
    }
}
