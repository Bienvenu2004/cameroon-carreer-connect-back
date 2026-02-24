package com.hostdesign24.jobportal.dto.userDevice;

import com.hostdesign24.jobportal.model.enums.DeviceStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class UserDeviceDto {
    private UUID id;
    private String deviceId;
    private String deviceName;
    private String ipAddress;
    private String userAgent;
    private boolean verified;
    private LocalDateTime firstLogin;
    private LocalDateTime lastLogin;
    private LocalDateTime verifiedAt;
    private DeviceStatus deviceStatus;
    private String location;
}
