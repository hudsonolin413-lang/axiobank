-- Migration: Create Cards Table
-- Description: Adds support for storing user credit and debit cards
-- Date: 2026-02-08

-- Create cards table
CREATE TABLE IF NOT EXISTS cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    card_holder_name VARCHAR(255) NOT NULL,
    card_type VARCHAR(50) NOT NULL CHECK (card_type IN ('CREDIT', 'DEBIT')),
    card_brand VARCHAR(50) NOT NULL CHECK (card_brand IN ('VISA', 'MASTERCARD', 'AMERICAN_EXPRESS', 'DISCOVER', 'UNKNOWN')),
    card_number_hash VARCHAR(255) NOT NULL,
    last_four_digits VARCHAR(4) NOT NULL,
    expiry_month INTEGER NOT NULL CHECK (expiry_month >= 1 AND expiry_month <= 12),
    expiry_year INTEGER NOT NULL CHECK (expiry_year >= 2020),
    cvv_hash VARCHAR(255),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_VERIFICATION' CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'BLOCKED', 'EXPIRED', 'INVALID')),
    nickname VARCHAR(100),
    added_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_date TIMESTAMP,
    last_used_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cards_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_cards_user_id ON cards(user_id);
CREATE INDEX IF NOT EXISTS idx_cards_is_default ON cards(is_default);
CREATE INDEX IF NOT EXISTS idx_cards_is_active ON cards(is_active);
CREATE INDEX IF NOT EXISTS idx_cards_status ON cards(status);
CREATE INDEX IF NOT EXISTS idx_cards_added_date ON cards(added_date DESC);

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_cards_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_cards_updated_at
    BEFORE UPDATE ON cards
    FOR EACH ROW
    EXECUTE FUNCTION update_cards_updated_at();

-- Create trigger to ensure only one default card per user
CREATE OR REPLACE FUNCTION ensure_single_default_card()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_default = TRUE THEN
        UPDATE cards
        SET is_default = FALSE
        WHERE user_id = NEW.user_id
          AND id != NEW.id
          AND is_default = TRUE;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_ensure_single_default_card
    BEFORE INSERT OR UPDATE ON cards
    FOR EACH ROW
    EXECUTE FUNCTION ensure_single_default_card();

-- Add comments to document the table
COMMENT ON TABLE cards IS 'Stores user credit and debit card information with encrypted data';
COMMENT ON COLUMN cards.card_number_hash IS 'Hashed card number using BCrypt - never store in plain text';
COMMENT ON COLUMN cards.last_four_digits IS 'Last 4 digits of card number for display purposes';
COMMENT ON COLUMN cards.cvv_hash IS 'Hashed CVV - typically not stored per PCI DSS compliance';
COMMENT ON COLUMN cards.is_default IS 'Whether this is the default payment card for the user';
COMMENT ON COLUMN cards.is_active IS 'Whether the card is active (soft delete)';
COMMENT ON COLUMN cards.status IS 'Card verification and activation status';
