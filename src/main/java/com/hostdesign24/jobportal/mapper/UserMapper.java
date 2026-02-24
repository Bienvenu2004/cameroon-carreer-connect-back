package com.hostdesign24.jobportal.mapper;

import com.hostdesign24.jobportal.dto.UserDto;
import com.hostdesign24.jobportal.dto.UserRegistrationDto;
import com.hostdesign24.jobportal.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toEntity(UserRegistrationDto dto);
    UserDto toUserDto (User user);
}
