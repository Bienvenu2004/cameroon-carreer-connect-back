package com.hostdesign24.jobportal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "recommendations")
@Getter
@Setter
@NoArgsConstructor
public class Recommendation extends BaseEntity {

    /** Recipient — always a JOB_SEEKER. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** The recommended job. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    /** Model's self-reported confidence (0..1). */
    @Column(nullable = false)
    private double score;

    /** One- or two-sentence explanation shown next to the recommendation. */
    @Column(name = "explanation", columnDefinition = "TEXT", nullable = false)
    private String explanation;

    /** Provider + model name + prompt revision (e.g. "gemini-flash-latest/v1"). */
    @Column(name = "model_version", nullable = false)
    private String modelVersion;

    /**
     * Structured feature breakdown for future learning-to-rank work.
     * Empty {@code {}} in v1 — we don't compute features yet.
     * JSONB so we can add fields without a schema change.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "feature_breakdown", columnDefinition = "jsonb")
    private String featureBreakdown;

    /** When this recommendation was shown to the user. */
    @Column(name = "shown_at", nullable = false)
    private LocalDateTime shownAt;

    /* ---- Outcome columns — written by future feedback wiring. ---- */

    /** User clicked through to the job detail page. */
    @Column(nullable = false)
    private boolean clicked = false;

    /** User applied to the job. */
    @Column(nullable = false)
    private boolean applied = false;

    /** User explicitly dismissed / hid it. */
    @Column(nullable = false)
    private boolean dismissed = false;

    @PrePersist
    void onCreate() {
        if (shownAt == null) shownAt = LocalDateTime.now();
    }

    public Recommendation(User user, Job job, double score, String explanation, String modelVersion) {
        this.user = user;
        this.job = job;
        this.score = score;
        this.explanation = explanation;
        this.modelVersion = modelVersion;
        this.featureBreakdown = "{}";
    }
}
