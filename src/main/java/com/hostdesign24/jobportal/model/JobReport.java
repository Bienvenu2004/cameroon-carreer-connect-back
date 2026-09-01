package com.hostdesign24.jobportal.model;

import com.hostdesign24.jobportal.model.enums.ReportReason;
import com.hostdesign24.jobportal.model.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A user's report against a job listing.
 *
 * The trust and safety card on the job page has always offered a report button;
 * until now it thanked the user and discarded what they said. Anti-scam is the
 * differentiator this platform claims for itself, and reporting is the one
 * control that acts on it, so the reports now go somewhere: an admin moderation
 * queue shaped like the company approval queue that already exists.
 *
 * {@code reporterId} is nullable so anonymous visitors — who are exactly the
 * people browsing listings before they trust the site enough to register — can
 * still flag a fraudulent advert.
 */
@Entity
@Table(name = "job_reports")
@Getter
@Setter
public class JobReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id")
    private Job job;

    /** Null when reported by an anonymous visitor. */
    private UUID reporterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.PENDING;

    private UUID resolvedBy;

    private LocalDateTime resolvedAt;

    @Column(columnDefinition = "TEXT")
    private String resolutionNote;
}
