package com.hostdesign24.jobportal.dto;

import com.hostdesign24.jobportal.dto.common.FilterDto;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class JobApplicationFilterDto extends FilterDto {
    private UUID jobId;
    private UUID profileId;
}
