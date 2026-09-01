package com.hostdesign24.jobportal.services.impl;

import java.util.Comparator;
import java.time.LocalDateTime;
import com.hostdesign24.jobportal.model.enums.DiplomaLevel;
import com.hostdesign24.jobportal.model.Education;
import com.hostdesign24.jobportal.mapper.EducationMapper;
import com.hostdesign24.jobportal.dto.EducationSaveDto;
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
    private final EducationMapper educationMapper;



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

        if (dto.getVideoResume() != null && !dto.getVideoResume().isEmpty()) {
            File videoResume = fileService.uploadFile(dto.getVideoResume(), jobSeekerProfile.getId(), "JOB_SEEKER_VIDEO", relatedEntity);
            jobSeekerProfile.setVideoResume(videoResume);
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
        syncEducationsFromDto(dto, jobSeekerProfile);
        applySearchability(dto, jobSeekerProfile);

        return jobSeekerProfileRepository.save(jobSeekerProfile);
    }

    /**
     * Opt in or out of recruiter candidate search.
     *
     * A null flag means "leave it alone", so saving an unrelated part of the
     * profile never silently changes a privacy setting the seeker did not touch.
     * Appearing in an employer-facing search is materially different from posting
     * an application, and it should only ever happen because someone chose it.
     */
    private void applySearchability(JobSeekerProfileSaveDto dto, JobSeekerProfile profile) {
        if (dto == null || dto.getSearchable() == null) {
            return;
        }
        boolean wanted = dto.getSearchable();
        if (wanted == profile.isSearchable()) {
            return;
        }
        profile.setSearchable(wanted);
        profile.setSearchableSince(wanted ? LocalDateTime.now() : null);
    }

    /**
     * Replace the profile's education rows with whatever the DTO sent.
     *
     * Mirrors {@link #syncExperiencesFromDto}: mutate the managed collection in
     * place so orphanRemoval fires and stale rows are actually deleted, rather
     * than replacing the reference and leaking them.
     */
    private void syncEducationsFromDto(JobSeekerProfileSaveDto dto, JobSeekerProfile profile) {
        if (profile.getEducations() == null) {
            profile.setEducations(new ArrayList<>());
        }
        profile.getEducations().clear();

        if (dto == null || dto.getEducations() == null) return;

        LocalDate today = LocalDate.now();
        for (EducationSaveDto row : dto.getEducations()) {
            if (row == null) continue;
            validateEducation(row, today);

            Education entity = educationMapper.toEntity(row);
            if (entity == null) continue;

            // One source of truth for the ongoing-study invariant: never trust an
            // end date that arrived alongside isCurrent=true.
            if (row.isCurrent()) {
                entity.setEndDate(null);
                entity.setCurrent(true);
            }

            entity.setProfile(profile);
            profile.getEducations().add(entity);
        }
    }

    private static void validateEducation(EducationSaveDto row, LocalDate today) {
        if (row.getLevel() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Education: diploma level is required");
        }
        if (row.getInstitution() == null || row.getInstitution().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Education: institution is required");
        }
        if (row.getStartDate() != null && row.getStartDate().isAfter(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Education: start date cannot be in the future");
        }
        if (!row.isCurrent() && row.getEndDate() != null && row.getStartDate() != null
                && row.getEndDate().isBefore(row.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Education: end date cannot be before the start date");
        }
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
        dto.setResume(fileMapper.toDto(seekerProfile.getResume()));
        dto.setVideoResume(fileMapper.toDto(seekerProfile.getVideoResume()));
        dto.setProfilePhoto(fileMapper.toDto(seekerProfile.getProfilePhoto()));

        // Derived total — sum of every range, with ongoing roles running
        // up to today. Null when there are no experiences (lets the UI
        // hide the badge entirely instead of showing "0 years").
        Integer years = computeTotalYearsOfExperience(seekerProfile);
        dto.setTotalYearsOfExperience(years);

        dto.setHighestDiploma(highestDiploma(seekerProfile));
        applyCompleteness(seekerProfile, dto);

        return dto;
    }

    /** The best qualification on the profile, by Bac+N rank. */
    static DiplomaLevel highestDiploma(JobSeekerProfile profile) {
        if (profile == null || profile.getEducations() == null) {
            return null;
        }
        return profile.getEducations().stream()
                .filter(e -> e != null && !e.isDeleted() && e.getLevel() != null)
                .map(Education::getLevel)
                .max(Comparator.comparingInt(DiplomaLevel::getBacPlus))
                .orElse(null);
    }

    /**
     * Score the profile out of 100 and say what is missing.
     *
     * Weighted by what actually drives outcomes rather than by field count: a
     * recruiter searching for candidates filters on skills, diploma and location,
     * so those are worth more than a portfolio link. The hints are returned as
     * i18n keys, not sentences, because the frontend is bilingual and the backend
     * has no business deciding which language this seeker reads.
     */
    private static void applyCompleteness(JobSeekerProfile profile, JobSeekerProfileResponseDto dto) {
        record Check(String key, int weight, boolean done) {}

        boolean hasName = isSet(profile.getFirstName()) && isSet(profile.getLastName());
        boolean hasContact = isSet(profile.getPhoneNumber());
        boolean hasLocation = profile.getAddress() != null
                && (isSet(profile.getAddress().getCity()) || profile.getAddress().getRegion() != null);
        boolean hasSkills = profile.getSkills() != null && !profile.getSkills().isEmpty();
        boolean hasExperience = profile.getExperiences() != null && !profile.getExperiences().isEmpty();
        boolean hasEducation = profile.getEducations() != null && !profile.getEducations().isEmpty();
        boolean hasResume = profile.getResume() != null;
        boolean hasPhoto = profile.getProfilePhoto() != null;
        boolean hasLanguages = isSet(profile.getSpokenLanguages());

        List<Check> checks = List.of(
                new Check("profile.completeness.name", 10, hasName),
                new Check("profile.completeness.contact", 10, hasContact),
                new Check("profile.completeness.location", 10, hasLocation),
                new Check("profile.completeness.skills", 20, hasSkills),
                new Check("profile.completeness.education", 20, hasEducation),
                new Check("profile.completeness.experience", 15, hasExperience),
                new Check("profile.completeness.resume", 10, hasResume),
                new Check("profile.completeness.languages", 3, hasLanguages),
                new Check("profile.completeness.photo", 2, hasPhoto));

        int score = checks.stream().filter(Check::done).mapToInt(Check::weight).sum();
        dto.setCompleteness(score);
        dto.setCompletenessHints(checks.stream()
                .filter(c -> !c.done())
                .sorted(Comparator.comparingInt(Check::weight).reversed())
                .map(Check::key)
                .toList());
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
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
