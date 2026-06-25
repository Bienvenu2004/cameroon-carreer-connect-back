package com.hostdesign24.jobportal.ai.service;

import com.hostdesign24.jobportal.ai.dto.ParsedQuery;
import com.hostdesign24.jobportal.dto.JobPostResponseDto;
import com.hostdesign24.jobportal.dto.ai.AiSearchResponseDto;
import com.hostdesign24.jobportal.dto.ai.InterpretationDto;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.jobActivityPost.JobActivityFilterDto;
import com.hostdesign24.jobportal.services.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service backing {@code POST /api/hjp/ai/search}.
 *
 * Two-stage pipeline (matches the architecture proposed in the
 * milestone plan and §5.2 of the spec):
 *
 *   1. <b>Parse</b> the natural-language query via {@link AiNlqParser},
 *      which calls Gemini in JSON mode and returns a validated
 *      {@link ParsedQuery}. On any failure path the parser returns an
 *      empty ParsedQuery (confidence 0.0) — we never crash here.
 *
 *   2. <b>Filter</b> by translating the parsed query into the existing
 *      {@link JobActivityFilterDto} machinery used by the regular Jobs
 *      catalog. No new repository plumbing — same Specification, same
 *      filter rules, just driven by the LLM instead of the user's
 *      manual form input.
 *
 * Fallback: when the parse confidence is below threshold OR when the
 * parser returned an empty interpretation, we discard the structured
 * filter and run a plain keyword LIKE on the original query. The
 * frontend gets {@code usedFallback=true} so it can show a banner
 * ("we couldn't parse your query — showing keyword results").
 *
 * The endpoint is public, so we deliberately do NOT thread a user id
 * through to the LLM — the prompt sees only the raw query, the parser
 * logs against the opaque "nlq" tag, and the JobService applies the
 * filter without any seeker/recruiter scoping (the Specification's
 * "no current user → no role predicate" branch).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSearchService {

    /** Default page size if the caller doesn't specify. */
    private static final int DEFAULT_SIZE = 12;

    /** Upper bound to keep one bad client from asking for a huge page. */
    private static final int MAX_SIZE = 50;

    private final AiNlqParser nlqParser;
    private final JobService jobService;

    public AiSearchResponseDto search(String rawQuery, Integer requestedSize) {
        int size = clampSize(requestedSize);
        String query = rawQuery == null ? "" : rawQuery.trim();

        // 1. Parse via Gemini
        ParsedQuery parsed = nlqParser.parseQuery(query);
        boolean usedFallback = parsed.isLowConfidence();

        // 2. Build the filter — semantic-driven, or pure-keyword fallback
        JobActivityFilterDto filter = usedFallback
                ? buildFallbackFilter(query, size)
                : buildSemanticFilter(parsed, size);

        // 3. Run through the same machinery the public Jobs page uses
        PageResponseDto<JobPostResponseDto> page = jobService.getAll(filter);

        log.info("[ai-search] query='{}' confidence={} fallback={} results={}",
                truncate(query), parsed.confidence(), usedFallback, page.totalElements());

        return new AiSearchResponseDto(
                query,
                toInterpretationDto(parsed, usedFallback),
                page.content(),
                parsed.confidence(),
                usedFallback,
                (int) Math.min(page.totalElements(), Integer.MAX_VALUE)
        );
    }

    /* ------------------------------------------------------------------ *
     *  filter assembly
     * ------------------------------------------------------------------ */

    /**
     * Convert a confident {@link ParsedQuery} into a filter. Every
     * non-null structured field on the parse becomes a predicate; the
     * first keyword becomes the title LIKE so the search has at least
     * one text-based anchor.
     *
     * Always sets {@code isActive=true} — public search never surfaces
     * closed listings.
     */
    private JobActivityFilterDto buildSemanticFilter(ParsedQuery p, int size) {
        JobActivityFilterDto f = baseFilter(size);

        f.setRegion(p.region());
        f.setIndustry(p.industry());
        f.setJobType(p.jobType());
        f.setJobSite(p.jobSite());
        f.setRequiredLanguage(p.language());

        if (p.city() != null) f.setCompanyCity(p.city());

        if (p.salaryMin() != null) f.setSalaryMin(BigDecimal.valueOf(p.salaryMin()));
        if (p.salaryMax() != null) f.setSalaryMax(BigDecimal.valueOf(p.salaryMax()));

        // First keyword anchors a title LIKE. Skills go into description
        // LIKE so jobs whose title is generic ("Software Engineer") but
        // whose description mentions the skill still match.
        if (!p.keywords().isEmpty()) f.setJobTitle(p.keywords().get(0));
        if (!p.skills().isEmpty()) f.setDescriptionOfJob(p.skills().get(0));

        return f;
    }

    /**
     * Pure keyword fallback: title LIKE on the original query. We don't
     * try to be clever — the model already failed to extract structure,
     * any heuristic we layer on top is more likely to hurt than help.
     */
    private JobActivityFilterDto buildFallbackFilter(String query, int size) {
        JobActivityFilterDto f = baseFilter(size);
        f.setJobTitle(query);
        return f;
    }

    private JobActivityFilterDto baseFilter(int size) {
        JobActivityFilterDto f = new JobActivityFilterDto();
        f.setIsActive(true);
        f.setSize(size);
        f.setPage(0);
        f.setSortBy("createdAt");
        f.setSortOrder(Sort.Direction.DESC);
        return f;
    }

    /* ------------------------------------------------------------------ */

    /**
     * Project the internal {@link ParsedQuery} into the wire DTO. When
     * we're in fallback mode the structured fields are noise — we hide
     * them so the UI doesn't render misleading "we understood…" chips.
     */
    private InterpretationDto toInterpretationDto(ParsedQuery p, boolean usedFallback) {
        if (usedFallback) {
            // Echo only the original query echo via keywords — every
            // structured field stays null so the frontend renders no
            // chips, just the "showing keyword results" banner.
            return new InterpretationDto(
                    List.of(p.originalQuery()), List.of(),
                    null, null, null, null, null, null, null, null, null
            );
        }
        return new InterpretationDto(
                p.keywords(),
                p.skills(),
                p.region() == null ? null : p.region().name(),
                p.city(),
                p.jobType() == null ? null : p.jobType().name(),
                p.jobSite() == null ? null : p.jobSite().name(),
                p.language() == null ? null : p.language().name(),
                p.industry() == null ? null : p.industry().name(),
                p.level() == null ? null : p.level().name(),
                p.salaryMin(),
                p.salaryMax()
        );
    }

    private static int clampSize(Integer requested) {
        if (requested == null) return DEFAULT_SIZE;
        if (requested < 1) return DEFAULT_SIZE;
        return Math.min(requested, MAX_SIZE);
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() > 120 ? s.substring(0, 120) + "…" : s;
    }
}
