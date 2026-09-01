package com.hostdesign24.jobportal.model.enums;

/**
 * Kinds of employer activity the feed reports.
 *
 * Every one of these is derived from data the platform already records, which is
 * the point: the feed needs no authoring, so it cannot be empty on a young
 * platform, and it carries no moderation burden because no human writes it.
 *
 * Deliberately absent: an "application deadline approaching" type. A feed
 * ordered by when things happened cannot coherently also contain things that
 * have not happened yet, and the job page already carries a closing-date
 * countdown where it belongs.
 */
public enum ActivityType {

    /** An employer posted a job. */
    JOB_POSTED,

    /** An employer passed admin verification. */
    COMPANY_VERIFIED,

    /**
     * A role was filled through the platform.
     *
     * The strongest trust signal available here: it says both that the employer
     * genuinely hires and that applying through this site leads somewhere. No
     * candidate is named.
     */
    POSITION_FILLED
}
