package com.hostdesign24.jobportal.mapper;

import com.hostdesign24.jobportal.dto.JobSeekerProfileResponseDto;
import com.hostdesign24.jobportal.dto.JobSeekerProfileSaveDto;
import com.hostdesign24.jobportal.model.JobSeekerProfile;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {SkillMapper.class})
public interface JobSeekerProfileMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "resume", ignore = true)
    @Mapping(target = "profilePhoto", ignore = true)
    JobSeekerProfile toEntity(JobSeekerProfileSaveDto dto);

    JobSeekerProfileResponseDto toDto(JobSeekerProfile entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "resume", ignore = true)
    @Mapping(target = "profilePhoto", ignore = true)
    void updateFromDto(JobSeekerProfileSaveDto dto, @MappingTarget JobSeekerProfile entity);
}
