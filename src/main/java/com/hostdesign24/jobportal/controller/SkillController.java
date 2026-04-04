package com.hostdesign24.jobportal.controller;

import com.hostdesign24.jobportal.dto.common.ApiResponse;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.skill.SkillEntryDto;
import com.hostdesign24.jobportal.dto.skill.SkillFilterDto;
import com.hostdesign24.jobportal.dto.skill.SkillPatchDto;
import com.hostdesign24.jobportal.dto.skill.SkillResponseDto;
import com.hostdesign24.jobportal.services.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/hjp/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @PostMapping("/")
    public ApiResponse<SkillResponseDto> create(@Valid @RequestBody SkillEntryDto dto) {
        SkillResponseDto createdSkill = skillService.create(dto);
        return ApiResponse.success(createdSkill, "Skill created successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<SkillResponseDto> getById(@PathVariable UUID id) {
        SkillResponseDto skill = skillService.getById(id);
        return ApiResponse.success(skill, "Skill retrieved successfully");
    }

    @GetMapping("/")
    public ApiResponse<PageResponseDto<SkillResponseDto>> getAll(@ModelAttribute SkillFilterDto filter) {
        PageResponseDto<SkillResponseDto> skills = skillService.getAll(filter);
        return ApiResponse.success(skills, "Skills retrieved successfully");
    }

    @PatchMapping("/{id}")
    public ApiResponse<SkillResponseDto> patch(@PathVariable UUID id, @Valid @RequestBody SkillPatchDto dto) {
        SkillResponseDto updatedSkill = skillService.patch(id, dto);
        return ApiResponse.success(updatedSkill, "Skill updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        skillService.delete(id);
        return ApiResponse.success(null, "Skill deleted successfully");
    }
}

