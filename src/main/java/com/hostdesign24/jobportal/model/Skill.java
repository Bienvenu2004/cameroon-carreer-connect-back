package com.hostdesign24.jobportal.model;

import com.hostdesign24.jobportal.model.enums.ExperienceLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "skills")
@Getter
@Setter
@ToString(callSuper = true, exclude = {"jobSeekerProfile"})
public class Skill extends BaseEntity {

    private String name;

    @Enumerated(EnumType.STRING)
    private ExperienceLevel experienceLevel;

    private int yearsOfExperience = 0;

    /**
     * No cascade from a skill to its owner. This used to be CascadeType.ALL,
     * which runs the wrong way: removing one skill cascaded the REMOVE up to
     * the profile, so deleting a single skill tried to delete the job seeker's
     * whole profile. The profile owns its skills (see JobSeekerProfile.skills),
     * never the reverse.
     */
    @ManyToOne
    @JoinColumn(name = "job_seeker_profile")
    private JobSeekerProfile jobSeekerProfile;
}