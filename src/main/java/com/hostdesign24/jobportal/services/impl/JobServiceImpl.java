package com.hostdesign24.jobportal.services.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.hostdesign24.jobportal.dto.jobActivityPost.*;
import com.hostdesign24.jobportal.exception.ActionDeniedException;
import com.hostdesign24.jobportal.exception.ResourceNotFoundException;
import com.hostdesign24.jobportal.mapper.FileMapper;
import com.hostdesign24.jobportal.mapper.JobMapper;
import com.hostdesign24.jobportal.mapper.JobResponseMapper;
import com.hostdesign24.jobportal.model.Company;
import com.hostdesign24.jobportal.model.Job;
import com.hostdesign24.jobportal.model.enums.CompanyStatus;
import com.hostdesign24.jobportal.repository.JobCompanyRepository;
import com.hostdesign24.jobportal.repository.JobRepository;
import com.hostdesign24.jobportal.repository.specifications.JobActivitySpecification;
import com.hostdesign24.jobportal.services.FileService;
import com.hostdesign24.jobportal.services.JobService;
import com.hostdesign24.jobportal.dto.JobPostResponseDto;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.jobActivityPost.JobActivityFilterDto;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    public static final String COMPANY_LOGO = "COMPANY_LOGO";

    private final JobRepository jobRepository;
    private final JobMapper jobMapper;
    private final JobCompanyRepository jobCompanyRepository;
    private final JobActivitySpecification jobActivitySpecification;
    private final JobResponseMapper jobResponseMapper;
    private final FileMapper fileMapper;
    private final FileService fileService;



    @Override
    @Transactional
    public Job addNew(JobPostActivityUpsertDto dto) {
        Job job = jobMapper.toEntity(dto);
        job.setCompany(resolveApprovedCompany(dto.getCompanyId()));
        return jobRepository.save(job);
    }

    @Override
    @Transactional
    public Job update(UUID id, JobPostActivityUpsertDto dto) {
        Job job = findJobOrThrow(id);
        updateFromDto(dto, job);

        return jobRepository.save(job);
    }

    private void updateFromDto(JobPostActivityUpsertDto dto, Job job) {
        if (dto.getTitle() != null){
            job.setTitle(dto.getTitle());
        }

        if (dto.getDescription() != null){
            job.setDescription(dto.getDescription());
        }

        if (dto.getType() != null){
            job.setType(dto.getType());
        }

        if (dto.getSalary() != null){
            job.setSalary(dto.getSalary());
        }

        if (dto.getSalaryCurrency() != null){
            job.setSalaryCurrency(dto.getSalaryCurrency());
        }

        if (dto.getSite() != null){
            job.setSite(dto.getSite());
        }

        if (dto.getBenefits() != null){
            job.setBenefits(dto.getBenefits());
        }

        if (dto.getLocation() != null){
            job.setLocation(dto.getLocation());
        }

        if (dto.getCompanyId() != null) {
            job.setCompany(resolveApprovedCompany(dto.getCompanyId()));
        }
    }

    /**
     * Look up a company by id and assert it's in APPROVED state. Used by
     * both {@link #addNew} and {@link #updateFromDto} so jobs can only ever
     * be created or re-assigned under a verified company.
     *
     *   - {@link ResourceNotFoundException} (HTTP 404) when the id doesn't exist
     *   - {@link ActionDeniedException}     (HTTP 403) when the company is
     *     PENDING / REJECTED / SUSPENDED — i.e. not allowed to host listings
     *
     * The frontend filters the company picker to APPROVED only, so users
     * shouldn't normally hit this guard; it's defense-in-depth for a stale
     * client / direct API call.
     */
    private Company resolveApprovedCompany(UUID companyId) {
        Company company = jobCompanyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + companyId));
        if (company.getStatus() != CompanyStatus.APPROVED) {
            throw new ActionDeniedException(
                    "Only approved companies can post jobs. Current status: " + company.getStatus()
            );
        }
        return company;
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!jobRepository.existsById(id)) {
            throw new ResourceNotFoundException("Job not found with id: " + id);
        }
        jobRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void close(UUID id) {
        Job job = findJobOrThrow(id);
        job.setActive(false);
        jobRepository.save(job);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<JobPostResponseDto> getRecruiterJobs(JobActivityFilterDto filter) {
        return pageToResponse(jobRepository.findAll(jobActivitySpecification.build(filter), filter.toPageable()));
    }

    @Override
    @Transactional
    public JobPostResponseDto getOne(UUID id) {
        Job job = findJobOrThrow(id);
        job.setViews(job.getViews() == null ? 1 : job.getViews() + 1);
        jobRepository.save(job);
        return buildResponse(job);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<JobPostResponseDto> getAll(JobActivityFilterDto filter) {
        Specification<Job> spec = jobActivitySpecification.build(filter);
        return pageToResponse(jobRepository.findAll(spec, filter.toPageable()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobPostResponseDto> search(String title, String location, LocalDate date) {
        return jobRepository
                .search(title == null ? "" : title,
                        location == null ? "" : location,
                        date)
                .stream()
                .map(this::buildResponse)
                .toList();
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private Job findJobOrThrow(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));
    }

    /**
     * Map a Job to its response DTO via MapStruct, then enrich the company logo
     * (requires the publicUrl parameter that can't be injected into a static mapper).
     */
    @NonNull
    private JobPostResponseDto buildResponse(Job job) {
        JobPostResponseDto response = jobResponseMapper.toResponse(job);
        if (job.getCompany() != null
                && job.getCompany().getLogo() != null
                && response.getCompany() != null) {
            response.getCompany().setLogo(fileMapper.toDto(job.getCompany().getLogo()));
        }
        return response;
    }

    private PageResponseDto<JobPostResponseDto> pageToResponse(Page<Job> page) {
        List<JobPostResponseDto> content = page.getContent().stream()
                .map(this::buildResponse)
                .toList();
        return new PageResponseDto<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
