-- =============================================================
-- V1__init_schema.sql
-- Création du schéma authservice et des tables RBAC + tokens
-- =============================================================

CREATE SCHEMA IF NOT EXISTS authservice;

-- Table des utilisateurs
CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL                   PRIMARY KEY,
    email         VARCHAR(255)                NOT NULL UNIQUE,
    password_hash VARCHAR(255)                NOT NULL,
    first_name    VARCHAR(100)                NOT NULL,
    last_name     VARCHAR(100)                NOT NULL,
    status        VARCHAR(20)                 NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP WITH TIME ZONE
);

-- Table des rôles
CREATE TABLE IF NOT EXISTS roles (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(50)     NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- Table des permissions
CREATE TABLE IF NOT EXISTS permissions (
    id          BIGSERIAL       PRIMARY KEY,
    code        VARCHAR(100)    NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- Table de jointure N..N : utilisateurs <-> rôles
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Table de jointure N..N : rôles <-> permissions
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id       BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Refresh tokens (hashés SHA-256, rotation à chaque utilisation)
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id         BIGSERIAL                   PRIMARY KEY,
    user_id    BIGINT                      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255)                NOT NULL UNIQUE,
    expiration TIMESTAMP WITH TIME ZONE    NOT NULL,
    revoked    BOOLEAN                     NOT NULL DEFAULT FALSE
);

-- Tokens de réinitialisation de mot de passe (hashés SHA-256)
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id          BIGSERIAL                   PRIMARY KEY,
    user_id     BIGINT                      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255)                NOT NULL UNIQUE,
    expiry_date TIMESTAMP WITH TIME ZONE    NOT NULL,
    used        BOOLEAN                     NOT NULL DEFAULT FALSE
);
