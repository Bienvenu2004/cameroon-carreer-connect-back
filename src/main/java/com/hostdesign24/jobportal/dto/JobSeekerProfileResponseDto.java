package com.hostdesign24.jobportal.dto;

import com.hostdesign24.jobportal.dto.company.CompanyAddressDto;
import com.hostdesign24.jobportal.dto.file.FileDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Public projection of a job seeker profile. Used by both /me (own profile)
 * and /{id} (recruiter viewing an applicant's profile).
 *
 * Wire shape mirrors the embedded {@code Address} entity 1:1 via the nested
 * {@code address} field, so MapStruct auto-populates it without explicit
 * @Mapping declarations.
 */
@Getter
@Setter
public class JobSeekerProfileResponseDto {

    private UUID id;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private CompanyAddressDto address;
    private String workAuthorization;
    private String employmentType;

    /** Comma-separated list of spoken languages, e.g. "French,English". */
    private String spokenLanguages;

    /* Optional portfolio / social URLs — null when not set by the seeker. */
    private String githubUrl;
    private String linkedinUrl;
    private String websiteUrl;
    private String portfolioUrl;
    private String twitterUrl;
    private String facebookUrl;

    private FileDto profilePhoto;
    private FileDto resume;
    private List<SkillDto> skills;
}
