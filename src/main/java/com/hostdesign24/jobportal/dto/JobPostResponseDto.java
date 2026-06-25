package com.hostdesign24.jobportal.dto;

import com.hostdesign24.jobportal.dto.jobActivityPost.JobCompanyResponseDto;
import com.hostdesign24.jobportal.dto.jobActivityPost.JobLocationDto;
import com.hostdesign24.jobportal.model.enums.JobLanguage;
import com.hostdesign24.jobportal.model.enums.JobSite;
import com.hostdesign24.jobportal.model.enums.JobType;
import com.hostdesign24.jobportal.model.enums.SalaryCurrency;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Wire-level field names match the frontend JobDto contract
 * (id / title / location / company, not jobPostId / jobTitle / ...).
 * If you need the legacy names back on the wire, add @JsonProperty.
 */
@Getter
@Setter
public class JobPostResponseDto {

    private UUID id;

    private String title;

    private String description;

    private JobType type;

    private BigDecimal salary;

    private SalaryCurrency salaryCurrency;

    private JobSite site;

    /** Working language required for the role. */
    private JobLanguage requiredLanguage;

    private String benefits;

    private Integer views;

    private boolean isActive;

    private boolean isSaved;

    private LocalDate postedDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private JobLocationDto location;

    private JobCompanyResponseDto company;

    private Long totalCandidates;
}