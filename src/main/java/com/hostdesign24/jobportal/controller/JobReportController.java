package com.hostdesign24.jobportal.controller;

import com.hostdesign24.jobportal.dto.common.ApiResponse;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.report.CreateJobReportDto;
import com.hostdesign24.jobportal.dto.report.JobReportDto;
import com.hostdesign24.jobportal.dto.report.ResolveJobReportDto;
import com.hostdesign24.jobportal.model.enums.ReportStatus;
import com.hostdesign24.jobportal.services.JobReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Trust and safety.
 *
 * Reporting is open to anonymous visitors: the people most likely to spot a
 * "pay a deposit to secure the position" advert are exactly those browsing
 * before they trust the site enough to register, and requiring an account first
 * would filter out the reports that matter most. The endpoint is rate limited
 * like everything else under /api/hjp.
 */
@RestController
@RequestMapping("/api/hjp")
@RequiredArgsConstructor
@Tag(name = "Trust and safety", description = "Reporting suspect listings and moderating them")
public class JobReportController {

    private final JobReportService jobReportService;

    @PostMapping("/jobs/{jobId}/report")
    @Operation(
            summary = "Report a job listing",
            description = "Open to anonymous visitors. A signed-in user may report a given listing once."
    )
    public ApiResponse<Void> report(@PathVariable UUID jobId,
                                    @Valid @RequestBody CreateJobReportDto request) {
        jobReportService.report(jobId, request);
        return ApiResponse.success(null, "Thank you - our team will review this listing");
    }

    @GetMapping("/admin/reports")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Moderation queue")
    public ApiResponse<PageResponseDto<JobReportDto>> list(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(jobReportService.list(status, page, size),
                "Reports retrieved successfully");
    }

    @PatchMapping("/admin/reports/{id}/resolve")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(
            summary = "Resolve a report",
            description = "Upholding it takes the listing down; either way the reporter is told what happened."
    )
    public ApiResponse<Void> resolve(@PathVariable UUID id,
                                     @Valid @RequestBody ResolveJobReportDto request) {
        jobReportService.resolve(id, request);
        return ApiResponse.success(null, "Report resolved");
    }

    @GetMapping("/admin/reports/pending-count")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Outstanding reports, for the dashboard badge")
    public ApiResponse<Long> pendingCount() {
        return ApiResponse.success(jobReportService.pendingCount(),
                "Pending report count retrieved successfully");
    }
}
