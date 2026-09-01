package com.hostdesign24.jobportal.dto.candidate;

import com.hostdesign24.jobportal.dto.common.FilterDto;
import com.hostdesign24.jobportal.model.enums.DiplomaLevel;
import com.hostdesign24.jobportal.model.enums.Region;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * What a recruiter can search the candidate pool by.
 *
 * The platform used to be one-directional: seekers applied, recruiters reacted,
 * and there was no route to anyone who had not already applied to you. Every
 * field here maps to something the profile already stored and never exposed.
 *
 * Note what is absent: no name search, and no free-text over the whole profile.
 * Searching by name turns an opt-in talent pool into a people-lookup directory,
 * which is a different product with different consent implications; a recruiter
 * looking for a specific person they already know can be sent a link instead.
 */
@Getter
@Setter
public class CandidateSearchFilterDto extends FilterDto {

    /** Free text matched against job titles held and field of study. */
    private String keyword;

    /** Candidate must hold every skill listed. */
    private List<String> skills;

    private Region region;

    private String city;

    /** "Bac+3 minimum": matches candidates at this level or above. */
    private DiplomaLevel minimumDiploma;

    /** Matched against the profile's free-text spoken languages. */
    private String language;

    private Integer minYearsOfExperience;

    /**
     * Only candidates who opted in within the last N days.
     *
     * A talent pool is only useful if the people in it are still looking, and a
     * profile that opted in two years ago is closer to a lead list than a
     * candidate list.
     */
    private Integer activeWithinDays;

    /** Exclude candidates already invited to this job. */
    private java.util.UUID excludeInvitedForJobId;
}
