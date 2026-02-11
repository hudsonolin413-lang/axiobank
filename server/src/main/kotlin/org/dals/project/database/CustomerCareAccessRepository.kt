package org.dals.project.database

import org.dals.project.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class CustomerCareAccessRepository {

    fun createAccessRequest(request: CreateCustomerCareAccessRequest): CustomerCareAccessRequestDto {
        return transaction {
            val requestId = UUID.randomUUID()
            val now = Instant.now()

            // Get customer details
            val customer = Customers.select { Customers.id eq UUID.fromString(request.customerId) }
                .firstOrNull() ?: throw IllegalArgumentException("Customer not found")

            val customerName = "${customer[Customers.firstName] ?: ""} ${customer[Customers.lastName] ?: ""}".trim()
            val customerEmail = customer[Customers.email] ?: ""
            val customerPhone = customer[Customers.phoneNumber] ?: ""

            CustomerCareAccessRequests.insert {
                it[id] = requestId
                it[customerId] = UUID.fromString(request.customerId)
                it[CustomerCareAccessRequests.customerName] = customerName
                it[CustomerCareAccessRequests.customerEmail] = customerEmail
                it[CustomerCareAccessRequests.customerPhone] = customerPhone
                it[requestType] = request.requestType
                it[reason] = request.reason
                it[requestedPermissions] = Json.encodeToString(request.requestedPermissions)
                it[status] = "PENDING"
                it[priority] = request.priority
                it[requestedBy] = UUID.fromString(request.customerId)
                it[createdAt] = now
                it[updatedAt] = now
            }

            mapToDto(CustomerCareAccessRequests.select { CustomerCareAccessRequests.id eq requestId }.first())
        }
    }

    fun getAllAccessRequests(page: Int = 1, pageSize: Int = 50): Pair<List<CustomerCareAccessRequestDto>, Int> {
        return transaction {
            val offset = (page - 1) * pageSize

            val requests = CustomerCareAccessRequests
                .selectAll()
                .orderBy(CustomerCareAccessRequests.createdAt, SortOrder.DESC)
                .limit(pageSize, offset.toLong())
                .map { mapToDto(it) }

            val total = CustomerCareAccessRequests.selectAll().count().toInt()

            Pair(requests, total)
        }
    }

    fun getAccessRequestsByStatus(status: String, page: Int = 1, pageSize: Int = 50): Pair<List<CustomerCareAccessRequestDto>, Int> {
        return transaction {
            val offset = (page - 1) * pageSize

            val requests = CustomerCareAccessRequests
                .select { CustomerCareAccessRequests.status eq status }
                .orderBy(CustomerCareAccessRequests.createdAt, SortOrder.DESC)
                .limit(pageSize, offset.toLong())
                .map { mapToDto(it) }

            val total = CustomerCareAccessRequests.select { CustomerCareAccessRequests.status eq status }.count().toInt()

            Pair(requests, total)
        }
    }

    fun getAccessRequestsByCustomerId(customerId: String): List<CustomerCareAccessRequestDto> {
        return transaction {
            CustomerCareAccessRequests
                .select { CustomerCareAccessRequests.customerId eq UUID.fromString(customerId) }
                .orderBy(CustomerCareAccessRequests.createdAt, SortOrder.DESC)
                .map { mapToDto(it) }
        }
    }

    fun getAccessRequestById(requestId: String): CustomerCareAccessRequestDto? {
        return transaction {
            CustomerCareAccessRequests
                .select { CustomerCareAccessRequests.id eq UUID.fromString(requestId) }
                .firstOrNull()
                ?.let { mapToDto(it) }
        }
    }

    fun approveAccessRequest(
        requestId: String,
        approvedBy: String,
        reviewNotes: String? = null,
        expiresAt: String? = null
    ): CustomerCareAccessRequestDto {
        return transaction {
            val now = Instant.now()
            val expirationTime = expiresAt?.let { Instant.parse(it) }

            CustomerCareAccessRequests.update({ CustomerCareAccessRequests.id eq UUID.fromString(requestId) }) {
                it[status] = "APPROVED"
                it[CustomerCareAccessRequests.approvedBy] = UUID.fromString(approvedBy)
                it[CustomerCareAccessRequests.reviewedBy] = UUID.fromString(approvedBy)
                it[approvedAt] = now
                it[CustomerCareAccessRequests.reviewNotes] = reviewNotes
                it[CustomerCareAccessRequests.expiresAt] = expirationTime
                it[updatedAt] = now
            }

            getAccessRequestById(requestId)
                ?: throw IllegalStateException("Failed to retrieve updated request")
        }
    }

    fun rejectAccessRequest(
        requestId: String,
        rejectedBy: String,
        rejectionReason: String,
        reviewNotes: String? = null
    ): CustomerCareAccessRequestDto {
        return transaction {
            val now = Instant.now()

            CustomerCareAccessRequests.update({ CustomerCareAccessRequests.id eq UUID.fromString(requestId) }) {
                it[status] = "REJECTED"
                it[CustomerCareAccessRequests.rejectedBy] = UUID.fromString(rejectedBy)
                it[CustomerCareAccessRequests.reviewedBy] = UUID.fromString(rejectedBy)
                it[rejectedAt] = now
                it[CustomerCareAccessRequests.rejectionReason] = rejectionReason
                it[CustomerCareAccessRequests.reviewNotes] = reviewNotes
                it[updatedAt] = now
            }

            getAccessRequestById(requestId)
                ?: throw IllegalStateException("Failed to retrieve updated request")
        }
    }

    fun revokeAccessRequest(
        requestId: String,
        revokedBy: String,
        revocationReason: String
    ): CustomerCareAccessRequestDto {
        return transaction {
            val now = Instant.now()

            CustomerCareAccessRequests.update({ CustomerCareAccessRequests.id eq UUID.fromString(requestId) }) {
                it[status] = "REVOKED"
                it[CustomerCareAccessRequests.revokedBy] = UUID.fromString(revokedBy)
                it[revokedAt] = now
                it[CustomerCareAccessRequests.revocationReason] = revocationReason
                it[updatedAt] = now
            }

            getAccessRequestById(requestId)
                ?: throw IllegalStateException("Failed to retrieve updated request")
        }
    }

    fun getActiveAccessRequestForCustomer(customerId: String): CustomerCareAccessRequestDto? {
        return transaction {
            val now = Instant.now()

            CustomerCareAccessRequests
                .select {
                    (CustomerCareAccessRequests.customerId eq UUID.fromString(customerId)) and
                    (CustomerCareAccessRequests.status eq "APPROVED") and
                    ((CustomerCareAccessRequests.expiresAt.isNull()) or (CustomerCareAccessRequests.expiresAt greater now))
                }
                .orderBy(CustomerCareAccessRequests.approvedAt, SortOrder.DESC)
                .firstOrNull()
                ?.let { mapToDto(it) }
        }
    }

    fun checkExpiredAccessRequests(): List<CustomerCareAccessRequestDto> {
        return transaction {
            val now = Instant.now()

            val expiredRequests = CustomerCareAccessRequests
                .select {
                    (CustomerCareAccessRequests.status eq "APPROVED") and
                    (CustomerCareAccessRequests.expiresAt.isNotNull()) and
                    (CustomerCareAccessRequests.expiresAt less now)
                }
                .map { mapToDto(it) }

            // Auto-revoke expired requests
            CustomerCareAccessRequests.update({
                (CustomerCareAccessRequests.status eq "APPROVED") and
                (CustomerCareAccessRequests.expiresAt.isNotNull()) and
                (CustomerCareAccessRequests.expiresAt less now)
            }) {
                it[status] = "REVOKED"
                it[revocationReason] = "Access expired automatically"
                it[revokedAt] = now
                it[updatedAt] = now
            }

            expiredRequests
        }
    }

    private fun mapToDto(row: ResultRow): CustomerCareAccessRequestDto {
        val permissions = try {
            Json.decodeFromString<List<String>>(row[CustomerCareAccessRequests.requestedPermissions])
        } catch (e: Exception) {
            emptyList()
        }

        return CustomerCareAccessRequestDto(
            id = row[CustomerCareAccessRequests.id].toString(),
            customerId = row[CustomerCareAccessRequests.customerId].toString(),
            customerName = row[CustomerCareAccessRequests.customerName],
            customerEmail = row[CustomerCareAccessRequests.customerEmail],
            customerPhone = row[CustomerCareAccessRequests.customerPhone],
            requestType = row[CustomerCareAccessRequests.requestType],
            reason = row[CustomerCareAccessRequests.reason],
            requestedPermissions = permissions,
            status = row[CustomerCareAccessRequests.status],
            priority = row[CustomerCareAccessRequests.priority],
            requestedBy = row[CustomerCareAccessRequests.requestedBy].toString(),
            reviewedBy = row[CustomerCareAccessRequests.reviewedBy]?.toString(),
            approvedBy = row[CustomerCareAccessRequests.approvedBy]?.toString(),
            rejectedBy = row[CustomerCareAccessRequests.rejectedBy]?.toString(),
            revokedBy = row[CustomerCareAccessRequests.revokedBy]?.toString(),
            reviewNotes = row[CustomerCareAccessRequests.reviewNotes],
            rejectionReason = row[CustomerCareAccessRequests.rejectionReason],
            revocationReason = row[CustomerCareAccessRequests.revocationReason],
            approvedAt = row[CustomerCareAccessRequests.approvedAt]?.toString(),
            rejectedAt = row[CustomerCareAccessRequests.rejectedAt]?.toString(),
            revokedAt = row[CustomerCareAccessRequests.revokedAt]?.toString(),
            expiresAt = row[CustomerCareAccessRequests.expiresAt]?.toString(),
            createdAt = row[CustomerCareAccessRequests.createdAt].toString(),
            updatedAt = row[CustomerCareAccessRequests.updatedAt].toString()
        )
    }
}
