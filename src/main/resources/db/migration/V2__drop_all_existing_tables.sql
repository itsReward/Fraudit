-- Drop all tables in the correct order to respect foreign key constraints
DROP TABLE IF EXISTS risk_alerts CASCADE;
DROP TABLE IF EXISTS fraud_risk_assessment CASCADE;
DROP TABLE IF EXISTS ml_predictions CASCADE;
DROP TABLE IF EXISTS ml_features CASCADE;
DROP TABLE IF EXISTS ml_models CASCADE;
DROP TABLE IF EXISTS piotroski_f_score CASCADE;
DROP TABLE IF EXISTS beneish_m_score CASCADE;
DROP TABLE IF EXISTS altman_z_score CASCADE;
DROP TABLE IF EXISTS financial_ratios CASCADE;
DROP TABLE IF EXISTS document_storage CASCADE;
DROP TABLE IF EXISTS financial_data CASCADE;
DROP TABLE IF EXISTS financial_statements CASCADE;
DROP TABLE IF EXISTS fiscal_years CASCADE;
DROP TABLE IF EXISTS refresh_tokens CASCADE;
DROP TABLE IF EXISTS audit_log CASCADE;
DROP TABLE IF EXISTS companies CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- Also drop the flyway schema history so we can start fresh
DROP TABLE IF EXISTS flyway_schema_history CASCADE;