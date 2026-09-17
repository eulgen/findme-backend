-- =============================================================
-- V9__seed_default_admin_and_support.sql
-- Seed des comptes par défaut Admin et Support
-- Mot de passe par défaut : "password"
-- (Hash BCrypt généré pour "password" : $2a$10$8.UnVuG9HLB0fT7veL.6V.C.h7h1RzCj/gG4gCqZlW1w1A5xO03dO)
-- =============================================================

INSERT INTO authservice.users (id, full_name, email, password_hash, account_verified, status, created_at)
VALUES 
    (1001, 'Super Admin', 'admin@geolink.com', '$2a$10$8.UnVuG9HLB0fT7veL.6V.C.h7h1RzCj/gG4gCqZlW1w1A5xO03dO', true, 'ACTIVE', CURRENT_TIMESTAMP),
    (1002, 'Support Agent', 'support@geolink.com', '$2a$10$8.UnVuG9HLB0fT7veL.6V.C.h7h1RzCj/gG4gCqZlW1w1A5xO03dO', true, 'ACTIVE', CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;

-- Associer Admin (1001) au rôle ADMIN
INSERT INTO authservice.user_roles (user_id, role_id)
SELECT 1001, id FROM authservice.roles WHERE name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- Associer Support (1002) au rôle SUPPORT_AGENT
INSERT INTO authservice.user_roles (user_id, role_id)
SELECT 1002, id FROM authservice.roles WHERE name = 'SUPPORT_AGENT'
ON CONFLICT DO NOTHING;
