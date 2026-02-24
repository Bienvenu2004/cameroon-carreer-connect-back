package com.hostdesign24.jobportal.dto.userDevice;

import com.hostdesign24.jobportal.dto.common.FilterDto;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class LoginActivityFilterDto extends FilterDto {
    private UUID userId;
    private String ipAddress;
    private Boolean successful;
    private UUID deviceId;
    private Date date;
    private Date betweenStartDate;
    private Date betweenEndDate;
}
