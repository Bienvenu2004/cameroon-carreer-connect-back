package com.hostdesign24.jobportal.ai.dto;

import com.hostdesign24.jobportal.model.enums.ExperienceLevel;
import com.hostdesign24.jobportal.model.enums.Industry;
import com.hostdesign24.jobportal.model.enums.JobLanguage;
import com.hostdesign24.jobportal.model.enums.JobSite;
import com.hostdesign24.jobportal.model.enums.JobType;
import com.hostdesign24.jobportal.model.enums.Region;

import java.util.List;

/**
 * Structured interpretation of a free-form natural-language job query
 * (e.g. "Remote junior Java developer jobs in Cameroon").
 *
 * Produced by {@code AiNlqParser} by asking Gemini to map the user's
 * conversational query onto our existing enum vocabulary
 * ({@link Region}, {@link JobType}, {@link JobSite}, {@link JobLanguage},
 * {@link Industry}, {@link ExperienceLevel}) plus a few free-text
 * fields (keywords, skills, city, salary band).
 *
 * Every enum field is nullable: the model only fills in what it could
 * confidently infer. Hallucinations are filtered out at parse time —
 * any string that doesn't match a known enum constant is dropped to
 * null. The caller can rely on these fields being safe to feed directly
 * into the database filter without further validation.
 *
 * {@code confidence} is the model's self-reported certainty (0..1).
 * The caller should compare against the configured threshold and, when
 * it falls below, fall back to plain keyword search rather than
 * trusting a noisy interpretation.
 *
 * {@code keywords} is always populated (at least with one token) so the
 * fallback path always has something to LIKE against.
 */
public record ParsedQuery(
        /** The raw user input — preserved so the API response can echo it back. */
        String originalQuery,

        /** Lowercase noun phrases for fallback LIKE/keyword matching. Always ≥ 1 item. */
        List<String> keywords,

        /** Technical or domain skills (e.g. "Spring Boot", "OHADA"). */
        List<String> skills,

        /** Resolved Cameroon region, or null if the query didn't imply one. */
        Region region,

        /** Free-text city as written by the user (or normalized). May be null. */
        String city,

        /** Contract type, or null. */
        JobType jobType,

        /** Work site preference, or null. */
        JobSite jobSite,

        /** Required working language, or null. */
        JobLanguage language,

        /** Inferred industry sector, or null. */
        Industry industry,

        /** Seniority level, or null. */
        ExperienceLevel level,

        /** Minimum salary band in XAF, or null. */
        Long salaryMin,

        /** Maximum salary band in XAF, or null. */
        Long salaryMax,

        /** Model's self-reported confidence in this parse (0..1). */
        double confidence
) {
    /**
     * True when the parse should be considered unreliable and the caller
     * should fall back to keyword-only matching. Threshold of 0.4 mirrors
     * the spec's "fallback to keyword search for queries where semantic
     * inference confidence is low".
     */
    public boolean isLowConfidence() {
        return confidence < 0.4;
    }

    /**
     * Defensive empty result. Used when the LLM is unreachable, the API
     * key is unset, or the response is unparseable — keeps the caller
     * from having to deal with nulls.
     */
    public static ParsedQuery empty(String originalQuery) {
        return new ParsedQuery(
                originalQuery,
                List.of(originalQuery == null ? "" : originalQuery.trim()),
                List.of(),
                null, null, null, null, null, null, null,
                null, null,
                0.0
        );
    }
}
