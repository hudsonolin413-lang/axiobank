package org.dals.project.services

import org.dals.project.database.*
import org.dals.project.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import java.util.UUID
import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class GenerateTaxReportRequest(
    val customerId: String,
    val year: Int,
    val reportType: String // ANNUAL, QUARTERLY, INTEREST_EARNED, CAPITAL_GAINS
)

@Serializable
data class TaxReportResponse(
    val id: String,
    val customerId: String,
    val year: Int,
    val reportType: String,
    val totalIncome: String,
    val totalExpenses: String,
    val interestEarned: String,
    val interestPaid: String,
    val capitalGains: String,
    val dividends: String,
    val taxWithheld: String,
    val documentUrl: String?,
    val status: String,
    val generatedAt: String,
    val createdAt: String
)

@Serializable
data class TaxSummaryResponse(
    val year: Int,
    val totalIncome: String,
    val totalExpenses: String,
    val netIncome: String,
    val interestEarned: String,
    val interestPaid: String,
    val capitalGains: String,
    val dividends: String,
    val taxWithheld: String,
    val estimatedTax: String,
    val reportsGenerated: Int
)

class TaxReportService {

    suspend fun getAllReports(customerId: UUID): List<TaxReportResponse> = DatabaseFactory.dbQuery {
        TaxReports.select { TaxReports.customerId eq customerId }
            .orderBy(TaxReports.year to SortOrder.DESC, TaxReports.generatedAt to SortOrder.DESC)
            .map { row ->
                TaxReportResponse(
                    id = row[TaxReports.id].toString(),
                    customerId = row[TaxReports.customerId].toString(),
                    year = row[TaxReports.year],
                    reportType = row[TaxReports.reportType],
                    totalIncome = row[TaxReports.totalIncome].toString(),
                    totalExpenses = row[TaxReports.totalExpenses].toString(),
                    interestEarned = row[TaxReports.interestEarned].toString(),
                    interestPaid = row[TaxReports.interestPaid].toString(),
                    capitalGains = row[TaxReports.capitalGains].toString(),
                    dividends = row[TaxReports.dividends].toString(),
                    taxWithheld = row[TaxReports.taxWithheld].toString(),
                    documentUrl = row[TaxReports.documentUrl],
                    status = row[TaxReports.status],
                    generatedAt = row[TaxReports.generatedAt].toString(),
                    createdAt = row[TaxReports.createdAt].toString()
                )
            }
    }

    suspend fun getReportsByYear(customerId: UUID, year: Int): List<TaxReportResponse> = DatabaseFactory.dbQuery {
        TaxReports.select {
            (TaxReports.customerId eq customerId) and (TaxReports.year eq year)
        }
            .orderBy(TaxReports.generatedAt to SortOrder.DESC)
            .map { row ->
                TaxReportResponse(
                    id = row[TaxReports.id].toString(),
                    customerId = row[TaxReports.customerId].toString(),
                    year = row[TaxReports.year],
                    reportType = row[TaxReports.reportType],
                    totalIncome = row[TaxReports.totalIncome].toString(),
                    totalExpenses = row[TaxReports.totalExpenses].toString(),
                    interestEarned = row[TaxReports.interestEarned].toString(),
                    interestPaid = row[TaxReports.interestPaid].toString(),
                    capitalGains = row[TaxReports.capitalGains].toString(),
                    dividends = row[TaxReports.dividends].toString(),
                    taxWithheld = row[TaxReports.taxWithheld].toString(),
                    documentUrl = row[TaxReports.documentUrl],
                    status = row[TaxReports.status],
                    generatedAt = row[TaxReports.generatedAt].toString(),
                    createdAt = row[TaxReports.createdAt].toString()
                )
            }
    }

    suspend fun getReportById(reportId: UUID): TaxReportResponse? = DatabaseFactory.dbQuery {
        TaxReports.select { TaxReports.id eq reportId }
            .mapNotNull { row ->
                TaxReportResponse(
                    id = row[TaxReports.id].toString(),
                    customerId = row[TaxReports.customerId].toString(),
                    year = row[TaxReports.year],
                    reportType = row[TaxReports.reportType],
                    totalIncome = row[TaxReports.totalIncome].toString(),
                    totalExpenses = row[TaxReports.totalExpenses].toString(),
                    interestEarned = row[TaxReports.interestEarned].toString(),
                    interestPaid = row[TaxReports.interestPaid].toString(),
                    capitalGains = row[TaxReports.capitalGains].toString(),
                    dividends = row[TaxReports.dividends].toString(),
                    taxWithheld = row[TaxReports.taxWithheld].toString(),
                    documentUrl = row[TaxReports.documentUrl],
                    status = row[TaxReports.status],
                    generatedAt = row[TaxReports.generatedAt].toString(),
                    createdAt = row[TaxReports.createdAt].toString()
                )
            }.singleOrNull()
    }

    suspend fun getTaxSummary(customerId: UUID, year: Int): TaxSummaryResponse = DatabaseFactory.dbQuery {
        val reports = TaxReports.select {
            (TaxReports.customerId eq customerId) and (TaxReports.year eq year)
        }.toList()

        if (reports.isEmpty()) {
            // Calculate from transactions
            val summary = calculateTaxSummaryFromTransactions(customerId, year)
            return@dbQuery summary
        }

        // Aggregate from existing reports
        var totalIncome = java.math.BigDecimal.ZERO
        var totalExpenses = java.math.BigDecimal.ZERO
        var interestEarned = java.math.BigDecimal.ZERO
        var interestPaid = java.math.BigDecimal.ZERO
        var capitalGains = java.math.BigDecimal.ZERO
        var dividends = java.math.BigDecimal.ZERO
        var taxWithheld = java.math.BigDecimal.ZERO

        reports.forEach { row ->
            totalIncome = totalIncome.add(row[TaxReports.totalIncome])
            totalExpenses = totalExpenses.add(row[TaxReports.totalExpenses])
            interestEarned = interestEarned.add(row[TaxReports.interestEarned])
            interestPaid = interestPaid.add(row[TaxReports.interestPaid])
            capitalGains = capitalGains.add(row[TaxReports.capitalGains])
            dividends = dividends.add(row[TaxReports.dividends])
            taxWithheld = taxWithheld.add(row[TaxReports.taxWithheld])
        }

        val netIncome = totalIncome - totalExpenses
        val estimatedTax = calculateEstimatedTax(netIncome, interestEarned, capitalGains, dividends)

        TaxSummaryResponse(
            year = year,
            totalIncome = totalIncome.toString(),
            totalExpenses = totalExpenses.toString(),
            netIncome = netIncome.toString(),
            interestEarned = interestEarned.toString(),
            interestPaid = interestPaid.toString(),
            capitalGains = capitalGains.toString(),
            dividends = dividends.toString(),
            taxWithheld = taxWithheld.toString(),
            estimatedTax = estimatedTax.toString(),
            reportsGenerated = reports.size
        )
    }

    suspend fun generateReport(request: GenerateTaxReportRequest): TaxReportResponse = DatabaseFactory.dbQuery {
        val customerId = UUID.fromString(request.customerId)

        // Calculate tax data from transactions
        val taxData = calculateTaxDataFromTransactions(customerId, request.year, request.reportType)

        // Create report
        val reportId = TaxReports.insertAndGetId {
            it[TaxReports.customerId] = customerId
            it[year] = request.year
            it[reportType] = request.reportType
            it[totalIncome] = taxData["totalIncome"] as java.math.BigDecimal
            it[totalExpenses] = taxData["totalExpenses"] as java.math.BigDecimal
            it[interestEarned] = taxData["interestEarned"] as java.math.BigDecimal
            it[interestPaid] = taxData["interestPaid"] as java.math.BigDecimal
            it[capitalGains] = taxData["capitalGains"] as java.math.BigDecimal
            it[dividends] = taxData["dividends"] as java.math.BigDecimal
            it[taxWithheld] = taxData["taxWithheld"] as java.math.BigDecimal
            it[status] = "GENERATED"
        }

        getReportById(UUID.fromString(reportId.toString()))!!
    }

    suspend fun downloadReport(reportId: UUID): String? = DatabaseFactory.dbQuery {
        val report = TaxReports.select { TaxReports.id eq reportId }.firstOrNull()
        report?.get(TaxReports.documentUrl)
    }

    suspend fun deleteReport(reportId: UUID): Boolean = DatabaseFactory.dbQuery {
        val deleted = TaxReports.deleteWhere {
            Op.build { TaxReports.id eq reportId }
        }
        deleted > 0
    }

    private fun calculateTaxDataFromTransactions(
        customerId: UUID,
        year: Int,
        reportType: String
    ): Map<String, java.math.BigDecimal> {
        // Get customer accounts
        val customerRecord = Customers.select { Customers.id eq customerId }.firstOrNull()
            ?: return emptyTaxData()

        val accounts = Accounts.select { Accounts.customerId eq customerId }.map { it[Accounts.id].value }

        if (accounts.isEmpty()) {
            return emptyTaxData()
        }

        // Aggregate transaction data
        var totalIncome = java.math.BigDecimal.ZERO
        var totalExpenses = java.math.BigDecimal.ZERO
        var interestEarned = java.math.BigDecimal.ZERO
        var interestPaid = java.math.BigDecimal.ZERO
        var capitalGains = java.math.BigDecimal.ZERO
        var dividends = java.math.BigDecimal.ZERO

        accounts.forEach { accountUuid ->
            val transactions = Transactions.select {
                Transactions.accountId eq accountUuid
            }

            transactions.forEach { tx ->
                val amount = tx[Transactions.amount]
                val type = tx[Transactions.type]

                when (type) {
                    TransactionType.DEPOSIT, TransactionType.DIRECT_DEPOSIT, TransactionType.MPESA_DEPOSIT,
                    TransactionType.MOBILE_MONEY_DEPOSIT, TransactionType.CHECK_DEPOSIT,
                    TransactionType.LOAN_DISBURSEMENT, TransactionType.QR_RECEIPT -> totalIncome = totalIncome.add(amount)

                    TransactionType.WITHDRAWAL, TransactionType.PAYMENT, TransactionType.ATM_WITHDRAWAL,
                    TransactionType.MPESA_WITHDRAWAL, TransactionType.MOBILE_MONEY_WITHDRAWAL,
                    TransactionType.LOAN_PAYMENT, TransactionType.QR_PAYMENT -> totalExpenses = totalExpenses.add(amount)

                    TransactionType.INTEREST_CREDIT -> interestEarned = interestEarned.add(amount)

                    TransactionType.FEE_DEBIT -> interestPaid = interestPaid.add(amount)

                    TransactionType.TRANSFER -> {
                        // Transfers can be income or expense depending on direction
                        // For simplicity, we'll count them as neutral
                    }

                    TransactionType.WIRE_TRANSFER, TransactionType.MPESA_B2C_PAYMENT -> {
                        // Count as expenses
                        totalExpenses = totalExpenses.add(amount)
                    }

                    TransactionType.REVERSAL -> {
                        // Reversals are handled separately
                    }

                    TransactionType.BULK_TRANSFER, TransactionType.NFC_PAYMENT, TransactionType.INTERNATIONAL_TRANSFER,
                    TransactionType.CRYPTO_BUY, TransactionType.CRYPTO_SEND -> {
                        totalExpenses = totalExpenses.add(amount)
                    }

                    TransactionType.NFC_RECEIPT, TransactionType.CRYPTO_SELL,
                    TransactionType.CRYPTO_RECEIVE -> {
                        totalIncome = totalIncome.add(amount)
                    }
                }
            }
        }

        // Calculate tax withholding (simplified - 10% of income)
        val taxWithheld = totalIncome.multiply(java.math.BigDecimal("0.10"))

        return mapOf(
            "totalIncome" to totalIncome,
            "totalExpenses" to totalExpenses,
            "interestEarned" to interestEarned,
            "interestPaid" to interestPaid,
            "capitalGains" to capitalGains,
            "dividends" to dividends,
            "taxWithheld" to taxWithheld
        )
    }

    private fun calculateTaxSummaryFromTransactions(
        customerId: UUID,
        year: Int
    ): TaxSummaryResponse {
        val taxData = calculateTaxDataFromTransactions(customerId, year, "ANNUAL")

        val totalIncome = taxData["totalIncome"] ?: java.math.BigDecimal.ZERO
        val totalExpenses = taxData["totalExpenses"] ?: java.math.BigDecimal.ZERO
        val interestEarned = taxData["interestEarned"] ?: java.math.BigDecimal.ZERO
        val interestPaid = taxData["interestPaid"] ?: java.math.BigDecimal.ZERO
        val capitalGains = taxData["capitalGains"] ?: java.math.BigDecimal.ZERO
        val dividends = taxData["dividends"] ?: java.math.BigDecimal.ZERO
        val taxWithheld = taxData["taxWithheld"] ?: java.math.BigDecimal.ZERO

        val netIncome = totalIncome - totalExpenses
        val estimatedTax = calculateEstimatedTax(netIncome, interestEarned, capitalGains, dividends)

        return TaxSummaryResponse(
            year = year,
            totalIncome = totalIncome.toString(),
            totalExpenses = totalExpenses.toString(),
            netIncome = netIncome.toString(),
            interestEarned = interestEarned.toString(),
            interestPaid = interestPaid.toString(),
            capitalGains = capitalGains.toString(),
            dividends = dividends.toString(),
            taxWithheld = taxWithheld.toString(),
            estimatedTax = estimatedTax.toString(),
            reportsGenerated = 0
        )
    }

    private fun calculateEstimatedTax(
        netIncome: java.math.BigDecimal,
        interestEarned: java.math.BigDecimal,
        capitalGains: java.math.BigDecimal,
        dividends: java.math.BigDecimal
    ): java.math.BigDecimal {
        // Simplified progressive tax calculation
        val taxableIncome = netIncome.add(interestEarned).add(capitalGains).add(dividends)

        return when {
            taxableIncome <= java.math.BigDecimal("10000") ->
                taxableIncome.multiply(java.math.BigDecimal("0.10"))
            taxableIncome <= java.math.BigDecimal("50000") ->
                java.math.BigDecimal("1000").add(
                    taxableIncome.subtract(java.math.BigDecimal("10000"))
                        .multiply(java.math.BigDecimal("0.15"))
                )
            taxableIncome <= java.math.BigDecimal("100000") ->
                java.math.BigDecimal("7000").add(
                    taxableIncome.subtract(java.math.BigDecimal("50000"))
                        .multiply(java.math.BigDecimal("0.22"))
                )
            else ->
                java.math.BigDecimal("18000").add(
                    taxableIncome.subtract(java.math.BigDecimal("100000"))
                        .multiply(java.math.BigDecimal("0.30"))
                )
        }
    }

    private fun emptyTaxData(): Map<String, java.math.BigDecimal> {
        return mapOf(
            "totalIncome" to java.math.BigDecimal.ZERO,
            "totalExpenses" to java.math.BigDecimal.ZERO,
            "interestEarned" to java.math.BigDecimal.ZERO,
            "interestPaid" to java.math.BigDecimal.ZERO,
            "capitalGains" to java.math.BigDecimal.ZERO,
            "dividends" to java.math.BigDecimal.ZERO,
            "taxWithheld" to java.math.BigDecimal.ZERO
        )
    }

    suspend fun getAvailableYears(customerId: UUID): List<Int> = DatabaseFactory.dbQuery {
        // Get years from existing reports
        val reportYears = TaxReports.slice(TaxReports.year)
            .select { TaxReports.customerId eq customerId }
            .withDistinct()
            .map { it[TaxReports.year] }

        // Get years from customer account creation to current year
        val customer = Customers.select { Customers.id eq customerId }.firstOrNull()
        val currentYear = java.time.LocalDate.now().year

        if (customer != null) {
            val createdAt = customer[Customers.createdAt].toString()
            val startYear = try {
                createdAt.substring(0, 4).toInt()
            } catch (e: Exception) {
                currentYear - 5
            }

            val allYears = (startYear..currentYear).toList()
            return@dbQuery allYears.sortedDescending()
        }

        reportYears.sortedDescending()
    }
}
