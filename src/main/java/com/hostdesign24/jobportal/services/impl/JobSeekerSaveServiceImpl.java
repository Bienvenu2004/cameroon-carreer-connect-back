package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.dto.JobSeekerSaveDto;
import com.hostdesign24.jobportal.dto.JobSeekerSaveFilter;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.exception.ResourceNotFoundException;
import com.hostdesign24.jobportal.mapper.JobSeekerSaveMapper;
import com.hostdesign24.jobportal.model.Job;
import com.hostdesign24.jobportal.model.JobSave;
import com.hostdesign24.jobportal.model.JobSeekerProfile;
import com.hostdesign24.jobportal.repository.JobRepository;
import com.hostdesign24.jobportal.repository.JobSeekerSaveRepository;
import com.hostdesign24.jobportal.repository.specifications.JobSeekerSaveSpecification;
import com.hostdesign24.jobportal.services.JobSeekerProfileService;
import com.hostdesign24.jobportal.services.JobSeekerSaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobSeekerSaveServiceImpl implements JobSeekerSaveService {

    private final JobSeekerSaveRepository jobSeekerSaveRepository;
    private final JobSeekerProfileService jobSeekerProfileService;
    private final JobRepository jobRepository;
    private final JobSeekerSaveSpecification jobSeekerSaveSpecification;
    private final JobSeekerSaveMapper jobSeekerSaveMapper;

    @Override
    public PageResponseDto<JobSeekerSaveDto> getSavedJobsResponse(JobSeekerSaveFilter filter) {
        Specification<JobSave> spec = jobSeekerSaveSpecification.build(filter);
        Page<JobSave> page = jobSeekerSaveRepository.findAll(spec, filter.toPageable());
        List<JobSeekerSaveDto> savedJobs = page.getContent().stream().map(
                jobSeekerSaveMapper::toDto
        ).toList();

        return new PageResponseDto<>(
                savedJobs,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()

        ) ;
    }

    /**
     * Toggle the saved state of a job for the current seeker.
     *
     *   - If the seeker has no existing save row for this job → create one.
     *   - If they already have one (or more, from the historical bug where
     *     this method always created a new row) → delete them all.
     *
     * Wrapped in a transaction so a partial outcome doesn't leave the
     * row-set in an inconsistent state.
     */
    @Override
    @org.springframework.transaction.annotation.Transactional
    public void addNew(UUID jobId) {
        JobSeekerProfile seekerProfile = jobSeekerProfileService.getJobSeekerProfileEntity();
        if (seekerProfile == null) {
            throw new IllegalArgumentException("seeker cannot be null");
        }

        Job job = jobRepository.findById(jobId).orElseThrow(
                () -> new ResourceNotFoundException("job not found with id : " + jobId)
        );

        List<JobSave> existing = jobSeekerSaveRepository
                .findByJobIdAndProfileIdAndDeletedFalse(jobId, seekerProfile.getId());

        if (!existing.isEmpty()) {
            // Already saved — toggle off. deleteAll handles the rare
            // duplicate case left over from the previous buggy version
            // in one statement so the user gets a clean state.
            jobSeekerSaveRepository.deleteAll(existing);
            return;
        }

        JobSave save = new JobSave();
        save.setJob(job);
        save.setProfile(seekerProfile);
        jobSeekerSaveRepository.save(save);
    }
}
