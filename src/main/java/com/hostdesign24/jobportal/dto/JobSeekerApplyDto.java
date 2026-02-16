package com.hostdesign24.jobportal.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class JobSeekerApplyDto {
    private UUID jobPostActivityId;

    private LocalDate applyDate;

    private String coverLetter;
}
