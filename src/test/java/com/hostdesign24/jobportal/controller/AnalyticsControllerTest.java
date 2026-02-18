package com.hostdesign24.jobportal.controller;

import com.hostdesign24.jobportal.dto.analytics.DashboardDto;
import com.hostdesign24.jobportal.dto.analytics.JobStatsDto;
import com.hostdesign24.jobportal.services.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
public class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @Test
    public void testGetDashboard() throws Exception {
        DashboardDto mockDashboard = DashboardDto.builder()
                .totalJobs(10)
                .totalViews(100)
                .totalApplications(5)
                .jobsStats(Collections.singletonList(
                        JobStatsDto.builder()
                                .jobTitle("Software Engineer")
                                .views(50)
                                .applicationsCount(2L)
                                .build()
                ))
                .demographics(new HashMap<>())
                .build();

        Mockito.when(analyticsService.getDashboardStats(any())).thenReturn(mockDashboard);

        mockMvc.perform(get("/api/hjp/analytics/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalJobs").value(10))
                .andExpect(jsonPath("$.data.totalViews").value(100))
                .andExpect(jsonPath("$.data.jobsStats[0].jobTitle").value("Software Engineer"));
    }
}
