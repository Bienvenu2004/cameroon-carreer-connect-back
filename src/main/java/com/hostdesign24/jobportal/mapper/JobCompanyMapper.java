package com.hostdesign24.jobportal.mapper;

import com.hostdesign24.jobportal.dto.jobActivityPost.JobCompanyDto;
import com.hostdesign24.jobportal.model.JobCompany;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface JobCompanyMapper {
    JobCompanyDto toDto(JobCompany jobActivity);
    JobCompany toEntity(JobCompanyDto dto);
}
