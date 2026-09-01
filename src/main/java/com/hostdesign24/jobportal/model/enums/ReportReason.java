package com.hostdesign24.jobportal.model.enums;

/**
 * Why someone flagged a job listing.
 *
 * The values mirror the choices offered in the trust and safety card on the job
 * page, and are ordered by how seriously the moderation queue treats them:
 * a suspected scam is the reason this feature exists at all, since
 * "pay a fee to be hired" fraud is the dominant complaint about job hunting in
 * Cameroon.
 */
public enum ReportReason {
    SCAM,
    MISLEADING,
    OFFENSIVE,
    ALREADY_FILLED,
    OTHER
}
