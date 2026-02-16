package com.hostdesign24.jobportal.mapper;

import com.hostdesign24.jobportal.dto.SkillDto;
import com.hostdesign24.jobportal.dto.SkillSaveDto;
import com.hostdesign24.jobportal.model.Skill;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SkillMapper {
    Skill toEntity(SkillSaveDto dto);
    SkillDto toDto(Skill entity);
}
