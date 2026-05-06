package com.hostdesign24.jobportal.controller;

import com.hostdesign24.jobportal.dto.admin.AdminPlatformStatsDto;
import com.hostdesign24.jobportal.dto.admin.AdminUserDto;
import com.hostdesign24.jobportal.dto.admin.AdminUserFilterDto;
import com.hostdesign24.jobportal.dto.admin.CompanyDecisionDto;
import com.hostdesign24.jobportal.dto.common.ApiResponse;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.company.CompanyFilterDto;
import com.hostdesign24.jobportal.dto.company.CompanyResponseDto;
import com.hostdesign24.jobportal.services.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin moderation endpoints. All operations require SYSTEM_ADMIN role.
 */
@RestController
@RequestMapping("/api/hjp/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class AdminController {

    private final AdminService adminService;

    /* ---------------- USERS ---------------- */

    @GetMapping("/users")
    public ApiResponse<PageResponseDto<AdminUserDto>> listUsers(@ModelAttribute AdminUserFilterDto filter) {
        return ApiResponse.success(adminService.listUsers(filter), "Users retrieved");
    }

    @GetMapping("/users/{id}")
    public ApiResponse<AdminUserDto> getUser(@PathVariable UUID id) {
        return ApiResponse.success(adminService.getUser(id), "User retrieved");
    }

    @PatchMapping("/users/{id}/suspend")
    public ApiResponse<AdminUserDto> suspend(@PathVariable UUID id) {
        return ApiResponse.success(adminService.suspendUser(id), "User suspended");
    }

    @PatchMapping("/users/{id}/reactivate")
    public ApiResponse<AdminUserDto> reactivate(@PathVariable UUID id) {
        return ApiResponse.success(adminService.reactivateUser(id), "User reactivated");
    }

    @DeleteMapping("/users/{id}")
    public ApiResponse<Void> softDelete(@PathVariable UUID id) {
        adminService.softDeleteUser(id);
        return ApiResponse.success("User soft-deleted");
    }

    /* ---------------- COMPANIES ---------------- */

    @GetMapping("/companies")
    public ApiResponse<PageResponseDto<CompanyResponseDto>> listCompanies(@ModelAttribute CompanyFilterDto filter) {
        return ApiResponse.success(adminService.listCompanies(filter), "Companies retrieved");
    }

    @PatchMapping("/companies/{id}/approve")
    public ApiResponse<CompanyResponseDto> approveCompany(@PathVariable UUID id) {
        return ApiResponse.success(adminService.approveCompany(id), "Company approved");
    }

    @PatchMapping("/companies/{id}/reject")
    public ApiResponse<CompanyResponseDto> rejectCompany(@PathVariable UUID id,
                                                          @RequestBody CompanyDecisionDto body) {
        return ApiResponse.success(adminService.rejectCompany(id, body.getReason()), "Company rejected");
    }

    @PatchMapping("/companies/{id}/suspend")
    public ApiResponse<CompanyResponseDto> suspendCompany(@PathVariable UUID id,
                                                           @RequestBody CompanyDecisionDto body) {
        return ApiResponse.success(adminService.suspendCompany(id, body.getReason()), "Company suspended");
    }

    /* ---------------- STATS ---------------- */

    @GetMapping("/stats")
    public ApiResponse<AdminPlatformStatsDto> stats() {
        return ApiResponse.success(adminService.getPlatformStats(), "Platform stats");
    }
}
