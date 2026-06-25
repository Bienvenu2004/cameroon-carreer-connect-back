package com.hostdesign24.jobportal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hostdesign24.jobportal.model.enums.UserRole;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class UserDto {
    private UUID id;
    private String email;
    private boolean active;
    private UserRole role;
    private Date passwordChangedAt;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date lastLogin;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date lastTokenRefresh;
    private boolean deleted;
    private RecruiterProfileResponseDto recruiterProfile;
    private JobSeekerProfileResponseDto jobSeekerProfile;
}
