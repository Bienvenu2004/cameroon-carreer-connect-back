package com.hostdesign24.jobportal.dto;

import com.hostdesign24.jobportal.model.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Request body for changing an application's status.
 *
 * The interview fields are only meaningful when {@link #status} is
 * {@code INTERVIEW}; they carry the place, date/time, phone number and an
 * optional note the recruiter enters so the candidate can be told where and
 * when to show up. They are ignored for every other status.
 */
@Getter
@Setter
public class UpdateApplicationStatusDto {

    @NotNull
    private ApplicationStatus status;

    private String interviewPlace;
    private LocalDateTime interviewDateTime;
    private String interviewPhone;
    private String interviewNote;

    /**
     * Why the application reached this status, shown to the candidate and
     * included in the email.
     *
     * This DTO used to carry four fields of care for INTERVIEW and nothing at
     * all for REJECTED, so a candidate learned they were rejected and never why.
     * Being ghosted is the most common complaint job seekers have; one sentence
     * costs a recruiter a click.
     */
    private String statusReason;
}
