package com.hostdesign24.jobportal.dto.candidate;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * An invitation as the candidate sees it: which job, at which company, and
 * whether they have already acted on it.
 */
@Getter
@Setter
@Builder
public class InvitationDto {

    private UUID id;
    private UUID jobId;
    private String jobTitle;
    private String companyName;
    private String message;
    private LocalDateTime sentAt;

    /** Set once the candidate applies, so the list can show what is outstanding. */
    private LocalDateTime respondedAt;

    /** False when the job has since closed or its deadline passed. */
    private boolean jobStillOpen;
}
