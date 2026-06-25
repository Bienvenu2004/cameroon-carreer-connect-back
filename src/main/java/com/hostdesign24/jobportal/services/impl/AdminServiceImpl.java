package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.dto.admin.AdminPlatformStatsDto;
import com.hostdesign24.jobportal.dto.admin.AdminUserDto;
import com.hostdesign24.jobportal.dto.admin.AdminUserFilterDto;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.company.CompanyFilterDto;
import com.hostdesign24.jobportal.dto.company.CompanyResponseDto;
import com.hostdesign24.jobportal.exception.ResourceNotFoundException;
import com.hostdesign24.jobportal.model.JobApplication;
import com.hostdesign24.jobportal.model.JobSeekerProfile;
import com.hostdesign24.jobportal.model.RecruiterProfile;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.enums.ApplicationStatus;
import com.hostdesign24.jobportal.model.enums.CompanyStatus;
import com.hostdesign24.jobportal.model.enums.UserRole;
import com.hostdesign24.jobportal.repository.CompanyRepository;
import com.hostdesign24.jobportal.repository.JobRepository;
import com.hostdesign24.jobportal.repository.JobSeekerApplyRepository;
import com.hostdesign24.jobportal.repository.UserRepository;
import com.hostdesign24.jobportal.repository.specifications.AdminUserSpecification;
import com.hostdesign24.jobportal.services.AdminService;
import com.hostdesign24.jobportal.services.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final JobSeekerApplyRepository jobApplicationRepository;
    private final AdminUserSpecification adminUserSpecification;
    private final CompanyService companyService;

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<AdminUserDto> listUsers(AdminUserFilterDto filter) {
        Specification<User> spec = adminUserSpecification.build(filter);
        Page<User> page = userRepository.findAll(spec, filter.toPageable());

        List<AdminUserDto> content = page.getContent().stream()
                .map(this::toAdminUserDto)
                .toList();

        return new PageResponseDto<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserDto getUser(UUID userId) {
        return toAdminUserDto(findUserOrThrow(userId));
    }

    @Override
    @Transactional
    public AdminUserDto suspendUser(UUID userId) {
        User user = findUserOrThrow(userId);
        user.setActive(false);
        return toAdminUserDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public AdminUserDto reactivateUser(UUID userId) {
        User user = findUserOrThrow(userId);
        user.setActive(true);
        return toAdminUserDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public void softDeleteUser(UUID userId) {
        User user = findUserOrThrow(userId);
        user.setDeleted(true);
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<CompanyResponseDto> listCompanies(CompanyFilterDto filter) {
        return companyService.getAll(filter);
    }

    @Override
    public CompanyResponseDto approveCompany(UUID companyId) {
        return companyService.approve(companyId);
    }

    @Override
    public CompanyResponseDto rejectCompany(UUID companyId, String reason) {
        return companyService.reject(companyId, reason);
    }

    @Override
    public CompanyResponseDto suspendCompany(UUID companyId, String reason) {
        return companyService.suspend(companyId, reason);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPlatformStatsDto getPlatformStats() {
        long totalUsers = userRepository.count();

        Map<UserRole, Long> roleCounts = new HashMap<>();
        for (UserRole role : UserRole.values()) roleCounts.put(role, 0L);
        userRepository.findAll().stream()
                .filter(u -> !u.isDeleted())
                .forEach(u -> roleCounts.merge(u.getRole(), 1L, Long::sum));

        long activeUsers = userRepository.findAll().stream()
                .filter(u -> !u.isDeleted() && u.isActive()).count();

        long totalCompanies = companyRepository.count();
        Map<CompanyStatus, Long> companyStatusCounts = new HashMap<>();
        for (CompanyStatus s : CompanyStatus.values()) companyStatusCounts.put(s, 0L);
        companyRepository.findAll().stream()
                .filter(c -> !c.isDeleted())
                .forEach(c -> companyStatusCounts.merge(
                        c.getStatus() == null ? CompanyStatus.PENDING : c.getStatus(),
                        1L, Long::sum));

        long totalJobs = jobRepository.count();
        long activeJobs = jobRepository.findAll().stream()
                .filter(j -> !j.isDeleted() && j.isActive()).count();

        long totalApps = jobApplicationRepository.count();
        Map<String, Long> appsByStatus = new HashMap<>();
        for (ApplicationStatus s : ApplicationStatus.values()) appsByStatus.put(s.name(), 0L);
        jobApplicationRepository.findAll().forEach(a -> appsByStatus.merge(
                a.getStatus() == null ? ApplicationStatus.APPLIED.name() : a.getStatus().name(),
                1L, Long::sum));

        Map<String, Long> jobsByMonth = new TreeMap<>();
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        LocalDate cutoff6m = LocalDate.now().minusMonths(6);
        jobRepository.findAll().stream()
                .filter(j -> !j.isDeleted() && j.getPostedDate() != null
                        && !j.getPostedDate().isBefore(cutoff6m))
                .forEach(j -> jobsByMonth.merge(j.getPostedDate().format(monthFmt), 1L, Long::sum));

        Map<String, Long> signupsByWeek = new TreeMap<>();
        LocalDate cutoff12w = LocalDate.now().minusWeeks(12);
        WeekFields wf = WeekFields.of(Locale.getDefault());
        userRepository.findAll().stream()
                .filter(u -> !u.isDeleted() && u.getRegistrationDate() != null
                        && !u.getRegistrationDate().isBefore(cutoff12w))
                .forEach(u -> {
                    LocalDate d = u.getRegistrationDate();
                    String key = d.getYear() + "-W"
                            + String.format("%02d", d.get(wf.weekOfWeekBasedYear()));
                    signupsByWeek.merge(key, 1L, Long::sum);
                });

        Map<String, Long> jobsByRegion = new HashMap<>();
        jobRepository.findAll().stream()
                .filter(j -> !j.isDeleted() && j.getLocation() != null
                        && j.getLocation().getRegion() != null)
                .forEach(j -> jobsByRegion.merge(
                        j.getLocation().getRegion().name(), 1L, Long::sum));

        return AdminPlatformStatsDto.builder()
                .totalUsers(totalUsers)
                .totalJobSeekers(roleCounts.getOrDefault(UserRole.JOB_SEEKER, 0L))
                .totalRecruiters(roleCounts.getOrDefault(UserRole.RECRUITER, 0L))
                .totalAdmins(roleCounts.getOrDefault(UserRole.SYSTEM_ADMIN, 0L))
                .activeUsers(activeUsers)
                .suspendedUsers(totalUsers - activeUsers)
                .totalCompanies(totalCompanies)
                .pendingCompanies(companyStatusCounts.getOrDefault(CompanyStatus.PENDING, 0L))
                .approvedCompanies(companyStatusCounts.getOrDefault(CompanyStatus.APPROVED, 0L))
                .rejectedCompanies(companyStatusCounts.getOrDefault(CompanyStatus.REJECTED, 0L))
                .totalJobs(totalJobs)
                .activeJobs(activeJobs)
                .totalApplications(totalApps)
                .applicationsByStatus(appsByStatus)
                .jobsByMonth(jobsByMonth)
                .signupsByWeek(signupsByWeek)
                .jobsByRegion(jobsByRegion)
                .build();
    }

    /* ---------------- helpers ---------------- */

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private AdminUserDto toAdminUserDto(User u) {
        AdminUserDto d = new AdminUserDto();
        d.setId(u.getId());
        d.setEmail(u.getEmail());
        d.setRole(u.getRole());
        d.setActive(u.isActive());
        d.setDeleted(u.isDeleted());
        d.setRegistrationDate(u.getRegistrationDate());
        d.setLastLogin(u.getLastLogin());

        // Best-effort display name and company link.
        if (u.getJobSeekerProfile() != null) {
            JobSeekerProfile p = u.getJobSeekerProfile();
            d.setDisplayName(joinNonBlank(p.getFirstName(), p.getLastName()));
        } else if (u.getRecruiterProfile() != null) {
            RecruiterProfile p = u.getRecruiterProfile();
            d.setDisplayName(joinNonBlank(p.getFirstName(), p.getLastName()));
            d.setCompanyName(p.getCompany());
        }
        return d;
    }

    private static String joinNonBlank(String a, String b) {
        StringBuilder sb = new StringBuilder();
        if (a != null && !a.isBlank()) sb.append(a);
        if (b != null && !b.isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(b);
        }
        return sb.length() == 0 ? null : sb.toString();
    }
}
