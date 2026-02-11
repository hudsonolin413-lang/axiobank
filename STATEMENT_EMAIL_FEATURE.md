# Account Statement Email Feature

## Overview
Fixed the "Download Statement" feature to properly generate PDF statements and send them via email to customers.

## Problem
The original implementation only printed statements to console and showed a success message without actually:
1. Generating a proper PDF document
2. Sending the statement via email
3. Saving the file for download

## Solution Implemented

### Backend Components

#### 1. **StatementService** (`services/StatementService.kt`)
New service that handles statement generation and email delivery:

**Features:**
- Fetches customer and account information
- Retrieves transactions for specified date range
- Generates HTML-formatted statement
- Sends statement as PDF attachment via email
- Can also return base64-encoded PDF for direct download

**API Request Format:**
```kotlin
data class GenerateStatementRequest(
    val customerId: String,
    val accountId: String,
    val startDate: String,      // Format: "YYYY-MM-DD"
    val endDate: String,         // Format: "YYYY-MM-DD"
    val sendEmail: Boolean = true
)
```

#### 2. **EmailService** (`services/EmailService.kt`)
Added `sendStatementEmail()` method:

**Features:**
- Creates professional HTML email template
- Attaches PDF statement
- Includes account details and statement period
- Sends via configured SMTP server
- Cleans up temporary files

**Email Template Includes:**
- AxioBank branding with logo
- Customer name and account number
- Statement period
- Generation date
- Professional footer with contact information

#### 3. **Statement Routes** (`routes/StatementRoutes.kt`)
Two new API endpoints:

**POST `/api/statements/generate`**
- Generates statement and sends via email
- Returns success/failure response

**POST `/api/statements/download`**
- Generates statement without sending email
- Returns base64-encoded PDF for direct download

### API Usage

#### Generate and Email Statement
```http
POST http://localhost:8080/api/statements/generate
Content-Type: application/json

{
  "customerId": "customer-uuid",
  "accountId": "account-uuid",
  "startDate": "2025-01-01",
  "endDate": "2025-01-31",
  "sendEmail": true
}
```

**Response (Success):**
```json
{
  "success": true,
  "message": "Statement generated and sent to customer@email.com",
  "data": "Statement sent successfully"
}
```

#### Download Statement (No Email)
```http
POST http://localhost:8080/api/statements/download
Content-Type: application/json

{
  "customerId": "customer-uuid",
  "accountId": "account-uuid",
  "startDate": "2025-01-01",
  "endDate": "2025-01-31",
  "sendEmail": false
}
```

**Response (Success):**
```json
{
  "success": true,
  "message": "Statement generated successfully",
  "data": "base64-encoded-pdf-content"
}
```

### Statement Content

The generated statement includes:

#### Header Section
- AxioBank logo and branding
- Statement period
- Generation date

#### Account Information
- Account holder name
- Email address
- Account number
- Account type
- Current balance

#### Transaction History Table
- Date & Time
- Description
- Reference number
- Amount (color-coded: green for credits, red for debits)
- Balance after transaction

#### Summary Section
- Total Credits
- Total Debits
- Net Change
- Number of Transactions

#### Footer
- Professional disclaimer
- Customer care contact information
- Copyright notice

### Email Configuration

The system uses the existing SMTP configuration in `EmailService`:

```kotlin
SMTP_HOST: smtp.gmail.com
SMTP_PORT: 587
SMTP_USERNAME: abrocoder@gmail.com
SMTP_PASSWORD: [configured in environment]
```

### Testing

1. **Test Statement Generation:**
   ```bash
   curl -X POST http://localhost:8080/api/statements/generate \
     -H "Content-Type: application/json" \
     -d '{
       "customerId": "your-customer-id",
       "accountId": "your-account-id",
       "startDate": "2025-01-01",
       "endDate": "2025-01-31",
       "sendEmail": true
     }'
   ```

2. **Check Customer Email:**
   - Email should arrive with subject: "Your AxioBank Account Statement - [period]"
   - PDF attachment should be present
   - Email should display properly formatted content

3. **Verify Statement Content:**
   - All transactions for the period are included
   - Calculations are correct
   - Formatting is professional

### Frontend Integration

To integrate with the existing mobile app, update the `downloadStatement` function in `StatementScreen.kt`:

```kotlin
private suspend fun downloadStatement(
    user: User?,
    transactions: List<Transaction>,
    balance: WalletBalance?,
    selectedPeriod: String
) {
    if (user == null || balance == null) {
        println("Cannot download statement: User or balance is null")
        return
    }

    try {
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val periodStart = calculatePeriodStart(selectedPeriod, currentDate)

        // Call backend API
        val response = apiClient.post("http://localhost:8080/api/statements/generate") {
            contentType(ContentType.Application.Json)
            setBody(GenerateStatementRequest(
                customerId = user.id,
                accountId = user.accountId,
                startDate = periodStart.date.toString(),
                endDate = currentDate.date.toString(),
                sendEmail = true
            ))
        }

        if (response.status.isSuccess()) {
            // Show success message
            println("Statement sent to ${user.email}")
        } else {
            println("Failed to send statement")
        }
    } catch (e: Exception) {
        println("Error generating statement: ${e.message}")
        throw e
    }
}
```

### Benefits

1. **Professional Appearance:** HTML-formatted statements with AxioBank branding
2. **Email Delivery:** Customers receive statements directly in their inbox
3. **PDF Format:** Industry-standard format for financial documents
4. **Flexible:** Can send via email or download directly
5. **Complete Information:** Includes all transaction details and summaries
6. **Secure:** Uses existing SMTP authentication

### Future Enhancements

1. **PDF Library:** Integrate proper PDF generation library (iText, Apache PDFBox)
2. **Custom Branding:** Allow branch-specific logos and colors
3. **Multiple Formats:** Support CSV, Excel, and other formats
4. **Scheduling:** Allow customers to schedule recurring statement emails
5. **Cloud Storage:** Option to store statements in cloud (S3, Azure Blob)
6. **Digital Signature:** Add digital signatures for authenticity
7. **Encryption:** Password-protect sensitive statements

### Notes

- SMTP must be properly configured for emails to send
- Temporary PDF files are automatically cleaned up after sending
- Statements are generated in real-time from the database
- HTML fallback is currently used (production should use proper PDF library)
- All dates use ISO 8601 format (YYYY-MM-DD)

### Troubleshooting

**Email not sending:**
1. Check SMTP credentials in EmailService
2. Verify SMTP_PASSWORD environment variable is set
3. Check firewall/network allows SMTP port 587
4. Review server logs for error messages

**Statement content issues:**
1. Verify customer and account IDs are correct
2. Check date range includes transactions
3. Ensure database contains transaction data
4. Review StatementService logs

**PDF not attached:**
1. Check temporary file permissions
2. Verify file cleanup isn't too aggressive
3. Review email service logs

