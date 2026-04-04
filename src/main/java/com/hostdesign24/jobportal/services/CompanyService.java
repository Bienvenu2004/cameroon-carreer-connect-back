package com.hostdesign24.jobportal.services;

import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.company.CompanyEntryDto;
import com.hostdesign24.jobportal.dto.company.CompanyFilterDto;
import com.hostdesign24.jobportal.dto.company.CompanyPatchDto;
import com.hostdesign24.jobportal.dto.company.CompanyResponseDto;

import java.util.UUID;

public interface CompanyService {

    CompanyResponseDto create(CompanyEntryDto dto);

    CompanyResponseDto getById(UUID id);

    PageResponseDto<CompanyResponseDto> getAll(CompanyFilterDto filter);

    CompanyResponseDto patch(UUID id, CompanyPatchDto dto);

    void delete(UUID id);
}

