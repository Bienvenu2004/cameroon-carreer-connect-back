package com.hostdesign24.jobportal.mapper;

import com.hostdesign24.jobportal.dto.JobApplicationDto;
import com.hostdesign24.jobportal.model.JobApplication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JobApplicationMapper {
    @Mapping(source = "profile.id", target = "profileId")
    @Mapping(source = "applicationDate", target = "applyDate")
    @Mapping(source = "job.id", target = "jobId")
    @Mapping(source = "job.title", target = "jobTitle")
    @Mapping(source = "job.company.name", target = "companyName")
    @Mapping(target = "candidateName", expression = "java(((apply.getProfile().getFirstName() == null ? \"\" : apply.getProfile().getFirstName()) + \" \" + (apply.getProfile().getLastName() == null ? \"\" : apply.getProfile().getLastName())).trim())")
    JobApplicationDto toDto(JobApplication apply);

}
