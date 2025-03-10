-- This script can be run directly in PostgreSQL to fix the escalated column issue

-- Check if the column exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'disputes' 
        AND column_name = 'escalated'
    ) THEN
        -- If column doesn't exist, add it with a default value
        ALTER TABLE disputes ADD COLUMN escalated BOOLEAN DEFAULT false;
        
        -- Update all rows to have the default value
        UPDATE disputes SET escalated = false;
        
        -- Add the NOT NULL constraint
        ALTER TABLE disputes ALTER COLUMN escalated SET NOT NULL;
        
        RAISE NOTICE 'Column escalated added successfully with NOT NULL constraint';
    ELSE
        -- If column exists, update any NULL values to false
        UPDATE disputes SET escalated = false WHERE escalated IS NULL;
        
        -- Check if the column is nullable
        IF EXISTS (
            SELECT 1 
            FROM information_schema.columns 
            WHERE table_name = 'disputes' 
            AND column_name = 'escalated'
            AND is_nullable = 'YES'
        ) THEN
            -- Add the NOT NULL constraint
            ALTER TABLE disputes ALTER COLUMN escalated SET NOT NULL;
            RAISE NOTICE 'NOT NULL constraint added to existing escalated column';
        ELSE
            RAISE NOTICE 'Column escalated already exists with NOT NULL constraint';
        END IF;
    END IF;
END $$; 