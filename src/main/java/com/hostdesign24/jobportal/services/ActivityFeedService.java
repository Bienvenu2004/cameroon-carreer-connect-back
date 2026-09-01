package com.hostdesign24.jobportal.services;

import com.hostdesign24.jobportal.dto.feed.ActivityEventDto;

import java.util.List;

/**
 * Employer activity, derived from what the platform already records.
 *
 * Stage two of company following: the follow graph answers "who do I care
 * about", and this answers "what have they been doing".
 */
public interface ActivityFeedService {

    /** Recent activity across the whole platform. Public. */
    List<ActivityEventDto> platformFeed(int limit);

    /**
     * Activity from the employers the current seeker follows.
     *
     * Falls back to the platform feed when the seeker follows nobody yet, so a
     * new account never meets an empty screen. The point of deriving the feed
     * rather than authoring it is that there is always something true to show.
     */
    List<ActivityEventDto> myFeed(int limit);
}
