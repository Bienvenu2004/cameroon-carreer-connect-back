package com.hostdesign24.jobportal.mapper;

import com.hostdesign24.jobportal.dto.SkillDto;
import com.hostdesign24.jobportal.dto.SkillSaveDto;
import com.hostdesign24.jobportal.dto.skill.SkillEntryDto;
import com.hostdesign24.jobportal.dto.skill.SkillPatchDto;
import com.hostdesign24.jobportal.dto.skill.SkillResponseDto;
import com.hostdesign24.jobportal.model.Skill;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface SkillMapper {
    Skill toEntity(SkillSaveDto dto);

    Skill toEntity(SkillEntryDto dto);

    SkillDto toDto(Skill entity);

    SkillResponseDto toResponse(Skill entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "jobSeekerProfile", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateFromPatchDto(SkillPatchDto dto, @MappingTarget Skill entity);
}
