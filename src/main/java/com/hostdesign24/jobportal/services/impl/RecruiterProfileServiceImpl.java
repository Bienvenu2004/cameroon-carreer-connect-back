package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.dto.RecruiterProfileResponseDto;
import com.hostdesign24.jobportal.dto.RecruiterProfileSaveDto;
import com.hostdesign24.jobportal.dto.file.FileDto;
import com.hostdesign24.jobportal.exception.ResourceNotFoundException;
import com.hostdesign24.jobportal.mapper.FileMapper;
import com.hostdesign24.jobportal.mapper.RecruiterProfileMapper;
import com.hostdesign24.jobportal.model.File;
import com.hostdesign24.jobportal.model.RecruiterProfile;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.repository.RecruiterProfileRepository;
import com.hostdesign24.jobportal.repository.UserRepository;
import com.hostdesign24.jobportal.services.FileService;
import com.hostdesign24.jobportal.services.RecruiterProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecruiterProfileServiceImpl implements RecruiterProfileService {

    private final RecruiterProfileRepository recruiterProfileRepository;
    private final UserRepository userRepository;
    private final RecruiterProfileMapper recruiterProfileMapper;
    private final FileService fileService;
    private final FileMapper fileMapper;

    @Value("${app.storage.base-url}")
    private String publicUrl;

    @Override
    public RecruiterProfileResponseDto getOne(UUID id) {
        RecruiterProfile profile = recruiterProfileRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("profile not found with id: " + id)
        );

        return getRecruiterProfileResponse(profile);
    }

    @Override
    public RecruiterProfile addNew(RecruiterProfileSaveDto dto) {
        User currentUser = Utils.getCurrentUser().orElseThrow(
                () -> new UsernameNotFoundException("you most be authenticated to update your profile")
        );

        RecruiterProfile profile = recruiterProfileRepository.findByUserId(currentUser.getId());
        recruiterProfileMapper.updateFromDto(dto, profile);

        // upload
        String relatedEntity = Utils.getClassSimpleName(profile);

        if (dto.getProfilePhoto() != null && !dto.getProfilePhoto().isEmpty()) {
            File profilePicture = fileService.uploadFile(dto.getProfilePhoto(), profile.getId(), "RECRUITER_PROFILE", relatedEntity);
            profile.setProfilePhoto(profilePicture);
        }

        return recruiterProfileRepository.save(profile);
    }

    @Override
    public RecruiterProfileResponseDto getCurrentRecruiterProfile() {

        User user = Utils.getCurrentUser().orElseThrow(
                () -> new UsernameNotFoundException("You must be authenticated to get recruiter profile")
        );
        RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUserId(user.getId());
        return getRecruiterProfileResponse(recruiterProfile);

    }

    private RecruiterProfileResponseDto getRecruiterProfileResponse(RecruiterProfile recruiterProfile) {
        if (recruiterProfile  != null) {
            RecruiterProfileResponseDto response = recruiterProfileMapper.toResponse(recruiterProfile);
            FileDto fileDto = fileMapper.toDto(recruiterProfile.getProfilePhoto(), publicUrl);
            response.setProfilePhoto(fileDto);

            return response;
        }

        return new RecruiterProfileResponseDto();
    }
}
