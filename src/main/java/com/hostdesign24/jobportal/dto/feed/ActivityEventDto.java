package com.hostdesign24.jobportal.dto.feed;

import com.hostdesign24.jobportal.dto.file.FileDto;
import com.hostdesign24.jobportal.model.enums.ActivityType;
import com.hostdesign24.jobportal.model.enums.Region;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One thing an employer did.
 *
 * Carries no prose. The wording is the frontend's job, because the platform is
 * bilingual and a sentence assembled on the server would arrive in whichever
 * language the server happened to pick.
 */
@Getter
@Setter
@Builder
public class ActivityEventDto {

    private ActivityType type;

    private UUID companyId;
    private String companyName;
    private FileDto companyLogo;

    /** Null for company-level events such as verification. */
    private UUID jobId;
    private String jobTitle;

    private Region region;

    private LocalDateTime occurredAt;
}
