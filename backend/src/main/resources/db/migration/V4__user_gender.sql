-- Genul utilizatorului: MALE, FEMALE sau OTHER.
-- Nullable doar pentru conturile create inainte de aceasta migrare.
ALTER TABLE app_user ADD COLUMN gender VARCHAR(10);
ALTER TABLE app_user ADD CONSTRAINT ck_app_user_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER'));
