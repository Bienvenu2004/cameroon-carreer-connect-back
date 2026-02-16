package com.hostdesign24.jobportal.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class JobSeekerSaveDto {
    private UUID jobId;
    private String jobTitle;
    private String companyName;
}
