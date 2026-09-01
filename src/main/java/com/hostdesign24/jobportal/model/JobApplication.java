package com.hostdesign24.jobportal.model;

import com.hostdesign24.jobportal.model.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "job_applications", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"userId", "job"})
})
public class JobApplication extends BaseEntity implements Serializable {

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "profile_id")
    private JobSeekerProfile profile;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "job_id")
    private Job job;

    private LocalDate applicationDate = LocalDate.now();

    @Column(columnDefinition = "TEXT")
    private String coverLetter;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    // Interview details captured by the recruiter when moving the application
    // to INTERVIEW. Persisted so they can be shown back in the UI and included
    // in the invitation email sent to the candidate.
    private String interviewPlace;

    private LocalDateTime interviewDateTime;

    private String interviewPhone;

    @Column(columnDefinition = "TEXT")
    private String interviewNote;

    /**
     * Why the application reached its current status, shown to the candidate.
     *
     * The status DTO carried rich detail for INTERVIEW — place, time, phone,
     * note — and nothing at all for REJECTED, so a candidate learned they were
     * rejected and never why. Being ghosted is the single most common complaint
     * job seekers have; one sentence costs the recruiter a click.
     */
    @Column(columnDefinition = "TEXT")
    private String statusReason;

    /**
     * Append-only history of status changes, newest last.
     * Powers the candidate-facing timeline and the employer responsiveness
     * metric shown on company pages.
     */
    @OneToMany(
            mappedBy = "application",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("occurredAt ASC")
    private List<ApplicationEvent> events = new ArrayList<>();
}