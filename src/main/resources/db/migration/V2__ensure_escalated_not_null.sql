-- Make sure all rows have a value for escalated
UPDATE disputes SET escalated = false WHERE escalated IS NULL;

-- Try to add NOT NULL constraint if it doesn't already have it
DO $$
BEGIN
    -- Check if the column exists and is nullable
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'disputes' 
        AND column_name = 'escalated'
        AND is_nullable = 'YES'
    ) THEN
        -- Add the NOT NULL constraint
        ALTER TABLE disputes ALTER COLUMN escalated SET NOT NULL;
    END IF;
END $$; 