package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.dto.JobApplicationDto;
import com.hostdesign24.jobportal.dto.JobApplicationFilterDto;
import com.hostdesign24.jobportal.dto.JobSeekerApplyDto;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.mapper.JobApplicationMapper;
import com.hostdesign24.jobportal.model.*;
import com.hostdesign24.jobportal.repository.JobPostActivityRepository;
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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobSeekerApplyServiceImpl implements JobSeekerApplyService {

    private final JobSeekerApplyRepository jobSeekerApplyRepository;
    private final UsersService usersService;
    private final JobSeekerProfileService jobSeekerProfileService;
    private final JobPostActivityRepository jobPostActivityRepository;
    private final JobApplicationSpecification jobApplicationSpecification;
    private final JobApplicationMapper jobApplicationMapper;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;

    @Override
    public PageResponseDto<JobApplicationDto> getJobApplications(JobApplicationFilterDto filter) {
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

        JobPost job = jobPostActivityRepository.findById(jobPostActivityId).orElseThrow(
                () -> new EntityNotFoundException("Job not found with id: " + jobPostActivityId)
        );

        JobApplication apply = new JobApplication();
        apply.setProfile(seekerProfile);
        apply.setJob(job);
        jobSeekerApplyRepository.save(apply);
    }
}
