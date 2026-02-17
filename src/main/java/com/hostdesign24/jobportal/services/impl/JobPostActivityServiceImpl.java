package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.dto.RecruiterJobsDto;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.jobActivityPost.*;
import com.hostdesign24.jobportal.mapper.JobCompanyMapper;
import com.hostdesign24.jobportal.mapper.JobLocationMapper;
import com.hostdesign24.jobportal.mapper.JobPostActivityMapper;
import com.hostdesign24.jobportal.model.*;
import com.hostdesign24.jobportal.repository.JobCompanyRepository;
import com.hostdesign24.jobportal.repository.JobLocationRepository;
import com.hostdesign24.jobportal.repository.JobPostActivityRepository;
import com.hostdesign24.jobportal.repository.specifications.JobActivitySpecification;
import com.hostdesign24.jobportal.services.JobPostActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobPostActivityServiceImpl implements JobPostActivityService {

    private final JobPostActivityRepository jobPostActivityRepository;
    private final JobPostActivityMapper jobPostActivityMapper;
    private final JobCompanyMapper jobCompanyMapper;
    private final JobCompanyRepository jobCompanyRepository;
    private final JobLocationMapper jobLocationMapper;
    private final JobLocationRepository jobLocationRepository;
    private final JobActivitySpecification jobPostActivitySpecification;

    @Override
    public JobPost addNew(JobPostActivityDto dto) {
        JobPost jobPost = jobPostActivityMapper.toEntity(dto);
        JobLocation jobLocation = createJobLocation(dto.getJobLocation());
        JobCompany jobCompany = createJobCompany(dto.getJobCompany());
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
    public PageResponseDto<RecruiterJobsDto> getRecruiterJobs(JobActivityFilterDto filter) {

        Specification<JobPost> spec = jobPostActivitySpecification.build(filter);
        Page<JobPost> jobsPage = jobPostActivityRepository.findAll(spec, filter.toPageable());

        List<RecruiterJobsDto> recruiterJobsDtoList = new ArrayList<>();

        for (JobPost job : jobsPage.getContent()) {
            RecruiterJobsDto dto = new RecruiterJobsDto();
            dto.setJobPostId(job.getId());
            dto.setJobTitle(job.getTitle());
            dto.setJobCompany(jobCompanyMapper.toDto(job.getCompany()));
            dto.setJobLocation(jobLocationMapper.toDto(job.getLocation()));
            dto.setTotalCandidates(0L);// for the moment
            recruiterJobsDtoList.add(dto);
        }
        return new PageResponseDto<>(
                recruiterJobsDtoList,
                jobsPage.getNumber(),
                jobsPage.getSize(),
                jobsPage.getTotalElements(),
                jobsPage.getTotalPages(),
                jobsPage.isLast()
        );

    }

    @Override
    public JobPostActivityDto getOne(UUID id) {
        JobPost job = jobPostActivityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        return jobPostActivityMapper.toDto(job);
    }

    @Override
    public PageResponseDto<JobPostActivityDto> getAll(JobActivityFilterDto filter) {
        Specification<JobPost> spec = jobPostActivitySpecification.build(filter);
        Page<JobPost> page = jobPostActivityRepository.findAll(spec, filter.toPageable());

        List<JobPostActivityDto> jobPostActivities = page.getContent().stream()
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
    public List<JobPostActivityDto> search(String job, String location, List<String> remote, List<String> type, LocalDate searchDate) {
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
    public JobPost update(UUID id, JobPostActivityDto dto) {
        JobPost jobPost = jobPostActivityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        // Update fields
        jobPost.setJobTitle(dto.getJobTitle());
        jobPost.setDescriptionOfJob(dto.getDescriptionOfJob());
        jobPost.setJobType(dto.getJobType());
        jobPost.setSalary(dto.getSalary());
        jobPost.setSalaryCurrency(dto.getSalaryCurrency());
        jobPost.setJobSite(dto.getJobSite());
        jobPost.setBenefits(dto.getBenefits());
        
        // Update Location if provided
        if (dto.getJobLocation() != null) {
            JobLocation location = jobPost.getLocation();
            if (location == null) {
                location = new JobLocation();
            }
            // Assuming simple update for now, or use mapper/service if complex
            location.setCity(dto.getJobLocation().getCity());
            location.setCountry(dto.getJobLocation().getCountry());
            location.setState(dto.getJobLocation().getState());
            jobLocationRepository.save(location);
            jobPost.setLocation(location);
        }

        // Update Company if provided
        if (dto.getJobCompany() != null) {
            JobCompany company = jobPost.getCompany();
             if (company == null) {
                company = new JobCompany();
            }
            company.setName(dto.getJobCompany().getName());
            company.setLogo(dto.getJobCompany().getLogo());
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
