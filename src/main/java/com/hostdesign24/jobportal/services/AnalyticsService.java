package com.hostdesign24.jobportal.services;

import java.util.UUID;

import com.hostdesign24.jobportal.dto.analytics.DashboardDto;

public interface AnalyticsService {
    DashboardDto getDashboardStats(UUID userId);
}
