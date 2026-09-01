package com.hostdesign24.jobportal.dto;

import com.hostdesign24.jobportal.model.enums.DiplomaLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Inbound DTO for one education row in the multipart profile PATCH, bound from
 * indexed form-data keys exactly like skills and experiences:
 *
 *   educations[0].level=LICENCE
 *   educations[0].fieldOfStudy=Genie Logiciel
 *   educations[0].institution=Universite de Douala
 *   educations[0].startDate=2018-10-01
 *   educations[0].endDate=2021-07-31
 *   educations[0].isCurrent=false
 *
 * Validated in the service rather than here, because the rules are cross-field
 * (end after start, isCurrent implies no end date) and a coherent sentence beats
 * a generic constraint violation.
 */
@Getter
@Setter
public class EducationSaveDto {
    private DiplomaLevel level;
    private String fieldOfStudy;
    private String institution;
    private String city;
    private String country;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isCurrent;
    private String description;
}
