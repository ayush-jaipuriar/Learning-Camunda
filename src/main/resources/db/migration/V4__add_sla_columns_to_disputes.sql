-- Add SLA tracking columns to disputes table
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS sla_deadline TIMESTAMP;
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS reminders_sent INTEGER DEFAULT 0;
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS compliance_report_generated BOOLEAN DEFAULT false;

-- For existing records, set the SLA deadline to 5 minutes from now
UPDATE disputes 
SET 
    sla_deadline = now() + interval '5 minutes',
    reminders_sent = 0,
    compliance_report_generated = false
WHERE 
    sla_deadline IS NULL; 