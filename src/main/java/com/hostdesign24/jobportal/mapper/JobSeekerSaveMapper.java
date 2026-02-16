package com.hostdesign24.jobportal.mapper;

import com.hostdesign24.jobportal.dto.JobSeekerSaveDto;
import com.hostdesign24.jobportal.model.JobSeekerSave;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JobSeekerSaveMapper {
    @Mapping(source = "job.title", target = "jobTitle")
    @Mapping(source = "job.id", target = "jobId")
    @Mapping(source = "job.company.name", target = "companyName")
    JobSeekerSaveDto toDto(JobSeekerSave entity);
}
