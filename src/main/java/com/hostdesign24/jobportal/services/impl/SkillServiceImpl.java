package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.skill.SkillEntryDto;
import com.hostdesign24.jobportal.dto.skill.SkillFilterDto;
import com.hostdesign24.jobportal.dto.skill.SkillPatchDto;
import com.hostdesign24.jobportal.dto.skill.SkillResponseDto;
import com.hostdesign24.jobportal.exception.ResourceNotFoundException;
import com.hostdesign24.jobportal.mapper.SkillMapper;
import com.hostdesign24.jobportal.model.Skill;
import com.hostdesign24.jobportal.model.enums.ExperienceLevel;
import com.hostdesign24.jobportal.repository.SkillRepository;
import com.hostdesign24.jobportal.repository.specifications.SkillSpecification;
import com.hostdesign24.jobportal.services.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {

    private final SkillRepository skillRepository;
    private final SkillSpecification skillSpecification;
    private final SkillMapper skillMapper;

    @Override
    @Transactional
    public SkillResponseDto create(SkillEntryDto dto) {
        Skill skill = skillMapper.toEntity(dto);
        skill.setExperienceLevel(ExperienceLevel.fromYears(dto.getYearsOfExperience()));
        Skill savedSkill = skillRepository.save(skill);
        return skillMapper.toResponse(savedSkill);
    }

    @Override
    @Transactional(readOnly = true)
    public SkillResponseDto getById(UUID id) {
        Skill skill = findSkillOrThrow(id);
        return skillMapper.toResponse(skill);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<SkillResponseDto> getAll(SkillFilterDto filter) {
        Specification<Skill> specification = skillSpecification.build(filter);
        Page<Skill> skillsPage = skillRepository.findAll(specification, filter.toPageable());

        List<SkillResponseDto> content = skillsPage.getContent().stream()
                .map(skillMapper::toResponse)
                .toList();

        return new PageResponseDto<>(
                content,
                skillsPage.getNumber(),
                skillsPage.getSize(),
                skillsPage.getTotalElements(),
                skillsPage.getTotalPages(),
                skillsPage.isLast()
        );
    }

    @Override
    @Transactional
    public SkillResponseDto patch(UUID id, SkillPatchDto dto) {
        Skill skill = findSkillOrThrow(id);
        skillMapper.updateFromPatchDto(dto, skill);
        Skill savedSkill = skillRepository.save(skill);
        return skillMapper.toResponse(savedSkill);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Skill skill = findSkillOrThrow(id);
        skillRepository.delete(skill);
    }

    private Skill findSkillOrThrow(UUID id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + id));
    }
}

