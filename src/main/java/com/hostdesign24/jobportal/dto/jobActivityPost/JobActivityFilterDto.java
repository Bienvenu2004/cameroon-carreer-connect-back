package com.hostdesign24.jobportal.dto.jobActivityPost;

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
}
