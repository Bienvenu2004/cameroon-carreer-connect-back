package com.hostdesign24.jobportal.dto.admin;

import com.hostdesign24.jobportal.dto.common.FilterDto;
import com.hostdesign24.jobportal.model.enums.UserRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUserFilterDto extends FilterDto {

    /** Filter by role. Null = all roles. */
    private UserRole role;

    /** Filter by active status. Null = all. */
    private Boolean active;

    /** Search by email substring (case-insensitive). */
    private String search;
}
