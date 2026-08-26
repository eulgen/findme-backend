-- =============================================================
-- V11__update_admin_and_support_passwords.sql
-- Met à jour les mots de passe des comptes par défaut Admin et Support
-- pour utiliser un hash BCrypt valide correspondant à "password"
-- (Hash: $2a$06$jrOD9Rj2yue1TqNHEQYh5OZsHG91W3A15eiQXhtzWIfaTtovWaKiS)
-- =============================================================

UPDATE authservice.users 
SET password_hash = '$2a$06$jrOD9Rj2yue1TqNHEQYh5OZsHG91W3A15eiQXhtzWIfaTtovWaKiS'
WHERE email IN ('admin@geolink.com', 'support@geolink.com');
