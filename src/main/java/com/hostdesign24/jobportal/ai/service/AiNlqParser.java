package com.hostdesign24.jobportal.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostdesign24.jobportal.ai.client.GeminiClient;
import com.hostdesign24.jobportal.ai.client.GeminiClient.AiCallException;
import com.hostdesign24.jobportal.ai.dto.AiProperties;
import com.hostdesign24.jobportal.ai.dto.ParsedQuery;
import com.hostdesign24.jobportal.model.enums.ExperienceLevel;
import com.hostdesign24.jobportal.model.enums.Industry;
import com.hostdesign24.jobportal.model.enums.JobLanguage;
import com.hostdesign24.jobportal.model.enums.JobSite;
import com.hostdesign24.jobportal.model.enums.JobType;
import com.hostdesign24.jobportal.model.enums.Region;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a free-form natural-language job query (in EN or FR) into a
 * validated {@link ParsedQuery} that can be applied directly against
 * the database. Powers the AI Semantic Search endpoint (§5.2 of the
 * product spec).
 *
 * Pipeline:
 *   1. Short-circuit cleanly when the Gemini key is unset (local dev) or
 *      the query is blank — return {@link ParsedQuery#empty} with
 *      confidence 0.0 so the caller falls back to keyword search.
 *   2. Build the system + user prompts via {@link NlqParserPromptBuilder}.
 *   3. Call Gemini with JSON mode on.
 *   4. Parse the response tolerantly — accept the documented shape or a
 *      bare-object variant the model occasionally returns despite the
 *      schema.
 *   5. Coerce every enum field: any value that doesn't match a known
 *      constant is dropped to null (hallucination guard). City strings
 *      pass through verbatim because we don't have a Cameroonian-city
 *      enum to validate against.
 *
 * Never throws. On any failure path it returns an empty ParsedQuery and
 * logs at WARN. The caller decides what to do (fallback to keyword search
 * is the obvious move).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiNlqParser {

    private static final String LOG_PREFIX = "[ai-nlq]";

    private final GeminiClient gemini;
    private final NlqParserPromptBuilder promptBuilder;
    private final AiProperties props;

    @Qualifier("aiObjectMapper")
    private final ObjectMapper mapper;

    /**
     * Parse a user query. Returns an empty (confidence=0) ParsedQuery on
     * any failure so callers can rely on the result being non-null.
     */
    public ParsedQuery parseQuery(String query) {
        if (query == null || query.isBlank()) {
            return ParsedQuery.empty("");
        }

        if (!props.gemini().isConfigured()) {
            log.debug("{} Gemini key not configured — returning empty parse for fallback", LOG_PREFIX);
            return ParsedQuery.empty(query);
        }

        String trimmed = query.trim();
        String systemPrompt = promptBuilder.systemPrompt();
        String userPrompt = promptBuilder.userPrompt(trimmed);

        // userIdHash is "anon" here on purpose — we don't have a user
        // identity at NLQ time (public search), and even if we did we
        // wouldn't want to send it: the query itself is sensitive enough.
        GeminiClient.GeminiResult result;
        try {
            result = gemini.chat(systemPrompt, userPrompt, true, "nlq");
        } catch (AiCallException e) {
            log.warn("{} Gemini call failed: {}", LOG_PREFIX, e.getMessage());
            return ParsedQuery.empty(trimmed);
        }

        return parseJson(trimmed, result.rawContent());
    }

    /* ------------------------------------------------------------------ *
     *  JSON → ParsedQuery
     * ------------------------------------------------------------------ */

    private ParsedQuery parseJson(String originalQuery, String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            log.warn("{} empty content from model", LOG_PREFIX);
            return ParsedQuery.empty(originalQuery);
        }

        JsonNode root;
        try {
            root = mapper.readTree(rawContent);
        } catch (Exception e) {
            log.warn("{} could not parse JSON: {} — snippet: {}",
                    LOG_PREFIX, e.getMessage(), truncate(rawContent));
            return ParsedQuery.empty(originalQuery);
        }

        if (root == null || !root.isObject()) {
            log.warn("{} expected JSON object, got: {}", LOG_PREFIX, truncate(rawContent));
            return ParsedQuery.empty(originalQuery);
        }

        List<String> keywords = readStringArray(root, "keywords");
        if (keywords.isEmpty()) {
            // Guarantee at least one keyword for the fallback LIKE.
            keywords = List.of(originalQuery);
        }

        return new ParsedQuery(
                originalQuery,
                keywords,
                readStringArray(root, "skills"),
                readEnum(root, "region", Region.class),
                readNullableString(root, "city"),
                readEnum(root, "jobType", JobType.class),
                readEnum(root, "jobSite", JobSite.class),
                readEnum(root, "language", JobLanguage.class),
                readEnum(root, "industry", Industry.class),
                readEnum(root, "level", ExperienceLevel.class),
                readNullableLong(root, "salaryMin"),
                readNullableLong(root, "salaryMax"),
                clamp01(root.path("confidence").asDouble(0.0))
        );
    }

    /* ------------------------------------------------------------------ *
     *  Coercion helpers — every "read" method tolerates absence, wrong
     *  type, or hallucinated enum values by returning a safe default.
     * ------------------------------------------------------------------ */

    private static <E extends Enum<E>> E readEnum(JsonNode root, String field, Class<E> clazz) {
        JsonNode v = root.get(field);
        if (v == null || v.isNull()) return null;
        String raw = v.asText("").trim();
        if (raw.isEmpty()) return null;
        try {
            return Enum.valueOf(clazz, raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            // Hallucinated value — drop quietly. Logged at DEBUG so noisy
            // models don't spam the production log aggregator.
            log.debug("{} dropped hallucinated {}={}", LOG_PREFIX, field, raw);
            return null;
        }
    }

    private static List<String> readStringArray(JsonNode root, String field) {
        JsonNode arr = root.get(field);
        if (arr == null || !arr.isArray()) return List.of();
        List<String> out = new ArrayList<>(arr.size());
        for (JsonNode item : arr) {
            if (item == null || item.isNull()) continue;
            String s = item.asText("").trim();
            if (!s.isEmpty()) out.add(s);
        }
        return List.copyOf(out);
    }

    private static String readNullableString(JsonNode root, String field) {
        JsonNode v = root.get(field);
        if (v == null || v.isNull()) return null;
        String s = v.asText("").trim();
        return s.isEmpty() ? null : s;
    }

    private static Long readNullableLong(JsonNode root, String field) {
        JsonNode v = root.get(field);
        if (v == null || v.isNull() || !v.canConvertToLong()) return null;
        long val = v.asLong();
        return val < 0 ? null : val;
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v) || v < 0.0) return 0.0;
        return Math.min(v, 1.0);
    }

    private static String truncate(String s) {
        if (s == null) return "(null)";
        return s.length() > 200 ? s.substring(0, 200) + "…" : s;
    }
}
