package com.hostdesign24.jobportal.dto;

import com.hostdesign24.jobportal.model.enums.VerificationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyEmailDto {
    @NotBlank(message = " Verification code is required" )
    private String verificationCode;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "email verification type must be provided")
    private VerificationType verificationType = VerificationType.EMAIL_REGISTRATION;
}
