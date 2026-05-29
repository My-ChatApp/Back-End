-- Extended profile: date of birth, gender (phone already exists on app.users)

ALTER TABLE app.users
    ADD COLUMN IF NOT EXISTS date_of_birth DATE;

ALTER TABLE app.users
    ADD COLUMN IF NOT EXISTS gender VARCHAR(10);

DO $$ BEGIN
    ALTER TABLE app.users
        ADD CONSTRAINT chk_users_gender
        CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER'));
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;
