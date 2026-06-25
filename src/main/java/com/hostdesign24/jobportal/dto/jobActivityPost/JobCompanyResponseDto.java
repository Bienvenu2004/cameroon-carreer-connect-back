package com.hostdesign24.jobportal.dto.jobActivityPost;

import com.hostdesign24.jobportal.dto.file.FileDto;
import com.hostdesign24.jobportal.model.enums.CompanyStatus;
import com.hostdesign24.jobportal.model.enums.Industry;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Minimal company snapshot embedded in JobPostResponseDto.
 * Fields match the relevant subset of the frontend's CompanyDto so the
 * job card / detail page can render the linked company correctly
 * (status drives the "Verified" badge, id drives the company link).
 */
@Getter
@Setter
public class JobCompanyResponseDto {
    private UUID id;
    private String name;
    private String description;
    private Industry industry;
    private CompanyStatus status;
    private FileDto logo;
}
