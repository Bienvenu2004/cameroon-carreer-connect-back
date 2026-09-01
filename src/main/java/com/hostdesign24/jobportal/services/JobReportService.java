package com.hostdesign24.jobportal.services;

import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.report.CreateJobReportDto;
import com.hostdesign24.jobportal.dto.report.JobReportDto;
import com.hostdesign24.jobportal.dto.report.ResolveJobReportDto;
import com.hostdesign24.jobportal.model.enums.ReportStatus;

import java.util.UUID;

/**
 * Trust and safety: users flag suspect listings, administrators act on them.
 *
 * Anti-scam is the differentiator this platform claims for itself, and reporting
 * is the one control that acts on it — until now the button on the job page
 * thanked the user and discarded what they said.
 */
public interface JobReportService {

    /** File a report. Open to anonymous visitors as well as signed-in users. */
    void report(UUID jobId, CreateJobReportDto request);

    /** Moderation queue, optionally narrowed to one status. */
    PageResponseDto<JobReportDto> list(ReportStatus status, int page, int size);

    /**
     * Resolve a report. Upholding it deactivates the listing; dismissing leaves
     * it live. Either way the person who reported it is told what happened.
     */
    void resolve(UUID reportId, ResolveJobReportDto request);

    /** Outstanding reports, for the admin dashboard badge. */
    long pendingCount();
}
