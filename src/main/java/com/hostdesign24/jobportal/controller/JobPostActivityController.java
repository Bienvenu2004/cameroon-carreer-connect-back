package com.hostdesign24.jobportal.controller;

import java.util.UUID;

import com.hostdesign24.jobportal.dto.jobActivityPost.JobPostActivityUpsertDto;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hostdesign24.jobportal.dto.JobPostResponseDto;
import com.hostdesign24.jobportal.dto.common.ApiResponse;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.jobActivityPost.JobActivityFilterDto;
import com.hostdesign24.jobportal.services.JobPostActivityService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hjp/job-posts")
public class JobPostActivityController {

    private final JobPostActivityService jobPostActivityService;

    @GetMapping("/")
    public ApiResponse<PageResponseDto<JobPostResponseDto>> getJobs(
            @ModelAttribute JobActivityFilterDto filter
    ) {
        PageResponseDto<JobPostResponseDto> recruiterJobs = jobPostActivityService.getRecruiterJobs(filter);
        return ApiResponse.success(recruiterJobs, "Recruiter job posts retrieved successfully");
    }

    @PostMapping(value = "/", consumes = "Multipart/form-data")
    public ApiResponse<Object> addNew(@RequestBody JobPostActivityUpsertDto dto) {
        jobPostActivityService.addNew(dto);
        return ApiResponse.success(null, "Job post created successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<JobPostResponseDto> getOne(@PathVariable UUID id) {
        JobPostResponseDto jobPost = jobPostActivityService.getOne(id);
        return ApiResponse.success(jobPost, "Job post retrieved successfully");
    }

    @PutMapping(value = "/{id}", consumes = "Multipart/form-data")
    public ApiResponse<Object> update(@PathVariable UUID id, @RequestBody JobPostActivityUpsertDto dto) {
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