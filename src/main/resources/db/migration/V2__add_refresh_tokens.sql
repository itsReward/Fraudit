-- First, drop dependent foreign keys
ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS fk_audit_user;
ALTER TABLE financial_statements DROP CONSTRAINT IF EXISTS fk_statement_user;
ALTER TABLE fraud_risk_assessment DROP CONSTRAINT IF EXISTS fk_assessment_user;
ALTER TABLE ml_models DROP CONSTRAINT IF EXISTS fk_model_user;
ALTER TABLE risk_alerts DROP CONSTRAINT IF EXISTS fk_alert_user;

-- Drop the refresh_tokens table if it exists (migration V2 might have failed)
DROP TABLE IF EXISTS refresh_tokens;

-- Change user_id column in the users table
ALTER TABLE users
ALTER COLUMN user_id TYPE UUID USING user_id::UUID;

-- Now recreate all foreign key constraints
ALTER TABLE audit_log
    ADD CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE SET NULL;

ALTER TABLE financial_statements
    ADD CONSTRAINT fk_statement_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE;

ALTER TABLE fraud_risk_assessment
    ADD CONSTRAINT fk_assessment_user FOREIGN KEY (assessed_by) REFERENCES users (user_id) ON DELETE SET NULL;

ALTER TABLE ml_models
    ADD CONSTRAINT fk_model_user FOREIGN KEY (created_by) REFERENCES users (user_id) ON DELETE SET NULL;

ALTER TABLE risk_alerts
    ADD CONSTRAINT fk_alert_user FOREIGN KEY (resolved_by) REFERENCES users (user_id) ON DELETE SET NULL;

-- Create the refresh_tokens table with UUID data type
CREATE TABLE refresh_tokens (
                                id BIGSERIAL PRIMARY KEY,
                                token VARCHAR(255) NOT NULL UNIQUE,
                                user_id UUID NOT NULL,
                                expiry_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                                created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                                CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_expiry_date ON refresh_tokens(expiry_date);