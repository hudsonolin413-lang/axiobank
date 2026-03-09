package org.dals.project.services

import org.dals.project.database.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random

data class CreateVirtualCardRequest(
    val customerId: String,
    val accountId: String,
    val cardType: String = "VIRTUAL",
    val spendingLimit: Double? = null,
    val merchantRestrictions: List<String>? = null,
    val categoryRestrictions: List<String>? = null,
    val expiresInMinutes: Int? = null,
    val purpose: String? = null
)

data class VirtualCardDto(
    val id: String,
    val customerId: String,
    val accountId: String,
    val cardNumber: String,
    val cardHolderName: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val cvv: String,
    val cardType: String,
    val spendingLimit: Double?,
    val usedAmount: Double,
    val merchantRestrictions: List<String>?,
    val categoryRestrictions: List<String>?,
    val expiresAt: String?,
    val status: String,
    val lastUsedAt: String?,
    val usageCount: Int,
    val purpose: String?,
    val createdAt: String
)

object VirtualCardService {

    fun createVirtualCard(request: CreateVirtualCardRequest): Result<VirtualCardDto> {
        return try {
            transaction {
                val customer = Customers.select { Customers.id eq UUID.fromString(request.customerId) }.singleOrNull()
                    ?: return@transaction Result.failure(Exception("Customer not found"))

                val cardNumber = generateCardNumber()
                val cvv = String.format("%03d", Random.nextInt(100, 1000))
                val expiryMonth = Random.nextInt(1, 13)
                val expiryYear = LocalDateTime.now().year + Random.nextInt(2, 6)

                val cardId = VirtualCards.insert {
                    it[customerId] = UUID.fromString(request.customerId)
                    it[accountId] = UUID.fromString(request.accountId)
                    it[VirtualCards.cardNumber] = encrypt(cardNumber)
                    it[cardHolderName] = "${customer[Customers.firstName]} ${customer[Customers.lastName]}"
                    it[VirtualCards.expiryMonth] = expiryMonth
                    it[VirtualCards.expiryYear] = expiryYear
                    it[VirtualCards.cvv] = encrypt(cvv)
                    it[cardType] = request.cardType
                    it[spendingLimit] = request.spendingLimit?.let { limit -> BigDecimal.valueOf(limit) }
                    it[merchantRestrictions] = request.merchantRestrictions?.joinToString(",")
                    it[categoryRestrictions] = request.categoryRestrictions?.joinToString(",")
                    it[expiresAt] = request.expiresInMinutes?.let { mins -> Instant.now().plusSeconds(mins.toLong() * 60) }
                    it[purpose] = request.purpose
                }[VirtualCards.id].value

                val card = VirtualCards.select { VirtualCards.id eq cardId }.single()
                Result.success(mapToVirtualCardDto(card))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getVirtualCards(customerId: String): Result<List<VirtualCardDto>> {
        return try {
            transaction {
                val cards = VirtualCards.select { VirtualCards.customerId eq UUID.fromString(customerId) }
                    .orderBy(VirtualCards.createdAt to SortOrder.DESC)
                    .map { mapToVirtualCardDto(it) }
                Result.success(cards)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun freezeCard(cardId: String): Result<VirtualCardDto> {
        return try {
            transaction {
                VirtualCards.update({ VirtualCards.id eq UUID.fromString(cardId) }) {
                    it[VirtualCards.status] = "FROZEN"
                    it[VirtualCards.updatedAt] = Instant.now()
                }
                val card = VirtualCards.select { VirtualCards.id eq UUID.fromString(cardId) }.single()
                Result.success(mapToVirtualCardDto(card))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun unfreezeCard(cardId: String): Result<VirtualCardDto> {
        return try {
            transaction {
                VirtualCards.update({ VirtualCards.id eq UUID.fromString(cardId) }) {
                    it[VirtualCards.status] = "ACTIVE"
                    it[VirtualCards.updatedAt] = Instant.now()
                }
                val card = VirtualCards.select { VirtualCards.id eq UUID.fromString(cardId) }.single()
                Result.success(mapToVirtualCardDto(card))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun cancelCard(cardId: String): Result<VirtualCardDto> {
        return try {
            transaction {
                VirtualCards.update({ VirtualCards.id eq UUID.fromString(cardId) }) {
                    it[VirtualCards.status] = "CANCELLED"
                    it[VirtualCards.updatedAt] = Instant.now()
                }
                val card = VirtualCards.select { VirtualCards.id eq UUID.fromString(cardId) }.single()
                Result.success(mapToVirtualCardDto(card))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateCardNumber(): String {
        val bin = "4532" // Visa BIN
        val account = String.format("%011d", Random.nextLong(0, 100000000000))
        return bin + account
    }

    private fun encrypt(value: String): String = value // TODO: Implement encryption
    private fun decrypt(value: String): String = value // TODO: Implement decryption

    private fun mapToVirtualCardDto(row: ResultRow) = VirtualCardDto(
        id = row[VirtualCards.id].value.toString(),
        customerId = row[VirtualCards.customerId].toString(),
        accountId = row[VirtualCards.accountId].toString(),
        cardNumber = maskCardNumber(decrypt(row[VirtualCards.cardNumber])),
        cardHolderName = row[VirtualCards.cardHolderName],
        expiryMonth = row[VirtualCards.expiryMonth],
        expiryYear = row[VirtualCards.expiryYear],
        cvv = "***",
        cardType = row[VirtualCards.cardType],
        spendingLimit = row[VirtualCards.spendingLimit]?.toDouble(),
        usedAmount = row[VirtualCards.usedAmount].toDouble(),
        merchantRestrictions = row[VirtualCards.merchantRestrictions]?.split(","),
        categoryRestrictions = row[VirtualCards.categoryRestrictions]?.split(","),
        expiresAt = row[VirtualCards.expiresAt]?.toString(),
        status = row[VirtualCards.status],
        lastUsedAt = row[VirtualCards.lastUsedAt]?.toString(),
        usageCount = row[VirtualCards.usageCount],
        purpose = row[VirtualCards.purpose],
        createdAt = row[VirtualCards.createdAt].toString()
    )

    private fun maskCardNumber(cardNumber: String): String {
        if (cardNumber.length < 4) return cardNumber
        return "**** **** **** ${cardNumber.takeLast(4)}"
    }
}
