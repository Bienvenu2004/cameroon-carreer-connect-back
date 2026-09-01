package com.hostdesign24.jobportal.model;

import com.hostdesign24.jobportal.model.enums.ExperienceLevel;
import com.hostdesign24.jobportal.model.enums.JobLanguage;
import com.hostdesign24.jobportal.model.enums.JobSite;
import com.hostdesign24.jobportal.model.enums.JobType;
import com.hostdesign24.jobportal.model.enums.DiplomaLevel;
import com.hostdesign24.jobportal.model.enums.SalaryCurrency;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Table(name = "jobs")
@Entity(name = "Job")
@Getter
@Setter
public class Job extends BaseEntity {

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "company_id")
    private Company company;

    private boolean isActive = true;

    private boolean isSaved = false;

    // True when this job was auto-deactivated because a candidate was HIRED.
    // Lets us reopen the job to the public if the recruiter later moves that
    // application away from HIRED — without reopening jobs closed manually.
    private boolean closedByHire = false;

    @Embedded
    private Address location;

    private Integer views = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private JobType type;

    /**
     * Advertised pay, as a range.
     *
     * This replaced a single {@code salary} figure. Most real openings have a
     * band, and forcing recruiters to commit to one number mostly produced blank
     * salary fields — which is the worst outcome in a market where pay is rarely
     * advertised at all. Either bound may be null: min alone reads as "from X",
     * max alone as "up to X".
     *
     * The job filter has always thought in terms of min/max, so this also closes
     * a mismatch between what could be posted and what could be searched.
     */
    private BigDecimal salaryMin;

    private BigDecimal salaryMax;

    @Enumerated(EnumType.STRING)
    private SalaryCurrency salaryCurrency = SalaryCurrency.XAF;

    @Enumerated(EnumType.STRING)
    private JobSite site;

    /**
     * Working language(s) required for this role. Drives the language
     * badge on job cards and powers the language-based filter on the
     * jobs listing page. Set by the recruiter at posting time.
     */
    @Enumerated(EnumType.STRING)
    private JobLanguage requiredLanguage;

    private LocalDate postedDate = LocalDate.now();

    /**
     * Last day applications are accepted.
     *
     * Before this existed, nothing on the platform ever aged out: a post from
     * January was indistinguishable from one made this morning, and candidates
     * applied into listings that had been filled months earlier. On a board whose
     * stated purpose is to be the trustworthy alternative to WhatsApp groups,
     * stale listings and fraudulent ones look identical from the outside.
     *
     * Null means "no fixed deadline" and the job stays open until closed by hand.
     * {@code JobExpiryScheduler} deactivates jobs once this date has passed.
     */
    private LocalDate applicationDeadline;

    /** True when this job was auto-closed because its deadline passed. */
    private boolean closedByExpiry = false;

    /**
     * Seniority the role is pitched at.
     *
     * The natural-language search parser has always extracted an experience level
     * from queries like "junior developer jobs in Douala" — and then had nothing
     * to match it against, because jobs carried no such field. It does now.
     */
    @Enumerated(EnumType.STRING)
    private ExperienceLevel experienceLevel;

    /** Minimum qualification, expressed the way local adverts express it. */
    @Enumerated(EnumType.STRING)
    private DiplomaLevel minimumDiploma;

    /**
     * Public-sector competitive recruitment (a "concours").
     *
     * Modelled as a flag on Job rather than a parallel entity so these listings
     * inherit search, filtering, saving, alerts and sharing for free. Concours are
     * a large share of how Cameroonians actually look for work and are scattered
     * across ministry notice boards with no central listing; they are curated by
     * an admin rather than posted by a recruiter.
     */
    private boolean publicSector = false;

    /** Official reference of the concours, as printed on the ministry notice. */
    private String publicSectorRef;

    /** Ministry or public body running it. */
    private String publicSectorBody;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String benefits;
}