ALTER TABLE users ADD COLUMN account_verified BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS otp_codes (
    id            UUID PRIMARY KEY,
    user_id       BIGINT                   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_hash     VARCHAR(255)             NOT NULL,
    purpose       VARCHAR(30)              NOT NULL,
    expires_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed      BOOLEAN                  NOT NULL DEFAULT FALSE,
    attempt_count INT                      NOT NULL DEFAULT 0,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_otp_user_purpose ON otp_codes(user_id, purpose);
