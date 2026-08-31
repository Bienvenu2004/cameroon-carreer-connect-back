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
import com.hostdesign24.jobportal.model.enums.Region;
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

    /**
     * Platform-wide counters for the admin dashboard.
     *
     * This previously ran findAll() over the users table three times, the jobs
     * table three times and the applications table once, then filtered and
     * counted in Java -- seven full table scans per dashboard load, with every
     * row materialised as a managed entity.
     *
     * Every counter is now a database aggregate. The two time series group by
     * calendar day and are folded into weeks/months here, so the number of rows
     * crossing the wire is bounded by the length of the window (12 weeks, 6
     * months) rather than by the size of the tables.
     */
    @Override
    @Transactional(readOnly = true)
    public AdminPlatformStatsDto getPlatformStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countActive();

        Map<UserRole, Long> roleCounts = zeroed(UserRole.values());
        fold(userRepository.countByRole(), roleCounts);

        long totalCompanies = companyRepository.count();
        Map<CompanyStatus, Long> companyStatusCounts = zeroed(CompanyStatus.values());
        // A company with no status set is treated as PENDING, matching the entity default.
        for (Object[] row : companyRepository.countByStatus()) {
            CompanyStatus status = row[0] == null ? CompanyStatus.PENDING : (CompanyStatus) row[0];
            companyStatusCounts.merge(status, ((Number) row[1]).longValue(), Long::sum);
        }

        long totalJobs = jobRepository.count();
        long activeJobs = jobRepository.countActiveJobs();

        long totalApps = jobApplicationRepository.count();
        Map<String, Long> appsByStatus = new HashMap<>();
        for (ApplicationStatus s : ApplicationStatus.values()) appsByStatus.put(s.name(), 0L);
        for (Object[] row : jobApplicationRepository.countByStatus()) {
            ApplicationStatus status = row[0] == null ? ApplicationStatus.APPLIED : (ApplicationStatus) row[0];
            appsByStatus.merge(status.name(), ((Number) row[1]).longValue(), Long::sum);
        }

        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, Long> jobsByMonth = new TreeMap<>();
        for (Object[] row : jobRepository.countPostedByDaySince(LocalDate.now().minusMonths(6))) {
            LocalDate day = (LocalDate) row[0];
            jobsByMonth.merge(day.format(monthFmt), ((Number) row[1]).longValue(), Long::sum);
        }

        WeekFields wf = WeekFields.of(Locale.getDefault());
        Map<String, Long> signupsByWeek = new TreeMap<>();
        for (Object[] row : userRepository.countSignupsByDaySince(LocalDate.now().minusWeeks(12))) {
            LocalDate day = (LocalDate) row[0];
            String key = day.getYear() + "-W"
                    + String.format("%02d", day.get(wf.weekOfWeekBasedYear()));
            signupsByWeek.merge(key, ((Number) row[1]).longValue(), Long::sum);
        }

        Map<String, Long> jobsByRegion = new HashMap<>();
        for (Object[] row : jobRepository.countByRegion()) {
            jobsByRegion.merge(((Region) row[0]).name(), ((Number) row[1]).longValue(), Long::sum);
        }

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

    /** A map pre-populated with every enum constant at zero, so absent groups still render. */
    private static <E extends Enum<E>> Map<E, Long> zeroed(E[] values) {
        Map<E, Long> m = new HashMap<>();
        for (E v : values) m.put(v, 0L);
        return m;
    }

    /** Folds Object[]{ enumValue, count } aggregate rows into a counts map. */
    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> void fold(List<Object[]> rows, Map<E, Long> into) {
        for (Object[] row : rows) {
            if (row[0] == null) continue;
            into.merge((E) row[0], ((Number) row[1]).longValue(), Long::sum);
        }
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
