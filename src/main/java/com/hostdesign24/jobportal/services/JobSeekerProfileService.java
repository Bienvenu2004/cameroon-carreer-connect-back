package com.hostdesign24.jobportal.services;

import com.hostdesign24.jobportal.dto.JobSeekerProfileResponseDto;
import com.hostdesign24.jobportal.dto.JobSeekerProfileSaveDto;
import com.hostdesign24.jobportal.model.JobSeekerProfile;
import org.springframework.security.core.Authentication;

import java.util.UUID;

public interface JobSeekerProfileService {
    JobSeekerProfileResponseDto getProfileResponse(UUID id);

    JobSeekerProfile addNew(JobSeekerProfileSaveDto jobSeekerProfile);

    JobSeekerProfileResponseDto getCurrentSeekerProfileResponse();

    JobSeekerProfile getJobSeekerProfileEntity();
}
