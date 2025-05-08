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