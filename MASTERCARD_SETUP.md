# Mastercard Card Issuance Setup Guide

This guide will help you configure AxionBank to issue real Mastercard debit and credit cards to your customers.

## 📋 Prerequisites

1. **Mastercard Developer Account**
   - Sign up at https://developer.mastercard.com
   - Complete issuer registration
   - Get approved as a card issuer

2. **Required Credentials**
   - Consumer Key
   - P12 Certificate (Keystore)
   - Keystore Password
   - BIN Range (Bank Identification Number)
   - ICA Number (Interbank Card Association)

## 🔧 Configuration Steps

### 1. Get Your Mastercard Credentials

#### A. Consumer Key
1. Log in to Mastercard Developer Portal
2. Go to your project dashboard
3. Copy the **Consumer Key**
4. Update in `server/src/main/resources/mastercard.properties`:
   ```properties
   mastercard.consumer.key=YOUR_ACTUAL_CONSUMER_KEY_HERE
   ```

#### B. Download P12 Certificate
1. In Mastercard Developer Portal, go to **Keys & Certificates**
2. Download your `.p12` keystore file
3. Save it to: `Axio Bank/api/axiobank-sandbox.p12`
4. Note the keystore password

#### C. Get BIN Range
1. Contact Mastercard to request a BIN range
2. For sandbox: Use test BIN `555555`
3. For production: Use your assigned BIN
4. Update in `mastercard.properties`:
   ```properties
   mastercard.bin.range=YOUR_BIN_RANGE
   ```

#### D. Get ICA Number
1. Your ICA (Issuer ICA) is assigned when you register as an issuer
2. Update in `mastercard.properties`:
   ```properties
   mastercard.ica=YOUR_ICA_NUMBER
   ```

### 2. Update Configuration File

Edit `server/src/main/resources/mastercard.properties`:

```properties
# Environment: sandbox or production
mastercard.environment=sandbox

# Consumer Key from Mastercard Developer Portal
mastercard.consumer.key=YOUR_CONSUMER_KEY_FROM_PORTAL

# Keystore Configuration
mastercard.keystore.path=C:/Users/ADMIN/AxionBank/Axio Bank/api/axiobank-sandbox.p12
mastercard.keystore.password=YOUR_KEYSTORE_PASSWORD
mastercard.keystore.alias=YOUR_KEY_ALIAS

# BIN Range assigned by Mastercard
mastercard.bin.range=555555

# ICA Number from Mastercard
mastercard.ica=YOUR_ICA_NUMBER
```

### 3. Environment Variables (Optional - More Secure)

Instead of hardcoding credentials, use environment variables:

**Windows:**
```cmd
setx MASTERCARD_CONSUMER_KEY "your_consumer_key"
setx MASTERCARD_KEYSTORE_PASSWORD "your_password"
setx MASTERCARD_BIN_RANGE "your_bin_range"
setx MASTERCARD_ICA "your_ica"
```

**Linux/Mac:**
```bash
export MASTERCARD_CONSUMER_KEY="your_consumer_key"
export MASTERCARD_KEYSTORE_PASSWORD="your_password"
export MASTERCARD_BIN_RANGE="your_bin_range"
export MASTERCARD_ICA="your_ica"
```

## 📦 Certificate Files

Ensure all certificate files are in `Axio Bank/api/`:

```
api/
├── axiobank-sandbox.p12                                    # Main keystore
├── axiobank-mastercard-encryption-key.p12                 # Encryption key
├── mastercard-processing-debitClientEnc1770499674069.pem  # Debit API cert
├── mastercard-processing-creditClientEnc1770499633990.pem # Credit API cert
├── mastercard-processing-authenticationClientEnc1770499591539.pem  # 3DS cert
├── mastercard-account-validationClientEnc1770499573730.pem # Validation cert
└── transaction-notifications-axiobank-sandbox-mastercard-encryption-key.p12
```

## 🧪 Testing in Sandbox

### 1. Verify Configuration

Start the server and check logs:
```
🔧 Initializing Mastercard Issuance Service...
=== Mastercard Configuration ===
Environment: sandbox
Base URL: https://sandbox.api.mastercard.com
Consumer Key: YOUR_CONSU...
✅ Mastercard configuration validated successfully
```

### 2. Issue a Test Debit Card

**API Request:**
```bash
POST http://localhost:8081/api/v1/cards/issue
Content-Type: application/json

{
  "userId": "user-uuid",
  "cardHolderName": "John Doe",
  "cardType": "DEBIT",
  "linkedAccountId": "account-uuid",
  "linkedAccountNumber": "1234567890",
  "deliveryMethod": "VIRTUAL",
  "billingAddress": {
    "streetAddress": "123 Main St",
    "city": "Nairobi",
    "state": "Nairobi",
    "zipCode": "00100",
    "country": "KE"
  }
}
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Card issued successfully",
  "card": {
    "cardId": "mc-token-uuid",
    "maskedCardNumber": "**** **** **** 4567",
    "cardType": "DEBIT",
    "cardBrand": "MASTERCARD",
    "status": "PENDING_ACTIVATION",
    "deliveryMethod": "VIRTUAL",
    "estimatedDelivery": "Instant"
  }
}
```

### 3. Activate Card

**API Request:**
```bash
POST http://localhost:8081/api/v1/cards/issue/activate
Content-Type: application/json

{
  "cardId": "card-uuid",
  "pin": "1234"
}
```

### 4. Test Card Transaction

Use the issued card for a test transaction through Card Transactions screen.

## 🚀 Moving to Production

### 1. Switch Environment

Update `mastercard.properties`:
```properties
mastercard.environment=production
mastercard.production.url=https://api.mastercard.com
```

### 2. Update Credentials

- Get production consumer key
- Get production BIN range
- Get production ICA number
- Download production certificates

### 3. PCI DSS Compliance

**CRITICAL:** Before going to production:

1. **Get PCI DSS Certified**
   - Level 1 certification required for card issuers
   - Annual security audit
   - Penetration testing

2. **Secure Certificate Storage**
   - Use Hardware Security Module (HSM)
   - Encrypt certificates at rest
   - Implement key rotation

3. **Secure PIN Handling**
   - Never store PINs in plaintext
   - Use DUKPT encryption for PIN entry
   - Implement PIN change workflow

4. **Transaction Security**
   - Enable 3D Secure for online transactions
   - Implement fraud detection
   - Set up real-time alerts

## 🔒 Security Best Practices

### 1. Keystore Security
```kotlin
// ❌ DON'T: Hardcode passwords
val password = "mypassword"

// ✅ DO: Use environment variables
val password = System.getenv("MASTERCARD_KEYSTORE_PASSWORD")
```

### 2. Certificate Permissions
```bash
# Set restrictive permissions
chmod 600 api/*.p12
chmod 600 api/*.pem
```

### 3. Audit Logging
```kotlin
// Log all card issuance events
println("🎴 [AUDIT] Card issued - User: $userId, Type: $cardType, Time: ${Instant.now()}")
```

### 4. Rate Limiting
Implement rate limiting for card issuance endpoints to prevent abuse.

## 📊 Card Limits Configuration

### Debit Cards
```properties
mastercard.debit.daily.withdrawal=5000.00
mastercard.debit.daily.purchase=10000.00
mastercard.debit.daily.transaction=15000.00
```

### Credit Cards
```properties
mastercard.credit.default.limit=5000.00
mastercard.credit.gold.limit=10000.00
mastercard.credit.platinum.limit=25000.00
mastercard.credit.world.limit=50000.00
```

## 🐛 Troubleshooting

### Issue: "Invalid OAuth signature"
**Solution:** Check your consumer key and keystore password

### Issue: "BIN range not assigned"
**Solution:** Contact Mastercard to get your BIN range

### Issue: "Certificate expired"
**Solution:** Download new certificates from Mastercard portal

### Issue: "ICA not found"
**Solution:** Verify your ICA number from Mastercard registration

## 📞 Support

### Mastercard Support
- Developer Portal: https://developer.mastercard.com
- Email: developer@mastercard.com
- Phone: Check regional numbers on portal

### AxionBank Technical Support
- For integration issues, check logs
- Enable debug logging:
  ```properties
  mastercard.logging.enabled=true
  mastercard.logging.level=DEBUG
  ```

## 📚 API Endpoints

### Card Issuance
- `POST /api/v1/cards/issue` - Issue debit/credit card
- `POST /api/v1/cards/issue/activate` - Activate card with PIN
- `GET /api/v1/cards/issue/{cardId}/details` - Get card details
- `POST /api/v1/cards/issue/suspend` - Suspend card

### Card Management
- `GET /api/v1/cards/user/{userId}` - List user's cards
- `DELETE /api/v1/cards/{cardId}` - Delete card

## ✅ Pre-Launch Checklist

- [ ] Mastercard developer account created
- [ ] Issuer registration completed
- [ ] Consumer key obtained
- [ ] P12 certificate downloaded
- [ ] BIN range assigned
- [ ] ICA number obtained
- [ ] Configuration file updated
- [ ] Certificates placed in api folder
- [ ] Sandbox testing completed
- [ ] PCI DSS compliance achieved
- [ ] Production credentials obtained
- [ ] Security audit passed
- [ ] Fraud detection implemented
- [ ] Monitoring and alerts set up

## 🎉 Ready to Go!

Once all checklist items are complete, you're ready to issue real Mastercard cards to your customers!

---

**⚠️ IMPORTANT:** This is a real banking system. Never commit sensitive credentials to version control. Always use environment variables or secure vaults for production credentials.
