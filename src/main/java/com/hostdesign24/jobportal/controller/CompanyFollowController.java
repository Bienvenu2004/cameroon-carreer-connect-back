package com.hostdesign24.jobportal.controller;

import com.hostdesign24.jobportal.dto.common.ApiResponse;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.company.FollowedCompanyDto;
import com.hostdesign24.jobportal.services.CompanyFollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Following employers.
 *
 * "Tell me when this employer is hiring" is the standing request a job seeker
 * most wants to make, and until now the only way to answer it was to check the
 * listings by hand.
 *
 * Following is a seeker action: recruiters and admins have no use for it, and
 * the service resolves the seeker's own profile rather than taking an id, so
 * there is no way to follow on someone else's behalf.
 */
@RestController
@RequestMapping("/api/hjp/companies")
@RequiredArgsConstructor
@Tag(name = "Company follows", description = "Job seekers following employers")
public class CompanyFollowController {

    private final CompanyFollowService companyFollowService;

    @PostMapping("/{id}/follow")
    @PreAuthorize("hasRole('JOB_SEEKER')")
    @Operation(
            summary = "Follow or unfollow an employer",
            description = "Toggles. Returns true when the seeker now follows the company."
    )
    public ApiResponse<Boolean> toggleFollow(@PathVariable UUID id) {
        boolean following = companyFollowService.toggleFollow(id);
        return ApiResponse.success(following, following ? "Following" : "Unfollowed");
    }

    @GetMapping("/followed/me")
    @PreAuthorize("hasRole('JOB_SEEKER')")
    @Operation(summary = "Employers I follow")
    public ApiResponse<PageResponseDto<FollowedCompanyDto>> myFollowed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ApiResponse.success(companyFollowService.myFollowed(page, size),
                "Followed companies retrieved successfully");
    }

    /**
     * Ids only, so a list of company cards can render its follow buttons
     * without one request per card.
     */
    @GetMapping("/followed/me/ids")
    @PreAuthorize("hasRole('JOB_SEEKER')")
    @Operation(summary = "Ids of the employers I follow")
    public ApiResponse<List<UUID>> myFollowedIds() {
        return ApiResponse.success(companyFollowService.myFollowedIds(),
                "Followed company ids retrieved successfully");
    }

    @GetMapping("/{id}/followers/count")
    @Operation(
            summary = "How many seekers follow this employer",
            description = "Public. Shown on the company page as a signal of reach."
    )
    public ApiResponse<Long> followerCount(@PathVariable UUID id) {
        return ApiResponse.success(companyFollowService.followerCount(id),
                "Follower count retrieved successfully");
    }
}
