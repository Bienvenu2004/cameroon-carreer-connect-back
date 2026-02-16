package com.hostdesign24.jobportal.controller;

import com.hostdesign24.jobportal.dto.RecruiterJobsDto;
import com.hostdesign24.jobportal.dto.common.ApiResponse;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.jobActivityPost.JobActivityFilterDto;
import com.hostdesign24.jobportal.dto.jobActivityPost.JobPostActivityDto;
import com.hostdesign24.jobportal.model.*;
import com.hostdesign24.jobportal.services.JobPostActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hjp/job-post")
public class JobPostActivityController {

    private final JobPostActivityService jobPostActivityService;

    @GetMapping("/")
    public ApiResponse<Object> getJobs(
            @ModelAttribute JobActivityFilterDto filter
    ) {
        PageResponseDto<RecruiterJobsDto> recruiterJobs = jobPostActivityService.getRecruiterJobs(filter);
        return ApiResponse.success(recruiterJobs, "Recruiter job posts retrieved successfully");
    }

    @PostMapping("/")
    public ApiResponse<Object> addNew(@RequestBody JobPostActivityDto dto) {
        jobPostActivityService.addNew(dto);
        return ApiResponse.success(null, "Job post created successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<JobPostActivityDto> getOne(@PathVariable UUID id) {
        JobPostActivityDto jobPost = jobPostActivityService.getOne(id);
        return ApiResponse.success(jobPost, "Job post retrieved successfully");
    }
}