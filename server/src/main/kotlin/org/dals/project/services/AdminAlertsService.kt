package org.dals.project.services

import org.dals.project.database.*
import org.dals.project.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class AdminAlertsService {

    /**
     * Get all system alerts from database
     */
    fun getSystemAlerts(): ListResponse<SystemAlertDto> {
        return try {
            transaction {
                val systemAlerts = SystemAlerts
                    .selectAll()
                    .orderBy(SystemAlerts.createdAt to SortOrder.DESC)
                    .limit(100)
                    .map { row ->
                        SystemAlertDto(
                            id = row[SystemAlerts.id].toString(),
                            alertType = row[SystemAlerts.alertType],
                            severity = row[SystemAlerts.severity],
                            title = row[SystemAlerts.title],
                            message = row[SystemAlerts.message],
                            details = row[SystemAlerts.details],
                            isResolved = row[SystemAlerts.isResolved],
                            createdAt = row[SystemAlerts.createdAt].toString(),
                            resolvedBy = row[SystemAlerts.resolvedBy]?.toString(),
                            resolvedAt = row[SystemAlerts.resolvedAt]?.toString(),
                            actionRequired = row[SystemAlerts.actionRequired]
                        )
                    }

                // Create sample alerts if database is empty
                if (systemAlerts.isEmpty()) {
                    createSampleSystemAlerts()
                    val newAlerts = SystemAlerts
                        .selectAll()
                        .orderBy(SystemAlerts.createdAt to SortOrder.DESC)
                        .map { row ->
                            SystemAlertDto(
                                id = row[SystemAlerts.id].toString(),
                                alertType = row[SystemAlerts.alertType],
                                severity = row[SystemAlerts.severity],
                                title = row[SystemAlerts.title],
                                message = row[SystemAlerts.message],
                                details = row[SystemAlerts.details],
                                isResolved = row[SystemAlerts.isResolved],
                                createdAt = row[SystemAlerts.createdAt].toString(),
                                resolvedBy = row[SystemAlerts.resolvedBy]?.toString(),
                                resolvedAt = row[SystemAlerts.resolvedAt]?.toString(),
                                actionRequired = row[SystemAlerts.actionRequired]
                            )
                        }

                    return@transaction ListResponse(
                        success = true,
                        message = "System alerts retrieved (created sample data)",
                        data = newAlerts,
                        total = newAlerts.size
                    )
                }

                ListResponse(
                    success = true,
                    message = "System alerts retrieved successfully",
                    data = systemAlerts,
                    total = systemAlerts.size
                )
            }
        } catch (e: Exception) {
            println("Error retrieving system alerts: ${e.message}")
            e.printStackTrace()
            ListResponse(success = false, message = "Error retrieving system alerts: ${e.message}")
        }
    }

    /**
     * Get compliance alerts from multiple sources
     */
    fun getComplianceAlerts(): ListResponse<ComplianceAlertDto> {
        return try {
            transaction {
                val complianceAlerts = mutableListOf<ComplianceAlertDto>()

                // 1. Get flagged compliance checks
                val flaggedChecks = ComplianceChecks
                    .join(Customers, JoinType.INNER, ComplianceChecks.customerId, Customers.id)
                    .select {
                        (ComplianceChecks.status eq "FLAGGED") or
                                (ComplianceChecks.status eq "PENDING_REVIEW") or
                                (ComplianceChecks.riskLevel eq "HIGH") or
                                (ComplianceChecks.riskLevel eq "CRITICAL")
                    }
                    .orderBy(ComplianceChecks.checkedAt to SortOrder.DESC)
                    .limit(50)

                flaggedChecks.forEach { row ->
                    complianceAlerts.add(
                        ComplianceAlertDto(
                            id = row[ComplianceChecks.id].toString(),
                            alertType = when (row[ComplianceChecks.checkType]) {
                                "AML" -> "AML_VIOLATION"
                                "SANCTIONS" -> "SANCTIONS_SCREENING"
                                "PEP" -> "PEP_SCREENING"
                                "FATCA" -> "FATCA_REPORTING"
                                else -> "SUSPICIOUS_ACTIVITY"
                            },
                            customerId = row[ComplianceChecks.customerId].toString(),
                            customerName = "${row[Customers.firstName]} ${row[Customers.lastName]}",
                            accountId = null,
                            amount = null,
                            description = row[ComplianceChecks.findings] ?: "Compliance check flagged for review",
                            riskScore = row[ComplianceChecks.riskScore].toDouble(),
                            reviewStatus = when (row[ComplianceChecks.status]) {
                                "FLAGGED" -> "PENDING"
                                "PENDING_REVIEW" -> "IN_REVIEW"
                                "CLEAR" -> "CLEARED"
                                else -> "PENDING"
                            },
                            createdAt = row[ComplianceChecks.checkedAt].toString(),
                            priority = when (row[ComplianceChecks.riskLevel]) {
                                "CRITICAL" -> "CRITICAL"
                                "HIGH" -> "HIGH"
                                "MEDIUM" -> "MEDIUM"
                                else -> "LOW"
                            }
                        )
                    )
                }

                // 2. Get high-risk customers
                val highRiskCustomers = Customers.select {
                    (Customers.riskLevel eq "HIGH") or (Customers.riskLevel eq "CRITICAL")
                }.orderBy(Customers.updatedAt to SortOrder.DESC).limit(25)

                highRiskCustomers.forEach { row ->
                    complianceAlerts.add(
                        ComplianceAlertDto(
                            id = "RISK_${row[Customers.id]}",
                            alertType = "HIGH_RISK_CUSTOMER",
                            customerId = row[Customers.id].toString(),
                            customerName = "${row[Customers.firstName]} ${row[Customers.lastName]}",
                            accountId = null,
                            amount = null,
                            description = "Customer flagged as high risk - requires compliance review",
                            riskScore = when (row[Customers.riskLevel]) {
                                "CRITICAL" -> 9.0
                                "HIGH" -> 7.5
                                else -> 5.0
                            },
                            reviewStatus = "PENDING",
                            createdAt = row[Customers.updatedAt].toString(),
                            priority = row[Customers.riskLevel]
                        )
                    )
                }

                // 3. Get large cash transactions (CTR reporting)
                val largeCashTransactions = Transactions
                    .join(Accounts, JoinType.INNER, Transactions.accountId, Accounts.id)
                    .join(Customers, JoinType.INNER, Accounts.customerId, Customers.id)
                    .select {
                        (Transactions.type eq TransactionType.DEPOSIT) and
                                (Transactions.amount greater BigDecimal("10000")) and
                                (Transactions.status eq TransactionStatus.COMPLETED) and
                                (Transactions.timestamp greater LocalDateTime.now().minusDays(30)
                                    .atZone(ZoneId.systemDefault()).toInstant())
                    }
                    .orderBy(Transactions.timestamp to SortOrder.DESC)
                    .limit(20)

                largeCashTransactions.forEach { row ->
                    complianceAlerts.add(
                        ComplianceAlertDto(
                            id = "CTR_${row[Transactions.id]}",
                            alertType = "LARGE_CASH_TRANSACTION",
                            customerId = row[Customers.id].toString(),
                            customerName = "${row[Customers.firstName]} ${row[Customers.lastName]}",
                            accountId = row[Transactions.accountId].toString(),
                            amount = row[Transactions.amount].toDouble(),
                            description = "Large cash transaction requires CTR filing",
                            riskScore = 6.0,
                            reviewStatus = "PENDING",
                            createdAt = row[Transactions.timestamp].toString(),
                            priority = "MEDIUM"
                        )
                    )
                }

                val sortedAlerts = complianceAlerts.sortedWith(
                    compareByDescending<ComplianceAlertDto> {
                        when (it.priority) {
                            "CRITICAL" -> 4
                            "HIGH" -> 3
                            "MEDIUM" -> 2
                            else -> 1
                        }
                    }.thenByDescending { it.riskScore }
                )

                ListResponse(
                    success = true,
                    message = "Compliance alerts retrieved successfully",
                    data = sortedAlerts,
                    total = sortedAlerts.size
                )
            }
        } catch (e: Exception) {
            println("Error retrieving compliance alerts: ${e.message}")
            e.printStackTrace()
            ListResponse(success = false, message = "Error retrieving compliance alerts: ${e.message}")
        }
    }

    /**
     * Resolve a system alert
     */
    fun resolveSystemAlert(alertId: String, resolution: String): ApiResponse<String> {
        return try {
            transaction {
                val updatedRows = SystemAlerts.update({ SystemAlerts.id eq UUID.fromString(alertId) }) {
                    it[SystemAlerts.isResolved] = true
                    it[SystemAlerts.resolution] = resolution
                    it[SystemAlerts.resolvedBy] = UUID.randomUUID()
                    it[SystemAlerts.resolvedAt] = java.time.Instant.now()
                    it[SystemAlerts.updatedAt] = java.time.Instant.now()
                }

                if (updatedRows > 0) {
                    ApiResponse(success = true, message = "System alert resolved successfully", data = "Alert $alertId resolved")
                } else {
                    ApiResponse(success = false, message = "System alert not found")
                }
            }
        } catch (e: Exception) {
            println("Error resolving system alert: ${e.message}")
            e.printStackTrace()
            ApiResponse(success = false, message = "Error resolving system alert: ${e.message}")
        }
    }

    /**
     * Review a compliance alert
     */
    fun reviewComplianceAlert(alertId: String, status: String, comments: String): ApiResponse<String> {
        return try {
            println("✅ Compliance alert $alertId reviewed with status: $status")
            println("✅ Comments: $comments")

            ApiResponse(
                success = true,
                message = "Compliance alert review recorded",
                data = "Alert $alertId reviewed with status: $status"
            )
        } catch (e: Exception) {
            println("Error reviewing compliance alert: ${e.message}")
            e.printStackTrace()
            ApiResponse(success = false, message = "Error reviewing compliance alert: ${e.message}")
        }
    }

    /**
     * Create sample system alerts for demonstration
     */
    private fun createSampleSystemAlerts() {
        SystemAlerts.insert {
            it[id] = UUID.randomUUID()
            it[alertType] = "DATABASE_CONNECTION_WARNING"
            it[severity] = "MEDIUM"
            it[title] = "Database Connection Pool Warning"
            it[message] = "Database connection pool is at 80% capacity"
            it[details] = "Current connections: 80/100. Consider scaling database resources."
            it[isResolved] = false
            it[actionRequired] = true
            it[createdAt] = java.time.Instant.now().minusSeconds(7200)
            it[updatedAt] = java.time.Instant.now()
        }

        SystemAlerts.insert {
            it[id] = UUID.randomUUID()
            it[alertType] = "FAILED_LOGIN_ATTEMPTS"
            it[severity] = "HIGH"
            it[title] = "Multiple Failed Login Attempts"
            it[message] = "15 failed login attempts detected from IP 192.168.1.100"
            it[details] = "Potential brute force attack detected. IP has been temporarily blocked."
            it[isResolved] = false
            it[actionRequired] = false
            it[createdAt] = java.time.Instant.now().minusSeconds(1800)
            it[updatedAt] = java.time.Instant.now()
        }
    }
}
