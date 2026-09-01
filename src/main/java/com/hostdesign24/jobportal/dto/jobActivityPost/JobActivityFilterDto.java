package com.hostdesign24.jobportal.dto.jobActivityPost;

import com.hostdesign24.jobportal.model.enums.ExperienceLevel;
import com.hostdesign24.jobportal.model.enums.DiplomaLevel;
import com.hostdesign24.jobportal.dto.common.FilterDto;
import com.hostdesign24.jobportal.model.enums.Industry;
import com.hostdesign24.jobportal.model.enums.JobLanguage;
import com.hostdesign24.jobportal.model.enums.JobSite;
import com.hostdesign24.jobportal.model.enums.JobType;
import com.hostdesign24.jobportal.model.enums.Region;
import com.hostdesign24.jobportal.model.enums.SalaryCurrency;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class JobActivityFilterDto extends FilterDto {
    private String companyCity;

    private String companyState;

    private String companyCountry;

    private String companyName;

    /** Cameroonian administrative region of the job's location. */
    private Region region;

    /** Industry of the company posting the job. */
    private Industry industry;

    private Boolean isActive;

    private Boolean isSaved;

    private String descriptionOfJob;

    private JobType jobType;

    /** Inclusive lower bound on salary. */
    private BigDecimal salaryMin;

    /** Inclusive upper bound on salary. */
    private BigDecimal salaryMax;

    private SalaryCurrency salaryCurrency;

    private JobSite jobSite;

    /** Filter on required working language (FRENCH / ENGLISH / BILINGUAL). */
    private JobLanguage requiredLanguage;

    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private Date postedDate;

    private String jobTitle;

    private Integer createdDaysAgo;

    /**
     * Free-text tokens matched with OR semantics: a job matches when ANY
     * token appears in its title OR description. Set programmatically by
     * the AI keyword fallback (never bound from a request param), so a
     * conversational query like "i need a job where i wash dishes" can
     * still surface a "Dish Washer" listing.
     */
    private List<String> keywordAny;

    /* --- added by the functional expansion --- */

    /** Seniority. The natural-language parser has always produced this. */
    private ExperienceLevel experienceLevel;

    /** "Bac+3 minimum" — matches jobs asking for this level or less. */
    private DiplomaLevel minimumDiploma;

    /**
     * Hide listings whose deadline has passed. Defaults to true on the public
     * listing so stale adverts do not accumulate in front of job seekers;
     * recruiters and admins pass false to see their own closed postings.
     */
    private Boolean hideExpired;

    /** Restrict to (or exclude) public-sector concours listings. */
    private Boolean publicSector;
}
