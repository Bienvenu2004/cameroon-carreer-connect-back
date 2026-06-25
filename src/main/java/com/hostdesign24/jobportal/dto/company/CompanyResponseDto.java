package com.hostdesign24.jobportal.dto.company;

import com.hostdesign24.jobportal.dto.file.FileDto;
import com.hostdesign24.jobportal.model.enums.CompanySize;
import com.hostdesign24.jobportal.model.enums.CompanyStatus;
import com.hostdesign24.jobportal.model.enums.Industry;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class CompanyResponseDto {

    private UUID id;

    private String name;

    private String description;

    /** Long-form "About / Culture" copy for the company detail page. */
    private String about;

    /** Optional promotional video URL (YouTube, Vimeo, etc.). */
    private String promoVideoUrl;

    private String website;

    private Industry industry;

    private CompanySize size;

    private FileDto logo;

    /** Wide hero banner — null when the company hasn't uploaded one. */
    private FileDto banner;

    private CompanyAddressDto address;

    private CompanyStatus status;

    private String rejectionReason;

    private LocalDateTime verifiedAt;

    private LocalDateTime createdAt;

    private int activeJobs = 0;
}
