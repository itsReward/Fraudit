-- V2__fix_audit_log_sequence.sql

-- Make sure the sequences are properly configured for PostgreSQL
ALTER TABLE audit_log ALTER COLUMN log_id SET DEFAULT nextval('audit_log_log_id_seq');

-- Reset and properly configure the sequence
SELECT setval('audit_log_log_id_seq', COALESCE((SELECT MAX(log_id) FROM audit_log), 1), false);