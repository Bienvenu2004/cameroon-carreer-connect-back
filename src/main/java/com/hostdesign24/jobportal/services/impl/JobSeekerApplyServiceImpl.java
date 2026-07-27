package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.dto.JobApplicationDto;
import com.hostdesign24.jobportal.dto.JobApplicationFilterDto;
import com.hostdesign24.jobportal.dto.JobSeekerApplyDto;
import com.hostdesign24.jobportal.dto.UpdateApplicationStatusDto;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.mapper.JobApplicationMapper;
import com.hostdesign24.jobportal.exception.ActionDeniedException;
import com.hostdesign24.jobportal.exception.ResourceNotFoundException;
import com.hostdesign24.jobportal.model.*;
import com.hostdesign24.jobportal.model.enums.ApplicationStatus;
import com.hostdesign24.jobportal.model.enums.UserRole;
import com.hostdesign24.jobportal.repository.JobRepository;
import com.hostdesign24.jobportal.repository.JobSeekerApplyRepository;
import com.hostdesign24.jobportal.repository.JobSeekerProfileRepository;
import com.hostdesign24.jobportal.repository.UserRepository;
import com.hostdesign24.jobportal.repository.specifications.JobApplicationSpecification;
import com.hostdesign24.jobportal.services.JobSeekerApplyService;
import com.hostdesign24.jobportal.services.JobSeekerProfileService;
import com.hostdesign24.jobportal.services.NotificationAsyncService;
import com.hostdesign24.jobportal.services.UserNotificationService;
import com.hostdesign24.jobportal.services.UsersService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobSeekerApplyServiceImpl implements JobSeekerApplyService {

    private final JobSeekerApplyRepository jobSeekerApplyRepository;
    private final UsersService usersService;
    private final JobSeekerProfileService jobSeekerProfileService;
    private final JobRepository jobRepository;
    private final JobApplicationSpecification jobApplicationSpecification;
    private final JobApplicationMapper jobApplicationMapper;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final UserNotificationService userNotificationService;
    private final NotificationAsyncService notificationAsyncService;
    private final UserRepository userRepository;

    @Override
    public PageResponseDto<JobApplicationDto> getJobApplications(JobApplicationFilterDto filter) {
        // Job seekers automatically see only their own applications
        User user = usersService.getCurrentUser();
        if (user != null && user.getRole() == UserRole.JOB_SEEKER) {
            JobSeekerProfile profile = jobSeekerProfileRepository.findByUserId(user.getId());
            if (profile != null) {
                filter.setProfileId(profile.getId());
            }
        }

        Specification<JobApplication> spec = jobApplicationSpecification.build(filter);
        Page<JobApplication> page = jobSeekerApplyRepository.findAll(spec, filter.toPageable());
        List<JobApplicationDto> applications = page.getContent().stream()
                .map(jobApplicationMapper::toDto)
                .toList();

        return new PageResponseDto<>(
                applications,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Override
    public void addNew(JobSeekerApplyDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("JobSeekerApplyDto must not be null");
        }

        UUID jobPostActivityId = dto.getJobPostActivityId();
        if (jobPostActivityId == null) {
            throw new IllegalArgumentException("Job post activity id must not be null");
        }

        User user = usersService.getCurrentUser();
        if (user == null || user.getId() == null) {
            throw new EntityNotFoundException("Current user not found");
        }

        JobSeekerProfile seekerProfile = jobSeekerProfileRepository.findByUserId(user.getId());
        if (seekerProfile == null) {
            throw new EntityNotFoundException("Job seeker profile not found for user id: " + user.getId());
        }

        Job job = jobRepository.findById(jobPostActivityId).orElseThrow(
                () -> new EntityNotFoundException("Job not found with id: " + jobPostActivityId)
        );

        // Guard: don't accept applications on a job that's been closed
        // (e.g. because a candidate was already hired, or the recruiter
        // explicitly closed it). The frontend hides the Apply button for
        // closed jobs — this catches the race where the job is closed
        // between the seeker's page load and their click.
        if (!job.isActive() || job.isDeleted()) {
            throw new ActionDeniedException("This job is no longer accepting applications");
        }

        JobApplication apply = new JobApplication();
        apply.setProfile(seekerProfile);
        apply.setJob(job);
        apply.setCoverLetter(dto.getCoverLetter());
        jobSeekerApplyRepository.save(apply);

        // Notify recruiter: WebSocket + Email
        UUID recruiterId = job.getCreatedBy();
        if (recruiterId != null) {
            String candidateName = buildCandidateName(seekerProfile);
            String jobTitle = job.getTitle();

            userNotificationService.newJobApplicationNotification(recruiterId, candidateName, jobTitle, apply.getId());

            userRepository.findByIdAndDeletedFalse(recruiterId).ifPresent(recruiter ->
                    notificationAsyncService.notifyNewApplication(
                            recruiter.getEmail(), candidateName, jobTitle, user.getEmail()
                    )
            );
        }
    }

    private String buildCandidateName(JobSeekerProfile profile) {
        String first = profile.getFirstName();
        String last = profile.getLastName();
        if (first != null && last != null) return first + " " + last;
        if (first != null) return first;
        if (last != null) return last;
        return "A candidate";
    }

    /**
     * Update the status of an application.
     *
     * Status-driven side effects:
     *   - INTERVIEW: the recruiter-supplied place/date-time/phone/note are
     *     persisted on the application and emailed to the candidate as an
     *     interview invitation.
     *   - HIRED: the position is considered filled, so the job is closed
     *     automatically (no more applications, drops off the public listing)
     *     and flagged with {@code closedByHire}. The candidate gets a "hired"
     *     email.
     *   - Leaving HIRED (e.g. a mistaken hire is walked back): if the job was
     *     auto-closed by that hire ({@code closedByHire}), it is reopened to
     *     the public. Jobs the recruiter closed manually are left closed.
     *   - REJECTED: the candidate gets a rejection email.
     *
     * Other applications on the same job are left untouched — the recruiter
     * decides how to communicate with the remaining candidates.
     *
     * Wrapped in @Transactional so the application update and the job
     * open/close happen as a single atomic operation.
     */
    @Override
    @Transactional
    public void updateStatus(UUID applicationId, UpdateApplicationStatusDto request) {
        JobApplication application = jobSeekerApplyRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        ApplicationStatus previousStatus = application.getStatus();
        ApplicationStatus status = request.getStatus();

        application.setStatus(status);

        if (status == ApplicationStatus.INTERVIEW) {
            // Persist the interview details so they can be shown back in the UI
            // and included in the invitation email.
            application.setInterviewPlace(request.getInterviewPlace());
            application.setInterviewDateTime(request.getInterviewDateTime());
            application.setInterviewPhone(request.getInterviewPhone());
            application.setInterviewNote(request.getInterviewNote());
        }

        Job job = application.getJob();

        if (status == ApplicationStatus.HIRED) {
            // Fill the position: close the job and remember we did so, so the
            // action can be reversed if the hire is later walked back.
            if (job != null && job.isActive()) {
                job.setActive(false);
                job.setClosedByHire(true);
                jobRepository.save(job);
            }
        } else if (previousStatus == ApplicationStatus.HIRED) {
            // Moving away from HIRED — reopen the job to the public, but only
            // if it was this hire that closed it. Never reopen a job the
            // recruiter closed manually.
            if (job != null && job.isClosedByHire()) {
                job.setActive(true);
                job.setClosedByHire(false);
                jobRepository.save(job);
            }
        }

        jobSeekerApplyRepository.save(application);

        // Notify job seeker of status change (WebSocket for all,
        // + email for INTERVIEW / HIRED / REJECTED)
        notifyJobSeekerOfStatusChange(application, status);
    }

    private void notifyJobSeekerOfStatusChange(JobApplication application, ApplicationStatus status) {
        JobSeekerProfile profile = application.getProfile();
        if (profile == null || profile.getUser() == null) return;

        UUID jobSeekerId = profile.getUser().getId();
        Job job = application.getJob();
        String jobTitle = job != null ? job.getTitle() : "Unknown position";
        String companyName = job != null && job.getCompany() != null ? job.getCompany().getName() : "the company";

        // WebSocket notification for all status changes
        userNotificationService.applicationStatusChangedNotification(
                jobSeekerId, jobTitle, status.name(), application.getId()
        );

        // Email for the states the candidate cares about most
        String seekerName = buildCandidateName(profile);
        String seekerEmail = profile.getUser().getEmail();

        switch (status) {
            case INTERVIEW -> notificationAsyncService.notifyApplicationInterview(
                    seekerEmail, seekerName, jobTitle, companyName,
                    application.getInterviewPlace(),
                    application.getInterviewDateTime(),
                    application.getInterviewPhone(),
                    application.getInterviewNote()
            );
            case HIRED -> notificationAsyncService.notifyApplicationHired(seekerEmail, seekerName, jobTitle, companyName);
            case REJECTED -> notificationAsyncService.notifyApplicationRejected(seekerEmail, seekerName, jobTitle, companyName);
            default -> { /* APPLIED / REVIEWED: WebSocket only */ }
        }
    }
}
