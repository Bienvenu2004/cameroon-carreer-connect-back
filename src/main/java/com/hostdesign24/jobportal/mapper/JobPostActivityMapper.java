package com.hostdesign24.jobportal.mapper;

import com.hostdesign24.jobportal.dto.jobActivityPost.JobPostActivityDto;
import com.hostdesign24.jobportal.model.JobPost;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {JobLocationMapper.class, JobCompanyMapper.class})
public interface JobPostActivityMapper {
    JobPostActivityDto toDto(JobPost jobActivity);

    @Mapping(target = "location", ignore = true)
    @Mapping(target = "company", ignore = true)
    JobPost toEntity(JobPostActivityDto dto);
}
