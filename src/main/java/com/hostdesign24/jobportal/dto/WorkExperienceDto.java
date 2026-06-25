package com.hostdesign24.jobportal.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Wire shape for a single work-experience row on the seeker profile.
 *
 * Used by both /me (own profile) and /{id} (recruiter viewing an
 * applicant). Date fields serialize as ISO yyyy-MM-dd by default
 * thanks to the global Jackson JavaTimeModule registration.
 */
@Getter
@Setter
public class WorkExperienceDto {
    private UUID id;
    private String title;
    private String companyName;
    private String city;
    private String country;
    private LocalDate startDate;
    /** Null when {@link #isCurrent} is true. */
    private LocalDate endDate;
    private boolean isCurrent;
    private String description;
}
