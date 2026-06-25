package com.hostdesign24.jobportal.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * Aggregated employment-market analytics broken down by Cameroonian
 * administrative region. Powers the "Regional trending" dashboard
 * available to administrators (and useful for public-facing trend pages
 * later on).
 *
 * All maps are keyed by the region enum name (e.g. "CENTRE", "LITTORAL")
 * so the frontend can localize the label via {@code regions.{REGION}}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegionalStatsDto {

    /** Total active jobs per region. */
    private Map<String, Long> jobsByRegion;

    /** Total applications received per region. */
    private Map<String, Long> applicationsByRegion;

    /** Distribution of working languages across active jobs, platform-wide. */
    private Map<String, Long> languageDistribution;

    /** Top in-demand skills (by frequency of mention on active jobs) per region. */
    private Map<String, List<SkillCount>> topSkillsByRegion;

    /** Top hiring companies (by active-job count) per region. */
    private Map<String, List<NamedCount>> topCompaniesByRegion;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SkillCount {
        private String name;
        private long count;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NamedCount {
        private String name;
        private long count;
    }
}
