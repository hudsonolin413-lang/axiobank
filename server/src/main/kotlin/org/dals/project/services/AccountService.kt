package org.dals.project.services

import org.dals.project.database.*
import org.dals.project.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.util.*

class AccountService {

    suspend fun getAllAccounts(page: Int = 1, pageSize: Int = 10): ListResponse<AccountDto> {
        return DatabaseFactory.dbQuery {
            val offset = (page - 1) * pageSize
            val accounts = Accounts.selectAll()
                .limit(pageSize, offset.toLong())
                .map { toAccountDto(it) }

            val total = Accounts.selectAll().count().toInt()

            ListResponse(
                success = true,
                message = "Accounts retrieved successfully",
                data = accounts,
                total = total,
                page = page,
                pageSize = pageSize
            )
        }
    }

    suspend fun getAccountById(id: UUID): ApiResponse<AccountDto> {
        return DatabaseFactory.dbQuery {
            val account = Accounts.select { Accounts.id eq id }
                .singleOrNull()
                ?.let { toAccountDto(it) }

            if (account != null) {
                ApiResponse(
                    success = true,
                    message = "Account found",
                    data = account
                )
            } else {
                ApiResponse(
                    success = false,
                    message = "Account not found",
                    error = "No account found with ID: $id"
                )
            }
        }
    }

    suspend fun getAccountByNumber(accountNumber: String): ApiResponse<AccountDto> {
        return DatabaseFactory.dbQuery {
            val account = Accounts.select { Accounts.accountNumber eq accountNumber }
                .singleOrNull()
                ?.let { toAccountDto(it) }

            if (account != null) {
                ApiResponse(
                    success = true,
                    message = "Account found",
                    data = account
                )
            } else {
                ApiResponse(
                    success = false,
                    message = "Account not found",
                    error = "No account found with number: $accountNumber"
                )
            }
        }
    }

    suspend fun getAccountsByCustomerId(customerId: UUID): ListResponse<AccountDto> {
        return DatabaseFactory.dbQuery {
            val accounts = Accounts.select { Accounts.customerId eq customerId }
                .map { toAccountDto(it) }

            ListResponse(
                success = true,
                message = "Customer accounts retrieved successfully",
                data = accounts,
                total = accounts.size
            )
        }
    }

    suspend fun createAccount(request: CreateAccountRequest): ApiResponse<AccountDto> {
        return DatabaseFactory.dbQuery {
            try {
                val accountId = UUID.randomUUID()
                val accountNumber = generateAccountNumber(request.type)

                val insertedId = Accounts.insert {
                    it[id] = accountId
                    it[Accounts.accountNumber] = accountNumber
                    it[customerId] = UUID.fromString(request.customerId)
                    it[type] = AccountType.valueOf(request.type.uppercase())
                    it[minimumBalance] = BigDecimal(request.minimumBalance)
                    it[interestRate] = BigDecimal(request.interestRate)
                    it[creditLimit] = request.creditLimit?.let { BigDecimal(it) }
                    it[branchId] = UUID.fromString(request.branchId)
                    it[accountManagerId] = request.accountManagerId?.let { UUID.fromString(it) }
                    it[isJointAccount] = request.isJointAccount
                    it[nickname] = request.nickname
                }[Accounts.id]

                val createdAccount = Accounts.select { Accounts.id eq accountId }
                    .single()
                    .let { toAccountDto(it) }

                ApiResponse(
                    success = true,
                    message = "Account created successfully",
                    data = createdAccount
                )
            } catch (e: Exception) {
                ApiResponse(
                    success = false,
                    message = "Failed to create account",
                    error = e.message
                )
            }
        }
    }

    suspend fun updateAccount(id: UUID, request: CreateAccountRequest): ApiResponse<AccountDto> {
        return DatabaseFactory.dbQuery {
            try {
                val updated = Accounts.update({ Accounts.id eq id }) {
                    it[customerId] = UUID.fromString(request.customerId)
                    it[type] = AccountType.valueOf(request.type.uppercase())
                    it[minimumBalance] = BigDecimal(request.minimumBalance)
                    it[interestRate] = BigDecimal(request.interestRate)
                    it[creditLimit] = request.creditLimit?.let { BigDecimal(it) }
                    it[branchId] = UUID.fromString(request.branchId)
                    it[accountManagerId] = request.accountManagerId?.let { UUID.fromString(it) }
                    it[isJointAccount] = request.isJointAccount
                    it[nickname] = request.nickname
                }

                if (updated > 0) {
                    val updatedAccount = Accounts.select { Accounts.id eq id }
                        .single()
                        .let { toAccountDto(it) }

                    ApiResponse(
                        success = true,
                        message = "Account updated successfully",
                        data = updatedAccount
                    )
                } else {
                    ApiResponse(
                        success = false,
                        message = "Account not found",
                        error = "No account found with ID: $id"
                    )
                }
            } catch (e: Exception) {
                ApiResponse(
                    success = false,
                    message = "Failed to update account",
                    error = e.message
                )
            }
        }
    }

    suspend fun updateAccountBalance(accountId: UUID, newBalance: BigDecimal): ApiResponse<AccountDto> {
        return DatabaseFactory.dbQuery {
            try {
                val updated = Accounts.update({ Accounts.id eq accountId }) {
                    it[balance] = newBalance
                    it[availableBalance] = newBalance
                }

                if (updated > 0) {
                    val updatedAccount = Accounts.select { Accounts.id eq accountId }
                        .single()
                        .let { toAccountDto(it) }

                    ApiResponse(
                        success = true,
                        message = "Account balance updated successfully",
                        data = updatedAccount
                    )
                } else {
                    ApiResponse(
                        success = false,
                        message = "Account not found",
                        error = "No account found with ID: $accountId"
                    )
                }
            } catch (e: Exception) {
                ApiResponse(
                    success = false,
                    message = "Failed to update account balance",
                    error = e.message
                )
            }
        }
    }

    suspend fun deleteAccount(id: UUID): ApiResponse<String> {
        return DatabaseFactory.dbQuery {
            try {
                val deleted = Accounts.deleteWhere { Accounts.id eq id }

                if (deleted > 0) {
                    ApiResponse(
                        success = true,
                        message = "Account deleted successfully",
                        data = "Account with ID $id has been deleted"
                    )
                } else {
                    ApiResponse(
                        success = false,
                        message = "Account not found",
                        error = "No account found with ID: $id"
                    )
                }
            } catch (e: Exception) {
                ApiResponse(
                    success = false,
                    message = "Failed to delete account",
                    error = e.message
                )
            }
        }
    }

    private fun generateAccountNumber(accountType: String): String {
        // Generate 7-digit numeric account number (1000000 to 9999999)
        return (1000000..9999999).random().toString()
    }

    private fun toAccountDto(row: ResultRow): AccountDto {
        return AccountDto(
            id = row[Accounts.id].toString(),
            accountNumber = row[Accounts.accountNumber],
            customerId = row[Accounts.customerId].toString(),
            type = row[Accounts.type].name,
            status = row[Accounts.status].name,
            balance = row[Accounts.balance].toString(),
            availableBalance = row[Accounts.availableBalance].toString(),
            minimumBalance = row[Accounts.minimumBalance].toString(),
            interestRate = row[Accounts.interestRate].toString(),
            creditLimit = row[Accounts.creditLimit]?.toString(),
            branchId = row[Accounts.branchId].toString(),
            openedDate = row[Accounts.openedDate].toString(),
            closedDate = row[Accounts.closedDate]?.toString(),
            lastTransactionDate = row[Accounts.lastTransactionDate]?.toString(),
            accountManagerId = row[Accounts.accountManagerId]?.toString(),
            isJointAccount = row[Accounts.isJointAccount],
            nickname = row[Accounts.nickname],
            createdAt = row[Accounts.createdAt].toString(),
            updatedAt = row[Accounts.updatedAt].toString()
        )
    }
}