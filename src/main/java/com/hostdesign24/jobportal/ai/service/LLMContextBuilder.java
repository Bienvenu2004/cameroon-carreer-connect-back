package com.hostdesign24.jobportal.ai.service;

import com.hostdesign24.jobportal.ai.dto.AnonymizedProfileContext;
import com.hostdesign24.jobportal.model.JobSeekerProfile;
import com.hostdesign24.jobportal.model.Skill;
import com.hostdesign24.jobportal.model.WorkExperience;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * The ONLY path from a {@code JobSeekerProfile} entity to anything an LLM
 * call ever sees. Strips identifying data and returns a flat
 * {@link AnonymizedProfileContext}.
 *
 * Audit-grade. Don't bypass this. If you find yourself reaching for a
 * profile field that isn't exposed here, the right answer is to think
 * carefully about whether it's safe to send to the provider, then add it
 * explicitly to this builder + AnonymizedProfileContext — not to call
 * the LLM with a different shape.
 */
@Component
public class LLMContextBuilder {

    /**
     * Build a safe profile context for prompts. Tolerates partial profiles —
     * empty / null fields are passed through as null/empty collections.
     */
    public AnonymizedProfileContext fromProfile(JobSeekerProfile profile) {
        if (profile == null) {
            // Empty context — service layer will short-circuit before
            // calling the LLM with this.
            return new AnonymizedProfileContext(
                    "Candidate", List.of(), List.of(),
                    null, null, null, null,
                    null, null, null, null, null
            );
        }

        List<String> skills = profile.getSkills() == null ? List.of()
                : profile.getSkills().stream()
                    .map(Skill::getName)
                    .filter(n -> n != null && !n.isBlank())
                    .distinct()
                    .toList();

        List<String> langs = profile.getSpokenLanguages() == null ? List.of()
                : Arrays.stream(profile.getSpokenLanguages().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();

        return new AnonymizedProfileContext(
                "Candidate",
                skills,
                langs,
                deriveYearsOfExperience(profile),       // null until we ship WorkExperience
                profile.getEmploymentType(),
                profile.getWorkAuthorization(),
                profile.getAddress() == null ? null : profile.getAddress().getRegion(),
                null,                                   // salary band — comes from JobPreferences (future)
                null,
                null,
                null,                                   // preferredLanguage — comes from JobPreferences (future)
                deriveTargetRoleHint(profile)
        );
    }

    /**
     * Sum of (endDate − startDate) across every WorkExperience row, in
     * whole years. Ongoing roles (isCurrent=true, endDate=null) count
     * up to today.
     *
     * Notes:
     *   - Overlapping ranges are NOT de-duplicated. A seeker who
     *     freelanced while employed full-time genuinely accumulated
     *     experience on both fronts; both should count. If this ever
     *     produces "30 years experience" outliers for early-career
     *     candidates with parallel roles, switch to interval-merge.
     *   - Invalid rows (start > end, missing start) are skipped, not
     *     thrown — the service-layer save already validates. Defensive.
     *   - Returns null when there are zero rows so the LLM prompt shows
     *     "experience unknown" rather than "0 years of experience"
     *     (which signals "fresh grad" — different downstream interpretation).
     */
    private Integer deriveYearsOfExperience(JobSeekerProfile profile) {
        List<WorkExperience> xps = profile.getExperiences();
        if (xps == null || xps.isEmpty()) return null;

        LocalDate today = LocalDate.now();
        long totalDays = 0;
        boolean anyValid = false;

        for (WorkExperience xp : xps) {
            if (xp == null || xp.isDeleted()) continue;
            LocalDate start = xp.getStartDate();
            if (start == null) continue;

            LocalDate end = xp.isCurrent() || xp.getEndDate() == null
                    ? today
                    : xp.getEndDate();
            if (end.isBefore(start)) continue; // malformed row — ignore

            totalDays += ChronoUnit.DAYS.between(start, end);
            anyValid = true;
        }

        if (!anyValid) return null;
        // Floor-divide by 365 so "1 year and 6 months" surfaces as 1
        // rather than rounding up to 2 — matches how seekers describe
        // themselves and avoids over-promising seniority.
        return (int) (totalDays / 365);
    }


    /**
     * "What kind of role does this person want?" — a free-text hint the
     * LLM can use to bias ranking. Until we have a target-roles array on
     * a JobPreferences entity, return null and let the model rely purely
     * on skills + languages.
     */
    private String deriveTargetRoleHint(JobSeekerProfile profile) {
        return null;
    }

    /**
     * Short, deterministic, opaque identifier safe to log alongside
     * cost / latency metrics. NOT cryptographically reversible by an
     * attacker who doesn't have the raw user id (SHA-256, truncated).
     *
     * Use this — not the user UUID — anywhere logs or telemetry leave
     * the application boundary.
     */
    public String pseudoIdForLogs(UUID userId) {
        if (userId == null) return "anon";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(userId.toString().getBytes());
            return HexFormat.of().formatHex(digest).substring(0, 10);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed by the JDK; this branch is unreachable.
            return "anon";
        }
    }
}
