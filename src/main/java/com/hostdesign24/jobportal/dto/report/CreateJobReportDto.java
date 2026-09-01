package com.hostdesign24.jobportal.dto.report;

import com.hostdesign24.jobportal.model.enums.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** What a user tells us when they flag a listing. */
@Getter
@Setter
public class CreateJobReportDto {

    @NotNull
    private ReportReason reason;

    /**
     * Optional free text. Capped because this is a report, not correspondence,
     * and an unbounded text field on an endpoint open to anonymous callers is an
     * invitation.
     */
    @Size(max = 2000)
    private String details;
}
