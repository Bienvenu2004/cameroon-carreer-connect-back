package com.hostdesign24.jobportal.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Optional context when a candidate withdraws.
 *
 * The reason is never required. Someone leaving the process owes nobody an
 * explanation, and demanding one is the kind of friction that makes people
 * abandon the form and leave a stale application behind instead -- which is the
 * exact outcome withdrawal exists to prevent.
 */
@Getter
@Setter
public class WithdrawApplicationDto {

    private String reason;
}
