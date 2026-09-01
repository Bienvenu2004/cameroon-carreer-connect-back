package com.hostdesign24.jobportal.services;

import com.hostdesign24.jobportal.dto.candidate.CandidateSearchFilterDto;
import com.hostdesign24.jobportal.dto.candidate.CandidateSummaryDto;
import com.hostdesign24.jobportal.dto.candidate.InvitationDto;
import com.hostdesign24.jobportal.dto.candidate.InviteCandidateDto;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;

/**
 * Recruiter-facing candidate search, and the invitations that follow from it.
 *
 * Finding someone is only half of what makes this a recruitment platform rather
 * than a job board; being able to approach them is the other half.
 */
public interface CandidateSearchService {

    /** Search the opted-in candidate pool. */
    PageResponseDto<CandidateSummaryDto> search(CandidateSearchFilterDto filter);

    /**
     * Invite one candidate to apply for one of the recruiter's own jobs.
     *
     * @return true when an invitation was sent, false when one already existed
     */
    boolean invite(InviteCandidateDto request);

    /** Invitations received by the current job seeker, newest first. */
    PageResponseDto<InvitationDto> myInvitations(int page, int size);

    /** How many invitations the current seeker has not yet acted on. */
    long myPendingInvitationCount();
}
