package com.hostdesign24.jobportal.dto.jobActivityPost;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.hostdesign24.jobportal.model.Address;
import com.hostdesign24.jobportal.model.enums.DiplomaLevel;
import com.hostdesign24.jobportal.model.enums.ExperienceLevel;
import com.hostdesign24.jobportal.model.enums.JobLanguage;
import com.hostdesign24.jobportal.model.enums.JobSite;
import com.hostdesign24.jobportal.model.enums.JobType;
import com.hostdesign24.jobportal.model.enums.SalaryCurrency;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobPostActivityUpsertDto {

    private Address location;

    private UUID companyId;

    private String description;

    private JobType type;

    /** Advertised pay band. Either bound may be null. */
    private BigDecimal salaryMin;

    private BigDecimal salaryMax;

    private SalaryCurrency salaryCurrency = SalaryCurrency.XAF;

    private JobSite site;

    /** Required working language for the role (FRENCH / ENGLISH / BILINGUAL). */
    private JobLanguage requiredLanguage;

    private String title;

    private String benefits;

    /** Seniority the role is pitched at. Matched by natural-language search. */
    private ExperienceLevel experienceLevel;

    /** Minimum qualification, as local adverts express it ("Bac+3 minimum"). */
    private DiplomaLevel minimumDiploma;

    /** Last day applications are accepted. Null means no fixed deadline. */
    private LocalDate applicationDeadline;

    /* --- public-sector concours; only an admin may set these --- */

    private Boolean publicSector;

    private String publicSectorRef;

    private String publicSectorBody;
}
