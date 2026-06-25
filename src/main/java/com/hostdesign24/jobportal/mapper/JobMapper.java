package com.hostdesign24.jobportal.mapper;

import com.hostdesign24.jobportal.dto.jobActivityPost.JobPostActivityUpsertDto;
import com.hostdesign24.jobportal.model.Job;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JobMapper {
    JobPostActivityUpsertDto toDto(Job jobActivity);

    @Mapping(target = "company", ignore = true)
    Job toEntity(JobPostActivityUpsertDto dto);
}
