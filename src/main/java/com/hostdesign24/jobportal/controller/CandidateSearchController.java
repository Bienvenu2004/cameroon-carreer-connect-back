package com.hostdesign24.jobportal.controller;

import com.hostdesign24.jobportal.dto.candidate.CandidateSearchFilterDto;
import com.hostdesign24.jobportal.dto.candidate.CandidateSummaryDto;
import com.hostdesign24.jobportal.dto.candidate.InvitationDto;
import com.hostdesign24.jobportal.dto.candidate.InviteCandidateDto;
import com.hostdesign24.jobportal.dto.common.ApiResponse;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.services.CandidateSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Candidate search, invitations, and the seeker's own view of both.
 *
 * The platform used to be one-directional: seekers applied, recruiters reacted,
 * and there was no route to anyone who had not already applied to you. That is
 * a job board. Being able to go and find a person is what makes it a recruitment
 * platform.
 *
 * Every search runs against profiles whose owners opted in, and that rule lives
 * in the specification rather than here, so no endpoint can accidentally
 * sidestep it.
 */
@RestController
@RequestMapping("/api/hjp/candidates")
@RequiredArgsConstructor
@Tag(name = "Candidates", description = "Recruiter-facing candidate search and invitations to apply")
public class CandidateSearchController {

    private final CandidateSearchService candidateSearchService;

    @GetMapping
    @PreAuthorize("hasAnyRole('RECRUITER','SYSTEM_ADMIN')")
    @Operation(
            summary = "Search the candidate pool",
            description = "Only returns profiles whose owners have opted in to being found by employers."
    )
    public ApiResponse<PageResponseDto<CandidateSummaryDto>> search(
            @ModelAttribute CandidateSearchFilterDto filter) {
        return ApiResponse.success(candidateSearchService.search(filter),
                "Candidates retrieved successfully");
    }

    @PostMapping("/invite")
    @PreAuthorize("hasAnyRole('RECRUITER','SYSTEM_ADMIN')")
    @Operation(
            summary = "Invite a candidate to apply",
            description = "Sends an in-app invitation for one of the recruiter's own job postings."
    )
    public ApiResponse<Boolean> invite(@Valid @RequestBody InviteCandidateDto request) {
        boolean sent = candidateSearchService.invite(request);
        return ApiResponse.success(sent,
                sent ? "Invitation sent" : "This candidate has already been invited to that job");
    }

    /* ------------------------------------------------------------------
     * Seeker-facing: the other end of the same feature.
     * ------------------------------------------------------------------ */

    @GetMapping("/invitations/me")
    @PreAuthorize("hasRole('JOB_SEEKER')")
    @Operation(summary = "Invitations I have received")
    public ApiResponse<PageResponseDto<InvitationDto>> myInvitations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(candidateSearchService.myInvitations(page, size),
                "Invitations retrieved successfully");
    }

    @GetMapping("/invitations/me/pending-count")
    @PreAuthorize("hasRole('JOB_SEEKER')")
    @Operation(summary = "How many invitations I have not acted on")
    public ApiResponse<Long> myPendingInvitationCount() {
        return ApiResponse.success(candidateSearchService.myPendingInvitationCount(),
                "Pending invitation count retrieved successfully");
    }
}
