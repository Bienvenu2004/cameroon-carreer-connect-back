package com.hostdesign24.jobportal.dto.jobActivityPost;

import com.hostdesign24.jobportal.model.enums.JobSite;
import com.hostdesign24.jobportal.model.enums.JobType;
import com.hostdesign24.jobportal.model.enums.SalaryCurrency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class JobPostActivityDto {
    private UUID id;

    private JobLocationDto jobLocation;

    private JobCompanyDto jobCompany;

    private boolean isActive = true;

    private boolean isSaved = false;

    private String descriptionOfJob;

    @Enumerated(EnumType.STRING)
    private JobType jobType;

    private BigDecimal salary;

    @Enumerated(EnumType.STRING)
    private SalaryCurrency salaryCurrency = SalaryCurrency.XAF;

    @Enumerated(EnumType.STRING)
    private JobSite jobSite;

    private String jobTitle;

    private Integer createdDaysAgo;
}
