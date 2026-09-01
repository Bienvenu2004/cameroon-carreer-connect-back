package com.hostdesign24.jobportal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A recruiter asking a specific candidate to apply for a specific job.
 *
 * This is the flow that turns the platform from a noticeboard into a
 * marketplace: until candidate search existed, a recruiter's world was limited
 * to people who had already applied to them, and there was no route to anyone
 * else. Finding someone is only half of it — being able to approach them is the
 * other half.
 *
 * Recorded as its own row rather than as a bare notification so a recruiter can
 * see who they have already contacted, and so the same candidate is not invited
 * to the same job twice.
 */
@Entity
@Table(
        name = "job_invitations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_invitation_job_profile",
                columnNames = {"job_id", "profile_id"}))
@Getter
@Setter
public class JobInvitation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id")
    private JobSeekerProfile profile;

    /** The recruiter who sent it. */
    private UUID invitedBy;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    /** Set when the candidate goes on to apply, so recruiters see what worked. */
    private LocalDateTime respondedAt;
}
