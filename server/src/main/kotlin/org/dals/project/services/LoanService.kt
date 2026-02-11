package org.dals.project.services

import org.dals.project.database.*
import org.dals.project.models.*
import org.dals.project.utils.IdGenerator
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

class LoanService {
    private val notificationService = NotificationService()

    // Loan Applications
    suspend fun getAllLoanApplications(page: Int = 1, pageSize: Int = 10): CustomerCareListResponse<LoanApplicationDto> {
        return DatabaseFactory.dbQuery {
            val offset = (page - 1) * pageSize
            val applications = LoanApplications.selectAll()
                .orderBy(LoanApplications.applicationDate, SortOrder.DESC)
                .limit(pageSize, offset.toLong())
                .map { toLoanApplicationDto(it) }

            val total = LoanApplications.selectAll().count().toInt()

            CustomerCareListResponse(
                success = true,
                message = "Loan applications retrieved successfully",
                data = applications,
                total = total,
                page = page,
                pageSize = pageSize,
                timestamp = java.time.Instant.now().toString()
            )
        }
    }

    suspend fun getLoanApplicationById(id: UUID): ApiResponse<LoanApplicationDto> {
        return DatabaseFactory.dbQuery {
            val application = LoanApplications.select { LoanApplications.id eq id }
                .singleOrNull()
                ?.let { toLoanApplicationDto(it) }

            if (application != null) {
                ApiResponse(
                    success = true,
                    message = "Loan application found",
                    data = application
                )
            } else {
                ApiResponse(
                    success = false,
                    message = "Loan application not found",
                    error = "No loan application found with ID: $id"
                )
            }
        }
    }

    suspend fun getLoanApplicationsByCustomerId(customerId: UUID): CustomerCareListResponse<LoanApplicationDto> {
        return DatabaseFactory.dbQuery {
            val applications = LoanApplications.select { LoanApplications.customerId eq customerId }
                .orderBy(LoanApplications.applicationDate, SortOrder.DESC)
                .map { toLoanApplicationDto(it) }

            CustomerCareListResponse(
                success = true,
                message = "Customer loan applications retrieved successfully",
                data = applications,
                total = applications.size,
                page = 1,
                pageSize = applications.size,
                timestamp = java.time.Instant.now().toString()
            )
        }
    }

    suspend fun createLoanApplication(request: CreateLoanApplicationRequest): ApiResponse<LoanApplicationDto> {
        return DatabaseFactory.dbQuery {
            try {
                val applicationId = UUID.randomUUID()

                val insertedId = LoanApplications.insert {
                    it[id] = applicationId
                    it[customerId] = UUID.fromString(request.customerId)
                    it[loanType] = LoanType.valueOf(request.loanType.uppercase())
                    it[requestedAmount] = BigDecimal(request.requestedAmount)
                    it[purpose] = request.purpose
                    it[collateralDescription] = request.collateralDescription
                    it[collateralValue] = request.collateralValue?.let { BigDecimal(it) }
                    it[creditScore] = request.creditScore
                    // it[debtToIncomeRatio] = request.debtToIncomeRatio?.let { BigDecimal(it) }
                    it[annualIncome] = request.annualIncome?.let { BigDecimal(it) }
                    it[employmentHistory] = request.employmentHistory
                }[LoanApplications.id]

                val createdApplication = LoanApplications.select { LoanApplications.id eq applicationId }
                    .single()
                    .let { toLoanApplicationDto(it) }

                ApiResponse(
                    success = true,
                    message = "Loan application created successfully",
                    data = createdApplication
                )
            } catch (e: Exception) {
                ApiResponse(
                    success = false,
                    message = "Failed to create loan application",
                    error = e.message
                )
            }
        }
    }

    suspend fun updateLoanApplicationStatus(
        id: UUID,
        status: String,
        reviewedBy: UUID? = null,
        approvedAmount: String? = null,
        interestRate: String? = null,
        termMonths: Int? = null,
        comments: String? = null
    ): ApiResponse<LoanApplicationDto> {
        return DatabaseFactory.dbQuery {
            try {
//                println("Updating loan application in database: $id")
//                println("Status: $status, ApprovedAmount: $approvedAmount, InterestRate: $interestRate, TermMonths: $termMonths")
//                println("ReviewedBy: $reviewedBy, Comments: $comments")

                // Get the application first
                val application = LoanApplications.select { LoanApplications.id eq id }
                    .singleOrNull()
                    ?: return@dbQuery ApiResponse(
                        success = false,
                        message = "Loan application not found",
                        error = "No loan application found with ID: $id"
                    )

                val updated = LoanApplications.update({ LoanApplications.id eq id }) {
                    it[LoanApplications.status] = LoanStatus.valueOf(status.uppercase())
                    if (reviewedBy != null) {
                        it[LoanApplications.reviewedBy] = reviewedBy
                        it[reviewedDate] = LocalDate.now()
                    }
                    if (approvedAmount != null) {
                        it[LoanApplications.approvedAmount] = BigDecimal(approvedAmount)
                    }
                    if (interestRate != null) {
                        it[LoanApplications.interestRate] = BigDecimal(interestRate)
                    }
                    if (termMonths != null) {
                        it[LoanApplications.termMonths] = termMonths
                    }
                    if (comments != null) {
                        it[LoanApplications.comments] = comments
                    }
                }

//                println("Rows updated: $updated")

                // If approved, create loan and disburse funds
//                println("🔍 Checking approval conditions:")
//                println("   Status: $status (uppercase: ${status.uppercase()})")
//                println("   Approved Amount: $approvedAmount")
//                println("   Interest Rate: $interestRate")
//                println("   Term Months: $termMonths")
//                println("   Condition met: ${status.uppercase() == "APPROVED" && approvedAmount != null && interestRate != null && termMonths != null}")
                
                if (status.uppercase() == "APPROVED" && approvedAmount != null && interestRate != null && termMonths != null) {
//                    println("💰 Loan approved! Creating loan record and disbursing funds...")
                    
                    val customerId = application[LoanApplications.customerId]
                    val loanAmount = BigDecimal(approvedAmount)
                    val rate = BigDecimal(interestRate)
                    val term = termMonths
                    
                    // Calculate monthly payment (simple interest calculation)
                    val totalInterest = loanAmount.multiply(rate.divide(BigDecimal(100))).multiply(BigDecimal(term).divide(BigDecimal(12)))
                    val totalAmount = loanAmount.add(totalInterest)
                    val monthlyPayment = totalAmount.divide(BigDecimal(term), 2, BigDecimal.ROUND_HALF_UP)
                    
                    // Create loan record
                    val loanId = UUID.randomUUID()
                    
                    // Get customer's account
                    val customerAccount = Accounts.select { Accounts.customerId eq customerId }
                        .orderBy(Accounts.createdAt to SortOrder.DESC)
                        .limit(1)
                        .singleOrNull()
                    
                    if (customerAccount != null) {
                        val accountId = customerAccount[Accounts.id].value
                        
                        // Get customer's branch and loan officer
                        val customer = Customers.select { Customers.id eq customerId }.singleOrNull()
                        val customerBranchId = customer?.get(Customers.branchId) ?: customerAccount[Accounts.branchId]
                        val loanOfficerId = reviewedBy ?: UUID.randomUUID()
                        
                        // Create loan
                        Loans.insert {
                            it[Loans.id] = loanId
                            it[Loans.customerId] = customerId
                            it[Loans.accountId] = accountId
                            it[applicationId] = id  // Link to loan application
                            it[loanType] = application[LoanApplications.loanType]
                            it[originalAmount] = loanAmount
                            it[Loans.interestRate] = rate
                            it[Loans.termMonths] = term
                            it[Loans.monthlyPayment] = monthlyPayment
                            it[currentBalance] = totalAmount
                            it[Loans.status] = LoanStatus.ACTIVE
                            it[originationDate] = LocalDate.now()
                            it[maturityDate] = LocalDate.now().plusMonths(term.toLong())
                            it[nextPaymentDate] = LocalDate.now().plusMonths(1)
                            it[paymentsRemaining] = term
                            it[Loans.loanOfficerId] = loanOfficerId
                            it[Loans.branchId] = customerBranchId
                        }
                        
                        println("✅ Loan created with ID: $loanId")
                        
                        // Credit customer's account
                        val currentBalance = customerAccount[Accounts.balance]
                        val newBalance = currentBalance.add(loanAmount)
                        
                        Accounts.update({ Accounts.id eq accountId }) {
                            it[balance] = newBalance
                            it[availableBalance] = newBalance
                        }
                        
                        println("💳 Credited customer account: $loanAmount. New balance: $newBalance")
                        
                        // Create transaction record
                        Transactions.insert {
                            it[Transactions.id] = UUID.randomUUID()
                            it[Transactions.accountId] = accountId
                            it[Transactions.type] = TransactionType.DIRECT_DEPOSIT
                            it[Transactions.amount] = loanAmount
                            it[Transactions.description] = "Loan disbursement - ${application[LoanApplications.loanType].name}"
                            it[Transactions.balanceAfter] = newBalance
                            it[Transactions.reference] = IdGenerator.generateTransactionId()
                            it[Transactions.status] = TransactionStatus.COMPLETED
                            if (reviewedBy != null) {
                                it[Transactions.processedBy] = reviewedBy
                            }
                            it[Transactions.branchId] = customerBranchId
                        }
                        
                        println("📝 Transaction record created")

                        // Send notification for loan approval
                        try {
                            notificationService.notifyLoanApproved(
                                customerId = customerId,
                                loanId = loanId,
                                amount = approvedAmount
                            )
                            println("📬 Loan approval notification sent")
                        } catch (e: Exception) {
                            println("⚠️ Failed to send notification: ${e.message}")
                        }
                    } else {
                        println("⚠️ Warning: No account found for customer. Loan created but funds not disbursed.")
                    }
                } else if (status.uppercase() == "REJECTED") {
                    // Send notification for loan rejection
                    try {
                        val customerId = application[LoanApplications.customerId]
                        val rejectionReason = comments ?: "Application did not meet approval criteria"
                        notificationService.notifyLoanRejected(
                            customerId = customerId,
                            loanId = id,
                            reason = rejectionReason
                        )
                        println("📬 Loan rejection notification sent")
                    } catch (e: Exception) {
                        println("⚠️ Failed to send notification: ${e.message}")
                    }
                }

                if (updated > 0) {
                    val updatedApplication = LoanApplications.select { LoanApplications.id eq id }
                        .single()
                        .let { toLoanApplicationDto(it) }

                    ApiResponse(
                        success = true,
                        message = "Loan application updated successfully",
                        data = updatedApplication
                    )
                } else {
                    ApiResponse(
                        success = false,
                        message = "Loan application not found",
                        error = "No loan application found with ID: $id"
                    )
                }
            } catch (e: Exception) {
                println("❌ Error updating loan application: ${e.message}")
                e.printStackTrace()
                ApiResponse(
                    success = false,
                    message = "Failed to update loan application",
                    error = e.message
                )
            }
        }
    }

    // Loans
    suspend fun getAllLoans(page: Int = 1, pageSize: Int = 10): CustomerCareListResponse<LoanDto> {
        return DatabaseFactory.dbQuery {
            val offset = (page - 1) * pageSize
            val loans = Loans.selectAll()
                .orderBy(Loans.originationDate, SortOrder.DESC)
                .limit(pageSize, offset.toLong())
                .map { toLoanDto(it) }

            val total = Loans.selectAll().count().toInt()

            CustomerCareListResponse(
                success = true,
                message = "Loans retrieved successfully",
                data = loans,
                total = total,
                page = page,
                pageSize = pageSize,
                timestamp = java.time.Instant.now().toString()
            )
        }
    }

    suspend fun getLoanById(id: UUID): ApiResponse<LoanDto> {
        return DatabaseFactory.dbQuery {
            val loan = Loans.select { Loans.id eq id }
                .singleOrNull()
                ?.let { toLoanDto(it) }

            if (loan != null) {
                ApiResponse(
                    success = true,
                    message = "Loan found",
                    data = loan
                )
            } else {
                ApiResponse(
                    success = false,
                    message = "Loan not found",
                    error = "No loan found with ID: $id"
                )
            }
        }
    }

    suspend fun getLoansByCustomerId(customerId: UUID): CustomerCareListResponse<LoanDto> {
        return DatabaseFactory.dbQuery {
            val loans = Loans.select { Loans.customerId eq customerId }
                .orderBy(Loans.originationDate, SortOrder.DESC)
                .map { toLoanDto(it) }

            CustomerCareListResponse(
                success = true,
                message = "Customer loans retrieved successfully",
                data = loans,
                total = loans.size,
                page = 1,
                pageSize = loans.size,
                timestamp = java.time.Instant.now().toString()
            )
        }
    }

    suspend fun createLoanFromApplication(
        applicationId: UUID,
        loanOfficerId: UUID,
        branchId: UUID
    ): ApiResponse<LoanDto> {
        return DatabaseFactory.dbQuery {
            try {
                // Get the approved application
                val application = LoanApplications.select {
                    (LoanApplications.id eq applicationId) and
                            (LoanApplications.status eq LoanStatus.APPROVED)
                }.singleOrNull() ?: throw Exception("Approved loan application not found")

                val loanId = UUID.randomUUID()
                val approvedAmount = application[LoanApplications.approvedAmount]
                    ?: throw Exception("Approved amount is required")
                val interestRate = application[LoanApplications.interestRate]
                    ?: throw Exception("Interest rate is required")
                val termMonths = application[LoanApplications.termMonths]
                    ?: throw Exception("Term in months is required")

                // Calculate monthly payment (simple calculation)
                val monthlyInterestRate = interestRate.divide(BigDecimal(12), 6, BigDecimal.ROUND_HALF_UP)
                val monthlyPayment = calculateMonthlyPayment(approvedAmount, monthlyInterestRate, termMonths)

                val originationDate = LocalDate.now()
                val maturityDate = originationDate.plusMonths(termMonths.toLong())
                val nextPaymentDate = originationDate.plusMonths(1)

                // Create the loan
                val insertedId = Loans.insert {
                    it[id] = loanId
                    it[customerId] = application[LoanApplications.customerId]
                    it[Loans.applicationId] = applicationId
                    it[loanType] = application[LoanApplications.loanType]
                    it[originalAmount] = approvedAmount
                    it[currentBalance] = approvedAmount
                    it[Loans.interestRate] = interestRate
                    it[Loans.termMonths] = termMonths
                    it[Loans.monthlyPayment] = monthlyPayment
                    it[Loans.originationDate] = originationDate
                    it[Loans.maturityDate] = maturityDate
                    it[Loans.nextPaymentDate] = nextPaymentDate
                    it[paymentsRemaining] = termMonths
                    it[collateralDescription] = application[LoanApplications.collateralDescription]
                    it[collateralValue] = application[LoanApplications.collateralValue]
                    it[Loans.loanOfficerId] = loanOfficerId
                    it[Loans.branchId] = branchId
                }[Loans.id]

                // Update loan application status to ACTIVE
                LoanApplications.update({ LoanApplications.id eq applicationId }) {
                    it[status] = LoanStatus.ACTIVE
                }

                val createdLoan = Loans.select { Loans.id eq loanId }
                    .single()
                    .let { toLoanDto(it) }

                ApiResponse(
                    success = true,
                    message = "Loan created successfully",
                    data = createdLoan
                )
            } catch (e: Exception) {
                ApiResponse(
                    success = false,
                    message = "Failed to create loan",
                    error = e.message
                )
            }
        }
    }

    private fun calculateMonthlyPayment(
        principal: BigDecimal,
        monthlyRate: BigDecimal,
        termMonths: Int
    ): BigDecimal {
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal(termMonths), 2, BigDecimal.ROUND_HALF_UP)
        }

        val onePlusRate = BigDecimal.ONE.add(monthlyRate)
        val factor = onePlusRate.pow(termMonths)
        val numerator = principal.multiply(monthlyRate).multiply(factor)
        val denominator = factor.subtract(BigDecimal.ONE)

        return numerator.divide(denominator, 2, BigDecimal.ROUND_HALF_UP)
    }

    private fun toLoanApplicationDto(row: ResultRow): LoanApplicationDto {
        return LoanApplicationDto(
            id = row[LoanApplications.id].toString(),
            customerId = row[LoanApplications.customerId].toString(),
            loanType = row[LoanApplications.loanType].name,
            requestedAmount = row[LoanApplications.requestedAmount].toString(),
            purpose = row[LoanApplications.purpose],
            status = row[LoanApplications.status].name,
            applicationDate = row[LoanApplications.applicationDate].toString(),
            reviewedBy = row[LoanApplications.reviewedBy]?.toString(),
            reviewedDate = row[LoanApplications.reviewedDate]?.toString(),
            approvedAmount = row[LoanApplications.approvedAmount]?.toString(),
            interestRate = row[LoanApplications.interestRate]?.toString(),
            termMonths = row[LoanApplications.termMonths],
            collateralDescription = row[LoanApplications.collateralDescription],
            collateralValue = row[LoanApplications.collateralValue]?.toString(),
            creditScore = row[LoanApplications.creditScore],
            // debtToIncomeRatio = row[LoanApplications.debtToIncomeRatio]?.toString(),
            annualIncome = row[LoanApplications.annualIncome]?.toString(),
            employmentHistory = row[LoanApplications.employmentHistory],
            comments = row[LoanApplications.comments],
            createdAt = row[LoanApplications.createdAt].toString(),
            updatedAt = row[LoanApplications.updatedAt].toString()
        )
    }

    suspend fun makeLoanPayment(request: MakeLoanPaymentRequest): ApiResponse<LoanPaymentDto> {
        return DatabaseFactory.dbQuery {
            try {
                val loanId = UUID.fromString(request.loanId)
                val paymentAmount = BigDecimal(request.amount)

                // Get the loan
                val loanRow = Loans.select { Loans.id eq loanId }
                    .singleOrNull()
                    ?: throw Exception("Loan not found")

                // Validate loan is active
                if (loanRow[Loans.status] != LoanStatus.ACTIVE) {
                    throw Exception("Cannot make payment on loan with status: ${loanRow[Loans.status].name}")
                }

                val currentBalance = loanRow[Loans.currentBalance]
                val customerId = loanRow[Loans.customerId]
                val accountId = loanRow[Loans.accountId]
                val originalAmount = loanRow[Loans.originalAmount]
                val interestRate = loanRow[Loans.interestRate]
                val termMonths = loanRow[Loans.termMonths]

                // Validate payment amount
                if (paymentAmount <= BigDecimal.ZERO) {
                    throw Exception("Payment amount must be greater than zero")
                }

                if (paymentAmount > currentBalance) {
                    throw Exception("Payment amount exceeds loan balance")
                }

                // Calculate interest and principal portions
                // Interest = remaining balance * (annual rate / 12)
                val monthlyInterestRate = interestRate.divide(BigDecimal(1200), 6, BigDecimal.ROUND_HALF_UP)
                val interestPortion = currentBalance.multiply(monthlyInterestRate).setScale(2, BigDecimal.ROUND_HALF_UP)
                val principalPortion = paymentAmount.subtract(interestPortion).setScale(2, BigDecimal.ROUND_HALF_UP)

                // Ensure principal portion doesn't go negative
                val actualInterestPortion = if (principalPortion < BigDecimal.ZERO) {
                    paymentAmount
                } else {
                    interestPortion
                }
                val actualPrincipalPortion = if (principalPortion < BigDecimal.ZERO) {
                    BigDecimal.ZERO
                } else {
                    principalPortion
                }

                val newBalance = currentBalance.subtract(paymentAmount)
                val paymentId = UUID.randomUUID()

                // Create loan payment record
                LoanPayments.insert {
                    it[id] = paymentId
                    it[LoanPayments.loanId] = loanId
                    it[paymentDate] = LocalDate.now()
                    it[amount] = paymentAmount
                    it[principalAmount] = actualPrincipalPortion
                    it[interestAmount] = actualInterestPortion
                    it[feeAmount] = BigDecimal.ZERO
                    it[balanceAfter] = newBalance
                    it[paymentMethod] = request.paymentMethod
                    it[reference] = paymentId.toString()
                    it[processedBy] = request.processedBy?.let { UUID.fromString(it) } ?: UUID.randomUUID()
                }

                // Update loan record
                val updatedPaymentsRemaining = (loanRow[Loans.paymentsRemaining] ?: termMonths) - 1
                val newTotalInterestPaid = loanRow[Loans.totalInterestPaid].add(actualInterestPortion)
                val newTotalPrincipalPaid = loanRow[Loans.totalPrincipalPaid].add(actualPrincipalPortion)
                val newStatus = if (newBalance <= BigDecimal.ZERO) LoanStatus.PAID_OFF else LoanStatus.ACTIVE

                Loans.update({ Loans.id eq loanId }) {
                    it[Loans.currentBalance] = newBalance
                    it[Loans.lastPaymentDate] = LocalDate.now()
                    it[Loans.paymentsRemaining] = if (updatedPaymentsRemaining < 0) 0 else updatedPaymentsRemaining
                    it[Loans.totalInterestPaid] = newTotalInterestPaid
                    it[Loans.totalPrincipalPaid] = newTotalPrincipalPaid
                    it[Loans.status] = newStatus
                    // Update next payment date only if loan is still active
                    if (newStatus == LoanStatus.ACTIVE) {
                        it[Loans.nextPaymentDate] = LocalDate.now().plusMonths(1)
                    }
                }

                // Deduct payment from account balance (always, regardless of payment method)
                if (accountId != null) {
                    val account = Accounts.select { Accounts.id eq accountId }.singleOrNull()
                    if (account != null) {
                        val accountBalance = account[Accounts.balance]

                        // Check if account has sufficient balance
                        if (accountBalance < paymentAmount) {
                            throw Exception("Insufficient account balance. Available: $accountBalance, Required: $paymentAmount")
                        }

                        val newAccountBalance = accountBalance.subtract(paymentAmount)

                        // Update account balance
                        Accounts.update({ Accounts.id eq accountId }) {
                            it[balance] = newAccountBalance
                            it[availableBalance] = newAccountBalance
                        }

                        // Create transaction record
                        Transactions.insert {
                            it[Transactions.id] = UUID.randomUUID()
                            it[Transactions.accountId] = accountId
                            it[type] = TransactionType.LOAN_PAYMENT
                            it[amount] = paymentAmount
                            it[description] = "Loan payment - ${loanRow[Loans.loanType].name}"
                            it[balanceAfter] = newAccountBalance
                            it[Transactions.status] = TransactionStatus.COMPLETED
                            it[reference] = IdGenerator.generateTransactionId()
                            if (request.processedBy != null) {
                                it[processedBy] = UUID.fromString(request.processedBy)
                            }
                            it[branchId] = loanRow[Loans.branchId]
                        }
                    } else {
                        throw Exception("Account not found for this loan")
                    }
                } else {
                    throw Exception("No account linked to this loan")
                }

                // Send payment notification
                try {
                    if (newStatus == LoanStatus.PAID_OFF) {
                        notificationService.notifyLoanPaidOff(
                            customerId = customerId,
                            loanId = loanId
                        )
                        println("📬 Loan paid off notification sent")
                    } else {
                        notificationService.notifyLoanPaymentReceived(
                            customerId = customerId,
                            loanId = loanId,
                            amount = paymentAmount.toString(),
                            remainingBalance = newBalance.toString()
                        )
                        println("📬 Loan payment notification sent")
                    }
                } catch (e: Exception) {
                    println("⚠️ Failed to send payment notification: ${e.message}")
                }

                // Return payment details
                val paymentRecord = LoanPayments.select { LoanPayments.id eq paymentId }.single()

                ApiResponse(
                    success = true,
                    message = "Loan payment processed successfully",
                    data = LoanPaymentDto(
                        id = paymentId.toString(),
                        loanId = loanId.toString(),
                        paymentDate = paymentRecord[LoanPayments.paymentDate].toString(),
                        amount = paymentRecord[LoanPayments.amount].toString(),
                        principalAmount = paymentRecord[LoanPayments.principalAmount].toString(),
                        interestAmount = paymentRecord[LoanPayments.interestAmount].toString(),
                        feeAmount = paymentRecord[LoanPayments.feeAmount].toString(),
                        balanceAfter = paymentRecord[LoanPayments.balanceAfter].toString(),
                        paymentMethod = paymentRecord[LoanPayments.paymentMethod],
                        reference = paymentRecord[LoanPayments.reference],
                        processedBy = paymentRecord[LoanPayments.processedBy].toString(),
                        createdAt = paymentRecord[LoanPayments.createdAt].toString()
                    )
                )
            } catch (e: Exception) {
                println("❌ Error processing loan payment: ${e.message}")
                e.printStackTrace()
                ApiResponse(
                    success = false,
                    message = "Failed to process loan payment",
                    error = e.message
                )
            }
        }
    }

    suspend fun getLoanPaymentHistory(loanId: UUID): CustomerCareListResponse<LoanPaymentDto> {
        return DatabaseFactory.dbQuery {
            try {
                val payments = LoanPayments.select { LoanPayments.loanId eq loanId }
                    .orderBy(LoanPayments.paymentDate, SortOrder.DESC)
                    .map { paymentRow ->
                        LoanPaymentDto(
                            id = paymentRow[LoanPayments.id].toString(),
                            loanId = paymentRow[LoanPayments.loanId].toString(),
                            paymentDate = paymentRow[LoanPayments.paymentDate].toString(),
                            amount = paymentRow[LoanPayments.amount].toString(),
                            principalAmount = paymentRow[LoanPayments.principalAmount].toString(),
                            interestAmount = paymentRow[LoanPayments.interestAmount].toString(),
                            feeAmount = paymentRow[LoanPayments.feeAmount].toString(),
                            balanceAfter = paymentRow[LoanPayments.balanceAfter].toString(),
                            paymentMethod = paymentRow[LoanPayments.paymentMethod],
                            reference = paymentRow[LoanPayments.reference],
                            processedBy = paymentRow[LoanPayments.processedBy].toString(),
                            createdAt = paymentRow[LoanPayments.createdAt].toString()
                        )
                    }

                CustomerCareListResponse(
                    success = true,
                    message = "Loan payment history retrieved successfully",
                    data = payments,
                    total = payments.size,
                    page = 1,
                    pageSize = payments.size,
                    timestamp = java.time.Instant.now().toString()
                )
            } catch (e: Exception) {
                CustomerCareListResponse(
                    success = false,
                    message = "Failed to retrieve loan payment history: ${e.message}",
                    data = emptyList(),
                    total = 0,
                    page = 1,
                    pageSize = 10,
                    timestamp = java.time.Instant.now().toString()
                )
            }
        }
    }

    private fun toLoanDto(row: ResultRow): LoanDto {
        return LoanDto(
            id = row[Loans.id].toString(),
            accountId = row[Loans.accountId]?.toString(),
            customerId = row[Loans.customerId].toString(),
            applicationId = row[Loans.applicationId].toString(),
            loanType = row[Loans.loanType].name,
            originalAmount = row[Loans.originalAmount].toString(),
            currentBalance = row[Loans.currentBalance].toString(),
            interestRate = row[Loans.interestRate].toString(),
            termMonths = row[Loans.termMonths],
            monthlyPayment = row[Loans.monthlyPayment].toString(),
            paymentFrequency = row[Loans.paymentFrequency].name,
            status = row[Loans.status].name,
            originationDate = row[Loans.originationDate].toString(),
            maturityDate = row[Loans.maturityDate].toString(),
            nextPaymentDate = row[Loans.nextPaymentDate].toString(),
            lastPaymentDate = row[Loans.lastPaymentDate]?.toString(),
            paymentsRemaining = row[Loans.paymentsRemaining],
            totalInterestPaid = row[Loans.totalInterestPaid].toString(),
            totalPrincipalPaid = row[Loans.totalPrincipalPaid].toString(),
            latePaymentFees = row[Loans.latePaymentFees].toString(),
            collateralDescription = row[Loans.collateralDescription],
            collateralValue = row[Loans.collateralValue]?.toString(),
            loanOfficerId = row[Loans.loanOfficerId].toString(),
            branchId = row[Loans.branchId].toString(),
            createdAt = row[Loans.createdAt].toString(),
            updatedAt = row[Loans.updatedAt].toString()
        )
    }
}