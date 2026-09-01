package com.hostdesign24.jobportal.mapper;

import com.hostdesign24.jobportal.dto.EducationDto;
import com.hostdesign24.jobportal.dto.EducationSaveDto;
import com.hostdesign24.jobportal.model.Education;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EducationMapper {

    EducationDto toDto(Education entity);

    /** The profile back-reference is set by the service; see WorkExperienceMapper. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "profile", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Education toEntity(EducationSaveDto dto);
}
