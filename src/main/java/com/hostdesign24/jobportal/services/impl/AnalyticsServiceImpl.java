package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.dto.analytics.DashboardDto;
import com.hostdesign24.jobportal.dto.analytics.JobStatsDto;
import com.hostdesign24.jobportal.model.Address;
import com.hostdesign24.jobportal.model.Job;
import com.hostdesign24.jobportal.model.JobApplication;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.enums.ApplicationStatus;
import com.hostdesign24.jobportal.model.enums.UserRole;
import com.hostdesign24.jobportal.repository.JobRepository;
import com.hostdesign24.jobportal.repository.JobSeekerApplyRepository;
import com.hostdesign24.jobportal.services.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Aggregates dashboard analytics scoped to the caller's role:
 *   - SYSTEM_ADMIN: platform-wide stats
 *   - RECRUITER: only stats for jobs they themselves created (createdBy = current user id)
 *   - JOB_SEEKER: stats over their own applications (basic)
 */
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final JobRepository jobRepository;
    private final JobSeekerApplyRepository jobSeekerApplyRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardDto getDashboardStats(UUID userId) {
        Optional<User> currentUser = Utils.getCurrentUser();
        UserRole role = currentUser.map(User::getRole).orElse(null);
        UUID actorId = currentUser.map(User::getId).orElse(userId);

        List<Job> jobs = jobRepository.findAll().stream()
                .filter(j -> !j.isDeleted())
                .toList();

        if (role == UserRole.RECRUITER && actorId != null) {
            jobs = jobs.stream()
                    .filter(j -> actorId.equals(j.getCreatedBy()))
                    .toList();
        }

        long totalJobs = jobs.size();
        long totalActiveJobs = jobs.stream().filter(Job::isActive).count();
        long totalViews = 0;
        long totalApplications = 0;

        java.util.List<JobStatsDto> jobStatsList = new java.util.ArrayList<>();

        for (Job job : jobs) {
            long appsCount = jobSeekerApplyRepository.countByJobId(job.getId());
            totalApplications += appsCount;
            Integer views = job.getViews();
            long viewsCount = (views == null) ? 0 : views;
            totalViews += viewsCount;

            JobStatsDto js = new JobStatsDto();
            js.setJobId(job.getId());
            js.setJobTitle(job.getTitle());
            js.setViews((int) viewsCount);
            js.setApplicationsCount(appsCount);
            jobStatsList.add(js);
        }

        // Application-level aggregates (status, demographics, time-series)
        java.util.Set<UUID> jobIdSet = new java.util.HashSet<>();
        jobs.forEach(j -> jobIdSet.add(j.getId()));

        Map<String, Long> appsByStatus = new HashMap<>();
        for (ApplicationStatus s : ApplicationStatus.values()) appsByStatus.put(s.name(), 0L);

        Map<String, Long> demographics = new HashMap<>();
        Map<String, Long> appsByMonth = new TreeMap<>();
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        LocalDate cutoff6m = LocalDate.now().minusMonths(6);

        if (role == UserRole.JOB_SEEKER && actorId != null) {
            // Seeker view: their own applications.
            jobSeekerApplyRepository.findAll().stream()
                    .filter(a -> a.getProfile() != null
                            && a.getProfile().getUser() != null
                            && actorId.equals(a.getProfile().getUser().getId()))
                    .forEach(a -> aggregateApp(a, appsByStatus, demographics, appsByMonth, monthFmt, cutoff6m));
        } else {
            // Admin / recruiter view: applications on the visible jobs.
            jobSeekerApplyRepository.findAll().stream()
                    .filter(a -> a.getJob() != null && jobIdSet.contains(a.getJob().getId()))
                    .forEach(a -> aggregateApp(a, appsByStatus, demographics, appsByMonth, monthFmt, cutoff6m));
        }

        return DashboardDto.builder()
                .totalJobs(totalJobs)
                .totalActiveJobs(totalActiveJobs)
                .totalViews(totalViews)
                .totalApplications(totalApplications)
                .jobsStats(jobStatsList)
                .applicationsByStatus(appsByStatus)
                .demographics(demographics)
                .applicationsByMonth(appsByMonth)
                .build();
    }

    private void aggregateApp(JobApplication a,
                              Map<String, Long> appsByStatus,
                              Map<String, Long> demographics,
                              Map<String, Long> appsByMonth,
                              DateTimeFormatter monthFmt,
                              LocalDate cutoff6m) {
        ApplicationStatus st = a.getStatus() == null ? ApplicationStatus.APPLIED : a.getStatus();
        appsByStatus.merge(st.name(), 1L, Long::sum);

        if (a.getProfile() != null) {
            Address addr = a.getProfile().getAddress();
            if (addr != null) {
                String city = addr.getCity();
                String country = addr.getCountry();
                if (city != null || country != null) {
                    String key = (city != null ? city : "Unknown")
                            + ", " + (country != null ? country : "Unknown");
                    demographics.merge(key, 1L, Long::sum);
                }
            }
        }

        if (a.getApplicationDate() != null && !a.getApplicationDate().isBefore(cutoff6m)) {
            appsByMonth.merge(a.getApplicationDate().format(monthFmt), 1L, Long::sum);
        }
    }
}
