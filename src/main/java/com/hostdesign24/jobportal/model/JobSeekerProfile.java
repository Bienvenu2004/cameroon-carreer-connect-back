
package com.hostdesign24.jobportal.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
@Entity
@Table(name = "job_seeker_profile")
@Getter
@Setter
@NoArgsConstructor
public class JobSeekerProfile extends BaseEntity {
    @OneToOne
    @JoinColumn(name = "user_id")
    @MapsId
    private User user;
    private String firstName;
    private String lastName;
    private String city;
    private String state;
    private String country;
    private String workAuthorization;
    private String employmentType;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = true)
    @JoinColumn(name = "resume_id", referencedColumnName = "id")
    private File resume;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = true)
    @JoinColumn(name = "profile_photo_id", referencedColumnName = "id")
    private File profilePhoto;

    @OneToMany(targetEntity = Skill.class, cascade = CascadeType.ALL, mappedBy = "jobSeekerProfile")
    private List<Skill> skills;

    public JobSeekerProfile(User user) {
        this.user = user;
    }
}