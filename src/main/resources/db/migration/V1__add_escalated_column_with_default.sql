-- Add escalated column with default value (only if it doesn't exist)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'disputes' 
        AND column_name = 'escalated'
    ) THEN
        ALTER TABLE disputes ADD COLUMN escalated BOOLEAN DEFAULT false;
    END IF;
END $$;

-- Update existing rows to have the default value
UPDATE disputes SET escalated = false WHERE escalated IS NULL;

-- Now add the NOT NULL constraint
ALTER TABLE disputes ALTER COLUMN escalated SET NOT NULL; 