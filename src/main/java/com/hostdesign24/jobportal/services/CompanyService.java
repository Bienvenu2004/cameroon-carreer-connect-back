package com.hostdesign24.jobportal.services;

import com.hostdesign24.jobportal.dto.company.CompanyResponsivenessDto;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.company.CompanyEntryDto;
import com.hostdesign24.jobportal.dto.company.CompanyFilterDto;
import com.hostdesign24.jobportal.dto.company.CompanyPatchDto;
import com.hostdesign24.jobportal.dto.company.CompanyResponseDto;

import java.util.UUID;

public interface CompanyService {

    /**
     * How this employer treats applicants, computed from application history.
     * Public: it is for candidates deciding whether applying is worth their time.
     */
    CompanyResponsivenessDto getResponsiveness(UUID companyId);

    CompanyResponseDto create(CompanyEntryDto dto);

    CompanyResponseDto getById(UUID id);

    PageResponseDto<CompanyResponseDto> getAll(CompanyFilterDto filter);

    /**
     * Return all companies owned by the current authenticated recruiter,
     * newest first. Ownership = Company.createdBy matches the current user.
     */
    java.util.List<CompanyResponseDto> listMyCompanies();

    CompanyResponseDto patch(UUID id, CompanyPatchDto dto);

    void delete(UUID id);

    /** Approve a PENDING company. Sets status = APPROVED, verifiedAt = now. */
    CompanyResponseDto approve(UUID id);

    /** Reject a PENDING company. Records reason, sets status = REJECTED. */
    CompanyResponseDto reject(UUID id, String reason);

    /** Suspend an APPROVED company. Records reason, sets status = SUSPENDED. */
    CompanyResponseDto suspend(UUID id, String reason);
}
