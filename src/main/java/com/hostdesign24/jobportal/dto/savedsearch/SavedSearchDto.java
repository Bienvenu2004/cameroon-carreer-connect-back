package com.hostdesign24.jobportal.dto.savedsearch;

import com.hostdesign24.jobportal.model.enums.Industry;
import com.hostdesign24.jobportal.model.enums.JobSite;
import com.hostdesign24.jobportal.model.enums.JobType;
import com.hostdesign24.jobportal.model.enums.Region;
import com.hostdesign24.jobportal.model.enums.SalaryCurrency;
import com.hostdesign24.jobportal.model.enums.SavedSearchFrequency;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class SavedSearchDto {
    private UUID id;
    private String label;
    private String keyword;
    private Region region;
    private Industry industry;
    private JobType jobType;
    private JobSite jobSite;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private SalaryCurrency salaryCurrency;
    private boolean active = true;
    private SavedSearchFrequency frequency = SavedSearchFrequency.DAILY;
    private LocalDateTime lastSentAt;
    private LocalDateTime createdAt;
}
