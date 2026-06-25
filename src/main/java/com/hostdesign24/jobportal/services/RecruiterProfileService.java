package com.hostdesign24.jobportal.services;

import com.hostdesign24.jobportal.dto.RecruiterProfileResponseDto;
import com.hostdesign24.jobportal.dto.RecruiterProfileUpsertDto;
import com.hostdesign24.jobportal.model.RecruiterProfile;

import java.util.UUID;

public interface RecruiterProfileService {
    RecruiterProfileResponseDto getOne(UUID id);

    RecruiterProfile addNew(RecruiterProfileUpsertDto recruiterProfile);

    RecruiterProfileResponseDto getCurrentRecruiterProfile();
}
