-- Migration V12: Ajout du champ profile_image à la table users
ALTER TABLE authservice.users ADD COLUMN IF NOT EXISTS profile_image VARCHAR(255);
