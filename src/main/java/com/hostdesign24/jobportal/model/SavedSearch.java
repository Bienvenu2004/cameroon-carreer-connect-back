package com.hostdesign24.jobportal.model;

import com.hostdesign24.jobportal.model.enums.Industry;
import com.hostdesign24.jobportal.model.enums.JobSite;
import com.hostdesign24.jobportal.model.enums.JobType;
import com.hostdesign24.jobportal.model.enums.Region;
import com.hostdesign24.jobportal.model.enums.SalaryCurrency;
import com.hostdesign24.jobportal.model.enums.SavedSearchFrequency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A persisted job-search criteria saved by a job seeker. The scheduler
 * periodically evaluates each active saved search and emails the seeker
 * any new jobs that match.
 */
@Entity
@Table(name = "saved_searches")
@Getter
@Setter
public class SavedSearch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private JobSeekerProfile profile;

    @Column(nullable = false)
    private String label;

    /* ---- search criteria (all optional, AND-combined) ---- */

    private String keyword;

    @Enumerated(EnumType.STRING)
    private Region region;

    @Enumerated(EnumType.STRING)
    private Industry industry;

    @Enumerated(EnumType.STRING)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    private JobSite jobSite;

    private BigDecimal salaryMin;

    private BigDecimal salaryMax;

    @Enumerated(EnumType.STRING)
    private SalaryCurrency salaryCurrency = SalaryCurrency.XAF;

    /* ---- delivery settings ---- */

    @Column(nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SavedSearchFrequency frequency = SavedSearchFrequency.DAILY;

    /** Last time we evaluated and sent matches for this search. */
    private LocalDateTime lastSentAt;
}
