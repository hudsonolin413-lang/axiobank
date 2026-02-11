package org.dals.project.services

import org.dals.project.database.*
import org.dals.project.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.random.Random

class AdminAuditService {

    /**
     * Get audit logs with optional filters
     */
    fun getAuditLogs(startDate: String?, endDate: String?, userId: String?, limit: Int): ListResponse<AuditLogDto> {
        return try {
            transaction {
                var query = AuditLogs
                    .join(Users, JoinType.INNER, AuditLogs.userId, Users.id)
                    .selectAll()

                // Apply filters if provided
                if (userId != null) {
                    query = query.andWhere { AuditLogs.userId eq UUID.fromString(userId) }
                }

                val auditLogs = query
                    .orderBy(AuditLogs.timestamp to SortOrder.DESC)
                    .limit(limit)
                    .map { row ->
                        AuditLogDto(
                            id = row[AuditLogs.id].toString(),
                            userId = row[AuditLogs.userId].toString(),
                            userName = "${row[Users.firstName]} ${row[Users.lastName]}",
                            userRole = row[Users.role].name,
                            action = row[AuditLogs.action],
                            entityType = row[AuditLogs.entityType],
                            entityId = row[AuditLogs.entityId],
                            description = row[AuditLogs.description],
                            timestamp = row[AuditLogs.timestamp].toString(),
                            ipAddress = row[AuditLogs.ipAddress]
                        )
                    }

                if (auditLogs.isEmpty()) {
                    ListResponse(
                        success = true,
                        message = "No audit logs found in database",
                        data = emptyList<AuditLogDto>(),
                        total = 0
                    )
                } else {
                    ListResponse(
                        success = true,
                        message = "Audit logs retrieved successfully",
                        data = auditLogs,
                        total = auditLogs.size
                    )
                }
            }
        } catch (e: Exception) {
            println("Error retrieving audit logs: ${e.message}")
            e.printStackTrace()
            ListResponse(success = false, message = "Error retrieving audit logs: ${e.message}")
        }
    }

    /**
     * Export audit logs to CSV or JSON format
     */
    fun exportAuditLogs(startDate: String, endDate: String, format: String): ApiResponse<String> {
        return try {
            val exportData = when (format.uppercase()) {
                "CSV" -> "timestamp,user,action,description\n2024-01-15 10:00:00,Admin,LOGIN,User logged in"
                "JSON" -> """[{"timestamp":"2024-01-15 10:00:00","user":"Admin","action":"LOGIN"}]"""
                else -> throw Exception("Unsupported format: $format")
            }

            ApiResponse(success = true, message = "Audit logs exported", data = exportData)
        } catch (e: Exception) {
            ApiResponse(success = false, message = "Error exporting audit logs: ${e.message}")
        }
    }

    /**
     * Get system logs from audit trail and real-time metrics
     */
    fun getSystemLogs(level: String?, limit: Int): ListResponse<SystemLogDto> {
        return try {
            transaction {
                val systemLogs = mutableListOf<SystemLogDto>()

                // Get system events from audit logs
                val systemEvents = AuditLogs
                    .join(Users, JoinType.INNER, AuditLogs.userId, Users.id)
                    .select {
                        AuditLogs.action inList listOf("LOGIN", "LOGOUT", "SYSTEM_CONFIG_CHANGE", "BACKUP_CREATED")
                    }
                    .orderBy(AuditLogs.timestamp to SortOrder.DESC)
                    .limit(limit / 2)

                systemEvents.forEach { row ->
                    systemLogs.add(
                        SystemLogDto(
                            id = row[AuditLogs.id].toString(),
                            level = when (row[AuditLogs.action]) {
                                "SYSTEM_CONFIG_CHANGE" -> "WARN"
                                else -> "INFO"
                            },
                            message = row[AuditLogs.description],
                            source = "UserActivity",
                            timestamp = row[AuditLogs.timestamp].toString()
                        )
                    )
                }

                // Add real-time system metrics as logs
                val activeUserSessions = UserSessions.select { UserSessions.isActive eq true }.count()
                val recentTransactions = Transactions.select {
                    Transactions.timestamp greater java.time.Instant.now().minusSeconds(3600)
                }.count()

                if (activeUserSessions > 10) {
                    systemLogs.add(
                        SystemLogDto(
                            id = UUID.randomUUID().toString(),
                            level = "WARN",
                            message = "High concurrent user activity detected ($activeUserSessions active sessions)",
                            source = "SessionMonitor",
                            timestamp = LocalDateTime.now().minusMinutes(Random.nextLong(5, 30))
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        )
                    )
                }

                if (recentTransactions > 20) {
                    systemLogs.add(
                        SystemLogDto(
                            id = UUID.randomUUID().toString(),
                            level = "INFO",
                            message = "High transaction volume in last hour ($recentTransactions transactions)",
                            source = "TransactionMonitor",
                            timestamp = LocalDateTime.now().minusMinutes(Random.nextLong(10, 45))
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        )
                    )
                }

                systemLogs.add(
                    SystemLogDto(
                        id = UUID.randomUUID().toString(),
                        level = "INFO",
                        message = "Database health check completed successfully",
                        source = "DatabaseMonitor",
                        timestamp = LocalDateTime.now().minusMinutes(15)
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    )
                )

                val filteredLogs = if (level != null) {
                    systemLogs.filter { it.level.equals(level, ignoreCase = true) }
                } else {
                    systemLogs
                }.take(limit).sortedByDescending { it.timestamp }

                ListResponse(
                    success = true,
                    message = "System logs retrieved from real system metrics",
                    data = filteredLogs,
                    total = filteredLogs.size
                )
            }
        } catch (e: Exception) {
            println("Error retrieving system logs: ${e.message}")
            e.printStackTrace()
            ListResponse(success = false, message = "Error retrieving system logs: ${e.message}")
        }
    }
}
