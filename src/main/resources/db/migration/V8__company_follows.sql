-- =============================================================================
-- V8__company_follows.sql
-- =============================================================================
-- Job seekers following employers, so they hear when one starts hiring.
--
-- The first step towards a company activity feed, but useful on its own: "tell
-- me when this employer posts" is the standing request a job seeker most wants
-- to make, and until now the only way to answer it was to check the listings by
-- hand.
--
-- profile_id references job_seeker_profiles(user_id): that table is keyed on
-- user_id because the entity uses @MapsId, so the profile's identifier IS its
-- user's. Same as educations and job_invitations in V7.
--
-- IF NOT EXISTS throughout, since Hibernate's ddl-auto may have created this
-- already on a running instance.
-- =============================================================================

CREATE TABLE IF NOT EXISTS company_follows (
    id           UUID PRIMARY KEY,
    created_at   TIMESTAMP NOT NULL,
    updated_at   TIMESTAMP NOT NULL,
    created_by   UUID,
    updated_by   UUID,
    deleted      BOOLEAN   NOT NULL DEFAULT FALSE,
    company_id   UUID      NOT NULL REFERENCES companies (id),
    profile_id   UUID      NOT NULL REFERENCES job_seeker_profiles (user_id),
    email_alerts BOOLEAN   NOT NULL DEFAULT TRUE,
    followed_at  TIMESTAMP NOT NULL,
    CONSTRAINT uk_company_follow UNIQUE (company_id, profile_id)
);

-- Two read paths: "who follows this company" when a job is posted, and "who do
-- I follow" on the seeker's own list.
CREATE INDEX IF NOT EXISTS idx_company_follows_company ON company_follows (company_id, deleted);
CREATE INDEX IF NOT EXISTS idx_company_follows_profile ON company_follows (profile_id, deleted, followed_at DESC);
