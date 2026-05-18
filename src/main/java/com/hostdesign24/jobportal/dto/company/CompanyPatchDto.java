package com.hostdesign24.jobportal.dto.company;

import com.hostdesign24.jobportal.model.enums.CompanySize;
import com.hostdesign24.jobportal.model.enums.Industry;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class CompanyPatchDto {

    private String name;

    private String description;

    /** Longer "About / Culture" copy, optional. */
    private String about;

    /** Optional promotional video URL (YouTube, Vimeo, etc.). */
    private String promoVideoUrl;

    private String website;

    private Industry industry;

    private CompanySize size;

    private MultipartFile logo;

    @Valid
    private CompanyAddressDto address;
}
