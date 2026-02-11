package org.dals.project.utils

import kotlinx.datetime.LocalDateTime
import org.dals.project.model.*

object AccountStatementUtils {

    /**
     * Generates a formatted account statement as text
     */
    fun generateStatement(
        userFullName: String,
        accountNumber: String,
        transactions: List<Transaction>,
        walletBalance: WalletBalance,
        periodStart: LocalDateTime,
        periodEnd: LocalDateTime
    ): String {
        val currentDateTime = DateTimeUtils.parseDateTime(DateTimeUtils.getCurrentTimestamp())
            ?: LocalDateTime(2024, 12, 15, 12, 0, 0)

        return buildString {
            appendLine("=" + "=".repeat(60))
            appendLine("                     AXIO BANK STATEMENT")
            appendLine("                  Digital Financial Services")
            appendLine("                    [AxioBank Logo]")
            appendLine("=" + "=".repeat(60))
            appendLine()

            appendLine("Account Holder: $userFullName")
            appendLine("Account Number: $accountNumber")
            appendLine(
                "Statement Period: ${DateTimeUtils.formatDateOnly(periodStart)} to ${
                    DateTimeUtils.formatDateOnly(
                        periodEnd
                    )
                }"
            )
            appendLine("Generated On: ${DateTimeUtils.formatForDisplay(currentDateTime)}")
            appendLine()

            appendLine("-" + "-".repeat(60))
            appendLine("ACCOUNT BALANCE SUMMARY")
            appendLine("-" + "-".repeat(60))
            appendLine(
                "Total Balance:     ${
                    CurrencyUtils.formatAmount(
                        walletBalance.totalBalance,
                        walletBalance.currency
                    )
                }"
            )
            appendLine(
                "Available Balance: ${
                    CurrencyUtils.formatAmount(
                        walletBalance.availableBalance,
                        walletBalance.currency
                    )
                }"
            )
            appendLine(
                "Pending Amount:    ${
                    CurrencyUtils.formatAmount(
                        walletBalance.pendingAmount,
                        walletBalance.currency
                    )
                }"
            )
            appendLine("Last Updated:      ${formatWalletUpdateTime(walletBalance.lastUpdated)}")
            appendLine()

            if (transactions.isEmpty()) {
                appendLine("No transactions found for the selected period.")
                appendLine()
            } else {
                appendLine("-" + "-".repeat(60))
                appendLine("TRANSACTION HISTORY")
                appendLine("-" + "-".repeat(60))
                appendLine()

                // Header
                val headerFormat = "%-12s %-18s %-15s %-12s %-15s %s"
                appendLine(
                    "${"Date".padEnd(12)} ${"Time".padEnd(18)} ${"Type".padEnd(15)} ${"Amount".padEnd(12)} ${
                        "Status".padEnd(
                            15
                        )
                    } Description"
                )
                appendLine("-" + "-".repeat(60))

                var totalDebits = 0.0
                var totalCredits = 0.0

                transactions.sortedByDescending { it.timestamp }.forEach { transaction ->
                    val dateTime = DateTimeUtils.parseDateTime(transaction.timestamp)
                        ?: LocalDateTime(2024, 12, 15, 12, 0, 0)

                    val dateStr = "${dateTime.date.dayOfMonth.toString().padStart(2, '0')}/${
                        dateTime.date.monthNumber.toString().padStart(2, '0')
                    }/${dateTime.date.year}"
                    val timeStr = "${dateTime.time.hour.toString().padStart(2, '0')}:${
                        dateTime.time.minute.toString().padStart(2, '0')
                    }"

                    val typeStr = when (transaction.type) {
                        TransactionType.SEND -> "SEND"
                        TransactionType.RECEIVE -> "RECEIVE"
                        TransactionType.BILL_PAYMENT -> "BILL PAY"
                        TransactionType.LOAN_PAYMENT -> "LOAN PAY"
                        TransactionType.INVESTMENT -> "INVEST"
                        TransactionType.WITHDRAWAL -> "WITHDRAW"
                        TransactionType.DEPOSIT -> "DEPOSIT"
                        TransactionType.RENT_PAYMENT -> "RENT PAY"
                    }

                    val amountStr = when (transaction.type) {
                        TransactionType.RECEIVE, TransactionType.DEPOSIT -> {
                            totalCredits += transaction.amount
                            "+${CurrencyUtils.formatAmount(transaction.amount, transaction.currency)}"
                        }

                        else -> {
                            totalDebits += transaction.amount
                            "-${CurrencyUtils.formatAmount(transaction.amount, transaction.currency)}"
                        }
                    }

                    val statusStr = when (transaction.status) {
                        TransactionStatus.COMPLETED -> "SUCCESS"
                        TransactionStatus.PENDING -> "PENDING"
                        TransactionStatus.FAILED -> "FAILED"
                        TransactionStatus.CANCELLED -> "CANCELLED"
                        TransactionStatus.REVERSED -> "REVERSED"
                    }

                    val description = if (transaction.description.length > 25) {
                        transaction.description.take(22) + "..."
                    } else {
                        transaction.description
                    }

                    appendLine(
                        "${dateStr.padEnd(12)} ${timeStr.padEnd(18)} ${typeStr.padEnd(15)} ${amountStr.padEnd(12)} ${
                            statusStr.padEnd(
                                15
                            )
                        } $description"
                    )

                    // Add recipient info if available
                    if (!transaction.recipientName.isNullOrEmpty()) {
                        appendLine("             To/From: ${transaction.recipientName}")
                    }

                    // Add fees if any
                    if (transaction.fee > 0) {
                        appendLine(
                            "             Fee: ${
                                CurrencyUtils.formatAmount(
                                    transaction.fee,
                                    transaction.currency
                                )
                            }"
                        )
                    }

                    appendLine()
                }

                appendLine("-" + "-".repeat(60))
                appendLine("PERIOD SUMMARY")
                appendLine("-" + "-".repeat(60))
                appendLine("Total Credits: ${CurrencyUtils.formatAmount(totalCredits, walletBalance.currency)}")
                appendLine("Total Debits:  ${CurrencyUtils.formatAmount(totalDebits, walletBalance.currency)}")
                appendLine(
                    "Net Change:    ${
                        CurrencyUtils.formatAmount(
                            totalCredits - totalDebits,
                            walletBalance.currency
                        )
                    }"
                )
                appendLine("Total Transactions: ${transactions.size}")
                appendLine()
            }

            appendLine("-" + "-".repeat(60))
            appendLine("IMPORTANT NOTES")
            appendLine("-" + "-".repeat(60))
            appendLine("• This statement is generated electronically and is valid without signature")
            appendLine("• All times are shown in local timezone")
            appendLine("• For any discrepancies, please contact Axio Bank support")
            appendLine("• Keep this statement for your records")
            appendLine()

            appendLine("Thank you for banking with Axio Bank!")
            appendLine("=" + "=".repeat(60))
        }
    }

    /**
     * Generates a CSV format statement for easy import to spreadsheet applications
     */
    fun generateCSVStatement(
        transactions: List<Transaction>,
        walletBalance: WalletBalance
    ): String {
        return buildString {
            // CSV Header
            appendLine("Date,Time,Transaction ID,Type,Category,Amount,Currency,Fee,Status,Description,Recipient,Reference")

            transactions.sortedByDescending { it.timestamp }.forEach { transaction ->
                val dateTime = DateTimeUtils.parseDateTime(transaction.timestamp)
                    ?: LocalDateTime(2024, 12, 15, 12, 0, 0)

                val date = "${dateTime.date.dayOfMonth.toString().padStart(2, '0')}/${
                    dateTime.date.monthNumber.toString().padStart(2, '0')
                }/${dateTime.date.year}"
                val time = "${dateTime.time.hour.toString().padStart(2, '0')}:${
                    dateTime.time.minute.toString().padStart(2, '0')
                }:${dateTime.time.second.toString().padStart(2, '0')}"

                val amount = when (transaction.type) {
                    TransactionType.RECEIVE, TransactionType.DEPOSIT -> transaction.amount
                    else -> -transaction.amount
                }

                val status = when (transaction.status) {
                    TransactionStatus.REVERSED -> "REVERSED"
                    else -> transaction.status.name
                }

                appendLine("$date,$time,${transaction.id},${transaction.type},${transaction.category},$amount,${transaction.currency},${transaction.fee},$status,\"${transaction.description}\",\"${transaction.recipientName ?: ""}\",\"${transaction.reference ?: ""}\"")
            }
        }
    }

    /**
     * Generates a filename for the statement
     */
    fun generateStatementFileName(
        accountNumber: String,
        periodStart: LocalDateTime,
        periodEnd: LocalDateTime,
        format: StatementFormat
    ): String {
        val currentDateTime = DateTimeUtils.parseDateTime(DateTimeUtils.getCurrentTimestamp())
            ?: LocalDateTime(2024, 12, 15, 12, 0, 0)

        val startDate = "${periodStart.date.year}-${
            periodStart.date.monthNumber.toString().padStart(2, '0')
        }-${periodStart.date.dayOfMonth.toString().padStart(2, '0')}"
        val endDate = "${periodEnd.date.year}-${
            periodEnd.date.monthNumber.toString().padStart(2, '0')
        }-${periodEnd.date.dayOfMonth.toString().padStart(2, '0')}"
        val timestamp = DateTimeUtils.formatForFileName(currentDateTime)

        val extension = when (format) {
            StatementFormat.TXT -> "txt"
            StatementFormat.CSV -> "csv"
        }

        return "AxioBank_Statement_${accountNumber}_${startDate}_to_${endDate}_${timestamp}.$extension"
    }

    /**
     * Generates a summary statement for a specific period
     */
    fun generateSummaryStatement(
        transactions: List<Transaction>,
        walletBalance: WalletBalance,
        period: DateRangePeriod
    ): StatementSummary {
        var totalCredits = 0.0
        var totalDebits = 0.0
        val transactionsByType = transactions.groupBy { it.type }
        val transactionsByStatus = transactions.groupBy { it.status }

        transactions.forEach { transaction ->
            // Skip reversed transactions for credit/debit totals as they are countered by REVERSAL transactions
            if (transaction.status == TransactionStatus.REVERSED) return@forEach

            when (transaction.type) {
                TransactionType.RECEIVE, TransactionType.DEPOSIT -> {
                    totalCredits += transaction.amount
                }

                else -> {
                    totalDebits += transaction.amount
                }
            }
        }

        return StatementSummary(
            period = period,
            totalTransactions = transactions.size,
            totalCredits = totalCredits,
            totalDebits = totalDebits,
            netChange = totalCredits - totalDebits,
            currentBalance = walletBalance.totalBalance,
            transactionsByType = transactionsByType.mapValues { it.value.size },
            transactionsByStatus = transactionsByStatus.mapValues { it.value.size },
            currency = walletBalance.currency
        )
    }

    private fun formatWalletUpdateTime(timestamp: String): String {
        val dateTime = DateTimeUtils.parseDateTime(timestamp)
        return if (dateTime != null) {
            DateTimeUtils.formatForDisplay(dateTime)
        } else {
            timestamp
        }
    }
}

data class StatementSummary(
    val period: DateRangePeriod,
    val totalTransactions: Int,
    val totalCredits: Double,
    val totalDebits: Double,
    val netChange: Double,
    val currentBalance: Double,
    val transactionsByType: Map<TransactionType, Int>,
    val transactionsByStatus: Map<TransactionStatus, Int>,
    val currency: String
)

enum class StatementFormat {
    TXT, CSV
}