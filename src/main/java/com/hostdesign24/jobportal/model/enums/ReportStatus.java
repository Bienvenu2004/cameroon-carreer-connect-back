package com.hostdesign24.jobportal.model.enums;

/**
 * Lifecycle of a job report in the admin moderation queue.
 *
 * PENDING reports are what the admin dashboard counts and surfaces. The two
 * terminal states record the decision so repeat reports against an already
 * dismissed listing don't re-open a settled question.
 */
public enum ReportStatus {
    PENDING,
    UPHELD,
    DISMISSED
}
