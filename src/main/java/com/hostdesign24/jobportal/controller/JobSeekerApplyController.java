package com.hostdesign24.jobportal.controller;

import java.util.List;
import com.hostdesign24.jobportal.dto.WithdrawApplicationDto;
import com.hostdesign24.jobportal.dto.ApplicationEventDto;
import com.hostdesign24.jobportal.dto.JobApplicationDto;
import com.hostdesign24.jobportal.dto.JobApplicationFilterDto;
import com.hostdesign24.jobportal.dto.JobSeekerApplyDto;
import com.hostdesign24.jobportal.dto.UpdateApplicationStatusDto;
import com.hostdesign24.jobportal.dto.common.ApiResponse;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.services.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/hjp/jobs")
@RequiredArgsConstructor
public class JobSeekerApplyController {

    private final JobSeekerApplyService jobSeekerApplyService;


    /**
     * Applying to a job is an action only available to JOB_SEEKERs. Recruiters
     * and admins are not candidates and have no JobSeekerProfile to anchor
     * the application to. The method-level guard returns 403 to anyone else.
     */
    @PreAuthorize("hasRole('JOB_SEEKER')")
    @PostMapping("/apply")
    public ApiResponse<Object> applyToJob(@RequestBody JobSeekerApplyDto applyDto) {
        jobSeekerApplyService.addNew(applyDto);
        return ApiResponse.success(null, "Applied to job successfully");
    }

    @GetMapping("/applications")
    public ApiResponse<PageResponseDto<JobApplicationDto>> getJobApplications(@ModelAttribute JobApplicationFilterDto filter) {

        PageResponseDto<JobApplicationDto> response =
                jobSeekerApplyService.getJobApplications(filter);

        return ApiResponse.success(response, "Job applications retrieved successfully");
    }

    /**
     * The candidate steps out of the pipeline.
     *
     * Deliberately a separate endpoint from the recruiter's status update rather
     * than another value they could send: withdrawal is the one transition the
     * seeker owns, and keeping it apart means the authorisation rule stays a
     * single line instead of a condition buried inside a shared handler.
     */
    @PreAuthorize("hasRole('JOB_SEEKER')")
    @PatchMapping("/applications/{id}/withdraw")
    public ApiResponse<Void> withdrawApplication(
            @PathVariable UUID id,
            @RequestBody(required = false) WithdrawApplicationDto request) {
        jobSeekerApplyService.withdraw(id, request == null ? null : request.getReason());
        return ApiResponse.success(null, "Application withdrawn");
    }

    /**
     * Status history for one application.
     *
     * Visible to the candidate it belongs to, the recruiter who posted the job,
     * and administrators; the service enforces that.
     */
    @GetMapping("/applications/{id}/timeline")
    public ApiResponse<List<ApplicationEventDto>> applicationTimeline(@PathVariable UUID id) {
        return ApiResponse.success(jobSeekerApplyService.getTimeline(id),
                "Application timeline retrieved successfully");
    }

    @PreAuthorize("hasRole('RECRUITER')")
    @PatchMapping("/applications/{id}/status")
    public ApiResponse<Void> updateApplicationStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateApplicationStatusDto request) {
        jobSeekerApplyService.updateStatus(id, request);
        return ApiResponse.success(null, "Application status updated successfully");
    }
}
