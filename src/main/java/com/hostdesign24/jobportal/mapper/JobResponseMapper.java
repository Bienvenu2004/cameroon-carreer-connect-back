package com.hostdesign24.jobportal.mapper;

import com.hostdesign24.jobportal.dto.JobPostResponseDto;
import com.hostdesign24.jobportal.model.Job;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JobResponseMapper {

    @Mapping(source = "id",       target = "jobPostId")
    @Mapping(source = "title",    target = "jobTitle")
    @Mapping(source = "location", target = "jobLocation")
    @Mapping(source = "company",  target = "jobCompany")
    @Mapping(target = "totalCandidates", ignore = true)
    JobPostResponseDto toResponse(Job job);
}

