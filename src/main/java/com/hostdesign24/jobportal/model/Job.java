package com.hostdesign24.jobportal.model;

import com.hostdesign24.jobportal.model.enums.JobLanguage;
import com.hostdesign24.jobportal.model.enums.JobSite;
import com.hostdesign24.jobportal.model.enums.JobType;
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

    @Embedded
    private Address location;

    private Integer views = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private JobType type;

    private BigDecimal salary;

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
    private String title;

    @Column(columnDefinition = "TEXT")
    private String benefits;
}