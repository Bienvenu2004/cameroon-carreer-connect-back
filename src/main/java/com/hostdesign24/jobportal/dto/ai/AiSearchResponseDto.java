package com.hostdesign24.jobportal.dto.ai;

import com.hostdesign24.jobportal.dto.JobPostResponseDto;

import java.util.List;

/**
 * Wire response from {@code POST /api/hjp/ai/search}.
 *
 * Shape:
 *   - {@code query}          The user's original input, echoed back so
 *                            the frontend can show "you searched for…".
 *   - {@code interpretation} Structured filter the AI extracted (every
 *                            field nullable). Frontend renders each set
 *                            value as a removable chip.
 *   - {@code jobs}           The matched listings, already mapped to the
 *                            standard {@link JobPostResponseDto} so the
 *                            frontend can drop them straight into the
 *                            existing JobCard component.
 *   - {@code confidence}     Model's self-reported confidence in the
 *                            parse, 0..1. Useful for UI cues ("low
 *                            confidence — showing keyword results").
 *   - {@code usedFallback}   {@code true} when we couldn't trust the
 *                            interpretation and fell back to plain
 *                            keyword search. The frontend shows a
 *                            banner explaining this.
 *   - {@code totalResults}   Number of jobs returned, for the header.
 */
public record AiSearchResponseDto(
        String query,
        InterpretationDto interpretation,
        List<JobPostResponseDto> jobs,
        double confidence,
        boolean usedFallback,
        int totalResults
) {}
