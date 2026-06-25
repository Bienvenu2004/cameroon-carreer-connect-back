-- =============================================================================
-- V3__create_recommendations_table.sql
-- =============================================================================
-- Persists every AI-generated recommendation at the moment it's shown to a
-- seeker. We use this for:
--   1. User-facing "previous recommendations" history (V2 feature)
--   2. Training data for a future learning-to-rank model
--   3. Debugging — "why was job X recommended to user Y last Tuesday?"
--
-- The actual application schema is still managed by Hibernate
-- (`ddl-auto: update`) — this migration only runs if the underlying
-- `recommendations` table doesn't already exist. That guards against
-- a fresh DB where Flyway runs before Hibernate has built the schema.
--
-- Idempotent: uses CREATE TABLE IF NOT EXISTS and CREATE INDEX IF NOT EXISTS
-- throughout. Re-running this migration is a no-op.
-- =============================================================================

DO $$
BEGIN
    -- Wait until Hibernate has built the dependent tables (`users`, `jobs`)
    -- before adding our FKs to them. If they aren't there yet, skip — next
    -- startup will pick this up.
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
         WHERE table_schema = current_schema()
           AND table_name   = 'users'
    ) OR NOT EXISTS (
        SELECT 1 FROM information_schema.tables
         WHERE table_schema = current_schema()
           AND table_name   = 'jobs'
    ) THEN
        RAISE NOTICE 'V3__create_recommendations_table: parent tables not ready, skipping.';
        RETURN;
    END IF;

    -- Create the table only if it isn't already present (Hibernate's
    -- ddl-auto:update may have created it on a previous run).
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
         WHERE table_schema = current_schema()
           AND table_name   = 'recommendations'
    ) THEN
        CREATE TABLE recommendations (
            id                 UUID         PRIMARY KEY,
            user_id            UUID         NOT NULL REFERENCES users(id),
            job_id             UUID         NOT NULL REFERENCES jobs(id),

            score              DOUBLE PRECISION NOT NULL,
            explanation        TEXT         NOT NULL,
            model_version      VARCHAR(255) NOT NULL,
            feature_breakdown  JSONB,
            shown_at           TIMESTAMP    NOT NULL DEFAULT NOW(),

            -- Outcome columns — future feedback endpoints flip these.
            clicked            BOOLEAN      NOT NULL DEFAULT FALSE,
            applied            BOOLEAN      NOT NULL DEFAULT FALSE,
            dismissed          BOOLEAN      NOT NULL DEFAULT FALSE,

            -- BaseEntity audit columns
            created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
            updated_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
            created_by         UUID,
            updated_by         UUID,
            deleted            BOOLEAN      NOT NULL DEFAULT FALSE
        );

        RAISE NOTICE 'V3__create_recommendations_table: created recommendations table.';
    END IF;

    -- Indexes for the read paths we expect to add in V2:
    --   - "show me my last N recommendations"
    --   - "what did we recommend for job X" (analytics)
    CREATE INDEX IF NOT EXISTS recommendations_user_shown_idx
        ON recommendations (user_id, shown_at DESC);

    CREATE INDEX IF NOT EXISTS recommendations_job_idx
        ON recommendations (job_id);
END $$;
