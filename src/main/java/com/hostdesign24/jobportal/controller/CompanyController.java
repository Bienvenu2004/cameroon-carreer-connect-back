package com.hostdesign24.jobportal.controller;

import com.hostdesign24.jobportal.dto.common.ApiResponse;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.company.CompanyEntryDto;
import com.hostdesign24.jobportal.dto.company.CompanyFilterDto;
import com.hostdesign24.jobportal.dto.company.CompanyPatchDto;
import com.hostdesign24.jobportal.dto.company.CompanyResponseDto;
import com.hostdesign24.jobportal.services.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/hjp/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CompanyResponseDto> create(@Valid @ModelAttribute CompanyEntryDto dto) {
        CompanyResponseDto createdCompany = companyService.create(dto);
        return ApiResponse.success(createdCompany, "Company created successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<CompanyResponseDto> getById(@PathVariable UUID id) {
        CompanyResponseDto company = companyService.getById(id);
        return ApiResponse.success(company, "Company retrieved successfully");
    }

    @GetMapping("/")
    public ApiResponse<PageResponseDto<CompanyResponseDto>> getAll(@ModelAttribute CompanyFilterDto filter) {
        PageResponseDto<CompanyResponseDto> companies = companyService.getAll(filter);
        return ApiResponse.success(companies, "Companies retrieved successfully");
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CompanyResponseDto> patch(@PathVariable UUID id, @Valid @ModelAttribute CompanyPatchDto dto) {
        CompanyResponseDto updatedCompany = companyService.patch(id, dto);
        return ApiResponse.success(updatedCompany, "Company updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        companyService.delete(id);
        return ApiResponse.success(null, "Company deleted successfully");
    }
}

