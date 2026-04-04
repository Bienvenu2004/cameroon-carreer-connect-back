package com.hostdesign24.jobportal.dto;

import com.hostdesign24.jobportal.dto.jobActivityPost.JobCompanyResponseDto;
import com.hostdesign24.jobportal.dto.jobActivityPost.JobLocationDto;
import com.hostdesign24.jobportal.model.enums.JobSite;
import com.hostdesign24.jobportal.model.enums.JobType;
import com.hostdesign24.jobportal.model.enums.SalaryCurrency;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class JobPostResponseDto {

    private UUID jobPostId;

    private String jobTitle;

    private String description;

    private JobType type;

    private BigDecimal salary;

    private SalaryCurrency salaryCurrency;

    private JobSite site;

    private String benefits;

    private Integer views;

    private boolean isActive;

    private boolean isSaved;

    private LocalDate postedDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private JobLocationDto jobLocation;

    private JobCompanyResponseDto jobCompany;

    private Long totalCandidates;
}