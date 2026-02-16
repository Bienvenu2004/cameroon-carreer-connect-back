package com.hostdesign24.jobportal.dto;

import lombok.Data;

@Data
public class JobApplicationStatusDto {
    private boolean alreadyApplied;
    private boolean alreadySaved;
}
