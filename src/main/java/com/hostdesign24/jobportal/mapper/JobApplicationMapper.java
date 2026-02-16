package com.hostdesign24.jobportal.mapper;

import com.hostdesign24.jobportal.dto.JobApplicationDto;
import com.hostdesign24.jobportal.model.JobApplication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JobApplicationMapper {
    @Mapping(source = "profile.id", target = "profileId")
    @Mapping(target = "candidateName", expression = "java(((apply.getProfile().getFirstName() == null ? \"\" : apply.getProfile().getFirstName()) + \" \" + (apply.getProfile().getLastName() == null ? \"\" : apply.getProfile().getLastName())).trim())")
    JobApplicationDto toDto(JobApplication apply);

}
