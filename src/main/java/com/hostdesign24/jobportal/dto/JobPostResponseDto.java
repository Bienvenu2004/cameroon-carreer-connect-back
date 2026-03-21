package com.hostdesign24.jobportal.dto;

import com.hostdesign24.jobportal.dto.jobActivityPost.JobCompanyResponseDto;
import com.hostdesign24.jobportal.dto.jobActivityPost.JobLocationDto;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
public class JobPostResponseDto {
    private Long totalCandidates;
    private UUID jobPostId;
    private String jobTitle;
    private JobLocationDto jobLocation;
    private JobCompanyResponseDto jobCompany;
}