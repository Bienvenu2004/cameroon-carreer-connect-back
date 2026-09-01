package com.hostdesign24.jobportal.dto.report;

import com.hostdesign24.jobportal.model.enums.ReportReason;
import com.hostdesign24.jobportal.model.enums.ReportStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** A report as the moderation queue shows it. */
@Getter
@Setter
@Builder
public class JobReportDto {

    private UUID id;
    private UUID jobId;
    private String jobTitle;
    private String companyName;

    /** Whether the listing is still live, so an admin can see if it needs action. */
    private boolean jobActive;

    private ReportReason reason;
    private String details;
    private ReportStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private String resolutionNote;

    /**
     * How many times this listing has been flagged in total.
     * Three separate reports on one advert is a very different signal from one.
     */
    private long totalReportsForJob;
}
