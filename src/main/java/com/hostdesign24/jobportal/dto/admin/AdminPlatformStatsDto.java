package com.hostdesign24.jobportal.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * Platform-wide statistics surfaced on the admin dashboard.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPlatformStatsDto {
    private long totalUsers;
    private long totalJobSeekers;
    private long totalRecruiters;
    private long totalAdmins;
    private long activeUsers;
    private long suspendedUsers;
    private long totalCompanies;
    private long pendingCompanies;
    private long approvedCompanies;
    private long rejectedCompanies;
    private long totalJobs;
    private long activeJobs;
    private long totalApplications;

    /** New user signups by ISO week (last 12 weeks). */
    private Map<String, Long> signupsByWeek;

    /** Jobs posted by month (last 6 months). */
    private Map<String, Long> jobsByMonth;

    /** Applications by status. */
    private Map<String, Long> applicationsByStatus;

    /** Jobs by Cameroon region. */
    private Map<String, Long> jobsByRegion;
}
