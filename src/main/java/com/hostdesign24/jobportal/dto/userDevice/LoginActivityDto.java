package com.hostdesign24.jobportal.dto.userDevice;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class LoginActivityDto {
    private UUID userId;
    private String ipAddress;
    private boolean successful = true;
    private String failureReason;
    private UserDeviceDto device;
    private LocalDateTime createdAt;
}
