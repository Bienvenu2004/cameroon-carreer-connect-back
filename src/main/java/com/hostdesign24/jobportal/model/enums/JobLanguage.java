package com.hostdesign24.jobportal.model.enums;

/**
 * Working language required for a job posting.
 *
 * Cameroon's two official languages plus an explicit BILINGUAL marker for
 * roles that demand both. The frontend uses this to badge job cards and to
 * filter the listing — recruiters set it during job creation.
 */
public enum JobLanguage {
    FRENCH,
    ENGLISH,
    BILINGUAL
}
