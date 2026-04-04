package com.hostdesign24.jobportal.services;

import com.hostdesign24.jobportal.dto.JobPostResponseDto;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.jobActivityPost.JobActivityFilterDto;
import com.hostdesign24.jobportal.dto.jobActivityPost.JobCompanyDto;
import com.hostdesign24.jobportal.dto.jobActivityPost.JobPostActivityUpsertDto;
import com.hostdesign24.jobportal.model.Company;
import com.hostdesign24.jobportal.model.Job;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface JobService {
    Job addNew(JobPostActivityUpsertDto dto);

    PageResponseDto<JobPostResponseDto> getRecruiterJobs(JobActivityFilterDto filter);

    JobPostResponseDto getOne(UUID id);

    PageResponseDto<JobPostResponseDto> getAll(JobActivityFilterDto filter);

    List<JobPostResponseDto> search(String title, String location, LocalDate date);

    Job update(UUID id, JobPostActivityUpsertDto dto);

    void delete(UUID id);

    void close(UUID id);
}
