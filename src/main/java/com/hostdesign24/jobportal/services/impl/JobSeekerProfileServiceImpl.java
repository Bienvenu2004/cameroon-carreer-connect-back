package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.dto.JobSeekerProfileResponseDto;
import com.hostdesign24.jobportal.dto.JobSeekerProfileSaveDto;
import com.hostdesign24.jobportal.mapper.FileMapper;
import com.hostdesign24.jobportal.mapper.JobSeekerProfileMapper;
import com.hostdesign24.jobportal.mapper.SkillMapper;
import com.hostdesign24.jobportal.model.File;
import com.hostdesign24.jobportal.model.JobSeekerProfile;
import com.hostdesign24.jobportal.model.Skill;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.repository.JobSeekerProfileRepository;
import com.hostdesign24.jobportal.repository.SkillRepository;
import com.hostdesign24.jobportal.repository.UserRepository;
import com.hostdesign24.jobportal.services.FileService;
import com.hostdesign24.jobportal.services.JobSeekerProfileService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobSeekerProfileServiceImpl implements JobSeekerProfileService {

    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final UserRepository userRepository;
    private final JobSeekerProfileMapper jobSeekerProfileMapper;
    private final FileService fileService;
    private final SkillMapper skillMapper;
    private final SkillRepository skillRepository;
    private final FileMapper fileMapper;

    @Value("${app.storage.base-url}")
    private String publicUrl;

    @Override
    public JobSeekerProfileResponseDto getProfileResponse(UUID id) {
        JobSeekerProfile profile = getJobSeekerProfile(id);

        return getJobSeekerProfileResponse(profile);
    }

    private @NonNull JobSeekerProfile getJobSeekerProfile(UUID id) {
        return jobSeekerProfileRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Job Seeker Profile not found with id: " + id)
        );
    }

    @Override
    public JobSeekerProfile addNew(JobSeekerProfileSaveDto dto) {

        User currentUser = Utils.getCurrentUser().orElseThrow(
                () -> new UsernameNotFoundException("must be authenticated to save profile")
        );

        // update entity
        JobSeekerProfile jobSeekerProfile = jobSeekerProfileRepository.findByUserId(currentUser.getId());
        jobSeekerProfileMapper.updateFromDto(dto, jobSeekerProfile);

        // save
        jobSeekerProfile = jobSeekerProfileRepository.save(jobSeekerProfile);
        String relatedEntity = Utils.getClassSimpleName(jobSeekerProfile);

        if (dto.getResumeFile() != null && !dto.getResumeFile().isEmpty()) {
            File resume = fileService.uploadFile(dto.getResumeFile(), jobSeekerProfile.getId(), "JOB_SEEKER_RESUME", relatedEntity);
            jobSeekerProfile.setResume(resume);
        }
        
        if (dto.getProfilePhoto() != null && !dto.getProfilePhoto().isEmpty()) {
            File profilePicture = fileService.uploadFile(dto.getProfilePhoto(), jobSeekerProfile.getId(), "JOB_SEEKER_PROFILE", relatedEntity);
            jobSeekerProfile.setProfilePhoto(profilePicture);
        }

        jobSeekerProfile = jobSeekerProfileRepository.save(jobSeekerProfile);

        List<Skill> skills = createSkillsFromDto(dto, jobSeekerProfile);
        jobSeekerProfile.setSkills(skills);
        return jobSeekerProfileRepository.save(jobSeekerProfile);
    }

    private List<Skill> createSkillsFromDto(JobSeekerProfileSaveDto dto, JobSeekerProfile jobSeekerProfile) {
        if (dto == null) {
            return List.of();
        }

        if (dto.getSkills() == null || dto.getSkills().isEmpty()) {
            return List.of();
        }

        return dto.getSkills().stream()
                .filter(Objects::nonNull)
                .map(skillDto -> {
                    Skill skill = skillMapper.toEntity(skillDto);
                    if (skill == null) {
                        return null;
                    }
                    if (jobSeekerProfile != null) {
                        skill.setJobSeekerProfile(jobSeekerProfile);
                    }
                    return skillRepository.save(skill);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public JobSeekerProfileResponseDto getCurrentSeekerProfileResponse() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            assert authentication != null;
            JobSeekerProfile seekerProfile = getJobSeekerProfileEntity();
            return getJobSeekerProfileResponse(seekerProfile);
        } else return null;

    }

    @Override
    public JobSeekerProfile getJobSeekerProfileEntity() {
        User user = Utils.getCurrentUser().orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return jobSeekerProfileRepository.findByUserId(user.getId());
    }

    private JobSeekerProfileResponseDto getJobSeekerProfileResponse(JobSeekerProfile seekerProfile) {
        if (seekerProfile == null) {
            return null;
        }
        JobSeekerProfileResponseDto dto = jobSeekerProfileMapper.toDto(seekerProfile);
        dto.setResume(fileMapper.toDto(seekerProfile.getResume(), publicUrl));
        dto.setProfilePhoto(fileMapper.toDto(seekerProfile.getProfilePhoto(), publicUrl));

        return dto;
    }
}
