package com.hostdesign24.jobportal.controller;

import com.hostdesign24.jobportal.dto.RecruiterProfileDto;
import com.hostdesign24.jobportal.dto.RecruiterProfileResponseDto;
import com.hostdesign24.jobportal.dto.RecruiterProfileSaveDto;
import com.hostdesign24.jobportal.dto.common.ApiResponse;
import com.hostdesign24.jobportal.services.RecruiterProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/hjp/recruiter-profile")
@RequiredArgsConstructor
public class RecruiterProfileController {

    private final RecruiterProfileService recruiterProfileService;

    @GetMapping("/me")
    public ApiResponse<RecruiterProfileResponseDto> getCurrentProfile() {

        RecruiterProfileResponseDto profile = recruiterProfileService.getCurrentRecruiterProfile();


        return ApiResponse.success(profile, "profile retrieved successfully");
    }


    @GetMapping("/{id}")
    public ApiResponse<RecruiterProfileResponseDto> getById(@PathVariable UUID id) {

        RecruiterProfileResponseDto response = recruiterProfileService.getOne(id);

        return ApiResponse.success(response, "recruiter profile retrieved successfully");
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ApiResponse<RecruiterProfileDto> createOrUpdate(
            @ModelAttribute RecruiterProfileSaveDto profile) {

        recruiterProfileService.addNew(profile);

        return ApiResponse.success(null, "recruiter profile updated successfully");
    }
}
