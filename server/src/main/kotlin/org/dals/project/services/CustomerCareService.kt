package org.dals.project.services

import org.dals.project.database.*
import org.dals.project.models.*
import org.dals.project.utils.IdGenerator
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

// Initialize repositories
private val accessRepository = CustomerCareAccessRepository()
private val reversalRepository = TransactionReversalRepository()

class CustomerCareService {

    fun getAllCustomers(page: Int = 1, pageSize: Int = 100): CustomerCareListResponse<CustomerDataDto> {
        return try {
            val offset = (page - 1) * pageSize

            val customers = transaction {
                val customersData = Customers.selectAll()
                    .limit(pageSize, offset.toLong())
                    .map { row ->
                        CustomerDataDto(
                            id = row[Customers.id].toString(),
                            customerNumber = row[Customers.customerNumber],
                            type = row[Customers.type].name,
                            status = row[Customers.status].name,
                            firstName = row[Customers.firstName] ?: "",
                            lastName = row[Customers.lastName] ?: "",
                            middleName = row[Customers.middleName],
                            dateOfBirth = row[Customers.dateOfBirth]?.toString(),
                            ssn = null, // SECURITY: Never expose SSN in API responses
                            email = row[Customers.email] ?: "",
                            phoneNumber = row[Customers.phoneNumber] ?: "",
                            alternatePhone = row[Customers.alternatePhone],
                            occupation = row[Customers.occupation],
                            employer = row[Customers.employer],
                            annualIncome = row[Customers.annualIncome]?.toDouble(),
                            creditScore = row[Customers.creditScore],
                            branchId = row[Customers.branchId].toString(),
                            accountManagerId = row[Customers.accountManagerId]?.toString(),
                            onboardedDate = row[Customers.onboardedDate].toString(),
                            lastContactDate = row[Customers.lastContactDate]?.toString(),
                            riskLevel = row[Customers.riskLevel],
                            kycStatus = row[Customers.kycStatus],
                            businessName = row[Customers.businessName],
                            businessType = row[Customers.businessType],
                            taxId = null, // SECURITY: Never expose Tax ID in API responses
                            businessLicenseNumber = null, // SECURITY: Remove license number from API responses
                            primaryAddress = if (row[Customers.primaryStreet] != null) {
                                AddressDataDto(
                                    street = row[Customers.primaryStreet]!!,
                                    city = row[Customers.primaryCity] ?: "",
                                    state = row[Customers.primaryState] ?: "",
                                    zipCode = row[Customers.primaryZipCode] ?: "",
                                    country = row[Customers.primaryCountry],
                                    type = "PRIMARY"
                                )
                            } else null,
                            mailingAddress = if (row[Customers.mailingStreet] != null) {
                                AddressDataDto(
                                    street = row[Customers.mailingStreet]!!,
                                    city = row[Customers.mailingCity] ?: "",
                                    state = row[Customers.mailingState] ?: "",
                                    zipCode = row[Customers.mailingZipCode] ?: "",
                                    country = row[Customers.mailingCountry] ?: "USA",
                                    type = "MAILING"
                                )
                            } else null
                        )
                    }

                val total = Customers.selectAll().count().toInt()
                Pair(customersData, total)
            }

            CustomerCareListResponse(
                success = true,
                message = "Customers retrieved successfully",
                data = customers.first,
                total = customers.second,
                page = page,
                pageSize = pageSize,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        } catch (e: Exception) {
            CustomerCareListResponse(
                success = false,
                message = "Failed to retrieve customers: ${e.message}",
                data = emptyList(),
                total = 0,
                page = 1,
                pageSize = 10,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }
    }

    fun getCustomerById(customerId: UUID): CustomerCareResponse<CustomerDataDto> {
        return try {
            val customer = transaction {
                Customers.select { Customers.id eq customerId }
                    .singleOrNull()
                    ?.let { row ->
                        CustomerDataDto(
                            id = row[Customers.id].toString(),
                            customerNumber = row[Customers.customerNumber],
                            type = row[Customers.type].name,
                            status = row[Customers.status].name,
                            firstName = row[Customers.firstName] ?: "",
                            lastName = row[Customers.lastName] ?: "",
                            middleName = row[Customers.middleName],
                            dateOfBirth = row[Customers.dateOfBirth]?.toString(),
                            ssn = null, // SECURITY: Never expose SSN in API responses
                            email = row[Customers.email] ?: "",
                            phoneNumber = row[Customers.phoneNumber] ?: "",
                            alternatePhone = row[Customers.alternatePhone],
                            occupation = row[Customers.occupation],
                            employer = row[Customers.employer],
                            annualIncome = row[Customers.annualIncome]?.toDouble(),
                            creditScore = row[Customers.creditScore],
                            branchId = row[Customers.branchId].toString(),
                            accountManagerId = row[Customers.accountManagerId]?.toString(),
                            onboardedDate = row[Customers.onboardedDate].toString(),
                            lastContactDate = row[Customers.lastContactDate]?.toString(),
                            riskLevel = row[Customers.riskLevel],
                            kycStatus = row[Customers.kycStatus],
                            businessName = row[Customers.businessName],
                            businessType = row[Customers.businessType],
                            taxId = null, // SECURITY: Never expose Tax ID in API responses
                            businessLicenseNumber = null, // SECURITY: Remove license number from API responses
                            primaryAddress = if (row[Customers.primaryStreet] != null) {
                                AddressDataDto(
                                    street = row[Customers.primaryStreet]!!,
                                    city = row[Customers.primaryCity] ?: "",
                                    state = row[Customers.primaryState] ?: "",
                                    zipCode = row[Customers.primaryZipCode] ?: "",
                                    country = row[Customers.primaryCountry],
                                    type = "PRIMARY"
                                )
                            } else null,
                            mailingAddress = if (row[Customers.mailingStreet] != null) {
                                AddressDataDto(
                                    street = row[Customers.mailingStreet]!!,
                                    city = row[Customers.mailingCity] ?: "",
                                    state = row[Customers.mailingState] ?: "",
                                    zipCode = row[Customers.mailingZipCode] ?: "",
                                    country = row[Customers.mailingCountry] ?: "USA",
                                    type = "MAILING"
                                )
                            } else null
                        )
                    }
            }

            if (customer != null) {
                CustomerCareResponse(
                    success = true,
                    message = "Customer retrieved successfully",
                    data = customer,
                    timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
            } else {
                CustomerCareResponse<CustomerDataDto>(
                    success = false,
                    message = "Customer not found",
                    timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
            }
        } catch (e: Exception) {
            CustomerCareResponse<CustomerDataDto>(
                success = false,
                message = "Failed to retrieve customer: ${e.message}",
                error = e.message,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }
    }

    fun getAllServiceRequests(page: Int = 1, pageSize: Int = 50): CustomerCareListResponse<ServiceRequestDataDto> {
        return try {
            val offset = (page - 1) * pageSize

            val result = transaction {
                val serviceRequestsData = ServiceRequests.selectAll()
                    .limit(pageSize, offset.toLong())
                    .map { row ->
                        ServiceRequestDataDto(
                            id = row[ServiceRequests.id].toString(),
                            customerId = row[ServiceRequests.customerId].toString(),
                            requestType = row[ServiceRequests.requestType],
                            title = row[ServiceRequests.title],
                            description = row[ServiceRequests.description],
                            status = row[ServiceRequests.status],
                            priority = row[ServiceRequests.priority],
                            createdBy = row[ServiceRequests.createdBy].toString(),
                            assignedTo = row[ServiceRequests.assignedTo]?.toString(),
                            completedBy = row[ServiceRequests.completedBy]?.toString(),
                            estimatedCompletionDate = row[ServiceRequests.estimatedCompletionDate]?.toString(),
                            actualCompletionDate = row[ServiceRequests.actualCompletionDate]?.toString(),
                            rejectionReason = row[ServiceRequests.rejectionReason],
                            approvalRequired = row[ServiceRequests.approvalRequired],
                            approvedBy = row[ServiceRequests.approvedBy]?.toString(),
                            approvedAt = row[ServiceRequests.approvedAt]?.toString(),
                            createdAt = row[ServiceRequests.createdAt].toString(),
                            updatedAt = row[ServiceRequests.updatedAt].toString(),
                            completedAt = row[ServiceRequests.completedAt]?.toString()
                        )
                    }

                val total = ServiceRequests.selectAll().count().toInt()
                Pair(serviceRequestsData, total)
            }

            CustomerCareListResponse(
                success = true,
                message = "Service requests retrieved successfully",
                data = result.first,
                total = result.second,
                page = page,
                pageSize = pageSize,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        } catch (e: Exception) {
            CustomerCareListResponse(
                success = false,
                message = "Failed to retrieve service requests: ${e.message}",
                data = emptyList(),
                total = 0,
                page = 1,
                pageSize = 10,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }
    }

    fun getAllComplaints(page: Int = 1, pageSize: Int = 50): CustomerCareListResponse<ComplaintDataDto> {
        return try {
            // For now, return mock data since complaints table structure needs to be defined
            val mockComplaints = listOf(
                ComplaintDataDto(
                    id = UUID.randomUUID().toString(),
                    customerId = UUID.randomUUID().toString(),
                    complaintType = "SERVICE_QUALITY",
                    title = "Service quality issue",
                    description = "Customer complaint about service quality",
                    status = "OPEN",
                    priority = "MEDIUM",
                    createdBy = "system",
                    assignedTo = null,
                    resolvedBy = null,
                    resolutionDate = null,
                    resolutionNotes = null,
                    createdAt = LocalDateTime.now().toString(),
                    updatedAt = LocalDateTime.now().toString()
                )
            )

            CustomerCareListResponse(
                success = true,
                message = "Complaints retrieved successfully",
                data = mockComplaints,
                total = mockComplaints.size,
                page = page,
                pageSize = pageSize,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        } catch (e: Exception) {
            CustomerCareListResponse(
                success = false,
                message = "Failed to retrieve complaints: ${e.message}",
                data = emptyList(),
                total = 0,
                page = 1,
                pageSize = 10,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }
    }

    fun getCustomerServiceMetrics(): CustomerCareResponse<CustomerServiceMetricsDto> {
        return try {
            val metrics = transaction {
                val totalCustomers = Customers.selectAll().count().toInt()
                val activeCustomers = Customers.select { Customers.status eq CustomerStatus.ACTIVE }.count().toInt()
                val pendingKYCDocuments = KycDocuments.select {
                    KycDocuments.status eq KycDocumentStatus.PENDING_REVIEW
                }.count().toInt()
                val openServiceRequests = ServiceRequests.select {
                    (ServiceRequests.status eq "PENDING") or (ServiceRequests.status eq "IN_PROGRESS")
                }.count().toInt()

                CustomerServiceMetricsDto(
                    totalCustomers = totalCustomers,
                    activeCustomers = activeCustomers,
                    pendingKYCDocuments = pendingKYCDocuments,
                    activeServiceRequests = openServiceRequests, // Changed from openServiceRequests to activeServiceRequests
                    openComplaints = (2..8).random(), // Mock data
                    newCustomersToday = (1..5).random(), // Mock data
                    completedServiceRequestsToday = (20..50).random(), // Mock data
                    resolvedComplaintsToday = (10..25).random() // Mock data
                )
            }

            CustomerCareResponse(
                success = true,
                message = "Customer service metrics retrieved successfully",
                data = metrics,
                timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        } catch (e: Exception) {
            CustomerCareResponse<CustomerServiceMetricsDto>(
                success = false,
                message = "Failed to retrieve metrics: ${e.message}",
                error = e.message,
                timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }
    }

    // Customer Care Access Request Methods
    fun createAccessRequest(request: CreateCustomerCareAccessRequest): CustomerCareResponse<CustomerCareAccessRequestDto> {
        return try {
            val accessRequest = accessRepository.createAccessRequest(request)

            CustomerCareResponse(
                success = true,
                message = "Customer care access request created successfully",
                data = accessRequest,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        } catch (e: Exception) {
            CustomerCareResponse<CustomerCareAccessRequestDto>(
                success = false,
                message = "Failed to create access request: ${e.message}",
                error = e.message,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }
    }

    fun getAllAccessRequests(page: Int = 1, pageSize: Int = 50): CustomerCareListResponse<CustomerCareAccessRequestDto> {
        return try {
            val (requests, total) = accessRepository.getAllAccessRequests(page, pageSize)

            CustomerCareListResponse(
                success = true,
                message = "Access requests retrieved successfully",
                data = requests,
                total = total,
                page = page,
                pageSize = pageSize,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        } catch (e: Exception) {
            CustomerCareListResponse(
                success = false,
                message = "Failed to retrieve access requests: ${e.message}",
                data = emptyList(),
                total = 0,
                page = 1,
                pageSize = 50,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }
    }

    fun getAccessRequestsByStatus(status: String, page: Int = 1, pageSize: Int = 50): CustomerCareListResponse<CustomerCareAccessRequestDto> {
        return try {
            val (requests, total) = accessRepository.getAccessRequestsByStatus(status, page, pageSize)

            CustomerCareListResponse(
                success = true,
                message = "Access requests retrieved successfully",
                data = requests,
                total = total,
                page = page,
                pageSize = pageSize,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        } catch (e: Exception) {
            CustomerCareListResponse(
                success = false,
                message = "Failed to retrieve access requests: ${e.message}",
                data = emptyList(),
                total = 0,
                page = 1,
                pageSize = 50,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }
    }

    fun getAccessRequestsByCustomerId(customerId: String): CustomerCareResponse<List<CustomerCareAccessRequestDto>> {
        return try {
            val requests = accessRepository.getAccessRequestsByCustomerId(customerId)

            CustomerCareResponse(
                success = true,
                message = "Access requests retrieved successfully",
                data = requests,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        } catch (e: Exception) {
            CustomerCareResponse<List<CustomerCareAccessRequestDto>>(
                success = false,
                message = "Failed to retrieve access requests: ${e.message}",
                error = e.message,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }
    }

    fun reviewAccessRequest(review: ReviewCustomerCareAccessRequest, reviewerId: String): CustomerCareResponse<CustomerCareAccessRequestDto> {
        return try {
            val accessRequest = when (review.action.uppercase()) {
                "APPROVE" -> accessRepository.approveAccessRequest(
                    requestId = review.requestId,
                    approvedBy = reviewerId,
                    reviewNotes = review.reviewNotes,
                    expiresAt = review.expiresAt
                )
                "REJECT" -> accessRepository.rejectAccessRequest(
                    requestId = review.requestId,
                    rejectedBy = reviewerId,
                    rejectionReason = review.rejectionReason ?: "No reason provided",
                    reviewNotes = review.reviewNotes
                )
                else -> throw IllegalArgumentException("Invalid action: ${review.action}. Must be APPROVE or REJECT")
            }

            CustomerCareResponse(
                success = true,
                message = "Access request ${review.action.lowercase()}d successfully",
                data = accessRequest,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        } catch (e: Exception) {
            CustomerCareResponse<CustomerCareAccessRequestDto>(
                success = false,
                message = "Failed to review access request: ${e.message}",
                error = e.message,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }
    }

    fun revokeAccessRequest(revoke: RevokeCustomerCareAccessRequest, revokerId: String): CustomerCareResponse<CustomerCareAccessRequestDto> {
        return try {
            val accessRequest = accessRepository.revokeAccessRequest(
                requestId = revoke.requestId,
                revokedBy = revokerId,
                revocationReason = revoke.revocationReason
            )

            CustomerCareResponse(
                success = true,
                message = "Access request revoked successfully",
                data = accessRequest,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        } catch (e: Exception) {
            CustomerCareResponse<CustomerCareAccessRequestDto>(
                success = false,
                message = "Failed to revoke access request: ${e.message}",
                error = e.message,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }
    }

    fun getActiveAccessForCustomer(customerId: String): CustomerCareResponse<CustomerCareAccessRequestDto> {
        return try {
            val accessRequest = accessRepository.getActiveAccessRequestForCustomer(customerId)

            if (accessRequest != null) {
                CustomerCareResponse(
                    success = true,
                    message = "Active access found",
                    data = accessRequest,
                    timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
            } else {
                CustomerCareResponse<CustomerCareAccessRequestDto>(
                    success = false,
                    message = "No active access found for customer",
                    timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
            }
        } catch (e: Exception) {
            CustomerCareResponse<CustomerCareAccessRequestDto>(
                success = false,
                message = "Failed to check active access: ${e.message}",
                error = e.message,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }
    }

    // Transaction Reversal Methods
    fun createTransactionReversal(request: CreateTransactionReversalRequest): CustomerCareResponse<TransactionReversalDto> {
        return try {
            // Validate that the transaction exists
            val transactionExists = transaction {
                Transactions.select { Transactions.id eq UUID.fromString(request.transactionId) }
                    .singleOrNull() != null
            }

            if (!transactionExists) {
                return CustomerCareResponse<TransactionReversalDto>(
                    success = false,
                    message = "Transaction not found",
                    error = "No transaction found with ID: ${request.transactionId}",
                    timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
            }

            val reversal = reversalRepository.createReversalRequest(request)

            CustomerCareResponse(
                success = true,
                message = "Transaction reversal request created successfully. Your request will be processed in 1-3 working days.",
                data = reversal,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        } catch (e: Exception) {
            CustomerCareResponse<TransactionReversalDto>(
                success = false,
                message = "Failed to create reversal request: ${e.message}",
                error = e.message,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }
    }

    fun getAllReversalRequests(page: Int = 1, pageSize: Int = 50): CustomerCareListResponse<TransactionReversalDto> {
        return try {
            val (reversals, total) = reversalRepository.getAllReversalRequests(page, pageSize)

            CustomerCareListResponse(
                success = true,
                message = "Reversal requests retrieved successfully",
                data = reversals,
                total = total,
                page = page,
                pageSize = pageSize,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        } catch (e: Exception) {
            CustomerCareListResponse(
                success = false,
                message = "Failed to retrieve reversal requests: ${e.message}",
                data = emptyList(),
                total = 0,
                page = 1,
                pageSize = 50,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }
    }

    fun getReversalRequestsByStatus(status: String, page: Int = 1, pageSize: Int = 50): CustomerCareListResponse<TransactionReversalDto> {
        return try {
            val (reversals, total) = reversalRepository.getReversalRequestsByStatus(status, page, pageSize)

            CustomerCareListResponse(
                success = true,
                message = "Reversal requests retrieved successfully",
                data = reversals,
                total = total,
                page = page,
                pageSize = pageSize,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        } catch (e: Exception) {
            CustomerCareListResponse(
                success = false,
                message = "Failed to retrieve reversal requests: ${e.message}",
                data = emptyList(),
                total = 0,
                page = 1,
                pageSize = 50,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }
    }

    fun getReversalRequestsByCustomerId(customerId: String): CustomerCareResponse<List<TransactionReversalDto>> {
        return try {
            val reversals = reversalRepository.getReversalRequestsByCustomerId(customerId)

            CustomerCareResponse(
                success = true,
                message = "Reversal requests retrieved successfully",
                data = reversals,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        } catch (e: Exception) {
            CustomerCareResponse<List<TransactionReversalDto>>(
                success = false,
                message = "Failed to retrieve reversal requests: ${e.message}",
                error = e.message,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }
    }

    fun reviewReversalRequest(review: ReviewTransactionReversalRequest, reviewerId: String): CustomerCareResponse<TransactionReversalDto> {
        return try {
            // Execute everything in a single transaction to avoid nested transaction issues
            val reversal = transaction {
                when (review.action.uppercase()) {
                    "APPROVE" -> {
                        // First, approve the reversal request
                        val now = java.time.Instant.now()

                        println("🔍 Reviewing Reversal Request: ${review.reversalId}")
                        println("  - Action: ${review.action}")
                        println("  - Override Type: ${review.reversalType}")

                        TransactionReversals.update({ TransactionReversals.id eq UUID.fromString(review.reversalId) }) {
                            it[status] = "APPROVED"
                            it[reviewedBy] = UUID.fromString(reviewerId)
                            it[TransactionReversals.reviewNotes] = review.reviewNotes
                            it[reviewedAt] = now
                            it[updatedAt] = now
                            if (review.reversalType != null) {
                                it[TransactionReversals.reversalType] = review.reversalType
                            }
                        }

                        // Get the updated reversal data
                        val reversalData = TransactionReversals
                            .select { TransactionReversals.id eq UUID.fromString(review.reversalId) }
                            .firstOrNull()
                            ?: throw IllegalStateException("Reversal request not found")

                        val reversalType = reversalData[TransactionReversals.reversalType]
                        println("  - Final Reversal Type: $reversalType")

                        // For REFUND, we take money from the receiver and give it back to the sender
                        // For SEND_TO_RECEIVER, it's already with the receiver, but if we held it, it means it's with the sender?
                        // Actually, SEND_TO_RECEIVER is usually when a transaction was failed but money was deducted.
                        
                        // Based on the requirement "reverver or sender balance does not change", 
                        // it seems both parties expect a change.
                        
                        // If reversalType is REFUND, we should hold money from the RECEIVER of the original transaction
                        val transactionId = reversalData[TransactionReversals.transactionId]
                        val originalTransaction = Transactions.select { Transactions.id eq transactionId }
                            .singleOrNull()
                            ?: throw IllegalStateException("Original transaction not found")

                        val originalSenderAccountId = originalTransaction[Transactions.accountId]
                        val originalReceiverAccountId = originalTransaction[Transactions.toAccountId]

                        println("  - Original Transaction Type: ${originalTransaction[Transactions.type]}")
                        println("  - Original Sender: $originalSenderAccountId")
                        println("  - Original Receiver: $originalReceiverAccountId")

                        val accountToHoldId = if (reversalType == "REFUND") {
                            // Take from receiver
                            originalReceiverAccountId
                                ?: reversalData[TransactionReversals.accountId] // Fallback to requester
                        } else {
                            // SEND_TO_RECEIVER: hold it from sender to ensure it can be sent.
                            originalSenderAccountId
                        }

                        println("  - Account to Hold Funds: $accountToHoldId")

                        val account = Accounts.select { Accounts.id eq accountToHoldId }
                            .singleOrNull()
                            ?: throw IllegalStateException("Account to hold funds not found")

                        val currentAvailable = account[Accounts.availableBalance]
                        val currentBalance = account[Accounts.balance]
                        val holdAmount = reversalData[TransactionReversals.amount]
                        
                        println("    - Current Balance: $currentBalance")
                        println("    - Current Available: $currentAvailable")
                        println("    - Amount to Hold: $holdAmount")

                        val newAvailable = currentAvailable - holdAmount
                        println("    - New Available (Calculated): $newAvailable")

                        if (newAvailable < java.math.BigDecimal.ZERO) {
                            throw IllegalStateException("Insufficient available balance in account ${account[Accounts.accountNumber]} to hold reversal amount")
                        }

                        Accounts.update({ Accounts.id eq accountToHoldId }) {
                            it[availableBalance] = newAvailable
                        }

                        // Update reversal request with the actual account we held funds from if it's different
                        if (accountToHoldId != reversalData[TransactionReversals.accountId]) {
                            println("  - Updating reversal request with correct account ID: $accountToHoldId")
                            TransactionReversals.update({ TransactionReversals.id eq reversalData[TransactionReversals.id] }) {
                                it[accountId] = accountToHoldId
                            }
                        }

                        // Map to DTO
                        TransactionReversalDto(
                            id = reversalData[TransactionReversals.id].toString(),
                            transactionId = reversalData[TransactionReversals.transactionId].toString(),
                            customerId = reversalData[TransactionReversals.customerId].toString(),
                            accountId = accountToHoldId.toString(),
                            amount = reversalData[TransactionReversals.amount].toString(),
                            reason = reversalData[TransactionReversals.reason],
                            requestedBy = reversalData[TransactionReversals.requestedBy].toString(),
                            status = reversalData[TransactionReversals.status],
                            reviewedBy = reversalData[TransactionReversals.reviewedBy]?.toString(),
                            reviewNotes = reversalData[TransactionReversals.reviewNotes],
                            rejectionReason = reversalData[TransactionReversals.rejectionReason],
                            requestedAt = reversalData[TransactionReversals.requestedAt].toString(),
                            reviewedAt = reversalData[TransactionReversals.reviewedAt]?.toString(),
                            completedAt = reversalData[TransactionReversals.completedAt]?.toString(),
                            estimatedCompletionDate = reversalData[TransactionReversals.estimatedCompletionDate]?.toString(),
                            onHoldUntil = reversalData[TransactionReversals.onHoldUntil]?.toString(),
                            reversalType = reversalData[TransactionReversals.reversalType],
                            createdAt = reversalData[TransactionReversals.createdAt].toString(),
                            updatedAt = reversalData[TransactionReversals.updatedAt].toString()
                        )
                    }
                    "REJECT" -> {
                        val now = java.time.Instant.now()

                        TransactionReversals.update({ TransactionReversals.id eq UUID.fromString(review.reversalId) }) {
                            it[status] = "REJECTED"
                            it[reviewedBy] = UUID.fromString(reviewerId)
                            it[TransactionReversals.rejectionReason] = review.rejectionReason ?: "No reason provided"
                            it[TransactionReversals.reviewNotes] = review.reviewNotes
                            it[reviewedAt] = now
                            it[updatedAt] = now
                        }

                        val reversalData = TransactionReversals
                            .select { TransactionReversals.id eq UUID.fromString(review.reversalId) }
                            .firstOrNull()
                            ?: throw IllegalStateException("Reversal request not found")

                        TransactionReversalDto(
                            id = reversalData[TransactionReversals.id].toString(),
                            transactionId = reversalData[TransactionReversals.transactionId].toString(),
                            customerId = reversalData[TransactionReversals.customerId].toString(),
                            accountId = reversalData[TransactionReversals.accountId].toString(),
                            amount = reversalData[TransactionReversals.amount].toString(),
                            reason = reversalData[TransactionReversals.reason],
                            requestedBy = reversalData[TransactionReversals.requestedBy].toString(),
                            status = reversalData[TransactionReversals.status],
                            reviewedBy = reversalData[TransactionReversals.reviewedBy]?.toString(),
                            reviewNotes = reversalData[TransactionReversals.reviewNotes],
                            rejectionReason = reversalData[TransactionReversals.rejectionReason],
                            requestedAt = reversalData[TransactionReversals.requestedAt].toString(),
                            reviewedAt = reversalData[TransactionReversals.reviewedAt]?.toString(),
                            completedAt = reversalData[TransactionReversals.completedAt]?.toString(),
                            estimatedCompletionDate = reversalData[TransactionReversals.estimatedCompletionDate]?.toString(),
                            onHoldUntil = reversalData[TransactionReversals.onHoldUntil]?.toString(),
                            reversalType = reversalData[TransactionReversals.reversalType],
                            createdAt = reversalData[TransactionReversals.createdAt].toString(),
                            updatedAt = reversalData[TransactionReversals.updatedAt].toString()
                        )
                    }
                    else -> throw IllegalArgumentException("Invalid action: ${review.action}. Must be APPROVE or REJECT")
                }
            }

            CustomerCareResponse(
                success = true,
                message = "Reversal request ${review.action.lowercase()}d successfully",
                data = reversal,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        } catch (e: Exception) {
            println("Error reviewing reversal request: ${e.message}")
            e.printStackTrace()
            CustomerCareResponse<TransactionReversalDto>(
                success = false,
                message = "Failed to review reversal request: ${e.message}",
                error = e.message,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }
    }

    fun completeReversalRequest(complete: CompleteTransactionReversalRequest, completedBy: String): CustomerCareResponse<TransactionReversalDto> {
        return try {
            val reversalData = reversalRepository.getReversalRequestById(complete.reversalId)
                ?: throw IllegalStateException("Reversal request not found")

            println("🚀 Completing Reversal Request: ${reversalData.id}")
            println("  - Type: ${reversalData.reversalType}")
            println("  - Amount: ${reversalData.amount}")
            println("  - Account ID from Reversal Data: ${reversalData.accountId}")

            if (reversalData.status != "APPROVED") {
                throw IllegalStateException("Only approved reversal requests can be completed")
            }

            // Process the reversal based on reversalType
            transaction {
                val accountId = UUID.fromString(reversalData.accountId)
                val transactionId = UUID.fromString(reversalData.transactionId)
                val amount = java.math.BigDecimal(reversalData.amount)

                val account = Accounts.select { Accounts.id eq accountId }
                    .singleOrNull()
                    ?: throw IllegalStateException("Account not found")

                val originalTransaction = Transactions.select { Transactions.id eq transactionId }
                    .singleOrNull()
                    ?: throw IllegalStateException("Original transaction not found")

                println("  - Original Transaction: ${originalTransaction[Transactions.id]}")
                println("  - Original Sender Account: ${originalTransaction[Transactions.accountId]}")
                println("  - Original Receiver Account: ${originalTransaction[Transactions.toAccountId]}")

                when (reversalData.reversalType) {
                    "REFUND" -> {
                        // In REFUND, we want to TAKE money from the ORIGINAL RECEIVER and give it back to the ORIGINAL SENDER.
                        
                        val senderAccountId = originalTransaction[Transactions.accountId]
                        val receiverAccountId = originalTransaction[Transactions.toAccountId]
                            ?: throw IllegalStateException("Original transaction has no receiver account")

                        // Safety Check: ensure we are deducting from the receiver
                        val deductionAccountId = if (accountId != receiverAccountId) {
                            println("  ⚠️ Reversal record account ID ($accountId) mismatch with original receiver ($receiverAccountId). Using original receiver.")
                            receiverAccountId
                        } else {
                            accountId
                        }

                        val deductionAccount = Accounts.select { Accounts.id eq deductionAccountId }
                            .singleOrNull()
                            ?: throw IllegalStateException("Receiver account not found")

                        val receiverBalance = deductionAccount[Accounts.balance]
                        val receiverAvailable = deductionAccount[Accounts.availableBalance]
                        
                        println("  - REFUND: Deducting from Receiver $deductionAccountId")
                        println("    - Current Receiver Balance: $receiverBalance")
                        println("    - Current Receiver Available: $receiverAvailable")

                        // Receiver: reduce balance AND availableBalance
                        val newReceiverBalance = receiverBalance - amount
                        val newReceiverAvailable = receiverAvailable - amount

                        println("    - New Receiver Balance: $newReceiverBalance")
                        println("    - New Receiver Available: $newReceiverAvailable")

                        Accounts.update({ Accounts.id eq deductionAccountId }) {
                            it[balance] = newReceiverBalance
                            it[availableBalance] = newReceiverAvailable
                        }

                        // Mark original transaction as reversed
                        Transactions.update({ Transactions.id eq transactionId }) {
                            it[status] = TransactionStatus.REVERSED
                        }

                        // Sender: increase balance and availableBalance
                        val senderAccount = Accounts.select { Accounts.id eq senderAccountId }
                            .singleOrNull()
                            ?: throw IllegalStateException("Sender account not found")
                        
                        val senderBalance = senderAccount[Accounts.balance]
                        val senderAvailable = senderAccount[Accounts.availableBalance]
                        val newSenderBalance = senderBalance + amount
                        val newSenderAvailable = senderAvailable + amount

                        println("  - REFUND: Crediting back to Sender $senderAccountId")
                        println("    - Current Sender Balance: $senderBalance")
                        println("    - New Sender Balance: $newSenderBalance")

                        Accounts.update({ Accounts.id eq senderAccountId }) {
                            it[balance] = newSenderBalance
                            it[availableBalance] = newSenderAvailable
                        }

                        // Create reversal transaction for receiver (Debit)
                        Transactions.insert {
                            it[id] = UUID.randomUUID()
                            it[Transactions.accountId] = deductionAccountId
                            it[type] = TransactionType.REVERSAL
                            it[Transactions.amount] = amount
                            it[description] = "Reversal (Refund) to Sender: ${reversalData.reason}"
                            it[balanceAfter] = newReceiverBalance
                            it[reference] = IdGenerator.generateTransactionId()
                            it[processedBy] = UUID.fromString(completedBy)
                            it[status] = TransactionStatus.COMPLETED
                        }

                        // Create reversal transaction for sender (Credit)
                        Transactions.insert {
                            it[id] = UUID.randomUUID()
                            it[Transactions.accountId] = senderAccountId
                            it[type] = TransactionType.REVERSAL
                            it[Transactions.amount] = amount
                            it[description] = "Refund from Reversal: ${reversalData.reason}"
                            it[balanceAfter] = newSenderBalance
                            it[reference] = IdGenerator.generateTransactionId()
                            it[processedBy] = UUID.fromString(completedBy)
                            it[status] = TransactionStatus.COMPLETED
                        }
                    }
                    "SEND_TO_RECEIVER" -> {
                        // In SEND_TO_RECEIVER, we want to TAKE money from the ORIGINAL SENDER and give it to the ORIGINAL RECEIVER.
                        
                        val senderAccountId = originalTransaction[Transactions.accountId]
                        val receiverAccountId = originalTransaction[Transactions.toAccountId]
                            ?: throw IllegalStateException("Original transaction has no receiver")

                        // Safety Check: ensure we are deducting from the sender
                        val deductionAccountId = if (accountId != senderAccountId) {
                            println("  ⚠️ Reversal record account ID ($accountId) mismatch with original sender ($senderAccountId). Using original sender.")
                            senderAccountId
                        } else {
                            accountId
                        }

                        val deductionAccount = Accounts.select { Accounts.id eq deductionAccountId }
                            .singleOrNull()
                            ?: throw IllegalStateException("Sender account not found")

                        val senderBalance = deductionAccount[Accounts.balance]
                        val senderAvailable = deductionAccount[Accounts.availableBalance]
                        
                        println("  - SEND_TO_RECEIVER: Deducting from Sender $deductionAccountId")
                        println("    - Current Sender Balance: $senderBalance")

                        // Sender: reduce balance (available was already reduced during approval)
                        val newSenderBalance = senderBalance - amount
                        val newSenderAvailable = senderAvailable

                        println("    - New Sender Balance: $newSenderBalance")

                        Accounts.update({ Accounts.id eq deductionAccountId }) {
                            it[balance] = newSenderBalance
                            it[availableBalance] = newSenderAvailable
                        }

                        // Mark original transaction as reversed
                        Transactions.update({ Transactions.id eq transactionId }) {
                            it[status] = TransactionStatus.REVERSED
                        }

                        // Receiver: increase balance and availableBalance
                        val receiverAccount = Accounts.select { Accounts.id eq receiverAccountId }
                            .singleOrNull()
                            ?: throw IllegalStateException("Receiver account not found")
                        
                        val receiverBalance = receiverAccount[Accounts.balance]
                        val receiverAvailable = receiverAccount[Accounts.availableBalance]
                        val newReceiverBalance = receiverBalance + amount
                        val newReceiverAvailable = receiverAvailable + amount

                        println("  - SEND_TO_RECEIVER: Crediting Receiver $receiverAccountId")
                        println("    - Current Receiver Balance: $receiverBalance")
                        println("    - New Receiver Balance: $newReceiverBalance")

                        Accounts.update({ Accounts.id eq receiverAccountId }) {
                            it[balance] = newReceiverBalance
                            it[availableBalance] = newReceiverAvailable
                        }

                        // Create transaction for receiver
                        Transactions.insert {
                            it[id] = UUID.randomUUID()
                            it[Transactions.accountId] = receiverAccountId
                            it[type] = TransactionType.TRANSFER
                            it[Transactions.amount] = amount
                            it[description] = "Reversal completion (Sent): ${reversalData.reason}"
                            it[balanceAfter] = newReceiverBalance
                            it[reference] = IdGenerator.generateTransactionId()
                            it[processedBy] = UUID.fromString(completedBy)
                            it[status] = TransactionStatus.COMPLETED
                        }

                        // Create debit transaction for sender
                        Transactions.insert {
                            it[id] = UUID.randomUUID()
                            it[Transactions.accountId] = deductionAccountId
                            it[type] = TransactionType.FEE_DEBIT
                            it[Transactions.amount] = amount
                            it[description] = "Reversal sent to receiver: ${reversalData.reason}"
                            it[balanceAfter] = newSenderBalance
                            it[reference] = IdGenerator.generateTransactionId()
                            it[processedBy] = UUID.fromString(completedBy)
                            it[status] = TransactionStatus.COMPLETED
                        }
                    }
                }
            }

            val completedReversal = reversalRepository.completeReversalRequest(
                reversalId = complete.reversalId,
                completionNotes = complete.completionNotes
            )

            println("✅ Reversal request completed successfully!")
            println("  - Reversal ID: ${completedReversal.id}")
            println("  - Status: ${completedReversal.status}")
            println("  - Amount: ${completedReversal.amount}")

            CustomerCareResponse(
                success = true,
                message = "Reversal request completed successfully",
                data = completedReversal,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        } catch (e: Exception) {
            println("❌ Error completing reversal request: ${e.message}")
            e.printStackTrace()
            CustomerCareResponse<TransactionReversalDto>(
                success = false,
                message = "Failed to complete reversal request: ${e.message}",
                error = e.message,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }
    }
}