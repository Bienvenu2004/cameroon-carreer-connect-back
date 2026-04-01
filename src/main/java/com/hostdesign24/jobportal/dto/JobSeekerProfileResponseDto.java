package com.hostdesign24.jobportal.dto;

import com.hostdesign24.jobportal.dto.file.FileDto;
import com.hostdesign24.jobportal.model.File;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class JobSeekerProfileResponseDto {

    private UUID id;
    private String firstName;
    private String lastName;
    private String city;
    private String state;
    private String country;
    private String workAuthorization;
    private String employmentType;
    private FileDto profilePhoto;
    private FileDto resume;
    private List<SkillDto> skills;
}
