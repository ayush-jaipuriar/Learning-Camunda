-- Check if the column exists
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'disputes' 
        AND column_name = 'escalated'
    ) THEN
        -- If column exists, update any NULL values to false
        UPDATE disputes SET escalated = false WHERE escalated IS NULL;
    ELSE
        -- If column doesn't exist, add it with a default value
        ALTER TABLE disputes ADD COLUMN escalated BOOLEAN DEFAULT false;
        
        -- Update all rows to have the default value
        UPDATE disputes SET escalated = false;
        
        -- Add the NOT NULL constraint
        ALTER TABLE disputes ALTER COLUMN escalated SET NOT NULL;
    END IF;
END $$; 