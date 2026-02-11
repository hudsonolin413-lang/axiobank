package org.dals.project.services

import org.dals.project.database.*
import org.dals.project.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class AdminWorkflowService {

    /**
     * Get all workflow approvals from different sources
     */
    fun getWorkflowApprovals(): ListResponse<WorkflowApprovalDto> {
        return try {
            transaction {
                val approvals = mutableListOf<WorkflowApprovalDto>()

                // 1. Pending loan applications
                val pendingLoanApps = LoanApplications
                    .join(Customers, JoinType.INNER, LoanApplications.customerId, Customers.id)
                    .select {
                        (LoanApplications.status eq LoanStatus.APPLIED) or
                                (LoanApplications.status eq LoanStatus.UNDER_REVIEW)
                    }
                    .orderBy(LoanApplications.applicationDate to SortOrder.DESC)
                    .limit(50)

                pendingLoanApps.forEach { row ->
                    approvals.add(
                        WorkflowApprovalDto(
                            id = "LOAN_${row[LoanApplications.id]}",
                            workflowType = "LOAN_APPLICATION",
                            entityId = row[LoanApplications.id].toString(),
                            requesterId = row[LoanApplications.customerId].toString(),
                            requesterName = "${row[Customers.firstName]} ${row[Customers.lastName]}",
                            status = when (row[LoanApplications.status]) {
                                LoanStatus.APPLIED -> "PENDING"
                                LoanStatus.UNDER_REVIEW -> "IN_PROGRESS"
                                LoanStatus.APPROVED -> "APPROVED"
                                LoanStatus.REJECTED -> "REJECTED"
                                else -> "PENDING"
                            },
                            priority = when {
                                row[LoanApplications.requestedAmount] > BigDecimal("100000") -> "HIGH"
                                row[LoanApplications.requestedAmount] > BigDecimal("50000") -> "NORMAL"
                                else -> "LOW"
                            },
                            createdAt = row[LoanApplications.applicationDate].toString(),
                            deadline = row[LoanApplications.applicationDate].plusDays(7).toString()
                        )
                    )
                }

                // 2. Pending account opening requests
                val accountOpeningRequests = ServiceRequests
                    .join(Customers, JoinType.INNER, ServiceRequests.customerId, Customers.id)
                    .select {
                        (ServiceRequests.requestType eq "ACCOUNT_OPENING") and
                                (ServiceRequests.status eq "PENDING")
                    }
                    .orderBy(ServiceRequests.createdAt to SortOrder.DESC)
                    .limit(25)

                accountOpeningRequests.forEach { row ->
                    approvals.add(
                        WorkflowApprovalDto(
                            id = "ACCT_${row[ServiceRequests.id]}",
                            workflowType = "CUSTOMER_ACCOUNT_OPENING",
                            entityId = row[ServiceRequests.id].toString(),
                            requesterId = row[ServiceRequests.createdBy].toString(),
                            requesterName = "${row[Customers.firstName]} ${row[Customers.lastName]}",
                            status = when (row[ServiceRequests.status]) {
                                "PENDING" -> "PENDING"
                                "IN_PROGRESS" -> "IN_PROGRESS"
                                "COMPLETED" -> "APPROVED"
                                "REJECTED" -> "REJECTED"
                                else -> "PENDING"
                            },
                            priority = when (row[ServiceRequests.priority]) {
                                "URGENT" -> "HIGH"
                                "HIGH" -> "HIGH"
                                "MEDIUM" -> "NORMAL"
                                else -> "LOW"
                            },
                            createdAt = row[ServiceRequests.createdAt].toString(),
                            deadline = row[ServiceRequests.createdAt].atZone(ZoneId.systemDefault())
                                .toLocalDateTime().plusDays(3).toString()
                        )
                    )
                }

                // 3. Large transactions requiring approval
                val largeTransactions = Transactions
                    .join(Accounts, JoinType.INNER, Transactions.accountId, Accounts.id)
                    .join(Customers, JoinType.INNER, Accounts.customerId, Customers.id)
                    .select {
                        (Transactions.amount greater BigDecimal("50000")) and
                                (Transactions.status eq TransactionStatus.PENDING) and
                                (Transactions.timestamp greater LocalDateTime.now().minusDays(30)
                                    .atZone(ZoneId.systemDefault()).toInstant())
                    }
                    .orderBy(Transactions.timestamp to SortOrder.DESC)
                    .limit(20)

                largeTransactions.forEach { row ->
                    approvals.add(
                        WorkflowApprovalDto(
                            id = "TXN_${row[Transactions.id]}",
                            workflowType = "LARGE_TRANSACTION",
                            entityId = row[Transactions.id].toString(),
                            requesterId = row[Transactions.processedBy]?.toString() ?: "SYSTEM",
                            requesterName = "${row[Customers.firstName]} ${row[Customers.lastName]}",
                            status = "PENDING",
                            priority = when {
                                row[Transactions.amount] > BigDecimal("250000") -> "CRITICAL"
                                row[Transactions.amount] > BigDecimal("100000") -> "HIGH"
                                else -> "NORMAL"
                            },
                            createdAt = row[Transactions.timestamp].toString(),
                            deadline = row[Transactions.timestamp].atZone(ZoneId.systemDefault())
                                .toLocalDateTime().plusHours(4).toString()
                        )
                    )
                }

                // 4. Account freeze requests
                val freezeApprovals = AccountFreezeRequests
                    .join(Customers, JoinType.INNER, AccountFreezeRequests.customerId, Customers.id)
                    .select { AccountFreezeRequests.status eq "PENDING" }
                    .orderBy(AccountFreezeRequests.requestedAt to SortOrder.DESC)
                    .limit(25)

                freezeApprovals.forEach { row ->
                    approvals.add(
                        WorkflowApprovalDto(
                            id = "FRZA_${row[AccountFreezeRequests.id]}",
                            workflowType = "ACCOUNT_FREEZE_APPROVAL",
                            entityId = row[AccountFreezeRequests.accountId].toString(),
                            requesterId = row[AccountFreezeRequests.requestedBy].toString(),
                            requesterName = "${row[Customers.firstName]} ${row[Customers.lastName]}",
                            status = "PENDING",
                            priority = when (row[AccountFreezeRequests.freezeType]) {
                                "FRAUD" -> "CRITICAL"
                                "SUSPICIOUS_ACTIVITY" -> "HIGH"
                                "LEGAL" -> "HIGH"
                                else -> "NORMAL"
                            },
                            createdAt = row[AccountFreezeRequests.requestedAt].toString(),
                            deadline = row[AccountFreezeRequests.requestedAt].atZone(ZoneId.systemDefault())
                                .toLocalDateTime().plusHours(24).toString()
                        )
                    )
                }

                val sortedApprovals = approvals.sortedWith(
                    compareByDescending<WorkflowApprovalDto> {
                        when (it.priority) {
                            "CRITICAL" -> 4
                            "HIGH" -> 3
                            "NORMAL" -> 2
                            else -> 1
                        }
                    }.thenByDescending { it.createdAt }
                )

                ListResponse(
                    success = true,
                    message = "Workflow approvals retrieved successfully",
                    data = sortedApprovals,
                    total = sortedApprovals.size
                )
            }
        } catch (e: Exception) {
            println("Error retrieving workflow approvals: ${e.message}")
            e.printStackTrace()
            ListResponse(success = false, message = "Error retrieving workflow approvals: ${e.message}")
        }
    }

    /**
     * Get workflow approvals filtered by status
     */
    fun getWorkflowApprovalsByStatus(status: String?): ListResponse<WorkflowApprovalDto> {
        return try {
            val allApprovalsResponse = getWorkflowApprovals()

            if (!allApprovalsResponse.success) {
                return allApprovalsResponse
            }

            val filteredApprovals = if (status != null) {
                allApprovalsResponse.data?.filter { it.status.equals(status, ignoreCase = true) } ?: emptyList()
            } else {
                allApprovalsResponse.data ?: emptyList()
            }

            ListResponse(
                success = true,
                message = "Workflow approvals retrieved and filtered",
                data = filteredApprovals,
                total = filteredApprovals.size
            )
        } catch (e: Exception) {
            ListResponse(success = false, message = "Error retrieving workflow approvals: ${e.message}")
        }
    }

    /**
     * Process workflow approval (approve/reject)
     */
    fun processWorkflowApproval(approvalId: String, action: String, comments: String): ApiResponse<String> {
        return try {
            transaction {
                when {
                    approvalId.startsWith("LOAN_") -> {
                        val loanAppId = UUID.fromString(approvalId.removePrefix("LOAN_"))
                        val newStatus = when (action.uppercase()) {
                            "APPROVED" -> LoanStatus.APPROVED
                            "REJECTED" -> LoanStatus.REJECTED
                            else -> LoanStatus.UNDER_REVIEW
                        }

                        LoanApplications.update({ LoanApplications.id eq loanAppId }) {
                            it[LoanApplications.status] = newStatus
                            it[LoanApplications.reviewedBy] = UUID.randomUUID()
                            it[LoanApplications.comments] = comments
                            it[LoanApplications.reviewedDate] = java.time.LocalDate.now()
                            it[LoanApplications.updatedAt] = java.time.Instant.now()
                        }
                    }

                    approvalId.startsWith("ACCT_") -> {
                        val serviceRequestId = UUID.fromString(approvalId.removePrefix("ACCT_"))
                        val newStatus = when (action.uppercase()) {
                            "APPROVED" -> "COMPLETED"
                            "REJECTED" -> "REJECTED"
                            else -> "IN_PROGRESS"
                        }

                        ServiceRequests.update({ ServiceRequests.id eq serviceRequestId }) {
                            it[ServiceRequests.status] = newStatus
                            it[ServiceRequests.completedBy] = UUID.randomUUID()
                            if (newStatus == "COMPLETED") it[ServiceRequests.completedAt] = java.time.Instant.now()
                            if (newStatus == "REJECTED") it[ServiceRequests.rejectionReason] = comments
                            it[ServiceRequests.updatedAt] = java.time.Instant.now()
                        }
                    }

                    approvalId.startsWith("TXN_") -> {
                        val transactionId = UUID.fromString(approvalId.removePrefix("TXN_"))
                        val newStatus = when (action.uppercase()) {
                            "APPROVED" -> TransactionStatus.COMPLETED
                            "REJECTED" -> TransactionStatus.FAILED
                            else -> TransactionStatus.PENDING
                        }

                        Transactions.update({ Transactions.id eq transactionId }) {
                            it[Transactions.status] = newStatus
                            it[Transactions.processedBy] = UUID.randomUUID()
                        }
                    }

                    approvalId.startsWith("FRZA_") -> {
                        val freezeRequestId = UUID.fromString(approvalId.removePrefix("FRZA_"))
                        val newStatus = when (action.uppercase()) {
                            "APPROVED" -> "APPROVED"
                            "REJECTED" -> "REJECTED"
                            else -> "PENDING"
                        }

                        AccountFreezeRequests.update({ AccountFreezeRequests.id eq freezeRequestId }) {
                            it[AccountFreezeRequests.status] = newStatus
                            it[AccountFreezeRequests.approvedBy] = UUID.randomUUID()
                            it[AccountFreezeRequests.approvedAt] = java.time.Instant.now()
                            it[AccountFreezeRequests.updatedAt] = java.time.Instant.now()
                        }
                    }
                }

                ApiResponse(
                    success = true,
                    message = "Workflow approval processed successfully",
                    data = "Approval $approvalId processed with action: $action"
                )
            }
        } catch (e: Exception) {
            println("Error processing workflow approval: ${e.message}")
            e.printStackTrace()
            ApiResponse(success = false, message = "Error processing workflow approval: ${e.message}")
        }
    }
}
