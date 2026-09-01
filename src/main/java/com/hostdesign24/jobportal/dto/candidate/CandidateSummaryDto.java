package com.hostdesign24.jobportal.dto.candidate;

import com.hostdesign24.jobportal.dto.file.FileDto;
import com.hostdesign24.jobportal.model.enums.DiplomaLevel;
import com.hostdesign24.jobportal.model.enums.Region;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * A candidate as they appear in recruiter search results.
 *
 * Deliberately narrower than the full profile. Search results are browsed in
 * bulk by people the candidate has not spoken to, so this carries what is needed
 * to decide whether to open the profile and nothing more.
 *
 * Notably absent: email, phone number and the CV file. A recruiter reaches a
 * candidate by inviting them through the platform, which leaves a record and
 * which the candidate can ignore -- not by lifting a phone number out of a
 * search result. Contact details appear once the person applies or accepts.
 */
@Getter
@Setter
@Builder
public class CandidateSummaryDto {

    private UUID profileId;

    private String firstName;

    /** Initial only. Enough to address someone; not enough to identify them elsewhere. */
    private String lastNameInitial;

    private FileDto profilePhoto;

    private Region region;

    private String city;

    private DiplomaLevel highestDiploma;

    private String fieldOfStudy;

    /** Most recent role held, for the one-line summary. */
    private String currentTitle;

    private Integer totalYearsOfExperience;

    private List<String> topSkills;

    private String spokenLanguages;

    /** Whether this recruiter has already invited them to the job in context. */
    private boolean alreadyInvited;
}
