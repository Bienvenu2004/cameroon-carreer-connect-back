package com.hostdesign24.jobportal.controller;

import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.dto.analytics.DashboardDto;
import com.hostdesign24.jobportal.dto.analytics.RegionalStatsDto;
import com.hostdesign24.jobportal.dto.common.ApiResponse;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.services.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hjp/analytics")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Returns analytics scoped to the authenticated user's role.
     *  - SYSTEM_ADMIN sees platform-wide stats
     *  - RECRUITER sees stats for jobs they posted
     *  - JOB_SEEKER sees their application history
     */
    @GetMapping("/dashboard")
    public ApiResponse<DashboardDto> getDashboard() {
        User user = Utils.getCurrentUser().orElse(null);
        DashboardDto dashboard = analyticsService.getDashboardStats(user == null ? null : user.getId());
        return ApiResponse.success(dashboard, "Dashboard statistics retrieved successfully");
    }

    /**
     * Regional trending dashboard — admin-only. Returns counts of jobs and
     * applications per Cameroon region, the platform-wide language
     * distribution, and the top skills / hiring companies per region.
     */
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @GetMapping("/regional")
    public ApiResponse<RegionalStatsDto> getRegional() {
        return ApiResponse.success(
                analyticsService.getRegionalStats(),
                "Regional statistics retrieved successfully"
        );
    }
}
