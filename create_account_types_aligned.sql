-- ============================================================================
-- CREATE ACCOUNT TYPES TABLE ALIGNED WITH SERVER CODE STRUCTURE
-- This SQL creates the account_types table matching the server's Tables.kt definition
-- ============================================================================

-- Drop table if exists (for clean setup)
DROP TABLE IF EXISTS account_types CASCADE;

-- Create account_types table with exact column names from server code
CREATE TABLE account_types
(
    id                         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type_name                  VARCHAR(50) UNIQUE                  NOT NULL,
    display_name               VARCHAR(100)                        NOT NULL,
    description                TEXT,
    minimum_deposit            DECIMAL(15, 2)   DEFAULT 0.00       NOT NULL,
    minimum_balance            DECIMAL(15, 2)   DEFAULT 0.00       NOT NULL,
    interest_rate              DECIMAL(5, 4)    DEFAULT 0.0000     NOT NULL,
    monthly_maintenance_fee    DECIMAL(15, 2)   DEFAULT 0.00       NOT NULL,
    overdraft_limit            DECIMAL(15, 2),
    features                   TEXT, -- JSON array of features
    is_active                  BOOLEAN          DEFAULT true       NOT NULL,
    category                   VARCHAR(50)      DEFAULT 'PERSONAL' NOT NULL,
    max_transactions_per_month INTEGER,
    atm_withdrawal_limit       DECIMAL(15, 2),
    online_banking_enabled     BOOLEAN          DEFAULT true       NOT NULL,
    mobile_banking_enabled     BOOLEAN          DEFAULT true       NOT NULL,
    check_book_available       BOOLEAN          DEFAULT true       NOT NULL,
    debit_card_available       BOOLEAN          DEFAULT true       NOT NULL,
    created_at                 TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at                 TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_account_types_category ON account_types (category);
CREATE INDEX idx_account_types_active ON account_types (is_active);
CREATE INDEX idx_account_types_type_name ON account_types (type_name);

-- Insert account types data with proper JSON formatting for features
INSERT INTO account_types (type_name, display_name, description, minimum_deposit, minimum_balance,
                           interest_rate, monthly_maintenance_fee, overdraft_limit, features,
                           category, max_transactions_per_month, atm_withdrawal_limit,
                           online_banking_enabled, mobile_banking_enabled, check_book_available, debit_card_available)
VALUES ('PERSONAL_SAVINGS',
        'Personal Savings Account',
        'Basic savings account for individuals with competitive interest rates',
        100.00,
        50.00,
        0.0250,
        0.00,
        NULL,
        '["MOBILE_BANKING", "ONLINE_STATEMENTS", "ATM_ACCESS", "INTEREST_EARNING"]',
        'PERSONAL',
        NULL,
        500.00,
        true,
        true,
        false,
        true),
       ('PERSONAL_CHECKING',
        'Personal Checking Account',
        'Standard checking account for daily transactions and bill payments',
        50.00,
        25.00,
        0.0025,
        5.00,
        100.00,
        '["MOBILE_BANKING", "CHECK_WRITING", "DEBIT_CARD", "ONLINE_BILL_PAY", "ATM_ACCESS"]',
        'PERSONAL',
        50,
        400.00,
        true,
        true,
        true,
        true),
       ('BUSINESS_CHECKING',
        'Business Checking Account',
        'Professional checking account for business operations and transactions',
        500.00,
        100.00,
        0.0100,
        15.00,
        1000.00,
        '["MOBILE_BANKING", "CHECK_WRITING", "WIRE_TRANSFERS", "MERCHANT_SERVICES", "ATM_ACCESS"]',
        'BUSINESS',
        100,
        1000.00,
        true,
        true,
        true,
        true),
       ('BUSINESS_SAVINGS',
        'Business Savings Account',
        'High-yield savings account for business surplus funds',
        1000.00,
        500.00,
        0.0175,
        0.00,
        NULL,
        '["MOBILE_BANKING", "ONLINE_STATEMENTS", "WIRE_TRANSFERS", "ATM_ACCESS", "INTEREST_EARNING"]',
        'BUSINESS',
        10,
        1000.00,
        true,
        true,
        false,
        true),
       ('PREMIUM_CHECKING',
        'Premium Checking Account',
        'Enhanced checking account with premium benefits and higher limits',
        1000.00,
        500.00,
        0.0050,
        0.00,
        2000.00,
        '["MOBILE_BANKING", "CHECK_WRITING", "PREMIUM_SUPPORT", "FOREIGN_ATM_REBATES", "WIRE_TRANSFERS"]',
        'PERSONAL',
        NULL,
        800.00,
        true,
        true,
        true,
        true),
       ('STUDENT_CHECKING',
        'Student Checking Account',
        'No-fee checking account designed for students and young adults',
        0.00,
        0.00,
        0.0010,
        0.00,
        50.00,
        '["MOBILE_BANKING", "CHECK_WRITING", "DEBIT_CARD", "ONLINE_BILL_PAY", "ATM_ACCESS", "STUDENT_BENEFITS"]',
        'PERSONAL',
        25,
        300.00,
        true,
        true,
        true,
        true),
       ('MONEY_MARKET',
        'Money Market Account',
        'High-yield account with competitive rates and limited transaction privileges',
        2500.00,
        2000.00,
        0.0325,
        10.00,
        NULL,
        '["MOBILE_BANKING", "CHECK_WRITING", "DEBIT_CARD", "ONLINE_STATEMENTS", "ATM_ACCESS", "HIGH_YIELD"]',
        'PERSONAL',
        6,
        600.00,
        true,
        true,
        true,
        true);

-- Display confirmation and test queries
SELECT 'Account types table created successfully with ' || COUNT(*) || ' account types!' as status
FROM account_types;

-- Verify the data
SELECT type_name,
       display_name,
       category,
       minimum_deposit,
       interest_rate * 100                                   as interest_rate_percent,
       monthly_maintenance_fee,
       CASE WHEN is_active THEN 'ACTIVE' ELSE 'INACTIVE' END as status
FROM account_types
ORDER BY category, minimum_deposit;

-- Show available features for each account type
SELECT type_name,
       display_name,
       features,
       max_transactions_per_month,
       atm_withdrawal_limit
FROM account_types
ORDER BY category, type_name;