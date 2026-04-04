package com.hostdesign24.jobportal.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.hostdesign24.jobportal.dto.jobActivityPost.JobPostActivityUpsertDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.hostdesign24.jobportal.dto.JobPostResponseDto;
import com.hostdesign24.jobportal.dto.common.ApiResponse;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.jobActivityPost.JobActivityFilterDto;
import com.hostdesign24.jobportal.services.JobService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hjp/jobs")
public class JobController {

    private final JobService jobService;

    @GetMapping("/")
    public ApiResponse<PageResponseDto<JobPostResponseDto>> getJobs(
            @ModelAttribute JobActivityFilterDto filter) {
        return ApiResponse.success(jobService.getRecruiterJobs(filter), "Recruiter job posts retrieved successfully");
    }

    @GetMapping("/all")
    public ApiResponse<PageResponseDto<JobPostResponseDto>> getAll(
            @ModelAttribute JobActivityFilterDto filter) {
        return ApiResponse.success(jobService.getAll(filter), "Job posts retrieved successfully");
    }

    @GetMapping("/search")
    public ApiResponse<List<JobPostResponseDto>> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(jobService.search(title, location, date), "Search results retrieved successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<JobPostResponseDto> getOne(@PathVariable UUID id) {
        return ApiResponse.success(jobService.getOne(id), "Job post retrieved successfully");
    }

    @PostMapping(value = "/", consumes = "multipart/form-data")
    public ApiResponse<Void> addNew(@ModelAttribute JobPostActivityUpsertDto dto) {
        jobService.addNew(dto);
        return ApiResponse.success(null, "Job post created successfully");
    }

    @PatchMapping(value = "/{id}", consumes = "multipart/form-data")
    public ApiResponse<Void> update(@PathVariable UUID id, @ModelAttribute JobPostActivityUpsertDto dto) {
        jobService.update(id, dto);
        return ApiResponse.success(null, "Job post updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        jobService.delete(id);
        return ApiResponse.success(null, "Job post deleted successfully");
    }

    @PatchMapping("/{id}/close")
    public ApiResponse<Void> close(@PathVariable UUID id) {
        jobService.close(id);
        return ApiResponse.success(null, "Job post closed successfully");
    }
}