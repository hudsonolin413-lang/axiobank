package org.dals.project.services

import org.dals.project.database.*
import org.dals.project.models.*
import org.dals.project.utils.IdGenerator
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class CustomerService {

    suspend fun getAllCustomers(page: Int = 1, pageSize: Int = 10, branchId: UUID? = null): ListResponse<CustomerDto> {
        return DatabaseFactory.dbQuery {
            val offset = (page - 1) * pageSize

            val query = if (branchId != null) {
                Customers.select { Customers.branchId eq branchId }
            } else {
                Customers.selectAll()
            }

            val customers = query
                .limit(pageSize, offset.toLong())
                .map { toCustomerDto(it) }

            val total = if (branchId != null) {
                Customers.select { Customers.branchId eq branchId }.count().toInt()
            } else {
                Customers.selectAll().count().toInt()
            }

            ListResponse(
                success = true,
                message = "Customers retrieved successfully",
                data = customers,
                total = total,
                page = page,
                pageSize = pageSize
            )
        }
    }

    suspend fun searchCustomers(query: String, branchId: UUID? = null): ApiResponse<List<CustomerDto>> {
        return DatabaseFactory.dbQuery {
            try {
                println("=== CUSTOMER SERVICE SEARCH ===")
                println("Query: '$query', BranchId: $branchId")

                // First, let's check total customers in database
                val totalCustomers = if (branchId != null) {
                    Customers.select { Customers.branchId eq branchId }.count()
                } else {
                    Customers.selectAll().count()
                }
                println("Total customers in database: $totalCustomers (branchId filter: ${branchId != null})")

                val searchQuery = "%${query.trim().lowercase()}%"
                val phoneQuery = "%${query.trim().replace(Regex("[^0-9]"), "")}%"

                println("Search patterns - text: '$searchQuery', phone: '$phoneQuery'")

                val baseQuery = if (branchId != null) {
                    println("Filtering by branchId: $branchId")
                    Customers.select { Customers.branchId eq branchId }
                } else {
                    println("Searching all branches")
                    Customers.selectAll()
                }

                // Log how many rows in base query
                val baseCount = baseQuery.count()
                println("Base query returned $baseCount rows before search filter")

                val customers = baseQuery.andWhere {
                    (Customers.firstName.lowerCase() like searchQuery) or
                    (Customers.lastName.lowerCase() like searchQuery) or
                    (Customers.email.lowerCase() like searchQuery) or
                    (Customers.phoneNumber like phoneQuery) or
                    (Customers.customerNumber like searchQuery) or
                    (Customers.customerNumber.lowerCase() like searchQuery)
                }.map { toCustomerDto(it) }

                println("After applying search filters: Found ${customers.size} customers")

                if (customers.isNotEmpty()) {
                    println("Customer results:")
                    customers.forEach { 
                        println("  - ${it.firstName} ${it.lastName} (${it.customerNumber}, ${it.email}, ${it.phoneNumber})")
                    }
                } else {
                    println("No customers matched the search criteria")
                }

                ApiResponse(
                    success = true,
                    message = if (customers.isNotEmpty()) "Found ${customers.size} customer(s)" else "No customers found",
                    data = customers
                )
            } catch (e: Exception) {
                println("ERROR in searchCustomers: ${e.message}")
                e.printStackTrace()
                ApiResponse(
                    success = false,
                    message = "Error searching customers: ${e.message}",
                    data = emptyList(),
                    error = e.message
                )
            }
        }
    }

    suspend fun getCustomerById(id: UUID): ApiResponse<CustomerDto> {
        return DatabaseFactory.dbQuery {
            val customer = Customers.select { Customers.id eq id }
                .singleOrNull()
                ?.let { toCustomerDto(it) }

            if (customer != null) {
                ApiResponse(
                    success = true,
                    message = "Customer found",
                    data = customer
                )
            } else {
                ApiResponse(
                    success = false,
                    message = "Customer not found",
                    error = "No customer found with ID: $id"
                )
            }
        }
    }

    suspend fun getCustomerByNumber(customerNumber: String): ApiResponse<CustomerDto> {
        return DatabaseFactory.dbQuery {
            val customer = Customers.select { Customers.customerNumber eq customerNumber }
                .singleOrNull()
                ?.let { toCustomerDto(it) }

            if (customer != null) {
                ApiResponse(
                    success = true,
                    message = "Customer found",
                    data = customer
                )
            } else {
                ApiResponse(
                    success = false,
                    message = "Customer not found",
                    error = "No customer found with number: $customerNumber"
                )
            }
        }
    }

    suspend fun createCustomer(request: CreateCustomerRequest): ApiResponse<CustomerDto> {
        return DatabaseFactory.dbQuery {
            try {
                val customerId = UUID.randomUUID()
                val customerNumber = generateCustomerNumber()

                val insertedId = Customers.insert {
                    it[id] = customerId
                    it[Customers.customerNumber] = customerNumber
                    it[type] = CustomerType.valueOf(request.type.uppercase())
                    it[firstName] = request.firstName
                    it[lastName] = request.lastName
                    it[middleName] = request.middleName
                    it[dateOfBirth] = request.dateOfBirth?.let { LocalDate.parse(it) }
                    it[ssn] = request.ssn
                    it[email] = request.email
                    it[phoneNumber] = request.phoneNumber
                    it[alternatePhone] = request.alternatePhone
                    it[primaryStreet] = request.primaryStreet
                    it[primaryCity] = request.primaryCity
                    it[primaryState] = request.primaryState
                    it[primaryZipCode] = request.primaryZipCode
                    it[primaryCountry] = request.primaryCountry
                    it[mailingStreet] = request.mailingStreet
                    it[mailingCity] = request.mailingCity
                    it[mailingState] = request.mailingState
                    it[mailingZipCode] = request.mailingZipCode
                    it[mailingCountry] = request.mailingCountry
                    it[occupation] = request.occupation
                    it[employer] = request.employer
                    it[annualIncome] = request.annualIncome?.let { amount -> BigDecimal(amount.toString()) }
                    it[creditScore] = request.creditScore
                    it[branchId] = UUID.fromString(request.branchId)
                    it[businessName] = request.businessName
                    it[businessType] = request.businessType
                    it[taxId] = request.taxId
                    it[businessLicenseNumber] = request.businessLicenseNumber
                }[Customers.id]

                val createdCustomer = Customers.select { Customers.id eq customerId }
                    .single()
                    .let { toCustomerDto(it) }

                ApiResponse(
                    success = true,
                    message = "Customer created successfully",
                    data = createdCustomer
                )
            } catch (e: Exception) {
                ApiResponse(
                    success = false,
                    message = "Failed to create customer",
                    error = e.message
                )
            }
        }
    }

    suspend fun updateCustomer(id: UUID, request: CreateCustomerRequest): ApiResponse<CustomerDto> {
        return DatabaseFactory.dbQuery {
            try {
                val updated = Customers.update({ Customers.id eq id }) {
                    it[type] = CustomerType.valueOf(request.type.uppercase())
                    it[firstName] = request.firstName
                    it[lastName] = request.lastName
                    it[middleName] = request.middleName
                    it[dateOfBirth] = request.dateOfBirth?.let { LocalDate.parse(it) }
                    it[ssn] = request.ssn
                    it[email] = request.email
                    it[phoneNumber] = request.phoneNumber
                    it[alternatePhone] = request.alternatePhone
                    it[primaryStreet] = request.primaryStreet
                    it[primaryCity] = request.primaryCity
                    it[primaryState] = request.primaryState
                    it[primaryZipCode] = request.primaryZipCode
                    it[primaryCountry] = request.primaryCountry
                    it[mailingStreet] = request.mailingStreet
                    it[mailingCity] = request.mailingCity
                    it[mailingState] = request.mailingState
                    it[mailingZipCode] = request.mailingZipCode
                    it[mailingCountry] = request.mailingCountry
                    it[occupation] = request.occupation
                    it[employer] = request.employer
                    it[annualIncome] = request.annualIncome?.let { amount -> BigDecimal(amount.toString()) }
                    it[creditScore] = request.creditScore
                    it[branchId] = UUID.fromString(request.branchId)
                    it[businessName] = request.businessName
                    it[businessType] = request.businessType
                    it[taxId] = request.taxId
                    it[businessLicenseNumber] = request.businessLicenseNumber
                }

                if (updated > 0) {
                    val updatedCustomer = Customers.select { Customers.id eq id }
                        .single()
                        .let { toCustomerDto(it) }

                    ApiResponse(
                        success = true,
                        message = "Customer updated successfully",
                        data = updatedCustomer
                    )
                } else {
                    ApiResponse(
                        success = false,
                        message = "Customer not found",
                        error = "No customer found with ID: $id"
                    )
                }
            } catch (e: Exception) {
                ApiResponse(
                    success = false,
                    message = "Failed to update customer",
                    error = e.message
                )
            }
        }
    }

    suspend fun deleteCustomer(id: UUID): ApiResponse<String> {
        return DatabaseFactory.dbQuery {
            try {
                val deleted = Customers.deleteWhere { Customers.id eq id }

                if (deleted > 0) {
                    ApiResponse(
                        success = true,
                        message = "Customer deleted successfully",
                        data = "Customer with ID $id has been deleted"
                    )
                } else {
                    ApiResponse(
                        success = false,
                        message = "Customer not found",
                        error = "No customer found with ID: $id"
                    )
                }
            } catch (e: Exception) {
                ApiResponse(
                    success = false,
                    message = "Failed to delete customer",
                    error = e.message
                )
            }
        }
    }

    private fun generateCustomerNumber(): String {
        // Generate a 6-digit customer number
        return IdGenerator.generateCustomerNumber()
    }

    private fun toCustomerDto(row: ResultRow): CustomerDto {
        return CustomerDto(
            id = row[Customers.id].toString(),
            customerNumber = row[Customers.customerNumber],
            type = row[Customers.type].name,
            status = row[Customers.status].name,
            firstName = row[Customers.firstName],
            lastName = row[Customers.lastName],
            middleName = row[Customers.middleName],
            dateOfBirth = row[Customers.dateOfBirth]?.toString(),
            ssn = null, // SECURITY: Never expose SSN in API responses
            email = row[Customers.email],
            phoneNumber = row[Customers.phoneNumber],
            alternatePhone = row[Customers.alternatePhone],
            primaryStreet = row[Customers.primaryStreet],
            primaryCity = row[Customers.primaryCity],
            primaryState = row[Customers.primaryState],
            primaryZipCode = row[Customers.primaryZipCode],
            primaryCountry = row[Customers.primaryCountry],
            mailingStreet = row[Customers.mailingStreet],
            mailingCity = row[Customers.mailingCity],
            mailingState = row[Customers.mailingState],
            mailingZipCode = row[Customers.mailingZipCode],
            mailingCountry = row[Customers.mailingCountry],
            occupation = row[Customers.occupation],
            employer = row[Customers.employer],
            annualIncome = null, // SECURITY: Remove income data from general responses
            creditScore = null, // SECURITY: Remove credit score from general responses
            branchId = row[Customers.branchId].toString(),
            accountManagerId = row[Customers.accountManagerId]?.toString(),
            onboardedDate = row[Customers.onboardedDate].toString(),
            lastContactDate = row[Customers.lastContactDate]?.toString(),
            riskLevel = row[Customers.riskLevel],
            kycStatus = row[Customers.kycStatus],
            businessName = row[Customers.businessName],
            businessType = row[Customers.businessType],
            taxId = null, // SECURITY: Never expose Tax ID in API responses
            businessLicenseNumber = null, // SECURITY: Remove business license from general responses
            createdAt = row[Customers.createdAt].toString(),
            updatedAt = row[Customers.updatedAt].toString()
        )
    }
}