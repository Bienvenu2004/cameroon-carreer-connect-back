package com.hostdesign24.jobportal.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class SkillDto {
    private UUID id;
    private String name;
    private String experienceLevel;
    private String yearsOfExperience;
}
