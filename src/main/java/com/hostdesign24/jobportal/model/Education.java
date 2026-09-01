package com.hostdesign24.jobportal.model;

import com.hostdesign24.jobportal.model.enums.DiplomaLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * A qualification on a job seeker's profile.
 *
 * The profile previously had no way to record education at all — work history
 * and skills only. That is a large omission anywhere, and a decisive one here:
 * in the Cameroonian labour market the diploma is usually the first filter a
 * recruiter applies, and adverts are written as "Bac+3 minimum". A profile that
 * cannot express Bac+3 cannot be searched the way local recruiters think.
 *
 * Deliberately mirrors {@link WorkExperience}:
 *   - {@code institution} is free text, not a foreign key. Schools are not
 *     platform entities and never will be.
 *   - {@code isCurrent} and {@code endDate} are mutually exclusive; enforced in
 *     the service layer for the sake of readable error messages.
 *   - {@link DiplomaLevel} carries a Bac+N rank, so "at least Licence" is a
 *     single comparison rather than a set membership test.
 */
@Entity
@Table(name = "educations")
@Getter
@Setter
public class Education extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiplomaLevel level;

    /** Subject studied, e.g. "Comptabilité" or "Génie Logiciel". */
    private String fieldOfStudy;

    /** School, university or training centre, as the seeker writes it. */
    private String institution;

    private String city;

    private String country;

    private LocalDate startDate;

    private LocalDate endDate;

    /** Still studying. When true, endDate must be null. */
    private boolean isCurrent = false;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id")
    private JobSeekerProfile profile;
}
