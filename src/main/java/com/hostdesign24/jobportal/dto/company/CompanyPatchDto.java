package com.hostdesign24.jobportal.dto.company;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Getter
@Setter
public class CompanyPatchDto {

    private String name;

    private MultipartFile logo;

    @Valid
    private CompanyAddressDto address;
}

