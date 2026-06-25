package com.hostdesign24.jobportal.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class JobSeekerSaveDto {
    private UUID jobId;
    private String jobTitle;
    private String companyName;
}
