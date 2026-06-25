package com.hostdesign24.jobportal.services;

import com.hostdesign24.jobportal.dto.admin.AdminPlatformStatsDto;
import com.hostdesign24.jobportal.dto.admin.AdminUserDto;
import com.hostdesign24.jobportal.dto.admin.AdminUserFilterDto;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.company.CompanyFilterDto;
import com.hostdesign24.jobportal.dto.company.CompanyResponseDto;

import java.util.UUID;

/**
 * Admin-only operations: user moderation, company verification, platform stats.
 * All methods assume caller is SYSTEM_ADMIN — enforce with @PreAuthorize at controller layer.
 */
public interface AdminService {

    PageResponseDto<AdminUserDto> listUsers(AdminUserFilterDto filter);

    AdminUserDto getUser(UUID userId);

    AdminUserDto suspendUser(UUID userId);

    AdminUserDto reactivateUser(UUID userId);

    void softDeleteUser(UUID userId);

    PageResponseDto<CompanyResponseDto> listCompanies(CompanyFilterDto filter);

    CompanyResponseDto approveCompany(UUID companyId);

    CompanyResponseDto rejectCompany(UUID companyId, String reason);

    CompanyResponseDto suspendCompany(UUID companyId, String reason);

    AdminPlatformStatsDto getPlatformStats();
}
