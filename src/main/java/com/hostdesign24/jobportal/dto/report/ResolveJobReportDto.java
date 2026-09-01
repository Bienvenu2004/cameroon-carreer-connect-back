package com.hostdesign24.jobportal.dto.report;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** An administrator's decision on a reported listing. */
@Getter
@Setter
public class ResolveJobReportDto {

    /** True to uphold the report and take the listing down. */
    @NotNull
    private Boolean upheld;

    @Size(max = 2000)
    private String note;
}
