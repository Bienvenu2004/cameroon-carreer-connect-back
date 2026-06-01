package com.hostdesign24.jobportal.ai.service;

import com.hostdesign24.jobportal.ai.dto.AnonymizedProfileContext;
import com.hostdesign24.jobportal.model.Job;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds the system + user prompts sent to Gemini for job ranking.
 *
 * The system prompt is fixed and instructs the model to:
 *   - rank STRICTLY from the supplied candidate set (no invented jobs)
 *   - return JSON only (responseMimeType=application/json is also
 *     requested at the API level — belt and braces)
 *   - keep explanations concrete (cite skills, years, location)
 *
 * The user prompt embeds:
 *   - the anonymized candidate profile
 *   - the candidate jobs as a numbered list with stable {@code id}s
 *
 * Job description text is truncated to keep the prompt cheap. 600 chars
 * per job × 50 jobs = 30k chars ≈ ~8k tokens — well within Gemini's
 * 1M context window and very cheap on gemini-2.5-flash.
 */
@Component
public class RecommendationPromptBuilder {

    private static final int JOB_DESC_MAX_CHARS = 600;

    /**
     * Returns the system message used for every recommendation call.
     * Intentionally simple — too much instruction backfires with smaller
     * models. Keep this tight.
     */
    public String systemPrompt(int returnSize) {
        return """
                You are a job-matching assistant for a Cameroonian recruitment platform.
                A candidate profile and a list of active job postings are provided. Your
                task is to pick the %d jobs that best fit the candidate.

                Rules:
                  - Pick jobs ONLY from the supplied list. Never invent a jobId.
                  - Order from best to worst fit.
                  - For each pick, give a SHORT 1-sentence explanation that names a
                    concrete reason: a specific skill, language, region, or salary
                    alignment. Avoid vague phrases like "good match for your profile".
                  - Reply in the same language the candidate profile uses (default
                    French for Cameroon). Keep the JSON keys in English regardless.
                  - If fewer than %d jobs are a reasonable fit, return only the ones
                    that genuinely match. Returning 2 strong matches beats 5 weak ones.

                Output STRICTLY this JSON shape (no markdown, no preamble):
                {
                  "recommendations": [
                    { "jobId": "<uuid from the input>", "score": 0.92, "reason": "..." }
                  ]
                }
                The score is a float between 0 and 1 representing your confidence in
                the match.
                """.formatted(returnSize, returnSize);
    }

    /** Render the per-call user prompt with the candidate + the job pool. */
    public String userPrompt(AnonymizedProfileContext profile, List<Job> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("CANDIDATE PROFILE\n");
        sb.append("-----------------\n");
        sb.append("Skills: ").append(commaList(profile.skills())).append("\n");
        sb.append("Spoken languages: ").append(commaList(profile.spokenLanguages())).append("\n");
        if (profile.yearsOfExperience() != null) {
            sb.append("Years of experience: ").append(profile.yearsOfExperience()).append("\n");
        }
        if (profile.employmentType() != null && !profile.employmentType().isBlank()) {
            sb.append("Preferred employment type: ").append(profile.employmentType()).append("\n");
        }
        if (profile.workAuthorization() != null && !profile.workAuthorization().isBlank()) {
            sb.append("Work authorization: ").append(profile.workAuthorization()).append("\n");
        }
        if (profile.preferredRegion() != null) {
            sb.append("Preferred region: ").append(profile.preferredRegion().name()).append("\n");
        }
        if (profile.preferredLanguage() != null) {
            sb.append("Preferred working language: ").append(profile.preferredLanguage().name()).append("\n");
        }
        if (profile.salaryMin() != null || profile.salaryMax() != null) {
            sb.append("Salary expectation: ")
              .append(profile.salaryMin() == null ? "?" : profile.salaryMin())
              .append(" - ")
              .append(profile.salaryMax() == null ? "?" : profile.salaryMax())
              .append(" ").append(profile.salaryCurrency() == null ? "XAF" : profile.salaryCurrency())
              .append("\n");
        }
        if (profile.targetRoleHint() != null && !profile.targetRoleHint().isBlank()) {
            sb.append("Target role: ").append(profile.targetRoleHint()).append("\n");
        }

        sb.append("\nCANDIDATE JOBS (").append(candidates.size()).append(")\n");
        sb.append("-----------------\n");
        for (Job j : candidates) {
            sb.append("- jobId: ").append(j.getId()).append("\n");
            sb.append("  title: ").append(safe(j.getTitle())).append("\n");
            if (j.getCompany() != null) {
                sb.append("  company: ").append(safe(j.getCompany().getName())).append("\n");
                if (j.getCompany().getIndustry() != null) {
                    sb.append("  industry: ").append(j.getCompany().getIndustry().name()).append("\n");
                }
            }
            if (j.getLocation() != null) {
                String loc = String.join(", ",
                        safeOrEmpty(j.getLocation().getCity()),
                        j.getLocation().getRegion() == null ? "" : j.getLocation().getRegion().name(),
                        safeOrEmpty(j.getLocation().getCountry()));
                sb.append("  location: ").append(loc).append("\n");
            }
            if (j.getType() != null) {
                sb.append("  type: ").append(j.getType().name()).append("\n");
            }
            if (j.getSite() != null) {
                sb.append("  site: ").append(j.getSite().name()).append("\n");
            }
            if (j.getRequiredLanguage() != null) {
                sb.append("  language: ").append(j.getRequiredLanguage().name()).append("\n");
            }
            if (j.getSalary() != null) {
                sb.append("  salary: ").append(j.getSalary())
                  .append(" ").append(j.getSalaryCurrency() == null ? "XAF" : j.getSalaryCurrency().name())
                  .append("\n");
            }
            if (j.getDescription() != null && !j.getDescription().isBlank()) {
                String desc = j.getDescription().length() > JOB_DESC_MAX_CHARS
                        ? j.getDescription().substring(0, JOB_DESC_MAX_CHARS) + "…"
                        : j.getDescription();
                sb.append("  description: ").append(desc.replace("\n", " ")).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private static String commaList(List<String> items) {
        if (items == null || items.isEmpty()) return "(none provided)";
        return items.stream().filter(s -> s != null && !s.isBlank()).collect(Collectors.joining(", "));
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static String safeOrEmpty(String s) { return s == null ? "" : s; }

    /** Parsed item from the model's response. */
    public record LlmRankedItem(UUID jobId, double score, String reason) {}
}
