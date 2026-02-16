package com.hostdesign24.jobportal.dto;

import com.hostdesign24.jobportal.dto.file.FileDto;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class RecruiterProfileResponseDto {
    private String firstName;
    private String lastName;
    private String city;
    private String state;
    private String country;
    private String company;
    private FileDto profilePhoto;
}
