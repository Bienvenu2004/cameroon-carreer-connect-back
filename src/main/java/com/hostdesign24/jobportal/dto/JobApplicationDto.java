package com.hostdesign24.jobportal.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;

@Data
public class JobApplicationDto {
    private UUID profileId;
    private String candidateName;
    private LocalDate applyDate;
}
