package com.hostdesign24.jobportal.dto.company;

import com.hostdesign24.jobportal.dto.file.FileDto;
import com.hostdesign24.jobportal.model.enums.Industry;
import com.hostdesign24.jobportal.model.enums.Region;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** An employer as it appears on the seeker's "following" list. */
@Getter
@Setter
@Builder
public class FollowedCompanyDto {

    private UUID companyId;
    private String name;
    private Industry industry;
    private Region region;
    private String city;
    private FileDto logo;

    /**
     * Open jobs right now. The reason someone followed this employer, so it is
     * the first thing the row should answer.
     */
    private long openJobs;

    private LocalDateTime followedAt;
    private boolean emailAlerts;
}
