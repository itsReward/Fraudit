-- V1__complete_schema_setup.sql
-- Consolidated database setup script that replaces V1, V2, V3, and V4

-- Create sequences
CREATE SEQUENCE IF NOT EXISTS altman_z_score_z_score_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS audit_log_log_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS beneish_m_score_m_score_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS companies_company_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS document_storage_document_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS financial_data_data_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS financial_ratios_ratio_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS financial_statements_statement_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS fiscal_years_fiscal_year_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS fraud_risk_assessment_assessment_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ml_features_feature_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ml_models_model_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ml_predictions_prediction_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS piotroski_f_score_f_score_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS risk_alerts_alert_id_seq START WITH 1 INCREMENT BY 1;

-- Create tables with bigint IDs to match Kotlin Long types
CREATE TABLE altman_z_score
(
    z_score_id                             BIGINT PRIMARY KEY,
    statement_id                           BIGINT                                  NOT NULL,
    working_capital_to_total_assets        numeric(10, 4),
    retained_earnings_to_total_assets      numeric(10, 4),
    ebit_to_total_assets                   numeric(10, 4),
    market_value_equity_to_book_value_debt numeric(10, 4),
    sales_to_total_assets                  numeric(10, 4),
    z_score                                numeric(10, 4),
    risk_category                          VARCHAR(20),
    calculated_at                          TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE TABLE users
(
    user_id        UUID         DEFAULT gen_random_uuid() NOT NULL,
    username       VARCHAR(100)                           NOT NULL,
    email          VARCHAR(255)                           NOT NULL,
    password       VARCHAR(255)                           NOT NULL,
    first_name     VARCHAR(100),
    last_name      VARCHAR(100),
    role           VARCHAR(20)  DEFAULT 'ANALYST',
    active         BOOLEAN      DEFAULT TRUE,
    remember_token VARCHAR(255),
    created_at     TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at     TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    CONSTRAINT users_pkey PRIMARY KEY (user_id)
);

CREATE TABLE audit_log
(
    log_id      BIGINT PRIMARY KEY,
    user_id     UUID,
    action      VARCHAR(100)                             NOT NULL,
    entity_type VARCHAR(50)                              NOT NULL,
    entity_id   VARCHAR(50)                              NOT NULL,
    details     TEXT,
    ip_address  VARCHAR(45),
    timestamp   TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE TABLE beneish_m_score
(
    m_score_id                     BIGINT PRIMARY KEY,
    statement_id                   BIGINT                                  NOT NULL,
    days_sales_receivables_index   numeric(10, 4),
    gross_margin_index             numeric(10, 4),
    asset_quality_index            numeric(10, 4),
    sales_growth_index             numeric(10, 4),
    depreciation_index             numeric(10, 4),
    sg_admin_expenses_index        numeric(10, 4),
    leverage_index                 numeric(10, 4),
    total_accruals_to_total_assets numeric(10, 4),
    m_score                        numeric(10, 4),
    manipulation_probability       VARCHAR(20),
    calculated_at                  TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE TABLE companies
(
    company_id   BIGINT PRIMARY KEY,
    company_name VARCHAR(255)                             NOT NULL,
    stock_code   VARCHAR(20)                              NOT NULL,
    sector       VARCHAR(100),
    listing_date date,
    description  TEXT,
    created_at   TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at   TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE TABLE fiscal_years
(
    fiscal_year_id BIGINT PRIMARY KEY,
    company_id     BIGINT                                  NOT NULL,
    year           INTEGER                                  NOT NULL,
    start_date     date                                     NOT NULL,
    end_date       date                                     NOT NULL,
    is_audited     BOOLEAN DEFAULT FALSE,
    created_at     TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE TABLE financial_statements
(
    statement_id   BIGINT PRIMARY KEY,
    fiscal_year_id BIGINT                                  NOT NULL,
    user_id        UUID                                     NOT NULL,
    statement_type VARCHAR(20)                              NOT NULL,
    period VARCHAR (20),
    upload_date    TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    status         VARCHAR(20) DEFAULT 'PENDING'
);

CREATE TABLE document_storage
(
    document_id  BIGINT PRIMARY KEY,
    statement_id BIGINT                                  NOT NULL,
    file_name    VARCHAR(255)                             NOT NULL,
    file_type    VARCHAR(50)                              NOT NULL,
    file_size    INTEGER                                  NOT NULL,
    file_path    VARCHAR(255)                             NOT NULL,
    upload_date  TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE TABLE financial_data
(
    data_id                       BIGINT PRIMARY KEY,
    statement_id                  BIGINT                                  NOT NULL,
    revenue                       numeric(20, 2),
    cost_of_sales                 numeric(20, 2),
    gross_profit                  numeric(20, 2),
    operating_expenses            numeric(20, 2),
    administrative_expenses       numeric(20, 2),
    selling_expenses              numeric(20, 2),
    depreciation                  numeric(20, 2),
    amortization                  numeric(20, 2),
    operating_income              numeric(20, 2),
    interest_expense              numeric(20, 2),
    other_income                  numeric(20, 2),
    earnings_before_tax           numeric(20, 2),
    income_tax                    numeric(20, 2),
    net_income                    numeric(20, 2),
    cash                          numeric(20, 2),
    short_term_investments        numeric(20, 2),
    accounts_receivable           numeric(20, 2),
    inventory                     numeric(20, 2),
    other_current_assets          numeric(20, 2),
    total_current_assets          numeric(20, 2),
    property_plant_equipment      numeric(20, 2),
    accumulated_depreciation      numeric(20, 2),
    intangible_assets             numeric(20, 2),
    long_term_investments         numeric(20, 2),
    other_non_current_assets      numeric(20, 2),
    total_non_current_assets      numeric(20, 2),
    total_assets                  numeric(20, 2),
    accounts_payable              numeric(20, 2),
    short_term_debt               numeric(20, 2),
    accrued_liabilities           numeric(20, 2),
    other_current_liabilities     numeric(20, 2),
    total_current_liabilities     numeric(20, 2),
    long_term_debt                numeric(20, 2),
    deferred_taxes                numeric(20, 2),
    other_non_current_liabilities numeric(20, 2),
    total_non_current_liabilities numeric(20, 2),
    total_liabilities             numeric(20, 2),
    common_stock                  numeric(20, 2),
    additional_paid_in_capital    numeric(20, 2),
    retained_earnings             numeric(20, 2),
    treasury_stock                numeric(20, 2),
    other_equity                  numeric(20, 2),
    total_equity                  numeric(20, 2),
    net_cash_from_operating       numeric(20, 2),
    net_cash_from_investing       numeric(20, 2),
    net_cash_from_financing       numeric(20, 2),
    net_change_in_cash            numeric(20, 2),
    market_capitalization         numeric(20, 2),
    shares_outstanding            numeric(20, 2),
    market_price_per_share        numeric(20, 2),
    book_value_per_share          numeric(20, 2),
    earnings_per_share            numeric(20, 2),
    revenue_growth                numeric(10, 4),
    gross_profit_growth           numeric(10, 4),
    net_income_growth             numeric(10, 4),
    asset_growth                  numeric(10, 4),
    receivables_growth            numeric(10, 4),
    inventory_growth              numeric(10, 4),
    liability_growth              numeric(10, 4),
    created_at                    TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at                    TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE TABLE financial_ratios
(
    ratio_id                     BIGINT PRIMARY KEY,
    statement_id                 BIGINT                                  NOT NULL,
    current_ratio                numeric(10, 4),
    quick_ratio                  numeric(10, 4),
    cash_ratio                   numeric(10, 4),
    gross_margin                 numeric(10, 4),
    operating_margin             numeric(10, 4),
    net_profit_margin            numeric(10, 4),
    return_on_assets             numeric(10, 4),
    return_on_equity             numeric(10, 4),
    asset_turnover               numeric(10, 4),
    inventory_turnover           numeric(10, 4),
    accounts_receivable_turnover numeric(10, 4),
    days_sales_outstanding       numeric(10, 4),
    debt_to_equity               numeric(10, 4),
    debt_ratio                   numeric(10, 4),
    interest_coverage            numeric(10, 4),
    price_to_earnings            numeric(10, 4),
    price_to_book                numeric(10, 4),
    accrual_ratio                numeric(10, 4),
    earnings_quality             numeric(10, 4),
    calculated_at                TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE TABLE fraud_risk_assessment
(
    assessment_id        BIGINT PRIMARY KEY,
    statement_id         BIGINT                                  NOT NULL,
    z_score_risk         numeric(5, 2),
    m_score_risk         numeric(5, 2),
    f_score_risk         numeric(5, 2),
    financial_ratio_risk numeric(5, 2),
    ml_prediction_risk   numeric(5, 2),
    overall_risk_score   numeric(5, 2),
    risk_level           VARCHAR(20),
    assessment_summary   TEXT,
    assessed_at          TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    assessed_by          UUID
);

CREATE TABLE ml_features
(
    feature_id   BIGINT PRIMARY KEY,
    statement_id BIGINT                                  NOT NULL,
    feature_set  JSONB                                    NOT NULL,
    created_at   TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE TABLE ml_models
(
    model_id            BIGINT PRIMARY KEY,
    model_name          VARCHAR(100)                             NOT NULL,
    model_type          VARCHAR(50) DEFAULT 'RANDOM_FOREST'      NOT NULL,
    model_version       VARCHAR(20)                              NOT NULL,
    feature_list        TEXT                                     NOT NULL,
    performance_metrics TEXT                                     NOT NULL,
    trained_date        TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    is_active           BOOLEAN     DEFAULT FALSE                NOT NULL,
    model_path          VARCHAR(255)                             NOT NULL,
    created_by          UUID
);

CREATE TABLE ml_predictions
(
    prediction_id          BIGINT PRIMARY KEY,
    statement_id           BIGINT                                  NOT NULL,
    model_id               BIGINT                                  NOT NULL,
    fraud_probability      numeric(10, 6)                           NOT NULL,
    feature_importance     JSONB,
    prediction_explanation TEXT,
    predicted_at           TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE TABLE piotroski_f_score
(
    f_score_id                        BIGINT PRIMARY KEY,
    statement_id                      BIGINT                                  NOT NULL,
    positive_net_income               BOOLEAN,
    positive_operating_cash_flow      BOOLEAN,
    cash_flow_greater_than_net_income BOOLEAN,
    improving_roa                     BOOLEAN,
    decreasing_leverage               BOOLEAN,
    improving_current_ratio           BOOLEAN,
    no_new_shares                     BOOLEAN,
    improving_gross_margin            BOOLEAN,
    improving_asset_turnover          BOOLEAN,
    f_score                           INTEGER,
    financial_strength                VARCHAR(20),
    calculated_at                     TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE TABLE risk_alerts
(
    alert_id         BIGINT PRIMARY KEY,
    assessment_id    BIGINT                                  NOT NULL,
    alert_type       VARCHAR(50)                              NOT NULL,
    severity         VARCHAR(20)                              NOT NULL,
    message          TEXT                                     NOT NULL,
    created_at       TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    is_resolved      BOOLEAN DEFAULT FALSE                    NOT NULL,
    resolved_by      UUID,
    resolved_at      TIMESTAMP WITHOUT TIME ZONE,
    resolution_notes TEXT
);

CREATE TABLE refresh_tokens
(
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    user_id     UUID NOT NULL,
    expiry_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- Add unique constraints
ALTER TABLE altman_z_score
    ADD CONSTRAINT altman_z_score_statement_id_key UNIQUE (statement_id);

ALTER TABLE beneish_m_score
    ADD CONSTRAINT beneish_m_score_statement_id_key UNIQUE (statement_id);

ALTER TABLE financial_data
    ADD CONSTRAINT financial_data_statement_id_key UNIQUE (statement_id);

ALTER TABLE financial_ratios
    ADD CONSTRAINT financial_ratios_statement_id_key UNIQUE (statement_id);

ALTER TABLE fiscal_years
    ADD CONSTRAINT fiscal_years_company_id_year_key UNIQUE (company_id, year);

ALTER TABLE fraud_risk_assessment
    ADD CONSTRAINT fraud_risk_assessment_statement_id_key UNIQUE (statement_id);

ALTER TABLE ml_features
    ADD CONSTRAINT ml_features_statement_id_key UNIQUE (statement_id);

ALTER TABLE piotroski_f_score
    ADD CONSTRAINT piotroski_f_score_statement_id_key UNIQUE (statement_id);

ALTER TABLE users
    ADD CONSTRAINT users_email_key UNIQUE (email);

ALTER TABLE users
    ADD CONSTRAINT users_username_key UNIQUE (username);

-- Create indexes
CREATE UNIQUE INDEX companies_name_idx ON companies (company_name);
CREATE UNIQUE INDEX companies_stock_code_idx ON companies (stock_code);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_expiry_date ON refresh_tokens(expiry_date);

-- Add foreign key constraints
ALTER TABLE risk_alerts
    ADD CONSTRAINT fk_alert_assessment FOREIGN KEY (assessment_id) REFERENCES fraud_risk_assessment (assessment_id) ON DELETE CASCADE;

CREATE INDEX risk_alerts_assessment_idx ON risk_alerts (assessment_id);

ALTER TABLE risk_alerts
    ADD CONSTRAINT fk_alert_user FOREIGN KEY (resolved_by) REFERENCES users (user_id) ON DELETE SET NULL;

ALTER TABLE fraud_risk_assessment
    ADD CONSTRAINT fk_assessment_statement FOREIGN KEY (statement_id) REFERENCES financial_statements (statement_id) ON DELETE CASCADE;

ALTER TABLE fraud_risk_assessment
    ADD CONSTRAINT fk_assessment_user FOREIGN KEY (assessed_by) REFERENCES users (user_id) ON DELETE SET NULL;

ALTER TABLE audit_log
    ADD CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE SET NULL;

CREATE INDEX audit_log_user_idx ON audit_log (user_id);

ALTER TABLE document_storage
    ADD CONSTRAINT fk_document_statement FOREIGN KEY (statement_id) REFERENCES financial_statements (statement_id) ON DELETE CASCADE;

CREATE INDEX document_storage_statement_idx ON document_storage (statement_id);

ALTER TABLE piotroski_f_score
    ADD CONSTRAINT fk_f_score_statement FOREIGN KEY (statement_id) REFERENCES financial_statements (statement_id) ON DELETE CASCADE;

ALTER TABLE ml_features
    ADD CONSTRAINT fk_features_statement FOREIGN KEY (statement_id) REFERENCES financial_statements (statement_id) ON DELETE CASCADE;

ALTER TABLE financial_data
    ADD CONSTRAINT fk_financial_data_statement FOREIGN KEY (statement_id) REFERENCES financial_statements (statement_id) ON DELETE CASCADE;

ALTER TABLE fiscal_years
    ADD CONSTRAINT fk_fiscal_company FOREIGN KEY (company_id) REFERENCES companies (company_id) ON DELETE CASCADE;

ALTER TABLE beneish_m_score
    ADD CONSTRAINT fk_m_score_statement FOREIGN KEY (statement_id) REFERENCES financial_statements (statement_id) ON DELETE CASCADE;

ALTER TABLE ml_models
    ADD CONSTRAINT fk_model_user FOREIGN KEY (created_by) REFERENCES users (user_id) ON DELETE SET NULL;

ALTER TABLE ml_predictions
    ADD CONSTRAINT fk_prediction_model FOREIGN KEY (model_id) REFERENCES ml_models (model_id) ON DELETE CASCADE;

CREATE INDEX ml_predictions_model_idx ON ml_predictions (model_id);

ALTER TABLE ml_predictions
    ADD CONSTRAINT fk_prediction_statement FOREIGN KEY (statement_id) REFERENCES financial_statements (statement_id) ON DELETE CASCADE;

CREATE INDEX ml_predictions_statement_idx ON ml_predictions (statement_id);

ALTER TABLE financial_ratios
    ADD CONSTRAINT fk_ratios_statement FOREIGN KEY (statement_id) REFERENCES financial_statements (statement_id) ON DELETE CASCADE;

ALTER TABLE financial_statements
    ADD CONSTRAINT fk_statement_fiscal FOREIGN KEY (fiscal_year_id) REFERENCES fiscal_years (fiscal_year_id) ON DELETE CASCADE;

CREATE INDEX financial_statements_fiscal_year_idx ON financial_statements (fiscal_year_id);

ALTER TABLE financial_statements
    ADD CONSTRAINT fk_statement_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE;

CREATE INDEX financial_statements_user_idx ON financial_statements (user_id);

ALTER TABLE altman_z_score
    ADD CONSTRAINT fk_z_score_statement FOREIGN KEY (statement_id) REFERENCES financial_statements (statement_id) ON DELETE CASCADE;

ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

-- Fix sequences to use BIGINT for auto-generated IDs
ALTER SEQUENCE altman_z_score_z_score_id_seq AS BIGINT;
ALTER SEQUENCE audit_log_log_id_seq AS BIGINT;
ALTER SEQUENCE beneish_m_score_m_score_id_seq AS BIGINT;
ALTER SEQUENCE companies_company_id_seq AS BIGINT;
ALTER SEQUENCE document_storage_document_id_seq AS BIGINT;
ALTER SEQUENCE financial_data_data_id_seq AS BIGINT;
ALTER SEQUENCE financial_ratios_ratio_id_seq AS BIGINT;
ALTER SEQUENCE financial_statements_statement_id_seq AS BIGINT;
ALTER SEQUENCE fiscal_years_fiscal_year_id_seq AS BIGINT;
ALTER SEQUENCE fraud_risk_assessment_assessment_id_seq AS BIGINT;
ALTER SEQUENCE ml_features_feature_id_seq AS BIGINT;
ALTER SEQUENCE ml_models_model_id_seq AS BIGINT;
ALTER SEQUENCE ml_predictions_prediction_id_seq AS BIGINT;
ALTER SEQUENCE piotroski_f_score_f_score_id_seq AS BIGINT;
ALTER SEQUENCE risk_alerts_alert_id_seq AS BIGINT;