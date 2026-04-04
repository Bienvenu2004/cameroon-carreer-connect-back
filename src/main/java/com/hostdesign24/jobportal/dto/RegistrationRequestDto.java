package com.hostdesign24.jobportal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hostdesign24.jobportal.model.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationRequestDto {

    @NotBlank(message = "Email is required")
    @Email(message = "Email is not valid")
    private String email;

    private String phoneNumber;

    @NotNull(message = "User role is required")
    private UserRole role;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private boolean acceptedTerms;
    private boolean acceptedMessages;
    private String company;
    private String country;
}
