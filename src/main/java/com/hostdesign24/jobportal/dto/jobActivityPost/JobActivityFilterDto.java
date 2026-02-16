package com.hostdesign24.jobportal.dto.jobActivityPost;

import com.hostdesign24.jobportal.dto.common.FilterDto;
import com.hostdesign24.jobportal.model.enums.JobSite;
import com.hostdesign24.jobportal.model.enums.JobType;
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

    private Boolean isActive;

    private Boolean isSaved;

    private String descriptionOfJob;

    private JobType jobType;

    private BigDecimal salary;

    private SalaryCurrency salaryCurrency;

    private JobSite jobSite;

    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private Date postedDate;

    private String jobTitle;

    private Integer createdDaysAgo;
}
