package com.hostdesign24.jobportal.controller;

import com.hostdesign24.jobportal.dto.JobApplicationDto;
import com.hostdesign24.jobportal.dto.JobApplicationFilterDto;
import com.hostdesign24.jobportal.dto.JobSeekerApplyDto;
import com.hostdesign24.jobportal.dto.common.ApiResponse;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.model.*;
import com.hostdesign24.jobportal.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hjp/jobs")
@RequiredArgsConstructor
public class JobSeekerApplyController {

    private final JobSeekerApplyService jobSeekerApplyService;


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
}
