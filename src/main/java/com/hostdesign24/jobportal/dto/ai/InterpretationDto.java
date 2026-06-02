package com.hostdesign24.jobportal.dto.ai;

import java.util.List;

/**
 * Wire-friendly view of what the AI thought the user's natural-language
 * query meant. Returned alongside the search results so the frontend can
 * render "we understood your query as…" chips — every chip is removable
 * to let the user refine the interpretation.
 *
 * All enum values are serialized as their NAME strings (e.g.
 * {@code "LITTORAL"}, {@code "JUNIOR"}). The frontend already has the
 * matching i18n keys ({@code regions.LITTORAL}, {@code experienceLevels.JUNIOR})
 * so it renders human-readable labels.
 *
 * Mirrors the safe-to-expose subset of {@code ParsedQuery} — internal
 * fields like the original query text and the confidence score live on
 * the parent {@code AiSearchResponseDto} so this DTO stays focused on
 * "the filter we applied".
 */
public record InterpretationDto(
        List<String> keywords,
        List<String> skills,
        String region,
        String city,
        String jobType,
        String jobSite,
        String language,
        String industry,
        String level,
        Long salaryMin,
        Long salaryMax
) {}
