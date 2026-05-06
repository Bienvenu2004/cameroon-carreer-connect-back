package com.hostdesign24.jobportal.dto.analytics;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DashboardDto {
    private long totalApplications;
    private long totalJobs;
    private long totalActiveJobs;
    private long totalViews;
    private List<JobStatsDto> jobsStats;

    /** Application count by status name (APPLIED / REVIEWED / INTERVIEW / HIRED / REJECTED). */
    private Map<String, Long> applicationsByStatus;

    /** Geographic distribution of applicants ("City, Country" → count). */
    private Map<String, Long> demographics;

    /** Application volume by ISO yyyy-MM (last 6 months). */
    private Map<String, Long> applicationsByMonth;
}
