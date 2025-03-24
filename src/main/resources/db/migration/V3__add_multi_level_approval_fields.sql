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

-- Add default senior officers and compliance officers
INSERT INTO case_officers (username, full_name, email, role, available, case_load, max_case_load)
VALUES 
('senior_officer1', 'Senior Officer 1', 'senior1@example.com', 'SENIOR_OFFICER', TRUE, 0, 8),
('senior_officer2', 'Senior Officer 2', 'senior2@example.com', 'SENIOR_OFFICER', TRUE, 0, 8),
('compliance_officer1', 'Compliance Officer 1', 'compliance1@example.com', 'COMPLIANCE_OFFICER', TRUE, 0, 5),
('compliance_officer2', 'Compliance Officer 2', 'compliance2@example.com', 'COMPLIANCE_OFFICER', TRUE, 0, 5)
ON CONFLICT (username) DO NOTHING; 