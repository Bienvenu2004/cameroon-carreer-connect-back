package com.hostdesign24.jobportal.dto;

import com.hostdesign24.jobportal.model.enums.ApplicationStatus;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class JobApplicationDto {
    private UUID profileId;
    private String candidateName;
    private LocalDate applyDate;
    private ApplicationStatus status;
}
