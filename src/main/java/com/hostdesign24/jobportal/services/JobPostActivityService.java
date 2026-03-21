package com.hostdesign24.jobportal.services;

import com.hostdesign24.jobportal.dto.JobPostResponseDto;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.jobActivityPost.JobActivityFilterDto;
import com.hostdesign24.jobportal.dto.jobActivityPost.JobPostActivityUpsertDto;
import com.hostdesign24.jobportal.dto.jobActivityPost.JobCompanyDto;
import com.hostdesign24.jobportal.dto.jobActivityPost.JobLocationDto;
import com.hostdesign24.jobportal.model.JobCompany;
import com.hostdesign24.jobportal.model.JobLocation;
import com.hostdesign24.jobportal.model.JobPost;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface JobPostActivityService {
    JobPost addNew(JobPostActivityUpsertDto dto);

    JobCompany createJobCompany(JobCompanyDto jobCompany);

    JobLocation createJobLocation(JobLocationDto jobLocation);

    PageResponseDto<JobPostResponseDto> getRecruiterJobs(JobActivityFilterDto filter);

    JobPostResponseDto getOne(UUID id);

    PageResponseDto<JobPostActivityUpsertDto> getAll(JobActivityFilterDto filter);

    List<JobPostActivityUpsertDto> search(String job, String location, List<String> remote, List<String> type, LocalDate searchDate);

    JobPost update(UUID id, JobPostActivityUpsertDto dto);

    void delete(UUID id);

    void close(UUID id);
}
