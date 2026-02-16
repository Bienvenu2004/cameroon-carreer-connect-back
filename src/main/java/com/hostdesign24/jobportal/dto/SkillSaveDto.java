package com.hostdesign24.jobportal.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillSaveDto {

    private String name;
    private String experienceLevel;
    private String yearsOfExperience;
}
