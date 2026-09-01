package com.hostdesign24.jobportal.model.enums;

/**
 * Lifecycle of a job application.
 *
 * WITHDRAWN is candidate-driven and terminal: someone who has taken another job
 * can step out of the pipeline instead of sitting in every recruiter's shortlist
 * indefinitely. It is the only transition the seeker controls; every other move
 * belongs to the recruiter.
 */
public enum ApplicationStatus {
    APPLIED,
    REVIEWED,
    INTERVIEW,
    HIRED,
    REJECTED,
    WITHDRAWN;

    /** Terminal states no longer count as an open application. */
    public boolean isClosed() {
        return this == HIRED || this == REJECTED || this == WITHDRAWN;
    }
}
