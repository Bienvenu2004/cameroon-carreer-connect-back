package com.hostdesign24.jobportal.services;

import com.hostdesign24.jobportal.dto.RecruiterProfileResponseDto;
import com.hostdesign24.jobportal.dto.RecruiterProfileSaveDto;
import com.hostdesign24.jobportal.model.RecruiterProfile;

import java.util.Optional;
import java.util.UUID;

public interface RecruiterProfileService {
    RecruiterProfileResponseDto getOne(UUID id);

    RecruiterProfile addNew(RecruiterProfileSaveDto recruiterProfile);

    RecruiterProfileResponseDto getCurrentRecruiterProfile();
}
