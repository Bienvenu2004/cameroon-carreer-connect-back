package com.hostdesign24.jobportal.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobApplicationStatusDto {
    private boolean alreadyApplied;
    private boolean alreadySaved;
}
