-- Investment & Portfolio Management System Migration
-- Real investment products with actual returns and portfolio tracking

-- Investment Products
CREATE TABLE IF NOT EXISTS investment_products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_code VARCHAR(50) UNIQUE NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    product_type VARCHAR(50) NOT NULL, -- FIXED_DEPOSIT, MONEY_MARKET, STOCKS, BONDS, MUTUAL_FUNDS, ETF
    category VARCHAR(50) NOT NULL, -- LOW_RISK, MEDIUM_RISK, HIGH_RISK
    description TEXT,
    minimum_investment DECIMAL(15, 2) NOT NULL,
    maximum_investment DECIMAL(15, 2),
    expected_return_rate DECIMAL(10, 4) NOT NULL, -- Annual percentage
    actual_return_rate DECIMAL(10, 4), -- Current actual rate
    lock_period_days INTEGER DEFAULT 0, -- 0 for flexible, >0 for fixed term
    early_withdrawal_penalty_rate DECIMAL(10, 4) DEFAULT 0.0,
    compounding_frequency VARCHAR(20) DEFAULT 'MONTHLY', -- DAILY, MONTHLY, QUARTERLY, ANNUALLY
    is_active BOOLEAN DEFAULT TRUE,
    risk_level INTEGER DEFAULT 1, -- 1-5 scale
    currency VARCHAR(10) DEFAULT 'USD',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User Investment Accounts/Portfolios
CREATE TABLE IF NOT EXISTS user_investments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES investment_products(id),
    account_number VARCHAR(50) UNIQUE NOT NULL,
    principal_amount DECIMAL(15, 2) NOT NULL,
    current_value DECIMAL(15, 2) NOT NULL,
    total_returns DECIMAL(15, 2) DEFAULT 0.00,
    total_withdrawals DECIMAL(15, 2) DEFAULT 0.00,
    status VARCHAR(50) DEFAULT 'ACTIVE', -- ACTIVE, MATURED, CLOSED, SUSPENDED
    start_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    maturity_date TIMESTAMP,
    last_interest_date TIMESTAMP,
    auto_renew BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP
);

-- Investment Transactions (deposits, withdrawals, interest credits)
CREATE TABLE IF NOT EXISTS investment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    investment_id UUID NOT NULL REFERENCES user_investments(id) ON DELETE CASCADE,
    transaction_type VARCHAR(50) NOT NULL, -- DEPOSIT, WITHDRAWAL, INTEREST, DIVIDEND, FEE, PENALTY
    amount DECIMAL(15, 2) NOT NULL,
    balance_after DECIMAL(15, 2) NOT NULL,
    interest_rate DECIMAL(10, 4),
    description TEXT,
    reference_number VARCHAR(100) UNIQUE NOT NULL,
    wallet_transaction_id UUID REFERENCES transactions(id),
    status VARCHAR(50) DEFAULT 'COMPLETED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Investment Returns History (daily/periodic snapshots)
CREATE TABLE IF NOT EXISTS investment_performance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    investment_id UUID NOT NULL REFERENCES user_investments(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES investment_products(id),
    snapshot_date DATE NOT NULL,
    opening_value DECIMAL(15, 2) NOT NULL,
    closing_value DECIMAL(15, 2) NOT NULL,
    daily_return DECIMAL(15, 2) DEFAULT 0.00,
    daily_return_percentage DECIMAL(10, 4) DEFAULT 0.00,
    total_return DECIMAL(15, 2) DEFAULT 0.00,
    total_return_percentage DECIMAL(10, 4) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(investment_id, snapshot_date)
);

-- Stock/Asset Holdings (for stock/equity investments)
CREATE TABLE IF NOT EXISTS investment_holdings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    investment_id UUID NOT NULL REFERENCES user_investments(id) ON DELETE CASCADE,
    asset_symbol VARCHAR(20) NOT NULL, -- e.g., AAPL, NSE:SCOM, etc.
    asset_name VARCHAR(200) NOT NULL,
    quantity DECIMAL(20, 8) NOT NULL,
    average_buy_price DECIMAL(15, 4) NOT NULL,
    current_price DECIMAL(15, 4) NOT NULL,
    current_value DECIMAL(15, 2) NOT NULL,
    unrealized_gain_loss DECIMAL(15, 2) DEFAULT 0.00,
    unrealized_gain_loss_percentage DECIMAL(10, 4) DEFAULT 0.00,
    last_price_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Investment Goals (user can set investment targets)
CREATE TABLE IF NOT EXISTS investment_goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    goal_name VARCHAR(200) NOT NULL,
    target_amount DECIMAL(15, 2) NOT NULL,
    current_amount DECIMAL(15, 2) DEFAULT 0.00,
    target_date DATE,
    monthly_contribution DECIMAL(15, 2) DEFAULT 0.00,
    linked_investment_id UUID REFERENCES user_investments(id),
    status VARCHAR(50) DEFAULT 'ACTIVE', -- ACTIVE, ACHIEVED, CANCELLED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    achieved_at TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_investment_products_type ON investment_products(product_type);
CREATE INDEX IF NOT EXISTS idx_investment_products_category ON investment_products(category);
CREATE INDEX IF NOT EXISTS idx_user_investments_user ON user_investments(user_id);
CREATE INDEX IF NOT EXISTS idx_user_investments_product ON user_investments(product_id);
CREATE INDEX IF NOT EXISTS idx_user_investments_status ON user_investments(status);
CREATE INDEX IF NOT EXISTS idx_investment_transactions_investment ON investment_transactions(investment_id);
CREATE INDEX IF NOT EXISTS idx_investment_transactions_type ON investment_transactions(transaction_type);
CREATE INDEX IF NOT EXISTS idx_investment_performance_investment ON investment_performance(investment_id);
CREATE INDEX IF NOT EXISTS idx_investment_performance_date ON investment_performance(snapshot_date);
CREATE INDEX IF NOT EXISTS idx_investment_holdings_investment ON investment_holdings(investment_id);
CREATE INDEX IF NOT EXISTS idx_investment_goals_user ON investment_goals(user_id);

-- Insert sample investment products
INSERT INTO investment_products (product_code, product_name, product_type, category, description, minimum_investment, maximum_investment, expected_return_rate, actual_return_rate, lock_period_days, risk_level, compounding_frequency) VALUES
-- Low Risk Products
('FD-30D', '30-Day Fixed Deposit', 'FIXED_DEPOSIT', 'LOW_RISK', 'Fixed deposit account with 30-day term', 10000.00, 10000000.00, 5.50, 5.50, 30, 1, 'MONTHLY'),
('FD-90D', '90-Day Fixed Deposit', 'FIXED_DEPOSIT', 'LOW_RISK', 'Fixed deposit account with 90-day term', 25000.00, 10000000.00, 6.75, 6.75, 90, 1, 'MONTHLY'),
('FD-180D', '180-Day Fixed Deposit', 'FIXED_DEPOSIT', 'LOW_RISK', 'Fixed deposit account with 180-day term', 50000.00, 10000000.00, 7.50, 7.50, 180, 1, 'MONTHLY'),
('FD-1Y', '1-Year Fixed Deposit', 'FIXED_DEPOSIT', 'LOW_RISK', 'Fixed deposit account with 1-year term', 100000.00, 10000000.00, 8.25, 8.25, 365, 1, 'MONTHLY'),
('MM-FUND', 'Money Market Fund', 'MONEY_MARKET', 'LOW_RISK', 'Liquid money market fund with daily access', 5000.00, NULL, 7.00, 6.95, 0, 1, 'DAILY'),
('GOV-BOND-5Y', '5-Year Government Bond', 'BONDS', 'LOW_RISK', 'Government treasury bonds - 5 year maturity', 100000.00, NULL, 9.50, 9.50, 1825, 1, 'ANNUALLY'),

-- Medium Risk Products
('BALANCED-FUND', 'Balanced Mutual Fund', 'MUTUAL_FUNDS', 'MEDIUM_RISK', '60% stocks, 40% bonds balanced fund', 10000.00, NULL, 12.00, 11.80, 0, 3, 'MONTHLY'),
('INCOME-FUND', 'Income Fund', 'MUTUAL_FUNDS', 'MEDIUM_RISK', 'Bond and dividend-focused income fund', 15000.00, NULL, 10.50, 10.25, 0, 2, 'MONTHLY'),
('CORP-BOND', 'Corporate Bond Fund', 'BONDS', 'MEDIUM_RISK', 'AAA-rated corporate bond portfolio', 50000.00, NULL, 11.00, 10.75, 365, 2, 'QUARTERLY'),

-- High Risk Products
('EQUITY-FUND', 'Equity Growth Fund', 'MUTUAL_FUNDS', 'HIGH_RISK', 'Aggressive growth stock fund', 25000.00, NULL, 18.00, 17.50, 0, 4, 'MONTHLY'),
('INDEX-ETF', 'Market Index ETF', 'ETF', 'HIGH_RISK', 'Tracks major stock market index', 10000.00, NULL, 15.00, 14.75, 0, 4, 'MONTHLY'),
('TECH-FUND', 'Technology Sector Fund', 'STOCKS', 'HIGH_RISK', 'Technology stocks portfolio', 50000.00, NULL, 22.00, 21.25, 0, 5, 'MONTHLY'),
('EMERGING-MARKETS', 'Emerging Markets Fund', 'MUTUAL_FUNDS', 'HIGH_RISK', 'Diversified emerging markets exposure', 30000.00, NULL, 20.00, 19.50, 0, 5, 'MONTHLY')
ON CONFLICT (product_code) DO NOTHING;

COMMENT ON TABLE investment_products IS 'Available investment products with rates and terms';
COMMENT ON TABLE user_investments IS 'User investment accounts and portfolios';
COMMENT ON TABLE investment_transactions IS 'All investment-related transactions';
COMMENT ON TABLE investment_performance IS 'Daily performance snapshots for investments';
COMMENT ON TABLE investment_holdings IS 'Individual stock/asset holdings within investments';
COMMENT ON TABLE investment_goals IS 'User-defined investment goals and targets';
