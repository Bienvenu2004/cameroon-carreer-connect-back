package com.hostdesign24.jobportal.dto.jobActivityPost;

import java.math.BigDecimal;
import java.util.UUID;

import com.hostdesign24.jobportal.model.Address;
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

    private BigDecimal salary;

    private SalaryCurrency salaryCurrency = SalaryCurrency.XAF;

    private JobSite site;

    private String title;

    private String benefits;
}
