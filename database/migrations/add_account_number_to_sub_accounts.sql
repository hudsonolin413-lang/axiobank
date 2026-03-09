-- Add account_number column to sub_accounts table
ALTER TABLE sub_accounts 
ADD COLUMN IF NOT EXISTS account_number VARCHAR(20) NULL;

-- Create unique index on account_number
CREATE UNIQUE INDEX IF NOT EXISTS sub_accounts_account_number_unique 
ON sub_accounts(account_number) 
WHERE account_number IS NOT NULL;

-- Optional: Generate account numbers for existing records
UPDATE sub_accounts 
SET account_number = '0' || LPAD(FLOOR(RANDOM() * 10000000000)::TEXT, 9, '0')
WHERE account_number IS NULL;
