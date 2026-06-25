package com.hostdesign24.jobportal.dto.userDevice;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyDeviceDto {
    @Email
    @NotBlank
    private String email;
    
    @NotBlank
    private String verificationCode;
    
    @NotBlank
    private String deviceId;
}