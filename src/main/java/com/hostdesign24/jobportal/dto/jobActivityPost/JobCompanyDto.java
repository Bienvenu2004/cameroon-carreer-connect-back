package com.hostdesign24.jobportal.dto.jobActivityPost;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class JobCompanyDto {
    private String name;
    private MultipartFile logo;
}
