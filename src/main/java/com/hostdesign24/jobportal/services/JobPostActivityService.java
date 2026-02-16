package com.hostdesign24.jobportal.services;

import com.hostdesign24.jobportal.dto.RecruiterJobsDto;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.jobActivityPost.JobActivityFilterDto;
import com.hostdesign24.jobportal.dto.jobActivityPost.JobPostActivityDto;
import com.hostdesign24.jobportal.dto.jobActivityPost.JobCompanyDto;
import com.hostdesign24.jobportal.dto.jobActivityPost.JobLocationDto;
import com.hostdesign24.jobportal.model.JobCompany;
import com.hostdesign24.jobportal.model.JobLocation;
import com.hostdesign24.jobportal.model.JobPost;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface JobPostActivityService {
    JobPost addNew(JobPostActivityDto dto);

    JobCompany createJobCompany(JobCompanyDto jobCompany);

    JobLocation createJobLocation(JobLocationDto jobLocation);

    PageResponseDto<RecruiterJobsDto> getRecruiterJobs(JobActivityFilterDto filter);

    JobPostActivityDto getOne(UUID id);

    PageResponseDto<JobPostActivityDto> getAll(JobActivityFilterDto filter);

    List<JobPostActivityDto> search(String job, String location, List<String> remote, List<String> type, LocalDate searchDate);
}
