package com.hostdesign24.jobportal.dto.company;

import com.hostdesign24.jobportal.dto.file.FileDto;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class CompanyResponseDto {

    private UUID id;

    private String name;

    private FileDto logo;

    private CompanyAddressDto address;

    private LocalDateTime createdAt;

    private int activeJobs = 0;
}

