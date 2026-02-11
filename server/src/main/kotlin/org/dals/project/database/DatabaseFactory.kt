package org.dals.project.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.dao.id.EntityID
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDate
import java.util.UUID

object DatabaseFactory {
    lateinit var database: Database
        private set

    fun init() {
        // Use DATABASE_URL from environment (Railway) or fall back to local config
        val databaseUrl = System.getenv("DATABASE_URL")

        try {
            database = if (databaseUrl != null) {
                // Railway/Production: Use individual environment variables
                val host = System.getenv("PGHOST") ?: "localhost"
                val port = System.getenv("PGPORT") ?: "5432"
                val dbName = System.getenv("PGDATABASE") ?: "railway"
                val user = System.getenv("PGUSER") ?: "postgres"
                val password = System.getenv("PGPASSWORD") ?: ""

                val config = HikariConfig().apply {
                    jdbcUrl = "jdbc:postgresql://$host:$port/$dbName"
                    username = user
                    this.password = password
                    driverClassName = "org.postgresql.Driver"
                    maximumPoolSize = 10
                }
                val dataSource = HikariDataSource(config)
                Database.connect(dataSource)
            } else {
                // Local development: Use localhost
                val jdbcURL = "jdbc:postgresql://localhost:5433/AxionBank"
                val username = "postgres"
                val password = "Andama@95"
                Database.connect(
                    url = jdbcURL,
                    driver = "org.postgresql.Driver",
                    user = username,
                    password = password
                )
            }

            transaction<Unit>(database) {
                // Create all tables automatically
                try {
                    SchemaUtils.create(
                        Branches,
                        Users,
                        UserSessions,
                        Employees,
                        EmployeeAttendance,
                        EmployeeLeaves,
                        Customers,
                        AccountTypes,
                        Accounts,
                        Transactions,
                        TransactionReversals,
                        LoanApplications,
                        Loans,
                        LoanPayments,
                        CreditAssessments,
                        KycProfiles,
                        KycDocuments,
                        DocumentAuditEntries,
                        SystemAlerts,
                        ComplianceAlerts,
                        WorkflowApprovals,
                        ComplianceChecks,
                        ServiceRequests,
                        Complaints,
                        CustomerCareAccessRequests,
                        Notifications,
                        AccountFreezeRequests,
                        AuditLogs,
                        // Cards Table
                        Cards,
                        // Drawer Management Tables
                        Drawers,
                        DrawerAssignments,
                        // Master Wallet System Tables
                        MasterWallets,
                        MasterWalletTransactions,
                        FloatAllocations,
                        WalletSecurityAlerts,
                        WalletAuditTrail,
                        ReconciliationRecords,
                        // Teller Tables
                        TellerCashDrawers,
                        TellerCashDrawerTransactions,
                        TellerFloatAllocations,
                        TellerReconciliation,
                        // M-Pesa Integration Tables
                        MpesaTransactions,
                        MpesaPhoneAccountLinks,
                        MpesaConfiguration,
                        MpesaAuditTrail,
                        // Transaction Fee Management Tables
                        TransactionFeeStructure,
                        TransactionFeeRecords
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw e
                }

                // Run migrations for existing tables
                try {
                    runMigrations()
                } catch (e: Exception) {
                    // allow app to continue
                }

                // Insert essential data if needed
                insertSampleDataIfNeeded()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T {
        return org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction(db = database) { block() }
    }

    private lateinit var dataSource: HikariDataSource

    private fun createHikariDataSource(): HikariDataSource {
        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = "jdbc:postgresql://localhost:5433/AxionBank"
            username = "postgres"
            password = "Andama@95"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        return HikariDataSource(config)
    }

    private fun getConnection(): java.sql.Connection {
        if (!::dataSource.isInitialized) {
            dataSource = createHikariDataSource()
        }
        return dataSource.connection
    }

    private fun runMigrations() {
        // Migration: Add drawer_id column to teller_cash_drawers if it doesn't exist
        try {
            transaction {
                exec("""
                    DO ${'$'}${'$'}
                    BEGIN
                        IF NOT EXISTS (
                            SELECT 1 FROM information_schema.columns
                            WHERE table_name='teller_cash_drawers' AND column_name='drawer_id'
                        ) THEN
                            ALTER TABLE teller_cash_drawers
                            ADD COLUMN drawer_id UUID REFERENCES drawers(id) ON DELETE CASCADE;
                        END IF;
                    END ${'$'}${'$'};
                """.trimIndent())
            }
        } catch (e: Exception) {
            // Silence migration warnings
        }

        // Migration: Add permissions column to users if it doesn't exist
        try {
            transaction {
                exec("""
                    DO ${'$'}${'$'}
                    BEGIN
                        IF NOT EXISTS (
                            SELECT 1 FROM information_schema.columns
                            WHERE table_name='users' AND column_name='permissions'
                        ) THEN
                            ALTER TABLE users
                            ADD COLUMN permissions TEXT DEFAULT '[]';
                        END IF;
                    END ${'$'}${'$'};
                """.trimIndent())
            }
        } catch (e: Exception) {
            // Silence migration warnings
        }

        // Migration: Update existing users with default permissions based on role
        try {
            transaction {
                // Update TELLER users
                exec("""
                    UPDATE users
                    SET permissions = '["PROCESS_TRANSACTIONS","VIEW_CUSTOMERS","MANAGE_CASH_DRAWER"]'
                    WHERE role = 'TELLER' AND (permissions = '[]' OR permissions IS NULL);
                """.trimIndent())

                // Update CUSTOMER_SERVICE_OFFICER users
                exec("""
                    UPDATE users
                    SET permissions = '["VIEW_CUSTOMERS","EDIT_CUSTOMERS","CREATE_ACCOUNTS","KYC_VERIFICATION"]'
                    WHERE role = 'CUSTOMER_SERVICE_OFFICER' AND (permissions = '[]' OR permissions IS NULL);
                """.trimIndent())

                // Update LOAN_OFFICER users
                exec("""
                    UPDATE users
                    SET permissions = '["VIEW_CUSTOMERS","APPROVE_LOANS","VIEW_REPORTS"]'
                    WHERE role = 'LOAN_OFFICER' AND (permissions = '[]' OR permissions IS NULL);
                """.trimIndent())

                // Update BRANCH_MANAGER users
                exec("""
                    UPDATE users
                    SET permissions = '["VIEW_CUSTOMERS","EDIT_CUSTOMERS","CREATE_ACCOUNTS","PROCESS_TRANSACTIONS","APPROVE_LOANS","VIEW_REPORTS","MANAGE_CASH_DRAWER","MANAGE_EMPLOYEES"]'
                    WHERE role = 'BRANCH_MANAGER' AND (permissions = '[]' OR permissions IS NULL);
                """.trimIndent())
            }
        } catch (e: Exception) {
            // Silence migration warnings
        }
    }

    private fun insertSampleDataIfNeeded() {
        // Check if we have any branches, if not, create a main branch
        if (Branches.selectAll().empty()) {
            insertMainBranch()
        }

        // Check for each essential user individually and create if missing
        insertEssentialSystemUsers()

        // Initialize Account Types for customer care
        insertAccountTypes()

        // Initialize Master Wallet System
        initializeMasterWalletSystem()

        // Check if customer care data exists
        val customersCount = Customers.selectAll().count()
        val accountTypesCount = AccountTypes.selectAll().count()
    }

    private fun insertMainBranch(): EntityID<UUID> {
        val branchId = Branches.insertAndGetId { row ->
            row[Branches.branchCode] = "MAIN001"
            row[Branches.name] = "Main Branch"
            row[Branches.street] = "123 Banking Street"
            row[Branches.city] = "New York"
            row[Branches.state] = "NY"
            row[Branches.zipCode] = "10001"
            row[Branches.phoneNumber] = "+1-555-BANK"
        }
        return branchId
    }

    private fun insertEssentialSystemUsers() {
        // Get or create main branch
        val branchId = Branches.selectAll().singleOrNull()?.get(Branches.id) ?: insertMainBranch()

        // Check and create each essential user individually
        val essentialUsers = listOf(
            UserCreationData(
                "admin",
                "admin@axionbank.com",
                "System",
                "Administrator",
                "+1-555-0001",
                UserRole.SYSTEM_ADMIN,
                "EMP001",
                "admin123"
            ),
            UserCreationData(
                "cso1",
                "cso1@axionbank.com",
                "Customer Service",
                "Officer 1",
                "+1-555-0002",
                UserRole.CUSTOMER_SERVICE_OFFICER,
                "CSO001",
                "cso123"
            ),
            UserCreationData(
                "teller1",
                "teller1@axionbank.com",
                "Bank",
                "Teller 1",
                "+1-555-0003",
                UserRole.TELLER,
                "TEL001",
                "teller123"
            ),
            UserCreationData(
                "manager1",
                "manager1@axionbank.com",
                "Branch",
                "Manager",
                "+1-555-0004",
                UserRole.BRANCH_MANAGER,
                "MGR001",
                "manager123"
            ),
            UserCreationData(
                "loanofficer1",
                "loans@axionbank.com",
                "Loan",
                "Officer",
                "+1-555-0005",
                UserRole.LOAN_OFFICER,
                "LO001",
                "loan123"
            )
        )

        var branchManagerId: EntityID<UUID>? = null

        essentialUsers.forEach { userData ->
            // Check if user already exists
            val existingUser = Users.select { Users.username eq userData.username }.singleOrNull()

            if (existingUser == null) {
                // User doesn't exist, create it
                // Get default permissions based on role
                val defaultPermissions = when (userData.role) {
                    UserRole.TELLER -> listOf("PROCESS_TRANSACTIONS", "VIEW_CUSTOMERS", "MANAGE_CASH_DRAWER")
                    UserRole.CUSTOMER_SERVICE_OFFICER -> listOf("VIEW_CUSTOMERS", "EDIT_CUSTOMERS", "CREATE_ACCOUNTS", "KYC_VERIFICATION")
                    UserRole.LOAN_OFFICER -> listOf("VIEW_CUSTOMERS", "APPROVE_LOANS", "VIEW_REPORTS")
                    UserRole.BRANCH_MANAGER -> listOf("VIEW_CUSTOMERS", "EDIT_CUSTOMERS", "CREATE_ACCOUNTS", "PROCESS_TRANSACTIONS", "APPROVE_LOANS", "VIEW_REPORTS", "MANAGE_CASH_DRAWER", "MANAGE_EMPLOYEES")
                    UserRole.SYSTEM_ADMIN -> emptyList()
                    else -> emptyList()
                }
                val permissionsJson = kotlinx.serialization.json.Json.encodeToString(defaultPermissions)

                val userId = Users.insertAndGetId {
                    it[Users.username] = userData.username
                    it[Users.email] = userData.email
                    it[Users.firstName] = userData.firstName
                    it[Users.lastName] = userData.lastName
                    it[Users.phoneNumber] = userData.phoneNumber
                    it[Users.role] = userData.role
                    it[Users.status] = UserStatus.ACTIVE
                    it[Users.branchId] = branchId.value
                    it[Users.employeeId] = userData.employeeId
                    val salt = BCrypt.gensalt()
                    it[Users.passwordHash] = BCrypt.hashpw(userData.password, salt)
                    it[Users.salt] = salt
                    it[Users.permissions] = permissionsJson
                }

                // Create corresponding employee record
                Employees.insert {
                    it[Employees.userId] = userId.value
                    it[Employees.employeeNumber] = userData.employeeId
                    it[Employees.department] = when (userData.role) {
                        UserRole.SYSTEM_ADMIN -> Department.IT
                        UserRole.CUSTOMER_SERVICE_OFFICER -> Department.CUSTOMER_SERVICE
                        UserRole.TELLER -> Department.OPERATIONS
                        UserRole.BRANCH_MANAGER -> Department.EXECUTIVE
                        UserRole.LOAN_OFFICER -> Department.LOANS
                        else -> Department.OPERATIONS
                    }
                    it[Employees.position] = userData.role.name.replace("_", " ")
                    it[Employees.employmentStatus] = EmploymentStatus.ACTIVE
                    it[Employees.hireDate] = LocalDate.now()
                    it[Employees.branchId] = branchId.value
                    it[Employees.accessLevel] = when (userData.role) {
                        UserRole.SYSTEM_ADMIN -> 5
                        UserRole.BRANCH_MANAGER -> 4
                        UserRole.LOAN_OFFICER -> 3
                        UserRole.CUSTOMER_SERVICE_OFFICER -> 2
                        UserRole.TELLER -> 2
                        else -> 1
                    }
                }

                // Store branch manager ID for later update
                if (userData.role == UserRole.BRANCH_MANAGER) {
                    branchManagerId = userId
                }
            } else {
                // Get default permissions based on role
                val defaultPermissions = when (userData.role) {
                    UserRole.TELLER -> listOf("PROCESS_TRANSACTIONS", "VIEW_CUSTOMERS", "MANAGE_CASH_DRAWER")
                    UserRole.CUSTOMER_SERVICE_OFFICER -> listOf("VIEW_CUSTOMERS", "EDIT_CUSTOMERS", "CREATE_ACCOUNTS", "KYC_VERIFICATION")
                    UserRole.LOAN_OFFICER -> listOf("VIEW_CUSTOMERS", "APPROVE_LOANS", "VIEW_REPORTS")
                    UserRole.BRANCH_MANAGER -> listOf("VIEW_CUSTOMERS", "EDIT_CUSTOMERS", "CREATE_ACCOUNTS", "PROCESS_TRANSACTIONS", "APPROVE_LOANS", "VIEW_REPORTS", "MANAGE_CASH_DRAWER", "MANAGE_EMPLOYEES")
                    UserRole.SYSTEM_ADMIN -> emptyList()
                    else -> emptyList()
                }
                val permissionsJson = kotlinx.serialization.json.Json.encodeToString(defaultPermissions)

                // Update existing user with correct password hash
                val salt = BCrypt.gensalt()
                val passwordHash = BCrypt.hashpw(userData.password, salt)

                // Check current permissions
                val currentPermissions = existingUser[Users.permissions]
                val shouldUpdatePermissions = currentPermissions.isNullOrEmpty() || currentPermissions == "[]"

                Users.update({ Users.username eq userData.username }) {
                    it[Users.passwordHash] = passwordHash
                    it[Users.salt] = salt
                    it[Users.role] = userData.role
                    it[Users.status] = UserStatus.ACTIVE
                    it[Users.firstName] = userData.firstName
                    it[Users.lastName] = userData.lastName
                    it[Users.phoneNumber] = userData.phoneNumber
                    it[Users.employeeId] = userData.employeeId
                    it[Users.branchId] = branchId.value
                    // Only update permissions if they're empty
                    if (shouldUpdatePermissions) {
                        it[Users.permissions] = permissionsJson
                    }
                }

                // Check if employee record exists, if not create it
                val employeeExists = Employees.select { Employees.userId eq existingUser[Users.id].value }.singleOrNull()
                if (employeeExists == null) {
                    Employees.insert {
                        it[Employees.userId] = existingUser[Users.id].value
                        it[Employees.employeeNumber] = userData.employeeId
                        it[Employees.department] = when (userData.role) {
                            UserRole.SYSTEM_ADMIN -> Department.IT
                            UserRole.CUSTOMER_SERVICE_OFFICER -> Department.CUSTOMER_SERVICE
                            UserRole.TELLER -> Department.OPERATIONS
                            UserRole.BRANCH_MANAGER -> Department.EXECUTIVE
                            UserRole.LOAN_OFFICER -> Department.LOANS
                            else -> Department.OPERATIONS
                        }
                        it[Employees.position] = userData.role.name.replace("_", " ")
                        it[Employees.employmentStatus] = EmploymentStatus.ACTIVE
                        it[Employees.hireDate] = LocalDate.now()
                        it[Employees.branchId] = branchId.value
                        it[Employees.accessLevel] = when (userData.role) {
                            UserRole.SYSTEM_ADMIN -> 5
                            UserRole.BRANCH_MANAGER -> 4
                            UserRole.LOAN_OFFICER -> 3
                            UserRole.CUSTOMER_SERVICE_OFFICER -> 2
                            UserRole.TELLER -> 2
                            else -> 1
                        }
                    }
                }

                // If this is the branch manager, get the ID for later update
                if (userData.role == UserRole.BRANCH_MANAGER) {
                    branchManagerId = existingUser[Users.id]
                }
            }
        }

        // Update the branch to set the manager if we have a branch manager
        branchManagerId?.let { managerId ->
            Branches.update({ Branches.id eq branchId }) {
                it[Branches.managerUserId] = managerId.value
            }
        }

        // Initialize drawers for the branch
        initializeDrawers(branchId)
    }

    private fun initializeMasterWalletSystem() {
        try {
            // Get the admin user ID for wallet creation
            val adminUser = Users.select { Users.username eq "admin" }
                .singleOrNull()?.get(Users.id)
                ?: return

            // Initialize master wallets using the service
            val masterWalletService = org.dals.project.services.MasterWalletService()
            masterWalletService.initializeMasterWallets(adminUser.value)
        } catch (e: Exception) {
            // Silence initialization errors
        }
    }

    private fun insertAccountTypes() {
        try {
            // Check if account types already exist
            if (AccountTypes.selectAll().count() > 0) {
                return
            }

            // Define comprehensive account types for customer care
            val accountTypesData = listOf(
                AccountTypeData(
                    "SAVINGS", "Personal Savings Account",
                    "Earn interest on your deposits with our standard savings account",
                    100.00, 25.00, 2.50, 0.00, null,
                    """["Interest earning", "Mobile banking", "ATM access", "Online banking"]""",
                    "PERSONAL", null, 500.00
                ),
                AccountTypeData(
                    "CHECKING", "Personal Checking Account",
                    "Full-service checking account for everyday banking needs",
                    50.00, 0.00, 0.25, 5.00, 100.00,
                    """["Unlimited transactions", "Debit card", "Mobile banking", "Check writing", "Online bill pay"]""",
                    "PERSONAL", null, 800.00
                ),
                AccountTypeData(
                    "BUSINESS_CHECKING", "Business Checking Account",
                    "Professional checking account designed for business operations",
                    500.00, 100.00, 1.00, 15.00, 1000.00,
                    """["Business features", "Higher limits", "Merchant services", "Business debit card", "Cash management"]""",
                    "BUSINESS", 200, 2000.00
                ),
                AccountTypeData(
                    "BUSINESS_SAVINGS", "Business Savings Account",
                    "High-yield savings account for business funds",
                    1000.00, 500.00, 1.75, 10.00, null,
                    """["Business savings features", "Higher interest rates", "Business banking", "Sweep accounts"]""",
                    "BUSINESS", 6, 1000.00
                ),
                AccountTypeData(
                    "PREMIUM_CHECKING", "Premium Checking Account",
                    "Premium account with enhanced benefits and higher limits",
                    1000.00, 500.00, 0.50, 25.00, 2000.00,
                    """["Premium benefits", "Higher transaction limits", "Priority customer service", "Travel benefits"]""",
                    "PERSONAL", null, 1500.00
                ),
                AccountTypeData(
                    "STUDENT_CHECKING", "Student Checking Account",
                    "Special checking account for students with no monthly fees",
                    0.00, 0.00, 0.10, 0.00, 50.00,
                    """["No monthly fees", "Student benefits", "Mobile banking", "ATM access", "Study abroad support"]""",
                    "PERSONAL", null, 300.00
                ),
                AccountTypeData(
                    "MONEY_MARKET", "Money Market Account",
                    "High-yield account combining features of savings and checking",
                    2500.00, 1000.00, 3.25, 12.00, null,
                    """["High interest rates", "Limited check writing", "Tiered interest", "ATM access"]""",
                    "PERSONAL", 6, 1000.00
                )
            )

            accountTypesData.forEach { accountTypeData ->
                AccountTypes.insert {
                    it[AccountTypes.typeName] = accountTypeData.typeName
                    it[AccountTypes.displayName] = accountTypeData.displayName
                    it[AccountTypes.description] = accountTypeData.description
                    it[AccountTypes.minimumDeposit] = java.math.BigDecimal(accountTypeData.minimumDeposit)
                    it[AccountTypes.minimumBalance] = java.math.BigDecimal(accountTypeData.minimumBalance)
                    it[AccountTypes.interestRate] =
                        java.math.BigDecimal(accountTypeData.interestRate).divide(java.math.BigDecimal("100"))
                    it[AccountTypes.monthlyMaintenanceFee] = java.math.BigDecimal(accountTypeData.monthlyMaintenanceFee)
                    it[AccountTypes.overdraftLimit] =
                        accountTypeData.overdraftLimit?.let { limit -> java.math.BigDecimal(limit) }
                    it[AccountTypes.features] = accountTypeData.features
                    it[AccountTypes.category] = accountTypeData.category
                    it[AccountTypes.maxTransactionsPerMonth] = accountTypeData.maxTransactionsPerMonth
                    it[AccountTypes.atmWithdrawalLimit] =
                        accountTypeData.atmWithdrawalLimit?.let { limit -> java.math.BigDecimal(limit) }
                }
            }
        } catch (e: Exception) {
            // Silence errors
        }
    }

    private data class AccountTypeData(
        val typeName: String,
        val displayName: String,
        val description: String,
        val minimumDeposit: Double,
        val minimumBalance: Double,
        val interestRate: Double, // As percentage
        val monthlyMaintenanceFee: Double,
        val overdraftLimit: Double?,
        val features: String, // JSON string
        val category: String,
        val maxTransactionsPerMonth: Int?,
        val atmWithdrawalLimit: Double?
    )

    // Data class to hold user creation information
    private data class UserCreationData(
        val username: String,
        val email: String,
        val firstName: String,
        val lastName: String,
        val phoneNumber: String,
        val role: UserRole,
        val employeeId: String,
        val password: String
    )

    private fun initializeDrawers(branchId: EntityID<UUID>) {
        // Check if drawers already exist
        val existingDrawersCount = Drawers.select { Drawers.branchId eq branchId.value }.count()

        if (existingDrawersCount > 0) {
            // Ensure teller has a drawer assignment
            ensureTellerDrawerAssignment(branchId)
            return
        }

        // Create 3 drawers for the branch
        val drawerNumbers = listOf("DRAWER-001", "DRAWER-002", "DRAWER-003")
        val drawerNames = listOf("Main Counter Drawer 1", "Main Counter Drawer 2", "Express Counter Drawer")
        val adminUser = Users.select { Users.username eq "admin" }.singleOrNull()

        drawerNumbers.forEachIndexed { index, drawerNumber ->
            Drawers.insert {
                it[Drawers.drawerNumber] = drawerNumber
                it[Drawers.branchId] = branchId.value
                it[Drawers.drawerName] = drawerNames[index]
                it[Drawers.location] = "Main Counter - Position ${index + 1}"
                it[Drawers.maxFloatAmount] = java.math.BigDecimal("50000.00")
                it[Drawers.minFloatAmount] = java.math.BigDecimal("5000.00")
                it[Drawers.status] = if (index == 0) "ASSIGNED" else "AVAILABLE"
                it[Drawers.isActive] = true
                adminUser?.let { user -> it[Drawers.createdBy] = user[Users.id].value }
            }
        }

        // Assign first drawer to teller1
        ensureTellerDrawerAssignment(branchId)
    }

    private fun ensureTellerDrawerAssignment(branchId: EntityID<UUID>) {
        val tellerUser = Users.select { Users.username eq "teller1" }.singleOrNull()
        val adminUser = Users.select { Users.username eq "admin" }.singleOrNull()

        if (tellerUser == null || adminUser == null) {
            return
        }

        // Check if teller already has an active assignment
        val existingAssignment = DrawerAssignments.select {
            (DrawerAssignments.userId eq tellerUser[Users.id].value) and
            (DrawerAssignments.status eq "ACTIVE")
        }.singleOrNull()

        if (existingAssignment != null) {
            return
        }

        // Get the first drawer
        val firstDrawer = Drawers.select {
            (Drawers.branchId eq branchId.value) and
            (Drawers.drawerNumber eq "DRAWER-001")
        }.singleOrNull()

        if (firstDrawer == null) {
            return
        }

        // Create drawer assignment for teller1
        DrawerAssignments.insert {
            it[DrawerAssignments.drawerId] = firstDrawer[Drawers.id].value
            it[DrawerAssignments.userId] = tellerUser[Users.id].value
            it[DrawerAssignments.branchId] = branchId.value
            it[DrawerAssignments.assignedBy] = adminUser[Users.id].value
            it[DrawerAssignments.status] = "ACTIVE"
            it[DrawerAssignments.accessLevel] = "FULL"
            it[DrawerAssignments.notes] = "Initial drawer assignment for teller"
        }

        // Update drawer status to ASSIGNED
        Drawers.update({ Drawers.id eq firstDrawer[Drawers.id] }) {
            it[Drawers.status] = "ASSIGNED"
        }
    }

    fun getInvestmentRepository(): InvestmentRepository {
        return InvestmentRepository(getConnection())
    }
}