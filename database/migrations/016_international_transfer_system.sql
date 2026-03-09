-- International Money Transfer System Migration
-- Real cross-border transfers with exchange rates, corridors, and compliance

-- Supported Countries and Corridors
CREATE TABLE IF NOT EXISTS transfer_countries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_code VARCHAR(3) UNIQUE NOT NULL, -- ISO 3166-1 alpha-3
    country_name VARCHAR(200) NOT NULL,
    currency_code VARCHAR(3) NOT NULL, -- ISO 4217
    currency_name VARCHAR(100) NOT NULL,
    region VARCHAR(50), -- AFRICA, ASIA, EUROPE, AMERICAS, OCEANIA
    is_source_country BOOLEAN DEFAULT TRUE,
    is_destination_country BOOLEAN DEFAULT TRUE,
    requires_purpose_code BOOLEAN DEFAULT FALSE,
    requires_beneficiary_id BOOLEAN DEFAULT FALSE,
    max_transaction_limit DECIMAL(15, 2) DEFAULT 50000.00,
    is_active BOOLEAN DEFAULT TRUE,
    flag_emoji VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Exchange Rates
CREATE TABLE IF NOT EXISTS exchange_rates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    rate DECIMAL(20, 8) NOT NULL,
    inverse_rate DECIMAL(20, 8) NOT NULL,
    markup_percentage DECIMAL(10, 4) DEFAULT 2.5, -- Bank's margin
    effective_rate DECIMAL(20, 8) NOT NULL, -- Rate after markup
    mid_market_rate DECIMAL(20, 8) NOT NULL,
    source VARCHAR(50) DEFAULT 'CENTRAL_BANK',
    valid_from TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    valid_until TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(from_currency, to_currency, valid_from)
);

-- Transfer Fees Structure
CREATE TABLE IF NOT EXISTS transfer_fees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_country_id UUID REFERENCES transfer_countries(id),
    to_country_id UUID REFERENCES transfer_countries(id),
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    min_amount DECIMAL(15, 2) DEFAULT 0.00,
    max_amount DECIMAL(15, 2),
    fee_type VARCHAR(20) DEFAULT 'PERCENTAGE', -- PERCENTAGE, FLAT, TIERED
    fee_value DECIMAL(10, 4) NOT NULL,
    minimum_fee DECIMAL(15, 2) DEFAULT 5.00,
    maximum_fee DECIMAL(15, 2),
    processing_time_hours INTEGER DEFAULT 24,
    is_express BOOLEAN DEFAULT FALSE,
    express_fee_additional DECIMAL(15, 2) DEFAULT 0.00,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- International Transfers
CREATE TABLE IF NOT EXISTS international_transfers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reference_number VARCHAR(100) UNIQUE NOT NULL,

    -- Source
    source_country_id UUID NOT NULL REFERENCES transfer_countries(id),
    source_amount DECIMAL(15, 2) NOT NULL,
    source_currency VARCHAR(3) NOT NULL,
    source_account VARCHAR(100), -- Account debited

    -- Destination
    destination_country_id UUID NOT NULL REFERENCES transfer_countries(id),
    destination_amount DECIMAL(15, 2) NOT NULL,
    destination_currency VARCHAR(3) NOT NULL,

    -- Beneficiary
    beneficiary_name VARCHAR(200) NOT NULL,
    beneficiary_account VARCHAR(100) NOT NULL,
    beneficiary_bank_name VARCHAR(200),
    beneficiary_bank_code VARCHAR(50), -- SWIFT/BIC or local code
    beneficiary_address TEXT,
    beneficiary_id_number VARCHAR(100), -- For compliance
    beneficiary_phone VARCHAR(20),

    -- Exchange & Fees
    exchange_rate DECIMAL(20, 8) NOT NULL,
    transfer_fee DECIMAL(15, 2) DEFAULT 0.00,
    total_debit DECIMAL(15, 2) NOT NULL, -- Source amount + fee

    -- Compliance
    purpose_code VARCHAR(50), -- FAMILY_SUPPORT, BUSINESS, EDUCATION, MEDICAL, etc.
    purpose_description TEXT,
    relationship_to_beneficiary VARCHAR(100),
    source_of_funds VARCHAR(100),

    -- Processing
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLIANCE_CHECK, SENT, COMPLETED, FAILED, CANCELLED, REFUNDED
    delivery_method VARCHAR(50) DEFAULT 'BANK_ACCOUNT', -- BANK_ACCOUNT, CASH_PICKUP, MOBILE_WALLET
    delivery_time_estimate INTEGER, -- Hours
    is_express BOOLEAN DEFAULT FALSE,

    -- Tracking
    bank_transaction_id UUID REFERENCES transactions(id),
    partner_reference VARCHAR(100), -- Reference from transfer partner
    beneficiary_received_at TIMESTAMP,
    tracking_number VARCHAR(100),

    -- Metadata
    ip_address VARCHAR(50),
    device_info TEXT,
    compliance_notes TEXT,
    rejection_reason TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    metadata JSONB
);

-- Saved Beneficiaries for International Transfers
CREATE TABLE IF NOT EXISTS international_beneficiaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    nickname VARCHAR(100),
    country_id UUID NOT NULL REFERENCES transfer_countries(id),
    beneficiary_name VARCHAR(200) NOT NULL,
    beneficiary_account VARCHAR(100) NOT NULL,
    beneficiary_bank_name VARCHAR(200),
    beneficiary_bank_code VARCHAR(50),
    beneficiary_address TEXT,
    beneficiary_phone VARCHAR(20),
    relationship VARCHAR(100),
    is_verified BOOLEAN DEFAULT FALSE,
    is_favorite BOOLEAN DEFAULT FALSE,
    last_used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, beneficiary_account, country_id)
);

-- Transfer Compliance Checks
CREATE TABLE IF NOT EXISTS transfer_compliance_checks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transfer_id UUID NOT NULL REFERENCES international_transfers(id) ON DELETE CASCADE,
    check_type VARCHAR(50) NOT NULL, -- AML, SANCTIONS, PEP, FRAUD, LIMIT_CHECK
    check_status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, PASS, FAIL, REVIEW_REQUIRED
    check_result TEXT,
    risk_score INTEGER, -- 0-100
    checked_by VARCHAR(100),
    checked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Transfer Status History
CREATE TABLE IF NOT EXISTS transfer_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transfer_id UUID NOT NULL REFERENCES international_transfers(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    status_description TEXT,
    location VARCHAR(100), -- Where in the process
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_transfer_countries_code ON transfer_countries(country_code);
CREATE INDEX IF NOT EXISTS idx_exchange_rates_currencies ON exchange_rates(from_currency, to_currency);
CREATE INDEX IF NOT EXISTS idx_exchange_rates_active ON exchange_rates(is_active, valid_from);
CREATE INDEX IF NOT EXISTS idx_transfer_fees_currencies ON transfer_fees(from_currency, to_currency);
CREATE INDEX IF NOT EXISTS idx_international_transfers_user ON international_transfers(user_id);
CREATE INDEX IF NOT EXISTS idx_international_transfers_status ON international_transfers(status);
CREATE INDEX IF NOT EXISTS idx_international_transfers_created ON international_transfers(created_at);
CREATE INDEX IF NOT EXISTS idx_international_beneficiaries_user ON international_beneficiaries(user_id);
CREATE INDEX IF NOT EXISTS idx_transfer_compliance_transfer ON transfer_compliance_checks(transfer_id);
CREATE INDEX IF NOT EXISTS idx_transfer_status_history_transfer ON transfer_status_history(transfer_id);

-- Insert supported countries (East Africa + major destinations)
INSERT INTO transfer_countries (country_code, country_name, currency_code, currency_name, region, is_source_country, is_destination_country, flag_emoji) VALUES
-- East Africa
('KEN', 'Kenya', 'KES', 'Kenyan Shilling', 'AFRICA', TRUE, TRUE, '🇰🇪'),
('UGA', 'Uganda', 'UGX', 'Ugandan Shilling', 'AFRICA', TRUE, TRUE, '🇺🇬'),
('TZA', 'Tanzania', 'TZS', 'Tanzanian Shilling', 'AFRICA', TRUE, TRUE, '🇹🇿'),
('RWA', 'Rwanda', 'RWF', 'Rwandan Franc', 'AFRICA', TRUE, TRUE, '🇷🇼'),
('ETH', 'Ethiopia', 'ETB', 'Ethiopian Birr', 'AFRICA', TRUE, TRUE, '🇪🇹'),
('SOM', 'Somalia', 'SOS', 'Somali Shilling', 'AFRICA', TRUE, TRUE, '🇸🇴'),

-- West Africa
('NGA', 'Nigeria', 'NGN', 'Nigerian Naira', 'AFRICA', TRUE, TRUE, '🇳🇬'),
('GHA', 'Ghana', 'GHS', 'Ghanaian Cedi', 'AFRICA', TRUE, TRUE, '🇬🇭'),
('SEN', 'Senegal', 'XOF', 'CFA Franc', 'AFRICA', TRUE, TRUE, '🇸🇳'),

-- Southern Africa
('ZAF', 'South Africa', 'ZAR', 'South African Rand', 'AFRICA', TRUE, TRUE, '🇿🇦'),
('ZWE', 'Zimbabwe', 'ZWL', 'Zimbabwean Dollar', 'AFRICA', TRUE, TRUE, '🇿🇼'),

-- Europe
('GBR', 'United Kingdom', 'GBP', 'British Pound', 'EUROPE', TRUE, TRUE, '🇬🇧'),
('FRA', 'France', 'EUR', 'Euro', 'EUROPE', TRUE, TRUE, '🇫🇷'),
('DEU', 'Germany', 'EUR', 'Euro', 'EUROPE', TRUE, TRUE, '🇩🇪'),

-- North America
('USA', 'United States', 'USD', 'US Dollar', 'AMERICAS', TRUE, TRUE, '🇺🇸'),
('CAN', 'Canada', 'CAD', 'Canadian Dollar', 'AMERICAS', TRUE, TRUE, '🇨🇦'),

-- Asia
('IND', 'India', 'INR', 'Indian Rupee', 'ASIA', TRUE, TRUE, '🇮🇳'),
('CHN', 'China', 'CNY', 'Chinese Yuan', 'ASIA', TRUE, TRUE, '🇨🇳'),
('JPN', 'Japan', 'JPY', 'Japanese Yen', 'ASIA', TRUE, TRUE, '🇯🇵'),
('ARE', 'United Arab Emirates', 'AED', 'UAE Dirham', 'ASIA', TRUE, TRUE, '🇦🇪'),
('SAU', 'Saudi Arabia', 'SAR', 'Saudi Riyal', 'ASIA', TRUE, TRUE, '🇸🇦'),

-- Oceania
('AUS', 'Australia', 'AUD', 'Australian Dollar', 'OCEANIA', TRUE, TRUE, '🇦🇺')
ON CONFLICT (country_code) DO NOTHING;

-- Insert sample exchange rates (from USD)
INSERT INTO exchange_rates (from_currency, to_currency, rate, inverse_rate, markup_percentage, effective_rate, mid_market_rate) VALUES
('USD', 'KES', 129.50, 0.0077220, 2.5, 132.74, 129.50),
('USD', 'UGX', 3750.00, 0.0002667, 2.5, 3843.75, 3750.00),
('USD', 'TZS', 2500.00, 0.0004000, 2.5, 2562.50, 2500.00),
('USD', 'RWF', 1250.00, 0.0008000, 2.5, 1281.25, 1250.00),
('USD', 'ETB', 55.50, 0.0180180, 2.5, 56.89, 55.50),
('USD', 'NGN', 750.00, 0.0013333, 2.5, 768.75, 750.00),
('USD', 'ZAR', 18.50, 0.0540541, 2.5, 18.96, 18.50),
('USD', 'GBP', 0.79, 1.2658228, 2.5, 0.77, 0.79),
('USD', 'EUR', 0.92, 1.0869565, 2.5, 0.90, 0.92),
('USD', 'INR', 83.00, 0.0120482, 2.5, 85.08, 83.00),
('USD', 'CNY', 7.20, 0.1388889, 2.5, 7.38, 7.20),
('USD', 'JPY', 145.00, 0.0068966, 2.5, 148.63, 145.00),
('USD', 'AED', 3.67, 0.2724796, 2.5, 3.76, 3.67),
('USD', 'AUD', 1.52, 0.6578947, 2.5, 1.56, 1.52),
('USD', 'CAD', 1.36, 0.7352941, 2.5, 1.39, 1.36)
ON CONFLICT (from_currency, to_currency, valid_from) DO NOTHING;

-- Insert transfer fees
INSERT INTO transfer_fees (from_currency, to_currency, min_amount, max_amount, fee_type, fee_value, minimum_fee, maximum_fee, processing_time_hours) VALUES
-- USD to African currencies
('USD', 'KES', 10.00, 50000.00, 'PERCENTAGE', 2.5, 5.00, 50.00, 24),
('USD', 'UGX', 10.00, 50000.00, 'PERCENTAGE', 2.5, 5.00, 50.00, 24),
('USD', 'TZS', 10.00, 50000.00, 'PERCENTAGE', 2.5, 5.00, 50.00, 48),
('USD', 'NGN', 10.00, 50000.00, 'PERCENTAGE', 3.0, 5.00, 75.00, 24),
('USD', 'ZAR', 10.00, 50000.00, 'PERCENTAGE', 2.0, 5.00, 40.00, 12),

-- USD to major currencies
('USD', 'GBP', 50.00, 100000.00, 'PERCENTAGE', 1.5, 10.00, 100.00, 12),
('USD', 'EUR', 50.00, 100000.00, 'PERCENTAGE', 1.5, 10.00, 100.00, 12),
('USD', 'INR', 10.00, 50000.00, 'PERCENTAGE', 2.0, 5.00, 50.00, 24),
('USD', 'AED', 50.00, 100000.00, 'PERCENTAGE', 1.5, 10.00, 75.00, 12)
ON CONFLICT DO NOTHING;

COMMENT ON TABLE transfer_countries IS 'Countries supported for international transfers';
COMMENT ON TABLE exchange_rates IS 'Real-time currency exchange rates with bank markup';
COMMENT ON TABLE transfer_fees IS 'Fee structure for international transfers';
COMMENT ON TABLE international_transfers IS 'International money transfer records with compliance';
COMMENT ON TABLE international_beneficiaries IS 'Saved beneficiaries for recurring transfers';
COMMENT ON TABLE transfer_compliance_checks IS 'AML/KYC compliance checks for transfers';
COMMENT ON TABLE transfer_status_history IS 'Status tracking for international transfers';
