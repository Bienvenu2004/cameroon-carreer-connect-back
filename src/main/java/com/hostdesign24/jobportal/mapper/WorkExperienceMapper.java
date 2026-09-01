package com.hostdesign24.jobportal.mapper;

import com.hostdesign24.jobportal.dto.WorkExperienceDto;
import com.hostdesign24.jobportal.dto.WorkExperienceSaveDto;
import com.hostdesign24.jobportal.model.WorkExperience;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WorkExperienceMapper {

    WorkExperienceDto toDto(WorkExperience entity);

    /**
     * The {@code profile} back-reference is set explicitly by the service
     * once it has the owning {@link com.hostdesign24.jobportal.model.JobSeekerProfile}
     * in hand. Ignoring it here keeps MapStruct from generating a stub
     * that would try to dereference a null DTO field.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "profile", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    WorkExperience toEntity(WorkExperienceSaveDto dto);
}
