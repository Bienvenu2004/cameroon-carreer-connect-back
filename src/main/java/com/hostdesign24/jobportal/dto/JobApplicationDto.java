package com.hostdesign24.jobportal.dto;

import com.hostdesign24.jobportal.model.enums.ApplicationStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class JobApplicationDto {
    private UUID id;
    private UUID profileId;
    private String candidateName;
    private LocalDate applyDate;
    private ApplicationStatus status;
    private String coverLetter;
    private UUID jobId;
    private String jobTitle;
    private String companyName;
}
