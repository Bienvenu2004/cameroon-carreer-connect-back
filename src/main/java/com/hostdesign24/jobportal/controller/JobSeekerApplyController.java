package com.hostdesign24.jobportal.controller;

import com.hostdesign24.jobportal.dto.JobApplicationDto;
import com.hostdesign24.jobportal.dto.JobApplicationFilterDto;
import com.hostdesign24.jobportal.dto.JobSeekerApplyDto;
import com.hostdesign24.jobportal.dto.common.ApiResponse;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.model.enums.ApplicationStatus;
import com.hostdesign24.jobportal.services.*;
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

    @PreAuthorize("hasRole('RECRUITER')")
    @PatchMapping("/applications/{id}/status")
    public ApiResponse<Void> updateApplicationStatus(
            @PathVariable UUID id,
            @RequestParam ApplicationStatus status) {
        jobSeekerApplyService.updateStatus(id, status);
        return ApiResponse.success(null, "Application status updated successfully");
    }
}
