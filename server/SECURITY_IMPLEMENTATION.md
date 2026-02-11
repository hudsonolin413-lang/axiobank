# Security Implementation Guide

## Overview
This document describes the security measures implemented to protect sensitive API endpoints and enforce domain-based access control for the AxionBank server.

## Security Features

### 1. **CORS Configuration**
The server is configured with restrictive CORS policies in `Application.kt`:

- **Allowed Domains:**
  - `axionbank.com` (public website)
  - `axiobank.com` (public website)
  - `localhost` (development only)

- **Blocked:** All other domains are blocked by default

### 2. **Authentication Middleware**
Location: `plugins/Security.kt`

#### `requireAuth()`
Validates session tokens from the `Authorization` header.
- Checks if token exists
- Validates token against database
- Ensures token is not expired
- Returns `401 Unauthorized` if validation fails

**Usage Example:**
```kotlin
get("/protected-endpoint") {
    val sessionId = call.requireAuth() ?: return@get
    // Your protected logic here
}
```

### 3. **Domain-Based Access Control**
Location: `plugins/Security.kt`

#### `requireDomain(vararg allowedDomains: String)`
Restricts endpoints to specific domains (e.g., employee portal only).
- Checks `Origin`, `Referer`, and `Host` headers
- Returns `403 Forbidden` if domain not allowed

**Usage Example:**
```kotlin
get("/employee-only") {
    if (!call.requireDomain("axiobank.com")) return@get
    // Your employee-only logic here
}
```

#### `requireEmployeeAccess(vararg allowedDomains: String)`
Combined middleware for employee/admin routes.
- First validates domain
- Then validates authentication
- Returns null if either check fails

**Usage Example:**
```kotlin
get("/admin/sensitive-data") {
    val sessionId = call.requireEmployeeAccess("axiobank.com") ?: return@get
    // Your admin logic here
}
```

### 4. **Rate Limiting**
Location: `plugins/Security.kt`

#### `checkRateLimit(identifier, config)`
Protects endpoints from abuse by limiting requests per time window.
- Default: 100 requests per minute per IP
- Customizable per endpoint
- Returns `429 Too Many Requests` if limit exceeded

**Usage Example:**
```kotlin
post("/api/login") {
    if (!call.checkRateLimit()) return@post
    // Your login logic here
}
```

## Recommended Protection Levels

### **Public Endpoints (No Protection)**
- `/` - Homepage
- `/about`, `/services`, `/privacy`, `/terms` - Static pages
- `/health` - Health check
- `/api/customer/register` - Customer registration
- `/api/customer/login` - Customer login

### **Customer Endpoints (Auth Required)**
- `/api/customer/profile/*` - Customer profile data
- `/api/accounts/*` - Account operations
- `/api/transactions/*` - Transaction history
- `/api/loans/applications` - Loan applications

### **Employee/Admin Endpoints (Auth + Domain Required)**
Restrict to `axiobank.com` domain:
- `/api/v1/employees/*` - Employee management
- `/api/v1/admin/*` - Admin dashboard
- `/api/v1/customer-care/*` - Customer care operations
- `/api/v1/kyc/*` - KYC verification
- `/api/v1/teller/*` - Teller operations
- `/api/v1/master-wallet/*` - Master wallet operations
- `/api/v1/dashboard/*` - Dashboard analytics

### **Financial/Sensitive Endpoints (Auth + Domain + Rate Limit)**
Restrict to `axiobank.com` with rate limiting:
- `/api/v1/mpesa/*` - M-Pesa integration
- `/api/v1/loans/*/approve` - Loan approvals
- `/api/v1/transactions/reversal` - Transaction reversals

## Implementation Steps

### Step 1: Import Security Middleware
Add to the top of `Routing.kt`:
```kotlin
import org.dals.project.plugins.*
```

### Step 2: Protect Employee Routes
Wrap employee/admin routes with domain + auth:
```kotlin
route("/employees") {
    get {
        val sessionId = call.requireEmployeeAccess("axiobank.com") ?: return@get
        // ... rest of logic
    }
}
```

### Step 3: Protect Customer Routes
Wrap customer routes with auth only:
```kotlin
route("/customer/profile") {
    get {
        val sessionId = call.requireAuth() ?: return@get
        // ... rest of logic
    }
}
```

### Step 4: Add Rate Limiting to Sensitive Operations
Add rate limiting to login, registration, and financial operations:
```kotlin
post("/login") {
    if (!call.checkRateLimit()) return@post
    // ... rest of logic
}
```

## Security Best Practices

1. **Never expose sensitive data on public endpoints**
   - Customer lists, employee data, financial reports should require auth + domain restriction

2. **Use HTTPS in production**
   - Update CORS schemes to only allow `https` in production
   - Remove localhost entries from CORS in production

3. **Implement proper session management**
   - Sessions should expire after inactivity
   - Logout should invalidate sessions immediately
   - Use secure, random session IDs

4. **Log security events**
   - Log failed authentication attempts
   - Log rate limit violations
   - Monitor for suspicious patterns

5. **Regular security audits**
   - Review endpoints regularly for proper protection
   - Test security measures with penetration testing
   - Keep dependencies updated

## Domain Separation

### AxionBank.com (Public Website)
**Accessible APIs:**
- Customer registration
- Customer login
- Public pages (about, services, privacy)
- Customer account operations (with auth)
- Customer loan applications (with auth)

**Blocked APIs:**
- Employee management
- Admin dashboard
- Customer care operations
- KYC verification
- Teller operations
- Master wallet operations

### AxioBank.com (Employee Portal)
**Accessible APIs:**
- All employee operations (with auth)
- Admin dashboard (with auth)
- Customer care operations (with auth)
- KYC verification (with auth)
- Teller operations (with auth)
- Master wallet operations (with auth)

**Note:** Employee portal can also access customer-facing APIs for support purposes

## Testing Security

### Test Authentication
```bash
# Should fail without token
curl http://localhost:8081/api/v1/employees

# Should succeed with valid token
curl -H "Authorization: Bearer <valid-token>" http://localhost:8081/api/v1/employees
```

### Test Domain Restriction
```bash
# Should fail from wrong domain
curl -H "Origin: http://malicious.com" http://localhost:8081/api/v1/admin/dashboard

# Should succeed from allowed domain
curl -H "Origin: http://axiobank.com" -H "Authorization: Bearer <valid-token>" http://localhost:8081/api/v1/admin/dashboard
```

### Test Rate Limiting
```bash
# Run this in a loop - should fail after 100 requests
for i in {1..150}; do curl http://localhost:8081/api/customer/login; done
```

## Migration Notes

**IMPORTANT:** To apply these security measures:

1. The security middleware has been created in `plugins/Security.kt`
2. CORS has been updated in `Application.kt` to restrict domains
3. **Routes need to be updated individually** to add protection

You can choose to:
- **Option A:** Manually add protection to specific routes as needed
- **Option B:** Use the provided examples to protect all sensitive routes in one go
- **Option C:** Create a separate secured routing module and migrate endpoints gradually

The current implementation is **backward compatible** - existing routes will continue to work, but won't be protected until you explicitly add the middleware calls.

## Support

For questions or security concerns, contact the development team.

Last Updated: January 25, 2026
