package com.hostdesign24.jobportal.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hostdesign24.jobportal.dto.analytics.DashboardDto;
import com.hostdesign24.jobportal.dto.common.ApiResponse;
import com.hostdesign24.jobportal.services.AnalyticsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/hjp/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ApiResponse<DashboardDto> getDashboard() {
        // Currently fetching stats for all jobs (Admin/Global view)
        // In a real scenario, we would fetch the current authenticated user's ID
        // and filter the stats accordingly.
        DashboardDto dashboard = analyticsService.getDashboardStats(null);
        return ApiResponse.success(dashboard, "Dashboard statistics retrieved successfully");
    }
}
