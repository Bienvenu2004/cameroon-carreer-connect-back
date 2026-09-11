package com.hostdesign24.jobportal.dto;

import com.hostdesign24.jobportal.dto.common.FilterDto;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class JobApplicationFilterDto extends FilterDto {
    private UUID jobId;
    private UUID profileId;

    /**
     * Narrows the list to a single application, for a page opened from a
     * notification about it. It only ever narrows: the role scoping in
     * JobApplicationSpecification still applies, so it cannot reach an
     * application the caller could not already list.
     */
    private UUID applicationId;
}
