package com.hostdesign24.jobportal.dto.analytics;

import java.util.List;
import java.util.Map;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DashboardDto {
    private long totalApplications;
    private long totalJobs;
    private long totalViews;
    private List<JobStatsDto> jobsStats;
    private Map<String, Long> demographics; // e.g., Key: "City, Country", Value: Count
}
