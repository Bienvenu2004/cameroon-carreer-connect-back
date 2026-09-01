package com.hostdesign24.jobportal.dto;

import com.hostdesign24.jobportal.model.enums.DiplomaLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/** One qualification as returned on a profile. */
@Getter
@Setter
public class EducationDto {
    private UUID id;
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
