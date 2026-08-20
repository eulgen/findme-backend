-- =============================================================
-- V6__create_addresses_and_gps_tables.sql
-- Création des tables gps_coordinates, addresses et de la jonction user_addresses
-- =============================================================

-- Table des coordonnées GPS (1:1 avec addresses)
CREATE TABLE IF NOT EXISTS gps_coordinates (
    id        BIGSERIAL        PRIMARY KEY,
    latitude  DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL
);

-- Table des adresses
CREATE TABLE IF NOT EXISTS addresses (
    id                BIGSERIAL                   PRIMARY KEY,
    address_code      VARCHAR(50)                 NOT NULL UNIQUE,
    country           VARCHAR(100)                NOT NULL,
    city              VARCHAR(100)                NOT NULL,
    street            VARCHAR(150),
    house_number      VARCHAR(50),
    photo_url         VARCHAR(500),
    gps_coordinate_id BIGINT UNIQUE REFERENCES gps_coordinates(id) ON DELETE CASCADE,
    created_at        TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Table de jonction N..N : utilisateurs <-> adresses
CREATE TABLE IF NOT EXISTS user_addresses (
    user_id     BIGINT                      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    address_id  BIGINT                      NOT NULL REFERENCES addresses(id) ON DELETE CASCADE,
    attached_at TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, address_id)
);

-- Index pour accélérer les requêtes de recherche sur les adresses
CREATE INDEX IF NOT EXISTS idx_addresses_code ON addresses(address_code);
CREATE INDEX IF NOT EXISTS idx_addresses_country_city ON addresses(country, city);
