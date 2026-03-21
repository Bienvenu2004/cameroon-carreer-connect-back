package com.hostdesign24.jobportal.services.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.dto.jobActivityPost.*;
import com.hostdesign24.jobportal.mapper.FileMapper;
import com.hostdesign24.jobportal.services.FileService;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.hostdesign24.jobportal.dto.JobPostResponseDto;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.mapper.JobCompanyMapper;
import com.hostdesign24.jobportal.mapper.JobLocationMapper;
import com.hostdesign24.jobportal.mapper.JobPostActivityMapper;
import com.hostdesign24.jobportal.model.JobCompany;
import com.hostdesign24.jobportal.model.JobLocation;
import com.hostdesign24.jobportal.model.JobPost;
import com.hostdesign24.jobportal.repository.JobCompanyRepository;
import com.hostdesign24.jobportal.repository.JobLocationRepository;
import com.hostdesign24.jobportal.repository.JobPostActivityRepository;
import com.hostdesign24.jobportal.repository.specifications.JobActivitySpecification;
import com.hostdesign24.jobportal.services.JobPostActivityService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JobPostActivityServiceImpl implements JobPostActivityService {

    public static final String COMPANY_LOGO = "COMPANY_LOGO";
    private final JobPostActivityRepository jobPostActivityRepository;
    private final JobPostActivityMapper jobPostActivityMapper;
    private final JobCompanyMapper jobCompanyMapper;
    private final JobCompanyRepository jobCompanyRepository;
    private final JobLocationMapper jobLocationMapper;
    private final JobLocationRepository jobLocationRepository;
    private final JobActivitySpecification jobPostActivitySpecification;
    private final FileMapper fileMapper;
    private final FileService fileService;

    @Value("${app.storage.base-url}")
    private String publicUrl;

    @Override
    public JobPost addNew(JobPostActivityUpsertDto dto) {
        JobPost jobPost = jobPostActivityMapper.toEntity(dto);
        JobLocation jobLocation = createJobLocation(dto.getLocation());
        JobCompany jobCompany = createJobCompany(dto.getCompany());
        jobPost.setLocation(jobLocation);
        jobPost.setCompany(jobCompany);
        return jobPostActivityRepository.save(jobPost);
    }

    @Override
    public JobCompany createJobCompany(JobCompanyDto jobCompany) {
        if (jobCompany == null) {
            return null;
        }

        JobCompany company = jobCompanyMapper.toEntity(jobCompany);
        company = jobCompanyRepository.save(company);

        company.setLogo(fileService.uploadFile(
                jobCompany.getLogo(),
                company.getId(),
                COMPANY_LOGO,
                Utils.getClassSimpleName(company)
        ));

    return jobCompanyRepository.save(company);

    }

    @Override
    public JobLocation createJobLocation(JobLocationDto jobLocation) {
        if (jobLocation == null) {
            return null;
        }
        JobLocation location = jobLocationMapper.toEntity(jobLocation);
        return jobLocationRepository.save(location);
    }

    @Override
    public PageResponseDto<JobPostResponseDto> getRecruiterJobs(JobActivityFilterDto filter) {

        Specification<JobPost> spec = jobPostActivitySpecification.build(filter);
        Page<JobPost> jobsPage = jobPostActivityRepository.findAll(spec, filter.toPageable());

        List<JobPostResponseDto> jobPostResponseDtoList = new ArrayList<>();

        for (JobPost job : jobsPage.getContent()) {
            JobPostResponseDto dto = getJobPostResponseDto(job);
            jobPostResponseDtoList.add(dto);
        }
        return new PageResponseDto<>(
                jobPostResponseDtoList,
                jobsPage.getNumber(),
                jobsPage.getSize(),
                jobsPage.getTotalElements(),
                jobsPage.getTotalPages(),
                jobsPage.isLast()
        );

    }

    private @NonNull JobPostResponseDto getJobPostResponseDto(JobPost job) {
        JobPostResponseDto dto = new JobPostResponseDto();
        dto.setJobPostId(job.getId());
        dto.setJobTitle(job.getTitle());
        dto.setJobCompany(getJobCompanyResponse(job.getCompany()));
        dto.setJobLocation(jobLocationMapper.toDto(job.getLocation()));
        dto.setTotalCandidates(0L);// for the moment
        return dto;
    }

    private JobCompanyResponseDto getJobCompanyResponse(JobCompany company) {
        JobCompanyResponseDto dto = new JobCompanyResponseDto();
        dto.setName(company.getName());
        if (company.getLogo() != null) {
            dto.setLogo(fileMapper.toDto(company.getLogo(), publicUrl));
        }

        return dto;
    }

    @Override
    public JobPostResponseDto getOne(UUID id) {
        JobPost job = jobPostActivityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        // Increment views
        job.setViews(job.getViews() == null ? 1 : job.getViews() + 1);
        jobPostActivityRepository.save(job);

        return getJobPostResponseDto(job);
    }

    @Override
    public PageResponseDto<JobPostActivityUpsertDto> getAll(JobActivityFilterDto filter) {
        Specification<JobPost> spec = jobPostActivitySpecification.build(filter);
        Page<JobPost> page = jobPostActivityRepository.findAll(spec, filter.toPageable());

        List<JobPostActivityUpsertDto> jobPostActivities = page.getContent().stream()
                .map(jobPostActivityMapper::toDto)
                .toList();

        return new PageResponseDto<>(
                jobPostActivities,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Override
    public List<JobPostActivityUpsertDto> search(String job, String location, List<String> remote, List<String> type, LocalDate searchDate) {
        List<JobPost> posts = jobPostActivityRepository.search(
                job == null ? "" : job,
                location == null ? "" : location,
                remote,
                type,
                searchDate
        );
        return posts.stream().map(jobPostActivityMapper::toDto).toList();
    }

    @Override
    public JobPost update(UUID id, JobPostActivityUpsertDto dto) {
        JobPost jobPost = jobPostActivityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        // Update fields
        jobPost.setTitle(dto.getTitle());
        jobPost.setDescription(dto.getDescription());
        jobPost.setType(dto.getType());
        jobPost.setSalary(dto.getSalary());
        jobPost.setSalaryCurrency(dto.getSalaryCurrency());
        jobPost.setSite(dto.getSite());
        jobPost.setBenefits(dto.getBenefits());
        
        // Update Location if provided
        if (dto.getLocation() != null) {
            JobLocation location = jobPost.getLocation();
            if (location == null) {
                location = new JobLocation();
            }
            // Assuming simple update for now, or use mapper/service if complex
            location.setCity(dto.getLocation().getCity());
            location.setCountry(dto.getLocation().getCountry());
            location.setState(dto.getLocation().getState());
            jobLocationRepository.save(location);
            jobPost.setLocation(location);
        }

        if (dto.getFilesToRemove() != null && !dto.getFilesToRemove().isEmpty()) {
            dto.getFilesToRemove().forEach(fileService::deleteFile);
        }

        // Update Company if provided
        if (dto.getCompany() != null) {
            JobCompany company = jobPost.getCompany();
             if (company == null) {
                company = new JobCompany();
            }
            company.setName(dto.getCompany().getName());

             if (company.getLogo() != null) {
                 company.setLogo(fileService.uploadFile(
                         dto.getCompany().getLogo(),
                         company.getId(),
                         COMPANY_LOGO,
                         Utils.getClassSimpleName(company)
                 ));
             }

            jobCompanyRepository.save(company);
            jobPost.setCompany(company);
        }

        return jobPostActivityRepository.save(jobPost);
    }

    @Override
    public void delete(UUID id) {
        if (!jobPostActivityRepository.existsById(id)) {
             throw new RuntimeException("Job not found");
        }
        jobPostActivityRepository.deleteById(id);
    }

    @Override
    public void close(UUID id) {
        JobPost jobPost = jobPostActivityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        jobPost.setActive(false);
        jobPostActivityRepository.save(jobPost);
    }
}
