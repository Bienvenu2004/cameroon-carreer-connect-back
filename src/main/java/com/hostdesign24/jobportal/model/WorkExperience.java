package com.hostdesign24.jobportal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

/**
 * A single past or current role on a job seeker's resume.
 *
 * Why a dedicated entity (not a free integer {@code yearsOfExperience}
 * on {@link JobSeekerProfile})?
 *
 *   1. We can compute a meaningful total ourselves (sum of date ranges),
 *      which removes the "claim 10 years to look senior" gaming.
 *   2. The AI matcher gets richer signal: "5 years backend Java at a
 *      fintech" beats a flat "5" any day.
 *   3. Recruiters can later filter on "candidates with ≥2 years in
 *      fintech" — impossible with a single number.
 *
 * Schema notes:
 *   - {@code companyName} is intentionally a free-text String, NOT a FK
 *     to {@link Company}. Past employers usually aren't on our platform
 *     (we'd be locking out anyone who hasn't worked at a JobConnect-
 *     registered firm). When they coincidentally are, the seeker can
 *     type the same name and the match is informational only.
 *   - {@code isCurrent} ↔ {@code endDate} invariant: when isCurrent is
 *     true, endDate MUST be null. Enforced by the service layer rather
 *     than a DB constraint — clearer error messages, and Postgres CHECK
 *     constraints don't survive Hibernate ddl-auto updates well.
 *   - Soft-deletable via {@link BaseEntity#deleted}. When a seeker
 *     removes an experience, we soft-delete so a future "undo" or
 *     "restore from history" feature has the data to work with.
 */
@Entity
@Table(name = "work_experiences")
@Getter
@Setter
@ToString(callSuper = true, exclude = {"profile"})
public class WorkExperience extends BaseEntity {

    /** Job title — required. e.g. "Backend Engineer", "Marketing Intern". */
    @Column(nullable = false)
    private String title;

    /** Free-text company name. Not linked to {@link Company}. */
    @Column(nullable = false)
    private String companyName;

    /** Optional city of the role. */
    private String city;

    /** Optional country, defaults to Cameroon at the UI layer. */
    private String country;

    /** When the candidate started this role. */
    @Column(nullable = false)
    private LocalDate startDate;

    /**
     * When the candidate left. Null iff {@link #isCurrent} is true.
     * For ongoing roles we compute "years" against today at read time.
     */
    private LocalDate endDate;

    /** True for the candidate's ongoing role. Service layer nulls endDate. */
    @Column(nullable = false)
    private boolean isCurrent = false;

    /** Free-form bullets / paragraph describing what the candidate did. */
    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_seeker_profile_id", nullable = false)
    private JobSeekerProfile profile;
}
