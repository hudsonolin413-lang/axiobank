package org.dals.project.services

import org.dals.project.database.*
import org.dals.project.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class EmployeeService {

    private fun parsePermissions(permissionsJson: String): List<String> {
        return try {
            Json.decodeFromString(permissionsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun generateEmployeeNumber(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "EMP${timestamp}${random}"
    }

    suspend fun createEmployee(request: CreateEmployeeRequest): ApiResponse<EmployeeDto> {
        return transaction {
            try {
                // First, create the user account
                val userId = UUID.randomUUID()
                val employeeNumber = generateEmployeeNumber()

                // Hash password
                val salt = org.mindrot.jbcrypt.BCrypt.gensalt()
                val passwordHash = org.mindrot.jbcrypt.BCrypt.hashpw(request.password, salt)

                // Create user
                Users.insert {
                    it[id] = userId
                    it[username] = request.username
                    it[email] = request.email
                    it[firstName] = request.firstName
                    it[lastName] = request.lastName
                    it[phoneNumber] = request.phoneNumber
                    it[role] = UserRole.valueOf(request.role)
                    it[status] = UserStatus.ACTIVE
                    it[branchId] = UUID.fromString(request.branchId)
                    it[employeeId] = employeeNumber
                    it[this.passwordHash] = passwordHash
                    it[this.salt] = salt
                }

                // Create employee record
                val employeeId = Employees.insert {
                    it[this.userId] = userId
                    it[this.employeeNumber] = employeeNumber
                    it[department] = Department.valueOf(request.department)
                    it[position] = request.position
                    it[employmentStatus] = EmploymentStatus.ACTIVE
                    it[hireDate] = LocalDate.parse(request.hireDate)
                    it[salary] = request.salary?.let { sal -> java.math.BigDecimal.valueOf(sal) }
                    it[this.branchId] = UUID.fromString(request.branchId)
                    it[accessLevel] = when (UserRole.valueOf(request.role)) {
                        UserRole.SYSTEM_ADMIN -> 5
                        UserRole.BRANCH_MANAGER -> 4
                        UserRole.LOAN_OFFICER -> 3
                        UserRole.CUSTOMER_SERVICE_OFFICER -> 2
                        UserRole.TELLER -> 2
                        else -> 1
                    }
                } get Employees.id

                // Fetch and return the created employee
                val employeeRow = Employees.select { Employees.id eq employeeId }.single()
                val userRow = Users.select { Users.id eq userId }.single()

                ApiResponse(
                    success = true,
                    message = "Employee created successfully",
                    data = EmployeeDto(
                        id = employeeRow[Employees.id].toString(),
                        userId = userId.toString(),
                        employeeNumber = employeeNumber,
                        department = employeeRow[Employees.department].name,
                        position = employeeRow[Employees.position],
                        employmentStatus = employeeRow[Employees.employmentStatus].name,
                        hireDate = employeeRow[Employees.hireDate].toString(),
                        terminationDate = null,
                        salary = employeeRow[Employees.salary]?.toDouble(),
                        managerId = null,
                        branchId = employeeRow[Employees.branchId].toString(),
                        emergencyContactName = null,
                        emergencyContactPhone = null,
                        emergencyContactRelation = null,
                        shiftType = null,
                        performanceRating = null,
                        lastReviewDate = null,
                        nextReviewDate = null,
                        accessLevel = employeeRow[Employees.accessLevel],
                        user = UserDto(
                            id = userRow[Users.id].toString(),
                            username = userRow[Users.username],
                            email = userRow[Users.email],
                            firstName = userRow[Users.firstName],
                            lastName = userRow[Users.lastName],
                            phoneNumber = userRow[Users.phoneNumber],
                            role = userRow[Users.role].name,
                            status = userRow[Users.status].name,
                            branchId = userRow[Users.branchId]?.toString(),
                            employeeId = userRow[Users.employeeId],
                            createdAt = userRow[Users.createdAt].toString(),
                            lastLoginAt = null
                        ),
                        createdAt = employeeRow[Employees.createdAt].toString(),
                        updatedAt = employeeRow[Employees.updatedAt].toString()
                    )
                )
            } catch (e: Exception) {
                println("Error creating employee: ${e.message}")
                e.printStackTrace()
                ApiResponse(
                    success = false,
                    message = "Failed to create employee: ${e.message}",
                    error = e.message
                )
            }
        }
    }

    suspend fun getAllEmployees(page: Int = 1, pageSize: Int = 10, branchId: UUID? = null): ListResponse<EmployeeDto> {
        return transaction {
            val query = if (branchId != null) {
                Employees.select { Employees.branchId eq branchId }
            } else {
                Employees.selectAll()
            }

            val total = query.count().toInt()
            val employees = query
                .orderBy(Employees.createdAt, SortOrder.DESC)
                .limit(pageSize, ((page - 1) * pageSize).toLong())
                .map { employeeRow ->
                    val userId = employeeRow[Employees.userId]
                    val userRow = Users.select { Users.id eq userId }.singleOrNull()
                    
                    EmployeeDto(
                        id = employeeRow[Employees.id].toString(),
                        userId = userId.toString(),
                        employeeNumber = employeeRow[Employees.employeeNumber],
                        department = employeeRow[Employees.department].name,
                        position = employeeRow[Employees.position],
                        employmentStatus = employeeRow[Employees.employmentStatus].name,
                        hireDate = employeeRow[Employees.hireDate].toString(),
                        terminationDate = employeeRow[Employees.terminationDate]?.toString(),
                        salary = employeeRow[Employees.salary]?.toDouble(),
                        managerId = employeeRow[Employees.managerId]?.toString(),
                        branchId = employeeRow[Employees.branchId].toString(),
                        emergencyContactName = employeeRow[Employees.emergencyContactName],
                        emergencyContactPhone = employeeRow[Employees.emergencyContactPhone],
                        emergencyContactRelation = employeeRow[Employees.emergencyContactRelation],
                        shiftType = employeeRow[Employees.shiftType],
                        performanceRating = employeeRow[Employees.performanceRating]?.toDouble(),
                        lastReviewDate = employeeRow[Employees.lastReviewDate]?.toString(),
                        nextReviewDate = employeeRow[Employees.nextReviewDate]?.toString(),
                        accessLevel = employeeRow[Employees.accessLevel],
                        user = userRow?.let {
                            UserDto(
                                id = it[Users.id].toString(),
                                username = it[Users.username],
                                email = it[Users.email],
                                firstName = it[Users.firstName],
                                lastName = it[Users.lastName],
                                phoneNumber = it[Users.phoneNumber],
                                role = it[Users.role].name,
                                status = it[Users.status].name,
                                branchId = it[Users.branchId]?.toString(),
                                employeeId = it[Users.employeeId],
                                permissions = parsePermissions(it[Users.permissions]),
                                createdAt = it[Users.createdAt].toString(),
                                lastLoginAt = it[Users.lastLoginAt]?.toString()
                            )
                        },
                        createdAt = employeeRow[Employees.createdAt].toString(),
                        updatedAt = employeeRow[Employees.updatedAt].toString()
                    )
                }

            ListResponse(
                success = true,
                message = "Employees retrieved successfully",
                data = employees,
                total = total,
                page = page,
                pageSize = pageSize
            )
        }
    }

    suspend fun getEmployeeById(id: UUID): ApiResponse<EmployeeDto> {
        return transaction {
            val employeeRow = Employees.select { Employees.id eq id }.singleOrNull()

            if (employeeRow == null) {
                ApiResponse(success = false, message = "Employee not found")
            } else {
                val userId = employeeRow[Employees.userId]
                val userRow = Users.select { Users.id eq userId }.singleOrNull()

                ApiResponse(
                    success = true,
                    message = "Employee retrieved successfully",
                    data = EmployeeDto(
                        id = employeeRow[Employees.id].toString(),
                        userId = userId.toString(),
                        employeeNumber = employeeRow[Employees.employeeNumber],
                        department = employeeRow[Employees.department].name,
                        position = employeeRow[Employees.position],
                        employmentStatus = employeeRow[Employees.employmentStatus].name,
                        hireDate = employeeRow[Employees.hireDate].toString(),
                        terminationDate = employeeRow[Employees.terminationDate]?.toString(),
                        salary = employeeRow[Employees.salary]?.toDouble(),
                        managerId = employeeRow[Employees.managerId]?.toString(),
                        branchId = employeeRow[Employees.branchId].toString(),
                        emergencyContactName = employeeRow[Employees.emergencyContactName],
                        emergencyContactPhone = employeeRow[Employees.emergencyContactPhone],
                        emergencyContactRelation = employeeRow[Employees.emergencyContactRelation],
                        shiftType = employeeRow[Employees.shiftType],
                        performanceRating = employeeRow[Employees.performanceRating]?.toDouble(),
                        lastReviewDate = employeeRow[Employees.lastReviewDate]?.toString(),
                        nextReviewDate = employeeRow[Employees.nextReviewDate]?.toString(),
                        accessLevel = employeeRow[Employees.accessLevel],
                        user = userRow?.let {
                            UserDto(
                                id = it[Users.id].toString(),
                                username = it[Users.username],
                                email = it[Users.email],
                                firstName = it[Users.firstName],
                                lastName = it[Users.lastName],
                                phoneNumber = it[Users.phoneNumber],
                                role = it[Users.role].name,
                                status = it[Users.status].name,
                                branchId = it[Users.branchId]?.toString(),
                                employeeId = it[Users.employeeId],
                                permissions = parsePermissions(it[Users.permissions]),
                                createdAt = it[Users.createdAt].toString(),
                                lastLoginAt = it[Users.lastLoginAt]?.toString()
                            )
                        },
                        createdAt = employeeRow[Employees.createdAt].toString(),
                        updatedAt = employeeRow[Employees.updatedAt].toString()
                    )
                )
            }
        }
    }

    suspend fun updateEmployee(id: UUID, request: UpdateEmployeeRequest): ApiResponse<EmployeeDto> {
        val exists = transaction {
            val existing = Employees.select { Employees.id eq id }.singleOrNull()
            if (existing == null) {
                return@transaction false
            }

            Employees.update({ Employees.id eq id }) {
                request.department?.let { dept -> it[department] = Department.valueOf(dept) }
                request.position?.let { pos -> it[position] = pos }
                request.employmentStatus?.let { status -> it[employmentStatus] = EmploymentStatus.valueOf(status) }
                request.salary?.let { sal -> it[salary] = java.math.BigDecimal.valueOf(sal) }
                request.managerId?.let { mgr -> it[managerId] = UUID.fromString(mgr) }
                request.emergencyContactName?.let { name -> it[emergencyContactName] = name }
                request.emergencyContactPhone?.let { phone -> it[emergencyContactPhone] = phone }
                request.emergencyContactRelation?.let { rel -> it[emergencyContactRelation] = rel }
                request.shiftType?.let { shift -> it[shiftType] = shift }
                request.performanceRating?.let { rating -> it[performanceRating] = java.math.BigDecimal.valueOf(rating) }
            }
            true
        }

        return if (exists) {
            getEmployeeById(id)
        } else {
            ApiResponse(success = false, message = "Employee not found")
        }
    }

    suspend fun getEmployeeAttendance(employeeId: UUID, startDate: String?, endDate: String?): ListResponse<EmployeeAttendanceDto> {
        return transaction {
            var query = EmployeeAttendance.select { EmployeeAttendance.employeeId eq employeeId }

            startDate?.let {
                val start = LocalDate.parse(it, DateTimeFormatter.ISO_DATE)
                query = query.andWhere { EmployeeAttendance.date greaterEq start }
            }

            endDate?.let {
                val end = LocalDate.parse(it, DateTimeFormatter.ISO_DATE)
                query = query.andWhere { EmployeeAttendance.date lessEq end }
            }

            val attendance = query.orderBy(EmployeeAttendance.date, SortOrder.DESC)
                .map { row ->
                    EmployeeAttendanceDto(
                        id = row[EmployeeAttendance.id].toString(),
                        employeeId = row[EmployeeAttendance.employeeId].toString(),
                        date = row[EmployeeAttendance.date].toString(),
                        checkInTime = row[EmployeeAttendance.checkInTime]?.toString(),
                        checkOutTime = row[EmployeeAttendance.checkOutTime]?.toString(),
                        status = row[EmployeeAttendance.status],
                        hoursWorked = row[EmployeeAttendance.hoursWorked]?.toDouble(),
                        overtimeHours = row[EmployeeAttendance.overtimeHours].toDouble(),
                        notes = row[EmployeeAttendance.notes],
                        createdAt = row[EmployeeAttendance.createdAt].toString()
                    )
                }

            ListResponse(
                success = true,
                message = "Attendance records retrieved successfully",
                data = attendance,
                total = attendance.size,
                page = 1,
                pageSize = attendance.size
            )
        }
    }

    suspend fun getEmployeeLeaves(employeeId: UUID): ListResponse<EmployeeLeaveDto> {
        return transaction {
            val leaves = EmployeeLeaves.select { EmployeeLeaves.employeeId eq employeeId }
                .orderBy(EmployeeLeaves.startDate, SortOrder.DESC)
                .map { row ->
                    EmployeeLeaveDto(
                        id = row[EmployeeLeaves.id].toString(),
                        employeeId = row[EmployeeLeaves.employeeId].toString(),
                        leaveType = row[EmployeeLeaves.leaveType],
                        startDate = row[EmployeeLeaves.startDate].toString(),
                        endDate = row[EmployeeLeaves.endDate].toString(),
                        daysCount = row[EmployeeLeaves.daysCount],
                        reason = row[EmployeeLeaves.reason],
                        status = row[EmployeeLeaves.status],
                        approvedBy = row[EmployeeLeaves.approvedBy]?.toString(),
                        approvalDate = row[EmployeeLeaves.approvalDate]?.toString(),
                        rejectionReason = row[EmployeeLeaves.rejectionReason],
                        createdAt = row[EmployeeLeaves.createdAt].toString(),
                        updatedAt = row[EmployeeLeaves.updatedAt].toString()
                    )
                }

            ListResponse(
                success = true,
                message = "Leave records retrieved successfully",
                data = leaves,
                total = leaves.size,
                page = 1,
                pageSize = leaves.size
            )
        }
    }

    suspend fun createLeaveRequest(employeeId: UUID, request: CreateLeaveRequest): ApiResponse<EmployeeLeaveDto> {
        return transaction {
            val startDate = LocalDate.parse(request.startDate, DateTimeFormatter.ISO_DATE)
            val endDate = LocalDate.parse(request.endDate, DateTimeFormatter.ISO_DATE)
            val daysCount = java.time.Period.between(startDate, endDate).days + 1

            val leaveId = EmployeeLeaves.insert {
                it[this.employeeId] = employeeId
                it[leaveType] = request.leaveType
                it[this.startDate] = startDate
                it[this.endDate] = endDate
                it[this.daysCount] = daysCount
                it[reason] = request.reason
                it[status] = "PENDING"
            } get EmployeeLeaves.id

            val leaveRow = EmployeeLeaves.select { EmployeeLeaves.id eq leaveId }.single()

            ApiResponse(
                success = true,
                message = "Leave request created successfully",
                data = EmployeeLeaveDto(
                    id = leaveRow[EmployeeLeaves.id].toString(),
                    employeeId = leaveRow[EmployeeLeaves.employeeId].toString(),
                    leaveType = leaveRow[EmployeeLeaves.leaveType],
                    startDate = leaveRow[EmployeeLeaves.startDate].toString(),
                    endDate = leaveRow[EmployeeLeaves.endDate].toString(),
                    daysCount = leaveRow[EmployeeLeaves.daysCount],
                    reason = leaveRow[EmployeeLeaves.reason],
                    status = leaveRow[EmployeeLeaves.status],
                    approvedBy = leaveRow[EmployeeLeaves.approvedBy]?.toString(),
                    approvalDate = leaveRow[EmployeeLeaves.approvalDate]?.toString(),
                    rejectionReason = leaveRow[EmployeeLeaves.rejectionReason],
                    createdAt = leaveRow[EmployeeLeaves.createdAt].toString(),
                    updatedAt = leaveRow[EmployeeLeaves.updatedAt].toString()
                )
            )
        }
    }

    suspend fun approveLeave(leaveId: UUID, approverId: UUID): ApiResponse<EmployeeLeaveDto> {
        return transaction {
            EmployeeLeaves.update({ EmployeeLeaves.id eq leaveId }) {
                it[status] = "APPROVED"
                it[approvedBy] = approverId
                it[approvalDate] = LocalDate.now()
            }

            val leaveRow = EmployeeLeaves.select { EmployeeLeaves.id eq leaveId }.single()

            ApiResponse(
                success = true,
                message = "Leave request approved",
                data = EmployeeLeaveDto(
                    id = leaveRow[EmployeeLeaves.id].toString(),
                    employeeId = leaveRow[EmployeeLeaves.employeeId].toString(),
                    leaveType = leaveRow[EmployeeLeaves.leaveType],
                    startDate = leaveRow[EmployeeLeaves.startDate].toString(),
                    endDate = leaveRow[EmployeeLeaves.endDate].toString(),
                    daysCount = leaveRow[EmployeeLeaves.daysCount],
                    reason = leaveRow[EmployeeLeaves.reason],
                    status = leaveRow[EmployeeLeaves.status],
                    approvedBy = leaveRow[EmployeeLeaves.approvedBy]?.toString(),
                    approvalDate = leaveRow[EmployeeLeaves.approvalDate]?.toString(),
                    rejectionReason = leaveRow[EmployeeLeaves.rejectionReason],
                    createdAt = leaveRow[EmployeeLeaves.createdAt].toString(),
                    updatedAt = leaveRow[EmployeeLeaves.updatedAt].toString()
                )
            )
        }
    }

    suspend fun rejectLeave(leaveId: UUID, reason: String): ApiResponse<EmployeeLeaveDto> {
        return transaction {
            EmployeeLeaves.update({ EmployeeLeaves.id eq leaveId }) {
                it[status] = "REJECTED"
                it[rejectionReason] = reason
            }

            val leaveRow = EmployeeLeaves.select { EmployeeLeaves.id eq leaveId }.single()

            ApiResponse(
                success = true,
                message = "Leave request rejected",
                data = EmployeeLeaveDto(
                    id = leaveRow[EmployeeLeaves.id].toString(),
                    employeeId = leaveRow[EmployeeLeaves.employeeId].toString(),
                    leaveType = leaveRow[EmployeeLeaves.leaveType],
                    startDate = leaveRow[EmployeeLeaves.startDate].toString(),
                    endDate = leaveRow[EmployeeLeaves.endDate].toString(),
                    daysCount = leaveRow[EmployeeLeaves.daysCount],
                    reason = leaveRow[EmployeeLeaves.reason],
                    status = leaveRow[EmployeeLeaves.status],
                    approvedBy = leaveRow[EmployeeLeaves.approvedBy]?.toString(),
                    approvalDate = leaveRow[EmployeeLeaves.approvalDate]?.toString(),
                    rejectionReason = leaveRow[EmployeeLeaves.rejectionReason],
                    createdAt = leaveRow[EmployeeLeaves.createdAt].toString(),
                    updatedAt = leaveRow[EmployeeLeaves.updatedAt].toString()
                )
            )
        }
    }
}
