package com.hostdesign24.jobportal.services.impl;

import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import com.hostdesign24.jobportal.repository.JobInvitationRepository;
import com.hostdesign24.jobportal.repository.ApplicationEventRepository;
import com.hostdesign24.jobportal.model.ApplicationEvent;
import com.hostdesign24.jobportal.dto.ApplicationEventDto;
import com.hostdesign24.jobportal.common.utils.Utils;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class JobSeekerApplyServiceImpl implements JobSeekerApplyService {

    private final JobSeekerApplyRepository jobSeekerApplyRepository;
    private final UsersService usersService;
    private final JobSeekerProfileService jobSeekerProfileService;
    private final JobRepository jobRepository;
    private final ApplicationEventRepository applicationEventRepository;
    private final JobInvitationRepository jobInvitationRepository;
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

        recordEvent(apply, null, ApplicationStatus.APPLIED, user.getId(), null);

        // If a recruiter invited this candidate to this job, mark the invitation
        // answered so they can see which approaches actually worked.
        markInvitationAnswered(seekerProfile.getId(), job.getId());

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

        if (previousStatus == ApplicationStatus.WITHDRAWN) {
            throw new ActionDeniedException(
                    "This candidate has withdrawn and cannot be moved back into the pipeline");
        }

        application.setStatus(status);
        application.setStatusReason(request.getStatusReason());

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

        recordEvent(application, previousStatus, status,
                Utils.getCurrentUser().map(User::getId).orElse(null),
                request.getStatusReason());

        // Notify job seeker of status change (WebSocket for all,
        // + email for INTERVIEW / HIRED / REJECTED)
        notifyJobSeekerOfStatusChange(application, status);
    }

    /**
     * The candidate withdraws.
     *
     * Terminal and candidate-owned: a recruiter cannot move an application back
     * out of WITHDRAWN, because someone who has taken another job has not changed
     * their mind just because a recruiter would prefer they had. Withdrawing an
     * application that is already withdrawn is a no-op rather than an error, since
     * the outcome the caller wanted is already true.
     */
    @Override
    @Transactional
    public void withdraw(UUID applicationId, String reason) {
        JobApplication application = jobSeekerApplyRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        User user = usersService.getCurrentUser();
        JobSeekerProfile profile = application.getProfile();
        if (user == null || profile == null || profile.getUser() == null
                || !profile.getUser().getId().equals(user.getId())) {
            throw new ActionDeniedException("You can only withdraw your own applications");
        }

        if (application.getStatus() == ApplicationStatus.WITHDRAWN) {
            return;
        }
        if (application.getStatus() == ApplicationStatus.HIRED) {
            throw new ActionDeniedException(
                    "This application ended in an offer. Speak to the employer directly rather than withdrawing here.");
        }

        ApplicationStatus previous = application.getStatus();
        application.setStatus(ApplicationStatus.WITHDRAWN);
        application.setStatusReason(reason);
        jobSeekerApplyRepository.save(application);

        recordEvent(application, previous, ApplicationStatus.WITHDRAWN, user.getId(), reason);

        // Tell the recruiter, so their shortlist reflects reality without them
        // having to chase someone who is no longer available.
        Job withdrawnFrom = application.getJob();
        if (withdrawnFrom != null && withdrawnFrom.getCreatedBy() != null) {
            userNotificationService.applicationStatusChangedNotification(
                    withdrawnFrom.getCreatedBy(),
                    withdrawnFrom.getTitle(),
                    ApplicationStatus.WITHDRAWN.name(),
                    application.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationEventDto> getTimeline(UUID applicationId) {
        JobApplication application = jobSeekerApplyRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        User user = usersService.getCurrentUser();
        if (user == null || !canSee(application, user)) {
            throw new ActionDeniedException("You do not have access to this application");
        }

        return applicationEventRepository
                .findByApplicationIdOrderByOccurredAtAsc(applicationId)
                .stream()
                .map(e -> new ApplicationEventDto(
                        e.getId(), e.getFromStatus(), e.getToStatus(), e.getNote(), e.getOccurredAt()))
                .toList();
    }

    /** The candidate it belongs to, the recruiter who posted the job, or an admin. */
    private boolean canSee(JobApplication application, User user) {
        if (user.getRole() == UserRole.SYSTEM_ADMIN) {
            return true;
        }
        JobSeekerProfile profile = application.getProfile();
        if (profile != null && profile.getUser() != null
                && profile.getUser().getId().equals(user.getId())) {
            return true;
        }
        Job job = application.getJob();
        return job != null && user.getId().equals(job.getCreatedBy());
    }

    private void markInvitationAnswered(UUID profileId, UUID jobId) {
        jobInvitationRepository
                .findByProfileIdAndDeletedFalseOrderBySentAtDesc(profileId, Pageable.unpaged())
                .stream()
                .filter(inv -> inv.getJob() != null && jobId.equals(inv.getJob().getId()))
                .filter(inv -> inv.getRespondedAt() == null)
                .findFirst()
                .ifPresent(inv -> {
                    inv.setRespondedAt(LocalDateTime.now());
                    jobInvitationRepository.save(inv);
                });
    }

    /**
     * Append one row to the application's history.
     *
     * Best-effort: failing to write the audit trail must never block the
     * transition the user actually asked for, so it is logged rather than thrown.
     * The timeline is a record of what happened, not the mechanism.
     */
    private void recordEvent(JobApplication application,
                             ApplicationStatus from,
                             ApplicationStatus to,
                             UUID actorId,
                             String note) {
        try {
            ApplicationEvent event = new ApplicationEvent();
            event.setApplication(application);
            event.setFromStatus(from);
            event.setToStatus(to);
            event.setActorId(actorId);
            event.setNote(note);
            event.setOccurredAt(LocalDateTime.now());
            applicationEventRepository.save(event);
        } catch (RuntimeException e) {
            log.warn("Failed to record application event {} -> {} for {}: {}",
                    from, to, application.getId(), e.getMessage());
        }
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
            case REJECTED -> notificationAsyncService.notifyApplicationRejected(
                    seekerEmail, seekerName, jobTitle, companyName, application.getStatusReason());
            default -> { /* APPLIED / REVIEWED / WITHDRAWN: WebSocket only */ }
        }
    }
}
