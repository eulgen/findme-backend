-- =============================================================
-- V7__replace_first_last_name_with_full_name.sql
-- Remplacer first_name et last_name par full_name dans users
-- =============================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS full_name VARCHAR(100);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users' AND column_name = 'first_name'
    ) THEN
        UPDATE users 
        SET full_name = TRIM(CONCAT(first_name, ' ', last_name))
        WHERE full_name IS NULL OR full_name = '';

        ALTER TABLE users DROP COLUMN IF EXISTS first_name;
        ALTER TABLE users DROP COLUMN IF EXISTS last_name;
    END IF;
END $$;

UPDATE users SET full_name = email WHERE full_name IS NULL;
ALTER TABLE users ALTER COLUMN full_name SET NOT NULL;
