-- ============================================================================
-- AXIONBANK CUSTOMER CARE DATA SETUP SCRIPT
-- ============================================================================
-- Execute this script after the server starts to add customer care data
-- Connect to your H2 database and run these INSERT statements

-- First, get the branch ID (assuming main branch exists)
-- You may need to adjust the branch_id values based on your actual branch UUIDs

-- ============================================================================
-- ACCOUNT TYPES (Available for Customer Care Account Opening)
-- ============================================================================

INSERT INTO account_types (id, type_name, display_name, description, minimum_deposit, minimum_balance,
                           interest_rate, monthly_maintenance_fee, overdraft_limit, features, is_active,
                           category, max_transactions_per_month, atm_withdrawal_limit, online_banking_enabled,
                           mobile_banking_enabled, check_book_available, debit_card_available, created_at, updated_at)
VALUES
-- Personal Savings Account
(RANDOM_UUID(), 'SAVINGS', 'Personal Savings Account',
 'Earn interest on your deposits with our standard savings account',
 100.00, 25.00, 0.025, 0.00, NULL,
 '["Interest earning", "Mobile banking", "ATM access", "Online banking"]',
 TRUE, 'PERSONAL', NULL, 500.00, TRUE, TRUE, TRUE, TRUE,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Personal Checking Account
(RANDOM_UUID(), 'CHECKING', 'Personal Checking Account',
 'Full-service checking account for everyday banking needs',
 50.00, 0.00, 0.0025, 5.00, 100.00,
 '["Unlimited transactions", "Debit card", "Mobile banking", "Check writing", "Online bill pay"]',
 TRUE, 'PERSONAL', NULL, 800.00, TRUE, TRUE, TRUE, TRUE,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Business Checking Account
(RANDOM_UUID(), 'BUSINESS_CHECKING', 'Business Checking Account',
 'Professional checking account designed for business operations',
 500.00, 100.00, 0.01, 15.00, 1000.00,
 '["Business features", "Higher limits", "Merchant services", "Business debit card", "Cash management"]',
 TRUE, 'BUSINESS', 200, 2000.00, TRUE, TRUE, TRUE, TRUE,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Business Savings Account
(RANDOM_UUID(), 'BUSINESS_SAVINGS', 'Business Savings Account',
 'High-yield savings account for business funds',
 1000.00, 500.00, 0.0175, 10.00, NULL,
 '["Business savings features", "Higher interest rates", "Business banking", "Sweep accounts"]',
 TRUE, 'BUSINESS', 6, 1000.00, TRUE, TRUE, TRUE, TRUE,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Premium Checking Account
(RANDOM_UUID(), 'PREMIUM_CHECKING', 'Premium Checking Account',
 'Premium account with enhanced benefits and higher limits',
 1000.00, 500.00, 0.005, 25.00, 2000.00,
 '["Premium benefits", "Higher transaction limits", "Priority customer service", "Travel benefits"]',
 TRUE, 'PERSONAL', NULL, 1500.00, TRUE, TRUE, TRUE, TRUE,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Student Checking Account
(RANDOM_UUID(), 'STUDENT_CHECKING', 'Student Checking Account',
 'Special checking account for students with no monthly fees',
 0.00, 0.00, 0.001, 0.00, 50.00,
 '["No monthly fees", "Student benefits", "Mobile banking", "ATM access", "Study abroad support"]',
 TRUE, 'PERSONAL', NULL, 300.00, TRUE, TRUE, TRUE, TRUE,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Money Market Account
(RANDOM_UUID(), 'MONEY_MARKET', 'Money Market Account',
 'High-yield account combining features of savings and checking',
 2500.00, 1000.00, 0.0325, 12.00, NULL,
 '["High interest rates", "Limited check writing", "Tiered interest", "ATM access"]',
 TRUE, 'PERSONAL', 6, 1000.00, TRUE, TRUE, FALSE, TRUE,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================================
-- SAMPLE CUSTOMERS
-- ============================================================================

INSERT INTO customers (id, customer_number, username, password_hash, type, status,
                       first_name, last_name, middle_name, date_of_birth, ssn, email, phone_number,
                       alternate_phone, primary_street, primary_city, primary_state, primary_zip_code,
                       primary_country, occupation, employer, annual_income, credit_score,
                       branch_id, risk_level, kyc_status, created_at, updated_at, onboarded_date)
VALUES
-- Customer 1: John Doe
(RANDOM_UUID(), 'CUST001', 'john_doe',
 '$2a$10$N.zmdr9k7uOa4YLHOy1g6.ILwB8Qk7rCXqrBqyG8wGw6FTFyKh5Je', -- password: password123
 'INDIVIDUAL', 'ACTIVE', 'John', 'Doe', 'Michael', '1985-06-15',
 '123-45-6789', 'john.doe@email.com', '+1-555-0101', '+1-555-0102',
 '123 Main St', 'New York', 'NY', '10001', 'USA',
 'Software Engineer', 'Tech Corp', 75000.00, 720,
 (SELECT id FROM branches LIMIT 1), 'LOW', 'VERIFIED',
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_DATE),
-- Customer 2: Jane Smith  
(RANDOM_UUID(), 'CUST002', 'jane_smith',
 '$2a$10$N.zmdr9k7uOa4YLHOy1g6.ILwB8Qk7rCXqrBqyG8wGw6FTFyKh5Je', -- password: password123
 'INDIVIDUAL', 'ACTIVE', 'Jane', 'Smith', 'Elizabeth', '1990-03-22',
 '987-65-4321', 'jane.smith@email.com', '+1-555-0103', '+1-555-0104',
 '456 Oak Ave', 'New York', 'NY', '10002', 'USA',
 'Marketing Manager', 'Marketing Inc', 65000.00, 680,
 (SELECT id FROM branches LIMIT 1), 'MEDIUM', 'PENDING',
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_DATE),
-- Customer 3: Bob Johnson (Business)
(RANDOM_UUID(), 'CUST003', 'bob_johnson',
 '$2a$10$N.zmdr9k7uOa4YLHOy1g6.ILwB8Qk7rCXqrBqyG8wGw6FTFyKh5Je', -- password: password123
 'BUSINESS', 'ACTIVE', 'Bob', 'Johnson', NULL, '1978-11-08',
 '555-66-7777', 'bob.johnson@businesscorp.com', '+1-555-0105', NULL,
 '789 Business Blvd', 'New York', 'NY', '10003', 'USA',
 'Business Owner', 'Johnson Enterprises', 120000.00, 750,
 (SELECT id FROM branches LIMIT 1), 'HIGH', 'VERIFIED',
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_DATE),
-- Customer 4: Alice Brown
(RANDOM_UUID(), 'CUST004', 'alice_brown',
 '$2a$10$N.zmdr9k7uOa4YLHOy1g6.ILwB8Qk7rCXqrBqyG8wGw6FTFyKh5Je', -- password: password123
 'INDIVIDUAL', 'ACTIVE', 'Alice', 'Brown', 'Marie', '1992-07-14',
 '111-22-3333', 'alice.brown@email.com', '+1-555-0107', '+1-555-0108',
 '321 Pine St', 'New York', 'NY', '10004', 'USA',
 'Teacher', 'NYC Public Schools', 52000.00, 695,
 (SELECT id FROM branches LIMIT 1), 'LOW', 'VERIFIED',
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_DATE),
-- Customer 5: Charlie Wilson
(RANDOM_UUID(), 'CUST005', 'charlie_wilson',
 '$2a$10$N.zmdr9k7uOa4YLHOy1g6.ILwB8Qk7rCXqrBqyG8wGw6FTFyKh5Je', -- password: password123
 'BUSINESS', 'ACTIVE', 'Charlie', 'Wilson', 'Robert', '1980-12-03',
 '444-55-6666', 'charlie.wilson@wilsonco.com', '+1-555-0109', NULL,
 '654 Commerce St', 'New York', 'NY', '10005', 'USA',
 'Consultant', 'Wilson Consulting LLC', 85000.00, 710,
 (SELECT id FROM branches LIMIT 1), 'MEDIUM', 'PENDING',
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_DATE);

-- ============================================================================
-- SAMPLE SERVICE REQUESTS
-- ============================================================================

INSERT INTO service_requests (id, customer_id, request_type, title, description, status, priority,
                              created_by, assigned_to, estimated_completion_date, approval_required,
                              created_at, updated_at)
VALUES (RANDOM_UUID(),
        (SELECT id FROM customers WHERE customer_number = 'CUST001'),
        'ATM_CARD_REQUEST',
        'Request for New ATM Card',
        'Customer needs a replacement ATM card due to damage',
        'PENDING',
        'MEDIUM',
        (SELECT id FROM users WHERE username = 'cso1'),
        (SELECT id FROM users WHERE username = 'cso1'),
        DATEADD('DAY', 3, CURRENT_TIMESTAMP),
        FALSE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP),
       (RANDOM_UUID(),
        (SELECT id FROM customers WHERE customer_number = 'CUST002'),
        'PIN_RESET',
        'ATM PIN Reset Request',
        'Customer forgot ATM PIN and needs reset',
        'IN_PROGRESS',
        'HIGH',
        (SELECT id FROM users WHERE username = 'cso1'),
        (SELECT id FROM users WHERE username = 'teller1'),
        DATEADD('DAY', 1, CURRENT_TIMESTAMP),
        FALSE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP),
       (RANDOM_UUID(),
        (SELECT id FROM customers WHERE customer_number = 'CUST003'),
        'ACCOUNT_OPENING',
        'Business Savings Account Opening',
        'Request to open additional business savings account',
        'COMPLETED',
        'HIGH',
        (SELECT id FROM users WHERE username = 'cso1'),
        (SELECT id FROM users WHERE username = 'manager1'),
        DATEADD('DAY', 5, CURRENT_TIMESTAMP),
        TRUE,
        DATEADD('DAY', -2, CURRENT_TIMESTAMP),
        CURRENT_TIMESTAMP);

-- ============================================================================
-- SAMPLE COMPLAINTS
-- ============================================================================

INSERT INTO service_requests (id, customer_id, request_type, title, description, status, priority,
                              created_by, assigned_to, estimated_completion_date, approval_required,
                              created_at, updated_at)
VALUES (RANDOM_UUID(),
        (SELECT id FROM customers WHERE customer_number = 'CUST001'),
        'COMPLAINT',
        'Unauthorized Transaction Complaint',
        'Customer reports unauthorized transaction on account. Amount: $150.00. Transaction date: ' || CURRENT_DATE,
        'PENDING',
        'HIGH',
        (SELECT id FROM users WHERE username = 'cso1'),
        (SELECT id FROM users WHERE username = 'manager1'),
        DATEADD('DAY', 7, CURRENT_TIMESTAMP),
        TRUE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP),
       (RANDOM_UUID(),
        (SELECT id FROM customers WHERE customer_number = 'CUST004'),
        'COMPLAINT',
        'ATM Fee Dispute',
        'Customer disputes ATM fees charged on recent transactions',
        'IN_PROGRESS',
        'MEDIUM',
        (SELECT id FROM users WHERE username = 'cso1'),
        (SELECT id FROM users WHERE username = 'cso1'),
        DATEADD('DAY', 5, CURRENT_TIMESTAMP),
        FALSE,
        DATEADD('DAY', -1, CURRENT_TIMESTAMP),
        CURRENT_TIMESTAMP);

-- ============================================================================
-- SAMPLE ACCOUNTS FOR CUSTOMERS
-- ============================================================================

INSERT INTO accounts (id, account_number, customer_id, type, status, balance, available_balance,
                      minimum_balance, interest_rate, branch_id, account_manager_id,
                      is_joint_account, created_at, updated_at, opened_date)
VALUES
-- John Doe's checking account
(RANDOM_UUID(),
 'ACC' || CAST(RANDOM() * 1000000 AS INT),
 (SELECT id FROM customers WHERE customer_number = 'CUST001'),
 'CHECKING',
 'ACTIVE',
 2500.75,
 2500.75,
 100.00,
 0.0050,
 (SELECT id FROM branches LIMIT 1),
 (SELECT id FROM users WHERE username = 'manager1'),
 FALSE,
 CURRENT_TIMESTAMP,
 CURRENT_TIMESTAMP,
 CURRENT_DATE),
-- Jane Smith's savings account
(RANDOM_UUID(),
 'ACC' || CAST(RANDOM() * 1000000 AS INT),
 (SELECT id FROM customers WHERE customer_number = 'CUST002'),
 'SAVINGS',
 'ACTIVE',
 5750.25,
 5750.25,
 500.00,
 0.0200,
 (SELECT id FROM branches LIMIT 1),
 (SELECT id FROM users WHERE username = 'manager1'),
 FALSE,
 CURRENT_TIMESTAMP,
 CURRENT_TIMESTAMP,
 CURRENT_DATE),
-- Bob Johnson's business checking
(RANDOM_UUID(),
 'ACC' || CAST(RANDOM() * 1000000 AS INT),
 (SELECT id FROM customers WHERE customer_number = 'CUST003'),
 'BUSINESS_CHECKING',
 'ACTIVE',
 15750.50,
 15750.50,
 1000.00,
 0.0100,
 (SELECT id FROM branches LIMIT 1),
 (SELECT id FROM users WHERE username = 'manager1'),
 FALSE,
 CURRENT_TIMESTAMP,
 CURRENT_TIMESTAMP,
 CURRENT_DATE);

-- ============================================================================
-- SAMPLE TRANSACTIONS
-- ============================================================================

INSERT INTO transactions (id, account_id, type, amount, status, description, balance_after,
                          processed_by, timestamp, created_at)
VALUES
-- Recent transactions for testing
(RANDOM_UUID(),
 (SELECT id FROM accounts WHERE customer_id = (SELECT id FROM customers WHERE customer_number = 'CUST001') LIMIT 1),
 'DEPOSIT',
 500.00,
 'COMPLETED',
 'Cash deposit at branch',
 2500.75,
 (SELECT id FROM users WHERE username = 'teller1'),
 CURRENT_TIMESTAMP,
 CURRENT_TIMESTAMP),
(RANDOM_UUID(),
 (SELECT id FROM accounts WHERE customer_id = (SELECT id FROM customers WHERE customer_number = 'CUST002') LIMIT 1),
 'WITHDRAWAL',
 150.00,
 'COMPLETED',
 'ATM withdrawal',
 5750.25,
 NULL,
 DATEADD('HOUR', -2, CURRENT_TIMESTAMP),
 DATEADD('HOUR', -2, CURRENT_TIMESTAMP)),
(RANDOM_UUID(),
 (SELECT id FROM accounts WHERE customer_id = (SELECT id FROM customers WHERE customer_number = 'CUST003') LIMIT 1),
 'TRANSFER',
 1000.00,
 'COMPLETED',
 'Wire transfer to supplier',
 15750.50,
 (SELECT id FROM users WHERE username = 'teller1'),
 DATEADD('HOUR', -4, CURRENT_TIMESTAMP),
 DATEADD('HOUR', -4, CURRENT_TIMESTAMP));

-- ============================================================================
-- COMPLETION MESSAGE
-- ============================================================================

-- This script adds:
-- - 5 sample customers (3 individual, 2 business)
-- - 3 service requests (ATM card, PIN reset, account opening)
-- - 2 complaints (unauthorized transaction, fee dispute)
-- - 3 bank accounts (checking, savings, business checking)
-- - 3 recent transactions (deposit, withdrawal, transfer)
--
-- All customers use password: password123
-- All timestamps are set to current time with appropriate variations
--
-- To connect to H2 database:
-- 1. Start the AxionBank server
-- 2. Connect to: jdbc:h2:mem:axionbank
-- 3. Username: sa, Password: (empty)
-- 4. Execute this script

SELECT 'Customer care data setup completed successfully!' AS status;