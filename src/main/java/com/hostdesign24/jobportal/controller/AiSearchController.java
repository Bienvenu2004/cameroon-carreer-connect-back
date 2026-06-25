package com.hostdesign24.jobportal.controller;

import com.hostdesign24.jobportal.ai.service.AiSearchService;
import com.hostdesign24.jobportal.dto.ai.AiSearchRequestDto;
import com.hostdesign24.jobportal.dto.ai.AiSearchResponseDto;
import com.hostdesign24.jobportal.dto.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public AI Semantic Job Search endpoint (§5.2 of the product spec).
 *
 * Lives in its own controller — separate from {@link AiAssistantController}
 * — because the security profile is different:
 *
 *   - {@link AiAssistantController} → JOB_SEEKER only (personalized
 *     recommendations require an authenticated user with a profile).
 *   - This controller → PUBLIC. Anonymous visitors can issue queries
 *     just like they can browse {@code /jobs} unauthenticated. The
 *     Specification underneath has a "no current user → no role
 *     predicate" branch that handles anonymous calls cleanly.
 *
 * One endpoint, one purpose. Future siblings (e.g. {@code POST
 * /api/hjp/ai/explain} for per-job explanations) belong here too.
 */
@Slf4j
@RestController
@RequestMapping("/api/hjp/ai")
@RequiredArgsConstructor
public class AiSearchController {

    private final AiSearchService aiSearchService;

    /**
     * Run a natural-language job search.
     *
     * Request:  {@code POST /api/hjp/ai/search} with body
     *           {@code { "query": "...", "size": 12 }}.
     *
     * Response: {@link AiSearchResponseDto} wrapping the interpretation
     *           (what the AI thought the query meant), the job results
     *           it produced, and a {@code usedFallback} flag so the UI
     *           can render the right banner.
     *
     * Never throws on AI failure — the service returns an empty
     * interpretation + a keyword-fallback result set, so a flaky Gemini
     * never breaks search for the user.
     */
    @PostMapping("/search")
    public ApiResponse<AiSearchResponseDto> search(@Valid @RequestBody AiSearchRequestDto request) {
        AiSearchResponseDto result = aiSearchService.search(
                request.getQuery(),
                request.getSize()
        );
        return ApiResponse.success(result, "AI search completed");
    }
}
