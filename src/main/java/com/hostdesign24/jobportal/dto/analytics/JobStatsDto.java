package com.hostdesign24.jobportal.dto.analytics;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JobStatsDto {
    private UUID jobId;
    private String jobTitle;
    private Integer views;
    private Long applicationsCount;
}
