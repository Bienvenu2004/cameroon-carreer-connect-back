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

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "job_seeker_profile")
    private JobSeekerProfile jobSeekerProfile;
}