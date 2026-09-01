package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.dto.analytics.DashboardDto;
import com.hostdesign24.jobportal.dto.analytics.JobStatsDto;
import com.hostdesign24.jobportal.dto.analytics.RegionalStatsDto;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.enums.ApplicationStatus;
import com.hostdesign24.jobportal.model.enums.JobLanguage;
import com.hostdesign24.jobportal.model.enums.Region;
import com.hostdesign24.jobportal.model.enums.UserRole;
import com.hostdesign24.jobportal.repository.AnalyticsRepository;
import com.hostdesign24.jobportal.repository.JobRepository;
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
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Aggregates dashboard analytics scoped to the caller's role:
 *   - SYSTEM_ADMIN: platform-wide stats
 *   - RECRUITER: only stats for jobs they themselves created (createdBy = current user id)
 *   - JOB_SEEKER: stats over their own applications (basic)
 *
 * All counting is delegated to grouped SQL in {@link AnalyticsRepository}. The
 * service's job is to pick the right scope and reshape the resulting tuples into
 * the DTOs; it never loads a table to count it.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int TOP_N = 5;

    private final JobRepository jobRepository;
    private final AnalyticsRepository analyticsRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardDto getDashboardStats(UUID userId) {
        Optional<User> currentUser = Utils.getCurrentUser();
        UserRole role = currentUser.map(User::getRole).orElse(null);
        UUID actorId = currentUser.map(User::getId).orElse(userId);

        boolean recruiterScope = role == UserRole.RECRUITER && actorId != null;
        boolean seekerScope = role == UserRole.JOB_SEEKER && actorId != null;

        // ---- per-job breakdown -------------------------------------------
        // One grouped query replaces findAll() plus a countByJobId call per job.
        List<Object[]> jobRows = recruiterScope
                ? analyticsRepository.jobStatsByCreator(actorId)
                : analyticsRepository.jobStatsAll();

        List<JobStatsDto> jobStatsList = new ArrayList<>(jobRows.size());
        long totalViews = 0;
        long totalApplications = 0;

        for (Object[] row : jobRows) {
            long views = ((Number) row[2]).longValue();
            long apps = ((Number) row[3]).longValue();
            totalViews += views;
            totalApplications += apps;

            JobStatsDto js = new JobStatsDto();
            js.setJobId((UUID) row[0]);
            js.setJobTitle((String) row[1]);
            js.setViews((int) views);
            js.setApplicationsCount(apps);
            jobStatsList.add(js);
        }

        long totalJobs = recruiterScope
                ? jobRows.size()
                : jobRepository.countNotDeleted();
        long totalActiveJobs = recruiterScope
                ? jobRepository.countActiveJobsByCreator(actorId)
                : jobRepository.countActiveJobs();

        // ---- application-level aggregates ---------------------------------
        LocalDate cutoff6m = LocalDate.now().minusMonths(6);

        List<Object[]> statusRows;
        List<Object[]> demographicRows;
        List<Object[]> monthRows;

        if (seekerScope) {
            statusRows = analyticsRepository.appStatusBySeeker(actorId);
            demographicRows = analyticsRepository.appDemographicsBySeeker(actorId);
            monthRows = analyticsRepository.appsByDayBySeeker(actorId, cutoff6m);
        } else if (recruiterScope) {
            statusRows = analyticsRepository.appStatusByJobCreator(actorId);
            demographicRows = analyticsRepository.appDemographicsByJobCreator(actorId);
            monthRows = analyticsRepository.appsByDayByJobCreator(actorId, cutoff6m);
        } else {
            statusRows = analyticsRepository.appStatusAll();
            demographicRows = analyticsRepository.appDemographicsAll();
            monthRows = analyticsRepository.appsByDayAll(cutoff6m);
        }

        Map<String, Long> appsByStatus = new HashMap<>();
        for (ApplicationStatus s : ApplicationStatus.values()) appsByStatus.put(s.name(), 0L);
        for (Object[] row : statusRows) {
            ApplicationStatus st = row[0] == null ? ApplicationStatus.APPLIED : (ApplicationStatus) row[0];
            appsByStatus.merge(st.name(), count(row[1]), Long::sum);
        }

        Map<String, Long> demographics = new HashMap<>();
        for (Object[] row : demographicRows) {
            String city = row[0] == null ? "Unknown" : (String) row[0];
            String country = row[1] == null ? "Unknown" : (String) row[1];
            demographics.merge(city + ", " + country, count(row[2]), Long::sum);
        }

        Map<String, Long> appsByMonth = new TreeMap<>();
        for (Object[] row : monthRows) {
            LocalDate day = (LocalDate) row[0];
            if (day == null) continue;
            appsByMonth.merge(day.format(MONTH_FMT), count(row[1]), Long::sum);
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

    /* =========================================================================
     *  Regional analytics — powers the admin Regional Trending dashboard
     * ======================================================================= */

    @Override
    @Transactional(readOnly = true)
    public RegionalStatsDto getRegionalStats() {
        Map<String, Long> jobsByRegion = new HashMap<>();
        for (Object[] row : analyticsRepository.activeJobsByRegion()) {
            jobsByRegion.merge(((Region) row[0]).name(), count(row[1]), Long::sum);
        }

        Map<String, Long> applicationsByRegion = new HashMap<>();
        for (Object[] row : analyticsRepository.applicationsByRegion()) {
            applicationsByRegion.merge(((Region) row[0]).name(), count(row[1]), Long::sum);
        }

        Map<String, Map<String, Long>> companiesByRegion = new HashMap<>();
        for (Object[] row : analyticsRepository.activeJobsByRegionAndCompany()) {
            companiesByRegion
                    .computeIfAbsent(((Region) row[0]).name(), k -> new HashMap<>())
                    .merge((String) row[1], count(row[2]), Long::sum);
        }

        Map<String, Map<String, Long>> skillsByRegion = new HashMap<>();
        for (Object[] row : analyticsRepository.applicantSkillsByRegion()) {
            skillsByRegion
                    .computeIfAbsent(((Region) row[0]).name(), k -> new HashMap<>())
                    .merge((String) row[1], count(row[2]), Long::sum);
        }

        // Initialize every language bucket so the frontend always has every bar.
        Map<String, Long> languageDistribution = new LinkedHashMap<>();
        for (JobLanguage lang : JobLanguage.values()) {
            languageDistribution.put(lang.name(), 0L);
        }
        for (Object[] row : analyticsRepository.activeJobsByLanguage()) {
            languageDistribution.merge(((JobLanguage) row[0]).name(), count(row[1]), Long::sum);
        }

        return RegionalStatsDto.builder()
                .jobsByRegion(jobsByRegion)
                .applicationsByRegion(applicationsByRegion)
                .languageDistribution(languageDistribution)
                .topSkillsByRegion(topN(skillsByRegion, TOP_N, RegionalStatsDto.SkillCount::new))
                .topCompaniesByRegion(topN(companiesByRegion, TOP_N, RegionalStatsDto.NamedCount::new))
                .build();
    }

    /** COUNT() comes back as Long on most dialects, but read it defensively. */
    private static long count(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    /**
     * Reduce each region's name→count bucket to its top-N entries, preserving
     * descending order so the frontend can render them directly without
     * re-sorting.
     */
    private static <T> Map<String, List<T>> topN(
            Map<String, Map<String, Long>> input,
            int n,
            BiFunction<String, Long, T> ctor
    ) {
        Map<String, List<T>> out = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Long>> e : input.entrySet()) {
            List<T> top = e.getValue().entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                    .limit(n)
                    .map(kv -> ctor.apply(kv.getKey(), kv.getValue()))
                    .collect(Collectors.toCollection(ArrayList::new));
            out.put(e.getKey(), top);
        }
        return out;
    }
}
