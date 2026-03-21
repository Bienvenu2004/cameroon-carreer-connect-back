package com.hostdesign24.jobportal.mapper;

import com.hostdesign24.jobportal.dto.jobActivityPost.JobCompanyDto;
import com.hostdesign24.jobportal.model.JobCompany;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JobCompanyMapper {
    @Mapping(target = "logo", ignore = true)
    JobCompanyDto toDto(JobCompany jobActivity);

    @Mapping(target = "logo", ignore = true)
    JobCompany toEntity(JobCompanyDto dto);
}
