package com.hostdesign24.jobportal.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "job-saves", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"job_seeker_profile_id", "job"})
})
@Getter
@Setter
public class JobSave extends BaseEntity {

    // No cascade: un-saving a job deletes this row, and with CascadeType.ALL that
    // delete reached through to the Job and the seeker's profile.
    @ManyToOne
    @JoinColumn(name = "job_seeker_profile_id")
    private JobSeekerProfile profile;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;

    private LocalDate savedOn = LocalDate.now();
}