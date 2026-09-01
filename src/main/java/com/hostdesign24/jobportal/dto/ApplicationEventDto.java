package com.hostdesign24.jobportal.dto;

import com.hostdesign24.jobportal.model.enums.ApplicationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One entry in the candidate-facing application timeline.
 *
 * An application used to show a current status and nothing else, which made the
 * process feel like a black box. Rendering the history — applied on the 3rd,
 * reviewed on the 5th, interview on the 12th — costs nothing once the events are
 * recorded and makes the process feel like a process.
 *
 * {@code actorId} is deliberately not exposed: the candidate has no use for the
 * recruiter's internal id, and it is the kind of field that leaks by accident.
 */
public record ApplicationEventDto(
        UUID id,
        ApplicationStatus fromStatus,
        ApplicationStatus toStatus,
        String note,
        LocalDateTime occurredAt
) {
}
