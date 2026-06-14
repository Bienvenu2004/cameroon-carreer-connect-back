package com.hostdesign24.jobportal.mapper;

import com.hostdesign24.jobportal.dto.JobSeekerProfileResponseDto;
import com.hostdesign24.jobportal.dto.JobSeekerProfileSaveDto;
import com.hostdesign24.jobportal.model.JobSeekerProfile;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {SkillMapper.class, WorkExperienceMapper.class})
public interface JobSeekerProfileMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "resume", ignore = true)
    @Mapping(target = "profilePhoto", ignore = true)
    @Mapping(target = "experiences", ignore = true)
    JobSeekerProfile toEntity(JobSeekerProfileSaveDto dto);

    /**
     * {@code totalYearsOfExperience} is computed by the service, not derived
     * by MapStruct — needs LocalDate arithmetic across the experiences list.
     */
    @Mapping(target = "totalYearsOfExperience", ignore = true)
    JobSeekerProfileResponseDto toDto(JobSeekerProfile entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "resume", ignore = true)
    @Mapping(target = "profilePhoto", ignore = true)
    @Mapping(target = "experiences", ignore = true)
    void updateFromDto(JobSeekerProfileSaveDto dto, @MappingTarget JobSeekerProfile entity);
}
