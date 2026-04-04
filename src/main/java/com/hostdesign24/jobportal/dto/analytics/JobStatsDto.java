package com.hostdesign24.jobportal.dto.analytics;

import java.util.UUID;

import lombok.*;

@Getter
@Setter
public class JobStatsDto {
    private UUID jobId;
    private String jobTitle;
    private Integer views;
    private Long applicationsCount;
}
