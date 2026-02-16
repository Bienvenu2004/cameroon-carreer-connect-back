package com.hostdesign24.jobportal.controller;

import com.hostdesign24.jobportal.dto.JobSeekerSaveDto;
import com.hostdesign24.jobportal.dto.JobSeekerSaveFilter;
import com.hostdesign24.jobportal.dto.common.ApiResponse;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.services.JobSeekerSaveService;
import com.hostdesign24.jobportal.services.UsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/hjp/saved-jobs")
@RequiredArgsConstructor
public class JobSeekerSaveController {

    private final UsersService usersService;
    private final JobSeekerSaveService jobSeekerSaveService;

    @PostMapping("/{jobId}")
    public ApiResponse<Object> saveJob(@PathVariable UUID jobId) {

        jobSeekerSaveService.addNew(jobId);

        return ApiResponse.success(null, "Job successfully added to save");
    }

    @GetMapping
    public ApiResponse<PageResponseDto<JobSeekerSaveDto>> getSavedJobs(@ModelAttribute JobSeekerSaveFilter filter) {

        PageResponseDto<JobSeekerSaveDto> savedJobs = jobSeekerSaveService.getSavedJobsResponse(filter);
        return ApiResponse.success(savedJobs, "saved jobs retrieved successfully");
    }
}
