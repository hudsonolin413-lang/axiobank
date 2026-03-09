-- Cryptocurrency Wallet System Migration
-- Real crypto wallet with multiple currencies, trading, and real-time pricing

-- Supported Cryptocurrencies
CREATE TABLE IF NOT EXISTS crypto_currencies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    symbol VARCHAR(20) UNIQUE NOT NULL, -- BTC, ETH, USDT, etc.
    name VARCHAR(100) NOT NULL,
    full_name VARCHAR(200),
    network VARCHAR(50), -- BITCOIN, ETHEREUM, BINANCE_SMART_CHAIN, POLYGON, etc.
    decimals INTEGER DEFAULT 8,
    contract_address VARCHAR(200), -- For ERC-20 tokens, etc.
    current_price_usd DECIMAL(20, 8) DEFAULT 0.00,
    price_change_24h DECIMAL(10, 4) DEFAULT 0.00,
    market_cap_usd DECIMAL(20, 2),
    trading_volume_24h DECIMAL(20, 2),
    is_active BOOLEAN DEFAULT TRUE,
    is_tradeable BOOLEAN DEFAULT TRUE,
    icon_url TEXT,
    last_price_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User Crypto Wallets
CREATE TABLE IF NOT EXISTS crypto_wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    currency_id UUID NOT NULL REFERENCES crypto_currencies(id),
    wallet_address VARCHAR(200) UNIQUE NOT NULL,
    private_key_encrypted TEXT NOT NULL, -- Encrypted private key
    balance DECIMAL(30, 18) DEFAULT 0.000000000000000000,
    balance_usd DECIMAL(15, 2) DEFAULT 0.00,
    total_deposited DECIMAL(30, 18) DEFAULT 0.000000000000000000,
    total_withdrawn DECIMAL(30, 18) DEFAULT 0.000000000000000000,
    is_primary BOOLEAN DEFAULT FALSE,
    status VARCHAR(50) DEFAULT 'ACTIVE', -- ACTIVE, FROZEN, CLOSED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_transaction_at TIMESTAMP,
    UNIQUE(user_id, currency_id)
);

-- Crypto Transactions (deposits, withdrawals, trades)
CREATE TABLE IF NOT EXISTS crypto_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id UUID NOT NULL REFERENCES crypto_wallets(id) ON DELETE CASCADE,
    transaction_type VARCHAR(50) NOT NULL, -- DEPOSIT, WITHDRAWAL, BUY, SELL, SWAP, TRANSFER_IN, TRANSFER_OUT
    amount DECIMAL(30, 18) NOT NULL,
    amount_usd DECIMAL(15, 2) NOT NULL,
    fee DECIMAL(30, 18) DEFAULT 0.000000000000000000,
    fee_usd DECIMAL(15, 2) DEFAULT 0.00,
    price_per_unit DECIMAL(20, 8) NOT NULL,
    from_address VARCHAR(200),
    to_address VARCHAR(200),
    blockchain_tx_hash VARCHAR(200), -- Actual blockchain transaction hash
    blockchain_confirmations INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, CONFIRMING, COMPLETED, FAILED, CANCELLED
    description TEXT,
    reference_number VARCHAR(100) UNIQUE NOT NULL,
    bank_transaction_id UUID REFERENCES transactions(id), -- Link to fiat transaction
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP,
    metadata JSONB
);

-- Crypto Trading Orders
CREATE TABLE IF NOT EXISTS crypto_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    order_type VARCHAR(50) NOT NULL, -- MARKET, LIMIT, STOP_LOSS, STOP_LIMIT
    side VARCHAR(20) NOT NULL, -- BUY, SELL
    from_currency_id UUID NOT NULL REFERENCES crypto_currencies(id),
    to_currency_id UUID NOT NULL REFERENCES crypto_currencies(id),
    from_amount DECIMAL(30, 18) NOT NULL,
    to_amount DECIMAL(30, 18) NOT NULL,
    price DECIMAL(20, 8),
    limit_price DECIMAL(20, 8),
    stop_price DECIMAL(20, 8),
    filled_amount DECIMAL(30, 18) DEFAULT 0.000000000000000000,
    remaining_amount DECIMAL(30, 18),
    average_fill_price DECIMAL(20, 8),
    total_fee DECIMAL(30, 18) DEFAULT 0.000000000000000000,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, OPEN, PARTIALLY_FILLED, FILLED, CANCELLED, EXPIRED
    order_reference VARCHAR(100) UNIQUE NOT NULL,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    filled_at TIMESTAMP,
    cancelled_at TIMESTAMP
);

-- Crypto Trading Pairs
CREATE TABLE IF NOT EXISTS crypto_trading_pairs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    base_currency_id UUID NOT NULL REFERENCES crypto_currencies(id),
    quote_currency_id UUID NOT NULL REFERENCES crypto_currencies(id),
    symbol VARCHAR(20) NOT NULL, -- BTC/USD, ETH/BTC, etc.
    current_price DECIMAL(20, 8) NOT NULL,
    bid_price DECIMAL(20, 8) NOT NULL,
    ask_price DECIMAL(20, 8) NOT NULL,
    trading_fee_percentage DECIMAL(10, 4) DEFAULT 0.25,
    min_order_size DECIMAL(30, 18),
    max_order_size DECIMAL(30, 18),
    is_active BOOLEAN DEFAULT TRUE,
    last_trade_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(base_currency_id, quote_currency_id)
);

-- Crypto Price History (OHLCV data)
CREATE TABLE IF NOT EXISTS crypto_price_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    currency_id UUID NOT NULL REFERENCES crypto_currencies(id),
    timestamp TIMESTAMP NOT NULL,
    open_price DECIMAL(20, 8) NOT NULL,
    high_price DECIMAL(20, 8) NOT NULL,
    low_price DECIMAL(20, 8) NOT NULL,
    close_price DECIMAL(20, 8) NOT NULL,
    volume DECIMAL(30, 18) DEFAULT 0.000000000000000000,
    volume_usd DECIMAL(20, 2) DEFAULT 0.00,
    interval VARCHAR(20) DEFAULT '1h', -- 1m, 5m, 15m, 1h, 4h, 1d, 1w
    UNIQUE(currency_id, timestamp, interval)
);

-- User Crypto Watchlist
CREATE TABLE IF NOT EXISTS crypto_watchlist (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    currency_id UUID NOT NULL REFERENCES crypto_currencies(id),
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, currency_id)
);

-- Crypto Staking (for proof-of-stake coins)
CREATE TABLE IF NOT EXISTS crypto_staking (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id UUID NOT NULL REFERENCES crypto_wallets(id) ON DELETE CASCADE,
    staked_amount DECIMAL(30, 18) NOT NULL,
    annual_yield_rate DECIMAL(10, 4) NOT NULL,
    rewards_earned DECIMAL(30, 18) DEFAULT 0.000000000000000000,
    lock_period_days INTEGER DEFAULT 0,
    start_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_date TIMESTAMP,
    status VARCHAR(50) DEFAULT 'ACTIVE', -- ACTIVE, COMPLETED, UNSTAKED
    auto_compound BOOLEAN DEFAULT TRUE,
    last_reward_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_crypto_currencies_symbol ON crypto_currencies(symbol);
CREATE INDEX IF NOT EXISTS idx_crypto_wallets_user ON crypto_wallets(user_id);
CREATE INDEX IF NOT EXISTS idx_crypto_wallets_currency ON crypto_wallets(currency_id);
CREATE INDEX IF NOT EXISTS idx_crypto_transactions_wallet ON crypto_transactions(wallet_id);
CREATE INDEX IF NOT EXISTS idx_crypto_transactions_type ON crypto_transactions(transaction_type);
CREATE INDEX IF NOT EXISTS idx_crypto_transactions_status ON crypto_transactions(status);
CREATE INDEX IF NOT EXISTS idx_crypto_transactions_created ON crypto_transactions(created_at);
CREATE INDEX IF NOT EXISTS idx_crypto_orders_user ON crypto_orders(user_id);
CREATE INDEX IF NOT EXISTS idx_crypto_orders_status ON crypto_orders(status);
CREATE INDEX IF NOT EXISTS idx_crypto_trading_pairs_active ON crypto_trading_pairs(is_active);
CREATE INDEX IF NOT EXISTS idx_crypto_price_history_currency ON crypto_price_history(currency_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_crypto_watchlist_user ON crypto_watchlist(user_id);
CREATE INDEX IF NOT EXISTS idx_crypto_staking_wallet ON crypto_staking(wallet_id);

-- Insert popular cryptocurrencies
INSERT INTO crypto_currencies (symbol, name, full_name, network, decimals, current_price_usd, is_active, is_tradeable) VALUES
-- Major Cryptocurrencies
('BTC', 'Bitcoin', 'Bitcoin', 'BITCOIN', 8, 95000.00, TRUE, TRUE),
('ETH', 'Ethereum', 'Ethereum', 'ETHEREUM', 18, 3400.00, TRUE, TRUE),
('BNB', 'BNB', 'Binance Coin', 'BINANCE_SMART_CHAIN', 18, 620.00, TRUE, TRUE),
('SOL', 'Solana', 'Solana', 'SOLANA', 9, 145.00, TRUE, TRUE),
('XRP', 'XRP', 'Ripple', 'RIPPLE', 6, 2.50, TRUE, TRUE),
('ADA', 'Cardano', 'Cardano', 'CARDANO', 6, 1.05, TRUE, TRUE),
('DOGE', 'Dogecoin', 'Dogecoin', 'DOGECOIN', 8, 0.38, TRUE, TRUE),
('MATIC', 'Polygon', 'Polygon', 'POLYGON', 18, 0.85, TRUE, TRUE),
('DOT', 'Polkadot', 'Polkadot', 'POLKADOT', 10, 7.50, TRUE, TRUE),
('AVAX', 'Avalanche', 'Avalanche', 'AVALANCHE', 18, 38.50, TRUE, TRUE),

-- Stablecoins
('USDT', 'Tether', 'Tether USD', 'ETHEREUM', 6, 1.00, TRUE, TRUE),
('USDC', 'USD Coin', 'USD Coin', 'ETHEREUM', 6, 1.00, TRUE, TRUE),
('BUSD', 'BUSD', 'Binance USD', 'BINANCE_SMART_CHAIN', 18, 1.00, TRUE, TRUE),
('DAI', 'DAI', 'Dai Stablecoin', 'ETHEREUM', 18, 1.00, TRUE, TRUE),

-- DeFi Tokens
('UNI', 'Uniswap', 'Uniswap', 'ETHEREUM', 18, 12.50, TRUE, TRUE),
('LINK', 'Chainlink', 'Chainlink', 'ETHEREUM', 18, 22.00, TRUE, TRUE),
('AAVE', 'Aave', 'Aave', 'ETHEREUM', 18, 310.00, TRUE, TRUE),

-- Meme Coins
('SHIB', 'Shiba Inu', 'Shiba Inu', 'ETHEREUM', 18, 0.00002500, TRUE, TRUE),
('PEPE', 'Pepe', 'Pepe', 'ETHEREUM', 18, 0.00001800, TRUE, TRUE)
ON CONFLICT (symbol) DO NOTHING;

-- Insert trading pairs (crypto/USD)
INSERT INTO crypto_trading_pairs (base_currency_id, quote_currency_id, symbol, current_price, bid_price, ask_price, trading_fee_percentage, min_order_size)
SELECT
    c1.id,
    c2.id,
    c1.symbol || '/' || c2.symbol,
    c1.current_price_usd,
    c1.current_price_usd * 0.999,
    c1.current_price_usd * 1.001,
    0.25,
    CASE
        WHEN c1.symbol = 'BTC' THEN 0.0001
        WHEN c1.symbol = 'ETH' THEN 0.001
        ELSE 1.0
    END
FROM crypto_currencies c1
CROSS JOIN crypto_currencies c2
WHERE c2.symbol = 'USDT' AND c1.symbol != 'USDT'
ON CONFLICT DO NOTHING;

COMMENT ON TABLE crypto_currencies IS 'Supported cryptocurrencies with real-time prices';
COMMENT ON TABLE crypto_wallets IS 'User cryptocurrency wallets';
COMMENT ON TABLE crypto_transactions IS 'All cryptocurrency transactions';
COMMENT ON TABLE crypto_orders IS 'Buy/sell orders for cryptocurrency trading';
COMMENT ON TABLE crypto_trading_pairs IS 'Available trading pairs with pricing';
COMMENT ON TABLE crypto_price_history IS 'Historical price data (OHLCV)';
COMMENT ON TABLE crypto_staking IS 'Cryptocurrency staking records';
