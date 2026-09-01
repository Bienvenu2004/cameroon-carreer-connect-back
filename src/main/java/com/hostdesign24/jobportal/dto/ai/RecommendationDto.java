package com.hostdesign24.jobportal.dto.ai;

import com.hostdesign24.jobportal.dto.JobPostResponseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Wire-format DTO for AI-generated job recommendations.
 *
 * Reuses {@link JobPostResponseDto} verbatim for the {@code job} field so
 * the frontend can render the same {@code <JobCard>} component it
 * already uses on the public listing page. The two extra fields the AI
 * layer adds are {@code score} and {@code reason}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationDto {
    /** Recommendation row id — used by future feedback endpoints. */
    private UUID id;

    /** Full job snapshot, ready to drop into the existing JobCard component. */
    private JobPostResponseDto job;

    /** Model's confidence in the match (0..1). */
    private double score;

    /** Short user-facing explanation rendered under the job title. */
    private String reason;

    /** When the row was created — useful for "fetched X minutes ago" UI. */
    private LocalDateTime generatedAt;
}
