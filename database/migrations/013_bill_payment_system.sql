-- Bill Payment System Migration
-- This creates tables for bill payment services with real transaction support

-- Bill Payment Vendors/Billers
CREATE TABLE IF NOT EXISTS bill_vendors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_code VARCHAR(50) UNIQUE NOT NULL,
    vendor_name VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL, -- ELECTRICITY, WATER, INTERNET, PHONE, CABLE, INSURANCE, etc.
    description TEXT,
    logo_url TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    requires_account_number BOOLEAN DEFAULT TRUE,
    account_number_label VARCHAR(100) DEFAULT 'Account Number',
    account_number_format VARCHAR(100), -- Regex pattern for validation
    min_amount DECIMAL(15, 2) DEFAULT 1.00,
    max_amount DECIMAL(15, 2) DEFAULT 1000000.00,
    processing_fee_type VARCHAR(20) DEFAULT 'PERCENTAGE', -- PERCENTAGE or FLAT
    processing_fee_value DECIMAL(10, 4) DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User Saved Billers for quick access
CREATE TABLE IF NOT EXISTS user_saved_billers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vendor_id UUID NOT NULL REFERENCES bill_vendors(id) ON DELETE CASCADE,
    nickname VARCHAR(100), -- User's custom name for this biller
    account_number VARCHAR(100) NOT NULL,
    is_favorite BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, vendor_id, account_number)
);

-- Bill Payment Transactions
CREATE TABLE IF NOT EXISTS bill_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vendor_id UUID NOT NULL REFERENCES bill_vendors(id),
    account_number VARCHAR(100) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    processing_fee DECIMAL(15, 2) DEFAULT 0.00,
    total_amount DECIMAL(15, 2) NOT NULL,
    payment_method VARCHAR(50) DEFAULT 'WALLET', -- WALLET, CARD, BANK_ACCOUNT
    payment_reference VARCHAR(100) UNIQUE NOT NULL,
    vendor_reference VARCHAR(100), -- Reference from the biller
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED, REVERSED
    description TEXT,
    transaction_id UUID REFERENCES transactions(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    failed_reason TEXT,
    metadata JSONB -- Additional data specific to the vendor
);

-- Scheduled/Recurring Bill Payments
CREATE TABLE IF NOT EXISTS scheduled_bill_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vendor_id UUID NOT NULL REFERENCES bill_vendors(id),
    account_number VARCHAR(100) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    frequency VARCHAR(50) NOT NULL, -- ONCE, DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY
    next_payment_date DATE NOT NULL,
    end_date DATE,
    is_active BOOLEAN DEFAULT TRUE,
    auto_pay BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_executed_at TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_bill_vendors_category ON bill_vendors(category);
CREATE INDEX IF NOT EXISTS idx_bill_vendors_active ON bill_vendors(is_active);
CREATE INDEX IF NOT EXISTS idx_user_saved_billers_user ON user_saved_billers(user_id);
CREATE INDEX IF NOT EXISTS idx_bill_payments_user ON bill_payments(user_id);
CREATE INDEX IF NOT EXISTS idx_bill_payments_status ON bill_payments(status);
CREATE INDEX IF NOT EXISTS idx_bill_payments_created ON bill_payments(created_at);
CREATE INDEX IF NOT EXISTS idx_scheduled_payments_user ON scheduled_bill_payments(user_id);
CREATE INDEX IF NOT EXISTS idx_scheduled_payments_next_date ON scheduled_bill_payments(next_payment_date, is_active);

-- Insert sample bill vendors for Kenya market
INSERT INTO bill_vendors (vendor_code, vendor_name, category, description, requires_account_number, account_number_label, min_amount, max_amount, processing_fee_type, processing_fee_value) VALUES
-- Electricity
('KPLC', 'Kenya Power & Lighting', 'ELECTRICITY', 'Pay your electricity bills', TRUE, 'Meter Number', 50.00, 50000.00, 'FLAT', 25.00),
('UMEME', 'Umeme Uganda', 'ELECTRICITY', 'Uganda electricity provider', TRUE, 'Account Number', 100.00, 100000.00, 'PERCENTAGE', 0.5),

-- Water
('NCWSC', 'Nairobi City Water', 'WATER', 'Nairobi water services', TRUE, 'Account Number', 100.00, 20000.00, 'FLAT', 20.00),
('KIWASCO', 'Kisumu Water Company', 'WATER', 'Kisumu water and sewerage', TRUE, 'Account Number', 50.00, 15000.00, 'FLAT', 15.00),

-- Internet & Cable TV
('SAFARICOM_FIBER', 'Safaricom Fiber', 'INTERNET', 'Safaricom home fiber internet', TRUE, 'Account Number', 2500.00, 15000.00, 'FLAT', 0.00),
('ZUKU', 'Zuku', 'CABLE_TV', 'Zuku cable TV and internet', TRUE, 'Account Number', 1000.00, 10000.00, 'FLAT', 50.00),
('STARTIMES', 'StarTimes', 'CABLE_TV', 'StarTimes digital TV', TRUE, 'Smart Card Number', 400.00, 5000.00, 'FLAT', 30.00),
('DSTV', 'DSTV', 'CABLE_TV', 'DSTV satellite TV', TRUE, 'Smart Card Number', 500.00, 15000.00, 'PERCENTAGE', 1.0),
('GOTV', 'GOTV', 'CABLE_TV', 'GOTV satellite TV', TRUE, 'IUC Number', 300.00, 5000.00, 'FLAT', 25.00),

-- Mobile Airtime & Data
('SAFARICOM', 'Safaricom', 'MOBILE_AIRTIME', 'Safaricom airtime and bundles', TRUE, 'Phone Number', 10.00, 10000.00, 'FLAT', 0.00),
('AIRTEL', 'Airtel', 'MOBILE_AIRTIME', 'Airtel airtime and bundles', TRUE, 'Phone Number', 10.00, 10000.00, 'FLAT', 0.00),
('TELKOM', 'Telkom Kenya', 'MOBILE_AIRTIME', 'Telkom airtime and bundles', TRUE, 'Phone Number', 10.00, 10000.00, 'FLAT', 0.00),

-- Insurance
('JUBILEE_INSURANCE', 'Jubilee Insurance', 'INSURANCE', 'Pay insurance premiums', TRUE, 'Policy Number', 500.00, 100000.00, 'PERCENTAGE', 0.5),
('AAR_INSURANCE', 'AAR Insurance', 'INSURANCE', 'AAR health insurance', TRUE, 'Policy Number', 1000.00, 50000.00, 'PERCENTAGE', 0.75),
('BRITAM', 'Britam', 'INSURANCE', 'Britam insurance services', TRUE, 'Policy Number', 500.00, 100000.00, 'PERCENTAGE', 0.5),

-- Schools & Education
('SCHOOL_FEES', 'School Fees Payment', 'EDUCATION', 'Pay school fees', TRUE, 'Student Admission Number', 1000.00, 500000.00, 'PERCENTAGE', 0.5),

-- Government Services
('KRA', 'Kenya Revenue Authority', 'GOVERNMENT', 'Pay taxes to KRA', TRUE, 'KRA PIN', 100.00, 10000000.00, 'FLAT', 0.00),
('NHIF', 'NHIF', 'GOVERNMENT', 'National Hospital Insurance Fund', TRUE, 'NHIF Number', 500.00, 50000.00, 'FLAT', 0.00),
('NSSF', 'NSSF', 'GOVERNMENT', 'National Social Security Fund', TRUE, 'NSSF Number', 200.00, 50000.00, 'FLAT', 0.00),

-- Utilities
('TRASH_COLLECTION', 'Waste Collection Service', 'UTILITIES', 'Monthly waste collection', TRUE, 'Account Number', 200.00, 5000.00, 'FLAT', 15.00)
ON CONFLICT (vendor_code) DO NOTHING;

COMMENT ON TABLE bill_vendors IS 'Available bill payment vendors/billers in the system';
COMMENT ON TABLE user_saved_billers IS 'User saved billers for quick access';
COMMENT ON TABLE bill_payments IS 'Bill payment transaction records';
COMMENT ON TABLE scheduled_bill_payments IS 'Scheduled and recurring bill payments';
