package org.dals.project.database

import org.dals.project.model.LockPeriod
import org.dals.project.model.SavingsAccount
import org.dals.project.model.SavingsStatus
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

object SavingsAccountsTable : Table("savings_accounts") {
    val id = varchar("id", 255)
    val userId = varchar("user_id", 255)
    val accountName = varchar("account_name", 255)
    val lockPeriod = varchar("lock_period", 50)
    val interestRate = decimal("interest_rate", 10, 2)
    val amount = decimal("amount", 15, 2)
    val startDate = timestamp("start_date")
    val maturityDate = timestamp("maturity_date")
    val status = varchar("status", 50)
    val accruedInterest = decimal("accrued_interest", 15, 2).default(java.math.BigDecimal.ZERO)
    val projectedEarnings = decimal("projected_earnings", 15, 2).default(java.math.BigDecimal.ZERO)
    val earlyWithdrawalPenalty = decimal("early_withdrawal_penalty", 15, 2).default(java.math.BigDecimal.ZERO)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

class SavingsRepository {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun getSavingsAccountsByUserId(userId: String): List<SavingsAccount> = transaction {
        SavingsAccountsTable.select { SavingsAccountsTable.userId eq userId }
            .orderBy(SavingsAccountsTable.createdAt, SortOrder.DESC)
            .map { mapRowToSavingsAccount(it) }
    }

    fun getSavingsAccountById(savingsId: String, userId: String): SavingsAccount? = transaction {
        SavingsAccountsTable.select {
            (SavingsAccountsTable.id eq savingsId) and (SavingsAccountsTable.userId eq userId)
        }.firstOrNull()?.let { mapRowToSavingsAccount(it) }
    }

    fun createSavingsAccount(
        userId: String,
        accountName: String,
        lockPeriod: LockPeriod,
        amount: Double
    ): SavingsAccount = transaction {
        val id = UUID.randomUUID().toString()
        val now = LocalDateTime.now()
        val maturityDate = now.plusDays(lockPeriod.days.toLong())

        val interestRate = getInterestRateForPeriod(lockPeriod)
        val projectedEarnings = calculateProjectedEarnings(amount, interestRate, lockPeriod.days)
        val earlyWithdrawalPenalty = amount * 0.01

        SavingsAccountsTable.insert {
            it[SavingsAccountsTable.id] = id
            it[SavingsAccountsTable.userId] = userId
            it[SavingsAccountsTable.accountName] = accountName
            it[SavingsAccountsTable.lockPeriod] = lockPeriod.name
            it[SavingsAccountsTable.interestRate] = java.math.BigDecimal(interestRate)
            it[SavingsAccountsTable.amount] = java.math.BigDecimal(amount)
            it[SavingsAccountsTable.startDate] = now.atZone(ZoneId.systemDefault()).toInstant()
            it[SavingsAccountsTable.maturityDate] = maturityDate.atZone(ZoneId.systemDefault()).toInstant()
            it[SavingsAccountsTable.status] = SavingsStatus.ACTIVE.name
            it[SavingsAccountsTable.accruedInterest] = java.math.BigDecimal.ZERO
            it[SavingsAccountsTable.projectedEarnings] = java.math.BigDecimal(projectedEarnings)
            it[SavingsAccountsTable.earlyWithdrawalPenalty] = java.math.BigDecimal(earlyWithdrawalPenalty)
            it[SavingsAccountsTable.createdAt] = Instant.now()
        }

        SavingsAccount(
            id = id,
            userId = userId,
            accountName = accountName,
            lockPeriod = lockPeriod,
            interestRate = interestRate,
            amount = amount,
            startDate = now.format(dateFormatter),
            maturityDate = maturityDate.format(dateFormatter),
            status = SavingsStatus.ACTIVE,
            accruedInterest = 0.0,
            projectedEarnings = projectedEarnings,
            earlyWithdrawalPenalty = earlyWithdrawalPenalty
        )
    }

    fun withdrawSavings(savingsId: String, userId: String): Pair<Double, Boolean> = transaction {
        val savingsRow = SavingsAccountsTable.select {
            (SavingsAccountsTable.id eq savingsId) and (SavingsAccountsTable.userId eq userId)
        }.firstOrNull() ?: throw IllegalArgumentException("Savings account not found")

        val savings = mapRowToSavingsAccount(savingsRow)

        if (savings.status != SavingsStatus.ACTIVE) {
            throw IllegalStateException("Savings account is not active")
        }

        val now = LocalDateTime.now()
        val maturityDate = LocalDateTime.parse(savings.maturityDate, dateFormatter)
        val isMatured = now.isAfter(maturityDate) || now.isEqual(maturityDate)

        val accruedInterest = calculateAccruedInterest(savings, now)

        val totalAmount = if (isMatured) {
            savings.amount + accruedInterest
        } else {
            savings.amount + accruedInterest - savings.earlyWithdrawalPenalty
        }

        val newStatus = if (isMatured) SavingsStatus.MATURED else SavingsStatus.WITHDRAWN

        SavingsAccountsTable.update({ SavingsAccountsTable.id eq savingsId }) {
            it[status] = newStatus.name
            it[SavingsAccountsTable.accruedInterest] = java.math.BigDecimal(accruedInterest)
        }

        Pair(totalAmount, isMatured)
    }

    private fun calculateAccruedInterest(savings: SavingsAccount, currentDate: LocalDateTime): Double {
        val startDate = LocalDateTime.parse(savings.startDate, dateFormatter)
        val daysElapsed = ChronoUnit.DAYS.between(startDate, currentDate).toInt()

        val rate = savings.interestRate / 100.0
        val years = daysElapsed / 365.0
        return savings.amount * rate * years
    }

    private fun calculateProjectedEarnings(amount: Double, annualRate: Double, days: Int): Double {
        val rate = annualRate / 100.0
        val years = days / 365.0
        return amount * rate * years
    }

    private fun getInterestRateForPeriod(lockPeriod: LockPeriod): Double {
        return when (lockPeriod) {
            LockPeriod.ONE_MONTH -> 2.5
            LockPeriod.THREE_MONTHS -> 4.0
            LockPeriod.SIX_MONTHS -> 6.5
            LockPeriod.ONE_YEAR -> 9.0
            LockPeriod.TWO_YEARS -> 11.5
            LockPeriod.FIVE_YEARS -> 15.0
        }
    }

    private fun mapRowToSavingsAccount(row: ResultRow): SavingsAccount {
        return SavingsAccount(
            id = row[SavingsAccountsTable.id],
            userId = row[SavingsAccountsTable.userId],
            accountName = row[SavingsAccountsTable.accountName],
            lockPeriod = LockPeriod.valueOf(row[SavingsAccountsTable.lockPeriod]),
            interestRate = row[SavingsAccountsTable.interestRate].toDouble(),
            amount = row[SavingsAccountsTable.amount].toDouble(),
            startDate = LocalDateTime.ofInstant(row[SavingsAccountsTable.startDate], ZoneId.systemDefault()).format(dateFormatter),
            maturityDate = LocalDateTime.ofInstant(row[SavingsAccountsTable.maturityDate], ZoneId.systemDefault()).format(dateFormatter),
            status = SavingsStatus.valueOf(row[SavingsAccountsTable.status]),
            accruedInterest = row[SavingsAccountsTable.accruedInterest].toDouble(),
            projectedEarnings = row[SavingsAccountsTable.projectedEarnings].toDouble(),
            earlyWithdrawalPenalty = row[SavingsAccountsTable.earlyWithdrawalPenalty].toDouble()
        )
    }
}
