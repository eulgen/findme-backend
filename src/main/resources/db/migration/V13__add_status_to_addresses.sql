-- Migration Flyway V13: Ajout de la colonne status à la table addresses
ALTER TABLE addresses 
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE';
