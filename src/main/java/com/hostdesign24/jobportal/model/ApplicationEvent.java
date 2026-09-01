package com.hostdesign24.jobportal.model;

import com.hostdesign24.jobportal.model.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One recorded transition in an application's life.
 *
 * Two things needed this. The candidate side had no history at all — an
 * application showed a current status and nothing else, so the process felt like
 * a black box, which is the most common complaint job seekers have anywhere.
 * And the employer responsiveness metric ("replies to 4 in 5 applicants, usually
 * within 6 days") has to be computed from something; {@code updatedAt} on the
 * application only ever remembers the most recent change.
 *
 * Append-only: rows are written by the service on every status change and never
 * updated. {@code fromStatus} is null for the row recording the application
 * itself.
 */
@Entity
@Table(name = "application_events")
@Getter
@Setter
public class ApplicationEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id")
    private JobApplication application;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus toStatus;

    /** Whoever caused the transition: the recruiter, or the seeker withdrawing. */
    private UUID actorId;

    /** Optional note shown to the candidate — a rejection reason, for instance. */
    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false)
    private LocalDateTime occurredAt = LocalDateTime.now();
}
