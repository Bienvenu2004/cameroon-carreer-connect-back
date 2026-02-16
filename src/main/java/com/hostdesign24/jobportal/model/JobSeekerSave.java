package com.hostdesign24.jobportal.model;

    import jakarta.persistence.*;
    import lombok.*;

    import java.time.LocalDate;

@Entity
    @Table(uniqueConstraints = {
            @UniqueConstraint(columnNames = {"job_seeker_profile_id", "job"})
    })
    @Getter
    @Setter
    public class JobSeekerSave extends BaseEntity {

        @ManyToOne(cascade = CascadeType.ALL)
        @JoinColumn(name = "job_seeker_profile_id")
        private JobSeekerProfile profile;

        @ManyToOne(cascade = CascadeType.ALL)
        @JoinColumn(name = "job_id")
        private JobPost job;

        private LocalDate savedOn = LocalDate.now();
    }