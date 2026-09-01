
package com.hostdesign24.jobportal.model;

import org.hibernate.annotations.BatchSize;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
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

    /**
     * Short-form video introduction ("video résumé"). Uploaded to Cloudinary
     * as a {@code video} resource, so its delivery URL is directly streamable
     * (HTTP range requests / CDN) by an HTML {@code <video>} element — no
     * backend proxy needed. Playback only; the UI offers no download.
     */
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = true)
    @JoinColumn(name = "video_resume_id", referencedColumnName = "id")
    private File videoResume;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = true)
    @JoinColumn(name = "profile_photo_id", referencedColumnName = "id")
    private File profilePhoto;

    /**
     * Batched rather than join-fetched. Candidate search renders these for every
     * row, and Hibernate cannot fetch several bags in one query -- so it loads
     * them a page at a time instead of a row at a time.
     */
    @BatchSize(size = 25)
    @OneToMany(targetEntity = Skill.class, cascade = CascadeType.ALL, mappedBy = "jobSeekerProfile")
    private List<Skill> skills;

    /**
     * Structured CV — one row per past or current role.
     * Feeds the AI matcher's {@code yearsOfExperience} signal (the
     * computed total of all ranges) and powers the recruiter-facing
     * profile view. {@code orphanRemoval = true} guarantees deletes
     * propagate when a seeker removes a row through the bulk PATCH.
     */
    @BatchSize(size = 25)
    @OneToMany(
            mappedBy = "profile",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<WorkExperience> experiences;

    /**
     * Structured education — one row per qualification.
     * The diploma is the first filter most Cameroonian recruiters apply, so this
     * is what makes a profile findable in candidate search and what lets a job's
     * "Bac+3 minimum" be matched rather than merely stated.
     */
    @BatchSize(size = 25)
    @OneToMany(
            mappedBy = "profile",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<Education> educations;

    /**
     * Whether recruiters may find this person through candidate search.
     *
     * Defaults to false, and stays false until the seeker says otherwise.
     * Appearing in an employer-facing search is a materially different thing
     * from posting an application, so it is an explicit opt-in rather than
     * something that happens to people who signed up to look for work.
     */
    private boolean searchable = false;

    /** When the seeker last opted in, for the "active candidates" filter. */
    private LocalDateTime searchableSince;

    public JobSeekerProfile(User user) {
        this.user = user;
    }
}