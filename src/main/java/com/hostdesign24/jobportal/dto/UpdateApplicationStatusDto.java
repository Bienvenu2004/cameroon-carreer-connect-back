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
}
