package com.hostdesign24.jobportal.controller;

import com.hostdesign24.jobportal.dto.JobSeekerProfileResponseDto;
import com.hostdesign24.jobportal.dto.JobSeekerProfileSaveDto;
import com.hostdesign24.jobportal.dto.common.ApiResponse;
import com.hostdesign24.jobportal.services.JobSeekerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/hjp/job-seeker-profile")
@RequiredArgsConstructor
public class JobSeekerProfileController {

    private final JobSeekerProfileService jobSeekerProfileService;

    @GetMapping("/me")
    public ApiResponse<JobSeekerProfileResponseDto> getMyProfile() {
        JobSeekerProfileResponseDto profile = jobSeekerProfileService.getCurrentSeekerProfileResponse();
        return ApiResponse.success(profile, "Profile retrieved successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<JobSeekerProfileResponseDto> getById(@PathVariable UUID id) {
        JobSeekerProfileResponseDto profile = jobSeekerProfileService.getProfileResponse(id);
        return ApiResponse.success(profile, "Profile retrieved successfully");
    }

    @PreAuthorize("hasRole('JOB_SEEKER')")
    @PatchMapping(consumes = "multipart/form-data")
    public ApiResponse<Object> saveProfile(
            @ModelAttribute JobSeekerProfileSaveDto profile) {
        jobSeekerProfileService.addNew(profile);
        return ApiResponse.success(null, "Profile saved successfully");
    }
}
