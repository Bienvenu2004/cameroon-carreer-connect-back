package com.hostdesign24.jobportal.dto.userDevice;

import com.hostdesign24.jobportal.dto.common.FilterDto;
import com.hostdesign24.jobportal.model.enums.DeviceStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UserDeviceFilterDto extends FilterDto {
    private UUID userId;
    private String deviceName;
    private String ipAddress;
    private String userAgent;
    private Boolean verified;
    private DeviceStatus deviceStatus;
}
