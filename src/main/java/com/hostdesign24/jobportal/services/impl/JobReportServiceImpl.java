package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.report.CreateJobReportDto;
import com.hostdesign24.jobportal.dto.report.JobReportDto;
import com.hostdesign24.jobportal.dto.report.ResolveJobReportDto;
import com.hostdesign24.jobportal.exception.ResourceNotFoundException;
import com.hostdesign24.jobportal.model.Job;
import com.hostdesign24.jobportal.model.JobReport;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.enums.ReportStatus;
import com.hostdesign24.jobportal.repository.JobReportRepository;
import com.hostdesign24.jobportal.repository.JobRepository;
import com.hostdesign24.jobportal.services.JobReportService;
import com.hostdesign24.jobportal.services.UserNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Job reports and their moderation.
 *
 * Anonymous reporting is deliberate. The people most likely to spot a
 * "pay a deposit to secure the position" advert are exactly the people browsing
 * before they trust the site enough to register, and requiring an account first
 * would filter out the reports that matter most.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobReportServiceImpl implements JobReportService {

    private final JobReportRepository jobReportRepository;
    private final JobRepository jobRepository;
    private final UserNotificationService userNotificationService;

    @Override
    @Transactional
    public void report(UUID jobId, CreateJobReportDto request) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        UUID reporterId = Utils.getCurrentUser().map(User::getId).orElse(null);

        // One person flagging the same listing repeatedly adds nothing to the
        // queue and would let a single user inflate the report count on a rival's
        // advert. Silently treated as success: telling them it was a duplicate
        // reveals nothing useful and invites probing.
        if (reporterId != null
                && jobReportRepository.existsByJobIdAndReporterIdAndDeletedFalse(jobId, reporterId)) {
            log.debug("Duplicate report by {} for job {} ignored", reporterId, jobId);
            return;
        }

        JobReport report = new JobReport();
        report.setJob(job);
        report.setReporterId(reporterId);
        report.setReason(request.getReason());
        report.setDetails(trimToNull(request.getDetails()));
        report.setStatus(ReportStatus.PENDING);
        jobReportRepository.save(report);

        log.info("Job {} reported for {} by {}", jobId, request.getReason(),
                reporterId == null ? "an anonymous visitor" : reporterId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<JobReportDto> list(ReportStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<JobReport> result = status == null
                ? jobReportRepository.findByDeletedFalseOrderByCreatedAtDesc(pageable)
                : jobReportRepository.findByStatusAndDeletedFalseOrderByCreatedAtAsc(status, pageable);

        List<JobReportDto> content = result.getContent().stream().map(this::toDto).toList();

        return new PageResponseDto<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast());
    }

    @Override
    @Transactional
    public void resolve(UUID reportId, ResolveJobReportDto request) {
        JobReport report = jobReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportId));

        boolean upheld = Boolean.TRUE.equals(request.getUpheld());

        report.setStatus(upheld ? ReportStatus.UPHELD : ReportStatus.DISMISSED);
        report.setResolutionNote(trimToNull(request.getNote()));
        report.setResolvedAt(LocalDateTime.now());
        report.setResolvedBy(Utils.getCurrentUser().map(User::getId).orElse(null));
        jobReportRepository.save(report);

        Job job = report.getJob();

        if (upheld && job != null && job.isActive()) {
            job.setActive(false);
            jobRepository.save(job);
            log.info("Listing {} taken down after report {} was upheld", job.getId(), reportId);
        }

        // Tell the reporter what came of it. Someone who flags a fraudulent
        // listing and hears nothing learns that reporting is pointless, and stops
        // -- which costs far more than the notification does.
        if (report.getReporterId() != null && job != null) {
            userNotificationService.jobReportResolvedNotification(
                    report.getReporterId(), job.getTitle(), upheld, job.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long pendingCount() {
        return jobReportRepository.countByStatusAndDeletedFalse(ReportStatus.PENDING);
    }

    private JobReportDto toDto(JobReport report) {
        Job job = report.getJob();
        return JobReportDto.builder()
                .id(report.getId())
                .jobId(job != null ? job.getId() : null)
                .jobTitle(job != null ? job.getTitle() : null)
                .companyName(job != null && job.getCompany() != null ? job.getCompany().getName() : null)
                .jobActive(job != null && job.isActive())
                .reason(report.getReason())
                .details(report.getDetails())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolvedAt())
                .resolutionNote(report.getResolutionNote())
                .totalReportsForJob(job == null ? 0
                        : jobReportRepository.countByJobIdAndDeletedFalse(job.getId()))
                .build();
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
