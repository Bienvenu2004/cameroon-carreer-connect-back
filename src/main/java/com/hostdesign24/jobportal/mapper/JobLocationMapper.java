package com.hostdesign24.jobportal.mapper;

import com.hostdesign24.jobportal.dto.jobActivityPost.JobLocationDto;
import com.hostdesign24.jobportal.model.JobLocation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface JobLocationMapper {
    JobLocationDto toDto(JobLocation jobActivity);
    JobLocation toEntity(JobLocationDto dto);
}
