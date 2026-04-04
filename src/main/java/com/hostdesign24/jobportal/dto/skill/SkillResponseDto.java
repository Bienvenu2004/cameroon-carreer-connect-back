package com.hostdesign24.jobportal.dto.skill;

import com.hostdesign24.jobportal.model.enums.ExperienceLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class SkillResponseDto {

    private UUID id;

    private String name;

    private ExperienceLevel experienceLevel;

    private String yearsOfExperience;
}

