package com.hostdesign24.jobportal.dto;

import com.hostdesign24.jobportal.model.enums.ApplicationStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // Interview details — populated once the recruiter schedules an interview.
    private String interviewPlace;
    private LocalDateTime interviewDateTime;
    private String interviewPhone;
    private String interviewNote;
}
