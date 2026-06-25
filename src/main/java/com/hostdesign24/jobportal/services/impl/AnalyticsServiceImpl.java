package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.dto.analytics.DashboardDto;
import com.hostdesign24.jobportal.dto.analytics.JobStatsDto;
import com.hostdesign24.jobportal.dto.analytics.RegionalStatsDto;
import com.hostdesign24.jobportal.model.Address;
import com.hostdesign24.jobportal.model.Job;
import com.hostdesign24.jobportal.model.JobApplication;
import com.hostdesign24.jobportal.model.Skill;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.enums.ApplicationStatus;
import com.hostdesign24.jobportal.model.enums.JobLanguage;
import com.hostdesign24.jobportal.model.enums.UserRole;
import com.hostdesign24.jobportal.repository.JobRepository;
import com.hostdesign24.jobportal.repository.JobSeekerApplyRepository;
import com.hostdesign24.jobportal.services.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    /* =========================================================================
     *  Regional analytics — powers the admin Regional Trending dashboard
     * ======================================================================= */

    @Override
    @Transactional(readOnly = true)
    public RegionalStatsDto getRegionalStats() {
        List<Job> activeJobs = jobRepository.findAll().stream()
                .filter(j -> !j.isDeleted() && j.isActive())
                .toList();

        // 1. Jobs per region -------------------------------------------------
        Map<String, Long> jobsByRegion = new HashMap<>();
        // 2. Top hiring companies per region --------------------------------
        Map<String, Map<String, Long>> companiesByRegion = new HashMap<>();

        for (Job job : activeJobs) {
            String regionKey = regionKey(job);
            if (regionKey == null) continue;
            jobsByRegion.merge(regionKey, 1L, Long::sum);
            if (job.getCompany() != null && job.getCompany().getName() != null) {
                companiesByRegion
                        .computeIfAbsent(regionKey, k -> new HashMap<>())
                        .merge(job.getCompany().getName(), 1L, Long::sum);
            }
        }

        // 3. Applications per region (region of the job applied to) ----------
        Map<String, Long> applicationsByRegion = new HashMap<>();
        // 4. Top skills per region (from each applicant's profile) -----------
        Map<String, Map<String, Long>> skillsByRegion = new HashMap<>();

        List<JobApplication> allApps = jobSeekerApplyRepository.findAll().stream()
                .filter(a -> a.getJob() != null && !a.getJob().isDeleted())
                .toList();

        for (JobApplication app : allApps) {
            String regionKey = regionKey(app.getJob());
            if (regionKey == null) continue;
            applicationsByRegion.merge(regionKey, 1L, Long::sum);

            if (app.getProfile() != null && app.getProfile().getSkills() != null) {
                Map<String, Long> bucket = skillsByRegion
                        .computeIfAbsent(regionKey, k -> new HashMap<>());
                for (Skill s : app.getProfile().getSkills()) {
                    if (s != null && s.getName() != null && !s.getName().isBlank()) {
                        bucket.merge(s.getName(), 1L, Long::sum);
                    }
                }
            }
        }

        // 5. Platform-wide language distribution ----------------------------
        Map<String, Long> languageDistribution = new LinkedHashMap<>();
        // Initialize all buckets so the frontend always has every bar.
        for (JobLanguage lang : JobLanguage.values()) {
            languageDistribution.put(lang.name(), 0L);
        }
        for (Job job : activeJobs) {
            if (job.getRequiredLanguage() != null) {
                languageDistribution.merge(job.getRequiredLanguage().name(), 1L, Long::sum);
            }
        }

        return RegionalStatsDto.builder()
                .jobsByRegion(jobsByRegion)
                .applicationsByRegion(applicationsByRegion)
                .languageDistribution(languageDistribution)
                .topSkillsByRegion(topN(skillsByRegion, 5, RegionalStatsDto.SkillCount::new))
                .topCompaniesByRegion(topN(companiesByRegion, 5, RegionalStatsDto.NamedCount::new))
                .build();
    }

    /** Extract the region key from the job's embedded address (enum name, or null). */
    private static String regionKey(Job job) {
        if (job == null) return null;
        Address loc = job.getLocation();
        if (loc == null || loc.getRegion() == null) return null;
        return loc.getRegion().name();
    }

    /**
     * Reduce each region's name→count bucket to its top-N entries, preserving
     * descending order so the frontend can render them directly without
     * re-sorting.
     */
    private static <T> Map<String, List<T>> topN(
            Map<String, Map<String, Long>> input,
            int n,
            java.util.function.BiFunction<String, Long, T> ctor
    ) {
        Map<String, List<T>> out = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Long>> e : input.entrySet()) {
            List<T> top = e.getValue().entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                    .limit(n)
                    .map(kv -> ctor.apply(kv.getKey(), kv.getValue()))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            out.put(e.getKey(), top);
        }
        return out;
    }
}
