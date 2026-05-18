package com.hostdesign24.jobportal.dto;

import com.hostdesign24.jobportal.model.Address;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Multipart payload for PATCH /api/hjp/job-seeker-profile.
 *
 * Field names match the entity 1:1, so MapStruct can auto-map and so the
 * frontend's natural form-data keys ("firstName", "phoneNumber",
 * "address.city", "address.region", ...) bind via Spring's @ModelAttribute
 * data-binder without any custom converter.
 *
 * Multipart fields:
 *   - resume        — PDF/DOC/DOCX of the candidate's resume
 *   - profilePhoto  — image of the candidate
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobSeekerProfileSaveDto {

    private String firstName;
    private String lastName;
    private String phoneNumber;

    /**
     * Embedded address. Spring's WebDataBinder accepts dot-notation form
     * keys (e.g. address.city, address.region, address.country) and binds
     * them to the nested Address fields here.
     */
    private Address address;

    private String workAuthorization;
    private String employmentType;

    /** Comma-separated list of spoken languages, e.g. "French,English". */
    private String spokenLanguages;

    /* Optional portfolio / social URLs — all nullable. */
    private String githubUrl;
    private String linkedinUrl;
    private String websiteUrl;
    private String portfolioUrl;
    private String twitterUrl;
    private String facebookUrl;

    private MultipartFile resume;
    private MultipartFile profilePhoto;

    /**
     * Skills the candidate wants to advertise. Each entry binds from
     * skills[0].name, skills[1].name, ... in the multipart form data.
     * The full list replaces any previous skills on save.
     */
    private List<SkillSaveDto> skills;
}
