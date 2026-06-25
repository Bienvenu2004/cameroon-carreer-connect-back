-- =============================================================================
-- V2__create_admin_prosper.sql
-- =============================================================================
-- Adds a SYSTEM_ADMIN account for prosper.ambassa@facsciences-uy1.cm.
--
-- The password is a placeholder bcrypt hash (re-used from the demo seed —
-- it encodes the literal "Password123!"). The owner will reset it via the
-- /forgot-password flow on first sign-in, so the actual value is irrelevant.
--
-- Idempotent:
--   - Uses ON CONFLICT (email) DO NOTHING so re-running this migration —
--     or restoring a database that already has this account — is a no-op.
--   - Wrapped in a PL/pgSQL DO block that bails out cleanly when the
--     `users` table doesn't exist yet (e.g. on a brand-new database where
--     Flyway runs before Hibernate has had a chance to create the schema).
--
-- Column mapping (taken directly from the User entity + BaseEntity):
--   id                UUID, PK
--   email             VARCHAR, UNIQUE — must be stored lowercase to match
--                     UsersServiceImpl.normalizeEmail()
--   password          VARCHAR — Spring DelegatingPasswordEncoder format
--   is_active         BOOLEAN — true so login works without admin reactivation
--   role              VARCHAR (enum as string) — 'SYSTEM_ADMIN'
--   registration_date DATE   — CURRENT_DATE
--   deleted           BOOLEAN — false
--   created_at,
--   updated_at        TIMESTAMP — NOW()
-- =============================================================================

DO $$
DECLARE
    -- Fixed UUID so the row is stable across re-runs and reproducible across
    -- environments. Distinct namespace (90...) from the demo seed's admin
    -- (10...) so the two coexist without ambiguity.
    admin_id        UUID := '90000000-0000-0000-0000-000000000001';

    -- Spring Security DelegatingPasswordEncoder bcrypt of "Password123!".
    -- Will be replaced on first password reset.
    bcrypt_password TEXT := '{bcrypt}$2b$10$1GNvREL5n7Z/07sHxbL4YOABOh0.f/uAhqDRZ3pRvemXnZpcgs2CO';
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
         WHERE table_schema = current_schema()
           AND table_name   = 'users'
    ) THEN
        RAISE NOTICE 'V2__create_admin_prosper: `users` table not found — Hibernate has not initialized the schema yet. Skipping admin creation.';
        RETURN;
    END IF;

    INSERT INTO users (
        id,
        email,
        password,
        is_active,
        role,
        registration_date,
        deleted,
        created_at,
        updated_at
    )
    VALUES (
        admin_id,
        'prosper.ambassa@facsciences-uy1.cm',
        bcrypt_password,
        true,
        'SYSTEM_ADMIN',
        CURRENT_DATE,
        false,
        NOW(),
        NOW()
    )
    ON CONFLICT (email) DO NOTHING;

    RAISE NOTICE 'V2__create_admin_prosper: SYSTEM_ADMIN ensured for prosper.ambassa@facsciences-uy1.cm. Use /forgot-password to set a real password.';
END $$;
