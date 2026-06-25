package com.hostdesign24.jobportal.dto.admin;

import com.hostdesign24.jobportal.model.enums.UserRole;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;

/**
 * Admin-level user view. Contains audit-relevant fields but never the password hash.
 */
@Getter
@Setter
public class AdminUserDto {
    private UUID id;
    private String email;
    private UserRole role;
    private boolean active;
    private boolean deleted;
    private LocalDate registrationDate;
    private Date lastLogin;
    private String displayName;
    private UUID companyId;
    private String companyName;
}
