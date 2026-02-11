package org.dals.project.services

import java.io.File
import java.util.Properties
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMultipart
import javax.activation.DataHandler
import javax.activation.FileDataSource

class EmailService {
    // Email configuration - Hardcoded for Axio Bank with fallback to environment variables
    private val smtpHost = System.getenv("SMTP_HOST") ?: "smtp.gmail.com"
    private val smtpPort = System.getenv("SMTP_PORT") ?: "587"
    private val smtpUsername = System.getenv("SMTP_USERNAME") ?: "abrocoder@gmail.com"
    private val smtpPassword = System.getenv("SMTP_PASSWORD") ?: "qbdpvggzgslrqwvz"
    private val fromEmail = System.getenv("SMTP_FROM_EMAIL") ?: smtpUsername
    private val fromName = System.getenv("SMTP_FROM_NAME") ?: "Axio Bank"

    private val session: Session by lazy {
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", smtpHost)
            put("mail.smtp.port", smtpPort)
            put("mail.smtp.ssl.protocols", "TLSv1.2")
        }

        Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(smtpUsername, smtpPassword)
            }
        })
    }

    fun sendEmail(
        to: String,
        subject: String,
        body: String,
        isHtml: Boolean = false,
        attachments: List<String> = emptyList(),
        embedLogo: Boolean = false
    ): Result<Unit> {
        return try {
            // Check if SMTP is configured
            if (smtpPassword.isEmpty()) {
                println("⚠️ Email not sent: SMTP not configured. Set SMTP_PASSWORD environment variable.")
                return Result.failure(Exception("SMTP not configured"))
            }

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(fromEmail, fromName))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                this.subject = subject

                if (isHtml || attachments.isNotEmpty() || embedLogo) {
                    // Create multipart message
                    val multipart = MimeMultipart("related")

                    // Add body part
                    val bodyPart = MimeBodyPart()
                    if (isHtml) {
                        bodyPart.setContent(body, "text/html; charset=utf-8")
                    } else {
                        bodyPart.setText(body)
                    }
                    multipart.addBodyPart(bodyPart)

                    // Embed logo if requested
                    if (embedLogo) {
                        val logoResource = "static/AxioBank.png"
                        val logoStream = this::class.java.classLoader.getResourceAsStream(logoResource)
                        
                        if (logoStream != null) {
                            val logoPart = MimeBodyPart()
                            // We need to write the stream to a temp file or byte array because FileDataSource needs a File
                            val tempLogoFile = File.createTempFile("axiobank_logo", ".png")
                            tempLogoFile.deleteOnExit()
                            logoStream.use { input ->
                                tempLogoFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            
                            val logoSource = FileDataSource(tempLogoFile)
                            logoPart.dataHandler = DataHandler(logoSource)
                            logoPart.setHeader("Content-ID", "<axiobank-logo>")
                            logoPart.disposition = MimeBodyPart.INLINE
                            multipart.addBodyPart(logoPart)
                        } else {
                            println("⚠️ Logo resource not found in classpath: $logoResource")
                            // Fallback to file system if resource fails
                            val logoPath = "server/src/main/resources/static/AxioBank.png"
                            val logoFile = File(logoPath)
                            if (logoFile.exists()) {
                                val logoPart = MimeBodyPart()
                                val logoSource = FileDataSource(logoFile)
                                logoPart.dataHandler = DataHandler(logoSource)
                                logoPart.setHeader("Content-ID", "<axiobank-logo>")
                                logoPart.disposition = MimeBodyPart.INLINE
                                multipart.addBodyPart(logoPart)
                            } else {
                                println("⚠️ Logo file also not found at: $logoPath")
                            }
                        }
                    }

                    // Add attachment parts
                    attachments.forEach { filePath ->
                        val attachmentFile = File(filePath)
                        if (attachmentFile.exists()) {
                            val attachmentPart = MimeBodyPart()
                            val source = FileDataSource(attachmentFile)
                            attachmentPart.dataHandler = DataHandler(source)
                            attachmentPart.fileName = attachmentFile.name
                            multipart.addBodyPart(attachmentPart)
                        }
                    }

                    setContent(multipart)
                } else {
                    if (isHtml) {
                        setContent(body, "text/html; charset=utf-8")
                    } else {
                        setText(body)
                    }
                }
            }

            Transport.send(message)
            println("✅ Email sent to: $to")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Failed to send email to $to: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun sendBulkEmail(
        recipients: List<String>,
        subject: String,
        body: String,
        isHtml: Boolean = false
    ): Pair<Int, Int> {
        var successCount = 0
        var failureCount = 0

        recipients.forEach { email ->
            val result = sendEmail(email, subject, body, isHtml)
            if (result.isSuccess) {
                successCount++
            } else {
                failureCount++
            }
        }

        return Pair(successCount, failureCount)
    }

    fun sendNotificationEmail(
        to: String,
        title: String,
        message: String,
        priority: String,
        attachments: List<String> = emptyList()
    ): Result<Unit> {
        val priorityColor = when (priority) {
            "URGENT" -> "#DC2626"
            "HIGH" -> "#F59E0B"
            "NORMAL" -> "#3B82F6"
            else -> "#6B7280"
        }

        val priorityBadge = when (priority) {
            "URGENT" -> "URGENT"
            "HIGH" -> "HIGH PRIORITY"
            "NORMAL" -> "NOTIFICATION"
            else -> "INFO"
        }

        val htmlBody = createEmailTemplate(
            title = title,
            message = message,
            priorityBadge = priorityBadge,
            priorityColor = priorityColor
        )

        return sendEmail(to, "AxioBank: $title", htmlBody, isHtml = true, attachments = attachments, embedLogo = true)
    }

    fun sendOtpEmail(
        to: String,
        otp: String,
        purpose: String = "verification",
        expiryMinutes: Int = 10
    ): Result<Unit> {
        val htmlBody = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 20px auto; background-color: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #DC143C 0%, #A00F2B 100%); color: white; padding: 30px 20px; text-align: center; }
                    .logo { font-size: 32px; font-weight: bold; margin-bottom: 10px; }
                    .content { padding: 40px 30px; }
                    .otp-box { background-color: #f8f9fa; border: 2px dashed #DC143C; border-radius: 8px; padding: 30px; text-align: center; margin: 30px 0; }
                    .otp-code { font-size: 48px; font-weight: bold; color: #DC143C; letter-spacing: 8px; font-family: 'Courier New', monospace; }
                    .warning { background-color: #FFF3CD; border-left: 4px solid #FFC107; padding: 15px; margin: 20px 0; border-radius: 4px; }
                    .footer { background-color: #f8f9fa; padding: 25px 30px; border-top: 1px solid #e9ecef; font-size: 13px; color: #6c757d; }
                    .contact-info { margin-top: 15px; }
                    .contact-info p { margin: 5px 0; }
                    h2 { color: #DC143C; margin-top: 0; }
                    .btn { display: inline-block; padding: 12px 30px; background-color: #DC143C; color: white; text-decoration: none; border-radius: 5px; font-weight: bold; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <img src="cid:axiobank-logo" alt="AxioBank" style="height: 80px; margin-bottom: 10px;">
                        <p style="margin: 0; font-size: 16px;">Secure Banking Services</p>
                    </div>
                    <div class="content">
                        <h2>One-Time Password (OTP)</h2>
                        <p>Hello,</p>
                        <p>You have requested an OTP for <strong>$purpose</strong>. Please use the following code to complete your verification:</p>

                        <div class="otp-box">
                            <div class="otp-code">$otp</div>
                            <p style="margin: 10px 0 0 0; color: #6c757d;">This code expires in $expiryMinutes minutes</p>
                        </div>

                        <div class="warning">
                            <strong>⚠️ Security Notice:</strong>
                            <ul style="margin: 10px 0 0 0; padding-left: 20px;">
                                <li>Never share this code with anyone, including AxioBank staff</li>
                                <li>AxioBank will never ask for your OTP via phone or email</li>
                                <li>If you didn't request this code, please contact us immediately</li>
                            </ul>
                        </div>
                    </div>
                    <div class="footer">
                        <p><strong>AxioBank Customer Care</strong></p>
                        <div class="contact-info">
                            <p>📞 Phone: +1 (70) 970-7166</p>
                            <p>✉️ Email: customercare@axiobank.com</p>
                            <p>🌐 Website: www.axiobank.com</p>
                        </div>
                        <p style="margin-top: 20px;">This is an automated email. Please do not reply to this message.</p>
                        <p style="margin-top: 10px; font-size: 11px; color: #999;">© ${java.time.Year.now().value} AxioBank. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        return sendEmail(to, "AxioBank: Your Verification Code", htmlBody, isHtml = true, embedLogo = true)
    }

    fun sendStatementEmail(
        toEmail: String,
        customerName: String,
        accountNumber: String,
        statementPeriod: String,
        pdfBytes: ByteArray
    ): Boolean {
        return try {
            // Save PDF to temp file
            val tempFile = File.createTempFile("statement_", ".pdf")
            tempFile.writeBytes(pdfBytes)

            val htmlBody = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f4f4f4; }
                        .container { max-width: 600px; margin: 20px auto; background-color: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                        .header { background: linear-gradient(135deg, #DC143C 0%, #A00F2B 100%); color: white; padding: 30px 20px; text-align: center; }
                        .content { padding: 40px 30px; }
                        .statement-info { background-color: #f8f9fa; padding: 20px; border-radius: 6px; margin: 20px 0; }
                        .info-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #e9ecef; }
                        .info-row:last-child { border-bottom: none; }
                        .info-label { font-weight: bold; color: #666; }
                        .info-value { color: #333; }
                        .footer { background-color: #f8f9fa; padding: 25px 30px; border-top: 1px solid #e9ecef; font-size: 13px; color: #6c757d; }
                        .attachment-notice { background-color: #E3F2FD; border-left: 4px solid #2196F3; padding: 15px; margin: 20px 0; border-radius: 4px; }
                        h2 { color: #DC143C; margin-top: 0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <img src="cid:axiobank-logo" alt="AxioBank" style="height: 60px;">
                            <h1 style="margin: 10px 0 0 0; font-size: 24px; letter-spacing: 1px;">AXIOBANK</h1>
                            <p style="margin: 5px 0 0 0; font-size: 14px; opacity: 0.9;">Secure Banking Services</p>
                        </div>
                        <div class="content">
                            <h2>Your Account Statement</h2>
                            <p>Dear $customerName,</p>
                            <p>Please find attached your account statement for the requested period.</p>

                            <div class="statement-info">
                                <div class="info-row">
                                    <span class="info-label">Account Number:</span>
                                    <span class="info-value">$accountNumber</span>
                                </div>
                                <div class="info-row">
                                    <span class="info-label">Statement Period:</span>
                                    <span class="info-value">$statementPeriod</span>
                                </div>
                                <div class="info-row">
                                    <span class="info-label">Generated On:</span>
                                    <span class="info-value">${java.time.LocalDate.now()}</span>
                                </div>
                            </div>

                            <div class="attachment-notice">
                                <strong>📎 Attachment:</strong>
                                <p style="margin: 10px 0 0 0;">Your statement is attached to this email as a PDF document. For your security, the PDF is encrypted. The password to open the file is the <strong>last 4 digits of your account number</strong>.</p>
                            </div>

                            <p style="margin-top: 30px;">Thank you for banking with AxioBank.</p>
                        </div>
                        <div class="footer">
                            <p><strong>AxioBank Customer Care</strong></p>
                            <div style="margin-top: 15px;">
                                <p>📞 Phone: +1 (774) 600-6987</p>
                                <p>✉️ Email: customercare@axiobank.com</p>
                                <p>🌐 Website: www.axiobank.com</p>
                            </div>
                            <p style="margin-top: 20px;">This is an automated email. Please do not reply to this message.</p>
                            <p style="margin-top: 10px; font-size: 11px; color: #999;">© ${java.time.Year.now().value} AxioBank. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
            """.trimIndent()

            val result = sendEmail(
                to = toEmail,
                subject = "Your AxioBank Account Statement - $statementPeriod",
                body = htmlBody,
                isHtml = true,
                attachments = listOf(tempFile.absolutePath),
                embedLogo = true
            )

            // Clean up temp file
            tempFile.delete()

            result.isSuccess
        } catch (e: Exception) {
            println("Error sending statement email: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private fun createEmailTemplate(
        title: String,
        message: String,
        priorityBadge: String,
        priorityColor: String
    ): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 20px auto; background-color: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #DC143C 0%, #A00F2B 100%); color: white; padding: 30px 20px; text-align: center; }
                    .logo { font-size: 32px; font-weight: bold; margin-bottom: 10px; }
                    .content { padding: 40px 30px; }
                    .priority-badge { display: inline-block; padding: 8px 16px; border-radius: 20px; font-weight: bold; font-size: 12px; background-color: $priorityColor; color: white; margin-bottom: 20px; }
                    .message-box { background-color: #f8f9fa; padding: 20px; border-radius: 6px; border-left: 4px solid #DC143C; margin: 20px 0; }
                    .footer { background-color: #f8f9fa; padding: 25px 30px; border-top: 1px solid #e9ecef; font-size: 13px; color: #6c757d; }
                    .contact-info { margin-top: 15px; }
                    .contact-info p { margin: 5px 0; }
                    h2 { color: #DC143C; margin-top: 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <img src="cid:axiobank-logo" alt="AxioBank" style="height: 80px; margin-bottom: 10px;">
                        <p style="margin: 0; font-size: 16px;">Secure Banking Services</p>
                    </div>
                    <div class="content">
                        <span class="priority-badge">$priorityBadge</span>
                        <h2>$title</h2>
                        <div class="message-box">
                            <p style="margin: 0;">$message</p>
                        </div>
                    </div>
                    <div class="footer">
                        <p><strong>Need Help?</strong></p>
                        <div class="contact-info">
                            <p>📞 Phone: +1 (774) 600-6987</p>
                            <p>✉️ Email: customercare@axiobank.com</p>
                            <p>🌐 Website: www.axiobank.com</p>
                        </div>
                        <p style="margin-top: 20px;">This is an automated notification from AxioBank.</p>
                        <p style="margin-top: 10px; font-size: 11px; color: #999;">© ${java.time.Year.now().value} AxioBank. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
