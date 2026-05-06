package com.hostdesign24.jobportal.model.enums;

/**
 * Verification lifecycle of a Company on the platform.
 * Companies must be APPROVED by an admin before recruiters can post jobs.
 */
public enum CompanyStatus {
    PENDING,
    APPROVED,
    REJECTED,
    SUSPENDED
}
