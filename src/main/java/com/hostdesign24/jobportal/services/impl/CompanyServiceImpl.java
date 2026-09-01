package com.hostdesign24.jobportal.services.impl;

import java.util.Map;
import java.util.EnumMap;
import com.hostdesign24.jobportal.model.enums.Industry;
import com.hostdesign24.jobportal.dto.company.IndustryCountDto;
import com.hostdesign24.jobportal.repository.ApplicationEventRepository;
import com.hostdesign24.jobportal.dto.company.CompanyResponsivenessDto;
import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.company.CompanyEntryDto;
import com.hostdesign24.jobportal.dto.company.CompanyFilterDto;
import com.hostdesign24.jobportal.dto.company.CompanyPatchDto;
import com.hostdesign24.jobportal.dto.company.CompanyResponseDto;
import com.hostdesign24.jobportal.exception.ActionDeniedException;
import com.hostdesign24.jobportal.exception.InvalidInputException;
import com.hostdesign24.jobportal.exception.ResourceNotFoundException;
import com.hostdesign24.jobportal.mapper.CompanyMapper;
import com.hostdesign24.jobportal.mapper.FileMapper;
import com.hostdesign24.jobportal.model.Company;
import com.hostdesign24.jobportal.model.File;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.enums.CompanyStatus;
import com.hostdesign24.jobportal.repository.JobCompanyRepository;
import com.hostdesign24.jobportal.repository.JobRepository;
import com.hostdesign24.jobportal.repository.specifications.CompanySpecification;
import com.hostdesign24.jobportal.services.CompanyService;
import com.hostdesign24.jobportal.services.FileService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final ApplicationEventRepository applicationEventRepository;

    private final JobCompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final CompanyMapper companyMapper;
    private final CompanySpecification companySpecification;
    private final FileService fileService;
    private final FileMapper fileMapper;



    @Override
    @Transactional
    public CompanyResponseDto create(CompanyEntryDto dto) {
        Company company = companyMapper.toEntity(dto);
        // New companies always start as PENDING and require admin approval before recruiters
        // can post jobs under them.
        company.setStatus(CompanyStatus.PENDING);

        company = companyRepository.save(company);

        boolean filesUploaded = false;
        if (dto.getLogo() != null) {
            File logo = fileService.uploadFile(dto.getLogo(), company.getId(), "COMPANY_LOGO", "Company");
            company.setLogo(logo);
            filesUploaded = true;
        }
        if (dto.getBanner() != null) {
            File banner = fileService.uploadFile(dto.getBanner(), company.getId(), "COMPANY_BANNER", "Company");
            company.setBanner(banner);
            filesUploaded = true;
        }
        if (filesUploaded) {
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
        response.setLogo(fileMapper.toDto(company.getLogo()));
        response.setBanner(fileMapper.toDto(company.getBanner()));
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
    @Transactional(readOnly = true)
    public List<CompanyResponseDto> listMyCompanies() {
        User user = Utils.getCurrentUser()
                .orElseThrow(() -> new ActionDeniedException("Authentication required"));
        return companyRepository
                .findAllByCreatedByAndDeletedFalseOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::getCompanyResponseDto)
                .toList();
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

        if (dto.getBanner() != null) {
            // Same replace-then-upload pattern as the logo: delete the old
            // file from disk + DB before uploading the new one so we don't
            // orphan storage rows when the recruiter swaps banners.
            if (company.getBanner() != null) {
                fileService.deleteFile(company.getBanner().getId());
            }
            File banner = fileService.uploadFile(dto.getBanner(), company.getId(), "COMPANY_BANNER", "Company");
            company.setBanner(banner);
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

    @Override
    @Transactional
    public CompanyResponseDto approve(UUID id) {
        Company company = findCompanyOrThrow(id);
        if (company.getStatus() == CompanyStatus.APPROVED) {
            return getCompanyResponseDto(company);
        }
        // Business rule: at most one APPROVED company per name. Enforced here
        // (the single choke point where a company becomes APPROVED) rather than
        // at creation, so duplicate PENDING submissions can coexist but only
        // one can ever be accepted. Case-insensitive; ignores the company's own
        // row and any soft-deleted companies.
        String name = company.getName() == null ? "" : company.getName().trim();
        boolean nameTaken = companyRepository.existsByNameIgnoreCaseAndStatusAndDeletedFalseAndIdNot(
                name, CompanyStatus.APPROVED, company.getId());
        if (nameTaken) {
            throw new InvalidInputException(
                    "Another approved company already exists with the name \"" + name
                            + "\". Two approved companies cannot share the same name.");
        }
        company.setStatus(CompanyStatus.APPROVED);
        company.setRejectionReason(null);
        company.setVerifiedAt(LocalDateTime.now());
        return getCompanyResponseDto(companyRepository.save(company));
    }

    @Override
    @Transactional
    public CompanyResponseDto reject(UUID id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new InvalidInputException("A reason is required to reject a company");
        }
        Company company = findCompanyOrThrow(id);
        company.setStatus(CompanyStatus.REJECTED);
        company.setRejectionReason(reason);
        company.setVerifiedAt(LocalDateTime.now());
        return getCompanyResponseDto(companyRepository.save(company));
    }

    @Override
    @Transactional
    public CompanyResponseDto suspend(UUID id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new InvalidInputException("A reason is required to suspend a company");
        }
        Company company = findCompanyOrThrow(id);
        company.setStatus(CompanyStatus.SUSPENDED);
        company.setRejectionReason(reason);
        company.setVerifiedAt(LocalDateTime.now());
        return getCompanyResponseDto(companyRepository.save(company));
    }

    private Company findCompanyOrThrow(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + id));
    }

    /**
     * Employer responsiveness.
     *
     * The threshold matters as much as the arithmetic: publishing "0% response
     * rate" off a single unanswered application would defame an employer who
     * joined last week, so below it we say we do not know rather than guessing.
     */
    private static final int MIN_APPLICATIONS_FOR_A_RATE = 5;

    @Override
    @Transactional(readOnly = true)
    public CompanyResponsivenessDto getResponsiveness(UUID companyId) {
        List<Object[]> rows = applicationEventRepository.responsivenessForCompany(companyId);

        long received = 0;
        long answered = 0;
        Double avgDays = null;

        if (rows != null && !rows.isEmpty() && rows.get(0) != null) {
            Object[] row = rows.get(0);
            received = row[0] == null ? 0 : ((Number) row[0]).longValue();
            answered = row[1] == null ? 0 : ((Number) row[1]).longValue();
            avgDays = row[2] == null ? null : ((Number) row[2]).doubleValue();
        }

        boolean enough = received >= MIN_APPLICATIONS_FOR_A_RATE;

        return CompanyResponsivenessDto.builder()
                .applicationsReceived(received)
                .applicationsAnswered(answered)
                .responseRate(enough ? (int) Math.round(100.0 * answered / received) : null)
                .averageDaysToRespond(enough && avgDays != null
                        ? (int) Math.round(Math.max(0, avgDays)) : null)
                .enoughData(enough)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IndustryCountDto> getIndustryCounts() {
        Map<Industry, Long> counts = new EnumMap<>(Industry.class);
        for (Industry i : Industry.values()) {
            counts.put(i, 0L);
        }
        for (Object[] row : companyRepository.countApprovedByIndustry()) {
            if (row[0] == null) continue;
            counts.put((Industry) row[0], ((Number) row[1]).longValue());
        }
        return counts.entrySet().stream()
                .map(e -> new IndustryCountDto(e.getKey(), e.getValue()))
                .toList();
    }
}
