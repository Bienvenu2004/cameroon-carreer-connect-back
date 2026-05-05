package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.company.CompanyEntryDto;
import com.hostdesign24.jobportal.dto.company.CompanyFilterDto;
import com.hostdesign24.jobportal.dto.company.CompanyPatchDto;
import com.hostdesign24.jobportal.dto.company.CompanyResponseDto;
import com.hostdesign24.jobportal.exception.ResourceNotFoundException;
import com.hostdesign24.jobportal.mapper.CompanyMapper;
import com.hostdesign24.jobportal.mapper.FileMapper;
import com.hostdesign24.jobportal.model.Company;
import com.hostdesign24.jobportal.model.File;
import com.hostdesign24.jobportal.repository.JobCompanyRepository;
import com.hostdesign24.jobportal.repository.JobRepository;
import com.hostdesign24.jobportal.repository.specifications.CompanySpecification;
import com.hostdesign24.jobportal.services.CompanyService;
import com.hostdesign24.jobportal.services.FileService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final JobCompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final CompanyMapper companyMapper;
    private final CompanySpecification companySpecification;
    private final FileService fileService;
    private final FileMapper fileMapper;

    @Value("${app.storage.base-url}")
    private String publicUrl;

    @Override
    @Transactional
    public CompanyResponseDto create(CompanyEntryDto dto) {
        Company company = companyMapper.toEntity(dto);

        company = companyRepository.save(company);

        if (dto.getLogo() != null) {
            File logo = fileService.uploadFile(dto.getLogo(), company.getId(), "COMPANY_LOGO", "Company");
            company.setLogo(logo);
            companyRepository.save(company);
        }
        return getCompanyResponseDto(company);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponseDto getById(UUID id) {
        Company company = findCompanyOrThrow(id);

        return getCompanyResponseDto(company);
    }

    private @NonNull CompanyResponseDto getCompanyResponseDto(Company company) {
        CompanyResponseDto response = companyMapper.toResponse(company);
        response.setLogo(fileMapper.toDto(company.getLogo(), publicUrl));
        response.setActiveJobs(jobRepository.countByCompanyIdAndIsActiveTrueAndDeletedFalse(company.getId()));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<CompanyResponseDto> getAll(CompanyFilterDto filter) {
        Specification<Company> specification = companySpecification.build(filter);
        Page<Company> companyPage = companyRepository.findAll(specification, filter.toPageable());

        List<CompanyResponseDto> content = companyPage.getContent().stream()
                .map(this::getCompanyResponseDto)
                .toList();

        return new PageResponseDto<>(
                content,
                companyPage.getNumber(),
                companyPage.getSize(),
                companyPage.getTotalElements(),
                companyPage.getTotalPages(),
                companyPage.isLast()
        );
    }

    @Override
    @Transactional
    public CompanyResponseDto patch(UUID id, CompanyPatchDto dto) {
        Company company = findCompanyOrThrow(id);
        companyMapper.updateFromPatchDto(dto, company);

        if (dto.getLogo() != null) {
            if (company.getLogo() != null) {
                fileService.deleteFile(company.getLogo().getId());
            }
            File logo = fileService.uploadFile(dto.getLogo(), company.getId(), "COMPANY_LOGO", "Company");
            company.setLogo(logo);
        }

        Company savedCompany = companyRepository.save(company);
        return getCompanyResponseDto(savedCompany);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Company company = findCompanyOrThrow(id);
        companyRepository.delete(company);
    }

    private Company findCompanyOrThrow(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + id));
    }
}

