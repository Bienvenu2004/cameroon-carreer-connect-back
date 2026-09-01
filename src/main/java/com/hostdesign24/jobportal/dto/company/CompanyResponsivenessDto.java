package com.hostdesign24.jobportal.dto.company;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * How an employer actually behaves towards applicants.
 *
 * A verification badge tells a candidate the company is real. This tells them
 * whether applying is worth their evening and their data bundle — a much sharper
 * question, and one no WhatsApp group or notice board can answer. It also
 * creates a quiet incentive for recruiters to work their pipeline rather than
 * letting applications rot.
 *
 * Computed entirely from application status history, so it costs no new data
 * collection and cannot be self-reported.
 */
@Getter
@Setter
@Builder
public class CompanyResponsivenessDto {

    private long applicationsReceived;

    /** Applications that moved beyond APPLIED at least once. */
    private long applicationsAnswered;

    /** 0-100, or null when there is not enough history to be meaningful. */
    private Integer responseRate;

    /** Average days from applying to the first response. Null when unknown. */
    private Integer averageDaysToRespond;

    /**
     * False when too few applications have been received to say anything honest.
     * Publishing "0% response rate" off a single unanswered application would
     * defame an employer who simply joined last week.
     */
    private boolean enoughData;
}
