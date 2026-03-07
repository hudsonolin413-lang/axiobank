package org.dals.project.services

import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.activation.DataHandler
import javax.activation.DataSource
import javax.mail.util.ByteArrayDataSource

class EmailService {
    // These should ideally come from environment variables
    private val smtpHost = System.getenv("SMTP_HOST") ?: "smtp.gmail.com"
    private val smtpPort = System.getenv("SMTP_PORT") ?: "587"
    private val smtpUser = System.getenv("SMTP_USERNAME") ?: "abrocoder@gmail.com"
    private val smtpPass = System.getenv("SMTP_PASSWORD") ?: "" // Must be set in environment

    private val properties = Properties().apply {
        put("mail.smtp.auth", "true")
        put("mail.smtp.starttls.enable", "true")
        put("mail.smtp.host", smtpHost)
        put("mail.smtp.port", smtpPort)
        put("mail.smtp.ssl.trust", smtpHost)
    }

    private fun getSession(): Session {
        return Session.getInstance(properties, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(smtpUser, smtpPass)
            }
        })
    }

    fun sendEmail(to: String, subject: String, htmlContent: String, attachments: Map<String, ByteArray> = emptyMap()): Result<Unit> {
        if (smtpPass.isBlank()) {
            val errorMsg = "Email service not configured. Please set SMTP_PASSWORD environment variable."
            println("⚠️ EmailService: SMTP_PASSWORD not set. Email to $to suppressed. Content: $subject")
            return Result.failure(Exception(errorMsg))
        }

        if (smtpUser.isBlank()) {
            val errorMsg = "Email service not configured. Please set SMTP_USERNAME environment variable."
            println("⚠️ EmailService: SMTP_USERNAME not set.")
            return Result.failure(Exception(errorMsg))
        }

        return try {
            val message = MimeMessage(getSession())
            message.setFrom(InternetAddress(smtpUser, "AxioBank"))
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
            message.subject = subject

            val multipart = MimeMultipart()

            // HTML Content
            val messageBodyPart = MimeBodyPart()
            messageBodyPart.setContent(htmlContent, "text/html; charset=utf-8")
            multipart.addBodyPart(messageBodyPart)

            // Attachments
            attachments.forEach { (fileName, data) ->
                val attachmentPart = MimeBodyPart()
                val source: DataSource = ByteArrayDataSource(data, "application/pdf")
                attachmentPart.dataHandler = DataHandler(source)
                attachmentPart.fileName = fileName
                multipart.addBodyPart(attachmentPart)
            }

            message.setContent(multipart)

            Transport.send(message)
            println("📧 Email sent successfully to $to: $subject")
            Result.success(Unit)
        } catch (e: AuthenticationFailedException) {
            val errorMsg = "Email authentication failed. Please check your SMTP credentials and ensure you're using an App Password for Gmail."
            println("❌ Failed to authenticate email for $to: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception(errorMsg, e))
        } catch (e: Exception) {
            val errorMsg = "Failed to send email. Error: ${e.message}"
            println("❌ Failed to send email to $to: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception(errorMsg, e))
        }
    }

    fun sendReferralInvitation(toEmail: String, referrerName: String, referralCode: String): Result<Unit> {
        val subject = "$referrerName invited you to join AxioBank!"
        val htmlContent = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;">
                <div style="text-align: center; margin-bottom: 20px;">
                    <h1 style="color: #1a73e8; margin: 0;">AxioBank</h1>
                    <p style="color: #5f6368; font-size: 14px;">Your Digital Banking Partner</p>
                </div>
                
                <h2 style="color: #202124;">You're Invited!</h2>
                <p style="color: #3c4043; line-height: 1.5;">
                    Hi there,
                </p>
                <p style="color: #3c4043; line-height: 1.5;">
                    <strong>$referrerName</strong> thinks you'd love AxioBank and has invited you to join. 
                    AxioBank offers seamless digital banking, instant transfers, and great rewards.
                </p>
                
                <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px; text-align: center; margin: 25px 0;">
                    <p style="margin: 0 0 10px 0; color: #5f6368; font-size: 14px;">Use this referral code during signup:</p>
                    <div style="font-size: 32px; font-weight: bold; color: #1a73e8; letter-spacing: 2px;">$referralCode</div>
                </div>
                
                <p style="color: #3c4043; line-height: 1.5;">
                    Get <strong>$25.00</strong> reward after you sign up and complete your first transaction!
                </p>
                
                <div style="text-align: center; margin-top: 30px;">
                    <a href="https://axiobank.com/signup?code=$referralCode" style="background-color: #1a73e8; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold;">Join AxioBank Now</a>
                </div>
                
                <hr style="border: 0; border-top: 1px solid #e0e0e0; margin: 30px 0;">
                
                <p style="color: #70757a; font-size: 12px; text-align: center;">
                    If you didn't expect this invitation, you can safely ignore this email.<br>
                    &copy; ${java.time.Year.now().value} AxioBank. All rights reserved.
                </p>
            </div>
        """.trimIndent()

        return sendEmail(toEmail, subject, htmlContent)
    }

    fun sendOtpEmail(toEmail: String, otp: String, purpose: String): Result<Unit> {
        val subject = "Your AxioBank Verification Code"
        val htmlContent = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;">
                <div style="text-align: center; margin-bottom: 20px;">
                    <h1 style="color: #1a73e8; margin: 0;">AxioBank</h1>
                </div>
                
                <h2 style="color: #202124;">Verification Code</h2>
                <p style="color: #3c4043;">
                    Use the following code for <strong>$purpose</strong>. This code will expire in 10 minutes.
                </p>
                
                <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px; text-align: center; margin: 25px 0;">
                    <div style="font-size: 36px; font-weight: bold; color: #1a73e8; letter-spacing: 5px;">$otp</div>
                </div>
                
                <p style="color: #70757a; font-size: 12px;">
                    If you did not request this code, please secure your account immediately.
                </p>
            </div>
        """.trimIndent()

        return sendEmail(toEmail, subject, htmlContent)
    }

    fun sendStatementEmail(toEmail: String, customerName: String, accountNumber: String, statementPeriod: String, pdfBytes: ByteArray): Result<Unit> {
        val subject = "Your AxioBank Account Statement - $statementPeriod"
        val htmlContent = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;">
                <h1 style="color: #1a73e8;">AxioBank Statement</h1>
                <p>Dear $customerName,</p>
                <p>Please find attached your account statement for the period <strong>$statementPeriod</strong>.</p>
                <p>Account Number: ****${accountNumber.takeLast(4)}</p>
                <br>
                <p>Thank you for banking with AxioBank!</p>
            </div>
        """.trimIndent()

        val attachments = mapOf("Statement_$statementPeriod.pdf" to pdfBytes)
        return sendEmail(toEmail, subject, htmlContent, attachments)
    }
}
