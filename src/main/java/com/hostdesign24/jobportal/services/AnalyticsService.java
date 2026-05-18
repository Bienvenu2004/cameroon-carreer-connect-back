package com.hostdesign24.jobportal.services;

import java.util.UUID;

import com.hostdesign24.jobportal.dto.analytics.DashboardDto;
import com.hostdesign24.jobportal.dto.analytics.RegionalStatsDto;

public interface AnalyticsService {
    DashboardDto getDashboardStats(UUID userId);

    /**
     * Aggregated employment-market analytics broken down by Cameroonian
     * administrative region — powers the regional trending dashboard.
     * Admin-only at the controller layer.
     */
    RegionalStatsDto getRegionalStats();
}
