package com.hostdesign24.jobportal.ai.dto;

import com.hostdesign24.jobportal.model.enums.JobLanguage;
import com.hostdesign24.jobportal.model.enums.Region;

import java.math.BigDecimal;
import java.util.List;

/**
 * The strict subset of a job seeker's profile that is safe to send to a
 * third-party LLM. This is the ONLY shape the AI module ever sees of a
 * profile — there is no "raw" path.
 *
 * What's deliberately NOT here:
 *   - email, phone, full address, real first/last name
 *   - resume URL, profile photo URL, social URLs (GitHub etc.)
 *   - any free-text "about me" or notes the user may have entered
 *
 * What IS here, and why:
 *   - skills            → essential signal for matching
 *   - employmentType    → preference signal (full-time vs internship)
 *   - workAuthorization → eligibility signal (e.g. "Cameroonian national")
 *   - region            → location preference
 *   - salaryMin/Max     → expectation alignment
 *   - languages         → critical Cameroon filter (FR/EN/Bilingual)
 *   - yearsExperience   → seniority signal (count of work-history rows
 *                          OR derived from earliest work-history start date)
 *
 * The {@code pseudonym} is always the literal string "Candidate" — we
 * never personalize prompts with anything that could leak identity.
 */
public record AnonymizedProfileContext(
        String pseudonym,
        List<String> skills,
        List<String> spokenLanguages,
        Integer yearsOfExperience,
        String employmentType,
        String workAuthorization,
        Region preferredRegion,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String salaryCurrency,
        JobLanguage preferredLanguage,
        /**
         * Soft signal — what kind of role the seeker is targeting. Free
         * text because we don't have a structured target-role field yet.
         * Derived from spec-section {@code careerGoals} once we add
         * JobPreferences; for now pulls from the most recent work-history
         * title if available.
         */
        String targetRoleHint
) {

    /** True when the profile has at least skills OR a target hint — i.e. the LLM has something to work with. */
    public boolean hasMatchableSignal() {
        return (skills != null && !skills.isEmpty())
                || (targetRoleHint != null && !targetRoleHint.isBlank());
    }
}
