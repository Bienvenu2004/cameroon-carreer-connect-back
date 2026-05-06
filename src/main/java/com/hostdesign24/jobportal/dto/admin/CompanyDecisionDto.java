package com.hostdesign24.jobportal.dto.admin;

import lombok.Getter;
import lombok.Setter;

/**
 * Body for admin approve/reject/suspend decisions on a company.
 * `reason` is required for REJECT and SUSPEND, optional for APPROVE.
 */
@Getter
@Setter
public class CompanyDecisionDto {
    private String reason;
}
