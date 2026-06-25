package com.hostdesign24.jobportal.mapper;

import com.hostdesign24.jobportal.dto.userDevice.UserDeviceDto;
import com.hostdesign24.jobportal.model.UserDevice;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserDeviceMapper {
    UserDeviceDto toDto(UserDevice device);
    UserDevice toEntity(UserDeviceDto device);
}
