package com.hostdesign24.jobportal.dto;

import com.hostdesign24.jobportal.dto.common.FilterDto;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class JobSeekerSaveFilter extends FilterDto {
    private UUID profileId;
    private UUID jobId;
    private LocalDate savedOn;
}
