package com.hostdesign24.jobportal.dto.company;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Getter
@Setter
public class CompanyEntryDto {

    @NotBlank(message = "name is required")
    private String name;

    private MultipartFile logo;

    @Valid
    private CompanyAddressDto address;
}

