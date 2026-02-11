# AxionBank Server API

A comprehensive banking system REST API built with Ktor and PostgreSQL, providing full CRUD operations for customers,
accounts, transactions, and loan management.

## Features

- **Automatic Database Setup**: Tables are created automatically when the server starts
- **Sample Data Generation**: Pre-populated with sample data for immediate testing
- **Customer Management**: Create, read, update, and delete customer records
- **Account Management**: Handle various account types (Checking, Savings, Credit, etc.)
- **Transaction Processing**: Process deposits, withdrawals, transfers with real-time balance updates
- **Loan Management**: Loan applications, approvals, and loan tracking
- **Database Integration**: Full PostgreSQL integration with connection pooling

## Database Configuration

The server connects to PostgreSQL with the following configuration:
- **Host**: localhost
- **Port**: 5433
- **Database**: AxionBank
- **Username**: postgres
- **Password**: Andama@95

## Prerequisites

1. **PostgreSQL**: Ensure PostgreSQL is installed and running on port 5433
2. **Java 11+**: Required for running the Ktor server
3. **Create Database**: Create the AxionBank database (tables will be created automatically)

## Quick Start

### 1. Create the Database

Connect to PostgreSQL and create the database:
```sql
CREATE DATABASE "AxionBank";
```

### 2. Start the Server

#### Option A: Using the provided scripts
```bash
# Windows (Command Prompt)
cd "Axio Bank/server"
run-server.bat

# Windows (PowerShell)
cd "Axio Bank/server"
./run-server.ps1
```

#### Option B: Using Gradle directly

```bash
cd "Axio Bank"
./gradlew :server:run
```

The server will:

1. Connect to the PostgreSQL database
2. Automatically create all required tables
3. Insert sample data for testing
4. Start the REST API server on `http://localhost:8080`

### 3. Test the API

Once the server is running, you can test it:

#### Health Check
```bash
curl http://localhost:8080/health
```

#### Get All Customers

```bash
curl http://localhost:8080/api/v1/customers
```

#### Get All Accounts
```bash
curl http://localhost:8080/api/v1/accounts
```

## Sample Data

When the server starts for the first time, it automatically creates sample data:

### Users

- **Admin**: username: `admin`, email: `admin@axionbank.com`
- **Loan Officer**: username: `loan_officer`, email: `loans@axionbank.com`

### Customers

- **Individual Customer**: John Doe (Credit Score: 750)
- **Business Customer**: Sarah Johnson / Tech Solutions Inc (Credit Score: 800)

### Accounts

- **Checking Account**: Balance $800.00
- **Savings Account**: Balance $5,200.00
- **Business Checking**: Balance $25,000.00

### Loans

- **Personal Loan**: $20,000 approved, $18,500 current balance
- **Business Loan Application**: $100,000 requested (under review)

### Transactions

- Initial deposits and transfers between accounts

## API Endpoints

### Health Check
- `GET /health` - Check server status

### Customer Management
- `GET /api/v1/customers` - Get all customers (paginated)
- `GET /api/v1/customers/{id}` - Get customer by ID
- `GET /api/v1/customers/number/{customerNumber}` - Get customer by number
- `POST /api/v1/customers` - Create new customer
- `PUT /api/v1/customers/{id}` - Update customer
- `DELETE /api/v1/customers/{id}` - Delete customer

### Account Management
- `GET /api/v1/accounts` - Get all accounts (paginated)
- `GET /api/v1/accounts/{id}` - Get account by ID
- `GET /api/v1/accounts/number/{accountNumber}` - Get account by number
- `GET /api/v1/accounts/customer/{customerId}` - Get accounts by customer
- `GET /api/v1/accounts/{id}/balance` - Get account balance
- `POST /api/v1/accounts` - Create new account
- `PUT /api/v1/accounts/{id}` - Update account
- `DELETE /api/v1/accounts/{id}` - Delete account

### Transaction Management
- `GET /api/v1/transactions` - Get all transactions (paginated)
- `GET /api/v1/transactions/{id}` - Get transaction by ID
- `GET /api/v1/transactions/account/{accountId}` - Get transactions by account
- `POST /api/v1/transactions` - Create new transaction
- `PATCH /api/v1/transactions/{id}/status` - Update transaction status

### Loan Applications
- `GET /api/v1/loan-applications` - Get all loan applications (paginated)
- `GET /api/v1/loan-applications/{id}` - Get loan application by ID
- `GET /api/v1/loan-applications/customer/{customerId}` - Get applications by customer
- `POST /api/v1/loan-applications` - Create new loan application
- `PATCH /api/v1/loan-applications/{id}/status` - Update application status

### Loans
- `GET /api/v1/loans` - Get all loans (paginated)
- `GET /api/v1/loans/{id}` - Get loan by ID
- `GET /api/v1/loans/customer/{customerId}` - Get loans by customer
- `POST /api/v1/loans/from-application` - Create loan from approved application

## Request/Response Examples

### Create Customer
```bash
curl -X POST http://localhost:8080/api/v1/customers \
  -H "Content-Type: application/json" \
  -d '{
    "type": "INDIVIDUAL",
    "firstName": "Alice",
    "lastName": "Wonder",
    "email": "alice@example.com",
    "phoneNumber": "+1234567890",
    "primaryStreet": "123 Main St",
    "primaryCity": "Anytown",
    "primaryState": "CA",
    "primaryZipCode": "12345",
    "branchId": "use-branch-id-from-sample-data"
  }'
```

### Create Account

First, get a customer ID from the customers endpoint, then:
```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-uuid-from-sample-data",
    "type": "CHECKING",
    "minimumBalance": "100.00",
    "interestRate": "0.0250",
    "branchId": "branch-uuid-from-sample-data"
  }'
```

### Create Transaction (Deposit)
```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": "account-uuid-from-sample-data",
    "type": "DEPOSIT",
    "amount": "500.00",
    "description": "Cash deposit",
    "category": "DEPOSIT"
  }'
```

### Create Loan Application
```bash
curl -X POST http://localhost:8080/api/v1/loan-applications \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-uuid-from-sample-data",
    "loanType": "AUTO_LOAN",
    "requestedAmount": "35000.00",
    "purpose": "Vehicle purchase",
    "annualIncome": "65000.00",
    "employmentHistory": "3 years at current employer"
  }'
```

## Response Format

All API responses follow this standard format:

```json
{
  "success": boolean,
  "message": "string",
  "data": object,
  "error": "string (if error)"
}
```

For paginated endpoints:
```json
{
  "success": boolean,
  "message": "string",
  "data": array,
  "total": integer,
  "page": integer,
  "pageSize": integer
}
```

## Transaction Types

- `DEPOSIT` - Add money to account
- `WITHDRAWAL` - Remove money from account
- `TRANSFER` - Move money between accounts
- `PAYMENT` - Payment transactions
- `INTEREST_CREDIT` - Interest earned
- `FEE_DEBIT` - Fees charged
- `CHECK_DEPOSIT` - Check deposits
- `ATM_WITHDRAWAL` - ATM withdrawals
- `WIRE_TRANSFER` - Wire transfers
- `DIRECT_DEPOSIT` - Direct deposits
- `LOAN_PAYMENT` - Loan payments
- `LOAN_DISBURSEMENT` - Loan disbursements

## Account Types

- `CHECKING` - Standard checking account
- `SAVINGS` - Savings account
- `CREDIT` - Credit account
- `LOAN` - Loan account
- `INVESTMENT` - Investment account
- `BUSINESS_CHECKING` - Business checking account
- `BUSINESS_SAVINGS` - Business savings account

## Loan Types

- `PERSONAL_LOAN` - Personal loans
- `HOME_LOAN` - Home/mortgage loans
- `AUTO_LOAN` - Auto loans
- `BUSINESS_LOAN` - Business loans
- `STUDENT_LOAN` - Student loans
- `CREDIT_CARD` - Credit card accounts
- `LINE_OF_CREDIT` - Lines of credit
- `EQUIPMENT_LOAN` - Equipment financing
- `CONSTRUCTION_LOAN` - Construction loans

## Error Handling

The API returns appropriate HTTP status codes:

- `200 OK` - Success
- `201 Created` - Resource created successfully
- `400 Bad Request` - Invalid request data
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

## Development

### Building
```bash
./gradlew build
```

### Testing
```bash
./gradlew test
```

### Running in Development Mode
```bash
./gradlew :server:run
```

## Architecture

The server follows a clean architecture pattern:
- **Controllers**: Route handlers in `plugins/Routing.kt`
- **Services**: Business logic in `services/` package
- **Models**: Data transfer objects in `models/` package
- **Database**: ORM mappings and database access in `database/` package

## Troubleshooting

### Database Connection Issues

1. Make sure PostgreSQL is running on port 5433
2. Verify the AxionBank database exists
3. Check username/password credentials
4. Ensure PostgreSQL accepts connections on localhost

### Port Already in Use

If port 8080 is already in use, you can change it in `Application.kt`:

```kotlin
embeddedServer(Netty, port = 8081, host = "0.0.0.0", module = Application::module)
```

### Sample Data Not Loading

- Check the console output for any database errors
- Verify all table creation completed successfully
- Look for any constraint violations in the logs

## Security Notes

- Database credentials are currently hardcoded for development
- In production, use environment variables for sensitive configuration
- Consider implementing authentication and authorization
- Add input validation and sanitization
- Implement rate limiting for production use

## License

This project is part of the AxionBank application suite.