package com.hostdesign24.jobportal.services;

import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.company.FollowedCompanyDto;
import com.hostdesign24.jobportal.model.Job;

import java.util.List;
import java.util.UUID;

/**
 * Job seekers following employers.
 *
 * Answers the standing request "tell me when this employer is hiring", which
 * until now had no answer short of checking the listings by hand.
 */
public interface CompanyFollowService {

    /**
     * Follow or unfollow, depending on the current state.
     *
     * @return true if the seeker now follows the company
     */
    boolean toggleFollow(UUID companyId);

    /** Employers the current seeker follows, newest first. */
    PageResponseDto<FollowedCompanyDto> myFollowed(int page, int size);

    /** How many seekers follow this employer. Public. */
    long followerCount(UUID companyId);

    /** Which of the given companies the current seeker follows. */
    List<UUID> myFollowedIds();

    /**
     * Tell a company's followers about a job it has just posted.
     *
     * Best-effort by design: a notification failure must never roll back the
     * job the recruiter was actually trying to create.
     */
    void notifyFollowersOfNewJob(Job job);
}
