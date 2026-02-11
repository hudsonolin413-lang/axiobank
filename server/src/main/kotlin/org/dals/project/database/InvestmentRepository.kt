package org.dals.project.database

import kotlinx.serialization.Serializable
import org.dals.project.api.InvestmentResponse
import org.dals.project.model.InvestmentType
import java.sql.Connection
import java.sql.ResultSet
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.random.Random

class InvestmentRepository(private val connection: Connection) {

    init {
        createInvestmentTable()
    }

    private fun createInvestmentTable() {
        val statement = connection.createStatement()
        statement.executeUpdate("""
            CREATE TABLE IF NOT EXISTS investments (
                id VARCHAR(255) PRIMARY KEY,
                user_id VARCHAR(255) NOT NULL,
                type VARCHAR(50) NOT NULL,
                name VARCHAR(255) NOT NULL,
                symbol VARCHAR(50),
                amount DECIMAL(15, 2) NOT NULL,
                current_value DECIMAL(15, 2) NOT NULL,
                purchase_date TIMESTAMP NOT NULL,
                last_updated TIMESTAMP NOT NULL,
                status VARCHAR(50) NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )
        """.trimIndent())
    }

    fun getInvestmentsByUserId(userId: String): List<InvestmentResponse> {
        val investments = mutableListOf<InvestmentResponse>()
        val statement = connection.prepareStatement(
            "SELECT * FROM investments WHERE user_id = ? AND status = 'ACTIVE' ORDER BY purchase_date DESC"
        )
        statement.setString(1, userId)

        val resultSet = statement.executeQuery()
        while (resultSet.next()) {
            investments.add(mapResultSetToInvestment(resultSet))
        }

        return investments
    }

    fun getInvestmentById(investmentId: String, userId: String): InvestmentResponse? {
        val statement = connection.prepareStatement(
            "SELECT * FROM investments WHERE id = ? AND user_id = ?"
        )
        statement.setString(1, investmentId)
        statement.setString(2, userId)

        val resultSet = statement.executeQuery()
        return if (resultSet.next()) {
            mapResultSetToInvestment(resultSet)
        } else {
            null
        }
    }

    fun createInvestment(
        userId: String,
        type: InvestmentType,
        name: String,
        symbol: String?,
        amount: Double
    ): InvestmentResponse {
        val id = UUID.randomUUID().toString()
        val now = LocalDateTime.now()
        val currentValue = amount // Initially, current value equals invested amount

        val statement = connection.prepareStatement("""
            INSERT INTO investments 
            (id, user_id, type, name, symbol, amount, current_value, purchase_date, last_updated, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent())

        statement.setString(1, id)
        statement.setString(2, userId)
        statement.setString(3, type.name)
        statement.setString(4, name)
        statement.setString(5, symbol)
        statement.setDouble(6, amount)
        statement.setDouble(7, currentValue)
        statement.setObject(8, now)
        statement.setObject(9, now)
        statement.setString(10, "ACTIVE")

        statement.executeUpdate()

        return InvestmentResponse(
            id = id,
            userId = userId,
            type = type,
            name = name,
            symbol = symbol,
            amount = amount,
            currentValue = currentValue,
            totalReturn = 0.0,
            returnPercentage = 0.0,
            purchaseDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            status = "ACTIVE"
        )
    }

    fun withdrawFromInvestment(investmentId: String, userId: String, amount: Double): Boolean {
        val investment = getInvestmentById(investmentId, userId) ?: return false

        if (investment.currentValue < amount) {
            return false
        }

        // If withdrawing entire investment, mark as closed
        val newValue = investment.currentValue - amount
        val newStatus = if (newValue <= 0) "CLOSED" else "ACTIVE"

        val statement = connection.prepareStatement("""
            UPDATE investments 
            SET current_value = ?, status = ?, last_updated = ?
            WHERE id = ? AND user_id = ?
        """.trimIndent())

        statement.setDouble(1, newValue.coerceAtLeast(0.0))
        statement.setString(2, newStatus)
        statement.setObject(3, LocalDateTime.now())
        statement.setString(4, investmentId)
        statement.setString(5, userId)

        return statement.executeUpdate() > 0
    }

    fun updateInvestmentValue(investmentId: String, newValue: Double): Boolean {
        val statement = connection.prepareStatement("""
            UPDATE investments 
            SET current_value = ?, last_updated = ?
            WHERE id = ?
        """.trimIndent())

        statement.setDouble(1, newValue)
        statement.setObject(2, LocalDateTime.now())
        statement.setString(3, investmentId)

        return statement.executeUpdate() > 0
    }

    fun deleteInvestment(investmentId: String, userId: String): Boolean {
        val statement = connection.prepareStatement(
            "DELETE FROM investments WHERE id = ? AND user_id = ?"
        )
        statement.setString(1, investmentId)
        statement.setString(2, userId)

        return statement.executeUpdate() > 0
    }

    // Simulate market fluctuations for all active investments
    fun simulateMarketChanges() {
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery(
            "SELECT id, amount, current_value FROM investments WHERE status = 'ACTIVE'"
        )

        val updates = mutableListOf<Pair<String, Double>>()

        while (resultSet.next()) {
            val id = resultSet.getString("id")
            val currentValue = resultSet.getDouble("current_value")

            // Simulate market change (-5% to +5%)
            val changePercent = Random.nextDouble(-0.05, 0.05)
            val newValue = currentValue * (1 + changePercent)

            updates.add(id to newValue)
        }

        // Apply updates
        updates.forEach { (id, newValue) ->
            updateInvestmentValue(id, newValue)
        }
    }

    private fun mapResultSetToInvestment(resultSet: ResultSet): InvestmentResponse {
        val amount = resultSet.getDouble("amount")
        val currentValue = resultSet.getDouble("current_value")
        val totalReturn = currentValue - amount
        val returnPercentage = if (amount > 0) (totalReturn / amount) * 100 else 0.0

        return InvestmentResponse(
            id = resultSet.getString("id"),
            userId = resultSet.getString("user_id"),
            type = InvestmentType.valueOf(resultSet.getString("type")),
            name = resultSet.getString("name"),
            symbol = resultSet.getString("symbol"),
            amount = amount,
            currentValue = currentValue,
            totalReturn = totalReturn,
            returnPercentage = returnPercentage,
            purchaseDate = resultSet.getTimestamp("purchase_date").toString(),
            status = resultSet.getString("status")
        )
    }
}
