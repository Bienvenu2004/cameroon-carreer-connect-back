-- =============================================================================
-- V6__add_interview_details_and_hire_close_tracking.sql
-- =============================================================================
-- Two related additions to the application/hiring flow:
--
--  1. Interview scheduling. When a recruiter moves an application to INTERVIEW
--     they now provide a place, date/time, contact phone and an optional note.
--     These are persisted on the application so they survive refreshes, can be
--     shown back in the UI, and are included in the invitation email.
--
--  2. Precise job re-opening. Marking a candidate HIRED auto-closes the job.
--     `closed_by_hire` records *why* a job was closed so that moving the
--     candidate back out of HIRED only re-opens jobs that were closed by the
--     hire — never a job the recruiter closed manually for another reason.
--
-- The schema is otherwise managed by Hibernate (`ddl-auto: update`); these
-- statements are IF NOT EXISTS so re-running is a no-op even after Hibernate
-- has already added the columns on a running instance.
-- =============================================================================

ALTER TABLE job_applications
    ADD COLUMN IF NOT EXISTS interview_place     VARCHAR(255),
    ADD COLUMN IF NOT EXISTS interview_date_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS interview_phone     VARCHAR(50),
    ADD COLUMN IF NOT EXISTS interview_note      TEXT;

ALTER TABLE jobs
    ADD COLUMN IF NOT EXISTS closed_by_hire BOOLEAN NOT NULL DEFAULT FALSE;
