package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.dto.JobApplicationDto;
import com.hostdesign24.jobportal.dto.JobApplicationFilterDto;
import com.hostdesign24.jobportal.dto.JobSeekerApplyDto;
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
import com.hostdesign24.jobportal.repository.specifications.JobApplicationSpecification;
import com.hostdesign24.jobportal.services.JobSeekerApplyService;
import com.hostdesign24.jobportal.services.JobSeekerProfileService;
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
    }

    /**
     * Update the status of an application.
     *
     * Side effect: when a candidate is marked HIRED, the position is
     * considered filled — we close the job automatically so:
     *   1. No further candidates can apply
     *   2. It falls off the public listing (which filters isActive=true)
     *   3. The recruiter doesn't have to remember a separate "close" step
     *
     * Other applications on the same job are left untouched — the recruiter
     * decides how to communicate with the remaining candidates.
     *
     * Wrapped in @Transactional so the application status update and the
     * job-close happen as a single atomic operation.
     */
    @Override
    @Transactional
    public void updateStatus(UUID applicationId, ApplicationStatus status) {
        JobApplication application = jobSeekerApplyRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
        application.setStatus(status);
        jobSeekerApplyRepository.save(application);

        if (status == ApplicationStatus.HIRED) {
            Job job = application.getJob();
            if (job != null && job.isActive()) {
                job.setActive(false);
                jobRepository.save(job);
            }
        }
    }
}
