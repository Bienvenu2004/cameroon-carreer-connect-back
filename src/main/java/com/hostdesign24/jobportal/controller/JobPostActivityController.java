package com.hostdesign24.jobportal.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hostdesign24.jobportal.dto.RecruiterJobsDto;
import com.hostdesign24.jobportal.dto.common.ApiResponse;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.jobActivityPost.JobActivityFilterDto;
import com.hostdesign24.jobportal.dto.jobActivityPost.JobPostActivityDto;
import com.hostdesign24.jobportal.services.JobPostActivityService;

import lombok.RequiredArgsConstructor;

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

    @PutMapping("/{id}")
    public ApiResponse<Object> update(@PathVariable UUID id, @RequestBody JobPostActivityDto dto) {
        jobPostActivityService.update(id, dto);
        return ApiResponse.success(null, "Job post updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Object> delete(@PathVariable UUID id) {
        jobPostActivityService.delete(id);
        return ApiResponse.success(null, "Job post deleted successfully");
    }

    @PutMapping("/{id}/close")
    public ApiResponse<Object> close(@PathVariable UUID id) {
        jobPostActivityService.close(id);
        return ApiResponse.success(null, "Job post closed successfully");
    }
}