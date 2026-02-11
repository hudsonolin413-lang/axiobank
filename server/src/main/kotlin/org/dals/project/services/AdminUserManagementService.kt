package org.dals.project.services

import org.dals.project.database.*
import org.dals.project.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDate
import java.util.*

class AdminUserManagementService {

    /**
     * Get all users (employees) with detailed information
     */
    fun getAllUsers(page: Int = 1, pageSize: Int = 50, role: String? = null): ListResponse<UserManagementDto> {
        return try {
            transaction {
                var query = Users
                    .leftJoin(Branches, { Users.branchId }, { Branches.id })
                    .selectAll()

                // Filter by role if provided
                if (role != null) {
                    query = query.andWhere { Users.role eq UserRole.valueOf(role) }
                }

                val totalCount = query.count().toInt()
                val offset = ((page - 1) * pageSize).toLong()

                val users = query
                    .orderBy(Users.createdAt to SortOrder.DESC)
                    .limit(pageSize, offset)
                    .map { row ->
                        UserManagementDto(
                            id = row[Users.id].toString(),
                            username = row[Users.username],
                            email = row[Users.email],
                            firstName = row[Users.firstName],
                            lastName = row[Users.lastName],
                            phoneNumber = row[Users.phoneNumber],
                            role = row[Users.role].name,
                            status = row[Users.status].name,
                            branchId = row[Users.branchId]?.toString(),
                            branchName = row.getOrNull(Branches.name),
                            employeeId = row[Users.employeeId],
                            createdAt = row[Users.createdAt].toString(),
                            lastLoginAt = row[Users.lastLoginAt]?.toString(),
                            fullName = "${row[Users.firstName]} ${row[Users.lastName]}"
                        )
                    }

                ListResponse(
                    success = true,
                    message = "Users retrieved successfully",
                    data = users,
                    total = totalCount,
                    page = page,
                    pageSize = pageSize
                )
            }
        } catch (e: Exception) {
            println("Error retrieving users: ${e.message}")
            e.printStackTrace()
            ListResponse(success = false, message = "Error retrieving users: ${e.message}")
        }
    }

    /**
     * Get user by ID with detailed information
     */
    fun getUserById(userId: String): ApiResponse<UserManagementDto> {
        return try {
            transaction {
                val user = Users
                    .leftJoin(Branches, { Users.branchId }, { Branches.id })
                    .select { Users.id eq UUID.fromString(userId) }
                    .singleOrNull()

                if (user == null) {
                    ApiResponse(success = false, message = "User not found")
                } else {
                    val userDto = UserManagementDto(
                        id = user[Users.id].toString(),
                        username = user[Users.username],
                        email = user[Users.email],
                        firstName = user[Users.firstName],
                        lastName = user[Users.lastName],
                        phoneNumber = user[Users.phoneNumber],
                        role = user[Users.role].name,
                        status = user[Users.status].name,
                        branchId = user[Users.branchId]?.toString(),
                        branchName = user.getOrNull(Branches.name),
                        employeeId = user[Users.employeeId],
                        createdAt = user[Users.createdAt].toString(),
                        lastLoginAt = user[Users.lastLoginAt]?.toString(),
                        fullName = "${user[Users.firstName]} ${user[Users.lastName]}"
                    )

                    ApiResponse(success = true, message = "User retrieved successfully", data = userDto)
                }
            }
        } catch (e: Exception) {
            println("Error retrieving user: ${e.message}")
            e.printStackTrace()
            ApiResponse(success = false, message = "Error retrieving user: ${e.message}")
        }
    }

    /**
     * Create new user
     */
    fun createUser(request: CreateUserManagementRequest): ApiResponse<UserManagementDto> {
        return try {
            transaction {
                // Check if username or email already exists
                val existingUser = Users.select {
                    (Users.username eq request.username) or (Users.email eq request.email)
                }.singleOrNull()

                if (existingUser != null) {
                    return@transaction ApiResponse(
                        success = false,
                        message = "Username or email already exists"
                    )
                }

                // Hash password
                val userSalt = BCrypt.gensalt()
                val userPasswordHash = BCrypt.hashpw(request.password, userSalt)

                // Insert user
                val userId = Users.insertAndGetId {
                    it[username] = request.username
                    it[email] = request.email
                    it[firstName] = request.firstName
                    it[lastName] = request.lastName
                    it[phoneNumber] = request.phoneNumber
                    it[role] = UserRole.valueOf(request.role)
                    it[status] = UserStatus.valueOf(request.status ?: "ACTIVE")
                    it[branchId] = request.branchId?.let { bid -> UUID.fromString(bid) }
                    it[employeeId] = request.employeeId
                    it[passwordHash] = userPasswordHash
                    it[salt] = userSalt
                }

                // Retrieve created user
                val createdUser = Users
                    .leftJoin(Branches, { Users.branchId }, { Branches.id })
                    .select { Users.id eq userId }
                    .single()

                val userDto = UserManagementDto(
                    id = createdUser[Users.id].toString(),
                    username = createdUser[Users.username],
                    email = createdUser[Users.email],
                    firstName = createdUser[Users.firstName],
                    lastName = createdUser[Users.lastName],
                    phoneNumber = createdUser[Users.phoneNumber],
                    role = createdUser[Users.role].name,
                    status = createdUser[Users.status].name,
                    branchId = createdUser[Users.branchId]?.toString(),
                    branchName = createdUser.getOrNull(Branches.name),
                    employeeId = createdUser[Users.employeeId],
                    createdAt = createdUser[Users.createdAt].toString(),
                    lastLoginAt = createdUser[Users.lastLoginAt]?.toString(),
                    fullName = "${createdUser[Users.firstName]} ${createdUser[Users.lastName]}"
                )

                ApiResponse(success = true, message = "User created successfully", data = userDto)
            }
        } catch (e: Exception) {
            println("Error creating user: ${e.message}")
            e.printStackTrace()
            ApiResponse(success = false, message = "Error creating user: ${e.message}")
        }
    }

    /**
     * Update user information
     */
    fun updateUser(userId: String, request: UpdateUserManagementRequest): ApiResponse<UserManagementDto> {
        return try {
            transaction {
                val userUuid = UUID.fromString(userId)

                // Check if user exists
                val existingUser = Users.select { Users.id eq userUuid }.singleOrNull()
                if (existingUser == null) {
                    return@transaction ApiResponse(success = false, message = "User not found")
                }

                // Update user
                Users.update({ Users.id eq userUuid }) {
                    request.email?.let { email -> it[Users.email] = email }
                    request.firstName?.let { firstName -> it[Users.firstName] = firstName }
                    request.lastName?.let { lastName -> it[Users.lastName] = lastName }
                    request.phoneNumber?.let { phone -> it[Users.phoneNumber] = phone }
                    request.role?.let { role -> it[Users.role] = UserRole.valueOf(role) }
                    request.status?.let { status -> it[Users.status] = UserStatus.valueOf(status) }
                    request.branchId?.let { branchId -> it[Users.branchId] = UUID.fromString(branchId) }
                    request.employeeId?.let { empId -> it[Users.employeeId] = empId }
                    it[Users.updatedAt] = java.time.Instant.now()
                }

                // Update password if provided
                if (request.password != null) {
                    val newSalt = BCrypt.gensalt()
                    val newPasswordHash = BCrypt.hashpw(request.password, newSalt)
                    Users.update({ Users.id eq userUuid }) {
                        it[Users.passwordHash] = newPasswordHash
                        it[Users.salt] = newSalt
                    }
                }

                // Retrieve updated user
                val updatedUser = Users
                    .leftJoin(Branches, { Users.branchId }, { Branches.id })
                    .select { Users.id eq userUuid }
                    .single()

                val userDto = UserManagementDto(
                    id = updatedUser[Users.id].toString(),
                    username = updatedUser[Users.username],
                    email = updatedUser[Users.email],
                    firstName = updatedUser[Users.firstName],
                    lastName = updatedUser[Users.lastName],
                    phoneNumber = updatedUser[Users.phoneNumber],
                    role = updatedUser[Users.role].name,
                    status = updatedUser[Users.status].name,
                    branchId = updatedUser[Users.branchId]?.toString(),
                    branchName = updatedUser.getOrNull(Branches.name),
                    employeeId = updatedUser[Users.employeeId],
                    createdAt = updatedUser[Users.createdAt].toString(),
                    lastLoginAt = updatedUser[Users.lastLoginAt]?.toString(),
                    fullName = "${updatedUser[Users.firstName]} ${updatedUser[Users.lastName]}"
                )

                ApiResponse(success = true, message = "User updated successfully", data = userDto)
            }
        } catch (e: Exception) {
            println("Error updating user: ${e.message}")
            e.printStackTrace()
            ApiResponse(success = false, message = "Error updating user: ${e.message}")
        }
    }

    /**
     * Delete user (soft delete by setting status to INACTIVE)
     */
    fun deleteUser(userId: String): ApiResponse<String> {
        return try {
            transaction {
                val userUuid = UUID.fromString(userId)

                val updatedRows = Users.update({ Users.id eq userUuid }) {
                    it[status] = UserStatus.INACTIVE
                    it[updatedAt] = java.time.Instant.now()
                }

                if (updatedRows > 0) {
                    ApiResponse(success = true, message = "User deactivated successfully", data = userId)
                } else {
                    ApiResponse(success = false, message = "User not found")
                }
            }
        } catch (e: Exception) {
            println("Error deleting user: ${e.message}")
            e.printStackTrace()
            ApiResponse(success = false, message = "Error deleting user: ${e.message}")
        }
    }

    /**
     * Get user statistics
     */
    fun getUserStatistics(): ApiResponse<UserStatisticsDto> {
        return try {
            transaction {
                val totalUsers = Users.selectAll().count().toInt()
                val activeUsers = Users.select { Users.status eq UserStatus.ACTIVE }.count().toInt()
                val inactiveUsers = Users.select { Users.status eq UserStatus.INACTIVE }.count().toInt()
                val suspendedUsers = Users.select { Users.status eq UserStatus.SUSPENDED }.count().toInt()

                // Count by role
                val roleStats = UserRole.values().associate { role ->
                    role.name to Users.select { Users.role eq role }.count().toInt()
                }

                // Recent logins (last 24 hours)
                val recentLogins = Users.select {
                    Users.lastLoginAt greater java.time.Instant.now().minusSeconds(86400)
                }.count().toInt()

                val stats = UserStatisticsDto(
                    totalUsers = totalUsers,
                    activeUsers = activeUsers,
                    inactiveUsers = inactiveUsers,
                    suspendedUsers = suspendedUsers,
                    roleDistribution = roleStats,
                    recentLogins = recentLogins
                )

                ApiResponse(success = true, message = "User statistics retrieved", data = stats)
            }
        } catch (e: Exception) {
            println("Error retrieving user statistics: ${e.message}")
            e.printStackTrace()
            ApiResponse(success = false, message = "Error retrieving user statistics: ${e.message}")
        }
    }
}

// Data Transfer Objects
@kotlinx.serialization.Serializable
data class UserManagementDto(
    val id: String,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val role: String,
    val status: String,
    val branchId: String? = null,
    val branchName: String? = null,
    val employeeId: String? = null,
    val createdAt: String,
    val lastLoginAt: String? = null,
    val fullName: String
)

@kotlinx.serialization.Serializable
data class CreateUserManagementRequest(
    val username: String,
    val password: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val role: String,
    val status: String? = "ACTIVE",
    val branchId: String? = null,
    val employeeId: String? = null
)

@kotlinx.serialization.Serializable
data class UpdateUserManagementRequest(
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null,
    val role: String? = null,
    val status: String? = null,
    val branchId: String? = null,
    val employeeId: String? = null,
    val password: String? = null
)

@kotlinx.serialization.Serializable
data class UserStatisticsDto(
    val totalUsers: Int,
    val activeUsers: Int,
    val inactiveUsers: Int,
    val suspendedUsers: Int,
    val roleDistribution: Map<String, Int>,
    val recentLogins: Int
)
