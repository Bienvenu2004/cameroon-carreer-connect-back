package com.hostdesign24.jobportal.controller;

import com.hostdesign24.jobportal.dto.common.ApiResponse;
import com.hostdesign24.jobportal.dto.feed.ActivityEventDto;
import com.hostdesign24.jobportal.services.ActivityFeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Employer activity.
 *
 * Everything here is derived from what the platform already records, so nothing
 * was authored by anyone and nothing needs moderating.
 */
@RestController
@RequestMapping("/api/hjp/feed")
@RequiredArgsConstructor
@Tag(name = "Activity feed", description = "What employers have been doing")
public class ActivityFeedController {

    private final ActivityFeedService activityFeedService;

    @GetMapping
    @Operation(
            summary = "Recent activity across the platform",
            description = "Public. Doubles as a discovery surface for visitors with no account."
    )
    public ApiResponse<List<ActivityEventDto>> platform(
            @RequestParam(defaultValue = "12") int limit) {
        return ApiResponse.success(activityFeedService.platformFeed(limit),
                "Activity retrieved successfully");
    }

    @GetMapping("/following")
    @PreAuthorize("hasRole('JOB_SEEKER')")
    @Operation(
            summary = "Activity from the employers I follow",
            description = "Falls back to platform-wide activity when the seeker follows nobody."
    )
    public ApiResponse<List<ActivityEventDto>> following(
            @RequestParam(defaultValue = "12") int limit) {
        return ApiResponse.success(activityFeedService.myFeed(limit),
                "Activity retrieved successfully");
    }
}
