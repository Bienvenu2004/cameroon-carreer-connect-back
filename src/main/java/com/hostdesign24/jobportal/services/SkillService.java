package com.hostdesign24.jobportal.services;

import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.skill.SkillEntryDto;
import com.hostdesign24.jobportal.dto.skill.SkillFilterDto;
import com.hostdesign24.jobportal.dto.skill.SkillPatchDto;
import com.hostdesign24.jobportal.dto.skill.SkillResponseDto;

import java.util.List;
import java.util.UUID;

public interface SkillService {

    SkillResponseDto create(SkillEntryDto dto);

    SkillResponseDto getById(UUID id);

    PageResponseDto<SkillResponseDto> getAll(SkillFilterDto filter);

    SkillResponseDto patch(UUID id, SkillPatchDto dto);

    void delete(UUID id);

    /**
     * Distinct skill names matching the substring {@code query}, capped at
     * {@code limit} entries. Returns an empty list when the query is blank.
     * Powers the seeker-profile tag autocomplete.
     */
    List<String> suggestNames(String query, int limit);
}

