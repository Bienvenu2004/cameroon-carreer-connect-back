package com.hostdesign24.jobportal.dto.jobActivityPost;

import java.math.BigDecimal;
import java.util.UUID;

import com.hostdesign24.jobportal.model.enums.JobSite;
import com.hostdesign24.jobportal.model.enums.JobType;
import com.hostdesign24.jobportal.model.enums.SalaryCurrency;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobPostActivityDto {
    private UUID id;

    private JobLocationDto location;

    private JobCompanyDto company;

    private boolean isActive = true;

    private boolean isSaved = false;

    private String description;

    @Enumerated(EnumType.STRING)
    private JobType type;

    private BigDecimal salary;

    @Enumerated(EnumType.STRING)
    private SalaryCurrency salaryCurrency = SalaryCurrency.XAF;

    @Enumerated(EnumType.STRING)
    private JobSite site;

    private String title;

    private Integer createdDaysAgo;

    private String benefits;

    private Integer views;
}
