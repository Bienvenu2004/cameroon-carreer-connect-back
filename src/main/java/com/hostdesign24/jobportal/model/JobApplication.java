package com.hostdesign24.jobportal.model;

import com.hostdesign24.jobportal.model.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
}