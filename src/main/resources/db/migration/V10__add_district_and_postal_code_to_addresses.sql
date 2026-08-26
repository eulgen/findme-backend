-- =============================================================
-- V10__add_district_and_postal_code_to_addresses.sql
-- Ajout du quartier (district, obligatoire) et du code postal
-- (postal_code, optionnel) à la table addresses
-- =============================================================

-- Ajout de la colonne district (quartier) - obligatoire
-- On l'ajoute d'abord en nullable, on remplit les lignes existantes,
-- puis on pose la contrainte NOT NULL.
ALTER TABLE addresses ADD COLUMN IF NOT EXISTS district VARCHAR(150);
UPDATE addresses SET district = 'Non renseigné' WHERE district IS NULL;
ALTER TABLE addresses ALTER COLUMN district SET NOT NULL;

-- Ajout de la colonne postal_code (code postal) - optionnel
ALTER TABLE addresses ADD COLUMN IF NOT EXISTS postal_code VARCHAR(20);
