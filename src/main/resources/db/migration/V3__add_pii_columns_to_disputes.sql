-- Add user information columns from submitted documents
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS submitted_user_full_name VARCHAR(100);
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS submitted_user_address VARCHAR(255);
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS submitted_user_phone_number VARCHAR(20);
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS submitted_user_email_address VARCHAR(100);
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS description VARCHAR(1000);

-- Add PII validation status column
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS pii_validation_status VARCHAR(20) DEFAULT 'PENDING';

-- Create index for user ID to optimize lookups
CREATE INDEX IF NOT EXISTS idx_dispute_user_id ON disputes(user_id);

-- Set default values for existing records
UPDATE disputes
SET pii_validation_status = 'PENDING'
WHERE pii_validation_status IS NULL; 