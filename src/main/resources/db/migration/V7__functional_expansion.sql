-- =============================================================================
-- V7__functional_expansion.sql
-- =============================================================================
-- Schema for the functional review's recommendations. Grouped by the gap each
-- change closes.
--
-- As with V6, every statement is IF NOT EXISTS: Hibernate's ddl-auto may already
-- have created these on a running instance, and re-running must be a no-op.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- 1. Jobs: seniority, deadline, salary range, public-sector concours
-- -----------------------------------------------------------------------------
-- experience_level closes a loop that was already half-built: the natural
-- language search parser extracted a level from queries like "junior developer
-- in Douala" and then had nothing to match it against.
--
-- application_deadline is the bigger change. Nothing on the platform aged out
-- before, so a post from January looked identical to one made this morning.
-- On a board whose purpose is to be the trustworthy alternative to WhatsApp
-- groups, stale listings and fraudulent ones look the same from the outside.
-- -----------------------------------------------------------------------------
ALTER TABLE jobs
    ADD COLUMN IF NOT EXISTS experience_level     VARCHAR(32),
    ADD COLUMN IF NOT EXISTS minimum_diploma      VARCHAR(32),
    ADD COLUMN IF NOT EXISTS application_deadline DATE,
    ADD COLUMN IF NOT EXISTS closed_by_expiry     BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS salary_min           NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS salary_max           NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS public_sector        BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS public_sector_ref    VARCHAR(255),
    ADD COLUMN IF NOT EXISTS public_sector_body   VARCHAR(255);

-- Carry existing single-figure salaries into the range. Treated as a floor
-- rather than an exact figure, which is how a lone number in a job advert is
-- read in practice. Guarded so the statement is safe once `salary` is dropped.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'jobs' AND column_name = 'salary') THEN
        EXECUTE 'UPDATE jobs SET salary_min = salary WHERE salary_min IS NULL AND salary IS NOT NULL';
    END IF;
END $$;

-- Listing pages filter on open, non-deleted jobs and order by recency.
CREATE INDEX IF NOT EXISTS idx_jobs_deadline    ON jobs (application_deadline);
CREATE INDEX IF NOT EXISTS idx_jobs_public_open ON jobs (public_sector, is_active, deleted);


-- -----------------------------------------------------------------------------
-- 2. Education
-- -----------------------------------------------------------------------------
-- The profile had no way to record education at all. In this market the diploma
-- is typically the first screen a recruiter applies, and adverts are written as
-- "Bac+3 minimum" -- a profile that cannot express Bac+3 cannot be searched the
-- way local recruiters think.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS educations (
    id             UUID PRIMARY KEY,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL,
    created_by     UUID,
    updated_by     UUID,
    deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    level          VARCHAR(32)  NOT NULL,
    field_of_study VARCHAR(255),
    institution    VARCHAR(255),
    city           VARCHAR(255),
    country        VARCHAR(255),
    start_date     DATE,
    end_date       DATE,
    is_current     BOOLEAN      NOT NULL DEFAULT FALSE,
    description    TEXT,
    -- job_seeker_profiles is keyed on user_id, not id: the entity uses @MapsId so
    -- the profile's identifier IS its user's. work_experiences references it the
    -- same way.
    profile_id     UUID REFERENCES job_seeker_profiles (user_id)
);

CREATE INDEX IF NOT EXISTS idx_educations_profile ON educations (profile_id);
CREATE INDEX IF NOT EXISTS idx_educations_level   ON educations (level);


-- -----------------------------------------------------------------------------
-- 3. Candidate search opt-in
-- -----------------------------------------------------------------------------
-- Defaults to FALSE and stays there until the seeker says otherwise. Appearing
-- in an employer-facing search is materially different from posting an
-- application, so it is an explicit opt-in rather than something that happens
-- to people who signed up to look for work.
-- -----------------------------------------------------------------------------
ALTER TABLE job_seeker_profiles
    ADD COLUMN IF NOT EXISTS searchable       BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS searchable_since TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_profiles_searchable ON job_seeker_profiles (searchable, deleted);


-- -----------------------------------------------------------------------------
-- 4. Application transparency
-- -----------------------------------------------------------------------------
-- status_reason: the status payload carried rich detail for INTERVIEW and
-- nothing for REJECTED, so candidates learned they were rejected and never why.
--
-- application_events: an append-only history. The candidate-facing timeline
-- needs it, and so does the employer responsiveness metric -- updated_at only
-- ever remembers the most recent change.
-- -----------------------------------------------------------------------------
ALTER TABLE job_applications
    ADD COLUMN IF NOT EXISTS status_reason TEXT;

CREATE TABLE IF NOT EXISTS application_events (
    id             UUID PRIMARY KEY,
    created_at     TIMESTAMP   NOT NULL,
    updated_at     TIMESTAMP   NOT NULL,
    created_by     UUID,
    updated_by     UUID,
    deleted        BOOLEAN     NOT NULL DEFAULT FALSE,
    application_id UUID        NOT NULL REFERENCES job_applications (id),
    from_status    VARCHAR(32),
    to_status      VARCHAR(32) NOT NULL,
    actor_id       UUID,
    note           TEXT,
    occurred_at    TIMESTAMP   NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_app_events_application ON application_events (application_id, occurred_at);

-- Backfill an opening event for every existing application so timelines are not
-- blank for applications made before this table existed. Uses the application's
-- own date; nothing better is recoverable.
INSERT INTO application_events (id, created_at, updated_at, deleted, application_id, from_status, to_status, occurred_at)
SELECT gen_random_uuid(), NOW(), NOW(), FALSE, a.id, NULL, 'APPLIED',
       COALESCE(a.application_date::timestamp, a.created_at)
FROM job_applications a
WHERE NOT EXISTS (SELECT 1 FROM application_events e WHERE e.application_id = a.id);


-- -----------------------------------------------------------------------------
-- 5. Job reports (trust and safety moderation queue)
-- -----------------------------------------------------------------------------
-- The report button on the job page previously thanked the user and discarded
-- what they said. reporter_id is nullable so anonymous visitors -- exactly the
-- people browsing before they trust the site enough to register -- can flag a
-- fraudulent advert.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS job_reports (
    id              UUID PRIMARY KEY,
    created_at      TIMESTAMP   NOT NULL,
    updated_at      TIMESTAMP   NOT NULL,
    created_by      UUID,
    updated_by      UUID,
    deleted         BOOLEAN     NOT NULL DEFAULT FALSE,
    job_id          UUID        NOT NULL REFERENCES jobs (id),
    reporter_id     UUID,
    reason          VARCHAR(32) NOT NULL,
    details         TEXT,
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    resolved_by     UUID,
    resolved_at     TIMESTAMP,
    resolution_note TEXT
);

CREATE INDEX IF NOT EXISTS idx_job_reports_status ON job_reports (status, created_at);
CREATE INDEX IF NOT EXISTS idx_job_reports_job    ON job_reports (job_id);


-- -----------------------------------------------------------------------------
-- 6. Invitations to apply
-- -----------------------------------------------------------------------------
-- The other half of candidate search: finding someone is only useful if you can
-- then approach them. The unique constraint stops the same candidate being
-- invited to the same job twice.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS job_invitations (
    id           UUID PRIMARY KEY,
    created_at   TIMESTAMP NOT NULL,
    updated_at   TIMESTAMP NOT NULL,
    created_by   UUID,
    updated_by   UUID,
    deleted      BOOLEAN   NOT NULL DEFAULT FALSE,
    job_id       UUID      NOT NULL REFERENCES jobs (id),
    profile_id   UUID      NOT NULL REFERENCES job_seeker_profiles (user_id),
    invited_by   UUID,
    message      TEXT,
    sent_at      TIMESTAMP NOT NULL,
    responded_at TIMESTAMP,
    CONSTRAINT uk_invitation_job_profile UNIQUE (job_id, profile_id)
);

CREATE INDEX IF NOT EXISTS idx_invitations_profile ON job_invitations (profile_id, sent_at);
