package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.company.FollowedCompanyDto;
import com.hostdesign24.jobportal.exception.ActionDeniedException;
import com.hostdesign24.jobportal.exception.ResourceNotFoundException;
import com.hostdesign24.jobportal.mapper.FileMapper;
import com.hostdesign24.jobportal.model.Company;
import com.hostdesign24.jobportal.model.CompanyFollow;
import com.hostdesign24.jobportal.model.Job;
import com.hostdesign24.jobportal.model.JobSeekerProfile;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.repository.CompanyFollowRepository;
import com.hostdesign24.jobportal.repository.JobCompanyRepository;
import com.hostdesign24.jobportal.repository.JobRepository;
import com.hostdesign24.jobportal.repository.JobSeekerProfileRepository;
import com.hostdesign24.jobportal.services.CompanyFollowService;
import com.hostdesign24.jobportal.services.NotificationAsyncService;
import com.hostdesign24.jobportal.services.UserNotificationService;
import com.hostdesign24.jobportal.services.UsersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyFollowServiceImpl implements CompanyFollowService {

    private final CompanyFollowRepository companyFollowRepository;
    private final JobCompanyRepository companyRepository;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final JobRepository jobRepository;
    private final UsersService usersService;
    private final UserNotificationService userNotificationService;
    private final NotificationAsyncService notificationAsyncService;
    private final FileMapper fileMapper;

    @Override
    @Transactional
    public boolean toggleFollow(UUID companyId) {
        JobSeekerProfile profile = currentProfile();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));

        // Re-use a soft-deleted row rather than inserting a second one: the
        // unique constraint on (company_id, profile_id) covers deleted rows too,
        // so a follow/unfollow/follow cycle would otherwise fail on the third step.
        CompanyFollow follow = companyFollowRepository
                .findByCompanyIdAndProfileId(companyId, profile.getId())
                .orElse(null);

        if (follow == null) {
            follow = new CompanyFollow();
            follow.setCompany(company);
            follow.setProfile(profile);
            follow.setFollowedAt(LocalDateTime.now());
            companyFollowRepository.save(follow);
            return true;
        }

        boolean nowFollowing = follow.isDeleted();
        follow.setDeleted(!nowFollowing);
        if (nowFollowing) {
            // Treat re-following as a fresh start rather than resurrecting a
            // date from months ago, which would sort oddly in the list.
            follow.setFollowedAt(LocalDateTime.now());
        }
        companyFollowRepository.save(follow);
        return nowFollowing;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<FollowedCompanyDto> myFollowed(int page, int size) {
        JobSeekerProfile profile = currentProfile();

        Page<CompanyFollow> result = companyFollowRepository
                .findByProfileIdAndDeletedFalseOrderByFollowedAtDesc(
                        profile.getId(), PageRequest.of(page, size));

        List<FollowedCompanyDto> content = result.getContent().stream()
                .map(this::toDto)
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
    public long followerCount(UUID companyId) {
        return companyFollowRepository.countByCompanyIdAndDeletedFalse(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> myFollowedIds() {
        return companyFollowRepository.findFollowedCompanyIds(currentProfile().getId());
    }

    /**
     * Fan out a new job to the company's followers.
     *
     * Notifying immediately rather than batching into the nightly digest: a job
     * seeker who asked to hear about one specific employer wants that now, and
     * at this platform's posting volume the alternative — a digest — would mean
     * a candidate learning about a vacancy up to a day later for no benefit.
     * If posting volume ever rises to the point where a follower could get
     * several mails a day, this is the place to batch.
     */
    @Override
    @Transactional
    public void notifyFollowersOfNewJob(Job job) {
        if (job == null || job.getCompany() == null) {
            return;
        }
        Company company = job.getCompany();

        List<CompanyFollow> followers;
        try {
            followers = companyFollowRepository.findFollowersToNotify(company.getId());
        } catch (RuntimeException e) {
            log.warn("Could not load followers for company {}: {}", company.getId(), e.getMessage());
            return;
        }
        if (followers.isEmpty()) {
            return;
        }

        for (CompanyFollow follow : followers) {
            JobSeekerProfile profile = follow.getProfile();
            if (profile == null || profile.getUser() == null) continue;

            // Each follower is notified independently: one bad address or one
            // failed publish must not stop the rest of the list being told.
            try {
                userNotificationService.newJobAtFollowedCompanyNotification(
                        profile.getUser().getId(), company.getName(), job.getTitle(), job.getId());

                if (follow.isEmailAlerts()) {
                    notificationAsyncService.notifyFollowedCompanyPosted(
                            profile.getUser().getEmail(),
                            displayName(profile),
                            company.getName(),
                            job.getTitle(),
                            job.getId());
                }
            } catch (RuntimeException e) {
                log.warn("Failed to notify follower {} about job {}: {}",
                        profile.getId(), job.getId(), e.getMessage());
            }
        }

        log.info("Notified {} follower(s) of new job {} at {}",
                followers.size(), job.getId(), company.getName());
    }

    private FollowedCompanyDto toDto(CompanyFollow follow) {
        Company c = follow.getCompany();
        return FollowedCompanyDto.builder()
                .companyId(c.getId())
                .name(c.getName())
                .industry(c.getIndustry())
                .region(c.getAddress() != null ? c.getAddress().getRegion() : null)
                .city(c.getAddress() != null ? c.getAddress().getCity() : null)
                .logo(fileMapper.toDto(c.getLogo()))
                .openJobs(jobRepository.countOpenAtCompany(c.getId()))
                .followedAt(follow.getFollowedAt())
                .emailAlerts(follow.isEmailAlerts())
                .build();
    }

    private JobSeekerProfile currentProfile() {
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

    private static String displayName(JobSeekerProfile p) {
        String first = p.getFirstName();
        String last = p.getLastName();
        if (first != null && last != null) return first + " " + last;
        if (first != null) return first;
        if (last != null) return last;
        return "there";
    }
}
