package com.hostdesign24.jobportal.dto.skill;

import com.hostdesign24.jobportal.dto.common.FilterDto;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class SkillFilterDto extends FilterDto {

    private String name;

    private String experienceLevel;

    private String yearsOfExperience;

    private UUID jobSeekerProfileId;
}

