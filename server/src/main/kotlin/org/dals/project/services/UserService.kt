package org.dals.project.services

import org.dals.project.database.*
import org.dals.project.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import kotlin.random.Random
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class UserService {

    private fun parsePermissions(permissionsJson: String): List<String> {
        return try {
            Json.decodeFromString(permissionsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getDefaultPermissionsByRole(role: String): List<String> {
        return when (role) {
            "TELLER" -> listOf(
                "PROCESS_TRANSACTIONS",
                "VIEW_CUSTOMERS",
                "MANAGE_CASH_DRAWER"
            )
            "CUSTOMER_SERVICE_OFFICER" -> listOf(
                "VIEW_CUSTOMERS",
                "EDIT_CUSTOMERS",
                "CREATE_ACCOUNTS",
                "KYC_VERIFICATION"
            )
            "LOAN_OFFICER" -> listOf(
                "VIEW_CUSTOMERS",
                "APPROVE_LOANS",
                "VIEW_REPORTS"
            )
            "BRANCH_MANAGER" -> listOf(
                "VIEW_CUSTOMERS",
                "EDIT_CUSTOMERS",
                "CREATE_ACCOUNTS",
                "PROCESS_TRANSACTIONS",
                "APPROVE_LOANS",
                "VIEW_REPORTS",
                "MANAGE_CASH_DRAWER",
                "MANAGE_EMPLOYEES"
            )
            "SYSTEM_ADMIN" -> emptyList() // Admins have all permissions by default
            else -> emptyList()
        }
    }

    suspend fun getAllUsers(page: Int = 1, pageSize: Int = 10): ListResponse<UserDto> {
        return transaction {
            val total = Users.selectAll().count().toInt()
            val users = Users.selectAll()
                .orderBy(Users.lastName)
                .limit(pageSize, ((page - 1) * pageSize).toLong())
                .map { row ->
                    UserDto(
                        id = row[Users.id].toString(),
                        username = row[Users.username],
                        email = row[Users.email],
                        firstName = row[Users.firstName],
                        lastName = row[Users.lastName],
                        phoneNumber = row[Users.phoneNumber],
                        role = row[Users.role].name,
                        status = row[Users.status].name,
                        branchId = row[Users.branchId]?.toString(),
                        employeeId = row[Users.employeeId],
                        permissions = parsePermissions(row[Users.permissions]),
                        createdAt = row[Users.createdAt].toString(),
                        lastLoginAt = row[Users.lastLoginAt]?.toString()
                    )
                }

            ListResponse(
                success = true,
                message = "Users retrieved successfully",
                data = users,
                total = total,
                page = page,
                pageSize = pageSize
            )
        }
    }

    suspend fun getAllEmployees(): ListResponse<UserDto> {
        return transaction {
            val employees = Users.select { Users.role neq UserRole.CUSTOMER }
                .orderBy(Users.lastName)
                .map { row ->
                    UserDto(
                        id = row[Users.id].toString(),
                        username = row[Users.username],
                        email = row[Users.email],
                        firstName = row[Users.firstName],
                        lastName = row[Users.lastName],
                        phoneNumber = row[Users.phoneNumber],
                        role = row[Users.role].name,
                        status = row[Users.status].name,
                        branchId = row[Users.branchId]?.toString(),
                        employeeId = row[Users.employeeId],
                        permissions = parsePermissions(row[Users.permissions]),
                        createdAt = row[Users.createdAt].toString(),
                        lastLoginAt = row[Users.lastLoginAt]?.toString()
                    )
                }

            ListResponse(
                success = true,
                message = "Employees retrieved successfully",
                data = employees,
                total = employees.size,
                page = 1,
                pageSize = employees.size
            )
        }
    }

    suspend fun getUserById(id: UUID): ApiResponse<UserDto> {
        return transaction {
            val user = Users.select { Users.id eq id }.singleOrNull()

            if (user == null) {
                ApiResponse(
                    success = false,
                    message = "User not found"
                )
            } else {
                ApiResponse(
                    success = true,
                    message = "User retrieved successfully",
                    data = UserDto(
                        id = user[Users.id].toString(),
                        username = user[Users.username],
                        email = user[Users.email],
                        firstName = user[Users.firstName],
                        lastName = user[Users.lastName],
                        phoneNumber = user[Users.phoneNumber],
                        role = user[Users.role].name,
                        status = user[Users.status].name,
                        branchId = user[Users.branchId]?.toString(),
                        employeeId = user[Users.employeeId],
                        permissions = parsePermissions(user[Users.permissions]),
                        createdAt = user[Users.createdAt].toString(),
                        lastLoginAt = user[Users.lastLoginAt]?.toString()
                    )
                )
            }
        }
    }

    suspend fun createEmployee(request: CreateEmployeeRequest): ApiResponse<UserDto> {
        return transaction {
            try {
                // Check if username or email already exists
                val existingUser = Users.select {
                    (Users.username eq request.username) or (Users.email eq request.email)
                }.singleOrNull()

                if (existingUser != null) {
                    return@transaction ApiResponse<UserDto>(
                        success = false,
                        message = "User with this username or email already exists"
                    )
                }

                // Generate employee ID
                val employeeId = generateEmployeeId()

                // Generate salt and hash password
                val salt = generateSalt()
                val defaultPassword = "temp123"
                val passwordHash = hashPassword(defaultPassword, salt)

                val userId = UUID.randomUUID()

                // Get default permissions based on role
                val defaultPermissions = getDefaultPermissionsByRole(request.role)
                val permissionsJson = Json.encodeToString(defaultPermissions)

                Users.insert {
                    it[Users.id] = userId
                    it[Users.username] = request.username
                    it[Users.email] = request.email
                    it[Users.firstName] = request.firstName
                    it[Users.lastName] = request.lastName
                    it[Users.phoneNumber] = request.phoneNumber
                    it[Users.role] = UserRole.valueOf(request.role)
                    it[Users.status] = UserStatus.PENDING_APPROVAL
                    it[Users.branchId] = request.branchId?.let { UUID.fromString(it) }
                    it[Users.employeeId] = employeeId
                    it[Users.passwordHash] = passwordHash
                    it[Users.salt] = salt
                    it[Users.permissions] = permissionsJson
                }

                val newUser = UserDto(
                    id = userId.toString(),
                    username = request.username,
                    email = request.email,
                    firstName = request.firstName,
                    lastName = request.lastName,
                    phoneNumber = request.phoneNumber,
                    role = request.role,
                    status = UserStatus.PENDING_APPROVAL.name,
                    branchId = request.branchId,
                    employeeId = employeeId,
                    permissions = defaultPermissions,
                    createdAt = java.time.Instant.now().toString(),
                    lastLoginAt = null
                )

                ApiResponse(
                    success = true,
                    message = "Employee created successfully. Default password: $defaultPassword",
                    data = newUser
                )
            } catch (e: Exception) {
                ApiResponse<UserDto>(
                    success = false,
                    message = "Failed to create employee",
                    error = e.message
                )
            }
        }
    }

    suspend fun updateEmployeeRole(id: UUID, request: UpdateEmployeeRoleRequest): ApiResponse<UserDto> {
        return transaction {
            try {
                val existingUser = Users.select { Users.id eq id }.singleOrNull()

                if (existingUser == null) {
                    return@transaction ApiResponse<UserDto>(
                        success = false,
                        message = "Employee not found"
                    )
                }

                Users.update({ Users.id eq id }) {
                    it[Users.role] = UserRole.valueOf(request.role)
                }

                val updatedUser = Users.select { Users.id eq id }.single()

                ApiResponse(
                    success = true,
                    message = "Employee role updated successfully",
                    data = UserDto(
                        id = updatedUser[Users.id].toString(),
                        username = updatedUser[Users.username],
                        email = updatedUser[Users.email],
                        firstName = updatedUser[Users.firstName],
                        lastName = updatedUser[Users.lastName],
                        phoneNumber = updatedUser[Users.phoneNumber],
                        role = updatedUser[Users.role].name,
                        status = updatedUser[Users.status].name,
                        branchId = updatedUser[Users.branchId]?.toString(),
                        employeeId = updatedUser[Users.employeeId],
                        permissions = parsePermissions(updatedUser[Users.permissions]),
                        createdAt = updatedUser[Users.createdAt].toString(),
                        lastLoginAt = updatedUser[Users.lastLoginAt]?.toString()
                    )
                )
            } catch (e: Exception) {
                ApiResponse<UserDto>(
                    success = false,
                    message = "Failed to update employee role",
                    error = e.message
                )
            }
        }
    }

    suspend fun updateEmployeeStatus(id: UUID, status: String): ApiResponse<UserDto> {
        return transaction {
            try {
                val existingUser = Users.select { Users.id eq id }.singleOrNull()

                if (existingUser == null) {
                    return@transaction ApiResponse<UserDto>(
                        success = false,
                        message = "Employee not found"
                    )
                }

                Users.update({ Users.id eq id }) {
                    it[Users.status] = UserStatus.valueOf(status)
                }

                val updatedUser = Users.select { Users.id eq id }.single()

                ApiResponse(
                    success = true,
                    message = "Employee status updated successfully",
                    data = UserDto(
                        id = updatedUser[Users.id].toString(),
                        username = updatedUser[Users.username],
                        email = updatedUser[Users.email],
                        firstName = updatedUser[Users.firstName],
                        lastName = updatedUser[Users.lastName],
                        phoneNumber = updatedUser[Users.phoneNumber],
                        role = updatedUser[Users.role].name,
                        status = updatedUser[Users.status].name,
                        branchId = updatedUser[Users.branchId]?.toString(),
                        employeeId = updatedUser[Users.employeeId],
                        permissions = parsePermissions(updatedUser[Users.permissions]),
                        createdAt = updatedUser[Users.createdAt].toString(),
                        lastLoginAt = updatedUser[Users.lastLoginAt]?.toString()
                    )
                )
            } catch (e: Exception) {
                ApiResponse<UserDto>(
                    success = false,
                    message = "Failed to update employee status",
                    error = e.message
                )
            }
        }
    }

    suspend fun resetEmployeePassword(id: UUID): ApiResponse<String> {
        return transaction {
            try {
                val existingUser = Users.select { Users.id eq id }.singleOrNull()

                if (existingUser == null) {
                    return@transaction ApiResponse<String>(
                        success = false,
                        message = "Employee not found"
                    )
                }

                val newPassword = generateTemporaryPassword()
                val newSalt = generateSalt()
                val newPasswordHash = hashPassword(newPassword, newSalt)

                Users.update({ Users.id eq id }) {
                    it[Users.passwordHash] = newPasswordHash
                    it[Users.salt] = newSalt
                }

                ApiResponse(
                    success = true,
                    message = "Password reset successfully",
                    data = newPassword
                )
            } catch (e: Exception) {
                ApiResponse<String>(
                    success = false,
                    message = "Failed to reset password",
                    error = e.message
                )
            }
        }
    }

    suspend fun deleteEmployee(id: UUID): ApiResponse<String> {
        return transaction {
            try {
                val existingUser = Users.select { Users.id eq id }.singleOrNull()

                if (existingUser == null) {
                    return@transaction ApiResponse<String>(
                        success = false,
                        message = "Employee not found"
                    )
                }

                // Check if user is a customer (shouldn't be deleted through this endpoint)
                if (existingUser[Users.role] == UserRole.CUSTOMER) {
                    return@transaction ApiResponse<String>(
                        success = false,
                        message = "Cannot delete customer users through this endpoint"
                    )
                }

                // Instead of hard delete, we'll set status to INACTIVE
                Users.update({ Users.id eq id }) {
                    it[Users.status] = UserStatus.INACTIVE
                }

                ApiResponse(
                    success = true,
                    message = "Employee deactivated successfully"
                )
            } catch (e: Exception) {
                ApiResponse<String>(
                    success = false,
                    message = "Failed to delete employee",
                    error = e.message
                )
            }
        }
    }

    // Helper methods
    private fun generateEmployeeId(): String {
        return "EMP${Random.nextInt(1000, 9999)}"
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    private fun hashPassword(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val saltedPassword = password + salt
        val hashedBytes = md.digest(saltedPassword.toByteArray())
        return hashedBytes.joinToString("") { "%02x".format(it) }
    }

    private fun generateTemporaryPassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..8).map { chars.random() }.joinToString("")
    }

    suspend fun updateUserPermissions(userId: UUID, permissions: List<String>): ApiResponse<UserDto> {
        return transaction {
            try {
                println("🔍 updateUserPermissions called for userId: $userId")
                println("📋 Permissions to save: $permissions")

                val existingUser = Users.select { Users.id eq userId }.singleOrNull()

                if (existingUser == null) {
                    println("❌ User not found: $userId")
                    return@transaction ApiResponse<UserDto>(
                        success = false,
                        message = "User not found"
                    )
                }

                println("✅ User found: ${existingUser[Users.username]}")
                println("📝 Current permissions in DB: ${existingUser[Users.permissions]}")

                // Convert permissions list to JSON string
                val permissionsJson = Json.encodeToString(permissions)
                println("💾 Saving permissions JSON: $permissionsJson")

                // Update permissions
                val updateCount = Users.update({ Users.id eq userId }) {
                    it[Users.permissions] = permissionsJson
                }
                println("✅ Updated $updateCount row(s)")

                // Fetch updated user
                val updatedUser = Users.select { Users.id eq userId }.single()
                println("✅ Verification - permissions after update: ${updatedUser[Users.permissions]}")

                val parsedPermissions = parsePermissions(updatedUser[Users.permissions])
                println("✅ Parsed permissions: $parsedPermissions")

                ApiResponse(
                    success = true,
                    message = "Permissions updated successfully",
                    data = UserDto(
                        id = updatedUser[Users.id].toString(),
                        username = updatedUser[Users.username],
                        email = updatedUser[Users.email],
                        firstName = updatedUser[Users.firstName],
                        lastName = updatedUser[Users.lastName],
                        phoneNumber = updatedUser[Users.phoneNumber],
                        role = updatedUser[Users.role].name,
                        status = updatedUser[Users.status].name,
                        branchId = updatedUser[Users.branchId]?.toString(),
                        employeeId = updatedUser[Users.employeeId],
                        permissions = parsedPermissions,
                        createdAt = updatedUser[Users.createdAt].toString(),
                        lastLoginAt = updatedUser[Users.lastLoginAt]?.toString()
                    )
                )
            } catch (e: Exception) {
                println("❌ ERROR updating permissions: ${e.message}")
                e.printStackTrace()
                ApiResponse<UserDto>(
                    success = false,
                    message = "Failed to update permissions: ${e.message}",
                    error = e.message
                )
            }
        }
    }
}