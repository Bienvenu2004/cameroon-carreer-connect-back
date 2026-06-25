package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.dto.JobSeekerProfileResponseDto;
import com.hostdesign24.jobportal.dto.JobSeekerProfileSaveDto;
import com.hostdesign24.jobportal.dto.WorkExperienceSaveDto;
import com.hostdesign24.jobportal.mapper.FileMapper;
import com.hostdesign24.jobportal.mapper.JobSeekerProfileMapper;
import com.hostdesign24.jobportal.mapper.SkillMapper;
import com.hostdesign24.jobportal.mapper.WorkExperienceMapper;
import com.hostdesign24.jobportal.model.File;
import com.hostdesign24.jobportal.model.JobSeekerProfile;
import com.hostdesign24.jobportal.model.Skill;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.WorkExperience;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final WorkExperienceMapper workExperienceMapper;

    @Value("${app.storage.base-url}")
    private String publicUrl;

    /**
     * Read-only transaction so the mapper can traverse LAZY associations
     * (skills, resume, profilePhoto) inside an open Hibernate session.
     */
    @Override
    @Transactional(readOnly = true)
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

        if (dto.getResume() != null && !dto.getResume().isEmpty()) {
            File resume = fileService.uploadFile(dto.getResume(), jobSeekerProfile.getId(), "JOB_SEEKER_RESUME", relatedEntity);
            jobSeekerProfile.setResume(resume);
        }
        
        if (dto.getProfilePhoto() != null && !dto.getProfilePhoto().isEmpty()) {
            File profilePicture = fileService.uploadFile(dto.getProfilePhoto(), jobSeekerProfile.getId(), "JOB_SEEKER_PROFILE", relatedEntity);
            jobSeekerProfile.setProfilePhoto(profilePicture);
        }

        jobSeekerProfile = jobSeekerProfileRepository.save(jobSeekerProfile);

        List<Skill> skills = createSkillsFromDto(dto, jobSeekerProfile);
        jobSeekerProfile.setSkills(skills);

        // Sync experiences. We mutate the existing managed collection
        // in-place — clear() + addAll() — so Hibernate's orphanRemoval
        // triggers and stale rows are dropped. Replacing the reference
        // would skip the collection-listener and leak orphans.
        syncExperiencesFromDto(dto, jobSeekerProfile);

        return jobSeekerProfileRepository.save(jobSeekerProfile);
    }

    /**
     * Replace the profile's experiences list with whatever the DTO sent.
     * Validates each row, enforces the {@code isCurrent ↔ endDate=null}
     * invariant, and rejects obviously bogus date ranges.
     *
     * Validation rules (server-side last line of defense — UI also enforces):
     *   - title and companyName required
     *   - startDate required, not in the future
     *   - if NOT current: endDate required, endDate >= startDate
     *   - if current: endDate is forced to null regardless of what arrived
     */
    private void syncExperiencesFromDto(JobSeekerProfileSaveDto dto, JobSeekerProfile profile) {
        // Make sure the managed collection exists (new profile case)
        if (profile.getExperiences() == null) {
            profile.setExperiences(new ArrayList<>());
        }
        profile.getExperiences().clear();

        if (dto == null || dto.getExperiences() == null) return;

        LocalDate today = LocalDate.now();
        for (WorkExperienceSaveDto row : dto.getExperiences()) {
            if (row == null) continue;
            validateExperience(row, today);

            WorkExperience entity = workExperienceMapper.toEntity(row);
            if (entity == null) continue;

            // isCurrent rules — never trust the endDate the client sent
            // when isCurrent=true. One source of truth.
            if (row.isCurrent()) {
                entity.setEndDate(null);
                entity.setCurrent(true);
            }

            entity.setProfile(profile);
            profile.getExperiences().add(entity);
        }
    }

    private static void validateExperience(WorkExperienceSaveDto row, LocalDate today) {
        if (row.getTitle() == null || row.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Work experience: title is required");
        }
        if (row.getCompanyName() == null || row.getCompanyName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Work experience: company name is required");
        }
        if (row.getStartDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Work experience: start date is required");
        }
        if (row.getStartDate().isAfter(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Work experience: start date cannot be in the future");
        }
        if (!row.isCurrent()) {
            if (row.getEndDate() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Work experience: end date required unless this is the current role");
            }
            if (row.getEndDate().isBefore(row.getStartDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Work experience: end date must be on or after start date");
            }
        }
    }

    /**
     * IMPORTANT: this method MUST return a mutable List.
     *
     * The returned list is assigned to {@code jobSeekerProfile.setSkills(...)},
     * and Hibernate's merge cascade later calls {@code clear()} on that same
     * collection to re-synchronize the persistent state. {@link List#of()}
     * and {@code Stream#toList()} return unmodifiable lists — using them
     * here triggers {@link UnsupportedOperationException} inside
     * {@code CollectionType.replaceElements} on save. Always return a fresh
     * {@link ArrayList}.
     */
    private List<Skill> createSkillsFromDto(JobSeekerProfileSaveDto dto, JobSeekerProfile jobSeekerProfile) {
        if (dto == null || dto.getSkills() == null || dto.getSkills().isEmpty()) {
            return new ArrayList<>();
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
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
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

        // Derived total — sum of every range, with ongoing roles running
        // up to today. Null when there are no experiences (lets the UI
        // hide the badge entirely instead of showing "0 years").
        Integer years = computeTotalYearsOfExperience(seekerProfile);
        dto.setTotalYearsOfExperience(years);

        return dto;
    }

    /**
     * Sum of all experience-row date spans, expressed in whole years.
     * Ongoing roles use today as their effective end date. Overlapping
     * roles are NOT merged — a freelancer who freelanced while employed
     * legitimately accumulated experience in both, so we sum naively.
     * Caller decides how to render; null when no rows exist.
     */
    static Integer computeTotalYearsOfExperience(JobSeekerProfile profile) {
        if (profile == null || profile.getExperiences() == null
                || profile.getExperiences().isEmpty()) {
            return null;
        }
        LocalDate today = LocalDate.now();
        int totalDays = 0;
        for (WorkExperience xp : profile.getExperiences()) {
            if (xp == null || xp.isDeleted() || xp.getStartDate() == null) continue;
            LocalDate end = xp.isCurrent() || xp.getEndDate() == null
                    ? today
                    : xp.getEndDate();
            if (end.isBefore(xp.getStartDate())) continue;
            // Period.between → years/months/days; convert via approximate days
            // for the sum (we'll divide at the end). 30.44 avg days per month
            // is good enough for a "X years" UI badge.
            Period p = Period.between(xp.getStartDate(), end);
            totalDays += p.getYears() * 365 + p.getMonths() * 30 + p.getDays();
        }
        return totalDays / 365;
    }
}
