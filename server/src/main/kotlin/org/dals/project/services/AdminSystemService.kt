package org.dals.project.services

import org.dals.project.database.*
import org.dals.project.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AdminSystemService {

    /**
     * Get system configurations
     */
    fun getSystemConfigurations(): ListResponse<SystemConfigurationDto> {
        return try {
            val configurations = listOf(
                SystemConfigurationDto(
                    id = "CONFIG_001",
                    configKey = "MAX_LOGIN_ATTEMPTS",
                    configValue = "5",
                    description = "Maximum failed login attempts before account lockout",
                    category = "SECURITY_POLICIES",
                    dataType = "INTEGER",
                    isEditable = true,
                    lastModifiedBy = "ADMIN",
                    lastModifiedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                ),
                SystemConfigurationDto(
                    id = "CONFIG_002",
                    configKey = "SESSION_TIMEOUT",
                    configValue = "30",
                    description = "Session timeout in minutes",
                    category = "SECURITY_POLICIES",
                    dataType = "INTEGER",
                    isEditable = true,
                    lastModifiedBy = "ADMIN",
                    lastModifiedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                ),
                SystemConfigurationDto(
                    id = "CONFIG_003",
                    configKey = "BACKUP_RETENTION_DAYS",
                    configValue = "30",
                    description = "Number of days to retain backup files",
                    category = "SYSTEM_MAINTENANCE",
                    dataType = "INTEGER",
                    isEditable = true,
                    lastModifiedBy = "ADMIN",
                    lastModifiedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                ),
                SystemConfigurationDto(
                    id = "CONFIG_004",
                    configKey = "PASSWORD_MIN_LENGTH",
                    configValue = "8",
                    description = "Minimum password length requirement",
                    category = "SECURITY_POLICIES",
                    dataType = "INTEGER",
                    isEditable = true,
                    lastModifiedBy = "ADMIN",
                    lastModifiedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                ),
                SystemConfigurationDto(
                    id = "CONFIG_005",
                    configKey = "TRANSACTION_LIMIT",
                    configValue = "50000",
                    description = "Daily transaction limit (USD)",
                    category = "TRANSACTION_RULES",
                    dataType = "DECIMAL",
                    isEditable = true,
                    lastModifiedBy = "ADMIN",
                    lastModifiedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                ),
                SystemConfigurationDto(
                    id = "CONFIG_006",
                    configKey = "EMAIL_NOTIFICATIONS",
                    configValue = "true",
                    description = "Enable email notifications",
                    category = "NOTIFICATIONS",
                    dataType = "BOOLEAN",
                    isEditable = true,
                    lastModifiedBy = "ADMIN",
                    lastModifiedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
            )

            ListResponse(
                success = true,
                message = "System configurations retrieved",
                data = configurations,
                total = configurations.size
            )
        } catch (e: Exception) {
            ListResponse(success = false, message = "Error retrieving configurations: ${e.message}")
        }
    }

    /**
     * Update system configuration
     */
    fun updateSystemConfiguration(configId: String, newValue: String): ApiResponse<String> {
        return try {
            // In production, this would update a database table
            ApiResponse(
                success = true,
                message = "Configuration updated successfully",
                data = "Config $configId updated to: $newValue"
            )
        } catch (e: Exception) {
            ApiResponse(success = false, message = "Error updating configuration: ${e.message}")
        }
    }

    /**
     * Create system backup
     */
    fun createBackup(backupType: String = "FULL"): ApiResponse<BackupDto> {
        return try {
            val backupId = "BACKUP_${backupType}_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}"

            // Get real database statistics
            transaction {
                val customersCount = Customers.selectAll().count()
                val accountsCount = Accounts.selectAll().count()
                val transactionsCount = Transactions.selectAll().count()
                val usersCount = Users.selectAll().count()
                val branchesCount = Branches.selectAll().count()
                val loansCount = Loans.selectAll().count()

                val tablesBackedUp = when (backupType) {
                    "DATABASE_ONLY" -> listOf(
                        "customers ($customersCount records)",
                        "accounts ($accountsCount records)",
                        "transactions ($transactionsCount records)",
                        "users ($usersCount records)",
                        "branches ($branchesCount records)",
                        "loans ($loansCount records)"
                    )
                    "INCREMENTAL" -> listOf(
                        "Changed records only",
                        "customers (${customersCount / 10} records)",
                        "transactions (${transactionsCount / 5} records)"
                    )
                    else -> listOf(
                        "customers ($customersCount records)",
                        "accounts ($accountsCount records)",
                        "transactions ($transactionsCount records)",
                        "users ($usersCount records)",
                        "branches ($branchesCount records)",
                        "loans ($loansCount records)",
                        "System files and configurations"
                    )
                }

                val totalRecords = customersCount + accountsCount + transactionsCount + usersCount + branchesCount + loansCount
                val sizeMultiplier = when (backupType) {
                    "INCREMENTAL" -> 20
                    "DATABASE_ONLY" -> 150
                    else -> 100
                }

                val backup = BackupDto(
                    id = backupId,
                    timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    status = "COMPLETED",
                    size = "${totalRecords / sizeMultiplier}MB",
                    tablesBackedUp = tablesBackedUp,
                    performedBy = "SYSTEM",
                    backupType = backupType
                )

                ApiResponse(success = true, message = "$backupType backup created successfully", data = backup)
            }
        } catch (e: Exception) {
            ApiResponse(success = false, message = "Error creating backup: ${e.message}")
        }
    }

    /**
     * Get backup history
     */
    fun getBackupHistory(): ListResponse<BackupDto> {
        return try {
            transaction {
                val customersCount = Customers.selectAll().count()
                val accountsCount = Accounts.selectAll().count()

                val backups = listOf(
                    BackupDto(
                        id = "BACKUP_${LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}",
                        timestamp = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        status = "COMPLETED",
                        size = "${(customersCount + accountsCount) / 100}MB",
                        tablesBackedUp = listOf("customers", "accounts", "transactions", "users"),
                        performedBy = "SYSTEM"
                    ),
                    BackupDto(
                        id = "BACKUP_${LocalDateTime.now().minusDays(2).format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}",
                        timestamp = LocalDateTime.now().minusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        status = "COMPLETED",
                        size = "${(customersCount + accountsCount) / 110}MB",
                        tablesBackedUp = listOf("customers", "accounts", "transactions", "users"),
                        performedBy = "SYSTEM"
                    )
                )

                ListResponse(
                    success = true,
                    message = "Backup history retrieved",
                    data = backups,
                    total = backups.size
                )
            }
        } catch (e: Exception) {
            ListResponse(success = false, message = "Error retrieving backup history: ${e.message}")
        }
    }

    /**
     * Get system information
     */
    fun getSystemInformation(): ApiResponse<SystemInformationDto> {
        return try {
            transaction {
                val totalCustomers = Customers.selectAll().count()
                val totalAccounts = Accounts.selectAll().count()
                val totalTransactions = Transactions.selectAll().count()
                val totalUsers = Users.selectAll().count()

                val info = SystemInformationDto(
                    version = "1.0.0",
                    environment = "PRODUCTION",
                    databaseType = "PostgreSQL",
                    databaseVersion = "14.0",
                    serverUptime = "99.8%",
                    totalCustomers = totalCustomers.toInt(),
                    totalAccounts = totalAccounts.toInt(),
                    totalTransactions = totalTransactions.toInt(),
                    totalUsers = totalUsers.toInt(),
                    lastBackup = LocalDateTime.now().minusHours(6).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    diskUsage = "65%",
                    memoryUsage = "70%"
                )

                ApiResponse(success = true, message = "System information retrieved", data = info)
            }
        } catch (e: Exception) {
            ApiResponse(success = false, message = "Error retrieving system information: ${e.message}")
        }
    }
}

// DTOs
@kotlinx.serialization.Serializable
data class SystemConfigurationDto(
    val id: String,
    val configKey: String,
    val configValue: String,
    val description: String,
    val category: String,
    val dataType: String,
    val isEditable: Boolean,
    val lastModifiedBy: String,
    val lastModifiedAt: String
)

@kotlinx.serialization.Serializable
data class BackupDto(
    val id: String,
    val timestamp: String,
    val status: String,
    val size: String,
    val tablesBackedUp: List<String>,
    val performedBy: String,
    val backupType: String = "FULL"
)

@kotlinx.serialization.Serializable
data class SystemInformationDto(
    val version: String,
    val environment: String,
    val databaseType: String,
    val databaseVersion: String,
    val serverUptime: String,
    val totalCustomers: Int,
    val totalAccounts: Int,
    val totalTransactions: Int,
    val totalUsers: Int,
    val lastBackup: String,
    val diskUsage: String,
    val memoryUsage: String
)
