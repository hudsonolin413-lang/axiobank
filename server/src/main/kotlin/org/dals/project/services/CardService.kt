package org.dals.project.services

import org.dals.project.database.*
import org.dals.project.models.CardDto
import org.dals.project.models.AddCardRequest
import org.dals.project.utils.IdGenerator
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

class CardService {

    /**
     * Detect card brand from card number
     */
    private fun detectCardBrand(cardNumber: String): CardBrand {
        val cleanNumber = cardNumber.replace(Regex("[^0-9]"), "")
        
        return when {
            cleanNumber.startsWith("4") -> CardBrand.VISA
            cleanNumber.startsWith("5") && cleanNumber[1] in '1'..'5' -> CardBrand.MASTERCARD
            cleanNumber.startsWith("34") || cleanNumber.startsWith("37") -> CardBrand.AMERICAN_EXPRESS
            cleanNumber.startsWith("6011") || cleanNumber.startsWith("65") -> CardBrand.DISCOVER
            else -> CardBrand.UNKNOWN
        }
    }

    /**
     * Validate card number using Luhn algorithm
     */
    private fun validateCardNumber(cardNumber: String): Boolean {
        val cleanNumber = cardNumber.replace(Regex("[^0-9]"), "")
        
        if (cleanNumber.length < 13 || cleanNumber.length > 19) {
            return false
        }

        var sum = 0
        var alternate = false

        for (i in cleanNumber.length - 1 downTo 0) {
            var digit = cleanNumber[i].toString().toInt()
            
            if (alternate) {
                digit *= 2
                if (digit > 9) {
                    digit -= 9
                }
            }
            
            sum += digit
            alternate = !alternate
        }

        return sum % 10 == 0
    }

    /**
     * Validate expiry date
     */
    private fun validateExpiryDate(month: Int, year: Int): Boolean {
        if (month < 1 || month > 12) return false
        
        val now = LocalDateTime.now()
        val currentYear = now.year
        val currentMonth = now.monthValue

        return when {
            year < currentYear -> false
            year == currentYear && month < currentMonth -> false
            year > currentYear + 20 -> false // Card expires more than 20 years in future
            else -> true
        }
    }

    /**
     * Get all cards for a user
     */
    fun getCardsByUserId(userId: String): List<CardDto> {
        println("🔍 CardService: Getting cards for userId: $userId")
        return transaction {
            val userUuid = try {
                UUID.fromString(userId)
            } catch (e: Exception) {
                println("❌ CardService: Invalid UUID format: $userId")
                throw IllegalArgumentException("Invalid user ID format")
            }
            
            val cards = Cards.select { Cards.customerId eq userUuid }
                .orderBy(Cards.addedDate to SortOrder.DESC)
                .map { cardRow ->
                    println("📝 CardService: Mapping card - ID: ${cardRow[Cards.id]}, Last4: ${cardRow[Cards.lastFourDigits]}")

                    // Fetch linked account info separately if linkedAccountId exists
                    val linkedAccountId = cardRow[Cards.linkedAccountId]
                    var linkedAccountNumber: String? = null
                    var linkedAccountBalance: Double? = null

                    if (linkedAccountId != null) {
                        val accountRow = Accounts.select { Accounts.id eq linkedAccountId }.singleOrNull()
                        if (accountRow != null) {
                            linkedAccountNumber = accountRow[Accounts.accountNumber]
                            linkedAccountBalance = accountRow[Accounts.balance].toDouble()
                        }
                    }

                    CardDto(
                        id = cardRow[Cards.id].value.toString(),
                        userId = cardRow[Cards.customerId].toString(),
                        linkedAccountId = linkedAccountId?.toString(),
                        linkedAccountNumber = linkedAccountNumber,
                        linkedAccountBalance = linkedAccountBalance,
                        cardHolderName = cardRow[Cards.cardHolderName],
                        cardType = cardRow[Cards.cardType].name,
                        cardBrand = cardRow[Cards.cardBrand].name,
                        lastFourDigits = cardRow[Cards.lastFourDigits],
                        expiryMonth = cardRow[Cards.expiryMonth],
                        expiryYear = cardRow[Cards.expiryYear],
                        isDefault = cardRow[Cards.isDefault],
                        isActive = cardRow[Cards.isActive],
                        addedDate = cardRow[Cards.addedDate].toString(),
                        nickname = cardRow[Cards.nickname]
                    )
                }
            println("✅ CardService: Returning ${cards.size} cards")
            cards
        }
    }

    /**
     * Get a card by ID
     */
    fun getCardById(cardId: String): CardDto? {
        return transaction {
            Cards.select { Cards.id eq UUID.fromString(cardId) }
                .mapNotNull { resultRow ->
                    CardDto(
                        id = resultRow[Cards.id].value.toString(),
                        userId = resultRow[Cards.customerId].toString(),
                        cardHolderName = resultRow[Cards.cardHolderName],
                        cardType = resultRow[Cards.cardType].name,
                        cardBrand = resultRow[Cards.cardBrand].name,
                        lastFourDigits = resultRow[Cards.lastFourDigits],
                        expiryMonth = resultRow[Cards.expiryMonth],
                        expiryYear = resultRow[Cards.expiryYear],
                        isDefault = resultRow[Cards.isDefault],
                        isActive = resultRow[Cards.isActive],
                        addedDate = resultRow[Cards.addedDate].toString(),
                        nickname = resultRow[Cards.nickname]
                    )
                }
                .singleOrNull()
        }
    }

    /**
     * Add a new card
     */
    fun addCard(request: AddCardRequest): CardDto {
        val cleanCardNumber = request.cardNumber.replace(Regex("[^0-9]"), "")

        // Validate card number
        if (!validateCardNumber(cleanCardNumber)) {
            throw IllegalArgumentException("Invalid card number")
        }

        // Validate expiry date
        if (!validateExpiryDate(request.expiryMonth, request.expiryYear)) {
            throw IllegalArgumentException("Invalid or expired card")
        }

        // Detect card brand
        val cardBrand = detectCardBrand(cleanCardNumber)

        // Validate CVV
        val expectedCvvLength = if (cardBrand == CardBrand.AMERICAN_EXPRESS) 4 else 3
        if (request.cvv.length != expectedCvvLength) {
            throw IllegalArgumentException("Invalid CVV")
        }

        // Parse card type
        val cardType = try {
            CardType.valueOf(request.cardType.uppercase())
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid card type: ${request.cardType}")
        }

        return transaction {
            val userId = UUID.fromString(request.userId)
            
            // Check if user already has a card with the same last 4 digits
            val existingCard = Cards.select {
                (Cards.customerId eq userId) and
                (Cards.lastFourDigits eq cleanCardNumber.takeLast(4)) and
                (Cards.isActive eq true)
            }.singleOrNull()

            if (existingCard != null) {
                throw IllegalArgumentException("A card with these details is already added")
            }

            // Hash card number and CVV for security
            val cardNumberHash = BCrypt.hashpw(cleanCardNumber, BCrypt.gensalt())
            val cvvHash = BCrypt.hashpw(request.cvv, BCrypt.gensalt())

            // Check if this should be the default card (first card for user)
            val existingCardsCount = Cards.select { Cards.customerId eq userId }.count()
            val isDefault = existingCardsCount == 0L

            // If setting as default, unset other default cards
            if (isDefault) {
                Cards.update({ (Cards.customerId eq userId) and (Cards.isDefault eq true) }) {
                    it[Cards.isDefault] = false
                }
            }

            val cardId = UUID.randomUUID()

            Cards.insert {
                it[Cards.id] = cardId
                it[Cards.customerId] = userId
                it[Cards.cardHolderName] = request.cardHolderName
                it[Cards.cardType] = cardType
                it[Cards.cardBrand] = cardBrand
                it[Cards.cardNumberHash] = cardNumberHash
                it[Cards.lastFourDigits] = cleanCardNumber.takeLast(4)
                it[Cards.expiryMonth] = request.expiryMonth
                it[Cards.expiryYear] = request.expiryYear
                it[Cards.cvvHash] = cvvHash
                it[Cards.isDefault] = isDefault
                it[Cards.isActive] = true
                it[Cards.status] = CardStatus.ACTIVE // Auto-activate for now
                it[Cards.nickname] = request.nickname
                it[Cards.addedDate] = Instant.now()
                it[Cards.createdAt] = Instant.now()
                it[Cards.updatedAt] = Instant.now()
            }

            CardDto(
                id = cardId.toString(),
                userId = request.userId,
                cardHolderName = request.cardHolderName,
                cardType = cardType.name,
                cardBrand = cardBrand.name,
                lastFourDigits = cleanCardNumber.takeLast(4),
                expiryMonth = request.expiryMonth,
                expiryYear = request.expiryYear,
                isDefault = isDefault,
                isActive = true,
                addedDate = Instant.now().toString(),
                nickname = request.nickname
            )
        }
    }

    /**
     * Verify a card
     */
    fun verifyCard(cardId: String, verificationCode: String) {
        transaction {
            val card = Cards.select { Cards.id eq UUID.fromString(cardId) }
                .singleOrNull()
                ?: throw IllegalArgumentException("Card not found")

            // In a real implementation, verify the code against a stored verification code
            // For now, accept any 6-digit code
            if (verificationCode.length != 6 || !verificationCode.all { it.isDigit() }) {
                throw IllegalArgumentException("Invalid verification code")
            }

            Cards.update({ Cards.id eq UUID.fromString(cardId) }) {
                it[status] = CardStatus.ACTIVE
                it[verifiedDate] = Instant.now()
                it[updatedAt] = Instant.now()
            }
        }
    }

    /**
     * Set a card as default
     */
    fun setDefaultCard(cardId: String) {
        transaction {
            val card = Cards.select { Cards.id eq UUID.fromString(cardId) }
                .singleOrNull()
                ?: throw IllegalArgumentException("Card not found")

            val userId = card[Cards.customerId]

            // Unset all default cards for the user
            Cards.update({ (Cards.customerId eq userId) and (Cards.isDefault eq true) }) {
                it[Cards.isDefault] = false
            }

            // Set the specified card as default
            Cards.update({ Cards.id eq UUID.fromString(cardId) }) {
                it[isDefault] = true
                it[updatedAt] = Instant.now()
            }
        }
    }

    /**
     * Delete a card
     */
    fun deleteCard(cardId: String) {
        transaction {
            val card = Cards.select { Cards.id eq UUID.fromString(cardId) }
                .singleOrNull()
                ?: throw IllegalArgumentException("Card not found")

            // Soft delete: mark as inactive
            Cards.update({ Cards.id eq UUID.fromString(cardId) }) {
                it[isActive] = false
                it[updatedAt] = Instant.now()
            }

            // If it was the default card, set another card as default
            if (card[Cards.isDefault]) {
                val userId = card[Cards.customerId]
                val nextCard = Cards.select {
                    (Cards.customerId eq userId) and
                    (Cards.isActive eq true) and
                    (Cards.id neq UUID.fromString(cardId))
                }
                    .orderBy(Cards.addedDate to SortOrder.DESC)
                    .limit(1)
                    .singleOrNull()

                nextCard?.let {
                    Cards.update({ Cards.id eq it[Cards.id] }) {
                        it[isDefault] = true
                    }
                }
            }
        }
    }

    /**
     * Process a card payment
     */
    fun processCardPayment(request: org.dals.project.models.CardPaymentRequest): String {
        return transaction {
            val card = Cards.select { Cards.id eq UUID.fromString(request.cardId) }
                .singleOrNull()
                ?: throw IllegalArgumentException("Card not found")

            if (!card[Cards.isActive]) {
                throw IllegalArgumentException("Card is not active")
            }

            if (card[Cards.status] != CardStatus.ACTIVE) {
                throw IllegalArgumentException("Card is not verified or is blocked")
            }

            // Validate CVV
            val cvvHash = card[Cards.cvvHash]
            if (cvvHash != null && !BCrypt.checkpw(request.cvv, cvvHash)) {
                throw IllegalArgumentException("Invalid CVV")
            }

            // Check if card is expired
            if (!validateExpiryDate(card[Cards.expiryMonth], card[Cards.expiryYear])) {
                throw IllegalArgumentException("Card has expired")
            }

            // In a real implementation, integrate with payment gateway (Stripe, PayPal, etc.)
            // For now, simulate successful payment

            // Update last used date
            Cards.update({ Cards.id eq UUID.fromString(request.cardId) }) {
                it[lastUsedDate] = Instant.now()
                it[updatedAt] = Instant.now()
            }

            // Generate transaction ID
            "TXN-${IdGenerator.generateTransactionId()}"
        }
    }

    /**
     * Save issued Mastercard to database
     */
    fun saveIssuedCard(cardDto: CardDto) {
        transaction {
            val userId = UUID.fromString(cardDto.userId)
            val cardId = UUID.fromString(cardDto.id)

            // Check if card already exists
            val existingCard = Cards.select { Cards.id eq cardId }.singleOrNull()

            if (existingCard != null) {
                println("⚠️ CardService: Card $cardId already exists, updating...")
                // Update existing card
                Cards.update({ Cards.id eq cardId }) {
                    it[Cards.cardHolderName] = cardDto.cardHolderName
                    it[Cards.cardType] = CardType.valueOf(cardDto.cardType)
                    it[Cards.cardBrand] = CardBrand.valueOf(cardDto.cardBrand)
                    it[Cards.lastFourDigits] = cardDto.lastFourDigits
                    it[Cards.expiryMonth] = cardDto.expiryMonth
                    it[Cards.expiryYear] = cardDto.expiryYear
                    it[Cards.isDefault] = cardDto.isDefault
                    it[Cards.isActive] = cardDto.isActive
                    it[Cards.status] = CardStatus.PENDING_VERIFICATION
                    it[Cards.nickname] = cardDto.nickname
                    cardDto.linkedAccountId?.let { linkedId ->
                        it[Cards.linkedAccountId] = UUID.fromString(linkedId)
                    }
                    it[Cards.updatedAt] = Instant.now()
                }
            } else {
                println("✅ CardService: Creating new card $cardId")
                // Insert new card
                Cards.insert {
                    it[Cards.id] = cardId
                    it[Cards.customerId] = userId
                    cardDto.linkedAccountId?.let { linkedId ->
                        it[Cards.linkedAccountId] = UUID.fromString(linkedId)
                    }
                    it[Cards.cardHolderName] = cardDto.cardHolderName
                    it[Cards.cardType] = CardType.valueOf(cardDto.cardType)
                    it[Cards.cardBrand] = CardBrand.valueOf(cardDto.cardBrand)
                    it[Cards.cardNumberHash] = "" // Not storing for issued cards
                    it[Cards.lastFourDigits] = cardDto.lastFourDigits
                    it[Cards.expiryMonth] = cardDto.expiryMonth
                    it[Cards.expiryYear] = cardDto.expiryYear
                    it[Cards.cvvHash] = "" // Not storing CVV for issued cards
                    it[Cards.isDefault] = cardDto.isDefault
                    it[Cards.isActive] = cardDto.isActive
                    it[Cards.status] = CardStatus.PENDING_VERIFICATION // Requires activation
                    it[Cards.nickname] = cardDto.nickname
                    it[Cards.addedDate] = Instant.now()
                    it[Cards.createdAt] = Instant.now()
                    it[Cards.updatedAt] = Instant.now()
                }
            }
        }
    }

    /**
     * Activate a card (after PIN setup)
     */
    fun activateCard(cardId: String) {
        transaction {
            val card = Cards.select { Cards.id eq UUID.fromString(cardId) }
                .singleOrNull()
                ?: throw IllegalArgumentException("Card not found")

            println("🔓 CardService: Activating card $cardId")

            Cards.update({ Cards.id eq UUID.fromString(cardId) }) {
                it[isActive] = true
                it[status] = CardStatus.ACTIVE
                it[verifiedDate] = Instant.now()
                it[updatedAt] = Instant.now()
            }
        }
    }

    /**
     * Suspend a card
     */
    fun suspendCard(cardId: String) {
        transaction {
            val card = Cards.select { Cards.id eq UUID.fromString(cardId) }
                .singleOrNull()
                ?: throw IllegalArgumentException("Card not found")

            println("🚫 CardService: Suspending card $cardId")

            Cards.update({ Cards.id eq UUID.fromString(cardId) }) {
                it[isActive] = false
                it[status] = CardStatus.BLOCKED
                it[updatedAt] = Instant.now()
            }
        }
    }
}
