package com.hostdesign24.jobportal.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobSeekerProfileSaveDto {

    private String firstName;
    private String lastName;
    private String city;
    private String state;
    private String country;

    private String workAuthorization;
    private String employmentType;

    /**
     * File IDs (already uploaded files)
     */
    private MultipartFile resumeFile;
    private MultipartFile profilePhoto;

    /**
     * Skills to save/update
     */
    private List<SkillSaveDto> skills;
}
