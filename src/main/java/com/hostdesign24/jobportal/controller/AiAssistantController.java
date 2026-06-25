package com.hostdesign24.jobportal.controller;

import com.hostdesign24.jobportal.ai.dto.AiProperties;
import com.hostdesign24.jobportal.ai.service.AiRecommendationService;
// AiRecommendationService.Recommendation (record) and model.Recommendation (entity) share a simple name.
// We reference both via fully-qualified names in this file to avoid the import clash.
import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.dto.JobPostResponseDto;
import com.hostdesign24.jobportal.dto.ai.RecommendationDto;
import com.hostdesign24.jobportal.dto.common.ApiResponse;
import com.hostdesign24.jobportal.exception.ActionDeniedException;
import com.hostdesign24.jobportal.mapper.FileMapper;
import com.hostdesign24.jobportal.mapper.JobResponseMapper;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Public surface for the AI assistant features. Today this is just the
 * "Recommendations for you" endpoint — the seeker dashboard polls it and
 * renders the ranked job cards.
 *
 * Auth model:
 *   - Class-level @PreAuthorize("hasRole('JOB_SEEKER')") locks every
 *     endpoint to the seeker role. Recruiters / admins don't have a
 *     personalized recommendation surface.
 *
 * Side effects:
 *   - For each shown recommendation we persist a {@link Recommendation}
 *     row. We don't query that row in v1 — but having the data from
 *     day 1 means we can build "history", "feedback metrics", and
 *     learning-to-rank training sets later without backfilling.
 */
@Slf4j
@RestController
@RequestMapping("/api/hjp/ai")
@RequiredArgsConstructor
@PreAuthorize("hasRole('JOB_SEEKER')")
public class AiAssistantController {

    private final AiRecommendationService recommendationService;
    private final RecommendationRepository recommendationRepository;
    private final JobResponseMapper jobResponseMapper;
    private final FileMapper fileMapper;
    private final AiProperties aiProperties;



    /**
     * Returns the top-N AI-ranked active jobs for the current seeker.
     *
     * The service returns an empty list cleanly when:
     *   - The Gemini API key isn't configured (local dev)
     *   - The seeker's profile has no matchable signal (no skills yet)
     *   - There are no active+approved+not-applied candidates
     *   - The model errors or returns malformed JSON
     *
     * In all those cases the frontend shows an empty state — never an error.
     */
    @GetMapping("/recommendations")
    @Transactional
    public ApiResponse<List<RecommendationDto>> recommendations() {
        User currentUser = Utils.getCurrentUser()
                .orElseThrow(() -> new ActionDeniedException("Authentication required"));

        List<AiRecommendationService.Recommendation> recs =
                recommendationService.recommendForCurrentSeeker();

        if (recs.isEmpty()) {
            return ApiResponse.success(List.of(), "No recommendations available yet");
        }

        String modelVersion = aiProperties.gemini().model() + "/v1";
        LocalDateTime now = LocalDateTime.now();

        List<RecommendationDto> out = new ArrayList<>(recs.size());
        for (var r : recs) {
            // Persist for telemetry / future LTR.
            var entity = new com.hostdesign24.jobportal.model.Recommendation(
                    currentUser,
                    r.job(),
                    r.score(),
                    r.reason(),
                    modelVersion
            );
            entity = recommendationRepository.save(entity);

            // Build the wire DTO. Mirror buildResponse() from JobServiceImpl
            // so the job's company logo URL is publicly resolvable.
            JobPostResponseDto jobDto = jobResponseMapper.toResponse(r.job());
            if (r.job().getCompany() != null
                    && r.job().getCompany().getLogo() != null
                    && jobDto.getCompany() != null) {
                jobDto.getCompany().setLogo(
                        fileMapper.toDto(r.job().getCompany().getLogo())
                );
            }

            out.add(new RecommendationDto(
                    entity.getId(),
                    jobDto,
                    r.score(),
                    r.reason(),
                    now
            ));
        }

        return ApiResponse.success(out, "Recommendations retrieved");
    }
}
