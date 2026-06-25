package com.hostdesign24.jobportal.mapper;

import com.hostdesign24.jobportal.dto.userDevice.LoginActivityDto;
import com.hostdesign24.jobportal.model.LoginActivity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LoginActivityMapper {
    LoginActivityDto toDto(LoginActivity loginActivity);
    LoginActivity toEntity(LoginActivityDto loginActivity);
}
