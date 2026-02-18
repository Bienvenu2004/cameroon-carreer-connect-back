package com.hostdesign24.jobportal.dto;

import com.hostdesign24.jobportal.model.enums.ApplicationStatus;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class JobApplicationDto {
    private UUID profileId;
    private String candidateName;
    private LocalDate applyDate;
    private ApplicationStatus status;
}
