package com.hostdesign24.jobportal.services;

import com.hostdesign24.jobportal.dto.JobSeekerSaveDto;
import com.hostdesign24.jobportal.dto.JobSeekerSaveFilter;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;

import java.util.UUID;

public interface JobSeekerSaveService {
    PageResponseDto<JobSeekerSaveDto> getSavedJobsResponse(JobSeekerSaveFilter filter);

    void addNew(UUID jobId);
}
