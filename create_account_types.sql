-- ============================================================================
-- CREATE ACCOUNT TYPES TABLE FOR CUSTOMER CARE OPERATIONS
-- ============================================================================

-- Drop table if exists (for clean setup)
DROP TABLE IF EXISTS account_types CASCADE;

-- Create account_types table
CREATE TABLE account_types
(
    id                   UUID PRIMARY KEY            DEFAULT gen_random_uuid(),
    type_name            VARCHAR(50) UNIQUE NOT NULL,
    display_name         VARCHAR(100)       NOT NULL,
    description          TEXT,
    category             VARCHAR(20)        NOT NULL CHECK (category IN ('PERSONAL', 'BUSINESS')),
    minimum_deposit      DECIMAL(15, 2)     NOT NULL DEFAULT 0.00,
    minimum_balance      DECIMAL(15, 2)     NOT NULL DEFAULT 0.00,
    interest_rate        DECIMAL(5, 4)      NOT NULL DEFAULT 0.0000,
    monthly_fee          DECIMAL(8, 2)      NOT NULL DEFAULT 0.00,
    transaction_limit    INTEGER                     DEFAULT NULL,
    atm_withdrawal_limit DECIMAL(10, 2)              DEFAULT NULL,
    features             TEXT[], -- Array of features like 'MOBILE_BANKING', 'CHECK_WRITING', etc.
    is_active            BOOLEAN            NOT NULL DEFAULT true,
    created_at           TIMESTAMP                   DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP                   DEFAULT CURRENT_TIMESTAMP
);

-- Create index for faster queries
CREATE INDEX idx_account_types_category ON account_types (category);
CREATE INDEX idx_account_types_active ON account_types (is_active);

-- Insert account types data
INSERT INTO account_types (type_name, display_name, description, category,
                           minimum_deposit, minimum_balance, interest_rate, monthly_fee,
                           transaction_limit, atm_withdrawal_limit, features)
VALUES ('PERSONAL_SAVINGS',
        'Personal Savings Account',
        'Basic savings account for individuals with competitive interest rates',
        'PERSONAL',
        100.00,
        50.00,
        0.0250,
        0.00,
        NULL,
        500.00,
        ARRAY ['MOBILE_BANKING', 'ONLINE_STATEMENTS', 'ATM_ACCESS']),
       ('PERSONAL_CHECKING',
        'Personal Checking Account',
        'Standard checking account for daily transactions and bill payments',
        'PERSONAL',
        50.00,
        25.00,
        0.0025,
        5.00,
        50,
        400.00,
        ARRAY ['MOBILE_BANKING', 'CHECK_WRITING', 'DEBIT_CARD', 'ONLINE_BILL_PAY', 'ATM_ACCESS']),
       ('BUSINESS_CHECKING',
        'Business Checking Account',
        'Professional checking account for business operations and transactions',
        'BUSINESS',
        500.00,
        100.00,
        0.0100,
        15.00,
        100,
        1000.00,
        ARRAY ['MOBILE_BANKING', 'CHECK_WRITING', 'WIRE_TRANSFERS', 'MERCHANT_SERVICES', 'ATM_ACCESS']),
       ('BUSINESS_SAVINGS',
        'Business Savings Account',
        'High-yield savings account for business surplus funds',
        'BUSINESS',
        1000.00,
        500.00,
        0.0175,
        0.00,
        10,
        1000.00,
        ARRAY ['MOBILE_BANKING', 'ONLINE_STATEMENTS', 'WIRE_TRANSFERS', 'ATM_ACCESS']),
       ('PREMIUM_CHECKING',
        'Premium Checking Account',
        'Enhanced checking account with premium benefits and higher limits',
        'PERSONAL',
        1000.00,
        500.00,
        0.0050,
        0.00,
        NULL,
        800.00,
        ARRAY ['MOBILE_BANKING', 'CHECK_WRITING', 'PREMIUM_SUPPORT', 'FOREIGN_ATM_REBATES', 'WIRE_TRANSFERS']),
       ('STUDENT_CHECKING',
        'Student Checking Account',
        'No-fee checking account designed for students and young adults',
        'PERSONAL',
        0.00,
        0.00,
        0.0010,
        0.00,
        25,
        300.00,
        ARRAY ['MOBILE_BANKING', 'CHECK_WRITING', 'DEBIT_CARD', 'ONLINE_BILL_PAY', 'ATM_ACCESS']),
       ('MONEY_MARKET',
        'Money Market Account',
        'High-yield account with competitive rates and limited transaction privileges',
        'PERSONAL',
        2500.00,
        2000.00,
        0.0325,
        10.00,
        6,
        600.00,
        ARRAY ['MOBILE_BANKING', 'CHECK_WRITING', 'DEBIT_CARD', 'ONLINE_STATEMENTS', 'ATM_ACCESS']);

-- Display confirmation
SELECT 'Account types table created successfully!' as status;
SELECT COUNT(*) as total_account_types
FROM account_types;
SELECT type_name, display_name, category, minimum_deposit
FROM account_types
ORDER BY category, minimum_deposit;