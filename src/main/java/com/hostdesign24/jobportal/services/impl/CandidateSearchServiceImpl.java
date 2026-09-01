package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.dto.candidate.CandidateSearchFilterDto;
import com.hostdesign24.jobportal.dto.candidate.CandidateSummaryDto;
import com.hostdesign24.jobportal.dto.candidate.InvitationDto;
import com.hostdesign24.jobportal.dto.candidate.InviteCandidateDto;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.exception.ActionDeniedException;
import com.hostdesign24.jobportal.exception.ResourceNotFoundException;
import com.hostdesign24.jobportal.mapper.FileMapper;
import com.hostdesign24.jobportal.model.Education;
import com.hostdesign24.jobportal.model.Job;
import com.hostdesign24.jobportal.model.JobInvitation;
import com.hostdesign24.jobportal.model.JobSeekerProfile;
import com.hostdesign24.jobportal.model.Skill;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.WorkExperience;
import com.hostdesign24.jobportal.model.enums.DiplomaLevel;
import com.hostdesign24.jobportal.model.enums.UserRole;
import com.hostdesign24.jobportal.repository.JobInvitationRepository;
import com.hostdesign24.jobportal.repository.JobRepository;
import com.hostdesign24.jobportal.repository.JobSeekerProfileRepository;
import com.hostdesign24.jobportal.repository.specifications.CandidateSpecification;
import com.hostdesign24.jobportal.services.CandidateSearchService;
import com.hostdesign24.jobportal.services.UserNotificationService;
import com.hostdesign24.jobportal.services.UsersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Candidate search and invitations.
 *
 * The consent rule lives in {@link CandidateSpecification} and is not
 * re-litigated here: no query built through this service can reach a profile
 * whose owner has not opted in.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateSearchServiceImpl implements CandidateSearchService {

    /** Enough to judge a candidate at a glance without reproducing their CV. */
    private static final int TOP_SKILLS = 6;

    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final JobInvitationRepository jobInvitationRepository;
    private final JobRepository jobRepository;
    private final CandidateSpecification candidateSpecification;
    private final UsersService usersService;
    private final UserNotificationService userNotificationService;
    private final FileMapper fileMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<CandidateSummaryDto> search(CandidateSearchFilterDto filter) {
        Page<JobSeekerProfile> page = jobSeekerProfileRepository.findAll(
                candidateSpecification.build(filter), filter.toPageable());

        List<CandidateSummaryDto> content = page.getContent().stream()
                .map(this::toSummary)
                .toList();

        return new PageResponseDto<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }

    @Override
    @Transactional
    public boolean invite(InviteCandidateDto request) {
        User recruiter = usersService.getCurrentUser();
        if (recruiter == null) {
            throw new ActionDeniedException("Authentication required");
        }

        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + request.getJobId()));

        // A recruiter may only invite people to their own postings. Without this,
        // any recruiter could drive traffic to any listing on the platform.
        boolean ownsJob = recruiter.getId().equals(job.getCreatedBy());
        if (!ownsJob && recruiter.getRole() != UserRole.SYSTEM_ADMIN) {
            throw new ActionDeniedException("You can only invite candidates to your own job postings");
        }
        if (!job.isActive() || job.isDeleted()) {
            throw new ActionDeniedException("This job is closed and is not accepting applications");
        }

        JobSeekerProfile profile = jobSeekerProfileRepository.findById(request.getProfileId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Candidate not found: " + request.getProfileId()));

        // Re-check consent at the point of contact, not only at search time: the
        // candidate may have opted out between the recruiter loading the results
        // and clicking invite, and that decision should take effect immediately.
        if (!profile.isSearchable() || profile.isDeleted()) {
            throw new ActionDeniedException("This candidate is no longer open to being contacted");
        }

        if (jobInvitationRepository.existsByJobIdAndProfileIdAndDeletedFalse(
                job.getId(), profile.getId())) {
            return false;
        }

        JobInvitation invitation = new JobInvitation();
        invitation.setJob(job);
        invitation.setProfile(profile);
        invitation.setInvitedBy(recruiter.getId());
        invitation.setMessage(trimToNull(request.getMessage()));
        jobInvitationRepository.save(invitation);

        if (profile.getUser() != null) {
            String company = job.getCompany() != null ? job.getCompany().getName() : "An employer";
            userNotificationService.jobInvitationNotification(
                    profile.getUser().getId(), job.getTitle(), company, job.getId());
        }

        log.info("Recruiter {} invited candidate {} to job {}",
                recruiter.getId(), profile.getId(), job.getId());
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<InvitationDto> myInvitations(int page, int size) {
        JobSeekerProfile profile = currentSeekerProfile();

        Page<JobInvitation> result = jobInvitationRepository
                .findByProfileIdAndDeletedFalseOrderBySentAtDesc(
                        profile.getId(),
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt")));

        LocalDate today = LocalDate.now();
        List<InvitationDto> content = result.getContent().stream()
                .map(inv -> {
                    Job job = inv.getJob();
                    boolean open = job != null
                            && job.isActive()
                            && !job.isDeleted()
                            && (job.getApplicationDeadline() == null
                                || !job.getApplicationDeadline().isBefore(today));
                    return InvitationDto.builder()
                            .id(inv.getId())
                            .jobId(job != null ? job.getId() : null)
                            .jobTitle(job != null ? job.getTitle() : null)
                            .companyName(job != null && job.getCompany() != null
                                    ? job.getCompany().getName() : null)
                            .message(inv.getMessage())
                            .sentAt(inv.getSentAt())
                            .respondedAt(inv.getRespondedAt())
                            .jobStillOpen(open)
                            .build();
                })
                .toList();

        return new PageResponseDto<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public long myPendingInvitationCount() {
        return jobInvitationRepository
                .countByProfileIdAndRespondedAtIsNullAndDeletedFalse(currentSeekerProfile().getId());
    }

    private JobSeekerProfile currentSeekerProfile() {
        User user = usersService.getCurrentUser();
        if (user == null) {
            throw new ActionDeniedException("Authentication required");
        }
        JobSeekerProfile profile = jobSeekerProfileRepository.findByUserId(user.getId());
        if (profile == null) {
            throw new ResourceNotFoundException("No job seeker profile for the current user");
        }
        return profile;
    }

    /**
     * Project a profile down to what a search result should show.
     *
     * The surname is reduced to an initial. A recruiter browsing results has not
     * spoken to any of these people, and a full name plus a city and an employer
     * is enough to find someone elsewhere; the full name appears once the
     * candidate applies or accepts an invitation. Contact details never appear
     * here at all -- the way to reach a candidate is an invitation they can
     * ignore, not a phone number lifted out of a list.
     */
    private CandidateSummaryDto toSummary(JobSeekerProfile p) {
        return CandidateSummaryDto.builder()
                .profileId(p.getId())
                .firstName(p.getFirstName())
                .lastNameInitial(initial(p.getLastName()))
                .profilePhoto(fileMapper.toDto(p.getProfilePhoto()))
                .region(p.getAddress() != null ? p.getAddress().getRegion() : null)
                .city(p.getAddress() != null ? p.getAddress().getCity() : null)
                .highestDiploma(highestDiploma(p))
                .fieldOfStudy(topFieldOfStudy(p))
                .currentTitle(mostRecentTitle(p))
                .totalYearsOfExperience(JobSeekerProfileServiceImpl.computeTotalYearsOfExperience(p))
                .topSkills(topSkills(p))
                .spokenLanguages(p.getSpokenLanguages())
                .build();
    }

    private static String initial(String lastName) {
        return lastName == null || lastName.isBlank()
                ? null
                : lastName.trim().substring(0, 1).toUpperCase() + ".";
    }

    private static DiplomaLevel highestDiploma(JobSeekerProfile p) {
        return bestEducation(p).map(Education::getLevel).orElse(null);
    }

    private static String topFieldOfStudy(JobSeekerProfile p) {
        return bestEducation(p).map(Education::getFieldOfStudy).orElse(null);
    }

    private static java.util.Optional<Education> bestEducation(JobSeekerProfile p) {
        if (p.getEducations() == null) return java.util.Optional.empty();
        return p.getEducations().stream()
                .filter(e -> e != null && !e.isDeleted() && e.getLevel() != null)
                .max(Comparator.comparingInt(e -> e.getLevel().getBacPlus()));
    }

    /** Current role if there is one, otherwise the one that ended most recently. */
    private static String mostRecentTitle(JobSeekerProfile p) {
        if (p.getExperiences() == null) return null;
        return p.getExperiences().stream()
                .filter(x -> x != null && !x.isDeleted())
                .max(Comparator
                        .comparing(WorkExperience::isCurrent)
                        .thenComparing(x -> x.getEndDate() == null ? LocalDate.MAX : x.getEndDate()))
                .map(WorkExperience::getTitle)
                .orElse(null);
    }

    private static List<String> topSkills(JobSeekerProfile p) {
        if (p.getSkills() == null) return List.of();
        return p.getSkills().stream()
                .filter(s -> s != null && !s.isDeleted() && s.getName() != null)
                .sorted(Comparator.comparingInt(Skill::getYearsOfExperience).reversed())
                .map(Skill::getName)
                .limit(TOP_SKILLS)
                .toList();
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
