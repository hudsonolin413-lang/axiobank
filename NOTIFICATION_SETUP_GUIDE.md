# Multi-Channel Notification System Setup Guide

## Overview
AxioBank now supports **three notification channels**:
1. **In-App Notifications** - Always enabled, no setup required
2. **Email Notifications** - Requires SMTP configuration
3. **SMS Notifications** - Requires SMS provider setup

---

## 1. In-App Notifications ✅

**Status:** Fully configured and working out of the box!

**Features:**
- Real-time delivery with unread badges
- Priority levels (LOW, NORMAL, HIGH, URGENT)
- Category organization (GENERAL, SYSTEM, SECURITY, etc.)
- Mark as read tracking
- Notification panel in all dashboards

**No configuration needed!**

---

## 2. Email Notifications 📧

### Setup Instructions

#### A. Using Gmail (Recommended for Testing)

1. **Enable 2-Factor Authentication** on your Gmail account
2. **Generate App Password:**
   - Go to https://myaccount.google.com/apppasswords
   - Select "Mail" and "Other (Custom name)"
   - Enter "AxioBank Server"
   - Copy the 16-character password

3. **Set Environment Variables:**

```bash
# Windows (PowerShell)
$env:SMTP_HOST="smtp.gmail.com"
$env:SMTP_PORT="587"
$env:SMTP_USERNAME="your-email@gmail.com"
$env:SMTP_PASSWORD="your-16-char-app-password"
$env:FROM_EMAIL="noreply@axiobank.com"
$env:FROM_NAME="AxioBank"

# Linux/Mac
export SMTP_HOST="smtp.gmail.com"
export SMTP_PORT="587"
export SMTP_USERNAME="your-email@gmail.com"
export SMTP_PASSWORD="your-16-char-app-password"
export FROM_EMAIL="noreply@axiobank.com"
export FROM_NAME="AxioBank"
```

#### B. Using Other Email Providers

**Office 365:**
```bash
SMTP_HOST=smtp.office365.com
SMTP_PORT=587
SMTP_USERNAME=your-email@company.com
SMTP_PASSWORD=your-password
```

**SendGrid:**
```bash
SMTP_HOST=smtp.sendgrid.net
SMTP_PORT=587
SMTP_USERNAME=apikey
SMTP_PASSWORD=your-sendgrid-api-key
```

**AWS SES:**
```bash
SMTP_HOST=email-smtp.us-east-1.amazonaws.com
SMTP_PORT=587
SMTP_USERNAME=your-ses-access-key
SMTP_PASSWORD=your-ses-secret-key
```

### Email Features

✅ HTML formatted emails with AxioBank branding
✅ Priority badges (🔴 URGENT, 🟠 HIGH, 🔵 NORMAL)
✅ Responsive design
✅ Bulk email support

---

## 3. SMS Notifications 📱

### Setup Instructions

#### A. Using Twilio (International - Recommended)

1. **Sign up** at https://www.twilio.com/
2. **Get credentials** from Twilio Console
3. **Set Environment Variables:**

```bash
# Windows (PowerShell)
$env:SMS_PROVIDER="twilio"
$env:TWILIO_ACCOUNT_SID="your-account-sid"
$env:TWILIO_AUTH_TOKEN="your-auth-token"
$env:TWILIO_FROM_NUMBER="+1234567890"

# Linux/Mac
export SMS_PROVIDER="twilio"
export TWILIO_ACCOUNT_SID="your-account-sid"
export TWILIO_AUTH_TOKEN="your-auth-token"
export TWILIO_FROM_NUMBER="+1234567890"
```

#### B. Using Africa's Talking (Africa - Recommended)

1. **Sign up** at https://africastalking.com/
2. **Get API Key** from dashboard
3. **Set Environment Variables:**

```bash
# Windows (PowerShell)
$env:SMS_PROVIDER="africas_talking"
$env:AT_USERNAME="your-username"
$env:AT_API_KEY="your-api-key"

# Linux/Mac
export SMS_PROVIDER="africas_talking"
export AT_USERNAME="your-username"
export AT_API_KEY="your-api-key"
```

### SMS Features

✅ 160-character optimized messages
✅ Priority prefixes ([URGENT], [IMPORTANT])
✅ Automatic truncation
✅ Bulk SMS support
✅ Multiple provider support (Twilio, Africa's Talking)

---

## 4. Testing the System

### Step 1: Start the Server

```bash
cd "C:\Users\ADMIN\AxionBank\Axio Bank"
./gradlew run
```

**Check logs for:**
```
✅ Email configured: SMTP connected
✅ SMS configured: Provider ready
⚠️ Email not configured (if SMTP not set up)
⚠️ SMS not configured (if SMS provider not set up)
```

### Step 2: Login as Admin

1. Start client application
2. Login as admin
3. Go to **Quick Actions** or **Notifications Tab**
4. Click **"Send Notification"**

### Step 3: Send Test Notification

**Test Configuration:**
- **Recipients:** All Employees
- **Priority:** HIGH
- **Category:** SYSTEM
- **Title:** "System Test Notification"
- **Message:** "This is a test of the multi-channel notification system."
- **Channels:**
  - ☑ In-App
  - ☑ Email (if configured)
  - ☑ SMS (if configured)

### Step 4: Verify Delivery

**In-App:**
- Login as any employee (teller1, cso1, etc.)
- Check notification bell icon
- Should show unread badge
- Click to view notification

**Email:**
- Check recipient's email inbox
- Should receive HTML-formatted email
- Verify AxioBank branding

**SMS:**
- Check recipient's phone
- Should receive SMS with priority prefix
- Message under 160 characters

---

## 5. Troubleshooting

### Email Not Sending

**Error:** "SMTP not configured"
- **Fix:** Set all required environment variables (SMTP_HOST, SMTP_PORT, SMTP_USERNAME, SMTP_PASSWORD)

**Error:** "Authentication failed"
- **Fix:** If using Gmail, generate App Password (don't use account password)
- **Fix:** Check credentials are correct

**Error:** "Connection timeout"
- **Fix:** Check firewall allows outbound port 587
- **Fix:** Try port 465 (SSL) instead

### SMS Not Sending

**Error:** "SMS provider not configured"
- **Fix:** Set SMS_PROVIDER and provider-specific credentials

**Error:** "Invalid credentials"
- **Fix:** Verify API keys from provider dashboard
- **Fix:** Check account has sufficient balance

**Error:** "Invalid phone number"
- **Fix:** Use E.164 format (+[country code][number])
- **Example:** +254712345678 (Kenya), +1234567890 (US)

### In-App Notifications Not Showing

- Check user has permissions for the dashboard
- Check notification bell is visible in sidebar
- Refresh the page
- Check server logs for errors

---

## 6. Production Recommendations

### Email

✅ Use dedicated SMTP service (SendGrid, AWS SES, Mailgun)
✅ Set up SPF, DKIM, and DMARC records
✅ Monitor delivery rates
✅ Handle bounces and unsubscribes
✅ Rate limit bulk sends

### SMS

✅ Use reliable provider with good uptime
✅ Monitor costs (SMS can be expensive)
✅ Implement opt-out mechanism
✅ Cache phone numbers to avoid repeated API calls
✅ Consider message templates to reduce length

### Security

✅ Store credentials in secure vault (AWS Secrets Manager, Azure Key Vault)
✅ Rotate API keys regularly
✅ Use environment-specific configurations
✅ Log all notification attempts
✅ Implement rate limiting

---

## 7. Cost Estimates

### Email (Per 1000 emails)

- **SendGrid:** $0 (12k free/month), then $15/mo
- **AWS SES:** $0.10
- **Mailgun:** $0 (5k free/month), then $35/mo
- **Gmail:** Free (but not for production)

### SMS (Per message)

- **Twilio:** $0.0075 (US), varies by country
- **Africa's Talking:** $0.01 (Kenya), varies by country
- **AWS SNS:** $0.00645 (US)

**Monthly estimate for 10,000 notifications:**
- All In-App: $0
- All Email: $1-10
- All SMS: $75-100
- Mixed (50% each): $40-50

---

## 8. Environment Variables Reference

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `SMTP_HOST` | For Email | smtp.gmail.com | SMTP server hostname |
| `SMTP_PORT` | For Email | 587 | SMTP server port |
| `SMTP_USERNAME` | For Email | - | SMTP username/email |
| `SMTP_PASSWORD` | For Email | - | SMTP password/API key |
| `FROM_EMAIL` | No | noreply@axiobank.com | Sender email address |
| `FROM_NAME` | No | AxioBank | Sender display name |
| `SMS_PROVIDER` | For SMS | twilio | SMS provider (twilio/africas_talking) |
| `TWILIO_ACCOUNT_SID` | For Twilio | - | Twilio Account SID |
| `TWILIO_AUTH_TOKEN` | For Twilio | - | Twilio Auth Token |
| `TWILIO_FROM_NUMBER` | For Twilio | - | Twilio phone number |
| `AT_USERNAME` | For AT | - | Africa's Talking username |
| `AT_API_KEY` | For AT | - | Africa's Talking API key |

---

## 9. Quick Start (Development)

**Minimum setup for testing:**

```bash
# In-App only (no setup needed)
# Just start the server and client!

# Add Email (Gmail - 2 minutes)
$env:SMTP_USERNAME="your-gmail@gmail.com"
$env:SMTP_PASSWORD="your-app-password"

# Add SMS (Twilio - 5 minutes)
$env:SMS_PROVIDER="twilio"
$env:TWILIO_ACCOUNT_SID="your-sid"
$env:TWILIO_AUTH_TOKEN="your-token"
$env:TWILIO_FROM_NUMBER="+1234567890"
```

---

## Support

For issues or questions:
- Check server logs for detailed error messages
- Verify environment variables are set correctly
- Test with In-App only first, then add Email, then SMS
- Consult provider documentation for provider-specific issues

**The system is designed to gracefully degrade:**
- If Email/SMS not configured → Only In-App notifications sent
- If Email configured but fails → In-App still delivered
- If SMS configured but fails → In-App and Email still delivered

This ensures users always receive notifications even if external services fail!
