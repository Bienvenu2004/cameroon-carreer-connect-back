package com.hostdesign24.jobportal.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostdesign24.jobportal.ai.dto.AiProperties;
import com.hostdesign24.jobportal.ai.client.GeminiClient;
import com.hostdesign24.jobportal.ai.client.GeminiClient.AiCallException;
import com.hostdesign24.jobportal.ai.dto.AnonymizedProfileContext;
import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.exception.ActionDeniedException;
import com.hostdesign24.jobportal.model.Job;
import com.hostdesign24.jobportal.model.JobSeekerProfile;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.enums.UserRole;
import com.hostdesign24.jobportal.repository.JobRepository;
import com.hostdesign24.jobportal.repository.JobSeekerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates AI-powered job recommendations for the currently
 * authenticated job seeker.
 *
 * Flow:
 *   1. Resolve the seeker profile (must be JOB_SEEKER role).
 *   2. Pull a candidate pool from {@code JobRepository.findAiCandidatePool}:
 *      active + approved-company + not-already-applied. Capped at
 *      {@code app.ai.recommendations.candidate-pool-size}.
 *   3. Pseudonymize the profile via {@link LLMContextBuilder}. No raw
 *      PII ever leaves this service.
 *   4. Build the prompt via {@link RecommendationPromptBuilder}.
 *   5. Call Gemini (JSON mode). Validate the response shape.
 *   6. Drop any hallucinated jobIds (model returned an id not in our pool).
 *   7. Return the top-N (configurable) recommendations to the caller,
 *      keyed back to the original {@link Job} entities so the controller
 *      can render full job details.
 *
 * Defensive behaviors:
 *   - When the Gemini API key is unconfigured (local dev), returns an
 *     empty list rather than throwing. The UI shows the empty state.
 *   - When the candidate pool is empty (new seeker, no fresh jobs in
 *     their region), returns an empty list — no LLM call wasted.
 *   - When the model returns malformed JSON, logs + returns empty list.
 *     The user sees the empty state with a "try again later" message.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiRecommendationService {

    private final JobRepository jobRepository;
    private final JobSeekerProfileRepository profileRepository;
    private final LLMContextBuilder contextBuilder;
    private final RecommendationPromptBuilder promptBuilder;
    private final GeminiClient gemini;
    @Qualifier("aiObjectMapper")
    private final ObjectMapper aiObjectMapper;
    private final AiProperties props;

    /**
     * Public DTO returned to controllers. Pairs the recommended {@link Job}
     * with the model's score + explanation. The controller transforms
     * this into the wire shape the frontend expects.
     */
    public record Recommendation(Job job, double score, String reason) {}

    /**
     * Generate recommendations for the currently-authenticated seeker.
     * Returns at most {@code returnSize} items (configurable, default 5).
     *
     * Read-only transaction — we don't persist the result here. The
     * controller layer handles persistence so we keep the service free
     * of side effects.
     */
    @Transactional(readOnly = true)
    public List<Recommendation> recommendForCurrentSeeker() {
        // 1. Resolve current user
        User user = Utils.getCurrentUser()
                .orElseThrow(() -> new ActionDeniedException("Authentication required"));
        if (user.getRole() != UserRole.JOB_SEEKER) {
            throw new ActionDeniedException("Only job seekers receive AI recommendations");
        }

        JobSeekerProfile profile = profileRepository.findByUserId(user.getId());
        if (profile == null) {
            log.debug("[ai] no seeker profile for user={}, returning empty recommendations", user.getId());
            return List.of();
        }

        // 2. Fast bail-out when the key isn't set (local dev)
        if (!props.gemini().isConfigured()) {
            log.debug("[ai] Gemini API key not configured — returning empty list");
            return List.of();
        }

        // 3. Pull the candidate pool
        List<Job> candidates = jobRepository.findAiCandidatePool(
                profile.getId(),
                PageRequest.of(0, props.recommendations().candidatePoolSize())
        );
        if (candidates.isEmpty()) {
            log.debug("[ai] candidate pool empty for profile={}", profile.getId());
            return List.of();
        }

        // 4. Pseudonymize + bail if profile is too thin to match meaningfully
        AnonymizedProfileContext ctx = contextBuilder.fromProfile(profile);
        if (!ctx.hasMatchableSignal()) {
            log.debug("[ai] profile {} has no matchable signal (no skills, no target hint)",
                    profile.getId());
            return List.of();
        }

        // 5. Build prompt + call the LLM
        String systemPrompt = promptBuilder.systemPrompt(props.recommendations().returnSize());
        String userPrompt = promptBuilder.userPrompt(ctx, candidates);
        String pseudoId = contextBuilder.pseudoIdForLogs(user.getId());

        GeminiClient.GeminiResult result;
        try {
            result = gemini.chat(systemPrompt, userPrompt, true, pseudoId);
        } catch (AiCallException e) {
            log.warn("[ai] Gemini call failed for user={}: {}", pseudoId, e.getMessage());
            return List.of();
        }

        // 6. Parse & validate the JSON output
        List<RecommendationPromptBuilder.LlmRankedItem> ranked = parse(result.rawContent());
        if (ranked.isEmpty()) return List.of();

        // 7. Resolve jobIds against the candidate pool to drop hallucinations
        Map<UUID, Job> byId = new LinkedHashMap<>();
        for (Job j : candidates) byId.put(j.getId(), j);

        List<Recommendation> recommendations = new ArrayList<>();
        for (var item : ranked) {
            Job job = byId.get(item.jobId());
            if (job == null) {
                // Hallucinated id — model returned a UUID not in our pool.
                log.warn("[ai] dropped hallucinated jobId={} user={}", item.jobId(), pseudoId);
                continue;
            }
            recommendations.add(new Recommendation(job, item.score(), item.reason()));
            if (recommendations.size() >= props.recommendations().returnSize()) break;
        }
        return recommendations;
    }

    /**
     * Parse the model's response into a list of {@link RecommendationPromptBuilder.LlmRankedItem}.
     *
     * Tolerant: handles a few formats the model might return:
     *   - `{"recommendations": [{"jobId": "...", "score": 0.9, "reason": "..."}]}` (expected)
     *   - a bare array (some models drop the wrapper despite the prompt)
     *
     * Rejects: malformed JSON, missing jobId field, non-uuid jobId, missing reason.
     * Returns an empty list on any failure — never throws.
     */
    private List<RecommendationPromptBuilder.LlmRankedItem> parse(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) return List.of();

        try {
            JsonNode root = aiObjectMapper.readTree(rawContent);

            JsonNode arr;
            if (root.isArray()) {
                arr = root;
            } else if (root.has("recommendations") && root.get("recommendations").isArray()) {
                arr = root.get("recommendations");
            } else {
                log.warn("[ai] unexpected response shape (no recommendations array): {}",
                        truncate(rawContent));
                return List.of();
            }

            List<RecommendationPromptBuilder.LlmRankedItem> out = new ArrayList<>();
            for (JsonNode item : arr) {
                if (!item.hasNonNull("jobId")) continue;
                UUID jobId;
                try {
                    jobId = UUID.fromString(item.get("jobId").asText());
                } catch (IllegalArgumentException ex) {
                    continue; // non-UUID jobId — skip
                }
                double score = item.hasNonNull("score") ? item.get("score").asDouble(0.0) : 0.0;
                String reason = item.hasNonNull("reason") ? item.get("reason").asText("") : "";
                if (reason.isBlank()) continue; // empty reasons are useless to the user
                out.add(new RecommendationPromptBuilder.LlmRankedItem(jobId, clamp01(score), reason));
            }
            return out;
        } catch (Exception e) {
            log.warn("[ai] response parse failed: {} — body snippet: {}",
                    e.getMessage(), truncate(rawContent));
            return List.of();
        }
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    private static String truncate(String s) {
        if (s == null) return "(null)";
        return s.length() > 200 ? s.substring(0, 200) + "…" : s;
    }
}
