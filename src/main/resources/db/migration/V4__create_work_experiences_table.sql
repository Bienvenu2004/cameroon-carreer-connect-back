-- =============================================================================
-- V4__create_work_experiences_table.sql
-- =============================================================================
-- Adds the work_experiences table that backs JobSeekerProfile.experiences.
-- Each row is one position on a candidate's CV (title, employer, dates,
-- description). Feeds the AI matcher's "years of experience" signal and
-- powers the recruiter-facing profile view.
--
-- The application schema is otherwise managed by Hibernate
-- (`ddl-auto: update`) — this migration only runs on a fresh DB where
-- `job_seeker_profiles` is already present. The CREATE TABLE/INDEX
-- statements are IF NOT EXISTS so re-running is a no-op even after
-- Hibernate has already built the table.
-- =============================================================================

DO $$
BEGIN
    -- Wait until Hibernate has built the parent table before adding the FK.
    -- Re-running before that is harmless — Flyway will just skip this block.
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
         WHERE table_schema = current_schema()
           AND table_name   = 'job_seeker_profiles'
    ) THEN
        RAISE NOTICE 'job_seeker_profiles not yet built — skipping V4 (Hibernate will rebuild on next startup).';
        RETURN;
    END IF;

    CREATE TABLE IF NOT EXISTS work_experiences (
        id                       UUID PRIMARY KEY,
        title                    VARCHAR(255) NOT NULL,
        company_name             VARCHAR(255) NOT NULL,
        city                     VARCHAR(255),
        country                  VARCHAR(255),
        start_date               DATE NOT NULL,
        end_date                 DATE,
        is_current               BOOLEAN NOT NULL DEFAULT FALSE,
        description              TEXT,
        job_seeker_profile_id    UUID NOT NULL REFERENCES job_seeker_profiles(id) ON DELETE CASCADE,
        created_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
        created_by               UUID,
        updated_by               UUID,
        deleted                  BOOLEAN      NOT NULL DEFAULT FALSE
    );

    -- Fast retrieval of all experiences for a single profile in their
    -- preferred chronological-reverse order. Used by the read endpoint
    -- and by LLMContextBuilder when it computes totalYearsOfExperience.
    CREATE INDEX IF NOT EXISTS work_experiences_profile_start_idx
        ON work_experiences (job_seeker_profile_id, start_date DESC)
        WHERE deleted = FALSE;
END
$$;
