package com.hostdesign24.jobportal.dto.skill;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SkillEntryDto {

    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "experience level is required")
    private int yearsOfExperience;
}

