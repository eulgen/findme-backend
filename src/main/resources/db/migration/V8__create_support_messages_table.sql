-- =============================================================
-- V8__create_support_messages_table.sql
-- Table des messages / demandes de support client
-- =============================================================

CREATE TABLE IF NOT EXISTS support_messages (
    id         BIGSERIAL                   PRIMARY KEY,
    user_id    BIGINT                      REFERENCES users(id) ON DELETE SET NULL,
    name       VARCHAR(100)                NOT NULL,
    email      VARCHAR(255)                NOT NULL,
    message    TEXT                        NOT NULL,
    status     VARCHAR(20)                 NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_support_messages_status ON support_messages(status);
CREATE INDEX idx_support_messages_user_id ON support_messages(user_id);
