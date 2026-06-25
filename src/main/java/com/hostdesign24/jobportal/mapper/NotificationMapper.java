package com.hostdesign24.jobportal.mapper;

import com.hostdesign24.jobportal.dto.NotificationDto;
import com.hostdesign24.jobportal.model.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationDto toDto(Notification notification);
}
