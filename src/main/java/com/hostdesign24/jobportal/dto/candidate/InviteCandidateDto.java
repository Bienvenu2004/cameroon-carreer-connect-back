package com.hostdesign24.jobportal.dto.candidate;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** A recruiter asking one candidate to apply for one of their jobs. */
@Getter
@Setter
public class InviteCandidateDto {

    @NotNull
    private UUID profileId;

    @NotNull
    private UUID jobId;

    /**
     * Optional note. Kept short deliberately: this is an invitation to apply,
     * not a messaging system, and a platform that quietly becomes an inbox
     * acquires moderation obligations it is not built for.
     */
    private String message;
}
