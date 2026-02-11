-- ============================================================================
-- UPDATE ID FORMATS MIGRATION SCRIPT
-- ============================================================================
-- This script updates existing customer numbers and transaction references
-- to use the new format:
-- - Customer Numbers: 6 digits (e.g., 123456)
-- - Transaction References: TKG60ACDU9 format (3 letters + 2 digits + 5 alphanumeric)
--
-- NOTE: This is optional - new records will automatically use the new format
-- Run this only if you want to update existing records
-- ============================================================================

-- Function to generate random 6-digit customer number
-- Note: This is a simple sequential approach for PostgreSQL

-- Update customer numbers to 6-digit format
-- This will assign sequential 6-digit numbers starting from 100000
DO $$
DECLARE
    customer_record RECORD;
    new_customer_number VARCHAR(6);
    counter INT := 100000;
BEGIN
    FOR customer_record IN
        SELECT id, customer_number FROM customers
        WHERE LENGTH(customer_number) > 6
        ORDER BY created_at
    LOOP
        new_customer_number := LPAD(counter::TEXT, 6, '0');

        UPDATE customers
        SET customer_number = new_customer_number
        WHERE id = customer_record.id;

        counter := counter + 1;

        -- Ensure we don't exceed 999999
        IF counter > 999999 THEN
            counter := 100000;
        END IF;
    END LOOP;

    RAISE NOTICE 'Updated % customer numbers', counter - 100000;
END $$;

-- Function to generate random transaction reference in format: TKG60ACDU9
CREATE OR REPLACE FUNCTION generate_transaction_reference() RETURNS VARCHAR(10) AS $$
DECLARE
    letters CONSTANT TEXT := 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    digits CONSTANT TEXT := '0123456789';
    alphanumeric CONSTANT TEXT := 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    result TEXT := '';
    i INT;
BEGIN
    -- 3 random letters
    FOR i IN 1..3 LOOP
        result := result || SUBSTR(letters, FLOOR(RANDOM() * LENGTH(letters) + 1)::INT, 1);
    END LOOP;

    -- 2 random digits
    FOR i IN 1..2 LOOP
        result := result || SUBSTR(digits, FLOOR(RANDOM() * LENGTH(digits) + 1)::INT, 1);
    END LOOP;

    -- 5 random alphanumeric
    FOR i IN 1..5 LOOP
        result := result || SUBSTR(alphanumeric, FLOOR(RANDOM() * LENGTH(alphanumeric) + 1)::INT, 1);
    END LOOP;

    RETURN result;
END;
$$ LANGUAGE plpgsql;

-- Update transaction references to new format
-- Only update NULL or empty references
UPDATE transactions
SET reference = generate_transaction_reference()
WHERE reference IS NULL OR reference = '' OR LENGTH(reference) = 0;

-- Update existing non-standard references (optional - uncomment if needed)
-- UPDATE transactions
-- SET reference = generate_transaction_reference()
-- WHERE LENGTH(reference) != 10;

-- Clean up the function (optional)
-- DROP FUNCTION IF EXISTS generate_transaction_reference();

-- Verify the updates
SELECT 'Customer Numbers Updated' AS status, COUNT(*) AS count
FROM customers
WHERE LENGTH(customer_number) = 6
UNION ALL
SELECT 'Transaction References Updated' AS status, COUNT(*) AS count
FROM transactions
WHERE LENGTH(reference) = 10;

-- ============================================================================
-- NOTES:
-- ============================================================================
-- 1. The new IdGenerator class will automatically generate correct formats
--    for all new records
-- 2. Existing records with old formats will continue to work
-- 3. Run this script only if you want consistency across all records
-- 4. Backup your database before running this script
-- ============================================================================
