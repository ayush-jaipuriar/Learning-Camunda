-- Add multi-level approval fields to dispute table
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS level1_approval_status VARCHAR(20) DEFAULT 'PENDING';
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS level2_approval_status VARCHAR(20) DEFAULT 'PENDING';
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS level3_approval_status VARCHAR(20) DEFAULT 'PENDING';

ALTER TABLE disputes ADD COLUMN IF NOT EXISTS level1_approver_username VARCHAR(100);
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS level2_approver_username VARCHAR(100);
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS level3_approver_username VARCHAR(100);

ALTER TABLE disputes ADD COLUMN IF NOT EXISTS level1_approval_notes TEXT;
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS level2_approval_notes TEXT;
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS level3_approval_notes TEXT;

ALTER TABLE disputes ADD COLUMN IF NOT EXISTS level1_approval_timestamp TIMESTAMP;
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS level2_approval_timestamp TIMESTAMP;
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS level3_approval_timestamp TIMESTAMP;

ALTER TABLE disputes ADD COLUMN IF NOT EXISTS level1_escalated BOOLEAN DEFAULT FALSE;
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS level2_escalated BOOLEAN DEFAULT FALSE;
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS level3_escalated BOOLEAN DEFAULT FALSE;

-- Update case_officers table to add role and availability
ALTER TABLE case_officers ADD COLUMN IF NOT EXISTS role VARCHAR(50) DEFAULT 'OFFICER';
ALTER TABLE case_officers ADD COLUMN IF NOT EXISTS available BOOLEAN DEFAULT TRUE;
ALTER TABLE case_officers ADD COLUMN IF NOT EXISTS case_load INT DEFAULT 0;
ALTER TABLE case_officers ADD COLUMN IF NOT EXISTS max_case_load INT DEFAULT 10;

-- Drop the NOT NULL constraint on current_workload if it exists
ALTER TABLE case_officers ALTER COLUMN current_workload DROP NOT NULL;

-- Add current_workload and max_workload columns if they don't exist
ALTER TABLE case_officers ADD COLUMN IF NOT EXISTS current_workload INT DEFAULT 0;
ALTER TABLE case_officers ADD COLUMN IF NOT EXISTS max_workload INT DEFAULT 10;

-- Add unique constraint on username column
ALTER TABLE case_officers ADD CONSTRAINT unique_username UNIQUE (username);

-- Set values for current_workload and max_workload based on case_load and max_case_load
UPDATE case_officers SET current_workload = case_load WHERE current_workload IS NULL;
UPDATE case_officers SET max_workload = max_case_load WHERE max_workload IS NULL;

-- Add default senior officers and compliance officers
INSERT INTO case_officers (username, full_name, email, role, available, case_load, max_case_load, current_workload, max_workload)
VALUES 
('senior_officer1', 'Senior Officer 1', 'senior1@example.com', 'SENIOR_OFFICER', TRUE, 0, 8, 0, 8),
('senior_officer2', 'Senior Officer 2', 'senior2@example.com', 'SENIOR_OFFICER', TRUE, 0, 8, 0, 8),
('compliance_officer1', 'Compliance Officer 1', 'compliance1@example.com', 'COMPLIANCE_OFFICER', TRUE, 0, 5, 0, 5),
('compliance_officer2', 'Compliance Officer 2', 'compliance2@example.com', 'COMPLIANCE_OFFICER', TRUE, 0, 5, 0, 5)
ON CONFLICT (username) DO NOTHING; 