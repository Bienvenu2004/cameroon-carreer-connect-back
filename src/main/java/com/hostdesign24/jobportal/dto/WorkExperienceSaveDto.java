package com.hostdesign24.jobportal.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Inbound DTO for a single work-experience row in the multipart PATCH
 * payload. Each row binds from indexed form-data keys, just like skills:
 *
 *   experiences[0].title=Backend Engineer
 *   experiences[0].companyName=Acme Corp
 *   experiences[0].city=Yaoundé
 *   experiences[0].startDate=2022-01-15
 *   experiences[0].endDate=2024-06-30
 *   experiences[0].isCurrent=false
 *   experiences[0].description=Built REST APIs for ...
 *
 * Validation is done server-side in the service layer (not here via
 * jakarta validation) because the rules are cross-field: end > start,
 * isCurrent ↔ endDate=null. Service-level checks let us return a
 * coherent error message rather than a generic constraint violation.
 */
@Getter
@Setter
public class WorkExperienceSaveDto {
    private String title;
    private String companyName;
    private String city;
    private String country;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isCurrent;
    private String description;
}
