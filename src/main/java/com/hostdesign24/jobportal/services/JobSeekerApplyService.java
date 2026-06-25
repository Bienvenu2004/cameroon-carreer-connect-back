package com.hostdesign24.jobportal.services;

import com.hostdesign24.jobportal.dto.JobApplicationDto;
import com.hostdesign24.jobportal.dto.JobApplicationFilterDto;
import com.hostdesign24.jobportal.dto.JobSeekerApplyDto;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.model.enums.ApplicationStatus;

import java.util.UUID;

public interface JobSeekerApplyService {
    PageResponseDto<JobApplicationDto> getJobApplications(JobApplicationFilterDto filter);

    void addNew(JobSeekerApplyDto jobSeekerApply);

    void updateStatus(UUID applicationId, ApplicationStatus status);
}
