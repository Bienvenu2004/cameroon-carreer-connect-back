package com.hostdesign24.jobportal.mapper;

import com.hostdesign24.jobportal.dto.RecruiterProfileResponseDto;
import com.hostdesign24.jobportal.dto.RecruiterProfileUpsertDto;
import com.hostdesign24.jobportal.model.RecruiterProfile;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RecruiterProfileMapper {
    RecruiterProfileResponseDto toResponse(RecruiterProfile entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "profilePhoto", ignore = true)
    void updateFromDto(RecruiterProfileUpsertDto dto, @MappingTarget RecruiterProfile entity);
}
