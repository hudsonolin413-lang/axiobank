package org.dals.project.services

import org.dals.project.database.*
import org.dals.project.models.ApiResponse
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlinx.serialization.Serializable
import com.lowagie.text.*
import com.lowagie.text.pdf.*
import java.awt.Color

@Serializable
data class GenerateStatementRequest(
    val customerId: String,
    val accountId: String,
    val startDate: String,
    val endDate: String,
    val sendEmail: Boolean = true,
    val email: String? = null
)

class StatementService {
    private val emailService = EmailService()

    suspend fun generateAndSendStatement(request: GenerateStatementRequest): ApiResponse<String> {
        return DatabaseFactory.dbQuery {
            try {
                // Get customer info
                val customer = Customers.select { Customers.id eq UUID.fromString(request.customerId) }
                    .singleOrNull()
                    ?: return@dbQuery ApiResponse(
                        success = false,
                        message = "Customer not found",
                        error = "CUSTOMER_NOT_FOUND"
                    )

                // Get account info - try by UUID first, then by customer ID if UUID parsing fails
                val account = try {
                    Accounts.select { Accounts.id eq UUID.fromString(request.accountId) }
                        .singleOrNull()
                } catch (e: IllegalArgumentException) {
                    // If accountId is not a valid UUID, get the primary account for the customer
                    Accounts.select { Accounts.customerId eq UUID.fromString(request.customerId) }
                        .firstOrNull()
                } ?: return@dbQuery ApiResponse(
                    success = false,
                    message = "Account not found",
                    error = "ACCOUNT_NOT_FOUND"
                )

                // Get transactions for the period
                val startDate = LocalDate.parse(request.startDate)
                val endDate = LocalDate.parse(request.endDate)

                // Convert LocalDate to Instant for comparison
                val startInstant = startDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC)
                val endInstant = endDate.plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC)

                // Get transactions for the customer's account
                val accountUuid: UUID = account[Accounts.id].value

                val transactions = Transactions
                    .select {
                        (Transactions.accountId.eq(accountUuid)) and
                        (Transactions.timestamp greaterEq startInstant) and
                        (Transactions.timestamp less endInstant)
                    }
                    .orderBy(Transactions.timestamp, SortOrder.DESC)
                    .toList()

                // Generate PDF statement
                val pdfBytes = generatePdfStatement(
                    customerName = "${customer[Customers.firstName]} ${customer[Customers.lastName]}",
                    customerEmail = customer[Customers.email] ?: "",
                    accountNumber = account[Accounts.accountNumber],
                    accountType = account[Accounts.type].name,
                    currentBalance = account[Accounts.balance].toString(),
                    transactions = transactions,
                    startDate = startDate,
                    endDate = endDate
                )

                // Send email if requested
                if (request.sendEmail) {
                    val recipientEmail = request.email ?: customer[Customers.email]
                    
                    if (recipientEmail != null) {
                        val emailSent = emailService.sendStatementEmail(
                            toEmail = recipientEmail,
                            customerName = "${customer[Customers.firstName]} ${customer[Customers.lastName]}",
                            accountNumber = account[Accounts.accountNumber],
                            statementPeriod = "${startDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))} - ${endDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                            pdfBytes = pdfBytes
                        )
    
                        if (emailSent) {
                            ApiResponse(
                                success = true,
                                message = "Statement has been sent successfully. The PDF is encrypted with the last 4 digits of your account number.",
                                data = "Statement sent successfully"
                            )
                        } else {
                            ApiResponse(
                                success = false,
                                message = "Statement generated but failed to send email",
                                error = "EMAIL_SEND_FAILED"
                            )
                        }
                    } else {
                        ApiResponse(
                            success = false,
                            message = "No email address found for customer",
                            error = "EMAIL_NOT_FOUND"
                        )
                    }
                } else {
                    // Return PDF as base64 for download
                    val base64Pdf = Base64.getEncoder().encodeToString(pdfBytes)
                    ApiResponse(
                        success = true,
                        message = "Statement generated successfully. The PDF is encrypted with the last 4 digits of your account number.",
                        data = base64Pdf
                    )
                }
            } catch (e: Exception) {
                println("Error generating statement: ${e.message}")
                e.printStackTrace()
                ApiResponse(
                    success = false,
                    message = "Failed to generate statement: ${e.message}",
                    error = "STATEMENT_GENERATION_ERROR"
                )
            }
        }
    }

    /**
     * Mask phone numbers in a string by hiding the middle 3 digits
     * e.g., +254 712 345 678 -> +254 712 *** 678
     * e.g., 254712345678 -> 254712***678
     */
    private fun maskPhoneNumbers(text: String): String {
        // Regex to find phone numbers (simple version looking for 9-13 digits, optionally starting with +)
        val phoneRegex = Regex("""(\+?\d{6})(\d{3})(\d{3,4})""")
        return phoneRegex.replace(text) { matchResult ->
            val prefix = matchResult.groupValues[1]
            val suffix = matchResult.groupValues[3]
            "$prefix***$suffix"
        }
    }

    private fun generatePdfStatement(
        customerName: String,
        customerEmail: String,
        accountNumber: String,
        accountType: String,
        currentBalance: String,
        transactions: List<ResultRow>,
        startDate: LocalDate,
        endDate: LocalDate
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val document = Document(PageSize.A4)
        val writer = PdfWriter.getInstance(document, out)

        // Password for encryption: last 4 digits of account number
        val password = accountNumber.takeLast(4)
        writer.setEncryption(
            password.toByteArray(),
            password.toByteArray(),
            PdfWriter.ALLOW_PRINTING,
            PdfWriter.ENCRYPTION_AES_128
        )

        // Fonts
        val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f, Color(220, 20, 60)) // DC143C
        val subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14f)
        val boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f)
        val normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10f)
        val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, Color.WHITE)
        val mutedFont = FontFactory.getFont(FontFactory.HELVETICA, 8f, Color.GRAY)

        // Add Watermark and Logo
        class WatermarkHandler : PdfPageEventHelper() {
            private val logo: Image? = try {
                val logoResource = "static/AxioBank.png"
                val logoStream = this::class.java.classLoader.getResourceAsStream(logoResource)
                
                if (logoStream != null) {
                    val bytes = logoStream.readAllBytes()
                    Image.getInstance(bytes).apply {
                        scaleToFit(400f, 400f)
                    }
                } else {
                    val logoPath = "server/src/main/resources/static/AxioBank.png"
                    Image.getInstance(logoPath).apply {
                        scaleToFit(400f, 400f)
                    }
                }
            } catch (e: Exception) {
                null
            }

            override fun onEndPage(writer: PdfWriter, document: Document) {
                logo?.let {
                    val canvas = writer.directContentUnder
                    it.setAbsolutePosition(
                        (PageSize.A4.width - it.scaledWidth) / 2,
                        (PageSize.A4.height - it.scaledHeight) / 2
                    )
                    val gstate = PdfGState().apply {
                        setFillOpacity(0.1f)
                        setStrokeOpacity(0.1f)
                    }
                    canvas.saveState()
                    canvas.setGState(gstate)
                    canvas.addImage(it)
                    canvas.restoreState()
                }

                // Footer
                val canvas = writer.directContent
                val footer = Phrase("AxioBank Statement | Confidential | Page ${writer.pageNumber}", FontFactory.getFont(FontFactory.HELVETICA, 8f, Color.GRAY))
                ColumnText.showTextAligned(
                    canvas,
                    Element.ALIGN_CENTER,
                    footer,
                    (document.right() - document.left()) / 2 + document.leftMargin(),
                    document.bottom() - 10f,
                    0f
                )
            }
        }
        writer.setPageEvent(WatermarkHandler())

        document.open()

        // Header with Logo
        try {
            val logoResource = "static/AxioBank.png"
            val logoStream = this::class.java.classLoader.getResourceAsStream(logoResource)
            val logo = if (logoStream != null) {
                Image.getInstance(logoStream.readAllBytes())
            } else {
                Image.getInstance("server/src/main/resources/static/AxioBank.png")
            }
            logo.scaleToFit(120f, 120f)
            logo.alignment = Element.ALIGN_CENTER
            document.add(logo)
        } catch (e: Exception) {
            val header = Paragraph("AXIOBANK", titleFont)
            header.alignment = Element.ALIGN_CENTER
            document.add(header)
        }

        val statementTitle = Paragraph("Account Statement", subTitleFont)
        statementTitle.alignment = Element.ALIGN_CENTER
        document.add(statementTitle)

        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        val period = Paragraph("Statement Period: ${startDate.format(formatter)} to ${endDate.format(formatter)}", normalFont)
        period.alignment = Element.ALIGN_CENTER
        document.add(period)
        document.add(Paragraph("\n"))

        // Info Section
        val infoTable = PdfPTable(2)
        infoTable.widthPercentage = 100f
        infoTable.setWidths(floatArrayOf(1f, 2f))

        fun addInfoRow(label: String, value: String) {
            val labelCell = PdfPCell(Phrase(label, boldFont))
            labelCell.border = Rectangle.NO_BORDER
            labelCell.paddingBottom = 5f
            infoTable.addCell(labelCell)

            val valueCell = PdfPCell(Phrase(value, normalFont))
            valueCell.border = Rectangle.NO_BORDER
            valueCell.paddingBottom = 5f
            infoTable.addCell(valueCell)
        }

        addInfoRow("Account Holder:", customerName)
        addInfoRow("Email:", customerEmail)
        addInfoRow("Account Number:", accountNumber)
        addInfoRow("Account Type:", accountType)
        addInfoRow("Current Balance:", "$$currentBalance")
        addInfoRow("Statement Date:", LocalDate.now().format(formatter))

        document.add(infoTable)
        document.add(Paragraph("\n"))

        // Transaction History
        document.add(Paragraph("Transaction History", subTitleFont))
        document.add(Paragraph("\n"))

        val table = PdfPTable(5)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(2.5f, 3.5f, 2f, 2f, 2f))

        // Table Headers
        val headers = listOf("Date & Time", "Description", "Reference", "Amount", "Balance")
        headers.forEach { h ->
            val cell = PdfPCell(Phrase(h, headerFont))
            cell.backgroundColor = Color(220, 20, 60)
            cell.setPadding(8f)
            table.addCell(cell)
        }

        val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
        var totalCredits = java.math.BigDecimal.ZERO
        var totalDebits = java.math.BigDecimal.ZERO

        transactions.forEach { row ->
            val amount = row[Transactions.amount]
            val type = row[Transactions.type].name
            val isCredit = type in listOf("DEPOSIT", "TRANSFER", "INTEREST_CREDIT", "REVERSAL")

            if (isCredit) totalCredits += amount else totalDebits += amount

            val timestamp = row[Transactions.timestamp]
            val localDateTime = java.time.LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())
            
            // Mask phone numbers in description for security
            val rawDescription = row[Transactions.description]
            val maskedDescription = maskPhoneNumbers(rawDescription)

            table.addCell(PdfPCell(Phrase(localDateTime.format(dateTimeFormatter), normalFont)).apply { setPadding(5f) })
            table.addCell(PdfPCell(Phrase(maskedDescription, normalFont)).apply { setPadding(5f) })
            table.addCell(PdfPCell(Phrase(row[Transactions.reference] ?: "N/A", normalFont)).apply { setPadding(5f) })
            
            val amountText = "${if (isCredit) "+" else "-"}$$amount"
            val amountCell = PdfPCell(Phrase(amountText, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, if (isCredit) Color(76, 175, 80) else Color(244, 67, 54))))
            amountCell.setPadding(5f)
            table.addCell(amountCell)
            
            table.addCell(PdfPCell(Phrase("$$${row[Transactions.balanceAfter]}", normalFont)).apply { setPadding(5f) })
        }

        document.add(table)
        document.add(Paragraph("\n"))

        // Statement Summary
        document.add(Paragraph("Statement Summary", subTitleFont))
        document.add(Paragraph("\n"))

        val summaryTable = PdfPTable(2)
        summaryTable.widthPercentage = 40f
        summaryTable.horizontalAlignment = Element.ALIGN_LEFT

        fun addSummaryRow(label: String, value: String, isCredit: Boolean? = null) {
            val cell1 = PdfPCell(Phrase(label, boldFont))
            cell1.border = Rectangle.NO_BORDER
            cell1.setPadding(5f)
            summaryTable.addCell(cell1)

            val color = when(isCredit) {
                true -> Color(76, 175, 80)
                false -> Color(244, 67, 54)
                else -> Color.BLACK
            }
            val cell2 = PdfPCell(Phrase(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, color)))
            cell2.border = Rectangle.NO_BORDER
            cell2.setPadding(5f)
            summaryTable.addCell(cell2)
        }

        addSummaryRow("Total Credits:", "+$$totalCredits", true)
        addSummaryRow("Total Debits:", "-$$totalDebits", false)
        val netChange = totalCredits - totalDebits
        addSummaryRow("Net Change:", "${if (netChange >= java.math.BigDecimal.ZERO) "+" else "-"}$$${netChange.abs()}")
        addSummaryRow("Transactions:", transactions.size.toString())

        document.add(summaryTable)
        document.add(Paragraph("\n\n"))

        // Footer
        val footer = Paragraph()
        footer.alignment = Element.ALIGN_CENTER
        footer.add(Phrase("This is a computer-generated statement and does not require a signature.\n", FontFactory.getFont(FontFactory.HELVETICA, 8f, Color.GRAY)))
        footer.add(Phrase("For inquiries, please contact us at support@axionbank.com or call 1-800-AXION-BANK\n", FontFactory.getFont(FontFactory.HELVETICA, 8f, Color.GRAY)))
        footer.add(Phrase("© ${LocalDate.now().year} AxionBank. All rights reserved.", FontFactory.getFont(FontFactory.HELVETICA, 8f, Color.GRAY)))
        document.add(footer)

        document.close()
        return out.toByteArray()
    }

    private fun buildStatementHtml(
        customerName: String,
        customerEmail: String,
        accountNumber: String,
        accountType: String,
        currentBalance: String,
        transactions: List<ResultRow>,
        startDate: LocalDate,
        endDate: LocalDate
    ): String {
        return "" // No longer used, replaced by PDF generation
    }
}
