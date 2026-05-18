
package com.hostdesign24.jobportal.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
@Entity
@Table(name = "job_seeker_profiles")
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
    private String phoneNumber;

    @Embedded
    private Address address;

    private String workAuthorization;
    private String employmentType;

    /**
     * Comma-separated list of spoken languages, e.g. "French,English,Spanish".
     * Stored as a plain TEXT column to avoid an extra collection table; the
     * frontend manages adding/removing entries in a tag-input pattern.
     */
    @Column(columnDefinition = "TEXT")
    private String spokenLanguages;

    /* ---- Portfolio / social links (optional) ---- */
    private String githubUrl;
    private String linkedinUrl;
    /** Personal website / blog / portfolio. */
    private String websiteUrl;
    /** Behance, Dribbble, Itch.io, etc. */
    private String portfolioUrl;
    private String twitterUrl;
    private String facebookUrl;

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