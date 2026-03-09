package org.dals.project.services

import org.dals.project.database.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

data class CreateInternationalTransferRequest(
    val customerId: String,
    val fromAccountId: String,
    val recipientName: String,
    val recipientBank: String,
    val recipientAccountNumber: String,
    val swiftCode: String? = null,
    val iban: String? = null,
    val routingNumber: String? = null,
    val recipientAddress: String,
    val recipientCity: String,
    val recipientCountry: String,
    val amount: Double,
    val targetCurrency: String,
    val purpose: String? = null
)

data class InternationalTransferDto(
    val id: String,
    val customerId: String,
    val fromAccountId: String,
    val recipientName: String,
    val recipientBank: String,
    val recipientAccountNumber: String,
    val swiftCode: String?,
    val iban: String?,
    val routingNumber: String?,
    val recipientAddress: String?,
    val recipientCity: String?,
    val recipientCountry: String,
    val amount: Double,
    val sourceCurrency: String,
    val targetCurrency: String,
    val exchangeRate: Double,
    val convertedAmount: Double,
    val transferFee: Double,
    val intermediaryFee: Double,
    val totalDeducted: Double,
    val purpose: String?,
    val referenceNumber: String,
    val swiftReference: String?,
    val status: String,
    val estimatedDelivery: String?,
    val completedAt: String?,
    val failureReason: String?,
    val transactionId: String?,
    val correspondentBank: String?,
    val trackingStatus: String?,
    val createdAt: String
)

data class ExchangeRateDto(
    val fromCurrency: String,
    val toCurrency: String,
    val rate: Double,
    val buyRate: Double,
    val sellRate: Double,
    val validUntil: String?
)

data class TransferQuoteDto(
    val amount: Double,
    val sourceCurrency: String,
    val targetCurrency: String,
    val exchangeRate: Double,
    val convertedAmount: Double,
    val transferFee: Double,
    val estimatedDelivery: String,
    val totalCost: Double
)

object InternationalTransferService {

    fun getTransferQuote(
        amount: Double,
        fromCurrency: String,
        toCurrency: String,
        recipientCountry: String
    ): Result<TransferQuoteDto> {
        return try {
            transaction {
                val exchangeRate = getExchangeRate(fromCurrency, toCurrency)
                    ?: return@transaction Result.failure(Exception("Exchange rate not available for $fromCurrency to $toCurrency"))

                val convertedAmount = BigDecimal(amount * exchangeRate.sellRate)
                    .setScale(2, RoundingMode.HALF_UP).toDouble()

                val transferFee = calculateTransferFee(amount, recipientCountry)
                val totalCost = amount + transferFee

                val estimatedDelivery = calculateEstimatedDelivery(recipientCountry)

                Result.success(
                    TransferQuoteDto(
                        amount = amount,
                        sourceCurrency = fromCurrency,
                        targetCurrency = toCurrency,
                        exchangeRate = exchangeRate.sellRate,
                        convertedAmount = convertedAmount,
                        transferFee = transferFee,
                        estimatedDelivery = estimatedDelivery,
                        totalCost = totalCost
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun createInternationalTransfer(request: CreateInternationalTransferRequest): Result<InternationalTransferDto> {
        return try {
            transaction {
                // Validate account and balance
                val account = Accounts.select {
                    (Accounts.id eq UUID.fromString(request.fromAccountId)) and
                    (Accounts.customerId eq UUID.fromString(request.customerId))
                }.singleOrNull()
                    ?: return@transaction Result.failure(Exception("Account not found"))

                val currentBalance = account[Accounts.balance].toDouble()

                // Get exchange rate
                val exchangeRate = getExchangeRate("USD", request.targetCurrency)
                    ?: return@transaction Result.failure(Exception("Exchange rate not available"))

                // Calculate amounts
                val convertedAmount = BigDecimal(request.amount * exchangeRate.sellRate)
                    .setScale(2, RoundingMode.HALF_UP).toDouble()

                val transferFee = calculateTransferFee(request.amount, request.recipientCountry)
                val intermediaryFee = calculateIntermediaryFee(request.recipientCountry)
                val totalDeducted = request.amount + transferFee + intermediaryFee

                // Check balance
                if (currentBalance < totalDeducted) {
                    return@transaction Result.failure(Exception("Insufficient balance. Required: $$totalDeducted, Available: $$currentBalance"))
                }

                // Validate SWIFT/IBAN
                if (request.swiftCode == null && request.iban == null) {
                    return@transaction Result.failure(Exception("Either SWIFT code or IBAN is required"))
                }

                request.swiftCode?.let { swift ->
                    if (!isValidSwiftCode(swift)) {
                        return@transaction Result.failure(Exception("Invalid SWIFT code format"))
                    }
                }

                request.iban?.let { iban ->
                    if (!isValidIBAN(iban)) {
                        return@transaction Result.failure(Exception("Invalid IBAN format"))
                    }
                }

                // Generate reference numbers
                val referenceNumber = generateReferenceNumber()
                val swiftReference = request.swiftCode?.let { generateSwiftReference() }

                // Calculate estimated delivery
                val estimatedDelivery = Instant.now().plusSeconds(
                    calculateDeliveryHours(request.recipientCountry).toLong() * 3600
                )

                // Create international transfer record
                val transferId = InternationalTransfers.insert {
                    it[InternationalTransfers.customerId] = UUID.fromString(request.customerId)
                    it[InternationalTransfers.fromAccountId] = UUID.fromString(request.fromAccountId)
                    it[InternationalTransfers.recipientName] = request.recipientName
                    it[InternationalTransfers.recipientBank] = request.recipientBank
                    it[InternationalTransfers.recipientAccountNumber] = request.recipientAccountNumber
                    it[recipientSwiftCode] = request.swiftCode
                    it[InternationalTransfers.iban] = request.iban
                    it[InternationalTransfers.routingNumber] = request.routingNumber
                    it[InternationalTransfers.recipientAddress] = request.recipientAddress
                    it[InternationalTransfers.recipientCity] = request.recipientCity
                    it[InternationalTransfers.recipientCountry] = request.recipientCountry
                    it[sendAmount] = BigDecimal.valueOf(request.amount)
                    it[sendCurrency] = "USD"
                    it[receiveAmount] = BigDecimal.valueOf(convertedAmount)
                    it[receiveCurrency] = request.targetCurrency
                    it[InternationalTransfers.exchangeRate] = BigDecimal.valueOf(exchangeRate.sellRate)
                    it[fee] = BigDecimal.valueOf(transferFee)
                    it[InternationalTransfers.intermediaryFee] = BigDecimal.valueOf(intermediaryFee)
                    it[InternationalTransfers.totalDeducted] = BigDecimal.valueOf(totalDeducted)
                    it[InternationalTransfers.purpose] = request.purpose
                    it[reference] = referenceNumber
                    it[InternationalTransfers.swiftReference] = swiftReference
                    it[InternationalTransfers.status] = "PENDING"
                    it[InternationalTransfers.estimatedDelivery] = estimatedDelivery
                    it[InternationalTransfers.correspondentBank] = determineCorrespondentBank(request.recipientCountry)
                }[InternationalTransfers.id].value

                // Deduct from account
                Accounts.update({ Accounts.id eq UUID.fromString(request.fromAccountId) }) {
                    it[balance] = BigDecimal.valueOf(currentBalance - totalDeducted)
                }

                // Create bank transaction
                val transactionId = Transactions.insert {
                    it[accountId] = UUID.fromString(request.fromAccountId)
                    it[type] = TransactionType.INTERNATIONAL_TRANSFER
                    it[Transactions.amount] = BigDecimal.valueOf(totalDeducted)
                    it[Transactions.description] = "International Transfer to ${request.recipientName} - ${request.recipientCountry}"
                    it[Transactions.status] = TransactionStatus.PENDING
                    it[Transactions.category] = "TRANSFER"
                    it[balanceAfter] = BigDecimal.valueOf(currentBalance - totalDeducted)
                    it[Transactions.reference] = referenceNumber
                }[Transactions.id].value

                // Update transfer with transaction ID
                InternationalTransfers.update({ InternationalTransfers.id eq transferId }) {
                    it[InternationalTransfers.transactionId] = transactionId
                    it[status] = "PROCESSING"
                }

                val transfer = InternationalTransfers.select { InternationalTransfers.id eq transferId }
                    .single()

                Result.success(mapToInternationalTransferDto(transfer))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getInternationalTransfer(transferId: String): Result<InternationalTransferDto> {
        return try {
            transaction {
                val transfer = InternationalTransfers.select { InternationalTransfers.id eq UUID.fromString(transferId) }
                    .singleOrNull()
                    ?: return@transaction Result.failure(Exception("Transfer not found"))

                Result.success(mapToInternationalTransferDto(transfer))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCustomerTransfers(customerId: String): Result<List<InternationalTransferDto>> {
        return try {
            transaction {
                val transfers = InternationalTransfers.select {
                    InternationalTransfers.customerId eq UUID.fromString(customerId)
                }.orderBy(InternationalTransfers.createdAt to SortOrder.DESC)
                    .map { mapToInternationalTransferDto(it) }

                Result.success(transfers)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun updateTransferStatus(transferId: String, newStatus: String, failureReason: String? = null): Result<InternationalTransferDto> {
        return try {
            transaction {
                InternationalTransfers.update({ InternationalTransfers.id eq UUID.fromString(transferId) }) {
                    it[status] = newStatus
                    if (newStatus == "COMPLETED") {
                        it[completedAt] = java.time.Instant.now()
                    }
                    if (failureReason != null) {
                        it[InternationalTransfers.failureReason] = failureReason
                    }
                }

                val transfer = InternationalTransfers.select { InternationalTransfers.id eq UUID.fromString(transferId) }
                    .single()

                Result.success(mapToInternationalTransferDto(transfer))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun setExchangeRate(fromCurrency: String, toCurrency: String, rate: Double, buyRate: Double, sellRate: Double): Result<ExchangeRateDto> {
        return try {
            transaction {
                ExchangeRates.deleteWhere {
                    (ExchangeRates.fromCurrency eq fromCurrency) and
                    (ExchangeRates.toCurrency eq toCurrency)
                }

                ExchangeRates.insert {
                    it[ExchangeRates.fromCurrency] = fromCurrency
                    it[ExchangeRates.toCurrency] = toCurrency
                    it[ExchangeRates.rate] = BigDecimal(rate)
                    it[ExchangeRates.buyRate] = BigDecimal(buyRate)
                    it[ExchangeRates.sellRate] = BigDecimal(sellRate)
                    it[sourceType] = "MANUAL"
                }

                Result.success(ExchangeRateDto(fromCurrency, toCurrency, rate, buyRate, sellRate, null))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getExchangeRate(fromCurrency: String, toCurrency: String): ExchangeRateDto? {
        return ExchangeRates.select {
            (ExchangeRates.fromCurrency eq fromCurrency) and
            (ExchangeRates.toCurrency eq toCurrency)
        }.singleOrNull()?.let {
            ExchangeRateDto(
                fromCurrency = it[ExchangeRates.fromCurrency],
                toCurrency = it[ExchangeRates.toCurrency],
                rate = it[ExchangeRates.rate].toDouble(),
                buyRate = it[ExchangeRates.buyRate].toDouble(),
                sellRate = it[ExchangeRates.sellRate].toDouble(),
                validUntil = it[ExchangeRates.validUntil]?.toString()
            )
        }
    }

    private fun calculateTransferFee(amount: Double, country: String): Double {
        val baseFee = 15.0
        val percentageFee = amount * 0.01 // 1%
        return minOf(baseFee + percentageFee, 100.0) // Cap at $100
    }

    private fun calculateIntermediaryFee(country: String): Double {
        return when (country.uppercase()) {
            "GB", "FR", "DE", "IT", "ES" -> 5.0  // Europe
            "CA", "MX" -> 3.0  // North America
            "AU", "NZ" -> 7.0  // Oceania
            else -> 10.0  // Other countries
        }
    }

    private fun calculateEstimatedDelivery(country: String): String {
        val hours = calculateDeliveryHours(country)
        return LocalDateTime.now().plusHours(hours.toLong()).toString()
    }

    private fun calculateDeliveryHours(country: String): Int {
        return when (country.uppercase()) {
            "CA", "MX" -> 24  // 1 day
            "GB", "FR", "DE", "IT", "ES" -> 48  // 2 days
            "AU", "NZ", "JP", "SG" -> 72  // 3 days
            else -> 96  // 4 days
        }
    }

    private fun determineCorrespondentBank(country: String): String {
        return when (country.uppercase()) {
            "GB" -> "Barclays Bank PLC"
            "FR" -> "BNP Paribas"
            "DE" -> "Deutsche Bank AG"
            "JP" -> "MUFG Bank"
            "SG" -> "DBS Bank"
            else -> "JPMorgan Chase Bank"
        }
    }

    private fun isValidSwiftCode(swift: String): Boolean {
        val pattern = Regex("^[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?\$")
        return pattern.matches(swift)
    }

    private fun isValidIBAN(iban: String): Boolean {
        val cleanIban = iban.replace(" ", "")
        return cleanIban.length >= 15 && cleanIban.length <= 34 &&
                cleanIban.substring(0, 2).all { it.isLetter() } &&
                cleanIban.substring(2).all { it.isDigit() || it.isLetter() }
    }

    private fun generateReferenceNumber(): String {
        return "INT${System.currentTimeMillis()}${(1000..9999).random()}"
    }

    private fun generateSwiftReference(): String {
        return "SWIFT${System.currentTimeMillis()}"
    }

    private fun mapToInternationalTransferDto(row: ResultRow) = InternationalTransferDto(
        id = row[InternationalTransfers.id].value.toString(),
        customerId = row[InternationalTransfers.customerId].toString(),
        fromAccountId = row[InternationalTransfers.fromAccountId].toString(),
        recipientName = row[InternationalTransfers.recipientName],
        recipientBank = row[InternationalTransfers.recipientBank],
        recipientAccountNumber = row[InternationalTransfers.recipientAccountNumber],
        swiftCode = row[InternationalTransfers.recipientSwiftCode],
        iban = row[InternationalTransfers.iban],
        routingNumber = row[InternationalTransfers.routingNumber],
        recipientAddress = row[InternationalTransfers.recipientAddress],
        recipientCity = row[InternationalTransfers.recipientCity],
        recipientCountry = row[InternationalTransfers.recipientCountry],
        amount = row[InternationalTransfers.sendAmount].toDouble(),
        sourceCurrency = row[InternationalTransfers.sendCurrency],
        targetCurrency = row[InternationalTransfers.receiveCurrency],
        exchangeRate = row[InternationalTransfers.exchangeRate].toDouble(),
        convertedAmount = row[InternationalTransfers.receiveAmount].toDouble(),
        transferFee = row[InternationalTransfers.fee].toDouble(),
        intermediaryFee = row[InternationalTransfers.intermediaryFee].toDouble(),
        totalDeducted = row[InternationalTransfers.totalDeducted]?.toDouble() ?: 0.0,
        purpose = row[InternationalTransfers.purpose],
        referenceNumber = row[InternationalTransfers.reference],
        swiftReference = row[InternationalTransfers.swiftReference],
        status = row[InternationalTransfers.status],
        estimatedDelivery = row[InternationalTransfers.estimatedDelivery]?.toString(),
        completedAt = row[InternationalTransfers.completedAt]?.toString(),
        failureReason = row[InternationalTransfers.failureReason],
        transactionId = row[InternationalTransfers.transactionId]?.toString(),
        correspondentBank = row[InternationalTransfers.correspondentBank],
        trackingStatus = row[InternationalTransfers.trackingStatus],
        createdAt = row[InternationalTransfers.createdAt].toString()
    )
}
